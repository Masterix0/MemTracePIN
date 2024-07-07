public class HitRatioStats {
    double actualHitRatio;
    double estimatedHitRatio;

    public HitRatioStats(double actualHitRatio, double estimatedHitRatio) {
        this.actualHitRatio = actualHitRatio;
        this.estimatedHitRatio = estimatedHitRatio;
    }

    public double getActualHitRatio() {
        return actualHitRatio;
    }

    public void setActualHitRatio(double actualHitRatio) {
        this.actualHitRatio = actualHitRatio;
    }

    public double getEstimatedHitRatio() {
        return estimatedHitRatio;
    }

    public void setEstimatedHitRatio(double estimatedHitRatio) {
        this.estimatedHitRatio = estimatedHitRatio;
    }
}
