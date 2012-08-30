package org.stwerff.mslraws;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import java.io.IOException;
//import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.stwerff.mslraws.parser.SiteParser;

import com.chap.memo.memoNodes.MemoNode;
import com.eaio.uuid.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
//import com.google.appengine.api.blobstore.BlobKey;
//import com.google.appengine.api.blobstore.BlobstoreService;
//import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
//import com.google.appengine.api.files.AppEngineFile;
//import com.google.appengine.api.files.FileService;
//import com.google.appengine.api.files.FileServiceFactory;
//import com.google.appengine.api.files.FileWriteChannel;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

public class LandingPageServlet extends HttpServlet {
	private static final long serialVersionUID = 8110001398162695563L;
	static final ObjectMapper om = new ObjectMapper();
	static MemcacheService memCache = MemcacheServiceFactory.getMemcacheService();
	static Queue queue = QueueFactory.getDefaultQueue();
	
	static final String jpl = "http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol";
	static final String msss = "http://mars.jpl.nasa.gov/msl-raw-images/msss";
	
	static MemoNode quickServe = null;
	
	public ArrayNode generateJSON(int sol, String camera, boolean countsOnly, boolean flat, boolean repair){
		MemoNode baseNode = MemoNode.getRootNode().getChildByStringValue("msl-raw-images");
		
		ArrayList<MemoNode> imgList = null;
		MemoNode allImagesNode = baseNode.getChildByStringValue("allImages");
		if (repair){
			if (allImagesNode == null){
				allImagesNode = baseNode.addChild(new MemoNode("allImages"));
			}
			imgList = allImagesNode.getChildren();
		}

		
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
					if (node_sol.getChildByStringValue("images") == null){
						node_sol.addChild(new MemoNode("images"));
					}
					int nofc = 0;
					if (node_sol.getChildByStringValue("images").getChildren() != null){
						nofc = node_sol.getChildByStringValue("images").getChildren().size();
					}
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
								String url = image.getStringValue();
								if (url.equals(""))continue;
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
		if (repair){
			imgList.clear();
			ArrayList<MemoNode> allImages = allImagesNode.getChildren();
			HashSet<String> set = new HashSet<String>(allImages.size());
			for (MemoNode image : allImages){
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
				String totalCount = new Integer(allImagesNode.getChildren().size()).toString();
				statsNode.setPropertyValue("totalCount",totalCount);
				System.out.println("Set count to:"+totalCount);
			}
			memCache.delete("quickServe");
		}
		return result;
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
		boolean flat = (req.getParameter("flat") != null);
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
		if (!fullReload && memCache.get("quickServe") != null && memCache.get("quickServe") != ""){
			UUID uuid = new UUID((String)memCache.get("quickServe"));
			byte[] val = null;
			if (quickServe != null && quickServe.getId().equals(uuid)){
				val = quickServe.getValue();
				System.out.println("Got quickServe static node");
			}
			if (val == null){
				val = new MemoNode(uuid).getValue();
				System.out.println("Got datastore based static node:"+(quickServe != null?quickServe.getId():"")+"/"+uuid);
			}
			if (val != null && val.length>0){
				result = new String(val);
			}
		}
		if (result.equals("")) {
			System.out.println("Re-generated JSON");
			ArrayNode resultNode = generateJSON(sol,camera,countsOnly,flat,repair);
			result=resultNode.toString();
			if (mayCache){
				MemoNode lastServed= MemoNode.getRootNode().getChildByStringValue("msl-raws-lastServed");
				if (lastServed != null){
					for (MemoNode page : lastServed.getChildren()){
						page.delete();
					}
				} else {
					lastServed = MemoNode.getRootNode().addChild(new MemoNode("msl-raws-lastServed"));
				}
				quickServe = lastServed.addChild(new MemoNode(result));
				memCache.put("quickServe", quickServe.getId().toString());
				System.out.println("Stored for quickServe");
			}
		}
		resp.setBufferSize(500000);
		resp.setContentType("application/json");
		resp.setContentLength(result.length());
		resp.getWriter().write(result);
		if (repair){
			MemoNode.flushDB();MemoNode.compactDB();
		}
	}
}
