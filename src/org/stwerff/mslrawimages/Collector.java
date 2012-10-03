package org.stwerff.mslrawimages;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.common.base.CharMatcher;

public class Collector extends HttpServlet {
	static final String jpl = "http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol";
	static final String msss = "http://mars.jpl.nasa.gov/msl-raw-images/msss";
	private static final long serialVersionUID = 1453453545543354L;
	private static final Logger log = Logger.getLogger("msl-raw-images");
	private static MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
	public static TwitterFactory tf=null;
	
	static Queue queue = QueueFactory.getDefaultQueue();

	static final String MOBILEURL = "http://mars.jpl.nasa.gov/msl/multimedia/raw/";
	static final String SITEURL = "http://mars.jpl.nasa.gov/msl/multimedia/raw/";
	static String BASEURL = SITEURL;

	public Collector(){
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
		  .setOAuthConsumerKey("zqabV9xmGSxx3EFWUBDdA")
		  .setOAuthConsumerSecret("HMNudmnopG1xsoHebr3VKdwFyPErQUkS7XM6dJYj1kE")
		  .setOAuthAccessToken("784328444-PdhkGZ25pA1lz2atQHFJHfeiPB45IGLGw1fW3m9d")
		  .setOAuthAccessTokenSecret("oUxXVYqWtDqrufP4jLkaJPcT6UsNdwFemEBBu5llvU");
		tf = new TwitterFactory(cb.build());
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse res) {
		handle(req, res);
	}

	public void doGet(HttpServletRequest req, HttpServletResponse res) {
		handle(req, res);
	}

