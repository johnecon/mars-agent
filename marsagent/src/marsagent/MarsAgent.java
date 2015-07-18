package marsagent;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import massim.javaagents.Agent;
import massim.javaagents.agents.MarsUtil;
import spacesearch.AStarSolver;
import spacesearch.GoalTest;
import spacesearch.State;
import eis.iilang.Action;
import eis.iilang.Percept;

public abstract class MarsAgent extends Agent {
	static String ALL = "All";
    static int nextZoneIndex = 0;
    int zoneIndex = -1;

	protected final World world = World.getInstance(getTeam());
	protected final PerceptManager perceptManager = PerceptManager.getInstance(getTeam());
	protected final Queue<String> explorationPlan = new LinkedList<String>();
	protected Queue<String> enrouteToRepairerPlan = new LinkedList<String>();
	protected String position;
	protected String role;
	protected String team;
	protected String target;
	protected int energy = 30;
	protected int health;
	protected int visRange;
	protected int maxHealth;
	protected String vertexToOccupy;
	protected String lastAction = "";
	protected String lastActionResult = "";
	protected String lastActionParam = "";

	private boolean enrouteToRepairer = false;
	Repairer closestRepairer;
	private Repairer repairerTarget;
	private Enemy parryingEnemy;
	private boolean parriedBefore;
	protected boolean waitingForRepair;

	public MarsAgent(String name, String team) {
		super(name, team);
		world.addAgent(this);
	}

	public Enemy getParryingEnemy()
	{
		return parryingEnemy;
	}

	@Override
	public Action step() {
		if (!parriedBefore) {
			parryingEnemy = null;
		}

        System.out.printf("%d/%d explored, %d/%d known\n", world.getExplored().size(), world.numberOfVertices, world.getVertices().size(), world.numberOfVertices);

        // decrement counter of promised steps and break the promise if agent is disabled
        if (world.hasPromise(getName())) {
            world.decrementPromiseCounter(getName());
            if (health == 0) {
                world.breakPromise(getName());
                System.out.println("Agent disabled - breaking the promise.");
            }
        }

        if (shouldCharge()) {
			System.out.println("recharge");
			return MarsUtil.rechargeAction();
		}

		if (health == 0) {
			if (!enrouteToRepairer && repairerTarget != null) {
				meet(this, repairerTarget);
			}

			if (enrouteToRepairer) {
				if (repairerTarget.getPosition().equals(position) ||
						world.isAdjacent(repairerTarget.getPosition(),position)) {
					if (this.getRole().equals("Repairer")) {
						if (repairerTarget.waitingForRepair) {
							Action action = getAction();
							if (action != null) {
								return action;
							}
						}
					}
					waitingForRepair = true;
					return MarsUtil.rechargeAction();
				}
				else {
					waitingForRepair = false;
					Action findRepairer = executePlan(enrouteToRepairerPlan);
					if (findRepairer != null) {
						return findRepairer;
					}
				}
			}
		} else {
			if (enrouteToRepairer) {
				enrouteToRepairer = false;
				enrouteToRepairerPlan.clear();
			}
		}

		if (canParry()) {
			List<Enemy> nearbyEnemies = world.getNearbyEnemies(position);
			if (nearbyEnemies.size() > 0) {
				for (Enemy enemy : nearbyEnemies) {
					if (enemy.hasRole("Saboteur")) {
						MarsAgent ally = world.getParryingAlly(enemy);
						if (ally == null || ally.getName().equals(this.getName())) {
							parryingEnemy = enemy;
							parriedBefore = true;
							return MarsUtil.parryAction();
						}
					}
				}
			} else {
				parriedBefore = false;
			}
		}

		// Special behavior
		Action action = getAction();
		if (action != null) {
			return action;
		}

		// Random probe
		if (health > 0 && canProbe() && shouldProbe()) {
			System.out.println("probe");
			return MarsUtil.probeAction();
		}

		// Remove achieved actions
		String next = explorationPlan.peek();
		while (position.equals(next)) {
			explorationPlan.remove();
			if (explorationPlan.isEmpty()) {
				break;
			}
			next = explorationPlan.peek();
		}

		// Plan exploration
		if (explorationPlan.isEmpty() && !doneExploring()) {
			planExploration();
		}

		if (canProbe() && !world.isProbed(position) && shouldProbe()) {
			return MarsUtil.probeAction();
		}

        if (doneExploring() && !world.getZoneOptimization().IsInitialized()) {
            world.getZoneOptimization().Initialize();
        }

        if (!role.equals("Saboteur") && !role.equals("Repairer")) {
            if (explorationPlan.isEmpty() && doneExploring()) {
                vertexToOccupy = world.getVertexToOccupy(this);
                planPath(vertexToOccupy);
            }
        }

		Action goToAction = executePlan(explorationPlan);
		if (goToAction != null) {
			return goToAction;
		}
		return (canProbe() && !world.isProbed(position)) ? MarsUtil.probeAction() : MarsUtil.rechargeAction();
	}

