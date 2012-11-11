package edu.cwru.sepia.agent;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State.StateView;

public class PolicyAgent extends Agent {
	
	List<LearningUnit> units; // stores the learning units for each unit type
	Map<Integer, LearningUnit> unitLookup; // used for quick lookup
	
	int turnCount = 1;
	
	int episodeCount = 1;
	
	Map<Integer, Action> actions;

	public PolicyAgent(int playernum, String[] args) {
		super(playernum);
		units = new ArrayList<LearningUnit>();
	}

	@Override
	public Map<Integer, Action> initialStep(StateView newstate,
			HistoryView statehistory) {
		actions = new HashMap<Integer, Action>();
		unitLookup = new HashMap<Integer, LearningUnit>();
		
		// create the learning units 
		if(episodeCount == 1) // only do this for the first episode
		{
			for(Integer unitId : newstate.getUnitIds(playernum))
			{
				LearningUnit myunit = new LearningUnit(unitId);
				units.add(myunit);
				unitLookup.put(unitId, myunit);
			}
		}
		
		// execute the policy
		for(LearningUnit unit : units)
		{
			actions.put(unit.unitId, unit.getAction(newstate, statehistory, playernum));
		}
		// check if this is a learning episode or not
		return actions;
	}

	@Override
	public Map<Integer, Action> middleStep(StateView newstate,
			HistoryView statehistory) {
		turnCount++;
		// update the weights
		for(LearningUnit unit : units)
		{
			unit.updateWeights(newstate, statehistory);
		}
		
		// if this is an event step
		// then select new actions
		if(isEvent())
		{
			
			actions = new HashMap<Integer, Action>();
			
			for(Integer unitID : newstate.getUnitIds(playernum)) // for all of the units still alive
			{
				LearningUnit currUnit = unitLookup.get(unitID); // get the associated learning agent
				if(currUnit != null) 
				{
					currUnit.updateWeights(newstate, statehistory); // update the policy weights
					actions.put(currUnit.unitId, currUnit.getAction(newstate, statehistory, playernum)); // get an action for that unit
			
				}
			}
			return actions;
		}
		else
		{
			// if this isn't an event return an empty action list so the previous actions
			// continue to execute
			return new HashMap<Integer, Action>(); 
		}
		
	}

	@Override
	public void terminalStep(StateView newstate, HistoryView statehistory) {
		episodeCount++; // keep track of what episode the agent is on
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
		int eventTimeout = 10; // max number of turns before new "event"
		
		if(turnCount % eventTimeout == 0)
			return true;
		
		return false;
	}
}
