package marsagent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import spacesearch.DepthFirstSolver;
import spacesearch.GoalTest;
import spacesearch.Solver;
import spacesearch.State;

public class World {
	private final List<MarsAgent> agents = new ArrayList<MarsAgent>();
	private final Set<String> vertices = new HashSet<String>();
	private final Set<String> explored = new HashSet<String>();
	private final Queue<String> zone = new LinkedList<String>();
    private final Map<String, Map<String, Integer>> graph = new HashMap<String, Map<String, Integer>>();
	private final Map<String, Map<String, Integer>> promises = new HashMap<String, Map<String, Integer>>();
	private final Map<String, Integer> probed = new HashMap<String, Integer>();
	private final Map<String, Integer> bestProbed = new HashMap<String, Integer>();
	private static Map<String, World> instances = new HashMap<String, World>();
    private final TabuSearch zoneOptimization = new TabuSearch(this);

	public final Map<String, String> enemyLastKnownPos = new HashMap<String, String>();
	public final Map<String, String> disabledAlly = new HashMap<String, String>();
	private final Map<String, Enemy> enemies = new HashMap<String, Enemy>();

	private final String team;

	public int numberOfVertices = Integer.MAX_VALUE;

	public static int AVERAGE_EDGE_COST = 5;

	public World(String team) {
		this.team = team;
	}

	public TabuSearch getZoneOptimization () {
        return zoneOptimization;
    }

	public static World getInstance(String team) {
		if (instances.get(team) == null) {
			instances.put(team, new World(team));
		}
		return instances.get(team);
	}

	public Set<String> getVertices() {
		return Collections.unmodifiableSet(vertices);
	}

	public Set<String> getExplored() {
		return Collections.unmodifiableSet(explored);
	}

	public Map<String, Integer> getBestProbedVertices() {
		return Collections.unmodifiableMap(bestProbed);
	}

	public boolean isProbed(String vertex) {
		return probed.get(vertex) != null;
	}

	public void addProbed(String vertex, int value) {
		if (probed.get(vertex) == null) {
			probed.put(vertex, value);
		}
	}

	public void addVertex(String vertex) {
		vertices.add(vertex);
	}

	public void addExplored(String position) {
		if (explored.add(position)) {
			String key = null;

			for (Entry<String, Map<String, Integer>> entry : promises.entrySet()) {
				if (entry.getValue().containsKey(position)) {
					key = entry.getKey();
					break;
				}
			}

			if (key != null) {
				promises.remove(key);
				getAgent(key).clearExplorationPlan();
			}
		}
	}

	public void insertEdge(String src, String dst) {
		if (graph.get(src) == null) {
			graph.put(src, new HashMap<String, Integer>());
		}

		if (graph.get(dst) == null) {
			graph.put(dst, new HashMap<String, Integer>());
		}

		graph.get(src).put(dst, -1);
		graph.get(dst).put(src, -1);
	}

	public void insertEdgeWithCost(String src, String dst, int cost) {
		if (graph.get(src) == null) {
			graph.put(src, new HashMap<String, Integer>());
		}

		if (graph.get(dst) == null) {
			graph.put(dst, new HashMap<String, Integer>());
		}

		graph.get(src).put(dst, cost);
		graph.get(dst).put(src, cost);
	}

	public boolean isAdjacent(String nodeOne, String nodeTwo) {
		return getNeighbors(nodeOne) != null ? getNeighbors(nodeOne).contains(nodeTwo) : false;
	}

	public Set<String> getNeighbors(String position) {
		return graph.get(position) != null ? graph.get(position).keySet() : null;
	}

	public boolean unknownEdgeCost(String from, String to) {
		int cost = graph.get(from).get(to);
		return cost == -1;
	}

	public int getCost(String from, String to) {
		int cost = graph.get(from).get(to);
		if (cost == -1) {
			return AVERAGE_EDGE_COST;
		}

		return cost;
	}

	public void promise(String name, String id, int steps) {
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put(name, steps);
        promises.put(id, map);
	}

	public boolean hasFreePromise(String id, String name) {
		Map<String, Integer> otherName = promises.get(id);
		return otherName == null || otherName.containsKey(name);
	}

    public boolean hasPromise(String name) {
        for (Map<String,Integer> map : promises.values())
            if (map.containsKey(name))
                return true;
        return false;
    }

	public void setBestProbedVertices() {
		double average = getAverage(probed.values());
		for (Entry<String, Integer> vertex : probed.entrySet()) {
			for (Entry<String, Integer> vertexNested : probed.entrySet()) {
				if (isAdjacent(vertex.getKey(), vertexNested.getKey()) && vertex.getValue() >= average && vertexNested.getValue() >= average) {
					if (bestProbed.get(vertex.getKey()) == null) {
						bestProbed.put(vertex.getKey(), 1);
					} else {
						bestProbed.put(vertex.getKey(), bestProbed.get(vertex.getKey())+1);
					}
				}
			}
		}
	}

	public String getVertexToOccupy(MarsAgent agent) {
		LinkedHashSet<String> solution = zoneOptimization.GetBestKnownSolution();
		return (String) solution.toArray()[agent.getZoneIndex()];
	}

	private String getBestZoneVertice() {
		int max = Integer.MIN_VALUE;
		for (Entry<String, Integer>bp : bestProbed.entrySet()) {
			if (bp.getValue() > max) {
				max = bp.getValue();
			}
		}
		for (Entry<String, Integer>bp : bestProbed.entrySet()) {
			if (bp.getValue() == max) {
				String result = bp.getKey();
				bestProbed.remove(result);
				return result;
			}
		}
		return "";
	}

