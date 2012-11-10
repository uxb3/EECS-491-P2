package edu.cwru.sepia.agent;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.State.StateView;

public class PolicyAgent extends Agent {
	
	List<Feature> features;
	double[] weights;

	public PolicyAgent(int playernum, String[] args) {
		super(playernum);
		
	}

	@Override
	public Map<Integer, Action> initialStep(StateView newstate,
			HistoryView statehistory) {
		// TODO Auto-generated method stub
		
		// check if this is a learning episode or not
		return null;
	}

	@Override
	public Map<Integer, Action> middleStep(StateView newstate,
			HistoryView statehistory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void terminalStep(StateView newstate, HistoryView statehistory) {
		// TODO Auto-generated method stub

	}

	@Override
	public void savePlayerData(OutputStream os) {
		// TODO Auto-generated method stub

	}

	@Override
	public void loadPlayerData(InputStream is) {
		// TODO Auto-generated method stub

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	

	
	private boolean isEvent()
	{
		return false;
	}
	
	private void updateWeights()
	{
		
	}

}
