package marsagent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class InputHandler implements Runnable {
	private MarsAgent marsAgent;

	public InputHandler(MarsAgent marsAgent) {
		this.marsAgent = marsAgent;
	}

	@Override
	public void run() {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		while (true) {
			try {
				marsAgent.pathTo(reader.readLine().trim());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
