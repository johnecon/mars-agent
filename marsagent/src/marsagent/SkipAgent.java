package marsagent;

import massim.javaagents.Agent;
import massim.javaagents.agents.MarsUtil;
import eis.iilang.Action;
import eis.iilang.Percept;

public class SkipAgent extends Agent {
	public SkipAgent(String name, String team) {
		super(name, team);
	}

	@Override
	public Action step() {
		return MarsUtil.skipAction();
	}

	@Override
	public void handlePercept(Percept p) {
	}
}
