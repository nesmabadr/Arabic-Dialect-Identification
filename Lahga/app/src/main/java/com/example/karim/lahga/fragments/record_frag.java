package com.example.karim.lahga.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.example.karim.lahga.ImageClassifier;
import com.example.karim.lahga.MainActivity;
import com.example.karim.lahga.OpenSansSBTextView;
import com.example.karim.lahga.R;
import com.example.karim.lahga.historyAdapter;
import com.example.karim.lahga.history_item;
import com.example.karim.lahga.ripplelibrary.Rate;
import com.example.karim.lahga.ripplelibrary.VoiceRippleView;
import com.example.karim.lahga.ripplelibrary.listener.RecordingListener;
import com.example.karim.lahga.ripplelibrary.renderer.Renderer;
import com.example.karim.lahga.ripplelibrary.renderer.TimerCircleRippleRenderer;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.OnClickListener;
import com.orhanobut.dialogplus.OnItemClickListener;
import com.orhanobut.dialogplus.ViewHolder;
import com.wang.avi.AVLoadingIndicatorView;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by karim on 2/24/2018.
 */

public class record_frag extends Fragment {

    private static final String TAG = "RECORD";
    private static final String DIRECTORY_NAME = "AudioCache";
    private OpenSansSBTextView playButton;
    private OpenSansSBTextView progressText;
    private MediaPlayer player = null;
    private File directory = null;
    private File audioFile = null;
    private VoiceRippleView voiceRipple;
    private Renderer currentRenderer;
    private AVLoadingIndicatorView loading;
    private DialogPlus predictionDialog;
    private ImageClassifier imageClassifier;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.record_frag, container, false);
        loading =  rootView.findViewById(R.id.avi);
        voiceRipple = rootView.findViewById(R.id.voice_ripple_view);
        progressText = rootView.findViewById(R.id.textView);

        playButton = rootView.findViewById(R.id.button);

        if (((MainActivity)getActivity()).playEnabled)
            playButton.setVisibility(View.VISIBLE);
        else
            playButton.setVisibility(View.GONE);

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
        voiceRipple.setBackgroundRippleRatio(1.49);

        // set recorder related settings for ripple view
        voiceRipple.setMediaRecorder(new MediaRecorder());
        voiceRipple.setOutputFile(audioFile.getAbsolutePath());
        voiceRipple.setAudioSource(MediaRecorder.AudioSource.MIC);
        voiceRipple.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        voiceRipple.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        // set inner icon
        voiceRipple.setRecordDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_mic_none_black_48dp), ContextCompat.getDrawable(getActivity(), R.drawable.ic_mic_black_48dp));
        voiceRipple.setIconSize(45);

        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            public void run() {
                try {
                    loading.smoothToShow();
                    voiceRipple.reset();
                    voiceRipple.setRecordDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.plain), ContextCompat.getDrawable(getActivity(), R.drawable.plain));
                    voiceRipple.setClickable(false);
                    progressText.setText("Processing Audio..");
                    preProcess();
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

        predictionDialog = DialogPlus.newDialog(getActivity())
                .setOnItemClickListener(new OnItemClickListener() {
                    @Override
                    public void onItemClick(DialogPlus dialog, Object item, View view, int position) {
                    }
                })
                .setContentHolder(new ViewHolder(R.layout.prediction_diag))
                .setContentBackgroundResource(R.color.colorPrimary)
                .setContentHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
                .setCancelable(true)
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(DialogPlus dialog, View view) {
                        switch (view.getId()) {
                            case R.id.diag_okay:
                                dialog.dismiss();
                                break;
                        }
                    }
                })
                .create();

        return rootView;
    }

    private void preProcess(){
        String command = "-i " + directory + "/audio.mp3 -lavfi showspectrumpic=s=1920x1080 " + directory + "/spectrogram.png";
        String []cmd = command.split(" ");
        execFFMPEG(cmd);
    }

    private Paint getArcPaint() {
        Paint paint = new Paint();
        paint.setColor(ContextCompat.getColor(getActivity(), R.color.colorPrimaryLessDark));
        paint.setStrokeWidth(20);
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.SQUARE);
        paint.setStyle(Paint.Style.STROKE);
        return paint;
    }

    private Paint getDefaultRipplePaint() {
        Paint ripplePaint = new Paint();
        ripplePaint.setStyle(Paint.Style.FILL);
        ripplePaint.setColor(ContextCompat.getColor(getActivity(), R.color.colorPrimaryLessDark));
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

        try { voiceRipple.onStop(); }
        catch (IllegalStateException e) { Log.e(TAG, "onStop(): ", e); }
        if (player != null) {
            player.release();
            player = null;
        }
        deleteFilesInDir(directory);
        ((MainActivity)getActivity()).playEnabled = false;
    }

    @Override
    public void onDestroy() {
        voiceRipple.onDestroy();
        deleteFilesInDir(directory);
        imageClassifier.close();
        super.onDestroy();
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
                public void onSuccess(String message) { Log.i("Sucess:", message); }
                @Override
                public void onFinish() {
                    Log.i("FFMPEG", "finish");
                    initTensorFlowLite();
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // Handle if FFmpeg is already running
        }
    }

    private void initTensorFlowLite(){
        try {
            imageClassifier = new ImageClassifier(getActivity());
            final Handler handler = new Handler();
            final Runnable runnable = new Runnable() {
                public void run() {
                        classifyFrame();
                }
            };
            handler.post(runnable);
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize image classifier");
        }
    }

    private void classifyFrame() {
        if (imageClassifier == null || getActivity() == null) {
            showPredictionDialog("Uninitialized Classifier or invalid context.");
            return;
        }

        Bitmap bitmap = BitmapFactory.decodeFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) +"/photo.jpg");
        bitmap = Bitmap.createScaledBitmap(bitmap, ImageClassifier.DIM_IMG_SIZE_X, ImageClassifier.DIM_IMG_SIZE_Y, false);
        String textToShow = imageClassifier.classifyFrame(bitmap);
        showPredictionDialog(textToShow);
    }

    private void showPredictionDialog(String prediction) {
        imageClassifier.close();
        String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        ((MainActivity)getActivity()).DBHelper.addHistory(prediction, date);
        ((MainActivity)getActivity()).arrayOfHistory = ((MainActivity)this.getActivity()).DBHelper.getAllHistories();
        ((MainActivity)getActivity()).adapter = new historyAdapter(getActivity(), ((MainActivity)getActivity()).arrayOfHistory);
        ((MainActivity)getActivity()).listView.setAdapter(((MainActivity)getActivity()).adapter);
        ((MainActivity)getActivity()).listView.setMenuCreator(((MainActivity)getActivity()).creator);

        loading.smoothToHide();
        ((MainActivity)getActivity()).playEnabled = true;
        playButton.setVisibility(View.VISIBLE);
        progressText.setText("Tap to Record");
        voiceRipple.setRecordDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_mic_none_black_48dp), ContextCompat.getDrawable(getActivity(), R.drawable.ic_mic_black_48dp));
        voiceRipple.setClickable(true);

        com.example.karim.lahga.OpenSansSBTextView predictionText = (com.example.karim.lahga.OpenSansSBTextView)predictionDialog.findViewById(R.id.textView2);
        predictionText.setText(prediction);
        predictionDialog.show();
    }
}