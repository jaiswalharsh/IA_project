package template;

/* import table */
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeTemplate implements DeliberativeBehavior {

	enum Algorithm { BFS, ASTAR }
	
	/* Environment */
	Topology topology;
	TaskDistribution td;
	
	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;


		// initialize the planner
		int capacity = agent.vehicles().get(0).capacity();

		State.setTopology(topology, capacity);

		String algorithmName = agent.readProperty("algorithm", String.class, "BFS");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
		
		// ...
	}
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			plan = astarPlan(vehicle, tasks);
			break;
		case BFS:
			// ...
			plan = bfsPlan(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}		
		return plan;
	}
	
	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}

	private Plan astarPlan(Vehicle v, TaskSet tasks) {

		State startingState = new State(v.getCurrentCity(), TaskSet.noneOf(tasks.clone()) ,tasks, v.capacity());
		startingState.astar();
		Vector<State> stateVector = startingState.solution();

		Plan plan = new Plan(v.getCurrentCity());
		State current  = startingState;

		double cost = 0.0;
		System.out.println("Heuristic Time: " + State.heuristicTime/1000);
		System.out.println("Expansion Time: " + State.expansionTime/1000);
		System.out.println("Heapify Time: " + State.heapifyTime/1000);
		System.out.println("Total Time: " + State.totalTime/1000);

		for (State st : stateVector) {

			plan.appendMove(st.currentCity);
			cost += current.currentCity.distanceTo(st.currentCity);
			//calculate Deltas for deliveries
			TaskSet deliveredTasks = TaskSet.intersectComplement(current.carriedTasks, st.carriedTasks);
			for (Task t : deliveredTasks)
				plan.appendDelivery(t);

			TaskSet pickedUpTasks = TaskSet.intersectComplement(st.carriedTasks, current.carriedTasks);
			for (Task t : pickedUpTasks)
				plan.appendPickup(t);
			current = st;
		}
		System.out.println("Cost:" + cost);



		return plan;
	}

	private Plan bfsPlan(Vehicle v, TaskSet tasks) {

		State startingState = new State(v.getCurrentCity(), TaskSet.noneOf(tasks.clone()) ,tasks, v.capacity());
		startingState.bfs();
		Vector<State> stateVector = startingState.solution();

		Plan plan = new Plan(v.getCurrentCity());
		State current  = startingState;

		double cost = 0.0;
		for (State st : stateVector) {

			plan.appendMove(st.currentCity);
			cost += current.currentCity.distanceTo(st.currentCity);
			//calculate Deltas for deliveries
			TaskSet deliveredTasks = TaskSet.intersectComplement(current.carriedTasks, st.carriedTasks);
			for (Task t : deliveredTasks)
				plan.appendDelivery(t);

			TaskSet pickedUpTasks = TaskSet.intersectComplement(st.carriedTasks, current.carriedTasks);
				for (Task t : pickedUpTasks)
					plan.appendPickup(t);
			current = st;
		}
		System.out.println("Cost:" + cost);
		return plan;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {
		
		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
		}
	}
}
