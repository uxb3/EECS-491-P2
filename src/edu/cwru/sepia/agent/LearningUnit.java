package edu.cwru.sepia.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.history.DamageLog;
import edu.cwru.sepia.environment.model.history.DeathLog;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

public class LearningUnit {
	
	public final int unitId;
	
	final double alpha = .001; // learning rate
	final double beta = .5; 
	
	private double temperature;
	private double reward;
	private double[] e; // eligibility trace
	
	private TargetedAction currentAction = null;
	
	private List<Feature> features;
	private double[] weights;
	
	
	public LearningUnit(int unitId)
	{
		this.unitId = unitId;
		
		features = new ArrayList<Feature>();
		
		// add features here
		features.add(new IsClosestEnemy());
		features.add(new NumFootmenAttackingEnemy());
		features.add(new EnemyType());
		features.add(new ClosestTowerDistance());
		features.add(new IsEnemyAttackingMe());
		features.add(new HitPointsRatio());
		features.add(new ClosestBallistaDistance());
		features.add(new EnemyInTowerRange());
		
		weights = new double[features.size() + 1];
		for (int i = 0; i < weights.length; i++)
		{
			weights[i] = Math.random()*2 - 1;
		}
		
		e = new double[features.size()];
		
		temperature = 1000;
	}
	
	public void updateWeights()
	{
		// theta + alpha * reward * e
		for (int i = 0; i < e.length; i++)
		{
			weights[i] += alpha * (reward-0.1) * e[i];
			continue;
		}
		reward = 0;
	}
	
	public void updateReward(StateView state, HistoryView history)
	{
		List<DamageLog> damage = history.getDamageLogs(state.getTurnNumber()-1);
		List<DeathLog> death = history.getDeathLogs(state.getTurnNumber()-1);
		
		int targetID = -1;
		if (currentAction != null)
		{
			targetID = currentAction.getTargetId();
		}
		
		for (DamageLog log:damage)
		{
			if (log.getAttackerID() == unitId)
			{
				reward += log.getDamage();
			}
			else if (log.getDefenderID() == unitId)
			{
				reward -= log.getDamage();
			}
		}
		
		for (DeathLog log:death)
		{
			if (log.getDeadUnitID() == unitId)
			{
				reward -= 100;
			}
			else if (log.getDeadUnitID() == targetID)
			{
				reward += 100;
			}
		}
	}
	
	public double getReward()
	{
		return reward;
	}
	
	public void resetReward()
	{
		reward = 0;
	}
	
	public void resetE()
	{
		//e = new double[features.size()];
	}
	
	public List<Tuple<TargetedAction, Double>> getActions(StateView s, HistoryView h, int playerNum)
	{
		List<Tuple<TargetedAction,Double>> actions = new LinkedList<Tuple<TargetedAction,Double>>();
		double valueSum = 0;
		for (Integer i:s.getPlayerNumbers())
		{
			if (i != playerNum)
			{
				for(UnitView enemy:s.getUnits(i))
				{
					TargetedAction act = (TargetedAction) TargetedAction.createCompoundAttack(unitId, enemy.getID());
					double j = Math.exp((calcJ(s, h, act, playerNum)/temperature));
					Tuple<TargetedAction, Double> actValue = new Tuple<TargetedAction, Double>(act,j);
					actions.add(actValue);
					valueSum += j;
				}
			}
		}

		for (Tuple<TargetedAction, Double> t:actions)
		{
			t.second = t.second/valueSum;
		}

		return actions;
	}
	
	public Action getAction(StateView s, HistoryView log, int playerNum, boolean learning)
	{
		TargetedAction chosenAction = null;
		
		List<Tuple<TargetedAction, Double>> actions = getActions(s, log, playerNum);
		
		double rand = Math.random();
		
		for (Tuple<TargetedAction, Double> t:actions)
		{
			if (t.second<rand)
			{
				rand -= t.second;
			}
			else
			{
				chosenAction = t.first;
				break;
			}
		}
		
		if (chosenAction == null)
		{
			chosenAction = actions.get(actions.size()-1).first;
		}
		if (learning)
		{
			for (int i = 0; i < features.size(); i++)
			{
				double chosenF = 0;
				double sumF = 0;
				
				for (Tuple<TargetedAction, Double> t:actions)
				{
					double F = features.get(i).calculate(s, log, t.first, playerNum);
					if (t.first == chosenAction)
					{
						chosenF = F;
					}
					sumF += t.second*F;
				}
				
				e[i] = beta*e[i] + chosenF - sumF;
			}

			temperature *= .997;
		}
		currentAction = chosenAction;
		return chosenAction;
	}
	
