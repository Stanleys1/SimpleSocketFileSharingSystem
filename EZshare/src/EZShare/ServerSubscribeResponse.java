package EZShare;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.json.simple.JSONObject;

/**
 * the subscription thread
 *
 */
public class ServerSubscribeResponse  extends Thread{
	private EzServer server;
	private DataOutputStream out;
	private Resource template;
	private boolean finished = false;
	private int resultSize = 0;
	
	public ServerSubscribeResponse(EzServer server, DataOutputStream out,Resource template){
		this.out = out;
		this.server= server;
		this.template= template;
	}
	
	public void run(){
		//get current matching queries from the template
		ArrayList<Resource> r = server.getResource();
		for(int i = 0 ; i< r.size() ;i++){
			if(r.get(i).match_template(template)){
				try {
					out.writeUTF(r.get(i).getJSON().toJSONString());
					resultSize ++;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		//while not finished
		while(!finished){
			try {
				
				//TODO MUST CHANGE ALL THIS
				JSONObject c = new JSONObject();
				c.put("message", "sending stuff every 5 s");
				out.writeUTF(c.toJSONString());
				System.out.println(c.toJSONString());
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * get the resultSize from this subscription
	 * @return resultSize
	 */
	public int getResultSize(){
		return this.resultSize;
	}
	
	
	/**
	 * stop the thread
	 */
	public void stopThread() {
		finished = true;
		
	}
}
