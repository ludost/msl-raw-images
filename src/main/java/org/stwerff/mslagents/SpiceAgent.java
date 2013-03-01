package org.stwerff.mslagents;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.stwerff.mslagents.data.Image;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentFactory;
import com.almende.eve.agent.annotation.Name;
import com.almende.eve.agent.annotation.Required;
import com.almende.eve.agent.annotation.ThreadSafe;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@ThreadSafe(true)
public class SpiceAgent extends Agent {
	public static final ObjectMapper om = new ObjectMapper();
	
	public ArrayNode updateList(@Name("list") ArrayNode list, @Name("reload") @Required(false) Boolean reload){
		ArrayNode result = om.createArrayNode();
		if (reload == null) reload=false;
		try {
			String path = getAgentFactory().getConfig().get("environment",AgentFactory.getEnvironment(),"base_path");
			path+="c_bin/";
			File pathFile = new File(path);
			File test = new File(path+"msl_lmst");
			if (!test.canExecute()){
				Runtime.getRuntime().exec("chmod +x msl_lmst",null,pathFile);
			}
			ArrayList<Image> todo = new ArrayList<Image>(list.size());
			
			for (int i=0; i<list.size(); i++){
				Image image = om.treeToValue(list.get(i),Image.class);
				if (reload || image.getLmst() == null){
					todo.add(image);
				}
			}
			if (todo.size()==0) return result;
			String cmd_line="";
			for (Image image : todo){
				DateTime time = new DateTime(image.getUnixTimeStamp()).toDateTime(DateTimeZone.UTC);
				cmd_line+=" "+time.toString(ISODateTimeFormat.dateHourMinuteSecond());
			}
			Process proc = Runtime.getRuntime().exec("./msl_lmst"+cmd_line, null,pathFile);
			BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			
			for (int i=0; i<todo.size(); i++){
				String lmst = reader.readLine();
				String[] fields = lmst.split(":");
				Image image = todo.get(new Integer(fields[0]));
				
				if (new Integer(fields[0]) != i){
					System.err.println("MSL_LMST tools didn't return the all records? "+i+"/'"+fields[0]+"'");
					break;
				}
				
				String solString = String.format("%05d", image.getSol());
				if (("1/"+solString).equals(fields[1])){
					image.setLmst(fields[2]+":"+fields[3]+":"+fields[4]);	
				} else {
					System.err.println("Failed to convert to correct LMST, wrong sol?");
				}
				result.add(om.valueToTree(image));
			}
			proc.destroy();

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
