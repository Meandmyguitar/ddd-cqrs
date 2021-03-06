package com.demo.controller;

import com.demo.application.command.CargoCmdService;
import com.demo.application.command.cmd.CargoBookCommand;
import com.demo.application.command.cmd.CargoDeleteCommand;
import com.demo.application.command.cmd.CargoDeliveryUpdateCommand;
import com.demo.application.command.cmd.CargoSenderUpdateCommand;
import com.demo.application.query.CargoQueryService;
import com.demo.application.query.dto.CargoDTO;
import com.demo.application.query.qry.CargoFindbyCustomerQry;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cargo")
public class CargoController {

    private final CargoQueryService cargoQueryService;
    private final CargoCmdService cargoCmdService;

    public CargoController(CargoQueryService cargoQueryService, CargoCmdService cargoCmdService) {
        this.cargoQueryService = cargoQueryService;
        this.cargoCmdService = cargoCmdService;
    }

    @RequestMapping(method = RequestMethod.GET)
    public List<CargoDTO> queryCargos(@RequestParam(value = "phone", required = false) String phone) {
        if (!StringUtils.isEmpty(phone)) {
            CargoFindbyCustomerQry qry = new CargoFindbyCustomerQry();
            qry.setCustomerPhone(phone);
            return cargoQueryService.queryCargos(qry);
        }
        return cargoQueryService.queryCargos();
    }

    @RequestMapping(value = "/{cargoId}", method = RequestMethod.GET)
    public CargoDTO cargo(@PathVariable String cargoId) {
        return cargoQueryService.getCargo(cargoId);
    }

    @RequestMapping(method = RequestMethod.POST)
    public void book(@RequestBody CargoBookCommand cargoBookCommand) {
        cargoCmdService.bookCargo(cargoBookCommand);
    }

    @RequestMapping(value = "/{cargoId}/delivery", method = RequestMethod.PUT)
    public void modifyDestinationLocationCode(@PathVariable String cargoId,
                                              @RequestBody CargoDeliveryUpdateCommand cmd) {
        cmd.setCargoId(cargoId);
        cargoCmdService.updateCargoDelivery(cmd);
    }

    @RequestMapping(value = "/{cargoId}/sender", method = RequestMethod.PUT)
    public void modifySender(@PathVariable String cargoId,
                             @RequestBody CargoSenderUpdateCommand cmd) {
        cmd.setCargoId(cargoId);
        cargoCmdService.updateCargoSender(cmd);
    }

    @RequestMapping(value = "/{cargoId}", method = RequestMethod.DELETE)
    public void removeCargo(@PathVariable String cargoId) {
        CargoDeleteCommand cmd = new CargoDeleteCommand();
        cmd.setCargoId(cargoId);
        cargoCmdService.deleteCargo(cmd);
    }

}
