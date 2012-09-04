package org.stwerff.mslraws.parser;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

import org.stwerff.mslraws.LandingPageServlet;

import com.chap.memo.memoNodes.MemoNode;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;


public class SiteParser {
	private static final Logger log = Logger.getLogger("msl-raw-images");
	static Queue queue = QueueFactory.getDefaultQueue();
   
	static final String jpl = "http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol";
	static final String msss = "http://mars.jpl.nasa.gov/msl-raw-images/msss";
	
	public static String getCamera(String filename){
		String start = filename.substring(0, 3);
		if (start.matches("[0-9]+")){
			//Malin cam:
			return filename.substring(4,6);
		} else {
			return start;
		}
	}
	public static String getType(String filename){
		if (filename.charAt(16) == '_'){
			if (filename.charAt(17) == 'T') return "thumbnail";
			if (filename.charAt(17) == 'D') return "downscaled";
			if (filename.charAt(17) == 'S') return "subframe";
			if (filename.charAt(17) == 'F') return "full";
			return "unknown";
		} else {
			if (filename.charAt(16) == 'I') return "thumbnail";
			if (filename.charAt(16) == 'D') return "downscaled";
			if (filename.charAt(16) == 'C') return "subframe";
			if (filename.charAt(16) == 'E') return "full";
			return "unknown";
		}
	}
	
	public static void addToNew(MemoNode imageNode, int sol){
		MemoNode baseNode = MemoNode.getRootNode().getChildByStringValue("msl-raw-images");
		MemoNode newNode = baseNode.getChildByStringValue("newImages");
		if (newNode == null) newNode = baseNode.addChild(new MemoNode("newImages"));
		
		//Add to camera counter
		String type = imageNode.getPropertyValue("type");
		MemoNode camNode = newNode.getChildByStringValue(type);
		if (camNode == null) camNode = newNode.addChild(new MemoNode(type));
		camNode.setChild(imageNode);
		
		//Add to sol counter
		MemoNode solNode = newNode.getChildByStringValue(new Integer(sol).toString());
		if (solNode == null) solNode = newNode.addChild(new MemoNode(new Integer(sol).toString()));
		solNode.addChild(imageNode);
	}
	
	public static int fetch(String s_url, int sol) {
		MemoNode baseNode = MemoNode.getRootNode().getChildByStringValue("msl-raw-images");
		MemoNode allImagesNode = baseNode.getChildByStringValue("allImages");
		if (allImagesNode == null) allImagesNode = baseNode.addChild(new MemoNode("allImages"));
		ArrayList<String> images = new ArrayList<String>(100);
		try {
			URL url = new URL(s_url+"?s="+(sol>=0?sol:""));
			System.out.println("Now opening:"+s_url+"?s="+(sol>=0?sol:""));
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setConnectTimeout(10000);
			con.setReadTimeout(60000);
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					con.getInputStream()),15000);
			String line;
			boolean found = false;
			
			MemoNode solNode = null;
			MemoNode imageNode = null;
			int count=0;
			while ((line = reader.readLine()) != null) {
				if (!found && !line.equals("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" align=\"center\" width=\"100%\">")) continue;
				found=true;
				line=line.trim();
				if (line.isEmpty()) continue;
				int i = line.indexOf("./?rawid=");
				if ( i > -1) {
					line=line.substring(i+9).trim();
					if (sol == -1) sol=Integer.parseInt(line.split("&s=")[1].split("\">")[0]);
					String filename = new String(line.substring(0,line.indexOf("&s")));
					String res = new String(line.split("img src=\"")[1].split("\"")[0].replaceFirst("-thm",""));
					String thumbnail = new String(line.split("src=\"")[1].split("\"")[0]);
					String camera = getCamera(filename);
					if (camera.length()>2 && res.endsWith(".jpg")) res=res.replace(".jpg",".JPG");
					
					if (solNode == null) {
						solNode = baseNode.getChildByStringValue("sols").getChildByStringValue("sol"+sol);
						if (solNode == null) solNode = baseNode.getChildByStringValue("sols").addChild(new MemoNode("sol"+sol));
					}
					MemoNode camNode = baseNode.getChildByStringValue(camera);
					if (camNode == null){
						System.out.println("Warning, can't find camera:"+camera);
						continue;
					}
					MemoNode solcamNode = solNode.getChildByStringValue(camera);
					if (solcamNode == null){
						solcamNode = solNode.addChild(new MemoNode(camera));
					}
					MemoNode camsolNode = camNode.getChildByStringValue("sol"+sol);
					if (camsolNode == null){
						camsolNode = camNode.addChild(new MemoNode("sol"+sol));
					}
					MemoNode containerNode = solcamNode.getChildByStringValue("images");
					if (containerNode == null){
						containerNode = solcamNode.addChild(new MemoNode("images"));
						containerNode.addParent(camsolNode);
					}
					res = res.replace(jpl,"J$").replace(msss,"M$");
					if (containerNode.getChildByStringValue(res) == null){
						if (containerNode.getChildByStringValue(res.replace(".jpg", ".JPG")) != null ||containerNode.getChildByStringValue(res.replace(".jpg", ".JPG")) != null ||
								containerNode.getChildByStringValue(res.replace("J$", jpl).replace("M$", msss)) != null) continue;
						imageNode=containerNode.addChild(new MemoNode(res))
						.setPropertyValue("type",getType(filename))
						.setPropertyValue("thumbnail",thumbnail.replace(jpl,"J$").replace(msss,"M$"))
						.setParent(allImagesNode);
						images.add(imageNode.getId().toString());
						addToNew(imageNode, sol);
					}
				}
				if (imageNode != null) i = line.indexOf("RawImageUTC");
				if (imageNode != null && i > -1) {
					line=line.trim();
					String stringDate = new String(line.split("RawImageUTC\">")[1].split("</div>")[0]);
					stringDate = stringDate.replaceAll("<br/>", " ").replaceAll("&nbsp;", " ");
					imageNode.setPropertyValue("stringDate", stringDate);
					//2012-08-09 05:34:05&nbsp;UTC
					SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
					try {
						Date date = formatter.parse(stringDate);
						if (date != null){
							imageNode.setPropertyValue("timestamp",new Long(date.getTime()).toString());
							if (count++ > 100){
								MemoNode.flushDB();
								count=0;
							}
						} else {
							log.warning("Couldn't parse date:"+stringDate);
						}
					} catch (ParseException e){
						log.warning("Couldn't parse date:"+stringDate);						
					}
				}
			}
			reader.close();
			con.disconnect();
			
			count=0;
			String list="";
			for (String image: images){
				list+=image+";";
				if (count++>50){
				    queue.add(withUrl("/collector").param("imageUUIDs",list));
				    list="";
					count=0;
				}
			}
			if (list.length()>0){
				queue.add(withUrl("/collector").param("imageUUIDs",list));
				LandingPageServlet.quickServe="";
			}
			reader.close();
			con.disconnect();
			log.severe("Done:"+s_url+"?s="+(sol>=0?sol:0)+" found:"+images.size()+" new images");
			if (MemoNode.getRootNode().getChildByStringValue("newImagesFlag") == null) MemoNode.getRootNode().addChild(new MemoNode("newImagesFlag")).setPropertyValue("new", "false");
			if (images.size()>0) MemoNode.getRootNode().getChildByStringValue("newImagesFlag").setPropertyValue("new","true");
			return sol;
		} catch (Exception exp) {
			System.out.println("Ojh, this goes wrong:" + exp.getMessage());
			exp.printStackTrace();
		}
		System.out.println("No images found on:"+s_url);
		return sol;
	}
}
