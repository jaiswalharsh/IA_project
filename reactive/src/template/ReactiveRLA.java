package template;

import java.util.Random;
import java.util.Scanner;

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

public class ReactiveRLA implements ReactiveBehavior {

	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;
	private int best[][];
	private Topology tp;
	private int count = 0;
//	seed = 1602005433768

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.	
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		discount = 0.95;
		this.tp = topology;
		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;

		int numCities = topology.cities().size();
		int costPerKM = agent.vehicles().get(0).costPerKm();
		best = new int[numCities][numCities];
		//TODO possibly add more abstraction here by creating a class
		//Currently this a state is (int, int) and actions are also ints
		//actions are encoded as go q(state, newcity) and the newcity is the destination
		double Q[][][] = new double[numCities][numCities][numCities];
		double V[][] = new double[numCities][numCities];

		//initialize V arbitrarily
		for(int i=0; i<V.length; i++) {
			for (int j=0; j < V.length; j++) {
				V[i][j] = 1.;
			}
		}


		// Precompute R(s, a)
		double R[][][]= new double[numCities][numCities][numCities];
		double P[][] = new double[numCities][numCities];
		for (int i=0;i<numCities; i++) {
			City from = topology.cities().get(i);
			for (int j = 0; j < numCities; j++) {
				City to = topology.cities().get(j);
				P[i][j] = td.probability(from, to);
				for (int k = 0; k < numCities; k++) {
					City newcity = topology.cities().get(k);
					double cost = from.distanceTo(to) * costPerKM;
					//if this is a valid job
					if (j == k && i != k) {
						R[i][j][k] = td.reward(from, to) - cost;
						System.out.print(R[i][j][k] + " ");
					} else if ((!from.hasNeighbor(newcity)) || i == k)
						R[i][j][k] = Double.NEGATIVE_INFINITY;
					else
						R[i][j][k] = -cost;
				}
			}
			System.out.println();

		}


		// The offline learning algorithm
		
		boolean converged = false;
		while (!converged) {
			//for each S
			printV(V);

			converged = true;
			int bestA = -1;

			for(int i=0; i<topology.cities().size(); i++) {
				for (int j =0; j < numCities; j++) {
					//for each action
					double max = Double.NEGATIVE_INFINITY;
					double Vprev = V[i][j];
					for (int a = 0; a < numCities; a++) {
						//Sanity check i is the city, j is the task and a is the target
						//if a and i arent neighbours, the cost should be -infinity
						//NOTE this shouldnt have to exist
//						if ((!topology.cities().get(i).hasNeighbor(topology.cities().get(a)) && j != a) || i == a)
//							continue;
						double reward = R[i][j][a];
						double sum = 0.;
						//cut the the sum to only amount for valid events
						for (int k = 0; k < numCities; k++) {
							sum += P[a][k]*V[a][k];
						}

						Q[i][j][a] = reward + discount*sum;
						if (i == a) {
//							System.out.println("-->"+Q[i][j][a]);
						}
						if (max <= Q[i][j][a]) {
							bestA = a;
							max = Q[i][j][a];
						}
					}
					V[i][j] = max;
					best[i][j] = bestA;
					if (Math.abs(V[i][j]-Vprev)/Vprev > 0.000005)
						converged = false;

				}
			} count++;

		}


	}
	static private void printV(double[][] table) {
		for (int i=0; i < table.length; i++) {
			for (int j=0; j < table.length; j++) {
				System.out.printf("%5.2f ", table[i][j]);
			}
			System.out.println();
		}
		System.out.printf("\n\n\n\n\n");
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;


		City currentCity = vehicle.getCurrentCity();
		City delivCity = null;
		int deliveryCityId = currentCity.id;
		if (availableTask != null) {
			System.out.println("Not null");
			delivCity = availableTask.deliveryCity;
			deliveryCityId = delivCity.id;
		}

		if (deliveryCityId != currentCity.id && best[currentCity.id][deliveryCityId] == deliveryCityId ) {
			action = new Pickup(availableTask);
		} else {
			City newCity = tp.cities().get(best[currentCity.id][deliveryCityId]);
			action = new Move(newCity);
		}
		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
			System.out.println("Count:" + count);
		}
		numActions++;
		
		return action;
	}
}
