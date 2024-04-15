package com.gr8.fishclassifier;

import android.graphics.Bitmap;
import android.graphics.Color;

public class ULBP {
    public static int[][] applyULBP(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[][] grayscaleImage = new int[width][height];

        // Convert the image to grayscale
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = image.getPixel(x, y);
                grayscaleImage[x][y] = (int) (Color.red(pixel) * 0.299 + Color.green(pixel) * 0.587 + Color.blue(pixel) * 0.114);
            }
        }

        // Apply ULBP to each pixel
        int[][] ulbpImage = new int[width][height];

        for (int x = 1; x < width - 1; x++) {
            for (int y = 1; y < height - 1; y++) {
                int ulbpCode = computeULBP(grayscaleImage, x, y);
                ulbpImage[x][y] = ulbpCode;
            }
        }

        return ulbpImage;
    }

    public static int computeULBP(int[][] image, int x, int y) {
        int center = image[x][y];
        int ulbpCode = 0;
        int[] binaryPattern = new int[8];

        // Define the neighbor positions (clockwise from top-left)
        int[][] neighbors = {
                {-1, -1}, {-1, 0}, {-1, 1},
                {0, 1}, {1, 1}, {1, 0},
                {1, -1}, {0, -1}
        };

        for (int i = 0; i < 8; i++) {
            int neighborX = x + neighbors[i][0];
            int neighborY = y + neighbors[i][1];
            int neighborValue = image[neighborX][neighborY];
            binaryPattern[i] = (neighborValue >= center) ? 1 : 0;
        }

        // Convert binary pattern to decimal (LBP code)
        for (int i = 0; i < 8; i++) {
            ulbpCode += binaryPattern[i] * (1 << (7 - i));
        }

        // Check if the ULBP code is uniform or non-uniform
        boolean isUniform = isUniformPattern(ulbpCode);

        // If the ULBP code is non-uniform, assign a specific value (e.g., 255)
        if (!isUniform) {
            ulbpCode = 255; // You can adjust the value as needed
        }

        return ulbpCode;
    }

    public static boolean isUniformPattern(int ulbpCode) {
        // Count the number of bitwise transitions in the ULBP code
        int transitions = Integer.bitCount(ulbpCode ^ (ulbpCode << 1));
        return transitions <= 2; // A pattern is uniform if it has at most 2 transitions
    }
}
