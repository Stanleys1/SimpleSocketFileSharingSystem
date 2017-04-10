import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Resource {
	private String name;
	private String description;
	private String[] tags;
	private String uri;
	private String channel;
	private String owner;
	private String ezserver;
	
	public Resource(String name, String[] tags,String description,
			String uri, String channel,String owner,String ezserver){
		this.name= name;
		this.tags = tags;
		this.description= description;
		this.uri = uri;
		this.channel = channel;
		this.owner = owner;
		this.ezserver = ezserver;
		
	}
	
	public String getName(){
		return name;
	}
	
	public String getDescription(){
		return description;
	}
	
	public String[] getTags(){
		return tags;
	}
	
	public String getUri(){
		return uri;
	}
	
	public String getChannel(){
		return channel;
	}
	
	public String getOwner(){
		return owner;
	}
	
	public String getEzserver(){
		return ezserver;
	}
	
	public JSONObject getJSON(){
		JSONObject inside = new JSONObject();
		inside.put("name", name);
		inside.put("description",description);
		
		//tags
		JSONArray arraytags = new JSONArray();
		for(int i = 0 ; i<tags.length;i++){
			arraytags.add(tags[i]);
		}
		inside.put("tags", arraytags );
		
		inside.put("uri", uri);
		inside.put("channel", channel);
		inside.put("owner", owner);
		inside.put("ezserver", ezserver);
		
		return inside;
		
	}
	
	public boolean match_template(Resource template){
		if(!template.getChannel().equals(channel)){
			return false;
		}
		if(!template.getOwner().equals("")){
			if(!template.getOwner().equals(owner)){
				return false;
			}
		}
		boolean found;
		if(template.getTags().length!= 0){
			for(int i = 0; i<template.getTags().length ;i++){
				found = false;
				for(int j = 0 ; j < tags.length;j++){
					if(template.getTags()[i].equals(tags[j])){
						found = true;
						break;
					}
				}
				if(!found){
					return false;
				}
			}
		}
		
		if(!template.getUri().equals("")){
			if(!template.getUri().equals(uri)){
				return false;
			}
		}
		
		if(!template.getName().equals("")){
			return name.contains(template.getName());
		}else if(!template.getDescription().equals("")){
			return description.contains(template.getDescription());
		}else return true;
	}
	
	public Resource getResourceWithServer(String hostname, int port){
		this.ezserver= hostname+":"+port;
		return this;
	}
	
}
