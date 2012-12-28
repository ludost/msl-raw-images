package org.stwerff.mslagents;

import org.joda.time.DateTime;
import org.stwerff.mslagents.data.ImageList;

import com.almende.eve.agent.Agent;
import com.almende.eve.json.annotation.Name;
import com.almende.eve.json.annotation.Required;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SolAgent extends Agent {
	static final ObjectMapper om = new ObjectMapper();

	public void setList(@Name("list") ArrayNode list) {
		getContext().put("list", list.toString());
	}

	public String getLastChange() {
		if (getContext().containsKey("lastChange")) {
			if (getContext().containsKey("incomplete")
					&& !(Boolean) getContext().get("incomplete")) {
				return (String) getContext().get("lastChange");
			} else {
				System.err.println("Incomplete Sol!");
			}
		} else {
			System.err.println("lastChange not set!");
		}
		return DateTime.now().toString();
	}

	public ArrayNode getList() {
		if (getContext().containsKey("list")) {
			try {
				return (ArrayNode) om.readTree((String) getContext()
						.get("list"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return updateList(false);
	}

	public ArrayNode updateList(@Name("reload") @Required(false) Boolean reload) {
		int sol = new Integer(getContext().getAgentId().substring(4))
				.intValue();
		ArrayNode list = om.createArrayNode();
		try {
			if (reload == null || !reload) {
				if (getContext().containsKey("list")) {
					list = (ArrayNode) om.readTree((String) getContext().get(
							"list"));
				}
			}
			String url = "http://localhost:8080/MSLAgents/agents/collector";
			String method = "getSol";
			ObjectNode params = om.createObjectNode();
			params.put("sol", sol);
			try {
				ArrayNode result = send(url, method, params, ArrayNode.class);
				list = ImageList.merge(result, list);
			} catch (Exception e){
				e.printStackTrace();
			}
			getContext().put("list", list.toString());
//			updateHeads();

			url = "http://localhost:8080/MSLAgents/agents/stats";
			params = om.createObjectNode();
			params.put("list", list);
			send(url, "updateSol", params, void.class);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	public void updateHeads() {
		ArrayNode list = om.createArrayNode();
		try {
			list = (ArrayNode) om.readTree((String) getContext().get("list"));
			String url = "http://localhost:8080/MSLAgents/agents/heads";
			ObjectNode params = om.createObjectNode();
			params.put("list", list);
			ArrayNode result = om.createArrayNode();
			try {
				result = send(url, "updateList", params, ArrayNode.class);
				list = ImageList.merge(list, result);
				if (result.size() > 0
						|| !getContext().containsKey("lastChange")) {
					getContext().put("lastChange", DateTime.now().toString());
				}
			} catch (Exception e) {
				System.err.println("UpdateList error:" + e);
			}

			if (result == null || result.size() >= HeadsAgent.MAX_PER_RUN) {
				getContext().put("incomplete", true);
			} else {
				getContext().put("incomplete", false);
			}

			getContext().put("list", list.toString());
			url = "http://localhost:8080/MSLAgents/agents/stats";
			params = om.createObjectNode();
			params.put("list", list);
			send(url, "updateSol", params, void.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public String getDescription() {
		return "Collects and stores sol "
				+ getContext().getAgentId().substring(4);
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

}
