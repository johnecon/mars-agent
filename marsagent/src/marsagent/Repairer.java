package marsagent;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import massim.javaagents.agents.MarsUtil;
import eis.iilang.Action;



public class Repairer extends MarsAgent {

	public boolean enrouteToDisabledAlly = false;
	public Queue<String> enrouteToDisabledAllyPlan= new LinkedList<String>();
	private MarsAgent disabledAllyTarget;

	public Repairer(String name, String team) {
		super(name, team);
	}

	@Override
	protected boolean canProbe() {
		return false;
	}

	@Override
	protected Action getAction() {
		// Repair disabled ally on the same spot.
		if (disabledAllyTarget != null && disabledAllyTarget.getPosition().equals(position)) {
			enrouteToDisabledAlly = false;
			enrouteToDisabledAllyPlan.clear();
			System.out.println("Repairing nearby disabled ally " + disabledAllyTarget.getName());
			return MarsUtil.repairAction(disabledAllyTarget.getShortName());
		}

		if (!enrouteToDisabledAlly && disabledAllyTarget != null) {
			meet(disabledAllyTarget, this);
		}

		if (enrouteToDisabledAlly) {
			if (enrouteToDisabledAllyPlan.isEmpty()){
				enrouteToDisabledAlly = false;
			}
			else return executePlan(enrouteToDisabledAllyPlan);
		}

        // If the exploration is finished roam around
        if (doneExploring()) {
            Set<String> adjacentNodes = world.getGraph().get(position).keySet();
            int item = new Random().nextInt(adjacentNodes.size());
            String next = (adjacentNodes.toArray(new String[0]))[item];

            return goTo(next);
        }

		return null;
	}

	public void releasePlan() {
		enrouteToDisabledAlly = false;
	}

	@Override
	protected boolean shouldCharge() {
		return (energy <= 2 && health > 0) || (energy <= 3 && health == 0);
	}

	public MarsAgent getDisabledAllyTarget() {
		return disabledAllyTarget;
	}

	public void setDisabledAllyTarget(MarsAgent disabledAllyTarget) {
		this.disabledAllyTarget = disabledAllyTarget;
	}
}
