package com.demo.domain.aggregate.account;

import javax.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public class Amount {

    private BigDecimal amount;

    protected Amount() {
    }

    public Amount(BigDecimal amount) {
        if (null == amount || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("amount argument error");
        }
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
