import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


public class HexameterContext {

	protected String me;
	protected DaktylosContext medium;
	protected SpondeiosContext behavior;
	
	public HexameterContext init(String name, Space callback, Sphere[] spheres) {
		this.me = name;
		this.medium = new DaktylosContext();
		this.behavior = new SpondeiosContext();
		this.behavior.init(
				this.me,
				(type, recipient, space, parameter, author) -> {
					this.medium.message(type, recipient, space, parameter);
					return null;
				},
				callback,
				spheres
		);
		this.medium.init(
				this.me,
				(type, author, space, parameter, recipient) -> {
					return this.behavior.process(type, author, space, parameter, recipient);
				}
		);
		return this;
	}
	
	public HexameterContext init(String name, Space callback) {
		return this.init(name, callback, SpondeiosContext.DefaultSpheres);
	}
		
	public void term() {
		this.behavior.term();
		this.medium.term();
	}
	
	public String me() {
		return this.me;
	}
	
	public JSONArray tell(String type, String recipient, String space, JSONArray parameter, String author) {
		return this.behavior.act(type, recipient, space, parameter, author);
	}
	
	public JSONArray tell(String type, String recipient, String space, JSONArray parameter) {
		return this.behavior.act(type, recipient, space, parameter, this.me());
	}
	
	public JSONArray process(String type, String author, String space, JSONArray parameter, String recipient) {
		return this.behavior.process(type, author, space, parameter, recipient);
	}
	
	public JSONArray process(String type, String author, String space, JSONArray parameter) {
		return this.behavior.process(type, author, space, parameter, this.me());
	}
	
	public boolean respond(int tries) {
		return this.medium.respond(tries);
	}
	
	public boolean respond() {
		return this.medium.respond();
	}
	
	@SuppressWarnings("unchecked")
	public JSONArray ask(String type, String recipient, String space, JSONArray parameter) {
		JSONObject lustItem= new JSONObject();
		lustItem.put("author", recipient);
		lustItem.put("space", space);
		JSONArray lustParameter = new JSONArray();
		lustParameter.add(lustItem);
		this.process("put", this.me(), "net.lust", lustParameter, this.me());
		this.tell(type, recipient, space, parameter);
		JSONArray response = null;
		while ( response == null ) {
			this.respond(0);
			response = this.process("get", this.me(), "net.lust", lustParameter);
		}
		return response;
	}
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		HexameterContext hx = new HexameterContext();
		hx.init("localhost:99999", SpondeiosContext.TrivialSpace, SpondeiosContext.DefaultSpheres);
		//hx.respond(0);
		JSONObject testItem = new JSONObject();
		testItem.put("skills", 1337);
		testItem.put("profession", "haxxor");
		JSONArray testParameter = new JSONArray();
		testParameter.add(testItem);
		hx.tell("put", "localhost:77777", "test", testParameter);
		JSONArray testResponse = hx.ask("qry", "localhost:77777", "test", testParameter);
		System.out.println(testResponse);
	}

}
