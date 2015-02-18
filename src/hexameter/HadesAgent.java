package hexameter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


/**
 * HadesAgent is an abstract class providing the framework and interface for a agent controller
 * for (robotic) agents controller via HADES (A Discrete-time Environment Simulator). HADES uses
 * the Hexameter protocol for communication and can control robots simulated in ARGoS.
 * 
 * If you want to control a robot, you should inherit from this class and override the abstract
 * react method. HadesAgent.react is given an instance of ReactionContext (cf. the relevant
 * documentation), which provides information about the current state of the world and is able to
 * accept and deliver commands for the robots' actuators. Note that you should use the provided
 * constructors of HadesAgent in your inheriting class.
 * 
 * For an example of how an agent controller derived from HadesAgent might look like, please refer
 * to the class ObstacleAgent delivered with this project.
 * 
 * @author Thomas Gabor
 */
public abstract class HadesAgent {

	protected abstract void react(ReactionContext context);

	protected String hades;
	protected String[] bodies;
	protected HexameterContext hx;
	protected boolean registered = false;
	protected Clock clock;
	
	protected static class Clock {
		protected long time = 0;
		public boolean update(long newtime) {
			if ( newtime > this.time ) {
				this.time = newtime;
				return true;
			} else {
				return false;
			}
		}
		public long getTime() {
			return this.time;
		}
	}
	
	/**
	 * A ReactionContext contains all information needed for an agent to compute the action for
	 * the next time slice. It is made available as a parameter to the HadesAgent.react method.
	 * You can use the various getters and the ReactionContext.sensor method to query the current
	 * state of the world and the agents' behavior. You are then supposed to call
	 * ReactionContext.motor from inside your HadesAgent.react method to alter the world state via
	 * the agents' actuators if necessary.
	 *
	 */
	protected static class ReactionContext {
		protected HadesAgent agent;
		protected HexameterContext hx;
		protected long period;
		protected String body;
		protected JSONArray reactions;
		
		public ReactionContext(HadesAgent agent, long period, String body) {
			this.agent = agent;
			this.hx = agent.getHexameterContext();
			this.period = period;
			this.body = body;
			this.reactions = null;
		}
		
		public JSONArray getReactions() {
			return reactions;
		}

		public void setReactions(JSONArray reactions) {
			this.reactions = reactions;
		}

		public HadesAgent getAgent() {
			return agent;
		}

		public HexameterContext getHexameterContext() {
			return hx;
		}

		public long getPeriod() {
			return period;
		}

		public String getBody() {
			return body;
		}
		
		/**
		 * Calls a sensor defined by the HADES world config file and returns its measurements. Note
		 * that it returns a JSONArray or JSONObject according to the specification in the HADES
		 * config file, so if you know what sensor you're calling, the explicit type cast to either
		 * JSONArray or JSONObject is safe.
		 * 
		 * @param type the name of the sensor to be queried according to the HADES world config
		 * @param sensorControl a JSONObject containing additional parameters for the sensor (give
		 *                      null or just call sensor(type) for no parameters)
		 * @return either a JSONArray or a HSONObject containing the measurements from the sensor
		 *         according to sensor specification in HADES world config file
		 */
		@SuppressWarnings("unchecked")
		public Object sensor(String type, JSONObject sensorControl) {
			JSONArray sensorParameter = new JSONArray();
			JSONObject sensorItem = new JSONObject();
			sensorItem.put("body", this.body);
			sensorItem.put("type", type);
			if ( sensorControl != null ) {
				sensorItem.put("control", sensorControl);
			};
			sensorParameter.add(sensorItem);
			JSONArray sensorResponse = this.hx.ask("get", this.agent.getHades(), "sensors", sensorParameter);
			return ((JSONObject)sensorResponse.get(0)).get("value");
		}
		
		public Object sensor(String type) {
			return this.sensor(type, null);
		}
		
		/**
		 * Saves a motor command to pass on to HADES to execute in the next time slice. Note that
		 * the motor command isn't sent immediately, but sent automatically once HadesAgent.react
		 * is finished. This is what you want since motor commands can only be executed in the
		 * following time slice anyway.
		 * 
		 * @param type the name of the motor to be run according to the HADES world config
		 * @param motorControl a JSONObject containing additional parameters for the motor (give
		 *                      null or just call run(type) for no parameters)
		 */
		@SuppressWarnings("unchecked")
		public void motor(String type, JSONObject motorControl) {
			if ( this.reactions == null ) {
				this.reactions = new JSONArray();
			};
			JSONObject motorItem = new JSONObject();
			motorItem.put("body", this.body);
			motorItem.put("type", type);
			if ( motorControl != null ) {
				motorItem.put("control", motorControl);
			}
			this.reactions.add(motorItem);
		}
		
		public void motor(String type) {
			this.motor(type, null);
		}
	}
	

