package com.example.smarthomeapp;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.slider.Slider;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import android.widget.EditText;



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
    public static final String SERVER_IP = "192.168.66.183";
    public static final int SERVER_PORT = 32664;

    private EditText userInput;
    private TextView chatbotResponse;

    TextView txtTemp, txtLum, txtCamDevice, txtLightDevice, txtFanDevice, txtCam, txtFan, txtLight;
    LinearLayout linCam, linFan, linLight;
    LinearLayout CamView, FanView, LightView;
    LinearLayout chatbotView;
    ScrollView DeviceView;
    ImageView closeCam, closeLight, closeFan;
    ImageView camImage;
    SwitchCompat btnLight;
    Slider sdrFan;

    Button buttonChatbot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        camImage = findViewById(R.id.camImage);

//        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
//            @Override
//            public void handleOnBackPressed() {
//                Log.d("TEST", "BACK PRESSED!");
//                if(DeviceView.getVisibility() == View.VISIBLE){
//                    finishAffinity();
//                    finish();
//                } else if (LightView.getVisibility() == View.VISIBLE){
//                    LightView.setVisibility(View.GONE);
//                    DeviceView.setVisibility(View.VISIBLE);
//                } else if (CamView.getVisibility() == View.VISIBLE){
//                    CamView.setVisibility(View.GONE);
//                    DeviceView.setVisibility(View.VISIBLE);
//                } else if (FanView.getVisibility() == View.VISIBLE){
//                    FanView.setVisibility(View.GONE);
//                    DeviceView.setVisibility(View.VISIBLE);
//                }
//            }
//        };
//        OnBackPressedDispatcher onBackPressedDispatcher = new OnBackPressedDispatcher();
//        onBackPressedDispatcher.addCallback(this, onBackPressedCallback);
//        onBackPressedCallback.setEnabled(true);

        txtTemp = findViewById(R.id.Temperature);
        txtLum = findViewById(R.id.Luminance);

        txtCamDevice = findViewById(R.id.camTextDevice);
        txtFanDevice = findViewById(R.id.fanTextDevice);
        txtLightDevice = findViewById(R.id.lightTextDevice);
        txtCam = findViewById(R.id.camText);
        txtFan = findViewById(R.id.fanText);
        txtLight = findViewById(R.id.lightText);

        DeviceView = findViewById(R.id.deviceView);
        CamView = findViewById(R.id.camView);
        FanView = findViewById(R.id.fanView);
        LightView = findViewById(R.id.lightView);

        linCam = findViewById(R.id.CamTab);
        linFan = findViewById(R.id.FanTab);
        linLight = findViewById(R.id.LightTab);
        buttonChatbot = findViewById(R.id.button1);

        linCam.setOnClickListener(view -> {
            DeviceView.setVisibility(View.GONE);
            CamView.setVisibility(View.VISIBLE);
        });

        buttonChatbot.setOnClickListener(view -> {
            DeviceView.setVisibility(View.GONE);
            chatbotView.setVisibility(View.VISIBLE);
        });

        linFan.setOnClickListener(view -> {
            DeviceView.setVisibility(View.GONE);
            FanView.setVisibility(View.VISIBLE);
        });

        linLight.setOnClickListener(view -> {
            DeviceView.setVisibility(View.GONE);
            LightView.setVisibility(View.VISIBLE);
        });

        closeCam = findViewById(R.id.closeCam);
        closeFan = findViewById(R.id.closeFan);
        closeLight = findViewById(R.id.closeLight);

        closeCam.setOnClickListener(view -> {
            CamView.setVisibility(View.GONE);
            DeviceView.setVisibility(View.VISIBLE);
        });

        closeFan.setOnClickListener(view -> {
            FanView.setVisibility(View.GONE);
            DeviceView.setVisibility(View.VISIBLE);
        });

        closeLight.setOnClickListener(view -> {
            LightView.setVisibility(View.GONE);
            DeviceView.setVisibility(View.VISIBLE);
        });

        btnLight = findViewById(R.id.btnLight);
        btnLight.setOnCheckedChangeListener((compoundButton, b) -> {
            if(b) {
                sendMQTT("RegL/feeds/den", "L");
            }else {
                sendMQTT("RegL/feeds/den", "l");
            }
        });

        sdrFan = findViewById(R.id.sdrFan);
        sdrFan.addOnChangeListener((slider, value, fromUser) -> {
            if(fromUser) {
                sendMQTT("RegL/feeds/quat", Integer.toString((int)value));
            }
        });

        userInput = findViewById(R.id.user_input);
        chatbotResponse = findViewById(R.id.chatbot_response);
        Button sendButton = findViewById(R.id.send_button);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userText = userInput.getText().toString();
                String response = processInput(userText); // You need to implement this method.
                chatbotResponse.setText(response);
                userInput.setText(""); // Clear input field after sending message.
            }
        });

        startMQTT();
