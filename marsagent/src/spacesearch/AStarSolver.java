package spacesearch;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class AStarSolver extends AbstractSolver {
	private PriorityQueue<State> queue = null;
	private Map<String, State> open = new HashMap<String, State>();

	public AStarSolver() {
		queue = new PriorityQueue<State>(1,
			new Comparator<State>() {
				@Override
				public int compare(State s1, State s2) {
					// f(x) = g(x) + h(x)
					return Double.compare(
						s1.getDistance() + s1.getHeuristic(),
						s2.getDistance() + s2.getHeuristic()
					);
				}
			}
		);
	}

	@Override
	protected void addState(State s) {
		State neighbor = open.get(s.getId());

		if (neighbor == null) {
			// state not in open
			queue.offer(s);
			open.put(s.getId(), s);
		} else if (s.getDistance() < neighbor.getDistance()) {
			// update state if path cost is less
			queue.remove(neighbor);
			queue.offer(s);
		}
	}

	@Override
	protected boolean hasElements() {
		return !queue.isEmpty();
	}

	@Override
	protected State nextState() {
		State next = queue.remove();
		open.remove(next);
		return next;
	}

	@Override
	protected void clearOpen() {
		queue.clear();
		open.clear();
	}
}
