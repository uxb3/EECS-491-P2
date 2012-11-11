package edu.cwru.sepia.agent;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

public class LearningUnit {
	
	public final int unitId;
	
	final double alpha = .1; // learning rate
	final double beta = .1; 

	double e = 0; // eligibility trace
	
	List<Feature> features;
	double[] weights;
	
	
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
		
		weights = new double[features.size() + 1];
	}
	
	public void updateWeights(StateView state, HistoryView history)
	{
		// theta + alpha * reward * e
		e = calculateE();
	}
	
	private double calculateE()
	{
		// beta*e + grad log Pr(a | s, theta)
		return 0.0;
	}
	
	public Action getAction(StateView s, HistoryView log, int playerNum)
	{
		double maxJ = -10000;
		TargetedAction maxAct = null;
		
		for (Integer i:s.getPlayerNumbers())
		{
			if (i != playerNum)
			{
				for(UnitView enemy:s.getUnits(i))
				{
					TargetedAction act = (TargetedAction) TargetedAction.createCompoundAttack(unitId, enemy.getID());
					double j = calcJ(s, log, act, playerNum);
					if (j > maxJ)
					{
						maxJ = j;
						maxAct = act;
					}
				}
			}
		}
		
		return maxAct;
	}
	
	private double calcJ(StateView s, HistoryView log, TargetedAction a, int playerNum)
	{
		double j = weights[weights.length-1]; 
		for (int i = 0; i < features.size(); i++)
		{
			j += features.get(i).calculate(s, log, a, playerNum) * weights[i];
		}
		return j;
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
			int currentX = current.getXPosition();
			int currentY = current.getYPosition();
			
			int minDist = 500;
			
			for (Integer i:s.getPlayerNumbers())
			{
				if (i != playerNum)
				{
					for(UnitView enemy:s.getUnits(i))
					{
						if (enemy.getTemplateView().getName().equals("ScoutTower"));
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

}
