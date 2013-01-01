package org.stwerff.mslagents;

import java.util.ArrayList;
import java.util.List;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentFactory;
import com.almende.eve.json.JSONRequest;
import com.almende.eve.json.annotation.Name;
import com.almende.eve.json.annotation.Required;
import com.almende.eve.json.jackson.JOM;
import com.almende.eve.service.AsyncCallback;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ClockAgent extends Agent {

	public void updateSols(@Name("reload") @Required(false) Boolean reload) {
		if (reload == null) reload=false;
		try {
			
			System.err.println("Running check of sols!");
			AgentFactory factory = getAgentFactory();
			
			ObjectNode params = JOM.createObjectNode();
			String url = "http://localhost:8080/MSLAgents/agents/max";
			String method = "getMaxSol";
			Integer maxSol = send(url,method,params,Integer.class);
			
			url="http://localhost:8080/MSLAgents/agents/count";
			method = "getServerCount";
			Integer serverCount = send(url,method,params,Integer.class);
			
			url="http://localhost:8080/MSLAgents/agents/stats";
			method = "getTotalCount";
			
			int sol = maxSol;
			while ((reload || send(url,method,params,Integer.class) < serverCount) && sol>=0){
				System.err.println("Checking sol"+sol+"!");
				
				if (!factory.hasAgent("sol_"+sol)){
					factory.createAgent(
							org.stwerff.mslagents.SolAgent.class, "sol_" + sol);
				}
				String inner_url = "http://localhost:8080/MSLAgents/agents/sol_"+sol;
				String inner_method = "updateList";
				ObjectNode inner_params = JOM.createObjectNode();
				inner_params.put("reload",false);
				send(inner_url, inner_method, inner_params, ArrayNode.class);
				sol--;
			}
			System.err.println("Done running check of sols!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		ObjectNode params = JOM.createObjectNode();
		JSONRequest request = new JSONRequest("updateSols", params);
		long delay = 300000; // milliseconds

		getScheduler().createTask(request, delay);
	}

	@SuppressWarnings("unchecked")
	public void updateHeads(){
		String url = "http://localhost:8080/MSLAgents/agents/stats";
		String method = "getIncompleteSols";
		ObjectNode params = JOM.createObjectNode();
		params.put("type", "heads");
		TotalRet total = new TotalRet();
		try {
			List<Integer> sols = (List<Integer>) send(url, method, params,List.class);
			total.total = Math.min(10,sols.size());
			int totalCnt=total.total;
			for (Integer sol : sols){
				if (totalCnt--<=0) break;
				System.err.println("Doing heads on sol:"+sol);
				url = "http://localhost:8080/MSLAgents/agents/sol_"+sol;
				method = "updateHeads";
				sendAsync(url,method,JOM.createObjectNode(),new AsyncCallback<Object>() {
					private int sol=-1;
					private TotalRet total;
				    @Override
				    public void onSuccess(Object result) {
				        System.out.println("done:"+sol);
				        total.decr();
				    }

					@Override
					public void onFailure(Exception exception) {
				        System.out.println("failed sol:"+sol);
				        total.decr();
					}
					private AsyncCallback<Object> init(int var,TotalRet var2){
				        sol = var;
				        total = var2;
				        return this;
				    }
				}.init(sol,total),Object.class);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
		int count=15;
		while (total.total > 0 && count>0){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
			count--;
		}
		if (count<= 0) System.err.println("Warning: "+total.total+" async calls weren't finished within 15 seconds!");
		params = JOM.createObjectNode();
		JSONRequest request = new JSONRequest("updateHeads", params);
		long delay = 60000; // milliseconds

		getScheduler().createTask(request, delay);
	}

	@SuppressWarnings("unchecked")
	public void updateSpice(@Name("reload") @Required(false) Boolean reload){
		if(reload==null)reload=false;
		String url = "http://localhost:8080/MSLAgents/agents/stats";
		String method = "getIncompleteSols";
		ObjectNode params = JOM.createObjectNode();
		params.put("type", "spice");
		TotalRet total = new TotalRet();
		try {
			List<Integer> sols;
			sols = (List<Integer>) send(url, method, params,List.class);
			total.total = Math.min(10,sols.size());
			if (reload){
				url = "http://localhost:8080/MSLAgents/agents/max";
				method = "getMaxSol";
				params = JOM.createObjectNode();
				Integer maxSol = send(url,method,params,Integer.class);
				sols = new ArrayList<Integer>();
				for (int i=0; i<=maxSol; i++){
					sols.add(i);
				}
				total.total=sols.size();
			}
			
			int totalCnt = total.total;
			for (Integer sol : sols){
				if (totalCnt--<=0) break;
				System.err.println("Doing spice on sol:"+sol);
				url = "http://localhost:8080/MSLAgents/agents/sol_"+sol;
				method = "updateSpice";
				params = JOM.createObjectNode();
				params.put("reload", reload);
				sendAsync(url,method,params,new AsyncCallback<Object>() {
					private int sol=-1;
					private TotalRet total;
				    @Override
				    public void onSuccess(Object result) {
				        System.out.println("done:"+sol);
				        total.decr();
				    }

					@Override
					public void onFailure(Exception exception) {
				        System.out.println("failed sol:"+sol);
				        total.decr();
					}
					private AsyncCallback<Object> init(int var,TotalRet var2){
				        sol = var;
				        total = var2;
				        return this;
				    }
				}.init(sol,total),Object.class);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
		int count=15;
		while (total.total > 0 && count>0){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
			count--;
		}
		if (count<= 0) System.err.println("Warning: "+total.total+" async calls weren't finished within 15 seconds!");
		params = JOM.createObjectNode();
		JSONRequest request = new JSONRequest("updateSpice", params);
		long delay = 60000; // milliseconds

		getScheduler().createTask(request, delay);
	}
	
	private class TotalRet {
		int total=-1;
		public void decr(){
			synchronized(this){
				total--;
			}
		}
	}
	
	@Override
	public String getDescription() {
		return "Maintenance agent, checking and starting sol monitoring";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

}