	public void handle(HttpServletRequest req, HttpServletResponse res) {
		URL url;
		try {
			int sol = -1;
			int imageCount=0;
			try {
				sol = Integer.parseInt(req.getParameter("sol"));
			} catch (Exception e) {
			}
			
			url = new URL(BASEURL + "?s=" + (sol >= 0 ? sol : ""));
//			System.out.println("Opening:" + url.toString());

			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setConnectTimeout(10000);
			con.setReadTimeout(40000);
			con.setUseCaches(false);
			con.connect();

			XMLReader reader = new Parser();
			reader.setFeature(Parser.namespacesFeature, false);
			reader.setFeature(Parser.namespacePrefixesFeature, false);
			
			
			MyHandler handler = new MyHandler(sol);
			reader.setContentHandler(handler);

			reader.parse(new InputSource(con.getInputStream()));
			res.setContentType("text/plain");

			if (sol == -1) {
				StorageIntf store = new GoogleCloudStorage();
				Writer output = store.getWriter("MaxSol", -1);
				output.append("{\"maxsol\":" + handler.sol + "}");
				store.finalize();

				sol = handler.sol;
				syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
				for (int i=0; i<sol+1; i++){
					int count=0;
					try {
						count = (Integer)syncCache.get("sol_cnt_"+i);
					} catch (Exception e){
//						log.warning("Couldn't find sol cnt: sol_cnt_"+i);
					};
					imageCount+=count;
				}
				int serverCount = fetchImageCount();
//				log.warning("Comparing:"+imageCount+"/"+serverCount);
				if (imageCount == serverCount){
					log.warning("No need to run collector, total count reached:"+imageCount);
					res.getWriter().println("No need to run collector, total count reached:"+imageCount);
					return;
				}
				sol--;
				while (sol >= 0) {
					queue.add(withUrl("/collector").param("sol",
							Integer.toString(sol--)));
				}
			}
			if (handler.getImageCount() > 0) {
				int oldImageCount = fetchImageCount("http://storage.googleapis.com/msl-raw-images/sol_"+handler.sol+".json");
				res.getWriter().println(
						"Collection " + handler.sol + " done: ("+oldImageCount+"/"
								+ handler.getImageCount() + " images)");

				if (handler.getImageCount()>oldImageCount){
					StringWriter resultWriter = new StringWriter();
					JsonFactory f = new JsonFactory();
					f.setCodec(new ObjectMapper());
					JsonGenerator g = f.createJsonGenerator(resultWriter);
					g.writeStartArray();
					
					for (Image current : handler.getResult()){
						HeadReturn ret = fetchHead(current.getUrl());
						if (ret != null){
							current.setUrl(ret.checkedUrl);
							current.setLastModified(ret.lastModified);
							current.setFileTimeStamp(ret.fileTimeStamp);
						}
						g.writeObject(current);
					}
					g.writeEndArray();
					g.flush();
					
					String json = resultWriter.toString();
//					res.getWriter().append(json);
					
					StorageIntf store = new GoogleCloudStorage();
					Writer output = store.getWriter("sol_" + handler.sol + ".json",
						handler.getImageCount());
					if (output != null) {
						output.append(json);
						output.flush();
					}
					store.finalize();
					
					int count=0;
					try {
						count = (Integer)syncCache.get("sol_cnt_"+handler.sol);
					} catch (Exception e){};
					if (handler.getImageCount()>count){
						String tweet = "Found "+(count>0?(handler.getImageCount()-count)+" ":"")+"new images for sol "+handler.sol+"."
						+" Check: http://msl-raw-images.appspot.com/";
						try {
							Twitter twitter = tf.getInstance();
							twitter.updateStatus(tweet);
						} catch (TwitterException e) {
							e.printStackTrace();
							log.severe("Twitter error!"+e.getLocalizedMessage());
						}
					}
					Landing.quickServe="";
				} else {
					log.warning("Skipping sol:"+sol+" no new images: "+oldImageCount+"/"+handler.getImageCount());
					try {
						res.getWriter().println("No new images found!");
					} catch (Exception e1) {}
				}
				syncCache.put("sol_cnt_"+handler.sol,handler.getImageCount());
			}
		} catch (Exception e) {
			try {
				res.sendError(500, "Failed:"+e.getLocalizedMessage());
			} catch (IOException e1) {
				log.severe("failed to send error:"+e.getLocalizedMessage());
			}
			log.severe("failed to collect:" + e.getLocalizedMessage());
		}
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
	
	public int fetchImageCount(String s_url) {
		try {
			URL url = new URL(s_url);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setConnectTimeout(10000);
			con.setRequestMethod("HEAD");
			con.setUseCaches(false);
			con.connect();
			if (con.getResponseCode() == 200) {
				return Integer.parseInt(con
						.getHeaderField("x-goog-meta-imageCount"));
			}
		} catch (Exception e) {
			log.severe("Failed to get imageCount from:" + s_url + " -> "
					+ e.getLocalizedMessage());
		}
		return -1;
	}

	public HeadReturn fetchHead(String s_url) throws IOException {
		s_url=s_url.replace("J$", jpl).replace("M$", msss);
		URL url = new URL(s_url);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setConnectTimeout(10000);
		con.setRequestMethod("HEAD");
		con.setUseCaches(false);
		con.connect();

		if (con.getResponseCode() == 403) {
			if (s_url.contains(".jpg")) {
				con.disconnect();
				return fetchHead(s_url.replace(".jpg", ".JPG"));
			}
			if (s_url.contains(".JPG")) {
				con.disconnect();
				return fetchHead(s_url.replace(".JPG", ".jpg"));
			}
			log.severe("Warning: Image not found!");
		}
		if (con.getResponseCode() == 200) {
			if (con.getContentType().startsWith("image")) {
				con.disconnect();
				return new HeadReturn(s_url.replace(jpl, "J$").replace(msss, "M$"),new Long(con.getDate()).toString(),new Long(con.getLastModified()).toString());
			} else {
				log.severe("Image URL not valid:'"
						+ s_url+"'");
			}
		}
		con.disconnect();
		return null;
	}
}

class MyHandler extends DefaultHandler {
	static final String jpl = "http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol";
	static final String msss = "http://mars.jpl.nasa.gov/msl-raw-images/msss";

