package com.abeer.ai_treasure_hunt;

import android.media.MediaPlayer;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;


import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.ibm.watson.developer_cloud.android.library.audio.StreamPlayer;
import com.ibm.watson.developer_cloud.android.library.camera.CameraHelper;
import com.ibm.watson.developer_cloud.service.security.IamOptions;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.SynthesizeOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifiedImages;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;


import android.widget.Toast;

import static java.lang.Thread.sleep;
//level1

public class Level2 extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 100;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 101;
    private static final int REQUEST_TAKE_PHOTO = 100;
    private static final int REQUEST_IMAGE_CAPTURE = 100;

    private ImageView mImageView;
    private TextView mTextView;
    private TextView t_login;
    private Button btn_capture;
    private String hint = "Level 2. Find something with 163 floors";

    private StreamPlayer player = new StreamPlayer();
    private String speakLanguage;
    private TextToSpeech textToSpeech;

    private VisualRecognition mVisualRecognition;
    private CameraHelper mCameraHelper;
    private File photoFile;
    private static final String TAG = "Level2";
    private String levelss;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        final MediaPlayer mp = MediaPlayer.create(this,R.raw.pass);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(false);

     //   mImageView = (ImageView) findViewById(R.id.image_view_main);
        mTextView = (TextView) findViewById(R.id.text_view_main);
        t_login = (TextView) findViewById(R.id.t_login);
        mCameraHelper = new CameraHelper(this);

        mp.start();
        try {
            sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Toast toast = Toast.makeText(getApplicationContext(), "Congrats for passing level 1!", Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        toast.show();
        btn_capture = findViewById(R.id.btn_capture);
        auth();
        speakhint();


        captureImage();

    }
    public void onBackPressed(){


    }

    private void auth(){
        IamOptions options = new IamOptions.Builder()
                .apiKey(getString(R.string.api_keyVR))
                .build();
        mVisualRecognition = new VisualRecognition("2018-03-19", options);
    }
    public void speakhint() {
        IamOptions options = new IamOptions.Builder()
                .apiKey(getString(R.string.api_keyTTS))
                .build();

        textToSpeech = new TextToSpeech(options);

        textToSpeech.setEndPoint(getString(R.string.url_TTS));

        new Level2.SynthesisTask().execute(hint);

    }
    private class SynthesisTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            SynthesizeOptions synthesizeOptions = new SynthesizeOptions.Builder()
                    .text(params[0])
                    .voice(speakLanguage)
                    .accept(SynthesizeOptions.Accept.AUDIO_WAV)
                    .build();
            player.playStream(textToSpeech.synthesize(synthesizeOptions).execute());
            return "Did synthesize";
        }
    }
    public void captureImage(){

        Button button = (Button) findViewById(R.id.btn_capture);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });




    }

    //request the camera to take picture
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
        }
    }

    //after picture taken take image convert it to file and send apis
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            //convert to file
            File filesDir = this.getApplicationContext().getFilesDir();
            photoFile = new File(filesDir, "myphoto" + ".jpg");

            OutputStream os;
            try {
                os = new FileOutputStream(photoFile);
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                os.flush();
                os.close();
            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), "Error writing bitmap", e);
            }

            if (photoFile != null) {
                t_login.setVisibility(View.VISIBLE);
                backgroundThread();
            }
        }
    }

    private void backgroundThread(){



        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {

                InputStream imagesStream = null;

                try {
                    imagesStream = new FileInputStream(photoFile);



                    ClassifyOptions classifyOptions = new ClassifyOptions.Builder()
                            .imagesFile(imagesStream)
                            .imagesFilename(photoFile.getName())
                            .threshold((float) 0.6)
                            .classifierIds(Arrays.asList(getString(R.string.VR_modelid)))
                            .build();
                    ClassifiedImages result = mVisualRecognition.classify(classifyOptions).execute();
                    Gson gson = new Gson();
                    String json = gson.toJson(result);
                    Log.d("json", json);
                    String name = null;
                    try {
                        JSONObject jsonObject = new JSONObject(json);
                        JSONArray jsonArray = jsonObject.getJSONArray("images");
                        JSONObject jsonObject1 = jsonArray.getJSONObject(0);
                        JSONArray jsonArray1 = jsonObject1.getJSONArray("classifiers");
                        JSONObject jsonObject2 = jsonArray1.getJSONObject(0);
                        JSONArray jsonArray2 = jsonObject2.getJSONArray("classes");
                        JSONObject jsonObject3 = jsonArray2.getJSONObject(0);
                        name = jsonObject3.getString("class");

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    final String finalName = name;

                    final MediaPlayer fmp = MediaPlayer.create(Level2.this,R.raw.fail);



                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //  mTextView.setText("Detected Image: " + finalName);

                            Log.d(TAG, "Ans: " + finalName);
                            t_login.setVisibility(View.GONE);

                            if(finalName != null){
                                if (finalName.equals("BurjKhalifa"))
                                {
                                    Intent mass = new Intent(Level2.this, Level3.class);
                                    startActivity(mass);
                                } else {
                                    fmp.start();

                                    Toast toast = Toast.makeText(getApplicationContext(), "Sorry. Try Again!", Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                                    toast.show();

                                    t_login.setVisibility(View.GONE);

                                }}

                            else
                            {
                                fmp.start();
                                Toast toast = Toast.makeText(getApplicationContext(), "Sorry. Try Again!", Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                                toast.show();

                                t_login.setVisibility(View.GONE);
                            }


                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();



                }



            }



        });

    }
}
