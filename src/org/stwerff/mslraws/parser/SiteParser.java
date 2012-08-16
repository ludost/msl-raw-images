package org.stwerff.mslraws.parser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

import com.chap.memo.memoNodes.MemoNode;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import static com.google.appengine.api.taskqueue.TaskOptions.Builder.*;


public class SiteParser {
	private static final Logger log = Logger.getLogger("msl-raw-images");
	static Queue queue = QueueFactory.getDefaultQueue();
    
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
			if (filename.charAt(17) == 'D') return "downschaled";
			if (filename.charAt(17) == 'F') return "full";
			return "unknown";
		} else {
			if (filename.charAt(16) == 'I') return "thumbnail";
			if (filename.charAt(16) == 'E') return "full/subframe";
			return "unknown";
		}
	}
	
	public static boolean fetch(String s_url, int sol) {
		MemoNode baseNode = MemoNode.getRootNode().getChildByStringValue("msl-raw-images");
		MemoNode allImagesNode = baseNode.getChildByStringValue("allImages");
		if (allImagesNode == null) allImagesNode = baseNode.addChild(new MemoNode("allImages"));
		ArrayList<String> images = new ArrayList<String>();
		try {
			URL url = new URL(s_url+"?s="+sol);
			System.out.println("Now opening:"+s_url+"?s="+sol);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setConnectTimeout(10000);
			con.setReadTimeout(60000);

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					con.getInputStream()),50000);
			String line;
			boolean found = false;
			
			MemoNode solNode = null;
			MemoNode imageNode = null;
			while ((line = reader.readLine()) != null) {
				if (line.trim().equals("")) continue;
				if (line.contains("./?rawid=")) {
					String filename = line.split("./?rawid=")[1].split("&s")[0];
					String res = line.split("img src=\"")[1].split("\"")[0].replaceFirst("-thm","");
					String thumbnail = line.split("src=\"")[1].split("\"")[0];
					if (solNode == null) {
						solNode = baseNode.getChildByStringValue("sols").getChildByStringValue("sol"+sol);
						if (solNode == null) solNode = baseNode.getChildByStringValue("sols").addChild(new MemoNode("sol"+sol));
					}
					MemoNode camNode = baseNode.getChildByStringValue(getCamera(filename));
					if (camNode == null){
						System.out.println("Warning, can't find camera:"+getCamera(filename));
						continue;
					}
					MemoNode solcamNode = solNode.getChildByStringValue(getCamera(filename));
					if (solcamNode == null){
						solcamNode = solNode.addChild(new MemoNode(getCamera(filename)));
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
					if (containerNode.getChildByStringValue(res) == null){
						imageNode=containerNode.addChild(new MemoNode(res))
						.setPropertyValue("type",getType(filename))
						.setPropertyValue("thumbnail",thumbnail)
						.setParent(allImagesNode);
						images.add(imageNode.getId().toString());
					}
					found = true;
				}
				if (imageNode != null && line.contains("RawImageUTC")) {
					String stringDate = line.split("RawImageUTC\">")[1].split("</div>")[0];
					imageNode.setPropertyValue("stringDate", stringDate);
					//2012-08-09 05:34:05&nbsp;UTC
					SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss'&nbsp;'z");
					try {
						Date date = formatter.parse(stringDate);
						if (date != null){
							imageNode.setPropertyValue("timestamp",new Long(date.getTime()).toString());
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
			
			int count=0;
			String list="";
			for (String image: images){
				list+=image+";";
				if (count++>100){
				    queue.add(withUrl("/collector").param("imageUUIDs",list));
				    list="";
					count=0;
				}
			}
			if (count>0) queue.add(withUrl("/collector").param("imageUUIDs",list));
		
			System.out.println("Done:"+s_url+"?s="+sol);
			return found;
		} catch (Exception exp) {
			System.out.println("Ojh, this goes wrong:" + exp.getMessage());
			exp.printStackTrace();
		}
		System.out.println("No images found on:"+s_url);
		return false;
	}
}
