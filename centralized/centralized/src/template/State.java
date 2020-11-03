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

    private static Random rand;

    final private static double prob = 0.7;


    static void initStatic(Topology topology, TaskSet tasks, List<Vehicle> vehicles) {
        State.topology = topology;
        State.tasks = tasks;
        State.numTasks = tasks.size();
        State.numVehicles = vehicles.size();
        State.vehicles = vehicles;
        visited = new HashSet<State>();
        rand = new Random();
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

    public LinkedList<HalfTask> [] taskList;
    public int[] vehicle; //Maps tasks to vehicle

    public int[][] weightMatrix;


    public HalfTask next(Vehicle t) {
        return taskList[t.id()].get(0);
    }

    public HalfTask next(HalfTask t) {
        LinkedList<HalfTask> temp =  taskList[vehicle[t.task.id]];
        //TODO use iterator because this does twice as much

        return temp.get(temp.indexOf(t)+1);
    }





    public State(LinkedList<HalfTask> [] tasklist, int[]vehicle,  int[][] wm) {
        this.taskList = tasklist;
        this.vehicle = vehicle;
        this.weightMatrix = wm;

    }




    public State lookAround() {

        double minc = Double.POSITIVE_INFINITY;
        //info on what swap to do here , its bad we know...
        int i1 = -1, i2 = -1, i3 = -1, i4 = -1;
        boolean swapped = true;
        //this is weird but ok

        int totalSwaps = 0;

        //calculate probability here


        int v = rand.nextInt(numVehicles);
        int taskSize = taskList[v].size();
        if (Math.random() > State.prob && taskSize > 0) {
            int v2 = (v + rand.nextInt(numVehicles-1)+1)%numVehicles;
            double cost = swapVehiclesEvaluate(v, v2, rand.nextInt(taskSize), -1, true);
            if (Double.isFinite(cost))
                return this;
        }

        int j;
        for (j = 0; j < taskSize; j++) {
            if (taskList[v].get(j).type == 1)
                continue;


            j = rand.nextInt(taskSize);

            for (int vNew = 0; vNew < vehicles.size(); vNew++) {
                if (v == vNew)
                    continue;

                    double cost = swapVehiclesEvaluate(v, vNew, j, -1, false);
                    //System.out.println(cost);

                    if (minc > cost) {
                        minc = cost;
                        i1 = v;
                        i2 = vNew;
                        i3 = j;
                        i4 = -1;

                    }
                    totalSwaps++;

            }
        }


            v = rand.nextInt(numVehicles);
            taskSize = taskList[v].size();
            LinkedList<HalfTask> list = taskList[v];
            for (j = 0; j < taskSize; j++) {
                HalfTask ht = taskList[v].get(j).otherHalf;

                int otherIndex = taskList[v].indexOf(ht);
                int limit = -1;
                if (ht.type == 0) {
                    limit = otherIndex;
                }
                for (int k = j + 1; k < taskSize && k != limit; k++) {
                    //swap time


                    double cost = swapPositionsEvaluate(j, k, v, false);
                    if (minc > cost) {
                        minc = cost;
                        i1 = j;
                        i2 = k;
                        i3 = v;
                        swapped = false;

                    }
                    totalSwaps++;
                    //max  calc etc.
                }
            }




//        System.out.println("Performed : "+ totalSwaps);
//        System.out.println("Min cost : "+minc);
        //now return the best option
        State newState = this;

        //if all neighbors are visited
        //this shouldnt really happen
        if (i1 == -1) {
            return this;
        }

        double cnew;
        if (!swapped) {
            //

            cnew = newState.swapPositionsEvaluate(i1, i2, i3, true);

        } else {
            cnew = newState.swapVehiclesEvaluate(i1, i2, i3, i4, true);


        }

        if(Math.abs(cnew-minc) > 0.00001) {
            System.out.println(cnew);
            System.out.println(minc);
            System.out.println("Something went wrong!");
        }
        return newState;
    }


    double swapPositionsEvaluate(int oldPos, int newPos, int vehiclePos, boolean keep) {
        LinkedList<HalfTask> list = taskList[vehiclePos];

        //swap the order
        Collections.swap(list, oldPos, newPos);

        HalfTask h1 = list.get(newPos);
        HalfTask h2 = list.get(oldPos);

        //this.printState();

        double c = Double.POSITIVE_INFINITY;

        //uncomment to enable visited functionality
//        if (!visited.contains(this) || keep) {
//            if (this.constraintCheck(vehiclePos))
//                c = objectiveFunction();
//            visited.add(this.clone());
//        }
        if (this.constraintCheck(vehiclePos))
            c = objectiveFunction();

        if (keep && Double.isFinite(c)) {
            //System.out.println(String.format("move (%d, %d, %d)", oldPos, newPos, vehiclePos));
            return c;
        }
        //undo swaps
        Collections.swap(list, oldPos, newPos);
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
        LinkedList<HalfTask> listOld =  taskList[oldV];
        LinkedList<HalfTask> listNew =  taskList[newV];
        if (newpos == -1)
            newpos = listNew.size();

        //remove the task from beggining
        HalfTask t = listOld.remove(pos);
        HalfTask ot = t.otherHalf;
        int otherIndex = listOld.indexOf(ot);
        listOld.remove(ot);

        int newposOther = newpos;
        if (t.type == 1) {
            listNew.add(ot);
            listNew.add(t);
        }
        else {
            listNew.add(t);
            listNew.add(ot);
        }




        vehicle[t.task.id] = newV;

        //compute the cost, save max etc, etc
        double c = Double.POSITIVE_INFINITY;

        if (constraintCheck(newV))
            c = objectiveFunction();

        if (keep&& Double.isFinite(c)) {
            //System.out.println(String.format("swap (%d, %d, %d, %d)", oldV, newV, pos, newpos));
            return c;
        }


        //undo
        listNew.remove(t);
        listNew.remove(ot);


        listOld.add(otherIndex, ot);
        listOld.add(pos, t);

        return c;
    }

    double objectiveFunction() {
        double sum = 0.0;

        for (int i=0; i < taskList.length; i++) {
            LinkedList<HalfTask> tl = taskList[i];
            Vehicle v = vehicles.get(i);
            City currentCity = v.getCurrentCity();
            for (HalfTask ht: tl) {
                sum += currentCity.distanceTo(ht.city)*v.costPerKm();
                currentCity = ht.city;

            }

        }
        return sum;
    }

    boolean constraintCheck(int vid) {
        double weight = 0.0;
        LinkedList<HalfTask> list = taskList[vid];
        double capacity = vehicles.get(vid).capacity();

        boolean[] pickedUp = new boolean[tasks.size()];
        Arrays.fill(pickedUp, Boolean.FALSE);
        for (int i=0; i < list.size(); i++) {
            HalfTask t = list.get(i);
            if (t.type == 0) {
                weight += t.task.weight;
                pickedUp[t.task.id] = true;
            } else {
                weight -= t.task.weight;
                if (pickedUp[t.task.id] == false)
                    return false;
            }
            //System.out.println(weight+" " +capacity);
            if (weight > capacity)
                return false;

        }
        return true;
    }


    @Override
    @SuppressWarnings("unchecked")
    public State clone()  {
        int[] vehNew = vehicle.clone();
        int[][] wv = null;
        if (weightMatrix != null)
            wv = weightMatrix.clone();

        LinkedList<HalfTask>[] tlnew = taskList.clone();


        for (int i=0; i < taskList.length; i++) {
            tlnew[i] = (LinkedList<HalfTask>)taskList[i].clone();

        }
        return new State(tlnew, vehNew, wv);
    }
    @Override
    public int hashCode() {
        int prime = 11;
        int rv = 3;
        for (int i=0; i < taskList.length; i++)
            for (HalfTask t: taskList[i]) {
                rv += rv*prime + (t.id*2 +t.type)*(i+1);
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