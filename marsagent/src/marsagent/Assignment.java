package marsagent;

import java.util.ArrayList;
import java.util.List;

public class Assignment {
	public static void updateAssignments(World world) {
		
		// Fill in the two sets: repairers and disabled allies
		List<MarsAgent> disabledAgents = new ArrayList<MarsAgent>();
		List<MarsAgent> repairers = world.getAgents("Repairer");
		for (MarsAgent a : world.getAgents(MarsAgent.ALL)) {
			int health = a.getHealth();
			if (health == 0) {
				disabledAgents.add(a);
			} 
		}
		
		// Find the cost from each repairer to each disabled ally
		double[][] cost = new double[repairers.size()][disabledAgents.size()];
		for (MarsAgent r : repairers) {
			for (MarsAgent d : disabledAgents) {
				cost[repairers.indexOf(r)][disabledAgents.indexOf(d)]=r.getDistance(d);
				if (r.getName().equals(d.getName())) {
					cost[repairers.indexOf(r)][disabledAgents.indexOf(d)]=Double.MAX_VALUE;
				}
			}
		}
		
		// Solve the matching problem
		int[] assign = new int[repairers.size()];
		HungarianAlgorithm algo = new HungarianAlgorithm(cost);
		assign = algo.execute();
		
		// Assign repairer targets
		for (MarsAgent r : repairers) {
			if (assign[repairers.indexOf(r)] != -1) {
				((Repairer) r).setDisabledAllyTarget(disabledAgents.get(assign[repairers.indexOf(r)]));
				disabledAgents.get(assign[repairers.indexOf(r)]).setRepairerTarget((Repairer) r);
			}
			else {
				((Repairer) r).setDisabledAllyTarget(null);
			}
		}
	}
}
