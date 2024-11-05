public class HitRatioStats {
    long numPagesAccessed;
    long numAccesses;
    double actualHitRatio;
    double estimatedHitRatio;
    double ptsHitRatio;
    double microChronosHitRatio;

    public HitRatioStats(double actualHitRatio, double estimatedHitRatio, double ptsHitRatio,
            double microChronosHitRatio,
            long numPagesAccessed, long numAccesses) {
        this.actualHitRatio = actualHitRatio;
        this.estimatedHitRatio = estimatedHitRatio;
        this.ptsHitRatio = ptsHitRatio;
        this.microChronosHitRatio = microChronosHitRatio;
        this.numPagesAccessed = numPagesAccessed;
        this.numAccesses = numAccesses;
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

    public double getMicroChronosHitRatio() {
        return microChronosHitRatio;
    }

    public void setMicroChronosHitRatio(double microChronosHitRatio) {
        this.microChronosHitRatio = microChronosHitRatio;
    }

    public long getNumPagesAccessed() {
        return numPagesAccessed;
    }

    public void setNumPagesAccessed(long numPagesAccessed) {
        this.numPagesAccessed = numPagesAccessed;
    }

    public long getNumAccesses() {
        return numAccesses;
    }

    public void setNumAccesses(long numAccesses) {
        this.numAccesses = numAccesses;
    }
}
