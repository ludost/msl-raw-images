package org.stwerff.mslagents;

import java.util.ArrayList;
import java.util.List;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentFactory;
import com.almende.eve.agent.annotation.Name;
import com.almende.eve.agent.annotation.Required;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.transport.AsyncCallback;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ClockAgent extends Agent {

	public void updateSols(@Name("reload") @Required(false) Boolean reload) {
		System.err.println("Running check of sols!");
		if (reload == null) reload=false;
		try {
			AgentFactory factory = getAgentFactory();
			
			ObjectNode params = JOM.createObjectNode();
			String url = "local://max";
			String method = "getMaxSol";
			Integer maxSol = send(url,method,params,Integer.class);
			
			url="local://count";
			method = "getServerCount";
			Integer serverCount = send(url,method,params,Integer.class);
			
			url="local://stats";
			method = "getTotalCount";
			
			System.err.println("Starting run, max sol:"+maxSol);
			int sol = maxSol;
			while ((reload || send(url,method,params,Integer.class) < serverCount) && sol>=0){
				System.err.println("Checking sol"+sol+"!");
				
				if (!factory.hasAgent("sol_"+sol)){
					factory.createAgent(
							org.stwerff.mslagents.SolAgent.class, "sol_" + sol);
				}
				String inner_url = "local://sol_"+sol;
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
	}

	@SuppressWarnings("unchecked")
	public void updateHeads(){
		System.err.println("UpdateHeads called.");
		String url = "local://stats";
		String method = "getIncompleteSols";
		ObjectNode params = JOM.createObjectNode();
		params.put("type", "heads");
		TotalRet total = new TotalRet();
		int todoSize=0;
		try {
			List<Integer> sols = (List<Integer>) send(url, method, params,List.class);
			todoSize=sols.size();
			total.total = Math.min(10,sols.size());
			int totalCnt=total.total;
			for (Integer sol : sols){
				if (totalCnt--<=0) break;
				System.err.println("Doing heads on sol:"+sol);
				url = "local://sol_"+sol;
				method = "updateHeads";
				sendAsync(url,method,JOM.createObjectNode(),new AsyncCallback<Object>() {
					private int sol=-1;
					private TotalRet total;

				    public void onSuccess(Object result) {
				        System.out.println("done:"+sol);
				        total.decr();
				    }

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
		
		if (count<= 0) System.err.println("(heads) Warning: "+total.total+" async calls weren't finished within 15 seconds!");
		if (todoSize>20){
			System.err.println("Large amount of heads todo, restarting directly.");
			try {
				send(getFirstUrl(),"updateHeads",JOM.createObjectNode());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.err.println("Done update heads.");
	}

	@SuppressWarnings("unchecked")
	public void updateSpice(@Name("reload") @Required(false) Boolean reload){
		System.err.println("UpdateSpice called.");

		if(reload==null)reload=false;
		String url = "local://stats";
		String method = "getIncompleteSols";
		ObjectNode params = JOM.createObjectNode();
		params.put("type", "spice");
		TotalRet total = new TotalRet();
		try {
			List<Integer> sols;
			sols = (List<Integer>) send(url, method, params,List.class);
			total.total = Math.min(10,sols.size());
			if (reload){
				url = "local://max";
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
				url = "local://sol_"+sol;
				method = "updateSpice";
				params = JOM.createObjectNode();
				params.put("reload", reload);
				sendAsync(url,method,params,new AsyncCallback<Object>() {
					private int sol=-1;
					private TotalRet total;

				    public void onSuccess(Object result) {
				        System.out.println("done:"+sol);
				        total.decr();
				    }


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
		if (count<= 0) System.err.println("(spice) Warning: "+total.total+" async calls weren't finished within 15 seconds!");
		System.err.println("Done update spice.");
	}
	public void updateStats(){
		System.err.println("UpdateStats called.");

		TotalRet total = new TotalRet();
		try {
			List<Integer> sols;
			String url = "local://max";
			String method = "getMaxSol";
			ObjectNode params = JOM.createObjectNode();
			Integer maxSol = send(url,method,params,Integer.class);
			sols = new ArrayList<Integer>();
			for (int i=0; i<=maxSol; i++){
				sols.add(i);
			}
			total.total=sols.size();

			
			int totalCnt = total.total;
			for (Integer sol : sols){
				if (totalCnt--<=0) break;
				Thread.sleep(1000);//1 per second;
				System.err.println("Doing stats reload on sol:"+sol);
				url = "local://sol_"+sol;
				method = "reloadList";
				params = JOM.createObjectNode();
				sendAsync(url,method,params,new AsyncCallback<Object>() {
					private int sol=-1;
					private TotalRet total;

				    public void onSuccess(Object result) {
				        System.out.println("done:"+sol);
				        total.decr();
				    }

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
		System.err.println("Done update Stats.");
	}
	
	private class TotalRet {
		int total=-1;
		public void decr(){
			total--;
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
