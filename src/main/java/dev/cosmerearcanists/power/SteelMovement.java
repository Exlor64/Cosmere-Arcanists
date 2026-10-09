package dev.cosmerearcanists.power;

/** Nonnegative anchor forces; never invents a force direction that an anchor cannot supply. */
public final class SteelMovement {
    public static double[] weights(double[] desired, double[][] directions, double[] strengths, double minimumAlignment) {
        double[] result = new double[directions.length];
        double[] residual = desired.clone();
        double budget = 1;
        for (int pass = 0; pass < 4 && budget > 1e-6; pass++) {
            int best = -1; double score = 0;
            for (int i = 0; i < directions.length; i++) {
                double alignment = dot(desired, directions[i]);
                double capacity = Math.pow((alignment - minimumAlignment) / (1 - minimumAlignment), 2);
                if (alignment <= minimumAlignment || strengths[i] <= 0 || result[i] >= capacity - 1e-6) continue;
                double candidate = dot(residual, directions[i]) * strengths[i] * (alignment - minimumAlignment) / (1 - minimumAlignment);
                if (candidate > score) { best = i; score = candidate; }
            }
            if (best < 0) break;
            double alignment = dot(desired, directions[best]);
            double capacity = Math.pow((alignment - minimumAlignment) / (1 - minimumAlignment), 2);
            double amount = Math.min(Math.min(budget, capacity - result[best]), Math.max(0, dot(residual, directions[best]))
                * (alignment - minimumAlignment) / (1 - minimumAlignment));
            if (amount < 1e-6) break;
            result[best] += amount; budget -= amount;
            for (int axis = 0; axis < 3; axis++) residual[axis] -= directions[best][axis] * amount;
        }
        double[] supplied = new double[3];
        for (int i=0;i<result.length;i++) for (int axis=0;axis<3;axis++) supplied[axis] += result[i]*strengths[i]*directions[i][axis];
        double length = Math.sqrt(dot(supplied,supplied));
        // Reject net forces more than 45 degrees away rather than steering without a viable anchor.
        if (length > 1e-9 && dot(desired,supplied)/length < Math.cos(Math.PI/4)) java.util.Arrays.fill(result,0);
        return result;
    }
    private static double dot(double[] a, double[] b) { return a[0]*b[0] + a[1]*b[1] + a[2]*b[2]; }
    private SteelMovement() { }
}
