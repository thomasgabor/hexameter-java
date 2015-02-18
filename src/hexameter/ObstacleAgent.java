package hexameter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


/**
 * This is an example of how you can use the classes provided by this project to control a robot
 * via HADES. If you want to to something similar, create a similar subclass of HadesAgent. Also,
 * please refer to the readme.txt of this project for further explanation!
 * 
 * ObstacleAgent is designed to work with the obstacle scenario included in Academia (cf. 
 * github.com/hoelzl/Academia for more information), which is a simple example borrowed from the
 * basic examples provided with ARGoS (cf. iridia.ulb.ac.be/argos for more) and used to illustrate
 * the infrastructure provided by Academia. The relevant world config file is found under the path
 * Academia/Scenario/obstacles/nobstacles.lua alongside a readme.txt in the same directory
 * containing brief instructions on how to execute it. (Note that the readme.txt may refer to 
 * another world config file, whose name you may then need to replace by "nobstable.lua"!) Once
 * ARGoS is running, you can run the ObstacleAgent to control the robots in their environment.
 * 
 * @author Thomas Gabor
 */
public class ObstacleAgent extends HadesAgent {

	public ObstacleAgent(String hadesAddress, String name, String[] managedBodies) {
		super(hadesAddress, name, managedBodies);
	}

	public ObstacleAgent(String hadesAddress, HexameterContext hx, String[] managedBodies) {
		super(hadesAddress, hx, managedBodies);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void react(ReactionContext context) {
		//the algorithm for obstacle avoidance is copied from an example file for ARGoS (see iridia.ulb.ac.be/argos )
		double speed = 5;
		double value = -1; // highest value found so far
		int idx = -1; // index of the highest value
		
		JSONArray proximityMeasurement = (JSONArray) context.sensor("proximity"); //this is how you call a HADES sensor
		for ( int i = 1; i <= 24; i++ ) {
			double proxValue = ((Number) ((JSONObject)proximityMeasurement.get(i-1)).get("value")).doubleValue();
			if ( value < proxValue ) {
				idx = i;
				value = proxValue;
			}
		}
		
		JSONObject motorControl = new JSONObject();
		if ( value == 0 ) {
			motorControl.put("left", speed);
			motorControl.put("right", speed);
		} else {
			if ( idx <= 12 ) {
				motorControl.put("left", speed);
				motorControl.put("right", Math.ceil((double) (idx-1) * speed / 11.0));
			} else {
				motorControl.put("left", Math.ceil((double) (24-idx) * speed / 11.0));
				motorControl.put("right", speed);
			}
		}
		context.motor("setvelocity", motorControl); //this is how you call a HADES motor
		
		System.out.println("::  told "+context.getBody()+" to set velocity to ("+motorControl.get("left").toString()+", "+motorControl.get("right").toString()+")");
	}
	
	static public void main(String[] args) {
		String hadesAddress = "localhost:55555";
		if ( args.length >= 1 ) {
			hadesAddress = args[1];
		}
		String myAddress = "localhost:999999";
		if ( args.length >= 2 ) {
			myAddress = args[2];
		}
		System.out.println("**  Starting ObstacleAgent...");
		String[] bodies = {"math1","math2","math2","math3","math4","math5","math6","math7","math8","math9","math10","math11","math12"};
		ObstacleAgent agent = new ObstacleAgent(hadesAddress, myAddress, bodies);
		while ( true ) {
			agent.run();
		}
	}

}
