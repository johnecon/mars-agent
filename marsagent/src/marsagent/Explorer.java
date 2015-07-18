package marsagent;

import eis.iilang.Action;

public class Explorer extends MarsAgent {

	public Explorer(String name, String team) {
		super(name, team);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected boolean canProbe() {
		return energy >= 1;
	}

	@Override
	protected boolean canParry()
	{
		return false;
	}

	@Override
	protected Action getAction() {
		// TODO Auto-generated method stub
		return null;
	}
}
