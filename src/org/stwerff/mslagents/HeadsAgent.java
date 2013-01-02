package org.stwerff.mslagents;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.stwerff.mslagents.data.Image;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.annotation.ThreadSafe;
import com.almende.eve.json.annotation.Name;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@ThreadSafe(true)
public class HeadsAgent extends Agent {
	static final String jpl = "http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol";
	static final String msss = "http://mars.jpl.nasa.gov/msl-raw-images/msss";

	public static final int MAX_PER_RUN = 50;
	public static final ObjectMapper om = new ObjectMapper();

	public ArrayNode updateList(@Name("list") ArrayNode list){
		ArrayNode result = om.createArrayNode();
		try {
			for (int i=0; i<list.size(); i++){
				Image image = om.treeToValue(list.get(i),Image.class);
				if (image.getLastModified() == null){
					HeadReturn res = fetchHead(image.getUrl(),false);
					if (res != null){
						image.setFileTimeStamp(res.fileTimeStamp);
						image.setLastModified(res.lastModified);
						image.setUrl(res.checkedUrl);
						result.add(om.valueToTree(image));
					}
				}
				if (result.size()>= MAX_PER_RUN) break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public HeadReturn fetchHead(String s_url,boolean retry) throws IOException {
		s_url=s_url.replace("J$", jpl).replace("M$", msss);
		URL url = new URL(s_url);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setConnectTimeout(10000);
		con.setRequestMethod("HEAD");
		con.setUseCaches(false);
		con.setRequestProperty("Connection", "keep-alive");
		con.connect();

		if (con.getResponseCode() == 403) {
			if (retry){
				System.err.println("Warning, missing image:"+s_url);
				return null;
			}
			if (s_url.contains(".jpg")) {
				return fetchHead(s_url.replace(".jpg", ".JPG"),true);
			}
			if (s_url.contains(".JPG")) {
				return fetchHead(s_url.replace(".JPG", ".jpg"),true);
			}
		}
		if (con.getResponseCode() == 200) {
			if (con.getContentType().startsWith("image")) {
				con.disconnect();
				return new HeadReturn(s_url.replace(jpl, "J$").replace(msss, "M$"),new Long(con.getDate()).toString(),new Long(con.getLastModified()).toString());
			}
		}
		con.disconnect();
		return null;
	}

	
	@Override
	public String getDescription() {
		return "Adds head info to image list, keeps cache for efficiency";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

}

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
