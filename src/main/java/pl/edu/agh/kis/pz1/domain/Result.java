package pl.edu.agh.kis.pz1.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@AllArgsConstructor
public class Result {
    private String methodId;
    private BigDecimal amountSpend;

    @Override
    public String toString() {
        return methodId + " " + amountSpend.setScale(2, RoundingMode.HALF_UP);
    }
}