	public String getShortName() {
		return getName().replace("connectionB", "b").replace("connectionA", "a");
	}

	protected boolean shouldCharge() {
		return energy < 2;
	}

	protected abstract boolean canProbe();

	protected boolean shouldProbe() {
		double probability = !world.isProbed(position) ? (world.isAdjacentToHighValuedNode(position) ? 1 : 0.5) : 0;
		return Math.random() < probability;
	}

	public boolean isReady() {
		perceptManager.reset();

		for (Percept p : getAllPercepts()) {
			perceptManager.addPercept(p);
		}

		return perceptManager.isReady();
	}

	public void handlePercepts() {
		perceptManager.reset();

		for (Percept p : getAllPercepts()) {
			perceptManager.addPercept(p);
		}

		String prevPosition = position;
		int prevEnergy = energy;

		role = perceptManager.getRole();
		energy = perceptManager.getEnergy();
		health = perceptManager.getHealth();
		maxHealth = perceptManager.getMaxHealth();
		position = perceptManager.getPosition();

		world.addExplored(position);

		perceptManager.manageVisibleEdges();
		perceptManager.manageProbedVertices();
		perceptManager.manageEnemyLastKnownPos();
		perceptManager.manageVertices();

		if (perceptManager.isLastActionA("successful", "goto")) {
			world.removePromise(position);
			world.insertEdgeWithCost(prevPosition, position, prevEnergy - energy);
		}

		if (perceptManager.isLastActionA("successful", "inspect")) {
			world.setEnemyInfo(perceptManager.getPercepts("inspectedEntity"));
		}
	}

	protected Action executePlan(Queue<String> p) {
		String next = p.peek();
		while (position.equals(next)) {
			p.remove();
			if (p.isEmpty()) {
				break;
			}
			next = p.peek();
		}

		if (next == null) {
			System.out.println("executePlan: empty plan --> return null");
			return null;
		}

		if (!world.isAdjacent(position, next)) {
			System.out.println("executePlan: invalid plan --> return null");
			p.clear();
			return null;
		}

		return goTo(next);
	}

	protected Action goTo(String next) {
		System.out.println("executePlan: goto(" + next + ")");

		// Recharge if either
		// - edge cost is unknown and energy is less than 9, or
		// - edge cost is known to be greater than energy
		if (world.unknownEdgeCost(position, next)) {
			if (energy < 9) {
				System.out.println("executePlan: energy < 9 --> recharge");
				return MarsUtil.rechargeAction();
			}
		} else if (world.getCost(position, next) > energy) {
			System.out.println("executePlan: cost > energy --> recharge");
			return MarsUtil.rechargeAction();
		}

		return MarsUtil.gotoAction(next);
	}

	protected void planExploration() {
		explorationPlan.clear();

		GoalTest<Vertex> goalTest = new GoalTest<Vertex>() {
			@Override
			public boolean isSolution(Vertex vertex, int depth) {
				return !world.getExplored().contains(vertex.getId()) && world.hasFreePromise(vertex.getId(), getName());
			}
		};

		State initialState = new Vertex(world, goalTest, position);
		AStarSolver solver = new AStarSolver();

		System.out.println("planExploration: new plan:");

		List<State> solutionStates = solver.solve(initialState);
		if (solutionStates == null) {
			System.out.println("planExploration: no plan!");
			return;
		}

		world.promise(getName(), solutionStates.get(solutionStates.size()-1).getId(), solutionStates.size()+2);

		System.out.printf("planExploration: visited: %d, expanded: %d\n", solver.getVisitedStateCount(), solver.getExpandedStateCount());

		solutionStates.remove(0);
		for (State state : solutionStates) {
			explorationPlan.offer(state.getId());
			System.out.printf("planExploration: - %s\n", state.getId());
		}
	}

	protected void planPath(final String id) {
		explorationPlan.clear();

		GoalTest<Vertex> goalTest = new GoalTest<Vertex>() {
			@Override
			public boolean isSolution(Vertex vertex, int depth) {
				return vertex.getId().equals(id);
			}
		};

		State initialState = new Vertex(world, goalTest, position);
		AStarSolver solver = new AStarSolver();

		System.out.println("planPath: new plan:");

		List<State> solutionStates = solver.solve(initialState);
		if (solutionStates == null) {
			System.out.println("planPath: no plan!");
			return;
		}

		System.out.printf("planPath: visited: %d\n", solver.getVisitedStateCount());
		System.out.printf("planPath: expanded: %d\n", solver.getExpandedStateCount());

		solutionStates.remove(0);
		for (State state : solutionStates) {
			explorationPlan.offer(state.getId());
			System.out.printf("planPath: - %s\n", state.getId());
		}
	}

