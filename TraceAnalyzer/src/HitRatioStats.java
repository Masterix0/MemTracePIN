public class HitRatioStats {
    double actualHitRatio;
    double estimatedHitRatio;
    double ptsHitRatio;

    public HitRatioStats(double actualHitRatio, double estimatedHitRatio, double ptsHitRatio) {
        this.actualHitRatio = actualHitRatio;
        this.estimatedHitRatio = estimatedHitRatio;
        this.ptsHitRatio = ptsHitRatio;
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

    public double getPTSHitRatio() {
        return ptsHitRatio;
    }

    public void setPTSHitRatio(double ptsHitRatio) {
        this.ptsHitRatio = ptsHitRatio;
    }
}
