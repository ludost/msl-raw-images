package org.stwerff.mslraws;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.chap.memo.memoNodes.MemoNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class LandingPageServlet extends HttpServlet {
	//Generates json document for frontend sorting/listing
	private static final long serialVersionUID = 8110001398162695563L;
	static final ObjectMapper om = new ObjectMapper();
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		
		int sol=-1;
		String ssol = req.getParameter("sol");
		if (ssol != null) {
			try {
			   sol = Integer.parseInt(ssol);
			} catch (Exception e){
				System.out.println("Non numeric sol input!");
			}
		}
		String camera = req.getParameter("cam");
		if (camera == null){
			camera="all";
		}
		
		boolean countsOnly = false;
		if (req.getParameter("counts") != null) {
			countsOnly = true;
		}
		boolean flat = false;
		if (req.getParameter("flat") != null) {
			flat = true;
		}
		
		resp.setContentType("application/json");
		MemoNode baseNode = MemoNode.getRootNode().getChildByStringValue("msl-raw-images");
				
		ArrayList<MemoNode> cameras = baseNode.getChildren();
		Iterator<MemoNode> iter = cameras.iterator();
		ArrayNode result = om.createArrayNode();
		while (iter.hasNext()){
			MemoNode cam = iter.next();
			if (cam.getStringValue().equals("sols")) continue;
			if (!camera.equals("all") && !cam.getStringValue().equals(camera)) continue;
			
			ArrayList<MemoNode> sols;
			if (sol > 0){
				sols = cam.getChildrenByStringValue("sol"+sol,1);
			} else {
				sols = cam.getChildren();
			}
			if (sols.size()>0){
				ObjectNode cameraNode = om.createObjectNode();
				ObjectNode solNodes = om.createObjectNode();
				if (!flat){
					result.add(cameraNode);					
					cameraNode.put("camera", cam.getStringValue());
					cameraNode.put("sols", solNodes);
				}
				Iterator<MemoNode> soliter = sols.iterator();
				while (soliter.hasNext()){
					MemoNode node_sol = soliter.next();	
					int nofc = node_sol.getChildByStringValue("images").getChildren().size();
					if (nofc > 0){
						ObjectNode solNode = om.createObjectNode();
						if (!flat){
							solNodes.put(node_sol.getStringValue().replace("sol", ""),solNode);
							solNode.put("count", nofc);
						}
						if (!countsOnly){
							ArrayList<MemoNode> images = node_sol.getChildByStringValue("images").getChildren();
							Iterator<MemoNode> image_iter = images.iterator();
							ArrayNode imagesNode = om.createArrayNode();
							if (!flat){
								solNode.put("images", imagesNode);
							}
							while(image_iter.hasNext()){
								MemoNode image = image_iter.next();
								ObjectNode imageNode = om.createObjectNode();
								String url = image.getStringValue();
								imageNode.put("name", url.substring(url.lastIndexOf('/')+1));
								imageNode.put("url", url);
								imageNode.put("thumbnailUrl", image.getPropertyValue("thumbnail"));
								imageNode.put("type", image.getPropertyValue("type"));
								if (flat){
									imageNode.put("camera", cam.getStringValue());
									imageNode.put("sol", node_sol.getStringValue().replace("sol", ""));
									result.add(imageNode);
								} else {
									imagesNode.add(imageNode);
								}
							}
						}
					}
				}
			}
		}
		resp.getWriter().write(result.toString());
	}
}
