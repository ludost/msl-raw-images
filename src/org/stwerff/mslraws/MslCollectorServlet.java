package org.stwerff.mslraws;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
import com.google.common.base.CharMatcher;

@SuppressWarnings("serial")
public class MslCollectorServlet extends HttpServlet {
	static URLFetchService fs = URLFetchServiceFactory.getURLFetchService();
	static Queue queue = QueueFactory.getDefaultQueue();
	private static final Logger log = Logger.getLogger("msl-raw-images");
	static MemcacheService memCache = MemcacheServiceFactory
			.getMemcacheService();;
	static final String MOBILEURL="http://mars.jpl.nasa.gov/msl/multimedia/raw/";
	static final String SITEURL="http://mars.jpl.nasa.gov/msl/multimedia/raw/";
	static String BASEURL = SITEURL;

	static final String jpl = "http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol";
	static final String msss = "http://mars.jpl.nasa.gov/msl-raw-images/msss";

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		doGet(req, resp);
	}

	public int fetchImageCount() throws IOException {
		int result = 0;
		URL url = new URL(BASEURL);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setConnectTimeout(10000);
		con.setReadTimeout(40000);
		con.setUseCaches(false);
		con.connect();
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				con.getInputStream()), 15000);
		String line;
		CharMatcher ASCII_DIGITS = CharMatcher.inRange('0', '9').precomputed();
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("<div class=\"TotalRawImagesNote\">")) {
				result = Integer.parseInt(ASCII_DIGITS.retainFrom(line
						.substring(37, 46)));
				break;
			}
		}
		reader.close();
		con.disconnect();
		return result;
	}

	public void fetchHead(MemoNode imageNode) throws IOException {
		// check head of image, store last-modified and date
		String s_url = imageNode.getStringValue();
		s_url=s_url.replace("J$", jpl).replace("M$", msss);
		if (!s_url.startsWith("http")){
			log.severe("invalid URL, no protocol:'"+s_url+"'");
			return;
		}
		URL url = new URL(s_url);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setConnectTimeout(10000);
		con.setRequestMethod("HEAD");
		con.setUseCaches(false);
		con.connect();

		if (con.getResponseCode() == 403) {
			if (s_url.contains(".jpg")) {
				imageNode.update(s_url.replace(".jpg", ".JPG"));
				fetchHead(imageNode);
				return;
			}
			if (s_url.contains(".JPG")) {
				imageNode.update(s_url.replace(".JPG", ".jpg"));
				fetchHead(imageNode);
				return;
			}
			log.severe("Warning: Image not found!");
			return;
		}
		if (con.getResponseCode() == 200) {
			if (con.getContentType().startsWith("image")) {
				imageNode.setPropertyValue("fileTimeStamp",
						new Long(con.getDate()).toString());
				imageNode.setPropertyValue("lastModified",
						new Long(con.getLastModified()).toString());
			} else {
				log.severe("Image URL not valid:'"
						+ imageNode.getStringValue()+"'");
			}
		}
		con.disconnect();
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		resp.setContentType("text/plain");
		InitListener.initData();
		int damper=0;

		if (req.getParameter("site") != null) BASEURL=SITEURL;
		if (req.getParameter("mobile") != null) BASEURL=MOBILEURL;
		if (req.getParameter("damper") != null) damper=10;
		if (req.getParameter("passOn") != null) {
			if (req.getParameter("doHeads") != null) {
				queue.add(withUrl("/collector").param("doHeads", "true"));
			} else {
				queue.add(withUrl("/collector").param("damper", new Integer(damper).toString()));
			}
			return;
		}
		String images = req.getParameter("imageUUIDs");
		if (images != null) {
			log.warning("checking head:" + images);
			for (String image : images.split(";")) {
				MemoNode imageNode = new MemoNode(new UUID(image));
				if (imageNode != null) {
					fetchHead(imageNode);
				}
			}
			memCache.delete("quickServe");
		} else if (req.getParameter("doHeads") != null) {
			MemoNode baseNode = MemoNode.getRootNode().getChildByStringValue(
					"msl-raw-images");
			MemoNode allImagesNode = baseNode
					.getChildByStringValue("allImages");
			if (allImagesNode == null) {
				allImagesNode = baseNode.addChild(new MemoNode("allImages"));
			}
			log.warning("checking for images without head data.");
			ArrayList<MemoNode> all = allImagesNode.getChildren();
			int count = 0;
			for (MemoNode image : all) {
				if (image.getPropertyValue("fileTimeStamp").equals("")) {
					fetchHead(image);
					if (count++ > 10) {
						log.severe("Flushing DB after 100 heads fixed.");
						MemoNode.flushDB();
						count = 0;
					}
				}
			}
			if (all.size() > 0) {
				memCache.delete("quickServe");
			}
		} else if (req.getParameter("sol") != null) {
			String total="";
			if ((total = req.getParameter("total")) != null){
				MemoNode baseNode = MemoNode.getRootNode().getChildByStringValue(
						"msl-raw-images");
				MemoNode allImagesNode = baseNode.getChildByStringValue("allImages");
				if (allImagesNode != null){
					int count= Integer.parseInt(total);
					if (allImagesNode.getChildren().size()>=count){
						log.warning("Skipping sol:"+req.getParameter("sol")+" because total is reached!"+allImagesNode.getChildren().size()+"/"+total);
						return;
					}
				}
			}
			SiteParser.fetch(BASEURL,
					Integer.parseInt(req.getParameter("sol")));
		} else {
			int count = fetchImageCount();
			MemoNode baseNode = MemoNode.getRootNode().getChildByStringValue(
					"msl-raw-images");
			MemoNode allImagesNode = baseNode.getChildByStringValue("allImages");
			boolean doReload = true;
			if (allImagesNode != null) {
				try { 
				if (count-damper <= allImagesNode.getChildren().size())
					doReload = false;
				} catch (Exception e){
					log.severe("Warning:"+e.getLocalizedMessage());
				}
			}
			if (doReload) {
				log.warning("Collecting images!");
				int sol = SiteParser.fetch(
						BASEURL, -1);
				sol--;
				while (sol >= 0) {
					queue.add(withUrl("/collector").param("sol",
							Integer.toString(sol--)).param("total", new Integer(count-damper).toString()));
				}
			} else {
				log.warning("Skipping collection, nothing new.");
			}

		}
		MemoNode.flushDB();
	}
}
