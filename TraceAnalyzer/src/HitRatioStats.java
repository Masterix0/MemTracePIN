import java.math.BigDecimal;
import java.math.RoundingMode;

public class HitRatioStats {
    double actualHitRatio;
    double estimatedHitRatio;

    public HitRatioStats(double actualHitRatio, double estimatedHitRatio) {
        this.actualHitRatio = actualHitRatio;
        this.estimatedHitRatio = estimatedHitRatio;
    }

    public double getActualHitRatio() {
        return BigDecimal.valueOf(actualHitRatio)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public void setActualHitRatio(double actualHitRatio) {
        this.actualHitRatio = actualHitRatio;
    }

    public double getEstimatedHitRatio() {
        return BigDecimal.valueOf(estimatedHitRatio)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public void setEstimatedHitRatio(double estimatedHitRatio) {
        this.estimatedHitRatio = estimatedHitRatio;
    }
}
