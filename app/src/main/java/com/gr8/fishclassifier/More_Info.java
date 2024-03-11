package com.gr8.fishclassifier;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;

public class More_Info extends AppCompatActivity {

    ImageView img_main,img_left,img_right;
    Button btn_online;
    TextView txt_title, txt_info;
    String[] classes = {"Banak","Bangus","Sardine", "Carp",
            "Dalag", "Bakoko", "Salay Salay", "Pangasius", "Tilapia"};

    int predicted_fish = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more_info);

        img_main = findViewById(R.id.img_main);
        img_left = findViewById(R.id.img_left);
        img_right = findViewById(R.id.img_right);

        btn_online = findViewById(R.id.btn_online);

        txt_info = findViewById(R.id.txt_info);
        txt_title = findViewById(R.id.txt_title);

        Intent intent = getIntent();
        predicted_fish = intent.getIntExtra("fish_int", 0);
        btn_online.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri searchUri = Uri.parse("http://www.google.com/search?q="+classes[predicted_fish]);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, searchUri);
                startActivity(browserIntent);
            }
        });
        SetImage();

        txt_title.setText(classes[predicted_fish]);
        txt_info.setText(GetClassInfo(predicted_fish));
        btn_online.setText("Search online about "+ classes[predicted_fish]);
    }

    public void SetImage(){
        AssetManager assetManager = getAssets();

        try {
            // Replace "your_image_filename.jpg" with the actual name of your image file
            InputStream image_main = assetManager.open("fish_image/"+classes[predicted_fish]+"/0.jpg");
            Drawable drawable_main = Drawable.createFromStream(image_main, null);

            InputStream image_left = assetManager.open("fish_image/"+classes[predicted_fish]+"/1.jpg");
            Drawable drawable_left = Drawable.createFromStream(image_left, null);

            InputStream image_right = assetManager.open("fish_image/"+classes[predicted_fish]+"/2.jpg");
            Drawable drawable_right = Drawable.createFromStream(image_right, null);

            img_main.setImageDrawable(drawable_main);
            img_left.setImageDrawable(drawable_left);
            img_right.setImageDrawable(drawable_right);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public String GetClassInfo(int fish_label){
        switch (fish_label) {

            case 0:
                return getString(R.string.banak_desc);
            case 1:
                return getString(R.string.bangus_desc);
            case 2:
                return getString(R.string.sardine_desc);
            case 3:
                return getString(R.string.carp_desc);
            case 4:
                return getString(R.string.dalag_desc);
            case 5:
                return getString(R.string.bakoko_desc);
            case 6:
                return getString(R.string.salay_salay_desc);
            case 7:
                return getString(R.string.pangasius_desc);
            case 8:
                return getString(R.string.tilapia_desc);
            default:
                return "NULL";
        }
    }
}