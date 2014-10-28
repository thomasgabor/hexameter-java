import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


public class Ajent {

	public static class Clock {
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
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		JSONObject bodies = new JSONObject();
		bodies.put("math1", "whatever");
		bodies.put("math2", "whatever");
		bodies.put("math3", "whatever");
		bodies.put("math4", "whatever");
		bodies.put("math5", "whatever");
		bodies.put("math6", "whatever");
		bodies.put("math7", "whatever");
		bodies.put("math8", "whatever");
		bodies.put("math9", "whatever");
		bodies.put("math10", "whatever");
		bodies.put("math11", "whatever");
		bodies.put("math12", "whatever");
		String realm = "localhost:55555";
		HexameterContext hx = new HexameterContext();
		hx.init("localhost:99999", () -> {
			Clock clock = new Clock();
			return (type, author, space, parameter, recipient) -> {
				JSONArray response = new JSONArray();
				if ( type.equals("put") && space.equals("hades.ticks") ) {
					boolean updated = false;
					for ( Object parameterItem : parameter ) {
						JSONObject item = (JSONObject) parameterItem;
						if ( item.get("period") != null ) {
							updated = updated || clock.update((long) item.get("period"));
						}
					}
					if ( updated ) {
						//clock = newclock; // TODO find workaround here
						System.out.println("\n\n::  Entering time period #" + Long.toString(clock.getTime()));
						for ( Object bodiesKey : bodies.keySet() ) {
							String name = (String) bodiesKey;
							//System.out.println("::  Computing " + name);
							
							//<parametrize in future> <!-- kind of like psyche -->
							
							double speed = 5;
							long value = -1; // highest value found so far
							int idx = -1; // index of the highest value
							JSONArray sensorParameter = new JSONArray();
							JSONObject sensorItem = new JSONObject();
							sensorItem.put("body", name);
							sensorItem.put("type", "proximity");
							sensorParameter.add(sensorItem);
							JSONArray proximityResponse = hx.ask("get", realm, "sensors", sensorParameter);
							JSONArray prox = (JSONArray) ((JSONObject)proximityResponse.get(0)).get("value");
							for ( int i = 1; i <= 24; i++ ) {
								long proxValue = (long) ((JSONObject)prox.get(i-1)).get("value");
								if ( value < proxValue ) {
									idx = i;
									value = proxValue;
								}
							}
							
							JSONArray motorParameter = new JSONArray();
							JSONObject motorItem = new JSONObject();
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
							motorItem.put("body", name);
							motorItem.put("type", "setvelocity");
							motorItem.put("control", motorControl);
							motorParameter.add(motorItem);
							hx.tell("put", realm, "motors", motorParameter);
							
							//</parametrize in future>
							
							JSONArray tockParameter = new JSONArray();
							JSONObject tockItem = new JSONObject();
							tockItem.put("body", name);
							tockParameter.add(tockItem);
							hx.tell("put", realm, "tocks", tockParameter);
						}
					}
				}
				return response;
			};
		});
		//JSONArray reportParameter = new JSONArray();
		//reportParameter.add(new JSONObject());
		//bodies = hx.ask("qry", realm, "report", reportParameter).get(0).get("bodies");
		
		for ( Object bodiesKey : bodies.keySet() ) {
			String name = (String) bodiesKey;
			JSONArray tickParameter = new JSONArray();
			JSONObject tickItem = new JSONObject();
			tickItem.put("body", name);
			tickItem.put("soul", hx.me());
			tickParameter.add(tickItem);
			hx.tell("put", realm, "ticks", tickParameter);
			hx.tell("put", realm, "tocks", tickParameter);
			//System.out.println("**  first tocked " + name);
		};
		
		while ( true ) {
			hx.respond(0);
		}
	}

}
