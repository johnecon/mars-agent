package spacesearch;

import java.util.Stack;

public class DepthFirstSolver extends AbstractSolver {
	private Stack<State> stack = new Stack<State>();

	@Override
	protected void addState(State s) {
		stack.push(s);
	}

	@Override
	protected boolean hasElements() {
		return !stack.empty();
	}

	@Override
	protected State nextState() {
		return stack.pop();
	}

	@Override
	protected void clearOpen() {
		stack.clear();
	}
}
