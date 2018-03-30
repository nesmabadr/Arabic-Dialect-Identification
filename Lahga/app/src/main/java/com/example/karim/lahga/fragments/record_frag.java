package com.example.karim.lahga.fragments;

import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.karim.lahga.R;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.wang.avi.AVLoadingIndicatorView;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import info.kimjihyok.ripplelibrary.Rate;
import info.kimjihyok.ripplelibrary.VoiceRippleView;
import info.kimjihyok.ripplelibrary.listener.RecordingListener;
import info.kimjihyok.ripplelibrary.renderer.Renderer;
import info.kimjihyok.ripplelibrary.renderer.TimerCircleRippleRenderer;

/**
 * Created by karim on 2/24/2018.
 */

public class record_frag extends Fragment {

    private static final String TAG = "RECORD";
    private static final String DIRECTORY_NAME = "AudioCache";
    private Button playButton;
    private TextView progressText;
    private MediaPlayer player = null;
    private File directory = null;
    private File audioFile = null;
    private VoiceRippleView voiceRipple;
    private Renderer currentRenderer;
    private AVLoadingIndicatorView loading;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.record_frag, container, false);
        loading =  rootView.findViewById(R.id.avi);
        voiceRipple = rootView.findViewById(R.id.voice_ripple_view);
        progressText = rootView.findViewById(R.id.textView);

        playButton = rootView.findViewById(R.id.button);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (directory != null && audioFile != null) {
                    player = new MediaPlayer();
                    try {
                        player.setDataSource(audioFile.getAbsolutePath());
                        player.prepare();
                        player.start();
                    }
                    catch (IOException e) {
                        Log.e(TAG, "prepare() failed");
                    }
                }
            }

        });

        directory = new File(getActivity().getExternalCacheDir().getAbsolutePath(), DIRECTORY_NAME);

        if (directory.exists()) {
            deleteFilesInDir(directory);
        } else {
            directory.mkdirs();
        }

        audioFile = new File(directory + "/audio.mp3");
        voiceRipple.setRecordingListener(new RecordingListener() {

            @Override
            public void onRecordingStopped() {
                Log.d(TAG, "onRecordingStopped()");
                    progressText.setText("Tap to Record");
            }

            @Override
            public void onRecordingStarted() {
                Log.d(TAG, "onRecordingStarted()");
                progressText.setText("Recording..");
            }
        });

        // set view related settings for ripple view
        voiceRipple.setRippleSampleRate(Rate.LOW);
        voiceRipple.setRippleDecayRate(Rate.HIGH);
        voiceRipple.setBackgroundRippleRatio(1.49); //1.4

        // set recorder related settings for ripple view
        voiceRipple.setMediaRecorder(new MediaRecorder());
        voiceRipple.setOutputFile(audioFile.getAbsolutePath());
        voiceRipple.setAudioSource(MediaRecorder.AudioSource.MIC);
        voiceRipple.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        voiceRipple.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        // set inner icon
        voiceRipple.setRecordDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_mic_none_black_48dp), ContextCompat.getDrawable(getActivity(), R.drawable.ic_mic_black_48dp));
        voiceRipple.setIconSize(45); //30

        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            public void run() {
                try {
                    loading.smoothToShow();
                    voiceRipple.reset();
                    voiceRipple.setRecordDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.plain), ContextCompat.getDrawable(getActivity(), R.drawable.plain));
                    voiceRipple.setClickable(false);
                    playButton.setVisibility(View.GONE);
                    progressText.setText("Processing audio..");
                    preprocess();
                }
                catch (RuntimeException ex) {
                    Toast.makeText(getActivity(), "Something went wrong", Toast.LENGTH_SHORT).show();
                }
            }
        };

        voiceRipple.setOnClickListener(new View.OnClickListener() {

            @Override

            public void onClick(View view) {
                if (voiceRipple.isRecording()) {
                    voiceRipple.stopRecording();
                    voiceRipple.reset();
                    handler.removeCallbacks(runnable);
                } else {
                    try {
                        deleteFilesInDir(directory);
                        voiceRipple.startRecording();
                        handler.postDelayed(runnable, 5000);
                    }
                    catch(RuntimeException e){

                    }
                }
            }

        });

        currentRenderer = new TimerCircleRippleRenderer(getDefaultRipplePaint(), getDefaultRippleBackgroundPaint(), getButtonPaint(), getArcPaint(), 5000.0, 0.0);

        if (currentRenderer instanceof TimerCircleRippleRenderer) {
            ((TimerCircleRippleRenderer) currentRenderer).setStrokeWidth(20);
        }
        voiceRipple.setRenderer(currentRenderer);
        return rootView;
    }

    private void preprocess(){
        String command = "-i " + directory + "/audio.mp3 -lavfi showspectrumpic " + directory + "/spectrogram.png";
        String []cmd = command.split(" ");

        execFFMPEG(cmd);
    }

    private Paint getArcPaint() {
        Paint paint = new Paint();
        paint.setColor(ContextCompat.getColor(getActivity(), R.color.colorPrimary));
        paint.setStrokeWidth(20);
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.SQUARE);
        paint.setStyle(Paint.Style.STROKE);
        return paint;
    }

    private Paint getDefaultRipplePaint() {
        Paint ripplePaint = new Paint();
        ripplePaint.setStyle(Paint.Style.FILL);
        ripplePaint.setColor(ContextCompat.getColor(getActivity(), R.color.colorPrimary));
        ripplePaint.setAntiAlias(true);
        return ripplePaint;
    }

    private Paint getDefaultRippleBackgroundPaint() {
        Paint rippleBackgroundPaint = new Paint();
        rippleBackgroundPaint.setStyle(Paint.Style.FILL);
        rippleBackgroundPaint.setColor((ContextCompat.getColor(getActivity(), R.color.colorPrimaryDark) & 0x00FFFFFF) | 0x40000000);
        rippleBackgroundPaint.setAntiAlias(true);
        return rippleBackgroundPaint;
    }



    private Paint getButtonPaint() {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        return paint;
    }

    @Override
    public void onStop() {
        super.onStop();

        try {
            voiceRipple.onStop();
        }
        catch (IllegalStateException e) {
            Log.e(TAG, "onStop(): ", e);
        }
        if (player != null) {
            player.release();
            player = null;
        }
        deleteFilesInDir(directory);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        voiceRipple.onDestroy();
        deleteFilesInDir(directory);
    }

    @Override
    public void onResume() {
        super.onResume();
        progressText.setText("Tap to Record");
    }

    private boolean deleteFilesInDir(File path) {
        try {
            FileUtils.cleanDirectory(path);
        } catch (Exception ex) {
            Log.e(" Failed to delete: ", ex.getMessage());
        }
        return true;
    }

    private void execFFMPEG(String[] cmd){
        FFmpeg ffmpeg = FFmpeg.getInstance(getActivity());
        try {
            ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() {
                }

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
                    Log.i("Sucess:", message);
                }

                @Override
                public void onFinish() {
                    Log.i("FFMPEG", "finish");
                    loading.smoothToHide();
                    playButton.setVisibility(View.VISIBLE);
                    progressText.setText("Tap to Record");
                    voiceRipple.setRecordDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_mic_none_black_48dp), ContextCompat.getDrawable(getActivity(), R.drawable.ic_mic_black_48dp));
                    voiceRipple.setClickable(true);
                    // initTensorFlowAndLoadModel();
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // Handle if FFmpeg is already running
        }
    }
}