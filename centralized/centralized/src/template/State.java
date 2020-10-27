package template;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology;
import logist.topology.Topology.City;


public class State {
	
	public static Topology topology;
	public static int numTasks;
	public static int numVehicles;
	public static List<Vehicle> vehicles;

	
	
	
	static void initStatic(Topology topology, int numTasks, List<Vehicle> vehicles) {
		State.topology = topology;
		State.numTasks = numTasks;
		State.numVehicles = vehicles.size();
		State.vehicles = vehicles;
	}
	
	
	//////////////////////////////////////////////
	/////////////////////////////////////////////
	
	public LinkedList<Task> [] taskList;
	public int[] vehicle; //Maps tasks to vehicle
	public int[] time; // Maps tasks to tasks

	
	public Task next(Vehicle t) {
		return taskList[t.id()].get(0);
	}
	
	public Task next(Task t) {
		LinkedList<Task> temp =  taskList[vehicle[t.id]];
		//TODO use iterator because this does twice as much
		
		return temp.get(temp.indexOf(t)+1);
	}

	
	
	
	
	public State(LinkedList<Task> [] tasklist, int[]vehicle, int[]time) {
		this.taskList = tasklist;
		this.vehicle = vehicle;
		this.time = time;
		
	}
	
	
	
	
	public State lookAround() {
		LinkedList<State> rv = new LinkedList<State>();
		
		double minc = Double.POSITIVE_INFINITY;
		//info on what swap to do here , its bad we know...
		int i1 = -1,i2 = -1 , i3 = -1;
		LinkedList<Task> bestlist = null;
		//this is weird but ok
		
		//
		
		
		for (int v=0; v < vehicles.size(); v++) {
			int taskSize = taskList[v].size();
			LinkedList<Task> list =  taskList[v];
			for (int j=0; j < taskSize ; j++) {
				for (int k=j+1; k < taskSize; k++) {
					//swap time

					double cost = swapPositionsEvaluate(j, k, list, false);
					if (minc > cost) {
						minc = cost;
						i1 = j;
						i2 = k;
						bestlist = list;
						
					}
					//max  calc etc.
				}
			}
		}
		
		
		for (int v=0; v < vehicles.size(); v++) {
			int taskSize = taskList[v].size();
			for (int j=0; j < taskSize ; j++) {
				for(int vNew=v+1;  vNew < vehicles.size();  vNew++) {
		
					double cost = swapVehiclesEvaluate(v, vNew, j, false);	
					if (minc > cost) {
						minc = cost;
						i1 = v;
						i2 = vNew;
						i3 = j;
					}
					
				}
			}
			
		}
		
		
		//now return the best option
		State newState = this.clone();
		double cnew;
		if (i3 == -1) {
			// 
			
			cnew = swapPositionsEvaluate(i1, i2, bestlist, true);
			
		} else {
			cnew = swapVehiclesEvaluate(i1, i2, i3, true);
			

		}
		assert(!(cnew-minc > 0.00001 && cnew-minc < -0.00001));
			
		
		return newState;
	}
	
	@Override
	public State clone() {
		int[] timeNew = time.clone();
		int[] vehNew = vehicle.clone();
		
		LinkedList<Task>[] tlnew = taskList.clone();
		
		for (int i=0; i < taskList.length; i++) {
			tlnew[i] = (LinkedList<Task>)taskList[i].clone();
			
		}
		return new State(tlnew, vehNew, timeNew);
	}
	
	double swapPositionsEvaluate(int oldPos, int newPos, LinkedList<Task> list, boolean keep) {
		int temp = time[list.get(oldPos).id];
		time[list.get(oldPos).id] = time[list.get(newPos).id];
		time[list.get(newPos).id] = temp;
		//swap the order
		Collections.swap(list, oldPos, newPos);
		

		
		//TODO compute cost here!
		double c = objectiveFunction();
		if (keep)
			return c;
		
		//undo swaps
		Collections.swap(list, oldPos, newPos);
		temp = time[list.get(oldPos).id];
		time[list.get(oldPos).id] = time[list.get(newPos).id];
		time[list.get(newPos).id] = temp;
		return c;
	}
	

	
	double swapVehiclesEvaluate(int oldV, int newV, int pos, boolean keep) {
		LinkedList<Task> listOld =  taskList[oldV];
		LinkedList<Task> ListNew =  taskList[newV];

		//remove the task from beggining
		Task t = listOld.remove(pos);
		ListNew.push(t);
		time[t.id] = ListNew.size()-1;
		vehicle[t.id] = newV;
		
		//compute the cost, save max etc, etc
		double c = objectiveFunction();
		
		if (keep)
			return c;
		
		//undo
		ListNew.removeLast();
		listOld.add(pos, t);
		time[t.id] = pos;
		vehicle[t.id] = oldV;
		
		return c;
	}
	
	double objectiveFunction() {
		double sum = 0.0;
		for (int i=0; i < State.numVehicles; i++) {
			Vehicle v =  State.vehicles.get(i);
			City currentCity = v.getCurrentCity();
			System.out.println(i+ " " + sum);
			for (Task t: taskList[i]) {
				City nextCity = t.pickupCity;
				City deliveryCity = t.deliveryCity;
				
				sum += (currentCity.distanceTo(nextCity) + nextCity.distanceTo(deliveryCity))*v.costPerKm();
				
				currentCity = deliveryCity;
			}
		}
		System.out.println(sum);
		
		return sum;
	}
	


}
