package EZShare;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.json.simple.JSONObject;

/**
 * the subscription thread
 *
 */
public class ServerSubscribeResponse extends Thread implements ServerSubscribe {
	private DataOutputStream out;
	private Resource template;
	private boolean finished = false;
	private int resultSize = 0;
	//store relay for the id
	private boolean relay;
	
	public ServerSubscribeResponse(DataOutputStream out,Resource template,boolean relay){
		this.out = out;
		this.template= template;
		this.relay = relay;
	}
	
	public void run(){
		//get current matching queries from the template
		/*ArrayList<Resource> r = server.getResource();
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
		*/
		
		//while not finished
		while(!finished){
			try {
				
				sleep(5000);
			} catch (InterruptedException e) {
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
	
	public void checkResource(Resource rcs){
		if(rcs.match_template(this.template)){
			try {
				out.writeUTF(rcs.getJSON().toJSONString());
				resultSize++;
			} catch (IOException e) {

				e.printStackTrace();
			}
		}
	}
	
	public boolean getRelay(){
		return this.relay;
	}
	
	public Resource getTemplate(){
		return this.template;
	}
}