	public int sol;
	public MyHandler(int sol){
		this.sol=sol;
	}
	ArrayList<Image> images = new ArrayList<Image>(100);
	public int fullCount=0;
	public int dsCount=0;
	public int subframeCount=0;
	public int thumbCount=0;

	Image current = null;
	boolean doDate = false;
	String dateString = "";

	public int getImageCount() {
		return this.images.size();
	}

	public static String getCamera(String filename) {
		String start = filename.substring(0, 3);
		if (start.matches("[0-9]+")) {
			// Malin cam:
			return filename.substring(4, 6);
		} else {
			return start;
		}
	}

	public String getType(String filename) {
		try {
			char cmp =filename.charAt(16);
			if (cmp == '_') {
				cmp =filename.charAt(17);
			}
			if (cmp == 'U' || cmp == 'I' || cmp == 'T' || cmp == 'Q'){
				thumbCount++;
				return "thumbnail";
			}
			if (cmp == 'D'){
				dsCount++;
				return "downscaled";
			}
			if (cmp == 'C' || cmp == 'R' || cmp == 'S'){
				subframeCount++;
				return "subframe";
			}
			if (cmp == 'F' || cmp == 'B' || cmp == 'E' || cmp == 'K'){
				fullCount++;
				return "full";
			}
		} catch (Exception e) {
			System.out.println("Strange filename found:'" + filename + "'");
		}
		return "unknown";
	}

	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (current != null && "a".equals(qName)) {
			images.add(current);
		}
		if ("a".equals(qName)) {
			String name = attributes.getValue("href");
			if (this.sol == -1 && name != null && name.startsWith("./?s=")) {
				if ("font-weight:bold;color:#122744;".equals(attributes
						.getValue("style"))) {
					System.out.println("Found sol:" + name.substring(5));
					this.sol = Integer.parseInt(name.substring(5));
				}
			}
			if (name != null && name.startsWith("./?rawid=")) {
				current = new Image();
				name = name.substring(9, name.indexOf("&s"));
				current.setName(name);
				current.setCamera(getCamera(name));
				current.setType(getType(name));
				current.setSol(sol);
				doDate = false;
				dateString = "";
			} else {
				current = null;
			}
		}
		if (current != null && "img".equals(qName)) {
			current.setThumbnailUrl(attributes.getValue("src")
					.replace(jpl, "J$").replace(msss, "M$"));
			String url = current.getThumbnailUrl().replaceFirst("-thm", "");
			if (current.getCamera().length() > 2 && url.endsWith(".jpg"))
				url = url.replace(".jpg", ".JPG");
			current.setUrl(url);
		}
		if (current != null && "div".equals(qName)) {
			if ("RawImageUTC".equals(attributes.getValue("class"))) {
				doDate = true;
				dateString = "";
			}
		}
	}

	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (doDate && "div".equals(qName)) {
			doDate = false;
			if (current != null && dateString.length() > 0) {
				SimpleDateFormat formatter = new SimpleDateFormat(
						"yyyy-MM-dd HH:mm:ss");
				try {
					formatter.setLenient(true);
					formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
					Date date = formatter.parse(dateString.substring(0, 19));
					if (date != null) {
						current.setUnixTimeStamp(date.getTime());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		if ("body".equals(qName)) {
			if (current != null) images.add(current);
		}
	}

	public void characters(char ch[], int start, int length)
			throws SAXException {
		if (current != null && doDate) {
			dateString += new String(ch, start, length);
		}
	}

	public List<Image> getResult() {
		return images;
	}

};

class HeadReturn {
	public String checkedUrl="";
	public String fileTimeStamp="";
	public String lastModified="";
	public HeadReturn(String url,String fileTS,String lm){
		checkedUrl=url;
		fileTimeStamp=fileTS;
		lastModified=lm;
	}
}