	private void setZone(final int length, final String start) {
		final int limit = length - 1;
		GoalTest<Vertex> goalTest = new GoalTest<Vertex>() {
			@Override
			public boolean isSolution(Vertex vertex, int depth) {
				return getNeighbors(vertex.getId()).contains(start) && depth == limit;
			}
		};

		State initialState = new Vertex(this, goalTest, start);
		Solver solver = new DepthFirstSolver();
		System.out.println("setZone: new plan:");

		List<State> solutionStates = solver.solve(initialState, limit);
		if (solutionStates == null) {
			System.out.println("setZone: no plan!");
			return;
		}

		for (State state : solutionStates) {
			zone.add(state.getId());
		}
	}

    public LinkedHashSet<String> findZone(final int length, final String start) {
        final int limit = length - 1;
        GoalTest<Vertex> goalTest = new GoalTest<Vertex>() {
            @Override
            public boolean isSolution(Vertex vertex, int depth) {
                return getNeighbors(vertex.getId()).contains(start) && depth == limit;
            }
        };

        State initialState = new Vertex(this, goalTest, start);
        Solver solver = new DepthFirstSolver();

        LinkedHashSet<String> zone = new LinkedHashSet<>();
        List<State> solutionStates = solver.solve(initialState, limit);
        if (solutionStates == null) {
            System.out.println("findZone: no plan!");
            return zone;
        }

        for (State state : solutionStates) {
            zone.add(state.getId());
        }
        return zone;
    }

	public Map<String, Integer> getProbed() {
		return Collections.unmodifiableMap(probed);
	}

	public String getNearbyEnemy(String position) {
		for (Entry<String, String> entry : enemyLastKnownPos.entrySet()) {
			if (entry.getValue().toString().equals(position)
					|| isAdjacent(entry.getValue().toString(), position)) {
				return entry.getKey();
			}
		}

		return null;
	}

	public List<Enemy> getNearbyEnemies(String position) {
        synchronized (this) {
			List<Enemy> list = new ArrayList<Enemy>();
			for (Entry<String, String> entry : enemyLastKnownPos.entrySet()) {
				if (entry.getValue().toString().equals(position)
						|| isAdjacent(entry.getValue().toString(), position)) {
					list.add(getEnemy(entry.getKey()));
				}
			}
			return list;
        }
	}


	public void addOrUpdateEnemy(Enemy enemyNew) {
		Enemy enemy = getEnemy(enemyNew.getName());
		if (enemy == null) {
			enemies.put(enemyNew.getName(), enemyNew);
		} else {
			enemy.update(enemyNew.getStatus(), enemyNew.getPosition());
		}
	}

	public Enemy getEnemy(String name) {
		return enemies.get(name);
	}

	public Collection<Enemy> getEnemies() {
		return Collections.unmodifiableCollection(enemies.values());
	}

	public void addAgent(MarsAgent marsAgent) {
		agents.add(marsAgent);
	}

	public List<MarsAgent> getAgents(String role) {
		if (role.equals(MarsAgent.ALL)) {
			return agents;
		}
		List<MarsAgent> list = new ArrayList<MarsAgent>();
		for (MarsAgent a : agents) {
			if (a.getRole().equals(role)) {
				list.add(a);
			}
		}
		return list;
	}

	public MarsAgent getAgent(String name) {
		for (MarsAgent a : agents) {
			if (a.getName().equals(name)) {
				return a;
			}
		}
		return null;
	}

	public boolean isAdjacentToHighValuedNode(String position) {
		if (probed.values().size() == 0)
			return false;
		double average = getAverage(probed.values());
		for (String p : probed.keySet()) {
			if (isAdjacent(p, position) && average < probed.get(p)) {
				return true;
			}
		}
		return false;
	}

	private double getAverage(Collection<Integer> values) {
		int sum = 0;
		for (int val : values) {
			sum += val;
		}
		return sum/values.size();

	}

	public void setEnemyInfo(List<marsagent.Percept> inspectionPercepts) {
		for (Percept p : inspectionPercepts) {
			String teamToCheck = p.getParam2();
			if (!teamToCheck.equals(team)) {
				Enemy enemyNew = new Enemy(p);
				getEnemy(enemyNew.getName()).update(enemyNew);
			}
		}
	}

	public MarsAgent getParryingAlly(Enemy enemy) {
		for (MarsAgent a : agents) {
			if (a.getParryingEnemy() != null && a.getParryingEnemy().getName().equals(enemy.getName()))
				return a;
		}
		return null;
	}

	public void decrementPromiseCounter(String name) {
        Entry<String,Map<String,Integer>> promise = null;
        for (Entry<String, Map<String,Integer>> entry : promises.entrySet())
            if (entry.getValue().containsKey(name)) {
                promise = entry;
                promises.remove(entry.getKey());
                break;
            }
        if (promise == null)
            return;
        int newCount = promise.getValue().get(name)-1;
        if (newCount == 0) {
            System.out.println("Agent broke the promise (counter).");
            return;
        }
        Map <String,Integer> newMap = new HashMap<>();
        newMap.put(name,newCount);
        promises.put(promise.getKey(),newMap);
    }

	public int getNumberOfPromises()
	{
		return promises.size();
	}

    public void breakPromise(String name) {
        for (Entry<String, Map<String,Integer>> entry : promises.entrySet())
            if (entry.getValue().containsKey(name)) {
                promises.remove(entry.getKey());
                return;
            }
    }

    public void removePromise(String position) {
        for (Entry<String, Map<String,Integer>> entry : promises.entrySet())
            if (entry.getKey().equals(position)) {
                promises.remove(position);
                return;
            }
    }

    public Map<String, Map<String, Integer>> getGraph() {
        return graph;
    }
}
