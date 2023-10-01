package poker.explorer;

import poker.solver.CfrPlusTrainer;

public final class Main {
	private Main() {

	}

	public static void main(String[] args) {
		//This is a workaround for a known issue when starting JavaFX applications
		CfrPlusTrainer.startApp(args);
	}

}
