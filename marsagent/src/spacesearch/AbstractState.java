package spacesearch;

public abstract class AbstractState implements State {
	private State parent = null;
	private double distance = 0;

	public AbstractState() {
	}

	public AbstractState(State parent) {
		this.parent = parent;
		this.distance = parent.getDistance() + this.getCost();
	}

	protected void setDistance(double distance) {
		this.distance = distance;
	}

	protected int getCost() {
		return 1;
	}

	@Override
	public double getHeuristic() {
		return 0;
	}

	@Override
	public double getDistance() {
		return distance;
	}

	@Override
	public State getParent() {
		return parent;
	}
}
