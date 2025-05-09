package pl.edu.agh.kis.pz1.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.function.DoublePredicate;

@Getter
@Setter
@NoArgsConstructor
public class PromotionRuleData {
    private double discount;
    private DoublePredicate condition;

    public PromotionRuleData(double discount, double limit) {
        this.discount = discount;
        this.condition = x -> x <= discount;
    }

}
