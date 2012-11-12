package edu.cwru.sepia.agent;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State.StateView;

public class PolicyAgent extends Agent {
	
	Map<Integer, LearningUnit> units; // used for quick lookup
	
	int turnCount = 1;
	
	int episodeCount = 1;
	
	int numEpisodes;
	
	Map<Integer, Action> actions;
	
	double[] cumRewards = new double[5];
	
	int frozenGameCount = 5; // when less than the number of slots in cumRewards units won't be updated
	
	FileWriter fstream;
	BufferedWriter out;
	
	public PolicyAgent(int playernum, String[] args) throws IOException {
		super(playernum);
		units = new HashMap<Integer, LearningUnit>();
		
		fstream = new FileWriter("learningData.csv");
		out = new BufferedWriter(fstream);
		
		if(args.length < 4)
			numEpisodes = 50;
		else
			numEpisodes = Integer.parseInt(args[3]);
	}

	@Override
	public Map<Integer, Action> initialStep(StateView newstate,
			HistoryView statehistory) {
		System.out.println("Running for " + numEpisodes);
		if(frozenGameCount < cumRewards.length)
			System.out.println("Executing frozen game " + (frozenGameCount + 1) +
					           " for episode " + episodeCount);
		else
			System.out.println("Running episode " + episodeCount);
		
		
		actions = new HashMap<Integer, Action>();
		
		// create the learning units 
		if(episodeCount == 1) // only do this for the first episode
		{
			for(Integer unitId : newstate.getUnitIds(playernum))
			{
				LearningUnit myunit = new LearningUnit(unitId);
				units.put(unitId, myunit);
			}
		}
		
		// execute the policy
		for(Integer unitId : units.keySet())
		{
			actions.put(unitId, units.get(unitId).getAction(newstate, statehistory, playernum));
		}
		// check if this is a learning episode or not
		return actions;
	}

	@Override
	public Map<Integer, Action> middleStep(StateView newstate,
			HistoryView statehistory) {
		turnCount++;
		// update the weights
		for(Integer unitId : units.keySet())
		{
			units.get(unitId).updateReward(newstate, statehistory);
		}
		
		// if this is an event step
		// then select new actions
		if(isEvent())
		{
			
			actions = new HashMap<Integer, Action>();
			
			for(Integer unitId : units.keySet())
			{
				if(frozenGameCount >= cumRewards.length) // only update when not frozen
					units.get(unitId).updateWeights();
			}
			
			for(Integer unitId : newstate.getUnitIds(playernum)) // for all of the units still alive
			{
				LearningUnit currUnit = units.get(unitId); // get the associated learning agent

				if(currUnit != null) 
				{
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
		
		// update the agents otherwise they will miss the reward from killing the last unit
		for(Integer unitId: units.keySet())
		{
			units.get(unitId).updateReward(newstate, statehistory);
			if(frozenGameCount >= 5)
				units.get(unitId).updateWeights();
		}
		
		if(frozenGameCount < 5) // this is a frozen game
		{
			for(Integer unitId : units.keySet()) // so gather the rewards
			{
				cumRewards[frozenGameCount] += units.get(unitId).getReward();
				units.get(unitId).resetReward(); // reset the reward for the next episode
			}
			frozenGameCount++;
		}
		else
		{
			if(episodeCount % 10 == 0) // if this is the end of a frozen game run
			{
				double total = 0;
				for(int i=0; i<cumRewards.length; i++)
				{
					total += cumRewards[i];
					cumRewards[i] = 0;
				}
				try {
					out.write(episodeCount + "," + total/cumRewards.length + "\n"); // save the data to a file
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			episodeCount++; // keep track of what episode the agent is on
			
			if(episodeCount % 10 == 0) // if the game should be frozen 
			{
				frozenGameCount = 0;
			}
			
			if(episodeCount > numEpisodes)
			{
				
				// close the data file
				try {
					out.close();
					fstream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				System.out.println(episodeCount-1 + " episodes run");
				System.exit(0);
			}
		}
		
	}

	@Override
	public void savePlayerData(OutputStream os) {
	}

	@Override
	public void loadPlayerData(InputStream is) {
	}
	
	private boolean isEvent()
	{
		int eventTimeout = 5; // max number of turns before new "event"
		
		if(turnCount % eventTimeout == 0)
			return true;
		
		return false;
	}
}
