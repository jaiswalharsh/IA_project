package template;

import java.util.*;
import java.lang.Math;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;


public class State implements Cloneable{

    public static Topology topology;
    public static int numTasks;
    public static int numVehicles;
    public static List<Vehicle> vehicles;
    public static TaskSet tasks;

    public static HashSet<State> visited;

    public static HashMap<Task, Set<Task>> inDeliveryPickups;
    public static HashMap<Task, Set<Task>> inPickupPickups;




    static void initStatic(Topology topology, TaskSet tasks, List<Vehicle> vehicles) {
        State.topology = topology;
        State.tasks = tasks;
        State.numTasks = tasks.size();
        State.numVehicles = vehicles.size();
        State.vehicles = vehicles;
        visited = new HashSet<State>();

        irpCompute();
    }

    static private void irpCompute() {
        inDeliveryPickups = new HashMap<>();
        inPickupPickups = new HashMap<>();


        for (Task t: tasks) {
            List<City> dpath = t.pickupCity.pathTo(t.deliveryCity);

            Set<Task> ts = new HashSet<Task>();
            for (Task t1: tasks) {
                if (t.equals(t1))
                    continue;
                if (dpath.contains(t1.pickupCity))
                    ts.add(t1);
            }


            inDeliveryPickups.put(t, ts);

        }



    }


    //////////////////////////////////////////////
    /////////////////////////////////////////////

    public LinkedList<Task> [] taskList;
    public int[] vehicle; //Maps tasks to vehicle
    public int[] time; // Maps tasks to tasks

    public int[][] weightMatrix;


    public Task next(Vehicle t) {
        return taskList[t.id()].get(0);
    }

    public Task next(Task t) {
        LinkedList<Task> temp =  taskList[vehicle[t.id]];
        //TODO use iterator because this does twice as much

        return temp.get(temp.indexOf(t)+1);
    }





    public State(LinkedList<Task> [] tasklist, int[]vehicle, int[]time, int[][] wm) {
        this.taskList = tasklist;
        this.vehicle = vehicle;
        this.time = time;
        this.weightMatrix = wm;

    }




    public State lookAround() {

        double minc = Double.POSITIVE_INFINITY;
        //info on what swap to do here , its bad we know...
        int i1 = -1,i2 = -1 , i3 = -1, i4 = -1;
        boolean moved = false;
        //this is weird but ok

        int totalSwaps = 0;


		for (int v=0; v < vehicles.size(); v++) {
			int taskSize = taskList[v].size();
			LinkedList<Task> list =  taskList[v];
			for (int j=0; j < taskSize ; j++) {
				for (int k=j+1; k < taskSize; k++) {
					//swap time

					double cost = swapPositionsEvaluate(j, k, v, false);
					if (minc > cost) {
						minc = cost;
						i1 = j;
						i2 = k;
						i3 = v;

					}
                    totalSwaps++;
					//max  calc etc.
				}
			}
		}


        for (int v=0; v < vehicles.size(); v++) {
            int taskSize = taskList[v].size();
            for (int j=0; j < taskSize ; j++) {
                for(int vNew=v+1;  vNew < vehicles.size();  vNew++) {
                    for (int k=0; k < taskList[vNew].size()+1; k++) {

                        double cost = swapVehiclesEvaluate(v, vNew, j, k,false);
                        //System.out.println(cost);

                        if (minc > cost) {
                            minc = cost;
                            i1 = v;
                            i2 = vNew;
                            i3 = j;
                            i4 = k;
                            moved = true;

                        }
                        totalSwaps++;
                    }

                }
            }

        }

        System.out.println("Performed : "+ totalSwaps);
        System.out.println("Min weight : "+minc);
        //now return the best option
        State newState = this.clone();

        //if all neighbors are visited
        //this shouldnt really happen
        if (i1 == -1) {
            return this;
        }

        double cnew;
        if (!moved) {
            //

            cnew = newState.swapPositionsEvaluate(i1, i2, i3, true);

        } else {
            cnew = newState.swapVehiclesEvaluate(i1, i2, i3, i4, true);


        }

        assert(Math.abs(cnew-minc) < 0.00001):"Assertion failed";
        return newState;
    }


