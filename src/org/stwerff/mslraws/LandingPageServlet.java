package org.stwerff.mslraws;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.stwerff.mslraws.images.InitListener;
import org.stwerff.mslraws.parser.SiteParser;

import com.chap.memo.memoNodes.MemoNode;
import com.eaio.uuid.UUID;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

public class LandingPageServlet extends HttpServlet {
	private static final long serialVersionUID = 8110001398162695563L;
	static final ObjectMapper om = new ObjectMapper();
	static Queue queue = QueueFactory.getDefaultQueue();
	static final String jpl = "http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol";
	static final String msss = "http://mars.jpl.nasa.gov/msl-raw-images/msss";
	
	public static String quickServe = "";
	
	public String generateJSON(int sol, String camera, boolean countsOnly, boolean flat, boolean repair) throws IOException{
		long start = System.currentTimeMillis();
		MemoNode baseNode = InitListener.baseNode;
		MemoNode allImagesNode = InitListener.allImagesNode;
		if (baseNode == null){
			baseNode = MemoNode.getRootNode().getChildByStringValue("msl-raw-images");
		}
		
		List<MemoNode> imgList = null;
		if (repair){
			if (allImagesNode == null){
				allImagesNode = baseNode.getChildByStringValue("allImages");
			}
			if (allImagesNode == null){
				allImagesNode = baseNode.addChild(new MemoNode("allImages"));
			}
			imgList = allImagesNode.getChildren();
		}

		
		JsonFactory f = new JsonFactory();
		f.setCodec(om);
		StringWriter resultWriter = new StringWriter();
		JsonGenerator g = f.createJsonGenerator(resultWriter);
		g.writeStartArray();
		
		int imageCount=0;
		List<UUID> cameras = baseNode.getChildIds();
		Iterator<UUID> iter = cameras.iterator();
		while (iter.hasNext()){
			MemoNode cam = new MemoNode(iter.next());
			if (cam.getStringValue().equals("sols")) continue;
			if (!camera.equals("all") && !cam.getStringValue().equals(camera)) continue;
			
			List<MemoNode> sols;
			if (sol > 0){
				sols = cam.getChildrenByStringValue("sol"+sol,-1);
				if (sols.size()>1){
					System.out.println("Strange: sol"+sol+ " is duplicated!"+sols.size());
				}
			} else {
				sols = cam.getChildren();
			}
			if (sols.size()>0){
				Iterator<MemoNode> soliter = sols.iterator();
				while (soliter.hasNext()){
					MemoNode node_sol = soliter.next();
					MemoNode node_images = node_sol.getChildByStringValue("images");
					if (node_images == null){
						node_images = node_sol.addChild(new MemoNode("images"));
					}
					List<UUID> images = node_images.getChildIds();
					int nofc = images.size();
					if (nofc > 0){
						if (!countsOnly){
							Iterator<UUID> image_iter = images.iterator();
							while(image_iter.hasNext()){
								MemoNode image = new MemoNode(image_iter.next());
								String url = image.getStringValue();
								if (repair){
									if (imgList != null && !imgList.contains(image)){
										allImagesNode.addChild(image);
										//adds image to allImagesNode if missing
									}
								}
								ObjectNode imageNode = om.createObjectNode();
								imageNode.put("uuid", image.getId().toString());
								imageNode.put("name", url.substring(url.lastIndexOf('/')+1));
								imageNode.put("url", url.replace(jpl,"J$").replace(msss,"M$"));
								imageNode.put("thumbnailUrl", image.getPropertyValue("thumbnail").replace(jpl,"J$").replace(msss,"M$"));
								imageNode.put("type", image.getPropertyValue("type"));
								imageNode.put("unixTimeStamp", image.getPropertyValue("timestamp"));
								imageNode.put("fileTimeStamp", image.getPropertyValue("fileTimeStamp"));
								imageNode.put("lastModified", image.getPropertyValue("lastModified"));
								imageNode.put("camera", cam.getStringValue());
								imageNode.put("sol", node_sol.getStringValue().replace("sol", ""));
								g.writeTree(imageNode);
								if (imageCount++%100==0) g.flush();
							}
						}
					}
				}
			}
		}
		if (repair){
			imgList=null;
			List<UUID> allImages = allImagesNode.getChildIds();
			HashSet<String> set = new HashSet<String>(allImages.size());
			for (UUID uuid : allImages){
				MemoNode image = new MemoNode(uuid);
				String url = image.getStringValue();
				//remove duplicates
				if (set.contains(url)){
					image.delete();
					continue;
				} else {
					set.add(url);
				}
				//check type
				String type = SiteParser.getType(url.substring(url.lastIndexOf('/')+1));
				if (!image.getPropertyValue("type").equals(type)) image.setPropertyValue("type", type);
			}
			MemoNode statsNode=baseNode.getChildByStringValue("imageStats");
			if (statsNode != null){
				String totalCount = new Integer(allImagesNode.getChildIds().size()).toString();
				statsNode.setPropertyValue("totalCount",totalCount);
				System.out.println("Set count to:"+totalCount);
			}
			if (imageCount != allImages.size()){
				System.out.println("Returning less images than allImages!"+imageCount+"/"+allImages.size());
			}
			quickServe="";
		}
		g.writeEndArray();
		g.flush();
		g.close();
		String res =resultWriter.toString();
		System.out.println(imageCount+ " images took:"+(System.currentTimeMillis()-start)+"ms");
		return res;
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		doGet(req,resp);
	}
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		
		boolean fullReload = req.getParameter("reload")!=null?true:false;
		boolean mayCache = true;
		
		int sol=-1;
		String ssol = req.getParameter("sol");
		if (ssol != null) {
			try {
			   sol = Integer.parseInt(ssol);
			   fullReload=true;
			   mayCache=false;
			} catch (Exception e){
				System.out.println("Non numeric sol input!");
			}
		}
		String camera = req.getParameter("cam");
		if (camera == null){
			camera="all";
		} else {
			fullReload=true;
			mayCache=false;
		}
		boolean countsOnly = (req.getParameter("counts") != null);
		String flat_string = req.getParameter("flat"); 
		boolean flat = (flat_string != null);
		boolean cron = (flat && flat_string.equals("cron"));
		boolean repair = (req.getParameter("repair")!=null);
		boolean passOn = (req.getParameter("passOn")!=null);
		
		if (passOn){
			TaskOptions options = withUrl("/landing");
			if (repair) options.param("repair", "true");
			if (flat) options.param("flat", "true");
			if (fullReload) options.param("reload", "true");
			if (sol>=0) options.param("sol", ssol);
			if (!camera.equals("all")) options.param("cam", camera);
			queue.add(options);
			return;
		}
		
		if (countsOnly || !flat){
			fullReload=true;
			mayCache=false;
		}
		if (repair){
			fullReload=true;
		}
		String result="";
		if (!fullReload && !quickServe.equals("")){
			System.out.println("QuickServe string");
			result = quickServe;
		} else {
			System.out.println("Re-generating JSON");
			result = generateJSON(sol,camera,countsOnly,flat,repair);
			System.out.println("Done regenerating JSON");
			if (mayCache){
				quickServe = result;
				System.out.println("Stored for quickServe");
			}
		}
		resp.setBufferSize(500000);
		resp.setContentType("application/json");
		if (!cron){
			resp.setContentLength(result.length());
			resp.getWriter().write(result);
		} else {
			resp.setContentLength(2);
			resp.getWriter().write("[]");
		}
		if (repair){
			MemoNode.flushDB();
			MemoNode.compactDB();
		}
	}
}
