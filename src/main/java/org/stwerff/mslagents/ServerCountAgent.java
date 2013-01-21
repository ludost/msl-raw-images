package org.stwerff.mslagents;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.annotation.ThreadSafe;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.CharMatcher;

@ThreadSafe(true)
public class ServerCountAgent extends Agent {
	static final ObjectMapper om = new ObjectMapper();
	static final String NEWURL = "http://mars.jpl.nasa.gov/msl/admin/modules/multimedia/module/inc_ListImages_Raw.cfm";
	static final String MOBILEURL = "http://mars.jpl.nasa.gov/msl/multimedia/raw/";
	static final String SITEURL = "http://mars.jpl.nasa.gov/msl/multimedia/raw/";
	static final String BASEURL = SITEURL;

	public int getServerCount() {
		int result = 0;

		ObjectNode params = om.createObjectNode();
		String statsUrl = "http://localhost:8080/MSLAgents/agents/stats";
		try {
			result = send(statsUrl, "getMaxSol", params, Integer.class);
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		URL url;
		try {
			url = new URL(BASEURL);

			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			synchronized (con) {
				con.setRequestProperty("Cache-Control", "max-age=0");
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
				con.setRequestProperty(
						"Cookie",
						"__utma=259910805.1921224613.1343742021.1354541758.1354784789.8; __utmz=259910805.1354784789.8.6.utmccn=(referral)|utmcsr=unmannedspaceflight.com|utmcct=/index.php|utmcmd=referral; s_cc=true; gpv_pe5=MSL%20-%20Raw%20Images; s_vnum=1358840576579%26vn%3D10; s_invisit=true; s_sq=%5B%5BB%5D%5D; __utma=255717396.1026464737.1346745429.1356528763.1356533113.75; __utmb=255717396.1.10.1356533113; __utmc=255717396; __utmz=255717396.1354563682.65.12.utmcsr=unmannedspaceflight.com|utmccn=(referral)|utmcmd=referral|utmcct=/index.php; s_vi=[CS]v1|2822DA2A851D2C4B-40000136201A2467[CE]");
				con.setConnectTimeout(10000);
				con.setReadTimeout(40000);
				con.setUseCaches(true);
				con.connect();

				XMLReader reader = new Parser();
				reader.setFeature(Parser.namespacesFeature, false);
				reader.setFeature(Parser.namespacePrefixesFeature, false);

				InputStream input = con.getInputStream();
				if ("gzip".equals(con.getContentEncoding())) {
					input = new GZIPInputStream(input);
				}

				MyServerCount handler = new MyServerCount();
				reader.setContentHandler(handler);
				try {
					reader.parse(new InputSource(input));
				} catch (MySAXTerminatorException e) {
				}

				if (handler.count > result)
					result = handler.count;
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

class MySAXTerminatorException extends SAXException {
	private static final long serialVersionUID = -7819905689504395289L;
}

class MyServerCount extends DefaultHandler {

	public int count = -1;
	public String text = "";
	public boolean inDiv = false;

	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if ("div".equals(qName)) {
			String cl = attributes.getValue("class");
			if ("TotalRawImagesNote".equals(cl)) {
				inDiv = true;
			}
		}
	}

	public void endElement(String uri, String localName, String qName)
			throws SAXException, MySAXTerminatorException {
		if ("div".equals(qName) && inDiv) {
			CharMatcher ASCII_DIGITS = CharMatcher.inRange('0', '9')
					.precomputed();
			count = Integer.parseInt(ASCII_DIGITS.retainFrom(text));
			System.err.println("Setting to count: " + count);
			throw new MySAXTerminatorException();
		}
	}

	public void characters(char ch[], int start, int length)
			throws SAXException {
		if (inDiv) {
			text = new String(ch, start, length);
		}
	}
};
