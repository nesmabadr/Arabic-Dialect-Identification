package com.example.karim.lahga;

import android.Manifest;
import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.wear.widget.BoxInsetLayout;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.karim.lahga.tensorFlow.Classifier;
import com.example.karim.lahga.tensorFlow.TensorFlowImageClassifier;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.wang.avi.AVLoadingIndicatorView;
import java.io.File;
import java.io.IOException;
import java.util.List;
import omrecorder.AudioChunk;
import omrecorder.AudioRecordConfig;
import omrecorder.OmRecorder;
import omrecorder.PullTransport;
import omrecorder.PullableSource;
import omrecorder.Recorder;

public class MainActivity extends WearableActivity {

    private boolean permissionToRecordAccepted = false;
    private boolean permissionToReadAccepted = false;
    private boolean permissionToWriteAccepted = false;
    private static final int PERMISSIONS = 200;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private Recorder recorder = null;
    private Boolean recording = false;
    private Classifier classifier;
    private static final int INPUT_SIZE = 244;
    private static final String INPUT_NAME = "input_1";
    private static final String OUTPUT_NAME = "output_node0";
    private static final String MODEL_FILE = "file:///android_asset/optimized_graph.pb";
    private static final String LABEL_FILE = "file:///android_asset/labels.txt";
    private String directory = Environment.getExternalStorageDirectory().getAbsolutePath();
    private AVLoadingIndicatorView loading;
    private TextView textView;
    private ImageView imageView;
    private int audioAmplitudes;
    private Boolean isLoading = false;
    BoxInsetLayout box;