//        startMicro();
        startTextToSpeech();
        startSpeechToText();

    }
    private String processInput(String input) {
        // TODO: Process user input and return the chatbot's response.
        // This is where you'd include the logic or method calls to your chatbot service.
        return "This is a response from the chatbot.";
    }
    @Override
    public void onBackPressed() {
        Log.d("TEST", "BACK PRESSED!");
        if(DeviceView.getVisibility() == View.VISIBLE){
            finishAffinity();
            finish();
        } else if (LightView.getVisibility() == View.VISIBLE){
            LightView.setVisibility(View.GONE);
            DeviceView.setVisibility(View.VISIBLE);
        } else if (CamView.getVisibility() == View.VISIBLE){
            CamView.setVisibility(View.GONE);
            DeviceView.setVisibility(View.VISIBLE);
        } else if (FanView.getVisibility() == View.VISIBLE){
            FanView.setVisibility(View.GONE);
            DeviceView.setVisibility(View.VISIBLE);
        }
    }

    public void sendMQTT(String topic, String value){
        MqttMessage msg = new MqttMessage();
        msg.setQos(0);
        msg.setRetained(false);

        byte[] b = value.getBytes(StandardCharsets.UTF_8);
        msg.setPayload(b);

//        try {
            mqttHelper.mqttAndroidClient.publish(topic, msg);
//        }catch (MqttException e){
//        }
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
                    Thread.sleep(1000);
                    //  ai answer text -> speech
                    for(String line; (line = dis.readLine()) != null; ) {
                        Log.d("TEST-TTS", line);
                        tts.speak(line, TextToSpeech.QUEUE_FLUSH, null);
                        while (tts.isSpeaking()){}
                    }
                }
                catch(Exception e){
                    e.printStackTrace();
                }
                finally {
//                    try {
//                        client.close();
//                    }
//                    catch (IOException e){}
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
                        if (topic.contains("cambien-nhiet")){
                            txtTemp.setText(message + "°C");
                        } else if (topic.contains("cambien-as")){
                            txtLum.setText("Luminance: " + message.toString() + "%");
                        } else if (topic.contains("ai")){
                            String tmp = message.toString() + " detected";
                            txtCam.setText(tmp);
                            txtCamDevice.setText(tmp);
                        } else if (topic.contains("den")){
                            String tmp = "n/a";
                            if(message.toString().contains("L")){
                                tmp = "Status: On";
                                btnLight.setChecked(true);
                            } else if (message.toString().contains("l")){
                                tmp = "Status: Off";
                                btnLight.setChecked(false);
                            }
                            txtLight.setText(tmp);
                            txtLightDevice.setText(tmp);
                        } else if (topic.contains("quat")){
                            String tmp = "Fan speed: " + message.toString();
                            txtFan.setText(tmp);
                            txtFanDevice.setText(tmp);

                            sdrFan.setValue(Integer.parseInt(message.toString())/25 * 25);
                        } else if (topic.contains("image")){
                            byte[] decoded = Base64.getDecoder().decode(message.toString());
                            Bitmap img = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                            camImage.setImageBitmap(img);
                        }
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