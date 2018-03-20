package com.example.karim.lahga.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import com.example.karim.lahga.R;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import info.kimjihyok.ripplelibrary.Rate;
import info.kimjihyok.ripplelibrary.VoiceRippleView;
import info.kimjihyok.ripplelibrary.listener.RecordingListener;
import info.kimjihyok.ripplelibrary.renderer.Renderer;
import info.kimjihyok.ripplelibrary.renderer.TimerCircleRippleRenderer;

/**
 * Created by karim on 2/24/2018.
 */

public class record_frag extends Fragment {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final String DIRECTORY_NAME = "AudioCache";
    private Button playButton;
    private MediaPlayer player = null;
    private File directory = null;
    private File audioFile = null;
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private VoiceRippleView voiceRipple;
    private Renderer currentRenderer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.record_frag, container, false);
        voiceRipple = rootView.findViewById(R.id.voice_ripple_view);
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

        // directory = new File(Environment.getExternalStorageDirectory(), DIRECTORY_NAME);

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
            }

            @Override
            public void onRecordingStarted() {
                Log.d(TAG, "onRecordingStarted()");
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
                    voiceRipple.reset();
                    Toast.makeText(getActivity(), "Sending data", Toast.LENGTH_SHORT).show();
                    sendData();
                }
                catch(RuntimeException ex){
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
                    voiceRipple.startRecording();
                    handler.postDelayed(runnable, 5000);
                }
            }

        });

        currentRenderer = new TimerCircleRippleRenderer(getDefaultRipplePaint(), getDefaultRippleBackgroundPaint(), getButtonPaint(), getArcPaint(), 5000.0, 0.0);

        if (currentRenderer instanceof TimerCircleRippleRenderer) {
            ((TimerCircleRippleRenderer) currentRenderer).setStrokeWidth(20);
        }
        voiceRipple.setRenderer(currentRenderer);
        ActivityCompat.requestPermissions(getActivity(), permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        return rootView;
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        voiceRipple.onDestroy();
    }

    private boolean deleteFilesInDir(File path) {
        if(path.exists()) {
            File[] files = path.listFiles();
            if (files == null) {
                return true;
            }

            for(int i=0; i<files.length; i++) {
                if (files[i].isDirectory()) {
                    files[i].delete();
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }

        if (!permissionToRecordAccepted )
            getActivity().finish();
    }

    public void sendData() {
        new AsyncTask<Integer, Void, Void>() {
            @Override
            protected Void doInBackground(Integer... params) {
                try {
                    String sshReply = executeRemoteCommand("karimatwa", "Karim@900130589", "10.7.60.151", 22);
                    Log.i("SSH Reply: ", sshReply);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute(1);
    }

    public static String executeRemoteCommand(String username, String password, String hostname, int port)
            throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, hostname, port);
        session.setPassword(password);

        // Avoid asking for key confirmation
        Properties prop = new Properties();
        prop.put("StrictHostKeyChecking", "no");
        session.setConfig(prop);

        session.connect();

        // SSH Channel
        ChannelExec channelssh = (ChannelExec)
                session.openChannel("exec");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        channelssh.setOutputStream(baos);

        // Execute command
        channelssh.setCommand("ls");
        channelssh.connect();
        channelssh.disconnect();

        return baos.toString();
    }
}

