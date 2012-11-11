package edu.cwru.sepia.agent;

import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State.StateView;

public interface Feature {
	
	public double calculate(StateView s, HistoryView log, TargetedAction a, int playerNum);

}
