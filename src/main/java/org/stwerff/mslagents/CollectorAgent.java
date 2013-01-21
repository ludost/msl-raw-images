package org.stwerff.mslagents;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.GZIPInputStream;

import org.ccil.cowan.tagsoup.Parser;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.stwerff.mslagents.data.Image;
import org.stwerff.mslagents.data.ImageList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.annotation.Name;
import com.almende.eve.agent.annotation.ThreadSafe;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@ThreadSafe(true)
public class CollectorAgent extends Agent {
	static final ObjectMapper om = new ObjectMapper();
	static final String NEWURL = "http://mars.jpl.nasa.gov/msl/admin/modules/multimedia/module/inc_ListImages_Raw.cfm";
	static final String MOBILEURL = "http://mars.jpl.nasa.gov/msl/multimedia/raw/";
	static final String SITEURL = "http://mars.jpl.nasa.gov/msl/multimedia/raw/";
	static final String BASEURL = NEWURL;

	public ArrayNode getSol(@Name("sol") int sol) {
		ArrayNode result = _getSol(sol, true);
		result = ImageList.merge(result, _getSol(sol, false));
		return result;
	}

	public ArrayNode _getSol(int sol, boolean camera) {
		ArrayNode result = om.createArrayNode();
		URL url;
		try {
			url = new URL(BASEURL + "?" + (new Random().nextInt())
					+ (camera ? "&camera=" : "") + "&s=" + sol);

			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			synchronized (con) {
				con.setRequestProperty("Cache-Control", "no-cache");
				con.setRequestProperty("Pragma", "no-cache");
				con.setRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
				con.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
				con.setRequestProperty("Accept-Charset",
						"ISO-8859-1,utf-8;q-0.7,*;q=0.3");
				con.setRequestProperty("Accept",
						"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
				con.setRequestProperty("Connection", "keep-alive");
				con.setRequestProperty("Host", "mars.jpl.nasa.gov");
				con.setRequestProperty(
						"User-Agent",
						"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.97 Safari/537.11");
				con.setConnectTimeout(10000);
				con.setReadTimeout(40000);
				con.setUseCaches(false);
				con.connect();

				XMLReader reader = new Parser();
				reader.setFeature(Parser.namespacesFeature, false);
				reader.setFeature(Parser.namespacePrefixesFeature, false);

				InputStream input = con.getInputStream();
				if ("gzip".equals(con.getContentEncoding())) {
					input = new GZIPInputStream(input);
				}

				MyHandler handler = new MyHandler(sol);
				reader.setContentHandler(handler);
				reader.parse(new InputSource(input));

				sol = handler.sol;

				if (handler.getImageCount() > 0) {
					result = om.valueToTree(handler.getResult());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public String getDescription() {
		return "Parser agent for MSL website website";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}
}

class MyHandler extends DefaultHandler {
	static final String jpl = "http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol";
	static final String msss = "http://mars.jpl.nasa.gov/msl-raw-images/msss";
	// private static final Logger log = Logger.getLogger("msl-raw-images");

	public int sol;

	public MyHandler(int sol) {
		this.sol = sol;
	}

	ArrayList<Image> images = new ArrayList<Image>(100);

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

	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (current != null && "a".equals(qName)) {
			images.add(current);
		}
		if ("a".equals(qName)) {
			String name = attributes.getValue("href");
			if (name != null && name.startsWith("./?rawid=")) {
				current = new Image();
				name = name.substring(9, name.indexOf("&s"));
				current.setName(name);
				current.setCamera(getCamera(name));
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
				DateTimeFormatter fmt = DateTimeFormat.forPattern(
						"yyyy-MM-dd HH:mm:ss").withZoneUTC();
				try {
					DateTime date = fmt.parseDateTime(dateString.substring(0,
							19));
					if (date != null) {
						current.setUnixTimeStamp(date.getMillis());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		if ("body".equals(qName)) {
			if (current != null)
				images.add(current);
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
