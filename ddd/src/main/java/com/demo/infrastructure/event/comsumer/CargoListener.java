package com.demo.infrastructure.event.comsumer;

import com.demo.application.command.CargoCmdService;
import com.demo.domain.aggregate.cargo.CargoBookDomainEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

public class CargoListener {
    
    @Autowired
    private CargoCmdService cargoCmdService;
    @Autowired
    private EventBus eventBus;
    
    @PostConstruct
    public void init(){
        eventBus.register(this);
    }

    @Subscribe
    public void recordCargoBook(CargoBookDomainEvent event) {
        // invoke application service or domain service
        System.out.println("CargoListener: recordCargoBook......");
    }
}
