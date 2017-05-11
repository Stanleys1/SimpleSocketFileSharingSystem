package EZShare;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

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
		
		while(!finished){
			try {
				out.writeUTF("sending stuff every 5 s");
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public int getResultSize(){
		return this.resultSize;
	}

	public void stopThread() {
		finished = true;
		// TODO Auto-generated method stub
		
	}
}
