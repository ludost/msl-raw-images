package org.stwerff.mslraws;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.stwerff.mslraws.images.InitListener;
import org.stwerff.mslraws.parser.SiteParser;

import com.chap.memo.memoNodes.MemoNode;
import com.eaio.uuid.UUID;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;

@SuppressWarnings("serial")
public class MslCollectorServlet extends HttpServlet {
	static URLFetchService fs = URLFetchServiceFactory.getURLFetchService();
	static Queue queue = QueueFactory.getDefaultQueue();
	private static final Logger log = Logger.getLogger("msl-raw-images");
	static MemcacheService memCache = MemcacheServiceFactory.getMemcacheService();;
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		doGet(req, resp);
	}

	public void fetchHead(MemoNode imageNode) throws IOException{
		// check head of image, store last-modified and date
		String s_url = imageNode.getStringValue();
		if (!s_url.startsWith("http")) return;
		URL url = new URL(s_url);
		HttpURLConnection con = (HttpURLConnection) url
				.openConnection();
		con.setConnectTimeout(10000);
		con.setRequestMethod("HEAD");
		con.setUseCaches(false);
		con.connect();

		if (con.getResponseCode() == 403){
			if (s_url.contains(".jpg")){
				imageNode.update(s_url.replace(".jpg", ".JPG"));
				fetchHead(imageNode);
				return;
			}
			if (s_url.contains(".JPG")){
				imageNode.update(s_url.replace(".JPG", ".jpg"));
				fetchHead(imageNode);
				return;
			}
			System.err.println("Warning: Image not found!");
			return;
		}
		if (con.getResponseCode() == 200) {
			if (con.getContentType().startsWith("image")) {
				imageNode.setPropertyValue("fileTimeStamp",
						new Long(con.getDate()).toString());
				imageNode.setPropertyValue("lastModified",
						new Long(con.getLastModified()).toString());
			} else {
				System.err.println("Image URL not valid:"+imageNode.getStringValue());
			}
		}
		con.disconnect();
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		
		resp.setContentType("text/plain");
		InitListener.initDemoData();

		if (req.getParameter("passOn") != null){
			if (req.getParameter("doHeads") != null){
				queue.add(withUrl("/collector").param("doHeads","true"));				
			} else {
				queue.add(withUrl("/collector"));
			}
			return;
		}
		String images = req.getParameter("imageUUIDs");
		if (images != null) {
			System.out.println("checking head:" + images);
			for (String image : images.split(";")) {
				MemoNode imageNode = new MemoNode(new UUID(image));
				if (imageNode != null) {
					fetchHead(imageNode);
				}
			}
			memCache.delete("quickServe");
		} else if (req.getParameter("doHeads") != null){
			MemoNode baseNode = MemoNode.getRootNode().getChildByStringValue("msl-raw-images");
			MemoNode allImagesNode = baseNode.getChildByStringValue("allImages");
			if (allImagesNode == null){
				allImagesNode = baseNode.addChild(new MemoNode("allImages"));
			}
			System.out.println("checking for images without head data.");
			ArrayList<MemoNode> all = allImagesNode.getChildren();
			int count=0;
			for (MemoNode image: all){
				if (image.getPropertyValue("fileTimeStamp").equals("")){
					fetchHead(image);
					if (count++ >10){
						log.severe("Flushing DB after 10 heads fixed.");
						MemoNode.flushDB();
						count=0;
					}
				}
			}
			if (all.size()>0){
				memCache.delete("quickServe");
			}
		} else if (req.getParameter("sol") != null){
			System.out.println("Collecting images!");
			SiteParser.fetch(
					"http://mars.jpl.nasa.gov/msl/multimedia/raw/",Integer.parseInt(req.getParameter("sol")));
		} else {
			System.out.println("Collecting images!");
			int sol = SiteParser.fetch("http://mars.jpl.nasa.gov/msl/multimedia/raw/", -1);
			sol--;
			while (sol>=0){
				queue.add(withUrl("/collector").param("sol",Integer.toString(sol--)));
			}
		}
		MemoNode.flushDB();
	}
}
