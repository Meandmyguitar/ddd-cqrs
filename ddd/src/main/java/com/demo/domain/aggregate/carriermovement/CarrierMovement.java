package com.demo.domain.aggregate.carriermovement;

import java.util.Date;

public class CarrierMovement {

    private String scheduleId;
    private String fromLocationId;
    private String toLocationId;
    private Date startTime;
    private Date arriveTime;

    public CarrierMovement() {}

    public String getScheduleId() {
        return scheduleId;
    }

    public String getFromLocationId() {
        return fromLocationId;
    }

    public String getToLocationId() {
        return toLocationId;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getArriveTime() {
        return arriveTime;
    }

}