	private double calcJ(StateView s, HistoryView log, TargetedAction a, int playerNum)
	{
		//double j = weights[weights.length-1]; 
		double j = 0;
		for (int i = 0; i < features.size(); i++)
		{
			j += features.get(i).calculate(s, log, a, playerNum) * weights[i];
		}
		return j;
	}
	
	public JMap calcJMap(StateView s, HistoryView log, int playerNum)
	{
		Map<ActionCombination, Double> jmap = new HashMap<ActionCombination, Double>();

		List<Tuple<TargetedAction, Double>> actions = getActions(s, log, playerNum);
		for(Tuple<TargetedAction, Double> action : actions)
		{
			ActionCombination ac = new ActionCombination(new Tuple<Integer, TargetedAction>(unitId, action.first));
			jmap.put(ac, action.second);
		}

		return new JMap(jmap);
	}

	//ranking of the enemy being attacked in terms of how close the enemy is
	private static class IsClosestEnemy implements Feature
	{

		@Override
		public double calculate(StateView s, HistoryView log, TargetedAction a, int playerNum) 
		{
			int targetID = a.getTargetId();
			UnitView target = s.getUnit(targetID);
			int currentID = a.getUnitId();
			UnitView current = s.getUnit(currentID);
			
			if (target == null || current == null)
			{
				return 0;
			}
			
			int targetDistance = distance(current.getXPosition(), current.getYPosition(), target.getXPosition(), target.getYPosition());
			int rank = 1;
			
			for (Integer i:s.getPlayerNumbers())
			{
				if (i != playerNum)
				{
					for(UnitView enemy:s.getUnits(i))
					{
						if (enemy.getID() != targetID);
						{
							if (distance(current.getXPosition(), current.getYPosition(), enemy.getXPosition(), enemy.getYPosition()) < targetDistance)
							{
								rank++;
							}
						}
					}
				}
			}
			
			return rank;
		}
		
		private int distance(int x1, int y1, int x2, int y2)
		{
			return Math.max(Math.abs(x1-x2), Math.abs(y1-y2));
		}
		
	}
	
	//number of friendly attacking the targeted enemy
	private static class NumFootmenAttackingEnemy implements Feature
	{

		@Override
		public double calculate(StateView s, HistoryView log, TargetedAction a, int playerNum) 
		{
			int targetID = a.getTargetId();

			int count = 1;
			
			for(UnitView friendly:s.getUnits(playerNum))
			{
				TargetedAction act = (TargetedAction)friendly.getCurrentDurativeAction();
				if (act != null && friendly.getID() != a.getUnitId())
				{
					if (act.getTargetId() == targetID)
					{
						count++;
					}
				}
			}
			
			return count;
		}
		
	}
	
	//the type of enemy, 0 for footmen, 1 for tower
	private static class EnemyType implements Feature
	{

		@Override
		public double calculate(StateView s, HistoryView log, TargetedAction a, int playerNum) 
		{
			UnitView target = s.getUnit(a.getTargetId());
			
			if (target == null)
			{
				return 0;
			}
			
			if (target.getTemplateView().getName().equals("ScoutTower"))
				return 1;
			else
				return 0;
		}
		
	}
	
	//the distance to the closest tower, if no towers exist, a large number is returned
	private static class ClosestTowerDistance implements Feature
	{

