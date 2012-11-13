package edu.cwru.sepia.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State.StateView;

public class Factor {

	JMap jmap;
	Map<ActionCombination, ActionCombination> maxes = null;

	public Factor(StateView s, HistoryView h, int playerNum, LearningUnit...agents)
	{
		maxes = new HashMap<ActionCombination, ActionCombination>();

		jmap = new JMap();
		List<JMap> agentJMaps = new LinkedList<JMap>();

		// get all of the JMaps
		for(LearningUnit agent : agents)
		{
			agentJMaps.add(agent.calcJMap(s, h, playerNum));
		}

		// generate all of the combinations
		List<ActionCombination> combinations = null;
		for(JMap agentJMap : agentJMaps)
		{
			combinations = getCombinations(agentJMap, combinations);
		}

		// for each combination
		for(ActionCombination combination : combinations)
		{
			// query the JMap for the values inside of combination
			Double totVal = 0.0;
			for(JMap agentJMap : agentJMaps)
			{
				try {
					totVal += agentJMap.get(combination);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			jmap.put(combination, totVal);
		}
	}

	public Factor(StateView s, HistoryView h, int playerNum, List<LearningUnit> agents, List<Factor> factors)
	{
		maxes = new HashMap<ActionCombination, ActionCombination>();

		jmap = new JMap();
		List<JMap> agentJMaps = new LinkedList<JMap>();

		// get all of the JMaps
		for(LearningUnit agent : agents)
		{
			agentJMaps.add(agent.calcJMap(s, h, playerNum));
		}

		// generate all of the combinations
		List<ActionCombination> combinations = null;
		for(JMap agentJMap : agentJMaps)
		{
			combinations = getCombinations(agentJMap, combinations);
		}

		// add all of the combinations for the factors
		for(Factor f : factors)
		{
			combinations = getCombinations(f.jmap, combinations);
		}

		// for each combination
		for(ActionCombination combination : combinations)
		{
			// query the JMap for the values inside of the combination
			Double totVal = 0.0;
			for(JMap agentJMap : agentJMaps)
			{
				try {
					totVal += agentJMap.get(combination);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			for(Factor f : factors)
			{
				try {
					totVal += f.jmap.get(combination);
					for(ActionCombination acKey : f.maxes.keySet())
					{
						maxes.put(combination, f.maxes.get(acKey));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			jmap.put(combination, totVal);
		}
	}

	public Factor()
	{
		jmap = new JMap();
		maxes = new HashMap<ActionCombination, ActionCombination>();
	}

	private List<ActionCombination> getCombinations(JMap agentJMap, List<ActionCombination> workingList)
	{
		List<ActionCombination> newWorkingList = new LinkedList<ActionCombination>();
		List<ActionCombination> agentAC = agentJMap.getActionCombinationList();

		if(workingList == null)
			return agentAC;

		for(ActionCombination ac : agentAC)
		{
			for(ActionCombination wac : workingList)
				newWorkingList.add(new ActionCombination(ac, wac));
		}
		return newWorkingList;
	}

	public Factor max(int maxId) throws Exception
	{
		List<ActionCombination> actCombs = jmap.getActionCombinationList();

		Map<ActionCombination, List<ActionCombination>> maxBins = new HashMap<ActionCombination, List<ActionCombination>>();

		List<Integer> maxKey = new ArrayList<Integer>(1);
		maxKey.add(maxId);
		for(ActionCombination actComb : actCombs)
		{
			List<ActionCombination> splitAC = actComb.extract(maxKey);
			if(maxBins.containsKey(splitAC.get(1)))
			{
				List<ActionCombination> ac = maxBins.get(splitAC.get(1));
				ac.add(splitAC.get(0));
				maxBins.put(splitAC.get(1), ac);
			}
			else
			{
				List<ActionCombination> ac = new LinkedList<ActionCombination>();
				ac.add(splitAC.get(0));
				maxBins.put(splitAC.get(1), ac);
			}
		}

		Factor f = new Factor();

		for(ActionCombination otherAC : maxBins.keySet())
		{
			ActionCombination maxAC = null;
			Double maxVal = Double.NEGATIVE_INFINITY;
			for(ActionCombination currAC : maxBins.get(otherAC))
			{
				ActionCombination jMapKey = new ActionCombination(currAC, otherAC);
				if(jmap.get(jMapKey) > maxVal)
				{
					maxVal = jmap.get(jMapKey);
					ActionCombination prevMaxes = maxes.get(jMapKey);
					if(prevMaxes == null)
						maxAC = currAC;
					else
						maxAC = new ActionCombination(currAC, maxes.get(jMapKey));
				}
			}
			f.jmap.put(otherAC, maxVal);
			f.maxes.put(otherAC, maxAC);
		}

		return f;
	}

	public ActionCombination getMaxes()
	{
		for(ActionCombination ac : maxes.keySet())
		{
			return maxes.get(ac);
		}
		return new ActionCombination();
	}
}