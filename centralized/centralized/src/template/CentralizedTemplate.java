package template;

import java.io.File;
//the list of imports
import java.util.*;

import logist.LogistSettings;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.behavior.CentralizedBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 *
 */
@SuppressWarnings("unused")
public class CentralizedTemplate implements CentralizedBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long timeout_setup;
    private long timeout_plan;


    int[] taskMap;
    int[] prioMap;

    @Override
    public void setup(Topology topology, TaskDistribution distribution,
                      Agent agent) {

        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }

        // the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);

        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);

        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
    }

    private State solutionSearch(State init, int numIters) {

        State bestState = init;
        Boolean converged = false;
        double prevCost = bestState.objectiveFunction();
        State newState = bestState;
        State prevState = null;
        int counter = 0;
        double globalBest = prevCost;


        State.visited.add(newState);

        while( counter < numIters) {

            newState = newState.lookAround();

            double newCost  = newState.objectiveFunction();


            counter++;

//            System.out.println("New Cost" + newCost);
//            System.out.println("State:" + counter);

//            newState.printState();
            //over - search


            if (newCost < globalBest) {
                globalBest = newCost;
                bestState = newState.clone();
            }
            if ((newCost-globalBest)/globalBest > 0.1) {
                newState = bestState.clone();

            }

            prevCost = newCost;

        }
        System.out.println("The no. of states visited:" + counter);
        System.out.println("Cost:" + globalBest);
        bestState.printState();
        return bestState;
    }

    private int[] constructTaskMap(List<Vehicle> vehicles, TaskSet tasks) {
        int k = tasks.size()/vehicles.size();

        int[] tmap = new int[tasks.size()];
        int[] carried = new int[vehicles.size()];

        Arrays.fill(tmap, -1);
        Arrays.fill(carried, 0);

        int avgWeight = 0;
        for (Task t: tasks)
            avgWeight += t.weight;
        avgWeight = avgWeight/tasks.size();

        int c = 0;
        int sumWeight = 0;
        for (Vehicle v: vehicles) {
            for (Task t: tasks) {
                if (t.pickupCity == v.getCurrentCity() && tmap[t.id] == -1 && carried[v.id()] < v.capacity()) {
                    tmap[t.id] = v.id();
                    c++;
                    carried[v.id()] += t.weight;
                }
            }
        }
        this.prioMap = tmap;

        int maxCarried = tasks.size()/(avgWeight);
        tmap = new int[tasks.size()];


        int cv = 0;
        for (Task task: tasks) {
            int tid = task.id;
            if (prioMap[tid] != -1)
                continue;
            System.out.println(cv+" " +carried[cv]);
            while (cv < vehicles.size() && carried[cv] >= vehicles.get(cv).capacity())
                cv++;
            if (cv == vehicles.size())
                break;

            tmap[tid] = cv;
            carried[cv] += task.weight;
        }
        Random rand = new Random();

        for (Task task: tasks) {
            if (tmap[task.id] == -1)
                tmap[task.id] = rand.nextInt(vehicles.size());
        }
        return tmap;

    }

    private State getInitialSolution(List<Vehicle> vehicles, TaskSet tasks) {
        LinkedList<HalfTask>[] taskList = new LinkedList[vehicles.size()];
        for (int i=0; i < vehicles.size(); i++) {
            taskList[i] = new LinkedList<HalfTask>();
        }



        int[] tmap = constructTaskMap(vehicles, tasks);


        int[] vehicle = new int[tasks.size()];
        int[][] wv = null;//new int[tasks.size()][tasks.size()];


        Arrays.fill(vehicle, 0);


        for (Task task: tasks) {
            if (prioMap[task.id] == -1)
                continue;

            HalfTask h1 = new HalfTask(task, 0);
            HalfTask h2 = new HalfTask(task, 1);
            h1.otherHalf = h2;
            h2.otherHalf = h1;

            taskList[prioMap[task.id]].push(h1);
            taskList[prioMap[task.id]].add(h2);

        }

        for (Task task: tasks) {
            if (prioMap[task.id] != -1)
                continue;

            HalfTask h1 = new HalfTask(task, 0);
            HalfTask h2 = new HalfTask(task, 1);
            h1.otherHalf = h2;
            h2.otherHalf = h1;


            taskList[tmap[task.id]].add(h1);
            taskList[tmap[task.id]].add(h2);


        }

        System.out.println("Starting");


        State initialState = new State(taskList, vehicle, wv);
        return initialState;
    }


    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {

        State.initStatic(topology, tasks, vehicles);

        long time_start = System.currentTimeMillis();


        State bestSol = solutionSearch(getInitialSolution(vehicles, tasks), 80000);


        List<Plan> plans = new ArrayList<Plan>();

        double totCost = 0.0;

        for (Vehicle v: vehicles) {
            LinkedList<HalfTask> taskListOrder = bestSol.taskList[v.id()];
            City current = v.getCurrentCity();
            if (taskListOrder.size() == 0) {
                plans.add(Plan.EMPTY);
                continue;
            }

            Plan plan = new Plan(current);
            System.out.println(v.id());
            for (HalfTask ht: taskListOrder) {
                for (City intermidiate :current.pathTo(ht.city))
                    plan.appendMove(intermidiate);
                totCost += current.distanceTo(ht.city)*(v.costPerKm());


                if (ht.type == 0)
                    plan.appendPickup(ht.task);
                else
                    plan.appendDelivery(ht.task);
                current = ht.city;


            }
            System.out.println(plan);
            plans.add(plan);
        }



        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in " + duration + " milliseconds. With cost :" + totCost);

        return plans;
    }

    private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);

        for (Task task : tasks) {
            // move: current city => pickup location
            for (City city : current.pathTo(task.pickupCity)) {
                plan.appendMove(city);
            }

            plan.appendPickup(task);

            // move: pickup location => delivery location
            for (City city : task.path()) {
                plan.appendMove(city);
            }

            plan.appendDelivery(task);

            // set current city
            current = task.deliveryCity;
        }
        return plan;
    }
}