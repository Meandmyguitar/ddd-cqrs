package com.demo.domain.aggregate.account;

import javax.persistence.Embeddable;

@Embeddable
public class Owner {

    private Long ownerId;

    protected Owner() {
    }

    public Owner(Long ownerId) {
        if (ownerId < 10001) {
            throw new IllegalArgumentException("ownerId should greater than 10001");
        }
        this.ownerId = ownerId;
    }


}
