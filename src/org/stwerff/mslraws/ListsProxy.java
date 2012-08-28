package org.stwerff.mslraws;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;


@Path("/lists")
public class ListsProxy {
	@PUT
	@Path("/id/{uuid}")
	public Response publishList(@PathParam("uuid") String uuid, String json){
		System.out.println("Received list:"+uuid+" -> "+json);
		return Response.ok().build();
	}
	
}