	public void pathTo(String id) {
		if (target == null) {
			target = id;
		}
	}

	protected boolean doneExploring() {
        return world.getExplored().size() + world.getNumberOfPromises() == world.numberOfVertices;
	}

	protected abstract Action getAction();

	@Override
	public void handlePercept(Percept p) {
	}

	public String getRole() {
		return role;
	}

	public String getPosition() {
		return position;
	}

	public int getHealth() {
		return health;
	}

	public Repairer getClosestRepairer()
	{
		Repairer repairer = null;
		int minDistance=Integer.MAX_VALUE;
		for (MarsAgent agent : world.getAgents("Repairer")) {
			if (minDistance > this.getDistance(agent) && this != agent) {
				repairer = (Repairer) agent;
			}
		}
		return repairer;
	}

	public MarsAgent getClosestDisabledAlly()
	{
		MarsAgent closestDisabledAlly = null;
		int minDistance = Integer.MAX_VALUE;
		for (MarsAgent agent : world.getAgents(ALL)) {
			if (agent == this || agent.getHealth() > 0) {
				continue;
			}

			if (this.getDistance(agent) < minDistance) {
				closestDisabledAlly = agent;
			}
		}
		return closestDisabledAlly;
	}


	public int getEnergy()
	{
		return energy;
	}


	public int getVisRange() {
		// TODO Auto-generated method stub
		return visRange;
	}

	int getDistance(final MarsAgent agent) {
		GoalTest<Vertex> goalTest = new GoalTest<Vertex>() {
			@Override
			public boolean isSolution(Vertex vertex, int depth) {
				return agent.getPosition().toString().equals(vertex.getId());
			}
		};

		State initialState = new Vertex(world, goalTest, position);
		AStarSolver solver = new AStarSolver();

		List<State> solutionStates = solver.solve(initialState);
		return solutionStates == null ? Integer.MAX_VALUE : solutionStates.size();
	}

	protected boolean planPathToAgent(final MarsAgent agent, Queue<String> planToAgent) {
		GoalTest<Vertex> goalTest = new GoalTest<Vertex>() {
			@Override
			public boolean isSolution(Vertex vertex, int depth) {
				return agent.getPosition().toString().equals(vertex.getId());
			}
		};

		String planPathTo = "planPathTo" + agent.getRole() + ":";
		System.out.println(planPathTo + " new plan:");

		State initialState = new Vertex(world, goalTest, position);
		AStarSolver solver = new AStarSolver();

		List<State> solutionStates = solver.solve(initialState);
		if (solutionStates == null) {
			System.out.println(planPathTo + " no plan!");
			return false;
		}

		planToAgent.clear();

		System.out.printf(planPathTo + " visited: %d\n", solver.getVisitedStateCount());
		System.out.printf(planPathTo + " expanded: %d\n", solver.getExpandedStateCount());

		solutionStates.remove(0);
		for (State state : solutionStates) {
			planToAgent.offer(state.getId());
			System.out.printf(planPathTo + " - %s\n", state.getId());
		}

		return true;
	}

	protected MarsAgent getNearbyDisabledAlly() {
		for (MarsAgent agent : world.getAgents(ALL)) {
			if (this != agent) {
				if (agent.getHealth() == 0 && (agent.getPosition().equals(getPosition())
						|| world.isAdjacent(agent.getPosition(), getPosition()))) {
					return agent;
				}
			}
		}

		return null;
	}

	protected boolean canParry()
	{
		return health != 0 && energy >= 2;
	}

	public void clearExplorationPlan() {
		explorationPlan.clear();
	}

	public void setRepairerTarget(Repairer repairerTarget) {
		this.repairerTarget = repairerTarget;
	}

	public Repairer getRepairerTarget() {
		return repairerTarget;
	}

	public void meet(MarsAgent dead, Repairer repairer) {
		repairer.enrouteToDisabledAlly = planPathToAgent(dead, repairer.enrouteToDisabledAllyPlan);
		LinkedList<String> planCopy = new LinkedList<String>(repairer.enrouteToDisabledAllyPlan);
		Collections.reverse(planCopy);
		dead.enrouteToRepairerPlan = planCopy;
		dead.enrouteToRepairer = true;
	}

	@Override
	public String toString() {
		return getName();
	}

    public int getZoneIndex() {
        if (zoneIndex == -1) {
            zoneIndex = nextZoneIndex;
            nextZoneIndex = nextZoneIndex + 1;
        }
        return zoneIndex;
    }
}
