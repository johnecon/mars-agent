package marsagent;


public class Enemy {

	private int energy;
	private int visRange;
	private String role;
	private String position;
	private int health;
	private final String name;
	private final String team;
	private String status;

	public Enemy(Percept inspectionPercept) {
		name = inspectionPercept.getParam1();
		team = inspectionPercept.getParam2();
		energy = Integer.parseInt(inspectionPercept.getParam5());
		health = Integer.parseInt(inspectionPercept.getParam7());
		visRange = Integer.parseInt(inspectionPercept.getParam10());
		role = inspectionPercept.getParam3();
		position = inspectionPercept.getParam4();
	}

	public Enemy(String name, String team, String position, String status) {
		this.name = name;
		this.team = team;
		this.position = position;
		this.status = status;
	}

	public void update(Enemy enemy) {
		position = enemy.getPosition();
		role = enemy.getRole();
		energy = enemy.getEnergy();
		health = enemy.getHealth();
		visRange = enemy.getVisRange();
	}

	public boolean hasRole(String role)
	{
		return role == null ? this.role == null : (this.role == null ? false : this.role.equals(role));
	}

	public int getVisRange() {
		return visRange;
	}

	public int getHealth() {
		return health;
	}

	public int getEnergy() {
		return energy;
	}

	public String getRole() {
		return role;
	}

	public String getName() {
		return name;
	}

	public String getPosition() {
		return position;
	}

	public void update(String status, String position) {
		this.status = status;
		this.position = position;
	}

	public String getStatus() {
		return status;
	}
}
