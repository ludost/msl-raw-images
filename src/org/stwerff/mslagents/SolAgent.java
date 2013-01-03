package org.stwerff.mslagents;

import org.joda.time.DateTime;
import org.stwerff.mslagents.data.Image;
import org.stwerff.mslagents.data.ImageList;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.annotation.ThreadSafe;
import com.almende.eve.json.annotation.Name;
import com.almende.eve.json.annotation.Required;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ThreadSafe(true)
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
	public void updateSpice(@Name("reload") @Required(false) Boolean reload) {
		ArrayNode list = om.createArrayNode();
		if(reload==null)reload=false;
		try {
			list = (ArrayNode) om.readTree((String) getContext().get("list"));
			String url = "http://localhost:8080/MSLAgents/agents/spice";
			ObjectNode params = om.createObjectNode();
			params.put("list", list);
			params.put("reload", reload);
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

			url = "http://localhost:8080/MSLAgents/agents/bearing";
			params = om.createObjectNode();
			params.put("list", list);
			params.put("reload", reload);
			result = om.createArrayNode();
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

			getContext().put("list", list.toString());
			url = "http://localhost:8080/MSLAgents/agents/stats";
			params = om.createObjectNode();
			params.put("list", list);
			send(url, "updateSol", params, void.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//Reload list, redetermining image type and reloading stats.
	public void reloadList(){
		ArrayNode list = om.createArrayNode();
		ArrayNode result = om.createArrayNode();
		try {
			list = (ArrayNode) om.readTree((String) getContext().get("list"));
			for (int i=0; i<list.size(); i++){
				Image image = om.treeToValue(list.get(i),Image.class);
				result.add(om.valueToTree(image));
			}
			getContext().put("list", result.toString());
			String url = "http://localhost:8080/MSLAgents/agents/stats";
			ObjectNode params = om.createObjectNode();
			params.put("list", result);
			send(url, "updateSol", params, void.class);
		}catch (Exception e){
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
