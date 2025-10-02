package com.tmsimple.ToeClearance;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class BiasCalculation {

    private LogManager logManager;
    private BiasCalculationListener listener;

    DecimalFormat decimalFormat = new DecimalFormat("##.###");


    // Keep only ONE interface
    public interface BiasCalculationListener {
        void onBiasCalculationComplete(String imuId, int windowNum, double biasValue);
    }

    // Constructor
    public BiasCalculation(BiasCalculationListener listener, LogManager logManager) {
        this.listener = listener;
        this.logManager = logManager;
    }

    // Method to process bias calculation
    public void processBiasCalculation(ImuManager.GaitWindowData windowData, int windowNum) {

        if (windowData.eulerAnglesInWindow.isEmpty()) {
            logManager.log("ERROR: Empty window data received!");
            return;
        }

        int sampleCount = windowData.packetCountersInWindow.size();
        logManager.log("Processing " + sampleCount + " samples from packet " +
                windowData.startPacket + " to " + windowData.endPacket);

        // Extract the arrays from windowData
        ArrayList<Integer> packetCounters = windowData.packetCountersInWindow;
        ArrayList<double[]> eulerAngles = windowData.eulerAnglesInWindow;
        ArrayList<double[]> accelData = windowData.accelDataInWindow;
        ArrayList<float[]> quaternions = windowData.quaternionsInWindow;

        // Constants
        double gravity = 9.80665; // m/s²
        double sampleRate = 60.0; // Hz
        double delta_t = 1.0 / sampleRate;
        double gain_hs = 0.95;

        // Initialize arrays for integration
        ArrayList<double[]> v = new ArrayList<>();
        ArrayList<double[]> p = new ArrayList<>();
        ArrayList<double[]> a = new ArrayList<>();

        // Initial conditions
        v.add(new double[]{0, 0, 0});
        p.add(new double[]{0, 0, 0});

        // Bias Jacobian matrices
        double[][] Bv = new double[3][3];
        double[][] Bp = new double[3][3];

        // First integration: no bias
        for (int k = 0; k < sampleCount; k++) {
            double[] currentEuler = eulerAngles.get(k);
            double[] currentAccel = accelData.get(k);
            float[] currentQuat = quaternions.get(k);

            /*double roll = currentEuler[0];
            double pitch = currentEuler[1];
            double yaw = currentEuler[2];*/

            // Calculate Euler angles from quaternion using ZYX convention (matching MATLAB)
            double[] eulerZYX = quaternionToEulerZYX(currentQuat);
            double roll  = eulerZYX[0];
            double pitch = eulerZYX[1];
            double yaw   = eulerZYX[2];

            double[][] rotationMatrix = calculateRotationMatrix(roll, pitch, yaw);

            // double[][] rotationMatrixTranspose = transposeMatrix(rotationMatrix);

            double[] acc_world = multiplyMatrixVector(rotationMatrix, currentAccel);
            //double[] acc_world = multiplyMatrixVector(rotationMatrixTranspose, currentAccel);

            acc_world[0] = acc_world[0] - 0;
            acc_world[1] = acc_world[1] - 0;
            acc_world[2] = acc_world[2] - gravity;

            a.add(acc_world);

            double[] v_prev = v.get(k);
            double[] p_prev = p.get(k);

            double[] v_new = new double[3];
            v_new[0] = v_prev[0] + acc_world[0] * delta_t;
            v_new[1] = v_prev[1] + acc_world[1] * delta_t;
            v_new[2] = v_prev[2] + acc_world[2] * delta_t;
            v.add(v_new);

            double[] p_new = new double[3];
            p_new[0] = p_prev[0] + v_prev[0] * delta_t + 0.5 * acc_world[0] * delta_t * delta_t;
            p_new[1] = p_prev[1] + v_prev[1] * delta_t + 0.5 * acc_world[1] * delta_t * delta_t;
            p_new[2] = p_prev[2] + v_prev[2] * delta_t + 0.5 * acc_world[2] * delta_t * delta_t;
            p.add(p_new);


            // Log the calculated values for this sample
            /*logManager.log("-------------------------");
            logManager.log("packetCounter: " + packetCounters.get(k));
            logManager.log("Sample " + k + ":");
            logManager.log("  qaternion: [" + decimalFormat.format(currentQuat[0]) + ", " + decimalFormat.format(currentQuat[1]) + ", " + decimalFormat.format(currentQuat[2]) + ", " + decimalFormat.format(currentQuat[3]) + "]");
            logManager.log("  currentEuler: [" + decimalFormat.format(roll) + ", " + decimalFormat.format(pitch) + ", " + decimalFormat.format(yaw) + "]");
            logManager.log("  currentAccel: [" + decimalFormat.format(currentAccel[0]) + ", " +
                    decimalFormat.format(currentAccel[1]) + ", " +
                    decimalFormat.format(currentAccel[2]) + "]");
            logManager.log("  currentAccelMag: [" + decimalFormat.format(Math.sqrt(currentAccel[0]*currentAccel[0] + currentAccel[1]*currentAccel[1] + currentAccel[2]*currentAccel[2])) + "]");
            logManager.log("  currentAccelMagCalibrated: [" + decimalFormat.format(Math.sqrt(currentAccel[0]*currentAccel[0] + currentAccel[1]*currentAccel[1] + currentAccel[2]*currentAccel[2])
                                                             - windowData.segmentWindow.initAccelValue) + "]");
            logManager.log("  accWorld: [" + decimalFormat.format(acc_world[0]) + ", " +
                    decimalFormat.format(acc_world[1]) + ", " +
                    decimalFormat.format(acc_world[2]) + "]");
            logManager.log("  accWorldMag: [" + decimalFormat.format(Math.sqrt(acc_world[0]*acc_world[0] + acc_world[1]*acc_world[1] + acc_world[2]*acc_world[2])) + "]");
            logManager.log("  velocity: [" + String.format("%.4f", v_new[0]) + ", " +
                    String.format("%.4f", v_new[1]) + ", " +
                    String.format("%.4f", v_new[2]) + "]");
            logManager.log("  position: [" + String.format("%.4f", p_new[0]) + ", " +
                    String.format("%.4f", p_new[1]) + ", " +
                    String.format("%.4f", p_new[2]) + "]");*/

            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    Bv[i][j] += rotationMatrix[i][j] * delta_t;
                }
            }

            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    Bp[i][j] += Bv[i][j] * delta_t - 0.5 * rotationMatrix[i][j] * delta_t * delta_t;
                }
            }
        }

        // Calculate bias after integration loop
        // T_post_hs = time(endIdx) - time(idx_hs_global)
        // Assuming the entire stride is post heel-strike for now
        double T_post_hs = sampleCount * delta_t;

        // Build the Desired Jacobian (4x4 matrix)
        double[][] DesiredJacobian = new double[4][4];

        // Row 1: Bv(1,:), 0
        DesiredJacobian[0][0] = Bv[0][0];
        DesiredJacobian[0][1] = Bv[0][1];
        DesiredJacobian[0][2] = Bv[0][2];
        DesiredJacobian[0][3] = 0;

        // Row 2: Bv(2,:), 0
        DesiredJacobian[1][0] = Bv[1][0];
        DesiredJacobian[1][1] = Bv[1][1];
        DesiredJacobian[1][2] = Bv[1][2];
        DesiredJacobian[1][3] = 0;

        // Row 3: Bv(3,:), 1
        DesiredJacobian[2][0] = Bv[2][0];
        DesiredJacobian[2][1] = Bv[2][1];
        DesiredJacobian[2][2] = Bv[2][2];
        DesiredJacobian[2][3] = 1;

        // Row 4: Bp(3,:), T_post_hs
        DesiredJacobian[3][0] = Bp[2][0];
        DesiredJacobian[3][1] = Bp[2][1];
        DesiredJacobian[3][2] = Bp[2][2];
        DesiredJacobian[3][3] = T_post_hs;

        // Get last velocity and relative z position
        double[] v_last = v.get(v.size() - 1); // v(end,:)
        double pz_end_rel = p.get(p.size() - 1)[2] - p.get(0)[2]; // p(end,3) - p(1,3)

        // Build measurement vector [v_last, pz_end_rel]'
        double[] measurements = new double[4];
        measurements[0] = v_last[0];
        measurements[1] = v_last[1];
        measurements[2] = v_last[2];
        measurements[3] = pz_end_rel;

        // Solve: biasVector = inv(DesiredJacobian) * measurements
        double[] biasVector = solveLinearSystem(DesiredJacobian, measurements);

        // Extract bias components
        double biasAccX = biasVector[0];
        double biasAccY = biasVector[1];
        double biasAccZ = biasVector[2];
        double biasV_hs = biasVector[3];

        // Log the calculated bias
        logManager.log("----------------------------------");
        logManager.log("Bias calculated for " + windowData.imuId + " Window #" + windowNum);
        logManager.log("  Bias Acc: [" + decimalFormat.format(Math.sqrt(biasAccX*biasAccX + biasAccY*biasAccY + biasAccZ*biasAccZ)) + "]");
        logManager.log("  Bias V_hs: " + decimalFormat.format(biasV_hs));

        // For now, return the magnitude of acceleration bias
        // double calculatedBias = Math.sqrt(biasAccX*biasAccX + biasAccY*biasAccY + biasAccZ*biasAccZ);
        double calculatedBias = biasV_hs;

        // Call the callback
        if (listener != null) {
            listener.onBiasCalculationComplete(windowData.imuId, windowNum, calculatedBias);
        }
    }

    // Calculate rotation matrix from Euler angles (ZYX convention)
    private double[][] calculateRotationMatrix(double roll, double pitch, double yaw) {
        // Convert degrees to radians
        double rollRad = Math.toRadians(roll);
        double pitchRad = Math.toRadians(pitch);
        double yawRad = Math.toRadians(yaw);

        // Rz (yaw rotation around z-axis)
        double[][] Rz = {
                {Math.cos(yawRad), -Math.sin(yawRad), 0},
                {Math.sin(yawRad),  Math.cos(yawRad), 0},
                {0,                 0,                1}
        };

        // Ry (pitch rotation around y-axis)
        double[][] Ry = {
                { Math.cos(pitchRad), 0, Math.sin(pitchRad)},
                { 0,                  1, 0},
                {-Math.sin(pitchRad), 0, Math.cos(pitchRad)}
        };

        // Rx (roll rotation around x-axis)
        double[][] Rx = {
                {1, 0,                  0},
                {0, Math.cos(rollRad), -Math.sin(rollRad)},
                {0, Math.sin(rollRad),  Math.cos(rollRad)}
        };

        // Calculate combined rotation matrix: R = Rz * Ry * Rx
        return multiplyMatrices(multiplyMatrices(Rz, Ry), Rx);
    }
    // Transpose a 3x3 matrix
    private double[][] transposeMatrix(double[][] matrix) {
        double[][] result = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                result[i][j] = matrix[j][i];
            }
        }
        return result;
    }

    // Helper method to multiply two 3x3 matrices
    private double[][] multiplyMatrices(double[][] A, double[][] B) {
        double[][] result = new double[3][3];

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                result[i][j] = 0;
                for (int k = 0; k < 3; k++) {
                    result[i][j] += A[i][k] * B[k][j];
                }
            }
        }

        return result;
    }
    // Helper method to multiply 3x3 matrix by 3x1 vector
    private double[] multiplyMatrixVector(double[][] matrix, double[] vector) {
        double[] result = new double[3];
        for (int i = 0; i < 3; i++) {
            result[i] = 0;
            for (int j = 0; j < 3; j++) {
                result[i] += matrix[i][j] * vector[j];
            }
        }
        return result;
    }
    private double[] quaternionToEulerZYX(float[] q) {
        double w = q[0];
        double x = q[1];
        double y = q[2];
        double z = q[3];

        // ZYX Euler angles (Yaw-Pitch-Roll sequence)
        double roll = Math.atan2(2.0 * (w*x + y*z), 1.0 - 2.0 * (x*x + y*y));
        double pitch = Math.asin(2.0 * (w*y - z*x));
        double yaw = Math.atan2(2.0 * (w*z + x*y), 1.0 - 2.0 * (y*y + z*z));

        // Convert to degrees
        yaw = Math.toDegrees(yaw);
        pitch = Math.toDegrees(pitch);
        roll = Math.toDegrees(roll);

        return new double[]{roll, pitch, yaw};
    }


    // Solve linear system Ax = b using Gaussian elimination with partial pivoting
    private double[] solveLinearSystem(double[][] A, double[] b) {
        int n = b.length;
        double[][] augmented = new double[n][n + 1];

        // Create augmented matrix [A|b]
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                augmented[i][j] = A[i][j];
            }
            augmented[i][n] = b[i];
        }

        // Forward elimination with partial pivoting
        for (int k = 0; k < n; k++) {
            // Find pivot
            int maxRow = k;
            for (int i = k + 1; i < n; i++) {
                if (Math.abs(augmented[i][k]) > Math.abs(augmented[maxRow][k])) {
                    maxRow = i;
                }
            }

            // Swap rows
            double[] temp = augmented[k];
            augmented[k] = augmented[maxRow];
            augmented[maxRow] = temp;

            // Eliminate column
            for (int i = k + 1; i < n; i++) {
                double factor = augmented[i][k] / augmented[k][k];
                for (int j = k; j <= n; j++) {
                    augmented[i][j] -= factor * augmented[k][j];
                }
            }
        }

        // Back substitution
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double sum = 0;
            for (int j = i + 1; j < n; j++) {
                sum += augmented[i][j] * x[j];
            }
            x[i] = (augmented[i][n] - sum) / augmented[i][i];
        }

        return x;
    }

}