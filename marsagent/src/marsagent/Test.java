package marsagent;

public class Test {
	
	public static void main(String[] args) {
		double[][] cost = new double[3][4];
		cost[0][0] = 2;
		cost[0][1] = 3;
		cost[0][2] = 10;
		cost[0][3] = 10;
		cost[1][0] = 3;
		cost[1][1] = 10;
		cost[1][2] = 10;
		cost[1][3] = 10;
		cost[2][0] = 3;
		cost[2][1] = 10;
		cost[2][2] = 6;
		cost[2][3] = 10;
		int[] assign = new int[3];
		HungarianAlgorithm algo = new HungarianAlgorithm(cost);
		assign=algo.execute();
	}
	
}
