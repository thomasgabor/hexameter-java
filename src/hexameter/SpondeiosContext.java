package hexameter;
import java.util.HashMap;

import org.json.simple.*;


public class SpondeiosContext {

	@SuppressWarnings("unchecked")
	public static final Space TrivialSpace = () -> {
		return (type, author, space, parameter, recipient) -> {
			JSONObject responseItem = new JSONObject();
			responseItem.put("description", "answer to message " + type + "@" + space + " from " + author);
			JSONArray response = new JSONArray();
			response.add(responseItem);
			return response;
		};
	};
	
	public static final Sphere IdSphere = (continuation, direction) -> {
		return (type, author, space, parameter, recipient) -> {
			return continuation.process(type, author, space, parameter, recipient);
		};
	};
	
	public static final Sphere VerboseSphere = (continuation, direction) -> {
		if ( direction.equals("in") ) {
			return (type, author, space, parameter, recipient) -> {
				System.out.println("--  [recv " + type + "] " + parameter);
				System.out.println("--           @ " + space + " from " + author);
				return continuation.process(type, author, space, parameter, recipient);
			};
		} else {
			return (type, recipient, space, parameter, author) -> {
				System.out.println("++  [send " + type + "] " + parameter);
				System.out.println("++           @ " + space + " to " + recipient);
				return continuation.process(type, recipient, space, parameter, author);
			};
		}
	};
	
	@SuppressWarnings("unchecked")
	public static final Sphere NetworkingSphere  = (continuation, direction) -> {
		if ( direction == "in" ) {
			HashMap<String, HashMap<String, Boolean>> lust = new HashMap<String, HashMap<String, Boolean>>();
			HashMap<String, HashMap<String, JSONArray>> responses = new HashMap<String, HashMap<String, JSONArray>>();
			return (type, author, space, parameter, recipient) -> {
				if ( type.equals("ack") ) {
					for ( Object parameterItem : parameter ) {
						JSONObject item = (JSONObject) parameterItem;
						if ( lust.get(author) != null && lust.get(author).get(space) != null ) {
							responses.get(author).putIfAbsent(space, new JSONArray());
							responses.get(author).get(space).add(item);
						}
					}
					return null;
				}
				if ( space.equals("net.lust") ) {
					if ( type.equals("put") ) {
						for ( Object parameterItem : parameter ) {
							JSONObject item = (JSONObject) parameterItem;
							String wantedAuthor = (String) item.get("author");
							String wantedSpace = (String) item.get("space");
							if ( wantedAuthor != null && wantedSpace != null ) {
								responses.putIfAbsent(wantedAuthor, new HashMap<String, JSONArray>());
								responses.get(wantedAuthor).put(wantedSpace, null);
								lust.putIfAbsent(wantedAuthor, new HashMap<String, Boolean>());
								lust.get(wantedAuthor).put(wantedSpace, true);
							}
						}
						return parameter;
					} else {
						JSONArray response = new JSONArray();
						boolean answered = false;
						for ( Object parameterItem : parameter ) {
							JSONObject item = (JSONObject) parameterItem;
							String wantedAuthor = (String) item.get("author");
							String wantedSpace = (String) item.get("space");
							if ( wantedAuthor != null
									&& wantedSpace != null
									&& responses.get(wantedAuthor) != null
									&& responses.get(wantedAuthor).get(wantedSpace) != null
							) {
								answered = true;
								for ( Object answerItem : responses.get(wantedAuthor).get(wantedSpace) ) {
									JSONObject answer = (JSONObject) answerItem;
									response.add(answer);
								}
							}
						}
						if ( answered ) {
							return response;
						} else {
							return null;
						}
					}
				} else if ( space.equals("net.life") ) {
					return parameter;
				} else {
					return continuation.process(type, author, space, parameter, recipient);
				}
			};
		} else { // type == "out"
			return (type, recipient, space, parameter, author) -> {
				return continuation.process(type, recipient, space, parameter, author);
			};
		}
	};
	
	public static final Sphere[] DefaultSpheres = {NetworkingSphere};
	public static final Sphere[] VerboseSpheres = {NetworkingSphere, VerboseSphere};

	
	protected String me;
	protected MessageProcessor message;
	protected MessageProcessor processor;
	protected MessageProcessor actor;
	
	public SpondeiosContext init(String name, MessageProcessor message, Space space, Sphere[] spheres) {
		this.me = name;
		//this.message = message;
		this.processor = space.install();
		for ( int i = 0; i < spheres.length; i++ ) {
			this.processor = spheres[i].build(this.processor, "in");
		}
		this.actor = message;
		for ( int i = spheres.length - 1; i >= 0; i-- ) {
			this.actor = spheres[i].build(this.actor, "out");
		}
		return this;
	}
	
	public SpondeiosContext init(String name, MessageProcessor message, Space space) {
		return this.init(name, message, space, DefaultSpheres);
	}
	
	public boolean term() {
		return true;
	}
	
	public String me() {
		return this.me;
	}
	
	public JSONArray process(String type, String author, String space, JSONArray parameter, String recipient) {
		return this.processor.process(type, author, space, parameter, recipient);
	}
	
	public JSONArray act(String type, String recipient, String space, JSONArray parameter, String author) {
		if ( recipient.equals(this.me()) ) {
			return this.process(type, recipient, space, parameter, author);
		} else {
			return this.actor.process(type, recipient, space, parameter, author);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		Sphere[] spheres = {IdSphere, NetworkingSphere};
		SpondeiosContext context = new SpondeiosContext().init(
				"localhost:88888",
				(type, author, space, parameter, recipient) -> {
					System.out.println("sending " + type + "@" + space + " from " + author);
					return null;
				},
				TrivialSpace,
				spheres
		);
		JSONObject requestItem = new JSONObject();
		requestItem.put("a", 42);
		JSONArray request = new JSONArray();
		request.add(requestItem);
		System.out.println(context.act("get", "testmachine:123456", "test", request, "testmachine:789"));
		System.out.println(context.process("get", "testmachine:123456", "test", request, "testmachine:789"));
	}
	
}
