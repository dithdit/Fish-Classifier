package com.gr8.fishclassifier;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.gr8.fishclassifier.ml.TestModel;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Testing_Activity extends AppCompatActivity {

    Button btn_select, btn_predict;
    ImageView imageView;
    TextView txt_prediction;

    int imageSize = 32;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testing);

        btn_select = findViewById(R.id.btn_select);
        btn_predict = findViewById(R.id.btn_capture);

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

            image = Bitmap.createScaledBitmap(image_padded, imageSize, imageSize, false);
            classifyImage(image);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
    public void classifyImage(Bitmap image){
        try {
            TestModel model = TestModel.newInstance(getApplicationContext());

            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 32, 32, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer_rgb = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3).order(ByteOrder.nativeOrder());
            ByteBuffer byteBuffer_lbp = ByteBuffer.allocateDirect(4 * imageSize * imageSize).order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
            for (int val : intValues) {
                int red = Color.red(val);
                int green = Color.green(val);
                int blue = Color.blue(val);
                int grayscale = (int) (red* 0.299 + green * 0.587 + blue * 0.114);

                byteBuffer_rgb.putFloat( red * (1.f / 1));
                byteBuffer_rgb.putFloat( green * (1.f / 1));
                byteBuffer_rgb.putFloat(blue * (1.f / 1));

                byteBuffer_lbp.putFloat(grayscale * (1.f / 1));
            }

            inputFeature0.loadBuffer(byteBuffer_rgb);

            TestModel.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();
            // find the index of the class with the biggest confidence.
            int maxPos = 0;
            float maxConfidence = 0;
            for (int i = 0; i < confidences.length; i++) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }
            String[] classes = {"Apple", "Banana", "Orange"};
            txt_prediction.setText(classes[maxPos]);

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
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
}