package pl.edu.agh.kis.pz1.domain;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private String id;
    private BigDecimal value;
    private List<String> promotions;

    private boolean isPaid = false;
    private BigDecimal remainingValueToPay;


    public void initializeRemainingValue() {
        this.remainingValueToPay = this.value;
    }


    public void markAsPaid() {
        this.isPaid = true;
        this.remainingValueToPay = BigDecimal.ZERO;
    }
}