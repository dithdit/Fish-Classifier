package com.gr8.fishclassifier;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gr8.fishclassifier.ml.DcModel;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {

    Button btn_select, btn_predict;
    ImageView imageView;
    TextView txt_prediction;

    int imageSize = 64;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_select = findViewById(R.id.btn_select);
        btn_predict = findViewById(R.id.btn_predict);

        imageView = findViewById(R.id.view_image);
        txt_prediction = findViewById(R.id.txt_prediction);

        btn_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(cameraIntent, 1);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == 1){
            //Toast.makeText(this, "Got", Toast.LENGTH_SHORT).show();
            Uri dat = data.getData();
            Bitmap image = null;
            try {
                image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), dat);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Bitmap image_padded = resizeWithPadding(image);
            imageView.setImageBitmap(image_padded);
            // Resize the image to the target size (256x256)
            Bitmap resizedImage = Bitmap.createScaledBitmap(image_padded, imageSize, imageSize, true);

            int[][] lbp_values = LBP.applyLBP(resizedImage);
//            Bitmap lbp_image = createBitmapFromPixelValues(lbp_values);
//            BitmapDrawable drawable = new BitmapDrawable(getResources(), lbp_image);
//            drawable.setFilterBitmap(false);
            //imageView.setImageDrawable(drawable);
            //image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
            //Bitmap image_LBP = preprocessImage(image);
            classifyImage(lbp_values,resizedImage);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public Bitmap resizeWithPadding(Bitmap src) {
        // Determine the larger dimension (width or height)
        int largerDimension = Math.max(src.getWidth(), src.getHeight());

        // Calculate the padding required for both dimensions
        int paddingX = (largerDimension - src.getWidth()) / 2;
        int paddingY = (largerDimension - src.getHeight()) / 2;

        // Create a square output image with padding
        Bitmap outputImage = Bitmap.createBitmap(largerDimension, largerDimension, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(outputImage);

        // Draw a white background
        canvas.drawARGB(255, 0, 0, 0);

        // Draw the original image with padding
        canvas.drawBitmap(src, paddingX, paddingY, null);


        return outputImage;
    }
    public void classifyImage(int[][] image, Bitmap bitmap){
        try {
            DcModel model = DcModel.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature_lbp = TensorBuffer.createFixedSize(new int[]{1, 64, 64, 1}, DataType.FLOAT32);
            TensorBuffer inputFeature_rgb = TensorBuffer.createFixedSize(new int[]{1, 64, 64, 3}, DataType.FLOAT32);

            ByteBuffer byteBuffer_lbp = ByteBuffer.allocateDirect(4 * imageSize * imageSize).order(ByteOrder.nativeOrder());
            ByteBuffer byteBuffer_rgb = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3).order(ByteOrder.nativeOrder());

            for(int i=0; i<image[0].length;i++){
                for(int j=0; j<image.length;j++){
                    byteBuffer_lbp.putFloat(image[j][i] * (1.f / 1));
                }
            }
            int[] intValues = new int[imageSize * imageSize];
            bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
            for (int val : intValues) {
                int red = Color.red(val);
                int green = Color.green(val);
                int blue = Color.blue(val);
                byteBuffer_rgb.putFloat( red * (1.f / 1));
                byteBuffer_rgb.putFloat( green * (1.f / 1));
                byteBuffer_rgb.putFloat(blue * (1.f / 1));
            }
            //ByteBuffer byteBuffer = convertToByteBuffer(image);
            inputFeature_lbp.loadBuffer(byteBuffer_lbp);
            inputFeature_rgb.loadBuffer(byteBuffer_rgb);

            // Runs model inference and gets result.
            DcModel.Outputs outputs = model.process(inputFeature_lbp,inputFeature_rgb);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();

//            String text ="";
//            for (int i = 0; i < confidences.length; i++) {
//                text += confidences[i] + ": ";
//            }
//            txt_prediction.setText(String.valueOf(confidences[0]));
            // find the index of the class with the biggest confidence.
            int maxPos = 0;
            float maxConfidence = 0;
            for (int i = 0; i < confidences.length; i++) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }
            String[] classes = {"Banak","Bangus","Black Sea Sprat", "Carp",
                    "Dalag", "Bakoko", "Mackerel", "Pangasius", "Tilapia"};
            txt_prediction.setText(classes[maxPos]);
            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }
    public ByteBuffer convertToByteBuffer(int[][] imageArray) {
        int width = imageArray.length;
        int height = imageArray[0].length;

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * width * height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Assuming your image values are in the range [0, 255]
                byte pixelValue = (byte) imageArray[x][y];
                byteBuffer.put(pixelValue);
            }
        }

        // Reset the position to the beginning of the buffer before using it
        byteBuffer.rewind();

        return byteBuffer;
    }
    private Bitmap createBitmapFromPixelValues(int[][] pixelValues) {
        int width = pixelValues.length;
        int height = pixelValues[0].length;

        Bitmap imageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // Assuming pixelValues are grayscale values (0-255)
                int pixelValue = pixelValues[x][y];
                int color = Color.rgb(pixelValue, pixelValue, pixelValue);
                imageBitmap.setPixel(x, y, color);
            }
        }

        return imageBitmap;
    }
}