+-----------------------------------------------------------------------------+
|                             HEXAMETER FOR JAVA                              |
+-----------------------------------------------------------------------------+

To control the behavior of agents simulated via HADES, you may only need the
HadesAgent class and can thus skip to the third section of this readme.


## USING HEXAMETER ############################################################

The central class to be used by external components is HexameterContext. To 
establish a Hexameter connection, you will want to instantiate this class and
use its methods.

The class Ajent implements a simple example where Hexameter is used to
communicate with an instance of HADES as defined in the Academia scenario found
at:
	github.com/hoelzl/Academia/tree/master/Scenario/obstacles
A more advanced re-implementation of Ajent is provided though the class
ObstacleAgent, which comes with this project.

The basic usage scenario of Hexameter starts with an instantiation call like

HexameterContext hx = new HexameterContext();
hx.init("localhost:77777", () -> {
	return (type, author, space, parameter, recipient) -> {
		JSONArray response = new JSONArray();
		if ( type.equals("get") && space.equals("echo") ) {
			for ( Object parameterItem : parameter ) {
				JSONObject item = (JSONObject) parameterItem;
				response.add(item);
			}
		}
		return response;
	};
});

Here, we first create an empty HexameterContext, then call its init() method.
The first parameter for the init method is the address under which the current
component we're working on can be reached, i.e. our name. The second parameter
defines what to do when receiving messages through the network, which is best
realized by passing a closure here, which is executed once without any
parameters and expected to return a value of the type MessageProcessor, another
potential closure type, which is then called for every incoming message.
[[NOTE: In Java, it is rarely useful to put code besides the return statement
        inside the outer closure. Still, this design was chosen for consistency
        with Hexameter implementations in other languages, where this proves
        more useful.]]
Again, the MessageProcessor is most easily given by a closure. It receives
5 arguments describing the received message:
* String type
	one of "put", "get", "qry"
* String author
	the address of the component that sent the message. It can be used to send
	further messages to it.
* String space
	the name of the targeted knowledge repository. Note that these names are
	defined by the respective component, i.e. when init'ing a HexameterContext
	you are free to check for space names to your liking. However, not that
	existing Hexameter components already use space names prefixed with "net.",
	"hexameter.", "hades." and "iason.".
* JSONArray parameter
	A JSONArray which should consist of JSONObject elements only according to
	Hexameter specification. It contains the message payload, whose content is
	not further specified.
* String recipient
    That's the name the message sender used to reach this component under. In
    most cases, we can ignore that.
 Furthermore, a MessageProcessor is expected to return a value structured just
 like its parameter named "parameter", i.e. a JSONArray containing JSONObjects.
 [[NOTE: In Hexameter, it is common to return a response even for put messages.
         You should just return the "parameter" value you've been given to
         signal that the put has been processed successfully. The requesting
         component can then decide to wait on that response or not.]]

Once a HexameterContext has been instantiated, you can use it to send outgoing
and/or respond to incoming messages. To send a message, you can use a call to
the tell() method like this:
	JSONArray parameter = new JSONArray();
	JSONObject item = new JSONObject();
	item.put("the answer to life, the universe and everything", 42);
	parameter.add(item);
	hx.tell("get", "localhost:77777", "echo", parameter);
This code would send a message to the knowledge space defined above. The
parameters' contents match those of MessageProcessor parameters explained
above. In order, there are:
* String type
* String recipient
* String space
* JSONArray parameter
[[NOTE: You can send messages to your own HexameterContext this way, by
		specifying the address you used in the init() call as the recipient
		address of the tell() call as we did here. However, those messages
		will not travel over the network but will take the obvious shortcut.]]
Note that tell() is a void method, which ignores any response coming from the
recipient of the message. However, in most cases, you will want the response to
your message immediate. Then, you can just replace the last line of the example
above with:
	JSONArray response = hx.ask("get", "localhost:77777", "echo", parameter);
The ask() method features the same parameters as tell, but waits for a response
form the targeted component and return said response.

Messages received by our component will be queued up until message processing
is launched manually.
[[NOTE: At the basis of Hexameter, networking is done by ZeroMQ (zeromq.org).
		Cf. their documentation on details on when and where messages are
		piled up.]]
To process received messages, call:
	hx.respond();
This method will spend some short time to process messages it can find. You can
alter the amount of time spend by passing an integer to respond(). If you pass
the integer 0, Hexameter will (blocking, non-busy) wait for the next message
received, which is useful for implementing servers or reactive agents e.g.. 
	hx.respond(0);
