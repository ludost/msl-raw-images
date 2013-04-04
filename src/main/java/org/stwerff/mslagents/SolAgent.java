package org.stwerff.mslagents;

import org.joda.time.DateTime;
import org.stwerff.mslagents.data.Image;
import org.stwerff.mslagents.data.ImageList;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.annotation.Name;
import com.almende.eve.agent.annotation.Required;
import com.almende.eve.agent.annotation.ThreadSafe;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ThreadSafe(true)
public class SolAgent extends Agent {
	static final ObjectMapper om = new ObjectMapper();

	public void setList(@Name("list") ArrayNode list) {
		getState().put("list", list.toString());
	}

	public String getLastChange() {
		if (getState().containsKey("lastChange")) {
			if (getState().containsKey("incomplete")
					&& !(Boolean) getState().get("incomplete")) {
				return (String) getState().get("lastChange");
			} else {
				System.err.println("Incomplete Sol!");
			}
		} else {
			System.err.println("lastChange not set!");
		}
		return DateTime.now().toString();
	}

	public ArrayNode getList() {
		if (getState().containsKey("list")) {
			try {
				return (ArrayNode) om.readTree((String) getState().get("list"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return updateList(false);
	}

	public ArrayNode updateList(@Name("reload") @Required(false) Boolean reload) {
		int sol = new Integer(getState().getAgentId().substring(4)).intValue();
		ArrayNode list = om.createArrayNode();
		try {
			if (reload == null || !reload) {
				if (getState().containsKey("list")) {
					list = (ArrayNode) om.readTree((String) getState().get(
							"list"));
				}
			}
			String url = "local://collector";
			String method = "getSol";
			ObjectNode params = om.createObjectNode();
			params.put("sol", sol);
			try {
				ArrayNode result = send(url, method, params, ArrayNode.class);
				list = ImageList.merge(result, list);
			} catch (Exception e) {
				e.printStackTrace();
			}
			getState().put("list", list.toString());
			// updateHeads();

			url = "local://stats";
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
			list = (ArrayNode) om.readTree((String) getState().get("list"));
			String url = "local://heads";
			ObjectNode params = om.createObjectNode();
			params.put("list", list);
			ArrayNode result = om.createArrayNode();
			try {
				result = send(url, "updateList", params, ArrayNode.class);
				list = ImageList.merge(list, result);
				if (result.size() > 0 || !getState().containsKey("lastChange")) {
					getState().put("lastChange", DateTime.now().toString());
				}
			} catch (Exception e) {
				System.err.println("UpdateList error:" + e);
			}

			if (result == null || result.size() >= HeadsAgent.MAX_PER_RUN) {
				getState().put("incomplete", true);
			} else {
				getState().put("incomplete", false);
			}

			getState().put("list", list.toString());
			url = "local://stats";
			params = om.createObjectNode();
			params.put("list", list);
			send(url, "updateSol", params, void.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void updateSpice(@Name("reload") @Required(false) Boolean reload) {
		ArrayNode list = om.createArrayNode();
		if (reload == null)
			reload = false;
		try {
			list = (ArrayNode) om.readTree((String) getState().get("list"));
			String url = "local://spice";
			ObjectNode params = om.createObjectNode();
			params.put("list", list);
			params.put("reload", reload);
			ArrayNode result = om.createArrayNode();
			try {
				result = send(url, "updateList", params, ArrayNode.class);
				list = ImageList.merge(list, result);
				if (result.size() > 0 || !getState().containsKey("lastChange")) {
					getState().put("lastChange", DateTime.now().toString());
				}
			} catch (Exception e) {
				System.err.println("UpdateList error:" + e);
			}

			url = "local://bearing";
			params = om.createObjectNode();
			params.put("list", list);
			params.put("reload", reload);
			result = om.createArrayNode();
			try {
				result = send(url, "updateList", params, ArrayNode.class);
				list = ImageList.merge(list, result);
				if (result.size() > 0 || !getState().containsKey("lastChange")) {
					getState().put("lastChange", DateTime.now().toString());
				}
			} catch (Exception e) {
				System.err.println("UpdateList error:" + e);
			}

			getState().put("list", list.toString());
			url = "local://stats";
			params = om.createObjectNode();
			params.put("list", list);
			send(url, "updateSol", params, void.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Reload list, redetermining image type and reloading stats.
	public void reloadList() {
		ArrayNode list = om.createArrayNode();
		ArrayNode result = om.createArrayNode();
		try {
			list = (ArrayNode) om.readTree((String) getState().get("list"));
			for (int i = 0; i < list.size(); i++) {
				Image image = om.treeToValue(list.get(i), Image.class);
				result.add(om.valueToTree(image));
			}
			getState().put("list", result.toString());
			String url = "local://stats";
			ObjectNode params = om.createObjectNode();
			params.put("list", result);
			send(url, "updateSol", params, void.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getDescription() {
		return "Collects and stores sol "
				+ getState().getAgentId().substring(4);
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

}
