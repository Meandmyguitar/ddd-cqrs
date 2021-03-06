package com.demo.infrastructure.db.converter;

import com.demo.domain.aggregate.carriermovement.CarrierMovement;
import com.demo.infrastructure.db.dataobject.CarrierMovementDO;
import org.springframework.beans.BeanUtils;

public class CarrierMovementConverter {

    public static CarrierMovementDO serialize(CarrierMovement location) {
        CarrierMovementDO target = new CarrierMovementDO();
        BeanUtils.copyProperties(location, target);
        return target;
    }

    public static CarrierMovement deserialize(CarrierMovementDO locationDO) {
        CarrierMovement target = new CarrierMovement();
        BeanUtils.copyProperties(locationDO, target);
        return target;
    }

}
