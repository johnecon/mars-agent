package marsagent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import massim.javaagents.Agent;
import eis.EILoader;
import eis.EnvironmentInterfaceStandard;
import eis.exceptions.ActException;
import eis.exceptions.AgentException;
import eis.exceptions.ManagementException;
import eis.exceptions.RelationException;
import eis.iilang.Action;

public class App {
	private static EnvironmentInterfaceStandard envInterface = null;
	private static List<MarsAgent> agents = new ArrayList<MarsAgent>();

	private static void addAgent(MarsAgent agent, String entity) {
		try {
			envInterface.registerAgent(agent.getName());
		} catch (AgentException e) {
			e.printStackTrace();
		}

		try {
			envInterface.associateEntity(agent.getName(), entity);
		} catch (RelationException e) {
			e.printStackTrace();
		}

		agents.add(agent);
	}

	public static void main(String[] args) {
		// Setup environment interface
		try {
			envInterface = EILoader.fromClassName("massim.eismassim.EnvironmentInterface");
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			envInterface.start();
		} catch (ManagementException e) {
			e.printStackTrace();
		}

		Agent.setEnvironmentInterface(envInterface);

		// Add agents
		for (int i = 1; i <= 28; i++) {
			String entity = "connectionA" + i;
			if (i<=6) {
				addAgent(new Explorer(entity, "A"), entity);
			} else if (i<=12) {
				addAgent(new Repairer(entity, "A"), entity);
			} else if (i<=16) {
				addAgent(new Saboteur(entity, "A"), entity);
			} else if (i<=22) {
				addAgent(new Sentinel(entity, "A"), entity);
			} else if (i<=28) {
				addAgent(new Inspector(entity, "A"), entity);
			}
		}

		// Team names must match otherwise saboteurs will attack themselves!

		// Listen for input
		// (new Thread(new InputHandler(marsAgent))).start();

		// Ensure all agents are ready!
		while (true) {
			boolean ready = true;

			for (MarsAgent agent : agents) {
				if (!agent.isReady()) {
					ready = false;
					break;
				}
			}

			if (ready) {
				break;
			}
		}

//		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		// Main loop
		while (true) {
			System.out.println("\n\n");

//			try {
//				System.out.println("Ready!");
//				reader.readLine();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}

			for (MarsAgent agent : agents) {
				agent.handlePercepts();
			}

			Assignment.updateAssignments(World.getInstance("A"));
//			Assignment.updateAssignments(World.getInstance("B"));

			for (MarsAgent agent : agents) {
				System.out.println("======== " + agent.getName() + " =========");
				if (!PerceptManager.getInstance("A").getPercepts("score").isEmpty()) {
					System.out.println("steps: " + PerceptManager.getInstance("A").getPercepts("step").get(0).getParam1() +
							", score: "+ PerceptManager.getInstance("A").getPercepts("score").get(0).getParam1() +
							", zones score: " + PerceptManager.getInstance("A").getPercepts("zonesScore").get(0).getParam1());
				}
				Action action = null;

				try {
					action = agent.step();
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (action == null) continue;

				try {
					Agent.getEnvironmentInterface().performAction(agent.getName(), action);
				} catch (ActException e) {
					System.out.println("agent \"" + agent.getName() + "\" action \"" + action.toProlog() + "\" failed!");
					System.out.println("message:" + e.getMessage());
					System.out.println("cause:" + e.getCause());
				}
			}
		}
	}
}
