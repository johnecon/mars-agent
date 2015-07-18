package marsagent;

import eis.iilang.Action;

public class Sentinel extends MarsAgent {

	public Sentinel(String name, String team) {
		super(name, team);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected boolean canProbe() {
		return false;
	}

	@Override
	protected Action getAction() {
		// TODO Auto-generated method stub
		return null;
	}

}
