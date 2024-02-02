package com.example.smarthomeapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    enum AudioState{ACTIVE, DISABLE};
    MQTTHelper mqttHelper;
    private AppCompatImageButton mic;
    private MediaRecorder recorder;
    private MediaPlayer player;
    private File AudioSavePath = null;
    private static int MICRO_PERMISSION_CODE = 200;
    private static final int REQUEST_CODE_SPEECH_INPUT = 1;
    private TextToSpeech tts;

    private Socket client;
    public static final String SERVER_IP = "192.168.0.195";
    public static final int SERVER_PORT = 32664;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        startMQTT();
//        startMicro();
        startTextToSpeech();
        startSpeechToText();

    }

    private boolean isMicroPresent(){
        if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE))
            return true;
        return false;
    }

    private void getMicroPermission(){
        if(ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(MainActivity.this, new String[]
                    {Manifest.permission.RECORD_AUDIO}, MICRO_PERMISSION_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS);
                String ConvertedText = Objects.requireNonNull(result).get(0);
                Toast.makeText(MainActivity.this, ConvertedText,
                        Toast.LENGTH_LONG).show();

                // speak the COnvertedText
                startClient(ConvertedText);
            }
        }
    }

    public void startTextToSpeech(){
        tts = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i != TextToSpeech.ERROR)
                    tts.setLanguage(Locale.ENGLISH);
            }
        });
    }

    public void startSpeechToText(){
        mic = findViewById(R.id.mic_bttn);
        mic.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
//            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text");

                try {
                    startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
                }
                catch (Exception e) {
                    Toast.makeText(MainActivity.this, " " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void startClient(String text){
        // init client socket
        Thread thread = new Thread(){
            @Override
            public void run(){
                try {
                    client = new Socket(SERVER_IP, SERVER_PORT);
                    OutputStream dos = new DataOutputStream(client.getOutputStream());
                    BufferedReader dis = new BufferedReader(
                            new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8)
                    );
                    // send text to server
                    dos.write(text.getBytes(StandardCharsets.UTF_8));
                    //  ai answer text -> speech
                    for(String line; (line = dis.readLine()) != null; ) {
                        tts.speak(line, TextToSpeech.QUEUE_FLUSH, null);
                        while (tts.isSpeaking()){}
                    }
                }
                catch(Exception e){
                    e.printStackTrace();
                }
                finally {
                    try {
                        client.close();
                    }
                    catch (IOException e){}
                }
            }
        };
        thread.start();
    }

    public void startMicro(){
        if (isMicroPresent())
            getMicroPermission();

        mic = findViewById(R.id.mic_bttn);
        mic.setOnClickListener(new View.OnClickListener(){
            private AudioState status = AudioState.DISABLE;
            @Override
            public void onClick(View v){
                // permissions granted
                if(status == AudioState.DISABLE){
                    try{
                        status = AudioState.ACTIVE;
                        // audio is DISABLE -> open mic
                        File musicDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
                        AudioSavePath = new File(musicDir,"recordingAudio.mp3");

                        recorder = new MediaRecorder();
                        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                        recorder.setOutputFile(AudioSavePath.getPath());
                        Toast.makeText(MainActivity.this, AudioSavePath.getPath(), Toast.LENGTH_SHORT).show();

                        recorder.prepare();
                        recorder.start();
                        Toast.makeText(MainActivity.this, "Recording started", Toast.LENGTH_SHORT).show();
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                    return;
                }
                try{
                    // audio is ACTIVE
                    status = AudioState.DISABLE;

                    recorder.stop();
                    recorder.reset();
                    recorder.release();
                    Toast.makeText(MainActivity.this, "Recording stopped", Toast.LENGTH_SHORT).show();

                    // play media right after recorder stops
                    player = new MediaPlayer();
                    player.setOnCompletionListener(new OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            player.stop();
                            player.reset();
                            player.release();
                        }
                    });

                    player.setDataSource(AudioSavePath.getPath());
                    player.prepare();
                    player.start();
                    Toast.makeText(MainActivity.this, "Player started", Toast.LENGTH_SHORT).show();

                    Toast.makeText(MainActivity.this, "Player stopped", Toast.LENGTH_SHORT).show();
                } catch (IOException e){
                    e.printStackTrace();
                }

            }
        });
    }

    public void startMQTT(){
        Thread thread = new Thread(){
            public void run(){
                System.out.println("Thread Running");
                mqttHelper = new MQTTHelper(MainActivity.this);
                mqttHelper.setCallback(new MqttCallbackExtended() {
                    @Override
                    public void connectComplete(boolean reconnect, String serverURI) {

                    }

                    @Override
                    public void connectionLost(Throwable cause) {

                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                        Log.d("TEST", topic + " *** " + message.toString());
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {

                    }
                });
            }
        };
        thread.start();
    }
}