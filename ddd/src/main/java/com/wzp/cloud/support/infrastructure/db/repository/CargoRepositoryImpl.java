package com.wzp.cloud.support.infrastructure.db.repository;

import com.wzp.cloud.support.domain.aggregate.cargo.Cargo;
import com.wzp.cloud.support.domain.aggregate.cargo.CargoRepository;
import com.wzp.cloud.support.domain.aggregate.cargo.valueobject.EnterpriseSegment;
import com.wzp.cloud.support.infrastructure.db.converter.CargoConverter;
import com.wzp.cloud.support.infrastructure.db.dataobject.CargoDO;
import com.wzp.cloud.support.infrastructure.db.mapper.CargoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CargoRepositoryImpl implements CargoRepository {

    @Autowired
    private CargoMapper cargoMapper;

    @Override
    public Cargo find(String id) {
        CargoDO cargoDO = cargoMapper.select(id);
        Cargo cargo = CargoConverter.deserialize(cargoDO);
        return cargo;
    }

    @Override
    public void save(Cargo cargo) {
        CargoDO cargoDO = CargoConverter.serialize(cargo);
        CargoDO data = cargoMapper.select(cargoDO.getId());
        if (null == data) {
            cargoMapper.save(cargoDO);
        } else {
            cargoMapper.update(cargoDO);
        }
    }

    @Override
    public void remove(String id) {
        cargoMapper.remove(id);
    }

    @Override
    public int sizeByCustomer(String customerPhone) {
        return cargoMapper.countByCustomer(customerPhone);
    }

    @Override
    public int sizeByEnterpriseSegment(EnterpriseSegment enterpriseSegment) {
        // cargoMapper
        return 20;
    }

}
