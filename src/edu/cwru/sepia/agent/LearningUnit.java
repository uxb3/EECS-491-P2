package edu.cwru.sepia.agent;

import java.util.ArrayList;
import java.util.List;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.State.StateView;

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
	
	public Action getAction(StateView state)
	{
		// follow current policy
		return null;
	}

	//ranking of the enemy being attacked in terms of how 
	private static class IsClosestEnemy implements Feature
	{

		@Override
		public double calculate(StateView s, HistoryView log, TargetedAction a) {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}
	
	private static class NumFootmenAttackingEnemy implements Feature
	{

		@Override
		public double calculate(StateView s, HistoryView log, TargetedAction a) {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}
	
	private static class EnemyType implements Feature
	{

		@Override
		public double calculate(StateView s, HistoryView log, TargetedAction a) {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}
	
	private static class ClosestTowerDistance implements Feature
	{

		@Override
		public double calculate(StateView s, HistoryView log, TargetedAction a) {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}
	
	private static class IsEnemyAttackingMe implements Feature
	{

		@Override
		public double calculate(StateView s, HistoryView log, TargetedAction a) {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}
	
	private static class HitPointsRatio implements Feature
	{

		@Override
		public double calculate(StateView s, HistoryView log, TargetedAction a) {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}
	
	private static class ClosestBallistaDistance implements Feature
	{

		@Override
		public double calculate(StateView s, HistoryView log, TargetedAction a) {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}

}
