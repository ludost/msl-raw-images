package org.stwerff.mslrawimages;

import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

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
		List<ImageList> result=null;
		if (ids != null && !ids.equals("")){
			listIds = ids.split(",");
			result = ImageList.getListsByUuid(listIds);
		}
		if (listIds.length == 0){
			result = ImageList.getAllLists();
		}
		ObjectNode root = om.createObjectNode();
		if (result != null){
			for (ImageList list: result){
				if (list != null){
					root.put(list.uuid, list.toNode());
				}
			}
		}
		return Response.ok(root.toString()).build();
	}
	@POST
	@Produces("application/json")
	public Response getListsPost(@FormParam("listIds") String ids){
		return getLists(ids);
	}
	@PUT
	@Path("/id/{uuid}")
	public Response publishList(@PathParam("uuid") String uuid, String json){
		ImageList list = ImageList.getListByUuid(uuid);
		if (list == null){
			list = new ImageList();
		}
		list.json=json;
		list.uuid=uuid;
		list.store();
		log.warning("Stored list:"+json);
		return Response.ok().build();
	}
	
	@GET
	@Path("/id/{uuid}")
	@Produces("application/json")
	public Response getList(@PathParam("uuid") String uuid){
		return Response.ok(ImageList.getListByUuid(uuid).toString()).build();
	}
}
