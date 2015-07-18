package marsagent;

import java.util.HashSet;
import java.util.Set;

import spacesearch.AbstractState;
import spacesearch.GoalTest;
import spacesearch.State;

public class Vertex extends AbstractState {
	private World world;
	private GoalTest<Vertex> goalTest;
	private String id;

	public Vertex(World world, GoalTest<Vertex> goalTest, String id) {
		this.world = world;
		this.goalTest = goalTest;
		this.id = id;
	}

	public Vertex(State parent, World world, GoalTest<Vertex> goalTest, String id) {
		super(parent);

		this.world = world;
		this.goalTest = goalTest;
		this.id = id;

		setDistance(parent.getDistance() + world.getCost(parent.getId(), getId()));
	}

	@Override
	public Iterable<State> getPossibleMoves() {
		Set<State> moves = new HashSet<State>();

		Set<String> edges = this.world.getNeighbors(id);
		if (edges != null) {
			for (String neighbor : edges) {
				moves.add(new Vertex(this, this.world, goalTest, neighbor));
			}
		}

		return moves;
	}

	@Override
	public boolean isSolution() {
		return goalTest.isSolution(this, getDepth());
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof Vertex)) {
			return false;
		}

		Vertex vertex = (Vertex) o;
		return id.equals(vertex.getId());
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return String.format("Vertex (%s)", id);
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public int getDepth() {
		int depth;
		State parent = this.getParent();
		for (depth = 0; parent != null; depth++) {
			parent = parent.getParent();
		}
		return depth;
	}
}
