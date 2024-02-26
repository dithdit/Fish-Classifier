package com.gr8.fishclassifier;

import android.graphics.Bitmap;
import android.graphics.Color;

public class LBP {

    public static int[][] applyLBP(Bitmap image) {
        // Convert the image to grayscale
        int width = image.getWidth();
        int height = image.getHeight();
        int[][] grayscaleImage = new int[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = image.getPixel(x, y);
                grayscaleImage[x][y] = (int) (Color.red(pixel) * 0.299 + Color.green(pixel) * 0.587 + Color.blue(pixel) * 0.114);
            }
        }

        // Apply LBP to each pixel
        int[][] lbpImage = new int[width][height];

        for (int x = 1; x < width - 1; x++) {
            for (int y = 1; y < height - 1; y++) {
                int lbpCode = computeLBP(grayscaleImage, x, y);
                lbpImage[x][y] = lbpCode;
            }
        }

        return lbpImage;
    }

    public static int computeLBP(int[][] image, int x, int y) {
        int center = image[x][y];
        int lbpCode = 0;
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
            lbpCode += binaryPattern[i] * (1 << (7 - i));
        }

        return lbpCode;
    }
}
