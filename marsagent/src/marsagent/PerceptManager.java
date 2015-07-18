package marsagent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PerceptManager {
	private static Map<String, PerceptManager> instances = new HashMap<String, PerceptManager>();

	public static PerceptManager getInstance(String team) {
		if (instances.get(team) == null) {
			instances.put(team, new PerceptManager(team));
			instances.get(team).world = World.getInstance(team);

		}
		return instances.get(team);
	}

	private final String team;
	private final Map<String, List<Percept>> percepts = new HashMap<String, List<Percept>>();
	public World world = null;

	public PerceptManager(String team) {
		this.team = team;
	}

	public boolean isReady() {
		return getPercepts("role").size() > 0 &&
			   getPercepts("health").size() > 0;
	}

	public void addPercept(eis.iilang.Percept p) {
		Percept percept = new Percept(p);

		String name = percept.getName();
		List<Percept> perceptsWithEqualName = percepts.get(name);
		if (perceptsWithEqualName == null) {
			perceptsWithEqualName = new LinkedList<Percept>();
			percepts.put(name, perceptsWithEqualName);
		}

		perceptsWithEqualName.add(percept);
	}

	public void manageVisibleEdges() {
		for (Percept p : getPercepts("visibleEdge")) {
			String from = p.getParam1();
			String to = p.getParam2();
			world.addVertex(from);
			world.addVertex(to);
			world.insertEdge(from, to);
		}
	}

	public void manageProbedVertices() {
		for (Percept p : getPercepts("probedVertex")) {
			String probedVertex = p.getParam1();
			int value = Integer.parseInt(p.getParam2());
			world.addProbed(probedVertex, value);
		}
	}

	public void manageEnemyLastKnownPos() {
		// clear known enemies within visibility (range 1 for now)
		String origin = getPosition();
		Iterator<Map.Entry<String, String>> iter = world.enemyLastKnownPos.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, String> entry = iter.next();
			String pos = entry.getValue();

			if (pos.equals(origin) || world.isAdjacent(pos, origin)) {
				iter.remove();
			}
		}

		// update visible enemies (ignore disabled)
		for (Percept p : getPercepts("visibleEntity")) {
			if (!p.getParam3().equals(team)) {
				 if (p.getParam4().equals("normal")) {
					 world.enemyLastKnownPos.put(p.getParam1(), p.getParam2());
				 } else {
					 world.enemyLastKnownPos.remove(p.getParam1());
				 }
				world.addOrUpdateEnemy(new Enemy(p.getParam1(), p.getParam3(), p.getParam2(), p.getParam4()));
			}
		}
	}

	public void manageVertices() {
		for (Percept p : getPercepts("vertices")) {
			world.numberOfVertices = Integer.parseInt(p.getParam1());
		}
	}

	public int getEnergy() {
		return getPercepts("energy").isEmpty() ? 30 :
				Integer.parseInt(getPercepts("energy").get(0).getParam1());
	}

	public void reset() {
		percepts.clear();
	}

	public boolean isLastActionA(String status, String action) {
		return getPercepts("lastActionResult").size() > 0 &&
				getPercepts("lastAction").size() > 0 &&
				getPercepts("lastAction").get(0).getParam1() != null &&
				getPercepts("lastActionResult").get(0).getParam1().equals(status) &&
				getPercepts("lastAction").get(0).getParam1().equals(action);
	}

	public List<Percept> getPercepts(String name) {
		List<Percept> perceptsWithEqualName = percepts.get(name);
		if (perceptsWithEqualName == null) {
			return Collections.emptyList();
		}

		return perceptsWithEqualName;
	}

	public String getPosition() {
		return getPercepts("position").get(0).getParam1();
	}

	public int getHealth() {
		return Integer.parseInt(getPercepts("health").get(0).getParam1());

	}

	public String getRole() {
		return getPercepts("role").get(0).getParam1();

	}

	public int getVertexValue() {
		return Integer.parseInt(getPercepts("probedVertex").get(0).getParam2());
	}

	public int getMaxHealth() {
		return Integer.parseInt(getPercepts("maxHealth").get(0).getParam1());
	}
}
