package org.stwerff.mslagents;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.stwerff.mslagents.data.Image;

import com.almende.eve.agent.Agent;
import com.almende.eve.json.annotation.Name;
import com.almende.eve.json.annotation.Required;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class BearingSpiceAgent extends Agent {
	public static final ObjectMapper om = new ObjectMapper();
	
	public boolean filter(DateTime time){
		if (time.isBefore(new DateTime("2012-08-06T05:18:40Z"))) return true;
		if (time.isAfter(new DateTime("2012-08-08T04:34:41Z")) && time.isBefore(new DateTime("2012-08-08T04:45:02Z"))) return true;
		if (time.isAfter(new DateTime("2012-09-20T07:56:16Z")) && time.isBefore(new DateTime("2012-09-20T08:07:11Z"))) return true;
		if (time.isAfter(new DateTime("2012-09-20T08:08:33Z")) && time.isBefore(new DateTime("2012-09-20T08:14:40Z"))) return true;
		return false;
	}
	
	public ArrayNode updateList(@Name("list") ArrayNode list, @Name("reload") @Required(false) Boolean reload){
		ArrayNode result = om.createArrayNode();
		if (reload == null) reload=false;
		try {
			String path = getAgentFactory().getConfig().get("environment",getAgentFactory().getEnvironment(),"base_path");
			path+="c_bin/";
			File pathFile = new File(path);
			File test = new File(path+"msl_pointing");
			if (!test.canExecute()){
				Runtime.getRuntime().exec("chmod +x msl_pointing",null,pathFile);
			}
			Map<String,ArrayList<Image>> todo = new HashMap<String,ArrayList<Image>>();
			for (int i=0; i<list.size(); i++){
				Image image = om.treeToValue(list.get(i),Image.class);
				if (reload || image.getBearing() == null){
					ArrayList<Image> camTodo = todo.get(image.getCamera()); 
					if (camTodo == null){
						camTodo = new ArrayList<Image>();
					}
					camTodo.add(image);
					todo.put(image.getCamera(),camTodo);
				}
			}
			if (todo.size()==0) return result;
			String cmd_line="";
			for (Entry<String,ArrayList<Image>> entry : todo.entrySet()){
				int count=0;
				ArrayList<Image> camTodo = new ArrayList<Image>(entry.getValue().size());
				for (Image image : entry.getValue()){
					DateTime time = new DateTime(image.getUnixTimeStamp()).toDateTime(DateTimeZone.UTC);
					if (filter(time)){
						image.setBearing("---");
						result.add(om.valueToTree(image));
						continue;
					}
					cmd_line+=" "+time.toString(ISODateTimeFormat.dateHourMinuteSecond());
					camTodo.add(image);
					if (count++>500) break;
				}
				//System.err.println("Doing: ./msl_pointing "+entry.getKey()+cmd_line);
				Process proc = Runtime.getRuntime().exec("./msl_pointing "+entry.getKey()+cmd_line, null,pathFile);
				BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				for (int i=0; i<camTodo.size(); i++){
					String lmst = reader.readLine();
					if (lmst == null || "".equals(lmst.trim())){
						System.err.println("Empty result?"+i+" from:"+entry.getKey()+cmd_line);
						continue;
					}
					String[] fields = lmst.split(":");
					Image image = entry.getValue().get(new Integer(fields[0]));
					
					if (new Integer(fields[0]) != i){
						System.err.println("MSL_POINTING tool didn't return the all records? "+i+"/'"+fields[0]+"'");
						break;
					}
					image.setBearing(fields[2]);
					result.add(om.valueToTree(image));
				}
				proc.destroy();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	@Override
	public String getDescription() {
		return "Converts image time to LMST time";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

}
