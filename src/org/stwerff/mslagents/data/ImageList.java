package org.stwerff.mslagents.data;

import java.util.HashMap;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class ImageList {
	public static final ObjectMapper om = new ObjectMapper();

	public static ArrayNode merge(ArrayNode origList, ArrayNode newList) {
		HashMap<String, JsonNode> map = new HashMap<String, JsonNode>(
				origList.size());
		Iterator<JsonNode> iter = origList.elements();
		while (iter.hasNext()) {
			ObjectNode node = (ObjectNode) iter.next();
			map.put(node.get("name").asText(), node);
		}
		iter = newList.elements();
		while (iter.hasNext()) {
			ObjectNode node = (ObjectNode) iter.next();
			map.put(node.get("name").asText(), node);
		}
		ArrayNode result = om.createArrayNode();
		result.addAll(map.values());
		return result;
	}
}
