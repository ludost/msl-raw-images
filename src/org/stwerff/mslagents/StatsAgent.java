package org.stwerff.mslagents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.stwerff.mslagents.data.Image;
import org.stwerff.mslagents.data.SolStats;

import com.almende.eve.agent.Agent;
import com.almende.eve.json.annotation.Name;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@SuppressWarnings("unchecked")
public class StatsAgent extends Agent {
	final static ObjectMapper om = new ObjectMapper();
	Map<Integer,SolStats> stats = Collections.synchronizedMap(new HashMap<Integer,SolStats>());
	
	public List<Integer> getIncompleteSols(){
		List<Integer> result = new ArrayList<Integer>(10);
		synchronized(stats){
			if (getContext().containsKey("stats")){
				stats.putAll((Map<Integer,SolStats>)getContext().get("stats"));
			}
			for (SolStats sol : stats.values()){
				if (sol.isIncompleteHeads()){
					result.add(sol.getSol());
				}
			}
			return result;
		}
	}
	public int getMaxSol(){
		synchronized(stats){
			if (getContext().containsKey("stats")){
				stats.putAll((Map<Integer,SolStats>)getContext().get("stats"));
			}
			int result=0;
			for (int sol : stats.keySet()){
				result=Math.max(result, sol);
			}
			return result;
		}
	}
	public int getTotalCount(){
		synchronized(stats){
			if (getContext().containsKey("stats")){
				stats.putAll((Map<Integer,SolStats>)getContext().get("stats"));
			}
			int result=0;
			for (SolStats sol : stats.values()){
				result+=sol.getNofImages();
			}
			return result;
		}
	}
	
	public void updateSol(@Name("list") ArrayNode list){
		if (list.size()<= 0){
			return;
		}
		Integer sol = list.get(0).get("sol").asInt();
		SolStats solstats = new SolStats(sol);
		for (int i=0; i<list.size(); i++){
			try {
				Image image = om.treeToValue(list.get(i),Image.class);
				solstats.addImage();
				solstats.addGenImage(image.getType());
				if (!solstats.isIncompleteHeads() && image.getFileTimeStamp() == null){
					solstats.setIncompleteHeads(true);
				}
			} catch (Exception e){
				System.err.println("Invalid image found:"+list.get(i).toString());
			}
		}
		synchronized(stats){
			if (getContext().containsKey("stats")){
				stats.putAll((Map<Integer,SolStats>)getContext().get("stats"));
			}
			//TODO: compare for new images/twitter function
			if (stats.containsKey(sol)){
				SolStats oldStats = stats.get(sol);
				if (solstats.getNofImages()> oldStats.getNofImages()){
					System.err.println("Found "+(solstats.getNofImages()-oldStats.getNofImages())+" new images for sol:"+sol);
				}
			} else {
				System.err.println("Found "+(solstats.getNofImages())+" new images for new sol:"+sol);				
			}
			stats.put(sol, solstats);
			getContext().put("stats",stats);
		}
	}
	
	public ArrayNode getStats(){
		synchronized(stats){
			if (getContext().containsKey("stats")){
				stats.putAll((Map<Integer,SolStats>)getContext().get("stats"));
			}
			return om.valueToTree(stats.values());
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
