package org.stwerff.mslraws;

import java.util.logging.Logger;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.chap.memo.memoNodes.MemoNode;
import com.eaio.uuid.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


@Path("/lists")
public class ListsProxy {
	public static final ObjectMapper om = new ObjectMapper();
	public static final Logger log = Logger.getLogger("ListsProxy");
	
	@GET
	@Produces("application/json")
	public Response getLists(@FormParam("listIds") String ids){
		String[] listIds = new String[0];
		if (ids != null && !ids.equals("")){
			listIds = ids.split(",");
		}
		log.warning("Requesting lists:"+ids);
		ObjectNode root = om.createObjectNode();
		for (String id : listIds){
			root.put(id,getJsonList(id));
		}
		return Response.ok(root.toString()).build();
	}
	@POST
	@Produces("application/json")
	public Response getListsPost(@FormParam("listIds") String ids){
		return getLists(ids);
	}
	public JsonNode getJsonList(String uuid){
		JsonNode node = om.createObjectNode();
		try {
			UUID id = new UUID(uuid);
			if (id != null){
				MemoNode listNode = new MemoNode(id);
				node = om.readTree(new String(listNode.getValue()));
			}
		} catch (Exception e){}
		return node;
	}
	@PUT
	@Path("/id/{uuid}")
	public Response publishList(@PathParam("uuid") String uuid, String json){
		MemoNode baseNode = MemoNode.getRootNode().getChildByStringValue("msl-raws-published");
		if (baseNode == null){
			baseNode = MemoNode.getRootNode().addChild(new MemoNode("msl-raws-published"));
		}
		try {
			UUID id = new UUID(uuid);
			if (id != null){
				MemoNode listNode = baseNode.addChild(new MemoNode(id));
				listNode.update(json);
				log.warning("Stored list:"+json);
				return Response.ok().build();
			}
		} catch (Exception e){}
		return Response.status(Response.Status.BAD_REQUEST).build();
	}
	
	@GET
	@Path("/id/{uuid}")
	@Produces("application/json")
	public Response getList(@PathParam("uuid") String uuid){
		return Response.ok(getJsonList(uuid).toString()).build();
	}
}
