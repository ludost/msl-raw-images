package org.stwerff.mslraws.parser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import com.chap.memo.memoNodes.MemoNode;

public class SiteParser {
	private static final Logger log = Logger.getLogger("msl-raw-images");
	
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
		try {
			URL url = new URL(s_url+"?s="+sol);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					url.openStream()));
			String line;
			boolean found = false;

			MemoNode solNode = null;
			MemoNode imageNode = null;
			while ((line = reader.readLine()) != null) {
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
						imageNode=containerNode.addChild(new MemoNode(res)).
						setPropertyValue("type",getType(filename)).
						setPropertyValue("thumbnail",thumbnail);
						MemoNode.getRootNode().getChildByStringValue("newImagesFlag").setPropertyValue("new","true");
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
			return found;
		} catch (Exception exp) {
			System.out.println("Ojh, this goes wrong:" + exp.getMessage());
			exp.printStackTrace();
		}
		System.out.println("No images found on:"+s_url);
		return false;
	}
}
