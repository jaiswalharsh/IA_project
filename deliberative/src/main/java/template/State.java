package template;

import com.google.common.collect.Sets;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.lang.reflect.Array;
import java.util.*;


final public class State  {
    //state info
    final public City currentCity;
    final public TaskSet carriedTasks;
    final public TaskSet remainingTasks;

    //assisting variables for speed up computation
    private int currentWeight;
    private double hvalue;
    private double gvalue;
    private double fvalue;

    //meta data to help to recover the optimal solution plan
    private State prevState;
    private State goalState;
    public Set<Task> pickUpTasks;
    public Set<Task> deliveredTasks;

    //hscore
    //these will only exist for the initial state
    private HashMap<State, Double> hcost;
    static public HashMap<State, Double>gcost;


    //constructor
    public State(City city, TaskSet carriedTasks, TaskSet remainingTasks, int maxweight) {
        this.currentCity = city;
        this.carriedTasks = carriedTasks;
        this.remainingTasks = remainingTasks;

        this.goalState = null;
        this.pickUpTasks = null;
        this.deliveredTasks = null;
        this.gvalue = 0.0;
        this.hvalue = Double.POSITIVE_INFINITY;
        this.fvalue = Double.POSITIVE_INFINITY;

        this.hcost = null;
    }

    // goal state
    public boolean isGoal() {
        return remainingTasks.isEmpty() && carriedTasks.isEmpty();
    }

    // get next states
    public Vector<State> expand() {



        Set<City> currentNeighbors = State.neighbors[currentCity.id];
        Vector<State> rv = new Vector<>();


        //model all movements to new cities
        for (City neighbor : currentNeighbors) {

            //figure out the carried tasks later...
            TaskSet carriedTasksNew = getTaskSetAfterDelivery(neighbor);

            Set<Task> pickups = getPickups(neighbor);
            Set<Set<Task>> powerSet = Sets.powerSet(pickups);

            int count = 0;
            //this is really slow might require optimization
            for (Set<Task> tset: powerSet) {
                if (tset.isEmpty())
                    continue;
                TaskSet newCarriedTasks = carriedTasksNew.clone();

                newCarriedTasks.addAll(tset);

                if ((newCarriedTasks.weightSum() <= State.maxweight)) {

                        TaskSet newRemTasks = remainingTasks.clone();
                        newRemTasks.removeAll(tset);
                        State newState = new State(neighbor, newCarriedTasks, newRemTasks, this.maxweight);

                        //System.out.println(newState.currentCity);
                        newState.prevState = this;

                        rv.add(newState);

                }
            }


            State newSt = new State(neighbor, carriedTasksNew, remainingTasks.clone(), this.maxweight);
            newSt.prevState = this;

            rv.add(newSt);

        }
        return rv;
    }

    private double computeHeuristic1(State st) {
        double sum = 0.0;
        if (remainingTasks.isEmpty() && carriedTasks.isEmpty())
            return 0;
        for (Task task :st.remainingTasks) {
            //sum +=  ((task.pickupCity.distanceTo(task.deliveryCity)+st.currentCity.distanceTo(task.pickupCity))*task.weight/maxweight);
            sum +=  ((task.pickupCity.distanceTo(task.deliveryCity)+st.currentCity.distanceTo(task.pickupCity)));

        }
        double max = -1.0;

        for (Task task: st.carriedTasks) {
            double distance = task.deliveryCity.distanceTo(st.currentCity);
            if (distance > max)
                max = distance;
        }
        return sum+max;
    }

    private double computeHeuristic(State st) {
        double sum = 0.0;
        double factor = State.avgWeightRatio;
        double totalTask =
                (st.remainingTasks.size()+st.carriedTasks.size())*(State.minJobDistance + State.minCityDistance*5);
        double carriedTasks = (st.remainingTasks.size())*State.minCityDistance;

        sum = totalTask+carriedTasks;
        return sum*factor;

    }


        private Set<Task> getPickups(City c) {
        Set<Task> taskSet = new HashSet<>();
        for (Task t: remainingTasks) {
            if (t.pickupCity == c) {
                taskSet.add(t);
            }
        }
        return taskSet;
    }

    private TaskSet getTaskSetAfterDelivery(City c) {
        int count = 0;
        TaskSet copy = carriedTasks.clone();
        for (Task task : carriedTasks) {
            if (task.deliveryCity.equals(c)) {
                //System.out.println("Delivered");
                copy.remove(task);
                count++;
            }
        }

        return copy;
    }




    public Vector<State> solution() {
        if (goalState == null)
            return null;
        Vector<State> rv = new Vector<>();
        State cs = goalState;

        while (cs != this) {
            rv.add(cs);
            cs = cs.prevState;
        }
        Collections.reverse(rv);

        return rv;
    }