		@Override
		public double calculate(StateView s, HistoryView log, TargetedAction a, int playerNum) 
		{
			UnitView current = s.getUnit(a.getUnitId());
			
			if (current == null)
			{
				return 0;
			}
			
			int currentX = current.getXPosition();
			int currentY = current.getYPosition();
			
			int minDist = 500;
			
			for (Integer i:s.getPlayerNumbers())
			{
				if (i != playerNum)
				{
					for(UnitView enemy:s.getUnits(i))
					{
						if (enemy.getTemplateView().getName().equals("ScoutTower"))
						{
							int dist = distance(currentX, currentY, enemy.getXPosition(), enemy.getYPosition());
							if (dist < minDist)
							{
								minDist = dist;
							}
						}
					}
				}
			}
			
			return minDist;
		}
		
		private int distance(int x1, int y1, int x2, int y2)
		{
			return Math.max(Math.abs(x1-x2), Math.abs(y1-y2));
		}
		
	}
	
	//returns 1 if the target enemy is attacking the current unit
	private static class IsEnemyAttackingMe implements Feature
	{

		@Override
		public double calculate(StateView s, HistoryView log, TargetedAction a, int playerNum) 
		{
			UnitView enemy = s.getUnit(a.getTargetId());
			
			if (enemy == null)
			{
				return 0;
			}
			
			TargetedAction act = (TargetedAction) enemy.getCurrentDurativeAction();
			
			if (act != null)
			{
				if (act.getTargetId() == a.getUnitId())
				{
					return 1;
				}
			}
			return 0;
		}
	}
	
	//returns current unit hp by enemy unit hp
	private static class HitPointsRatio implements Feature
	{

		@Override
		public double calculate(StateView s, HistoryView log, TargetedAction a, int playerNum) 
		{
			UnitView current = s.getUnit(a.getUnitId());
			UnitView enemy = s.getUnit(a.getTargetId());
			
			if (current == null || enemy == null)
			{
				return 0;
			}
			
			if (enemy.getHP() == 0)
			{
				return 1;
			}
			
			return ((double)current.getHP())/enemy.getHP();
		}
		
	}
	
	//distance to closest ballista
	private static class ClosestBallistaDistance implements Feature
	{

		@Override
		public double calculate(StateView s, HistoryView log, TargetedAction a, int playerNum) 
		{
			UnitView current = s.getUnit(a.getUnitId());
			
			if (current == null)
			{
				return 0;
			}
			
			int currentX = current.getXPosition();
			int currentY = current.getYPosition();
			
			int minDist = 100;
			
			for(UnitView friendly:s.getUnits(playerNum))
			{
				if (friendly.getTemplateView().getName().equals("Ballista"))
				{
					int dist = distance(currentX, currentY, friendly.getXPosition(), friendly.getYPosition());
					if (dist < minDist)
					{
						minDist = dist;
					}
				}
			}
			
			return minDist;
		}
		
		private int distance(int x1, int y1, int x2, int y2)
		{
			return Math.max(Math.abs(x1-x2), Math.abs(y1-y2));
		}
		
	}
	
	private static class EnemyInTowerRange implements Feature
	{
		@Override
		public double calculate(StateView s, HistoryView log, TargetedAction a, int playerNum)
		{
			UnitView currentEnemy = s.getUnit(a.getTargetId());
			
			if (currentEnemy == null)
			{
				return 0;
			}
			
			int x = currentEnemy.getXPosition();
			int y = currentEnemy.getYPosition();
			
			for (Integer i:s.getPlayerNumbers())
			{
				if (i != playerNum)
				{
					for(UnitView enemy:s.getUnits(i))
					{
						if (enemy.getTemplateView().getName().equals("ScoutTower"))
						{
							int range = enemy.getTemplateView().getRange();
							int dist = distance(x,y,enemy.getXPosition(),enemy.getYPosition());
							UnitView current = s.getUnit(a.getUnitId());
							if (current == null)
							{
								return 0;
							}
							if (dist + current.getTemplateView().getRange() <= range + 1)
							{
								return 1;
							}
						}
					}
				}
			}
			return 0;
		}
		
		private int distance(int x1, int y1, int x2, int y2)
		{
			return Math.max(Math.abs(x1-x2), Math.abs(y1-y2));
		}
	}

	
}
