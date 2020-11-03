package template;

import logist.task.Task;
import logist.topology.Topology;

public class HalfTask {

    HalfTask(Task t, int type) {
        this.type = type;
        this.task = t;
        if (type == 0)
            city = t.pickupCity;
        else
            city = t.deliveryCity;
        symIndex = -1;
        id = t.id;
        otherHalf = null;
    }

    public int id;
    public int type;
    public int symIndex;
    Topology.City city;
    public Task task;
    public HalfTask otherHalf;


    public boolean equals(Object o) {
        HalfTask ht = (HalfTask)o;
        if (this.type != ht.type)
            return false;
        if (!this.task.equals(ht.task))
            return false;


        return true;
    }
}