    double swapPositionsEvaluate(int oldPos, int newPos, int vehiclePos, boolean keep) {
        LinkedList<Task> list = taskList[vehiclePos];
        int temp = time[list.get(oldPos).id];
        time[list.get(oldPos).id] = time[list.get(newPos).id];
        time[list.get(newPos).id] = temp;
        //swap the order
        Collections.swap(list, oldPos, newPos);



        //TODO compute cost here!
        double c = Double.POSITIVE_INFINITY;
        if (!visited.contains(this) || keep) {
            c = objectiveFunction();
            visited.add(this.clone());
        }

        if (keep) {
            System.out.println(String.format("move (%d, %d, %d)", oldPos, newPos, vehiclePos));
            return c;
        }


        //undo swaps
        Collections.swap(list, oldPos, newPos);
        temp = time[list.get(oldPos).id];
        time[list.get(oldPos).id] = time[list.get(newPos).id];
        time[list.get(newPos).id] = temp;
        return c;
    }
    //use this to test that lists work correctly
    public static void main(String[] args) {
        LinkedList<Integer> l = new LinkedList<>();

        l.add(5);
        l.add(6);
        l.add(8);

        l.add(3, 10);
        System.out.println(l);
    }

    double swapVehiclesEvaluate(int oldV, int newV, int pos, int newpos, boolean keep) {
        LinkedList<Task> listOld =  taskList[oldV];
        LinkedList<Task> listNew =  taskList[newV];


        //remove the task from beggining
        final Task t = listOld.remove(pos);
        listNew.add(newpos,t);
        time[t.id] = newpos;
        vehicle[t.id] = newV;

        //compute the cost, save max etc, etc
        double c = Double.POSITIVE_INFINITY;
        if (!visited.contains(this) || keep) {
            c = objectiveFunction();
            visited.add(this.clone());
        }

        if (keep) {
            System.out.println(String.format("swap (%d, %d, %d, %d)", oldV, newV, pos, newpos));
            return c;
        }


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
            int weight = 0;
            int maxweight = vehicles.get(i).capacity();
            Set<Integer> carried = new HashSet<>();
            for (int j=0; j < taskList[i].size(); j++) {
                Task t = taskList[i].get(j);
                //check if tasks can picked

                weight += t.weight;
                Set<Task> possiblePickups = inDeliveryPickups.get(t);
                int rem = possiblePickups.size();
                //possibly inverse this function
                for(int k=j+1;k < taskList[i].size() && rem > 0; k++) {
                   Task t2 = taskList[i].get(k);
                   if (possiblePickups.contains(t2) && (t2.weight + weight < maxweight)) {
                       carried.add(t2.id);
                       weight += t2.weight;
                       rem--;
                   }
                }




                City nextCity = t.pickupCity;
                City deliveryCity = t.deliveryCity;
                if (carried.contains(t.id)) {
                    sum += currentCity.distanceTo(deliveryCity)*v.costPerKm();
                    carried.remove(t);

                }
                else
                    sum += (currentCity.distanceTo(nextCity) + nextCity.distanceTo(deliveryCity))*v.costPerKm();

                weight -= t.weight;
                currentCity = deliveryCity;
            }
        }
//		System.out.println(sum);

        return sum;
    }


    @Override
    @SuppressWarnings("unchecked")
    public State clone()  {
        int[] timeNew = time.clone();
        int[] vehNew = vehicle.clone();
        int[][] wv = null;
        if (weightMatrix != null)
            wv = weightMatrix.clone();

        LinkedList<Task>[] tlnew = taskList.clone();


        for (int i=0; i < taskList.length; i++) {
            tlnew[i] = (LinkedList<Task>)taskList[i].clone();

        }
        return new State(tlnew, vehNew, timeNew, wv);
    }
    @Override
    public int hashCode() {
        int prime = 11;
        int rv = 3;
        for (int i=0; i < taskList.length; i++)
            for (Task t: taskList[i]) {
                rv += rv*prime + t.id*(i+1);
            }
        return rv;
    }
    @Override
    public boolean equals(Object o) {
        State s = (State)o;
        for (int i=0; i < this.taskList.length; i++) {
            if(!taskList[i].equals(s.taskList[i]))
                return false;
        }
        return true;
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