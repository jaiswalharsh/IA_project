package template;

import java.util.Collections;
import java.lang.Math;
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
		
		double minc = Double.POSITIVE_INFINITY;
		//info on what swap to do here , its bad we know...
		int i1 = -1,i2 = -1 , i3 = -1, i4 = -1;
		LinkedList<Task> bestlist = null;
		//this is weird but ok
		
		//
		
		
//		for (int v=0; v < vehicles.size(); v++) {
//			int taskSize = taskList[v].size();
//			LinkedList<Task> list =  taskList[v];
//			for (int j=0; j < taskSize ; j++) {
//				for (int k=j+1; k < taskSize; k++) {
//					//swap time
//
//					double cost = swapPositionsEvaluate(j, k, list, false);
//					if (minc > cost) {
//						minc = cost;
//						i1 = j;
//						i2 = k;
//						bestlist = list;
//						
//					}
//					//max  calc etc.
//				}
//			}
//		}
//		
		
		for (int v=0; v < vehicles.size(); v++) {
			int taskSize = taskList[v].size();
			for (int j=0; j < taskSize ; j++) {
				for(int vNew=v+1;  vNew < vehicles.size();  vNew++) {
					for (int k=0; k < taskList[vNew].size()+1; k++) {
						
						double cost = swapVehiclesEvaluate(v, vNew, j, k,false);
						System.out.println(cost);
	
						if (minc > cost) {
							minc = cost;
							i1 = v;
							i2 = vNew;
							i3 = j;
							i4 = k;

						}
					}
					
				}
			}
			
		}
		
		
		//now return the best option
		State newState = this; //= this.clone();
		double cnew;
		if (i3 == -1) {
			// 
			
			cnew = newState.swapPositionsEvaluate(i1, i2, bestlist, true);
			
		} else {
			cnew = newState.swapVehiclesEvaluate(i1, i2, i3, i4, true);
			
			
		}
		assert(Math.abs(cnew-minc) < 0.00001):"Assertion failed";
		
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
	

	
	double swapVehiclesEvaluate(int oldV, int newV, int pos, int newpos, boolean keep) {
		LinkedList<Task> listOld =  taskList[oldV];
		LinkedList<Task> listNew =  taskList[newV];


		//remove the task from beggining
		final Task t = listOld.remove(pos);
//		System.out.println("Old List task removed id:" + t.id);
//		if (newpos == listNew.size())
//			listNew.addLast(t);
		listNew.add(newpos,t);
		time[t.id] = newpos;
		vehicle[t.id] = newV;
		
		//compute the cost, save max etc, etc
		double c = objectiveFunction();
		
		if (keep)
			return c;
		
		//undo
		listNew.remove(newpos);
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
			for (final Task t: taskList[i]) {
				City nextCity = t.pickupCity;
				City deliveryCity = t.deliveryCity;
				
				sum += (currentCity.distanceTo(nextCity) + nextCity.distanceTo(deliveryCity))*v.costPerKm();
				
				currentCity = deliveryCity;
			}
		}
//		System.out.println(sum);
		
		return sum;
	}
	
	
	void printState(){
	
		for(int i= 0; i<this.taskList.length;i++) {
			System.out.print(i + ": ");
			for(int j=0; j<this.taskList[i].size();j++) {
				System.out.print(this.taskList[i].get(j).id+" ");
			}
			System.out.println();

		}
	}
	
	void printStateFull(){
		
		for(int i= 0; i<this.taskList.length;i++) {
			System.out.print(i + ": ");
			for(int j=0; j<this.taskList[i].size();j++) {
				System.out.print(this.taskList[i].get(j)+" ");
			}
			System.out.println();

		}
	}
	
}
