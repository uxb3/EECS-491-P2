package edu.cwru.sepia.agent;

import java.util.ArrayList;
import java.util.List;

import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State.StateView;

public class Factor {
	
	private LearningUnit max;
	private LearningUnit agent;
	private Factor f;
	
	private List<Tuple<TargetedAction, Double>> JTable;

	// for now this method assumes that there will be at most 2 edges for every node in the graph
	// so it only needs to accept a single agent and a single factor
	public Factor(StateView s, HistoryView h, int playernum, LearningUnit max, LearningUnit agent, Factor f)
	{
		// stores the J values of all the possible actions for the agent currently being
		// maximized
		List<Tuple<TargetedAction, Double>> maxJTable = max.calcJTable(s, h, playernum);
		
		// get the J values of all the possible actions for the agent
		List<Tuple<TargetedAction, Double>> agentJTable = agent.calcJTable(s, h, playernum);

		List<Tuple<TargetedAction, Double>> intermediateTable1 = constructIntermediateTable(maxJTable, agentJTable);
		
		List<Tuple<TargetedAction, Double>> intermediateTable2 = constructIntermediateTable(maxJTable, f.getJTable());
		
		List<Tuple<TargetedAction, Double>> finalActionTable = constructFinalActionTable(maxJTable.size(), intermediateTable1, intermediateTable2);
		
		JTable = compressFinalActionTable(maxJTable.size(), finalActionTable);

	}
	
	
	/* create intermediate action pair tables for each the agent
	 * 
	 * An example of an intermediate action pair table is
	 * 
	 * +--+--+---+
     * |a1|a2|J12|
     * +==+==+===+
     * |A0|A0|4  |
     * +--+--+---+
     * |A0|A1|2  |
     * +--+--+---+
     * |A1|A0|6  |
     * +--+--+---+
     * |A1|A1|8  |
     * +--+--+---+
     * 
     * Where the first two columns select the action for each agent
     * and the third column is the summation of each agents J for the given actions.
     * 
     * It is assumed that a1 will be the agent represented by max
     * and a2 will be whichever agent in agents is currently being
     * processed
     */		
	private List<Tuple<TargetedAction, Double>> constructIntermediateTable(List<Tuple<TargetedAction, Double>> a1JTable,
			                                                           List<Tuple<TargetedAction, Double>> a2JTable)
	{
	
		int numActions = a1JTable.size();
		
		// the number of actions in the intermediate table with two agents
		// is the square of the number of actions (assuming that each agent
		// has the same number of actions)
		List<Tuple<TargetedAction, Double>> intermediateTable = new ArrayList<Tuple<TargetedAction, Double>>(numActions*numActions);
		
		// think of these loops as constructing a truth table except
		// now there may be more than actions for each agent
		// (i.e. no longer just True and False)
		for(int i=0; i<a1JTable.size(); i++) 
		{
			Tuple<TargetedAction, Double> maxJVal = a1JTable.get(i); // convenience variable which saves some typing
			for(int j=0; j<a2JTable.size(); j++)
			{
				// get the index into the intermediate table
				int index = i*numActions + j;
				intermediateTable.add(new Tuple(a1JTable.get(i).first, maxJVal.second + a2JTable.get(j).second));
			}
		}
		
		return intermediateTable;
	}
	
	public List<Tuple<TargetedAction, Double>> getJTable()
	{
		return JTable;
	}

	/*
	 * Takes in two intermediate tables and combines them into a single table
	 * 
	 * For instance an intermediate table for a1, a2 and an intermediate table for
	 * a1 and a3 are combined to look like the following
	 * 
	 * +==+==+==+=+
	 * |a1|a2|a3|J|
	 * +==+==+==+=+
	 * |A0|A0|A0|3|
	 * +--+--+--+-+
	 * |A0|A0|A1|7|
	 * +--+--+--+-+
	 * |..........|
	 * +--+--+--+-+
	 * |A1|A1|A0|2|
	 * +--+--+--+-+
	 * |A1|A1|A1|9|
	 * +--+--+--+-+
	 * 
	 * However, only the J value of each row is stored.
	 * The rest of the information can be reconstructed from the index
	 * of J.
	 */
	private List<Tuple<TargetedAction, Double>> constructFinalActionTable(int numActions,
			                                   List<Tuple<TargetedAction, Double>> table1,
			                                   List<Tuple<TargetedAction, Double>> table2)
	{
		Double[] JVals = new Double[numActions*numActions*numActions];
		TargetedAction[] actions = new TargetedAction[numActions*numActions*numActions];
		
		for(int i=0; i<table1.size(); i++)
		{
			for(int j=0; j<table2.size(); j++)
			{
				if(i/numActions == j/numActions)
				{
					// the index is essentially like converting from a number in the base of Action to a number in decimal
					int index = (i/numActions)*numActions*numActions + (i % numActions)*numActions + (i % numActions);
					JVals[index] = table1.get(i).second + table2.get(j).second;
					actions[index] = table1.get(i).first;
				}
			}
		}
		
		List<Tuple<TargetedAction, Double>> finalJTable = new ArrayList<Tuple<TargetedAction, Double>>(JVals.length);
		for(int i=0; i<JVals.length; i++)
		{
			TargetedAction action = actions[i];
			finalJTable.add(new Tuple<TargetedAction, Double>(actions[i], JVals[i]));
		}
		return finalJTable;
	}
	
	/*
	 * Go through a table and find the maximum over the max agents
	 */
	private List<Tuple<TargetedAction, Double>> compressFinalActionTable(int numActions, List<Tuple<TargetedAction, Double>> finalActionTable)
	{
		Double[] JVals = new Double[numActions*numActions];
		TargetedAction[] actions = new TargetedAction[numActions*numActions];
		
		for(int i=0; i<numActions*numActions; i++)
		{
			JVals[i] = Double.NEGATIVE_INFINITY;
			for(int j=0; i<numActions; i++)
			{
				if(JVals[i] < finalActionTable.get(j*numActions*numActions).second) // if found a new bset number
				{
					// update everything
					JVals[i] = finalActionTable.get(j*numActions*numActions).second;
					actions[i] = finalActionTable.get(j*numActions*numActions).first;
				}
			}
		}
		
		List<Tuple<TargetedAction, Double>> finalJTable = new ArrayList<Tuple<TargetedAction, Double>>(JVals.length);
		for(int i=0; i<JVals.length; i++)
		{
			finalJTable.add(new Tuple<TargetedAction, Double>(actions[i], JVals[i]));
		}
		return finalJTable;
	}
	
	public void update(Factor f)
	{
		// TODO implement this method
		// The method is supposed to take in a factor
		// and using that factor compress the internal table
	}
	
	public TargetedAction selectActions()
	{
		// TODO implement this method
		// this method is supposed to return the
		// maximum action over the actions left in the JTable
		return null;
	}
}
