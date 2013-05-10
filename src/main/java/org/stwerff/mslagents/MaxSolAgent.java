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

@ThreadSafe(true)
public class MaxSolAgent extends Agent {
	static final ObjectMapper om = new ObjectMapper();
	static final String NEWURL = "http://mars.jpl.nasa.gov/msl/admin/modules/multimedia/module/inc_ListImages_Raw.cfm";
	static final String MOBILEURL = "http://mars.jpl.nasa.gov/msl/multimedia/raw/";
	static final String SITEURL = "http://mars.jpl.nasa.gov/msl/multimedia/raw/";
	static final String BASEURL = SITEURL;

	public int getMaxSol() {
		int result = 0;

		ObjectNode params = om.createObjectNode();
		String statsUrl = "local://stats";
		try {
			result = send(statsUrl, "getMaxSol", params, Integer.class);
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		URL url;
		try {
			url = new URL(BASEURL + "?s=");

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

				MyMaxSolHandler handler = new MyMaxSolHandler();
				reader.setContentHandler(handler);
				reader.parse(new InputSource(input));

				if (handler.sol > result)
					result = handler.sol;
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

class MyMaxSolHandler extends DefaultHandler {

	public int sol = -1;

	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if ("div".equals(qName)) {
			String id = attributes.getValue("id");
			if (id != null) {
				try {
					Integer sol = new Integer(id);
					if (sol > this.sol) {
						this.sol = sol;
					}
				} catch (Exception e) {
				}
			}
		}
	}
};
