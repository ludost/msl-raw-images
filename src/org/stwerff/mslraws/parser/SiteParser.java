package org.stwerff.mslraws.parser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import com.chap.memo.memoNodes.MemoNode;

public class SiteParser {

	public static String getCamera(String filename){
		String start = filename.substring(0, 3);
		if (start.matches("[0-9]+")){
			//Malin cam:
			return filename.substring(4,6);
		} else {
			return start;
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
			while ((line = reader.readLine()) != null) {
				if (line.contains("./?rawid=")) {
					String filename = line.split("./?rawid=")[1].split("&s")[0];
					String res = line.split("img src=\"")[1].split("(-thm)?.jpg")[0];
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
					if (containerNode.getChildByStringValue(res+".JPG") == null){
						containerNode.addChild(new MemoNode(res+".JPG"));
						MemoNode.getRootNode().getChildByStringValue("newImagesFlag").setPropertyValue("new","true");
					}
					found = true;
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
