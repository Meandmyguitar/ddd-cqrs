package com.demo.application.command;


import com.demo.application.command.cmd.CargoBookCommand;
import com.demo.application.command.cmd.CargoDeleteCommand;
import com.demo.application.command.cmd.CargoDeliveryUpdateCommand;
import com.demo.application.command.cmd.CargoSenderUpdateCommand;

public interface CargoCmdService {
    
    void bookCargo(CargoBookCommand cargoBookCommand);

    void updateCargoDelivery(CargoDeliveryUpdateCommand cmd);

    void deleteCargo(CargoDeleteCommand cmd);

    void updateCargoSender(CargoSenderUpdateCommand cmd);

}
