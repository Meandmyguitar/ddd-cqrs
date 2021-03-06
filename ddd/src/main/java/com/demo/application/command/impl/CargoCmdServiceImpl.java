package com.demo.application.command.impl;

import com.demo.application.command.CargoCmdService;
import com.demo.application.command.cmd.CargoBookCommand;
import com.demo.application.command.cmd.CargoDeleteCommand;
import com.demo.application.command.cmd.CargoDeliveryUpdateCommand;
import com.demo.application.command.cmd.CargoSenderUpdateCommand;
import com.demo.domain.aggregate.cargo.Cargo;
import com.demo.domain.aggregate.cargo.CargoBookDomainEvent;
import com.demo.domain.aggregate.cargo.CargoRepository;
import com.demo.domain.aggregate.cargo.valueobject.DeliverySpecification;
import com.demo.domain.aggregate.cargo.valueobject.EnterpriseSegment;
import com.demo.domain.aggregate.handlingevent.HandlingEvent;
import com.demo.domain.aggregate.handlingevent.HandlingEventRepository;
import com.demo.domain.service.CargoDomainService;
import com.demo.infrastructure.rpc.salessystem.SalersService;
import com.demo.shared.DomainEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
public class CargoCmdServiceImpl implements CargoCmdService {

    @Autowired
    private CargoRepository cargoRepository;
    @Autowired
    private HandlingEventRepository handlingEventRepository;
    @Autowired
    private CargoDomainService cargoDomainService;
    @Autowired
    private SalersService salersService;
    @Autowired
    DomainEventPublisher domainEventPublisher;

    @Override
    public void bookCargo(CargoBookCommand cargoBookCommand) {
        // create Cargo
        DeliverySpecification delivery = new DeliverySpecification(
                cargoBookCommand.getOriginLocationCode(), cargoBookCommand.getDestinationLocationCode());

        Cargo cargo = Cargo.newCargo(CargoDomainService.nextCargoId(), cargoBookCommand.getSenderPhone(),
                cargoBookCommand.getDescription(), delivery);

        // 流程编排
        int size = cargoRepository.sizeByCustomer(cargoBookCommand.getSenderPhone());
        EnterpriseSegment enterpriseSegment = salersService.deriveEnterpriseSegment(cargo);
        int sizeCargo = cargoRepository.sizeByEnterpriseSegment(enterpriseSegment);

        if (!cargoDomainService.mayAccept(size, sizeCargo, cargo)) {
            throw new IllegalArgumentException(
                    cargoBookCommand.getSenderPhone() + " cannot book cargo, exceed the limit: " + CargoDomainService.MAX_CARGO_LIMIT);
        }

        // saveCargo
        cargoRepository.save(cargo);

        // post domain event
        domainEventPublisher.publish(new CargoBookDomainEvent(cargo));
    }

    @Override
    public void updateCargoDelivery(CargoDeliveryUpdateCommand cmd) {
        // validate

        // find
        Cargo cargo = cargoRepository.find(cmd.getCargoId());

        // domain logic
        cargo.changeDelivery(cmd.getDestinationLocationCode());

        // save
        cargoRepository.save(cargo);
    }

    @Override
    public void updateCargoSender(CargoSenderUpdateCommand cmd) {

        // find
        Cargo cargo = cargoRepository.find(cmd.getCargoId());
        List<HandlingEvent> events = handlingEventRepository.findByCargo(cmd.getCargoId());

        // domain service
        cargoDomainService.updateCargoSender(cargo, cmd.getSenderPhone(), CollectionUtils.isEmpty(events) ? null : events.get(0));

        // save
        cargoRepository.save(cargo);
    }

    @Override
    public void deleteCargo(CargoDeleteCommand cmd) {
        cargoRepository.remove(cmd.getCargoId());
    }

}
