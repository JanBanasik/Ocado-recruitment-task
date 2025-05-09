package pl.edu.agh.kis.pz1.domain;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethod {
    private String id;
    private int discount;
    private BigDecimal limit;


    private BigDecimal remainingLimit;
    private BigDecimal totalSpent = BigDecimal.ZERO;


    public void initializeRemainingLimit() {
        this.remainingLimit = this.limit;
    }


    public void addSpent(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return;
        }
        this.totalSpent = this.totalSpent.add(amount);
    }

    public void deductLimit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return;
        }
        this.remainingLimit = this.remainingLimit.subtract(amount);
    }
}