If there is a message found to be reacted to, Hexameter will invoke the "space"
(i.e. inner closure) defined in the init() call to process the message and
deliver the response returned by it.

If you're done using Hexameter for your program, you should call
	hx.term();
to close the HexameterContext. However, I don't recall anything really bad
happening when not doing so.



## COMMUNICATING WITH HADES ###################################################

HADES (A Discrete-time Environment Simulator, github.com/thomasgabor/hades)
provides a framework for the  coordination and execution of simulation tasks.
It can connect to ARGoS (Autonomous Robots Go Swarming, iridia.ulb.ac.be/argos)
via a component called IASON, which is included (alongside HADES and some more
stuff) in the framework provided under the name "Academia", found at
github.com/hoelzl/Academia. If all you want to do is control robots in a HADES
and/or ARGoS simulation via Hexameter, you can skip this section and proceed
with the next section on the HadesAgent class, which provides an easy interface
to do just that.

However, you are free to communicate in various way with a running instance of
HADES using Hexameter. A HADES instance features multiple "bodies", which are
the agents acting in the simulated world. External controllers can take over
control over one or more of these bodies, thus being a "soul" to it. A body
usually features sensors and motors to interact with its environment. The whole
setup of the world is described in Lua in the HADES world config file, which is
provided as a parameter at the start of HADES. Since the setup of a world
simulation can easily become quite extensive, you should use a tool called
"CHARON", which can do most of the setup automatically. Call CHARON like this
	lua charon.lua -h
to view the documentation of CHARON and the world file specification and call
	lua charon.lua worldfile.lua
to run the simulation described in the world file worldfile.lua. The Academia
repository (see above) contains an easy example under Scenario/obstacles,
which features ARGoS as well and provides a short readme of its own.

When a HADES instance is running, a soul can register for one or several bodies
by sending a message like
	put({{body: "robot1", soul: "localhost:99999"}})@ticks
which translates into Java code like this (assuming we have an instance of
HexameterContext saved in a variable hx, which is using the Hexameter address
"localhost:99999", and HADES is running at "localhost:55555"): 
	JSONArray tickParameter = new JSONArray();
	JSONObject tickItem = new JSONObject();
	tickItem.put("body", "robot1");
	tickItem.put("soul", "localhost:99999");
	tickParameter.add(tickItem);
	hx.tell("put", "localhost:55555", "ticks", tickParameter);
	hx.tell("put", "localhost:55555", "tocks", tickParameter);
The meaning of the last line will be explained shortly.

From then on, at the beginning of each new time slice, HADES will send a tick
message of the form
	put({{period: 42}})@hades.ticks
to any registered soul. The soul is then expected to answer with a tock message
structured like this
	put({{body: "robot1"}})@tocks
to signal that the computation for this time step is done. In between, the soul
can send messages of the form
    qry({{body: "robot1", type: "position"}})@sensors
or
    qry({{body: "robot1", type: "position", control: {unit: "km"}}})@sensors
to query sensors of the respective robot or messages of the form
    put({{body: "robot1", type: "move"}})@motors
or
    put({{body: "robot1", type: "move", control: {to: "home"}}})@motors
to decide on motor actions for the next time step. Note that the computation of
the first time step starts without a tick, which is why souls are expected to
send a tock before receiving the first tick message (which explains the last
line in the code snippet above).

There are many more features that HADES provides and that can be controlled via
sending the right Hexameter messages, however, to keep things short, this text
focuses on robot control.



## COMMUNICATING VIA AN INSTANCE OF HADESAGENT ################################

The class HadesAgent already provides all the necessary protocol to control a
robot simulated via HADES. As its an abstract class, you are expected to derive
a subclass and fill in your own behavior for the robot agent. This is simply
done by overriding the react method. As the relevant methods of HadesAgent are
reasonably well documented, it is recommended to jump right into the code.

Also, have a look at ObstacleAgent, which is an example sub-class of
HadesAgent, which works with the nobstacles.lua world config file provided in
the "Academia" repository (see
github.com/hoelzl/Academia/tree/master/Scenario/obstacles ). In this directory,
you also find a readme.txt describing how to launch the HADES/ARGoS setup.



## APPENDIX ###################################################################

To test your Hexameter communication, a program called GHOST (Hands-On Scel
Terminal) comes in handy, as it allows you to write Hexameter messages
via a command-line interface. You can download GHOST as part of the Hexameter
implementation in Lua at:
	github.com/thomasgabor/hexameter
Once you start it using the Lua 5.1 implementation on your system via
	lua ghost.lua
it will ask for an address like "localhost:55555" on which you want your new
GHOST component to be reachable. You can type "help" for a list of available
commands.