    public double astar() {

        PriorityQueue<State> pq = new PriorityQueue<>(new StateComparator());
        Set<State> visited = new HashSet<>();
        computeStatistics(this);

        State.globalVisited = visited;
        hcost = new HashMap<State, Double>();
        gcost = new HashMap<>();

        gcost.put(this, this.gvalue);

        pq.add(this);

        double t3 = System.currentTimeMillis();

        while (!pq.isEmpty()) {

            State state = pq.poll();

            if (visited.contains(state)) {
                continue;
            }

            visited.add(state);
            if (state.isGoal()) {
                totalTime = (System.currentTimeMillis()-t3);
                System.out.println("Explored : " + visited.size());
                goalState = state;
                break;
            }

            double t1 = System.currentTimeMillis();
            Vector<State> expanded = state.expand();
            expansionTime += (System.currentTimeMillis()-t1);

            for (State st: expanded) {

                st.gvalue = gcost.get(state) + state.currentCity.distanceTo(st.currentCity);
                if (gcost.get(st) != null&& (gcost.get(st) - st.gvalue) < 0.02) {
                    continue;
                } else if(gcost.get(st) != null) {
                    //pq.remove(st);
                }

                //perform h update
                if (hcost.get(st) == null) {
                    double t2 =  System.currentTimeMillis();
                    st.hvalue = computeHeuristic(st);
                    heuristicTime += (System.currentTimeMillis()-t2);
                    hcost.put(st, st.hvalue);
                } else {
                    st.hvalue = hcost.get(st);
                }

                //insert costs and go
                hcost.put(st, st.hvalue);
                gcost.put(st, st.gvalue);
                st.fvalue = st.hvalue+st.gvalue;

                double t = System.currentTimeMillis();
                pq.add(st);
                heapifyTime += (System.currentTimeMillis()-t);


            }

        }
        return -1.0;
    }

    public double bfs() {
        Queue<State> queue = new ArrayDeque<>();
        Set<State> visited = new HashSet<>();
        State.globalVisited = visited;

        queue.add(this);

        while (!queue.isEmpty()) {
            State state = queue.poll();
            if (visited.contains(state))
                continue;

            //never visit this state again
            visited.add(state);
            if (state.isGoal()) {
                System.out.println("Explored : " + visited.size());
                goalState = state;
                break;
            }
            for (State st: state.expand()) {
                queue.add(st);
            }

        }
        return -1.0;
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int rv = 3;

        rv += rv*(2 << 8) + carriedTasks.hashCode();
        rv += rv*(2 << 8) + remainingTasks.hashCode();

        rv = rv*prime + currentCity.id;
        return rv;
    }



    @Override
    public boolean equals(Object obj) {
        //downcasting to state, since this only be compared to states
        State st = (State) obj;
        if (st.currentCity.id != this.currentCity.id)
            return false;
        if (!st.carriedTasks.equals(this.carriedTasks)) {
            return false;
        }
        if (!st.remainingTasks.equals(this.remainingTasks))
            return false;
        return true;
    }

    //static functions
    static private Topology tp = null;
    static private Set<City>[] neighbors = null;

    static public  double heapifyTime = 0;
    static public double heuristicTime = 0;
    static public double expansionTime = 0;
    static public double totalTime = 0;
    //likely to change this into
    static private int maxweight;
    static double minJobDistance = Double.POSITIVE_INFINITY;
    static double minCityDistance = Double.POSITIVE_INFINITY;
    static double avgJobDistance = 0.0;
    static double avgCityDistance = 0.0;
    static double avgWeightRatio = 0.0;



    static Set<State> globalVisited ;

    static void setTopology(Topology t, int m) {
        tp = t;
        computeNeighbors();
        maxweight = m;

    }

    static void computeStatistics(State s) {
        double minJobDistance = Double.POSITIVE_INFINITY;
        double minCityDistance =Double.POSITIVE_INFINITY;
        double avgJobDistance = 0.0;
        double avgCityDistance = 0.0;

        double minWeight = Double.POSITIVE_INFINITY;
        double avgWeight = 0.0;

        for (int i=0; i < tp.cities().size(); i++) {
            for (int j = i+1; j < tp.cities().size(); j++) {
                double cityDistance = tp.cities().get(i).distanceTo(tp.cities().get(j));
                if (cityDistance < minCityDistance)
                    minCityDistance = cityDistance;
                avgCityDistance += cityDistance;
            }
        }

        for (Task t : s.remainingTasks) {
            double taskDist = t.pickupCity.distanceTo(t.deliveryCity);
            if (taskDist < minJobDistance)
                minJobDistance = taskDist;
            if (t.weight < minWeight)
                minWeight = t.weight;

            avgWeight += t.weight;
            avgJobDistance += taskDist;
        }

        avgJobDistance = avgJobDistance/s.remainingTasks.size();
        avgCityDistance = avgCityDistance/((tp.cities().size()-1)*(tp.cities().size()));
        avgWeight = avgWeight/s.remainingTasks.size();

        State.avgWeightRatio = avgWeight/s.maxweight;
        State.avgCityDistance = avgCityDistance;
        State.minCityDistance = minCityDistance;
        State.minJobDistance = minJobDistance;
        State.avgJobDistance = avgJobDistance;


        System.out.println(avgWeightRatio);


    }


    static void computeNeighbors() {
        neighbors = new Set[tp.cities().size()];
        for (City city : tp.cities()) {
            neighbors[city.id] = new HashSet<>();
            for (City city2 : city.neighbors())
                neighbors[city.id].add(city2);
        }
    }



    //comparator for pq
    class StateComparator implements Comparator<State> {
        public int compare(State s1, State s2) {
            if (s2 == null)
                return 1;
            if (s1.fvalue > s2.fvalue)
                return 1;
            else if (s1.fvalue < s2.fvalue)
                return -1;
            return 0;
        }
    }

}
