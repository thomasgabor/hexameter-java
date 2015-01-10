import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * HexameterContext provides network communication via the Hexameter protocol. It is based on
 * 0MQ ( zeromq.org ) and JSON (this Java implementation uses the json-simple library found at 
 * code.google.com/p/json-simple ). Other implementations are available in Lua and Lisp.
 * 
 * The Hexameter API offers 6 primitive methods: init, term, me, tell, process and respond.
 * Please refer to their respective documentation for further explanation and to the static main()
 * method for a short usage example.
 * 
 * @author Thomas Gabor
 */
public class HexameterContext {

	protected String me;
	protected DaktylosContext medium;
	protected SpondeiosContext behavior;
	
	/**
	 * Initializes the HexameterContext. This causes Hexameter to listen on the specified port but
	 * does not cause it send or receive any messages.
	 * 
	 * @param name
	 *        the network address to be used for this HexameterContext, e.g. "localhost:123456".
	 *        Hexameter will listen for messages for this Context under this address/port. Thus,
	 *        other network components should be able to reach this address. The name given here
	 *        can always be retrieved via the me() method.
	 * @param callback
	 *        the responsive behavior of the HexameterContext. Is usually given via a closure of
	 *        the type Space (i.e. an unparametrized call returning a MessageProcessor). It will
	 *        be called on each received message upon processing it.
	 * @param spheres
	 *        pre- and post-processing steps for incoming and outgoing messages. Should usually
	 *        be left to default for common applications.
	 * @return
	 */
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
	
	public HexameterContext init(String name) {
		return this.init(name, SpondeiosContext.TrivialSpace);
	}
		
	/**
	 * Terminates the HexameterContext.
	 */
	public void term() {
		this.behavior.term();
		this.medium.term();
	}
	
	/**
	 * Returns the network address of this HexameterContext.
	 * @return name
	 */
	public String me() {
		return this.me;
	}
	
	/**
	 * Sends a Hexameter message via network connections as specified.
	 * 
	 * @param type
	 *        one of "get", "put" or "qry" according to SCEL specification
	 * @param recipient
	 *        the network address of the Hexameter component to send the message to
	 * @param space
	 *        the name of the knowledge space responsible for processing the message on the 
	 *        receiving Hexameter component
	 * @param parameter
	 *        The content of the message to be sent. Note that since Hexameter supports sending
	 *        multiple messages at once here, this array should always contain only one element
	 *        if you only intend to send one single message. Shortcuts available.
	 * @param author
	 *        optional, the author of the message to be sent. When left out, defaults to the
	 *        address of this HexameterContext, which is good.
	 * @return the sent message, should usually be ignored.
	 */
	public JSONArray tell(String type, String recipient, String space, JSONArray parameter, String author) {
		return this.behavior.act(type, recipient, space, parameter, author);
	}
	
	@SuppressWarnings("unchecked")
	public JSONArray tell(String type, String recipient, String space, JSONObject item, String author) {
		JSONArray parameter = new JSONArray();
		parameter.add(item);
		return this.tell(type, recipient, space, parameter, author);
	}
	
	public JSONArray tell(String type, String recipient, String space, JSONArray parameter) {
		return this.tell(type, recipient, space, parameter, this.me());
	}
	
	public JSONArray tell(String type, String recipient, String space, JSONObject item) {
		return this.tell(type, recipient, space, item, this.me());
	}
	
	public JSONArray put(String recipient, String space, JSONArray parameter) {
		return this.tell("put", recipient, space, parameter);
	}
	
	public JSONArray put(String recipient, String space, JSONObject item) {
		return this.tell("put", recipient, space, item);
	}
	
	/**
	 * Processes a Hexameter message locally as if received by an external component via network.
	 * 
	 * See the tell() method for explanations on the arguments. Note that author and recipient are
	 * exchanged in order, though.
	 */
	public JSONArray process(String type, String author, String space, JSONArray parameter, String recipient) {
		return this.behavior.process(type, author, space, parameter, recipient);
	}
	
	@SuppressWarnings("unchecked")
	public JSONArray process(String type, String recipient, String space, JSONObject item, String author) {
		JSONArray parameter = new JSONArray();
		parameter.add(item);
		return this.process(type, recipient, space, parameter, author);
	}

	public JSONArray process(String type, String author, String space, JSONArray parameter) {
		return this.process(type, author, space, parameter, this.me());
	}
	
	public JSONArray process(String type, String recipient, String space, JSONObject item) {
		return this.process(type, recipient, space, item, this.me());
	}
	
	
	/**
	 * Fetch, process and respond to one pending Hexameter message.
	 * 
	 * @param tries
	 *        specifies the amount of times HexameterContext should (non-deterministically) attempt
	 *        to fetch a pending message. If 0 is specified, HexameterContext will (non-busily)
	 *        wait for the next message. If left empty, defaults to a reasonable number of attempts
	 *        tested for common applications.
	 * @return true if new message was received during attempts
	 */
	public boolean respond(int tries) {
		return this.medium.respond(tries);
	}
	
	public boolean respond() {
		return this.medium.respond();
	}
	
	/**
	 * Send a Hexameter message as specified by the given arguments, wait for and return response.
	 * 
	 * See the tell() method for an explanation on the available arguments. Also note the various
	 * shortcuts available.
	 * 
	 * @return the received response
	 */
	@SuppressWarnings("unchecked")
	public JSONArray ask(String type, String recipient, String space, JSONArray parameter) {
		JSONObject lustItem = new JSONObject();
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
	public JSONArray ask(String type, String recipient, String space, JSONObject item) {
		JSONArray parameter = new JSONArray();
		parameter.add(item);
		return this.ask(type, recipient, space, parameter);
	}
	
	public JSONArray get(String recipient, String space, JSONArray parameter) {
		return this.ask("get", recipient, space, parameter);
	}
	
	public JSONArray get(String recipient, String space, JSONObject item) {
		return this.ask("get", recipient, space, item);
	}
	
	public JSONArray qry(String recipient, String space, JSONArray parameter) {
		return this.ask("qry", recipient, space, parameter);
	}
	
	public JSONArray qry(String recipient, String space, JSONObject item) {
		return this.ask("qry", recipient, space, item);
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
