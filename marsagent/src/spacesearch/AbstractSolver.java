package spacesearch;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public abstract class AbstractSolver implements Solver {
	private Set<State> closed = new HashSet<State>();
	private int expanded = 0;

	@Override
	public List<State> solve(State initialState) {
		return solve(initialState, -1);
	}

	@Override
	public List<State> solve(State initialState, int limit) {
		closed.clear();
		clearOpen();
		expanded = 0;
		addState(initialState);
		while (hasElements()) {
			State s = nextState();
			if (s.isSolution()) {
				return findPath(s);
			}
			closed.add(s);

			if (limit != -1 && s.getDepth() >= limit) {
				continue;
			}

			Iterable<State> moves = s.getPossibleMoves();
			for (State move : moves) {
				if (!closed.contains(move)) {
					expanded++;
					addState(move);
				}
			}
		}
		return null;
	}

	@Override
	public int getVisitedStateCount() {
		return closed.size();
	}

	@Override
	public int getExpandedStateCount() {
		return expanded;
	}

	private List<State> findPath(State solution) {
		LinkedList<State> path = new LinkedList<State>();
		while (solution != null) {
			path.addFirst(solution);
			solution = solution.getParent();
		}
		return path;
	}

	protected int getDepth() {
		return -1;
	}

	protected abstract boolean hasElements();
	protected abstract State nextState();
	protected abstract void addState(State s);
	protected abstract void clearOpen();
}
