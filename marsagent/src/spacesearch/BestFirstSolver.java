package spacesearch;

import java.util.Comparator;
import java.util.PriorityQueue;

public class BestFirstSolver extends AbstractSolver {
	private PriorityQueue<State> queue = null;

	public BestFirstSolver() {
		queue = new PriorityQueue<State>(1,
			new Comparator<State>() {
				@Override
				public int compare(State s1, State s2)
				{
					// f(x) = h(x)
					return Double.compare(
						s1.getHeuristic(),
						s2.getHeuristic()
					);
				}
			}
		);
	}

	@Override
	protected void addState(State s) {
		if (!queue.contains(s)) {
			queue.offer((State)s);
		}
	}

	@Override
	protected boolean hasElements() {
		return !queue.isEmpty();
	}

	@Override
	protected State nextState() {
		return queue.remove();
	}

	@Override
	protected void clearOpen() {
		queue.clear();
	}
}
