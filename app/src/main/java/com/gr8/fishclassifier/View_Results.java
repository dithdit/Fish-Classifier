package com.gr8.fishclassifier;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.gr8.fishclassifier.ml.AutoModel4dUniform64;
import com.gr8.fishclassifier.ml.FourthModel;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class View_Results extends AppCompatActivity {

    ImageView img_fish, img_fish_blur;
    TextView txt_result_class,txt_result_percentage, txt_desc;
    TextView[] table_titles, table_percentages;

    Button btn_reset, btn_info;
    int imageSize=100;

    int predicted_fish = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_results_v2);

        img_fish = findViewById(R.id.img_fish);
        img_fish_blur = findViewById(R.id.img_fish_blur);

        txt_result_class = findViewById(R.id.txt_result_class);
        txt_result_percentage =  findViewById(R.id.txt_result_percentage);
        txt_desc = findViewById(R.id.txt_info);

        btn_reset = findViewById(R.id.btn_reset);
        btn_info = findViewById(R.id.btn_info);

        btn_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        btn_info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), More_Info.class);
                intent.putExtra("fish_int",predicted_fish);
                startActivity(intent);
            }
        });

        table_titles = getTableTitles();
        table_percentages = getTablePercentage();

        Bitmap bmp = null;
        String filename = getIntent().getStringExtra("image");
        try {
            FileInputStream is = this.openFileInput(filename);
            bmp = BitmapFactory.decodeStream(is);
            is.close();


        } catch (Exception e) {
            e.printStackTrace();
        }
        if(bmp==null)return;
        //Bitmap image_view = Bitmap.createScaledBitmap(bmp, 512, 512, true);
        img_fish.setImageBitmap(bmp);

        Bitmap blurredBitmap = BlurUtility.blur(this, bmp);
        img_fish_blur.setImageBitmap(blurredBitmap);

        //Bitmap image_padded = resizeWithPadding(bmp);

        //Preprocess the image
        Bitmap resizedImage = Bitmap.createScaledBitmap(bmp, imageSize, imageSize, true);
        int[][] lbp_values = LBP.applyLBP(resizedImage);
        //img_fish.setImageBitmap(createBitmapFromIntArray(lbp_values));
        //Classify image
        classifyImage(lbp_values,resizedImage);
    }

    public void classifyImage(int[][] image, Bitmap bitmap){
        try {
            //DcModel model = DcModel.newInstance(getApplicationContext());
            //AutoModel4dUniform64 model = AutoModel4dUniform64.newInstance(getApplicationContext());
            FourthModel model = FourthModel.newInstance(getApplicationContext());
            // Creates inputs for reference.
            TensorBuffer inputFeature_rgb = TensorBuffer.createFixedSize(new int[]{1, 100, 100, 4}, DataType.FLOAT32);

            ByteBuffer byteBuffer_rgb = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 4).order(ByteOrder.nativeOrder());

            int[] lbpValues = new int[imageSize * imageSize];
            int lbp_count = 0;
            for(int i=0; i<image[0].length;i++){
                for(int j=0; j<image.length;j++){
                    lbpValues[lbp_count] = image[j][i];
                    lbp_count++;
                }
            }
            int[] intValues = new int[imageSize * imageSize];
            bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

            int count = 0;
            for (int val : intValues) {
                int red = Color.red(val);
                int green = Color.green(val);
                int blue = Color.blue(val);
                byteBuffer_rgb.putFloat( red * (1.f / 1));
                byteBuffer_rgb.putFloat( green * (1.f / 1));
                byteBuffer_rgb.putFloat(blue * (1.f / 1));
                byteBuffer_rgb.putFloat(lbpValues[count] * (1.f / 1));
            }

            inputFeature_rgb.loadBuffer(byteBuffer_rgb);

            // Runs model inference and gets result.
            FourthModel.Outputs outputs = model.process(inputFeature_rgb);
            //AutoModel4dUniform64.Outputs outputs = model.process(inputFeature_rgb);
            //DcModel.Outputs outputs = model.process(inputFeature_lbp,inputFeature_rgb);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();
            int maxPos = 0;
            float maxConfidence = 0;
            for (int i = 0; i < confidences.length; i++) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }
            String[] classes = {"Banak","Bangus","Sardine", "Carp",
                    "Dalag", "Bakoko", "Salay Salay", "Pangasius", "Tilapia"};
            assignTableTitles(classes);
            assignTablePercentages(confidences);
            String fishClass = classes[maxPos];
            predicted_fish = maxPos;
            txt_result_class.setText(fishClass);
            txt_result_percentage.setText(getPercentage(maxConfidence));

            SetDescriptionText(fishClass,maxConfidence);
            SetButtonInfoText(fishClass);

            model.close();
        } catch (IOException e) {

        }
    }

    public String getPercentage(float value){
        float perc = value * 100;
        return String.format("%.2f", perc) + "%";
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
    public TextView[] getTableTitles(){
        TextView[] textViews = new TextView[10];

        textViews[0] = findViewById(R.id.table00);
        textViews[1] = findViewById(R.id.table10);
        textViews[2] = findViewById(R.id.table20);
        textViews[3] = findViewById(R.id.table30);
        textViews[4] = findViewById(R.id.table40);
        textViews[5] = findViewById(R.id.table50);
        textViews[6] = findViewById(R.id.table60);
        textViews[7] = findViewById(R.id.table70);
        textViews[8] = findViewById(R.id.table80);
        textViews[9] = findViewById(R.id.table90);

        return textViews;
    }
    public void assignTableTitles(String[] titles){
        for(int i=0; i<table_titles.length;i++){
            String title = (i < titles.length)? titles[i] + ": ": null;
            table_titles[i].setText(title);
        }
    }
    public TextView[] getTablePercentage(){
        TextView[] textViews = new TextView[10];

        textViews[0] = findViewById(R.id.table01);
        textViews[1] = findViewById(R.id.table11);
        textViews[2] = findViewById(R.id.table21);
        textViews[3] = findViewById(R.id.table31);
        textViews[4] = findViewById(R.id.table41);
        textViews[5] = findViewById(R.id.table51);
        textViews[6] = findViewById(R.id.table61);
        textViews[7] = findViewById(R.id.table71);
        textViews[8] = findViewById(R.id.table81);
        textViews[9] = findViewById(R.id.table91);

        return textViews;
    }
    public void assignTablePercentages(float[] values){
        for(int i=0; i<table_percentages.length;i++){
            String perc = (i < values.length)? getPercentage(values[i]): null;
            table_percentages[i].setText(perc);
        }
    }
    public static Bitmap createBitmapFromIntArray(int[][] pixelData) {
        int width = pixelData.length;
        int height = pixelData[0].length;

        int[] pixels = new int[width * height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                pixels[x * height + y] = pixelData[x][y];
            }
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    }
    public void SetDescriptionText(String fishClass, float perc){
        String text = "The system predicted the image to be "
                + getPercentage(perc) +" " + fishClass;
        txt_desc.setText(text);
    }
    public void SetButtonInfoText(String fishClass){
        String text = "More Information about "+fishClass;
        btn_info.setText(text);
    }
}