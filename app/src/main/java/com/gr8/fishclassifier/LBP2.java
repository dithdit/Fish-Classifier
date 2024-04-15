package com.gr8.fishclassifier;

import android.graphics.Bitmap;
import android.graphics.Color;

public class LBP2 {
    public static int[][] uniformLocalBinaryPattern(Bitmap image, int radius, int numPoints) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[][] grayscaleImage = toGrayscale(image);
        int[][] ulbpImage = new int[width][height];

        for (int x = radius; x < width - radius; x++) {
            for (int y = radius; y < height - radius; y++) {
                int center = grayscaleImage[x][y];
                int ulbpCode = computeULBP(grayscaleImage, center, x, y, radius, numPoints);
                ulbpImage[x][y] = ulbpCode;
            }
        }

        return ulbpImage;
    }

    private static int computeULBP(int[][] image, int center, int x, int y, int radius, int numPoints) {
        int ulbpCode = 0;
        int[] binaryPattern = new int[numPoints];

        double[] cosValues = new double[numPoints];
        double[] sinValues = new double[numPoints];
        double angleStep = 2 * Math.PI / numPoints;
        for (int i = 0; i < numPoints; i++) {
            cosValues[i] = Math.cos(i * angleStep);
            sinValues[i] = Math.sin(i * angleStep);
        }

        for (int i = 0; i < numPoints; i++) {
            int neighborX = (int) (x + radius * cosValues[i]);
            int neighborY = (int) (y - radius * sinValues[i]);

            if (neighborX >= 0 && neighborX < image.length && neighborY >= 0 && neighborY < image[0].length) {
                int neighborValue = image[neighborX][neighborY];
                binaryPattern[i] = (neighborValue >= center) ? 1 : 0;
            }
        }

        // Convert binary pattern to decimal (LBP code)
        for (int i = 0; i < numPoints; i++) {
            ulbpCode += binaryPattern[i] * (1 << (numPoints - 1 - i));
        }

        // Check if the ULBP code is uniform or non-uniform
        boolean isUniform = isUniformPattern(ulbpCode, numPoints);

        // If the ULBP code is non-uniform, set it to -1 (or any other value to distinguish it)
        if (!isUniform) {
            ulbpCode = -1;
        }

        return ulbpCode;
    }

    private static boolean isUniformPattern(int ulbpCode, int numPoints) {
        // Count the number of bitwise transitions in the ULBP code
        int transitions = Integer.bitCount(ulbpCode ^ (ulbpCode << 1));
        return transitions <= 2; // A pattern is uniform if it has at most 2 transitions
    }

    private static int[][] toGrayscale(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[][] grayscaleImage = new int[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = image.getPixel(x, y);
                grayscaleImage[x][y] = (int) (Color.red(pixel) * 0.299 + Color.green(pixel) * 0.587 + Color.blue(pixel) * 0.114);
            }
        }

        return grayscaleImage;
    }
}
