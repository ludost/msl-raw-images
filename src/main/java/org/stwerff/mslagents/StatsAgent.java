package org.stwerff.mslagents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.stwerff.mslagents.data.Image;
import org.stwerff.mslagents.data.SolStats;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.annotation.Name;
import com.almende.eve.agent.annotation.ThreadSafe;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SuppressWarnings("unchecked")
@ThreadSafe(true)
public class StatsAgent extends Agent {
	final static ObjectMapper om = new ObjectMapper();
	Map<Integer, SolStats> stats = Collections
			.synchronizedMap(new HashMap<Integer, SolStats>());

	public List<Integer> getIncompleteSols(@Name("type") String type) {
		List<Integer> result = new ArrayList<Integer>(10);
		synchronized (stats) {
			if (getState().containsKey("stats")) {
				stats.putAll((Map<Integer, SolStats>) getState().get("stats"));
			}
			for (SolStats sol : stats.values()) {
				if (sol.isIncomplete(type)) {
					result.add(sol.getSol());
				}
			}
			return result;
		}
	}

	public int getMaxSol() {
		synchronized (stats) {
			if (getState().containsKey("stats")) {
				stats.putAll((Map<Integer, SolStats>) getState().get("stats"));
			}
			int result = 0;
			for (int sol : stats.keySet()) {
				result = Math.max(result, sol);
			}
			return result;
		}
	}

	public int getTotalCount() {
		synchronized (stats) {
			if (getState().containsKey("stats")) {
				stats.putAll((Map<Integer, SolStats>) getState().get("stats"));
			}
			int result = 0;
			for (SolStats sol : stats.values()) {
				result += sol.getNofImages();
			}
			return result;
		}
	}

	public void updateSol(@Name("list") ArrayNode list) {
		if (list.size() <= 0) {
			return;
		}
		Integer sol = list.get(0).get("sol").asInt();
		SolStats solstats = new SolStats(sol);
		solstats.setIncomplete("spice", false);
		solstats.setIncomplete("heads", false);
		for (int i = 0; i < list.size(); i++) {
			try {
				Image image = om.treeToValue(list.get(i), Image.class);
				solstats.addImage();
				if (image.getSol() != sol) {
					System.err
							.println("Incorrect sol image found in list for sol:"
									+ sol + "/" + image.getSol());
					continue;
				}
				solstats.addGenImage(image.getType());
				if (!solstats.isIncomplete("heads")
						&& image.getFileTimeStamp() == null) {
					solstats.setIncomplete("heads", true);
				}
				if (!solstats.isIncomplete("spice") && image.getLmst() == null) {
					solstats.setIncomplete("spice", true);
				}
				if (!solstats.isIncomplete("spice")
						&& (image.getBearing() == null || image.getBearing()
								.equals("---"))) {
					solstats.setIncomplete("spice", true);
				}
			} catch (Exception e) {
				System.err.println("Invalid image found:"
						+ list.get(i).toString());
			}
		}
		synchronized (stats) {
			if (getState().containsKey("stats")) {
				stats.putAll((Map<Integer, SolStats>) getState().get("stats"));
			}
			String message = "";
			if (stats.containsKey(sol)) {
				SolStats oldStats = stats.get(sol);
				if (solstats.getNofImages() > oldStats.getNofImages()) {
					message = ("Found "
							+ (solstats.getNofImages() - oldStats
									.getNofImages()) + " new images for sol:" + sol);
				}
			} else {
				message = ("Found " + (solstats.getNofImages())
						+ " new images for new sol:" + sol);
			}
			if (!"".equals(message)) {
				System.err.println(message);
				message += " Check: http://msl-raw-images.com/";
				ObjectNode params = om.createObjectNode();
				params.put("tweet", message);
				try {
					send("http://localhost:8080/MSLAgents/agents/twitter",
							"sendMessage", params);
				} catch (Exception e) {
					System.err.println("Failed to contact twitter agent.");
				}
			}
			stats.put(sol, solstats);
			getState().put("stats", stats);
		}
	}

	public ArrayNode getStats() {
		synchronized (stats) {
			if (getState().containsKey("stats")) {
				stats.putAll((Map<Integer, SolStats>) getState().get("stats"));
			}
			return om.valueToTree(stats.values());
		}
	}

	public void resetIncomplete(@Name("type") String type) {
		synchronized (stats) {
			if (getState().containsKey("stats")) {
				stats.putAll((Map<Integer, SolStats>) getState().get("stats"));
			}
			for (SolStats sol : stats.values()) {
				sol.setIncomplete(type, true);
				stats.put(sol.getSol(), sol);
			}
			getState().put("stats", stats);
		}
	}

	@Override
	public String getDescription() {
		return "Agent obtains basic statistics about sol-lists, global count, etc.";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

}
