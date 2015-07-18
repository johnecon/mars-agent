package spacesearch;

import java.util.List;

public interface Solver {
	public List<State> solve(State initialState);
	public List<State> solve(State initialState, int limit);
	public int getVisitedStateCount();
	public int getExpandedStateCount();
}
