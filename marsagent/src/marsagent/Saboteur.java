package marsagent;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import massim.javaagents.agents.MarsUtil;
import spacesearch.AStarSolver;
import spacesearch.GoalTest;
import spacesearch.State;
import eis.iilang.Action;



public class Saboteur extends MarsAgent {
	private boolean enrouteToEnemy = false;
	private final Queue<String> enrouteToEnemyPlan = new LinkedList<String>();

	public Saboteur(String name, String team) {
		super(name, team);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected boolean canProbe() {
		return false;
	}

	@Override
	protected boolean canParry() {
		return false;
	}

	@Override
	protected Action getAction() {
		// No special behavior for disabled saboteur
		if (health == 0) {
			return null;
		}

		// Attack nearby enemies
		Enemy target = null;

		List<Enemy> enemies = world.getNearbyEnemies(position);
		for (Enemy enemy : enemies) {
			if (target == null) {
				target = enemy;
			} else if (enemy.hasRole("Saboteur")) {
				target = enemy;
				break;
			} else if (enemy.hasRole("Repairer")) {
				target = enemy;
			}
		}

		if (target != null) {
			System.out.println("Attacking nearby enemy " + target.getName());

			int cost = 2;

			String enemyPosition = target.getPosition();
			if (!position.equals(enemyPosition)) {
				cost += world.getCost(position, enemyPosition);
			}

			// Recharge if energy required to attack exceeds current energy
			if (cost > energy) {
				return MarsUtil.rechargeAction();
			}
			if (target.getRole() == null ||
				target.getRole().equals("Saboteur") ||
				position.equals(enemyPosition))
				return MarsUtil.attackAction(target.getName());
		}

		// Plan path to enemy
		if (enrouteToEnemy) {
			if (enrouteToEnemyPlan.isEmpty()) {
				enrouteToEnemy = false;
			}
		} else {
			enrouteToEnemy = planPathToEnemy();
		}

		// Execute plan
		if (enrouteToEnemy) {
			return executePlan(enrouteToEnemyPlan);
		}

        // If the exploration is finished roam around
        if (doneExploring()) {
            Set<String> adjacentNodes = world.getGraph().get(position).keySet();
            int item = new Random().nextInt(adjacentNodes.size());
            String next = (adjacentNodes.toArray(new String[0]))[item];

            return goTo(next);
        }

		// Revert to explorer behavior
		return null;
	}

	protected boolean planPathToEnemy() {
		GoalTest<Vertex> goalTest = new GoalTest<Vertex>() {
			@Override
			public boolean isSolution(Vertex vertex, int depth) {
				return world.enemyLastKnownPos.containsValue(vertex.getId());
			}
		};

		System.out.println("planPathToEnemy: new plan:");

		State initialState = new Vertex(world, goalTest, position);
		AStarSolver solver = new AStarSolver();

		List<State> solutionStates = solver.solve(initialState);
		if (solutionStates == null) {
			System.out.println("planPathToEnemy: no plan!");
			return false;
		}

		enrouteToEnemyPlan.clear();

		System.out.printf("planPathToEnemy: visited: %d\n", solver.getVisitedStateCount());
		System.out.printf("planPathToEnemy: expanded: %d\n", solver.getExpandedStateCount());

		solutionStates.remove(0);
		for (State state : solutionStates) {
			enrouteToEnemyPlan.offer(state.getId());
			System.out.printf("planPathToEnemy: - %s\n", state.getId());
		}

		return true;
	}
}
