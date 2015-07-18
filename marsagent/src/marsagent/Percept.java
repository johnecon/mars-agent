package marsagent;

public class Percept {
	private final String name;
	private final String param1;
	private final String param2;
	private final String param3;
	private final String param4;
	private final String param5;
	private final String param6;
	private final String param7;
	private final String param8;
	private final String param9;
	private final String param10;

	public Percept(eis.iilang.Percept p) {
		this.name = p.getName();
		this.param1 = (p.getParameters().size() > 0) ? ((Object) p.getParameters().get(0)).toString() : null;
		this.param2 = (p.getParameters().size() > 1) ? ((Object) p.getParameters().get(1)).toString() : null;
		this.param3 = (p.getParameters().size() > 2) ? ((Object) p.getParameters().get(2)).toString() : null;
		this.param4 = (p.getParameters().size() > 3) ? ((Object) p.getParameters().get(3)).toString() : null;
		this.param5 = (p.getParameters().size() > 4) ? ((Object) p.getParameters().get(4)).toString() : null;
		this.param6 = (p.getParameters().size() > 5) ? ((Object) p.getParameters().get(5)).toString() : null;
		this.param7 = (p.getParameters().size() > 6) ? ((Object) p.getParameters().get(6)).toString() : null;
		this.param8 = (p.getParameters().size() > 7) ? ((Object) p.getParameters().get(7)).toString() : null;
		this.param9 = (p.getParameters().size() > 8) ? ((Object) p.getParameters().get(8)).toString() : null;
		this.param10 = (p.getParameters().size() > 9) ? ((Object) p.getParameters().get(9)).toString() : null;
	}

	public String getName() {
		return name;
	}

	public String getParam1() {
		return param1;
	}

	public String getParam2() {
		return param2;
	}

	public String getParam3() {
		return param3;
	}

	public String getParam4() {
		return param4;
	}

	public String getParam5() {
		return param5;
	}

	public String getParam6() {
		return param6;
	}
	
	public String getParam7() {
		return param7;
	}
	
	public String getParam8() {
		return param8;
	}
	
	public String getParam9() {
		return param9;
	}
	
	public String getParam10() {
		return param10;
	}
}
