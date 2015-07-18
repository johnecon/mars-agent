package marsagent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import spacesearch.AStarSolver;
import spacesearch.GoalTest;
import spacesearch.State;
import massim.javaagents.agents.MarsUtil;
import eis.iilang.Action;

public class Inspector extends MarsAgent {
	private boolean enrouteToUnknownEnemy = false;
	private Queue<String> enrouteToEnemyUnknownPlan = new LinkedList<String>();

	public Inspector(String name, String team) {
		super(name, team);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected boolean canProbe()
	{
		return false;
	}

	@Override
	protected boolean canParry()
	{
		return false;
	}

	@Override
	protected boolean shouldCharge() {
		return energy < 11;
	}

	@Override
	protected Action getAction() {
		// Inspect nearby enemies
		List<Enemy> enemies = new ArrayList<Enemy>();
		enemies = world.getNearbyEnemies(position);
		for (Enemy enemy : enemies) {
			if (enemy.hasRole(null)) {
				System.out.println("Inspecting nearby enemy " + enemy.getName());
				return MarsUtil.inspectAction(enemy.getName());
			}
		}

		// Plan path to unknown enemy
		if (enrouteToUnknownEnemy) {
			if (enrouteToEnemyUnknownPlan.isEmpty()) {
				enrouteToUnknownEnemy = false;
			}
		} else {
			enrouteToUnknownEnemy = planPathToUnknownEnemy();
		}

		// Execute plan
		if (enrouteToUnknownEnemy) {
			return executePlan(enrouteToEnemyUnknownPlan);
		}

		// Revert to explorer behavior
		return null;
	}

	protected boolean planPathToUnknownEnemy() {
		final Set<String> positionsWithUnknownEnemy = new HashSet<String>();
		for (Enemy enemy : world.getEnemies()) {
			if (enemy.hasRole(null)) {
				positionsWithUnknownEnemy.add(enemy.getPosition());
			}
		}

		GoalTest<Vertex> goalTest = new GoalTest<Vertex>() {
			@Override
			public boolean isSolution(Vertex vertex, int depth) {
				return positionsWithUnknownEnemy.contains(vertex.getId());
			}
		};

		System.out.println("planPathToUnknownEnemy: new plan:");

		State initialState = new Vertex(world, goalTest, position);
		AStarSolver solver = new AStarSolver();

		List<State> solutionStates = solver.solve(initialState);
		if (solutionStates == null) {
			System.out.println("planPathToUnknownEnemy: no plan!");
			return false;
		}

		enrouteToEnemyUnknownPlan.clear();

		System.out.printf("planPathToUnknownEnemy: visited: %d\n", solver.getVisitedStateCount());
		System.out.printf("planPathToUnknownEnemy: expanded: %d\n", solver.getExpandedStateCount());

		solutionStates.remove(0);
		for (State state : solutionStates) {
			enrouteToEnemyUnknownPlan.offer(state.getId());
			System.out.printf("planPathToUnknownEnemy: - %s\n", state.getId());
		}

		return true;
	}
}