	/**
	 * This constructor creates a new HexameterContext along with the agent controller. This is
	 * is what you want if you don't need Hexameter communication and your current component
	 * except for controller (robot) agents in HADES.
	 * 
	 * @param hadesAddress the Hexameter address of HADES (e.g. "localhost:55555")
	 * @param name the Hexameter address this agent controller should use (e.g. "localhost:99999")
	 * @param managedBodies the list of HADES bodies (names as specified in the HADES world config
	 *                      file) this agent controller shall control
	 */
	public HadesAgent(String hadesAddress, String name, String[] managedBodies) {
		this(hadesAddress, new HexameterContext(), managedBodies); 
		this.hx.init(name, () -> {
			return (type, author, space, parameter, recipient) -> {
				JSONArray response = this.processMessage(type, author, space, parameter, recipient);
				if ( response != null ) {
					return response;
				} else {
					return new JSONArray();
				}
			};
		});
	}
	
	/**
	 * This is the constructor to use if your component does Hexameter communication on its own
	 * already and shall work as an agent controller <em>as well</em>. Note that since the agent
	 * controller has no influence on HexameterContext's responsive behavior, you need to ensure
	 * that messages directed at the agent controller (i.e. ticks from HADES) reach the controller.
	 * To do so, you can call the processMessage and processMessageUnchecked methods.
	 * 
	 * @param hadesAddress the Hexameter address of HADES (e.g. "localhost:55555")
	 * @param hexameterContext the pre-existing HexameterContext
	 * @param managedBodies the list of HADES bodies (names as specified in the HADES world config
	 *                      file) this agent controller shall control
	 */
	public HadesAgent(String hadesAddress, HexameterContext hexameterContext, String[] managedBodies) {
		this.hades = hadesAddress;
		this.hx = hexameterContext;
		this.bodies = managedBodies;
		this.clock = new Clock();
	}


	public String getHades() {
		return this.hades;
	}
	
	public HexameterContext getHexameterContext() {
		return this.hx;
	}
	
	public String[] getBodies() {
		return this.bodies;
	}
	
	public void markBodiesRegistered() {
		this.registered = true;
	}
	
	/**
	 * Registers the bodies to be controlled by the HadesAgent with HADES so that the agent is
	 * sent ticks for the respective bodies. Usually, this will be done automatically by HadesAgent
	 * and you don't need to call this method any further.
	 */
	@SuppressWarnings("unchecked")
	public void registerBodiesWithHADES() {
		for ( String body : this.bodies ) {
			JSONArray tickParameter = new JSONArray();
			JSONObject tickItem = new JSONObject();
			tickItem.put("body", body);
			tickItem.put("soul", hx.me());
			tickParameter.add(tickItem);
			hx.tell("put", this.hades, "ticks", tickParameter);
			hx.tell("put", this.hades, "tocks", tickParameter);
		};
		this.registered = true;
	}
	
	/**
	 * When using HadesAgent with an externally provided HexameterContext, you can call this method
	 * in that HexameterContext's callback behavior definition to have the HadesAgent process it.
	 * Note that since processMethodUnchecked doesn't check if the message is even meant for a
	 * HadesAgent, you have to do that check manually in the surrounding code. The parameter and
	 * return types match the standard message processing conventions of Hexameter as defined in
	 * the MessageProcessor interface.
	 */
	@SuppressWarnings("unchecked")
	public JSONArray processMessageUnchecked(String type, String author, String space, JSONArray parameter, String recipient) {
		JSONArray response = new JSONArray();
		boolean updated = false;
		for ( Object parameterItem : parameter ) {
			JSONObject item = (JSONObject) parameterItem;
			if ( item.get("period") != null ) {
				updated = updated || this.clock.update((long) item.get("period"));
			}
		}
		if ( updated ) {
			//System.out.println("\n\n::  Entering time period #" + Long.toString(clock.getTime()));
			for ( String body : this.bodies ) {
				//System.out.println("::  Computing " + name);
				ReactionContext reactionContext = new ReactionContext(this, this.clock.getTime(), body);
				this.react(reactionContext);
				if ( reactionContext.getReactions() != null ) {
					this.hx.tell("put", this.hades, "motors", reactionContext.getReactions());
				};
				JSONArray tockParameter = new JSONArray();
				JSONObject tockItem = new JSONObject();
				tockItem.put("body", body);
				tockParameter.add(tockItem);
				hx.tell("put", this.hades, "tocks", tockParameter);
			}
		}
		return response;
	}
	
	
	/**
	 * Like processMessageUnchecked, except that it checks for the standard space name for ticks
	 * sent from HADES and returns null if the given message doesn't match.
	 */
	public JSONArray processMessage(String type, String author, String space, JSONArray parameter, String recipient) {
		if ( type.equals("put") && space.equals("hades.ticks") ) {
			return this.processMessageUnchecked(type, author, space, parameter, recipient);
		} else {
			return null;
		}
	}
	
	/**
	 * Agent blocks until one tick from HADES is received, then reacts to one new time slice.
	 */
	public void run() {
		if ( !this.registered ) {
			this.registerBodiesWithHADES();
		};
		this.hx.respond(0);
	}
	
	/**
	 * Agent tries to react to one time slice if it can currently find a pending tick message
	 * from HADES.
	 */
	public void runMaybe() {
		if ( !this.registered ) {
			this.registerBodiesWithHADES();
		};
		this.hx.respond();
	}
	
	public void runForever() {
		while ( true ) {
			this.run();
		}
	}
	
	public void stop() {
		this.hx.term();
	}	
}
