package template;

import java.util.Random;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveTemplate implements ReactiveBehavior {

	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;
	private int best[];

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.	
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;
		
		int numCities = topology.cities().size();
		int Q[][] = new int[numCities][2];
		int V[]= new int[topology.cities().size()];
//		for(int i=0; i<V.length; i++) {
//			V[i]=1;
//		}
		double R[][]= new double[numCities][2];
		for (int i=0;i<numCities; i++) {
			for (int j=0;j<numCities;j++) {
				R[i][j]=0;
				City from = topology.cities().get(i);
				City to = topology.cities().get(j);
				R[i][j] += td.reward(from, to);
			}
		}
		
		int count = 50;
// The offline learning algorithm
		
		while (count>1) {
			count--;
			for(int i=0; i<topology.cities().size(); i++) {
				for (int a =0; a<2; a++) {
					V[i] = 1;
					City from = topology.cities().get(i);
					City to = topology.cities().get(a);
					for (int j=0;j<numCities;j++) {
						
					}
					Q[i][a] += R[i][a] + discount * td.probability(from, to)* V[i];
				}
			}
		}
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
	
		
		
		if (availableTask == null || random.nextDouble() > pPickup) {
			City currentCity = vehicle.getCurrentCity();
			action = new Move(currentCity.randomNeighbor(random));
		} else {
			action = new Pickup(availableTask);
		}
		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}
}
