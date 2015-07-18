package spacesearch;

import java.util.LinkedList;
import java.util.Queue;

public class BreadthFirstSolver extends AbstractSolver {
	private Queue<State> queue = new LinkedList<State>();

	@Override
	protected void addState(State s) {
		if (!queue.contains(s)) {
			queue.offer(s);
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
