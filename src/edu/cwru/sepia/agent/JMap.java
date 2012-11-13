package edu.cwru.sepia.agent;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JMap {

	Map<ActionCombination, Double> jmap;

	public JMap()
	{
		jmap = new HashMap<ActionCombination, Double>();
	}

	public JMap(Map<ActionCombination, Double> jmap)
	{
		this.jmap = jmap;
	}

	public Double get(ActionCombination combination) throws Exception
	{
		ActionCombination[] keys = jmap.keySet().toArray(new ActionCombination[4]);

		ActionCombination queryKey = combination.prune(keys[0]);

		Double result;
		if((result = jmap.get(queryKey)) == null)
			throw new Exception("Did not specify enough agents to query this jmap");
		return result;
	}

	public void put(ActionCombination combination, Double value)
	{
		jmap.put(combination, value);
	}

	public Set<ActionCombination> keySet()
	{
		return jmap.keySet();
	}

	public List<ActionCombination> getActionCombinationList()
	{
		return new LinkedList<ActionCombination>(jmap.keySet());
	}

}