    final Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        public void run() {
            if(recorder != null) {
                try {
                    recorder.stopRecording();
                    recording = false;
                    isLoading = true;
                    Log.i("recording", "ended");
                    Log.i("AMPLITUDES", String.valueOf(audioAmplitudes));
                    /*if (audioAmplitudes >  0) {
                        Toast.makeText(MainActivity.this, "Can't hear you!", Toast.LENGTH_SHORT).show();
                        textView.setText("Tap to Record");
                        imageView.setImageResource(R.drawable.ic_mic_none_white_48dp);
                    }*/
                    //else {
                        loading.smoothToShow();
                        textView.setText("Processing Audio..");
                        imageView.setVisibility(View.GONE);
                        loadFFMPEG();
                    //}
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        imageView = findViewById(R.id.imageView);
        loading =  findViewById(R.id.avi);
        box = findViewById(R.id.box);
        // Enables Always-on
        setAmbientEnabled();
        ActivityCompat.requestPermissions(this, permissions, PERMISSIONS);
    }

    public void setupRecorder(){
        recorder = OmRecorder.wav(
                new PullTransport.Default(mic(),
                        new PullTransport.OnAudioChunkPulledListener() {
                            @Override public void onAudioChunkPulled(AudioChunk audioChunk) {
                                audioAmplitudes = audioAmplitudes + (int) audioChunk.maxAmplitude();
                            }
                        }), file());
    }

    public void record(View v){
        if (!recording){
            audioAmplitudes = 0;
            setupRecorder();
            recorder.startRecording();
            recording = true;
            Log.i("recording", "started");
            textView.setText("Recording..");
            imageView.setImageResource(R.drawable.ic_mic_white_48dp);
            handler.postDelayed(runnable, 5200);
        }
        else {
            try {
                recording = false;
                handler.removeCallbacks(runnable);
                recorder.stopRecording();
                recorder = null;
                textView.setText("Tap to Record");
                imageView.setImageResource(R.drawable.ic_mic_none_white_48dp);
                File file = new File(directory , "audio.wav");
                if (file.exists())
                    file.delete();
                Log.i("recording", "stopped");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private PullableSource mic() {
                return new PullableSource.Default(
                        new AudioRecordConfig.Default(
                                MediaRecorder.AudioSource.MIC, AudioFormat.ENCODING_PCM_16BIT,
                                AudioFormat.CHANNEL_IN_MONO, 44100));
    }

    @NonNull private File file() {
        return new File(Environment.getExternalStorageDirectory(), "audio.wav");
    }

    private void loadFFMPEG(){
        FFmpeg ffmpeg = FFmpeg.getInstance(this);
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onStart() {}
                @Override
                public void onFailure() {
                    Log.i("OnFailure", "Failed");
                }
                @Override
                public void onSuccess() {
                    ProcessFrame();
                }
                @Override
                public void onFinish() {}
            });
        } catch (FFmpegNotSupportedException e) { Log.i("FFMPEG", "device not supported"); }
    }

    private void ProcessFrame(){
        String command = "-i " + directory + "/audio.wav -lavfi showspectrumpic=s=224x224:legend=disabled " + directory + "/spectrogram.png";
        String []cmd = command.split(" ");
        execFFMPEG(cmd, true);
    }

    private void execFFMPEG(String[] cmd, final Boolean classify){
        FFmpeg ffmpeg = FFmpeg.getInstance(this);
        try {
            ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {
                @Override
                public void onStart() {}
                @Override
                public void onProgress(String message) {
                    Log.i("FFMPEG", "start");
                    Log.i("Progress:", message);
                }
                @Override
                public void onFailure(String message) {
                    Log.i("Failure:", message);
                }
                @Override
                public void onSuccess(String message) {
                    Log.i("Sucess:", message); }
                @Override
                public void onFinish() {
                    Log.i("FFMPEG", "finish");
                    if (classify)
                        LoadTensorFlow();
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // Handle if FFmpeg is already running
        }
    }

    private void LoadTensorFlow(){
        classifier = TensorFlowImageClassifier.create(
                getAssets(),
                MODEL_FILE,
                LABEL_FILE,
                INPUT_SIZE,
                INPUT_NAME,
                OUTPUT_NAME);
        ClassifyFrame();
    }

    private void ClassifyFrame() {
        if (classifier == null || this == null) {
            Log.i("ClassifyFrame","Uninitialized Classifier or invalid context.");
            return;
        }

        if (new File( directory + "/spectrogram.png").exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile( directory + "/spectrogram.png");
            //bitmap = Bitmap.createScaledBitmap(bitmap, 244, 244, false);
            final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);
            Log.i("PREDICTION", results.get(0).getTitle() + " " + results.get(0).getConfidence());
            textView.setText("Tap to Record");
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageResource(R.drawable.ic_mic_none_white_48dp);
            File file = new File(directory , "spectrogram.png");
            file.delete();
            File file2 = new File(directory , "audio.wav");
            file2.delete();
            Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(100);
            Toast.makeText(this, "Dialect detected: " + results.get(0).getTitle(), Toast.LENGTH_SHORT).show();
            isLoading = false;
            loading.smoothToHide();
        }
        else
            Log.i("Image Classifier", "Spectrogram not found");

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                permissionToReadAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                permissionToWriteAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }

        if (!permissionToRecordAccepted || !permissionToReadAccepted || !permissionToWriteAccepted)
            finish();
    }

    @Override
    public void onResume(){
        super.onResume();
        textView.setText("Tap to Record");
        imageView.setImageResource(R.drawable.ic_mic_none_white_48dp);
        if (isLoading) {
            isLoading = false;
            loading.smoothToHide();
            imageView.setVisibility(View.VISIBLE);
        }
    }
    @Override
    public void onStop() {
        super.onStop();
        if(recording) {
            try {
                handler.removeCallbacks(runnable);
                recording = false;
                recorder.stopRecording();
                Log.i("recording", "ended");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        File file = new File(directory , "spectrogram.png");
        if (file.exists())  file.delete();
        File file2 = new File(directory , "audio.wav");
        if (file2.exists()) file2.delete();
        File file3 = new File(directory , "audioTrim.wav");
        if (file3.exists())file3.delete();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(recording) {
            try {
                handler.removeCallbacks(runnable);
                recording = false;
                recorder.stopRecording();
                Log.i("recording", "ended");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        File file = new File(directory , "spectrogram.png");
        if (file.exists())  file.delete();
        File file2 = new File(directory , "audio.wav");
        if (file2.exists()) file2.delete();
        File file3 = new File(directory , "audioTrim.wav");
        if (file3.exists())file3.delete();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        box.setBackgroundColor(ContextCompat.getColor(this, R.color.black));
        textView.getPaint().setAntiAlias(false);
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        box.setBackgroundColor(Color.parseColor("#8ee5e0"));
        textView.getPaint().setAntiAlias(false);
    }
}
