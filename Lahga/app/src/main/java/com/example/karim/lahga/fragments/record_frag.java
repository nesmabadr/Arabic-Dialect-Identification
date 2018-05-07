package com.example.karim.lahga.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;
import com.example.karim.lahga.MainActivity;
import com.example.karim.lahga.OpenSansSBTextView;
import com.example.karim.lahga.R;
import com.example.karim.lahga.finishListener;
import com.example.karim.lahga.historyAdapter;
import com.example.karim.lahga.ripplelibrary.Rate;
import com.example.karim.lahga.ripplelibrary.VoiceRippleView;
import com.example.karim.lahga.ripplelibrary.listener.RecordingListener;
import com.example.karim.lahga.ripplelibrary.renderer.Renderer;
import com.example.karim.lahga.ripplelibrary.renderer.TimerCircleRippleRenderer;
import com.example.karim.lahga.tensorFlow.Classifier;
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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.example.karim.lahga.MainActivity.NoNoise;
import static com.example.karim.lahga.MainActivity.audioAmplitudes;
import static com.example.karim.lahga.MainActivity.finish;

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
    private VoiceRippleView voiceRipple;
    private Renderer currentRenderer;
    private AVLoadingIndicatorView loading;
    private DialogPlus predictionDialog;
    private Boolean isLoading = false;
    private Boolean playEnabled = false;
    private Switch segmentAudio, suppressNoise;
    private Boolean segment = false;
    private int [] segmentedResults = {0,0,0,0};
    private String fileIndex;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.record_frag, container, false);

        loading =  rootView.findViewById(R.id.avi);
        voiceRipple = rootView.findViewById(R.id.voice_ripple_view);
        progressText = rootView.findViewById(R.id.textView);
        playButton = rootView.findViewById(R.id.button);
        segmentAudio = rootView.findViewById(R.id.switch1);
        suppressNoise = rootView.findViewById(R.id.switch2);

        suppressNoise.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                NoNoise = isChecked;
            }
        });

        segmentAudio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                segment = isChecked;
                if (segment)
                    currentRenderer = new TimerCircleRippleRenderer(getDefaultRipplePaint(), getDefaultRippleBackgroundPaint(), getButtonPaint(), getArcPaint(), 6500.0, 0.0);
                else
                    currentRenderer = new TimerCircleRippleRenderer(getDefaultRipplePaint(), getDefaultRippleBackgroundPaint(), getButtonPaint(), getArcPaint(), 5500.0, 0.0);

                if (currentRenderer instanceof TimerCircleRippleRenderer) {
                    ((TimerCircleRippleRenderer) currentRenderer).setStrokeWidth(20);
                }
                voiceRipple.setRenderer(currentRenderer);
            }
        });

        if (playEnabled)
            playButton.setVisibility(View.VISIBLE);
        else
            playButton.setVisibility(View.GONE);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (directory != null) {
                    if (player == null){
                        player = new MediaPlayer();
                        try {
                            player.setDataSource(directory.getAbsolutePath() + "/audio.wav");
                            player.prepare();
                            player.start();
                        }
                        catch (IOException e) {
                            Log.e(TAG, "prepare() failed");
                        }
                    }
                    else{
                            player.release();
                            player = new MediaPlayer();
                            try {
                                player.setDataSource(directory.getAbsolutePath() + "/audio.wav");
                                player.prepare();
                                player.start();
                            }
                            catch (IOException e) {
                                Log.e(TAG, "prepare() failed");
                            }
                    }
                }
            }
        });

        directory = new File(getActivity().getExternalCacheDir().getAbsolutePath(), DIRECTORY_NAME);
        voiceRipple.setDirectory(directory);

        if (directory.exists()) { deleteFilesInDir(directory); }
        else { directory.mkdirs(); }

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
                playButton.setVisibility(View.INVISIBLE);
                segmentAudio.setClickable(false);
                suppressNoise.setClickable(false);
                if (player != null) {
                    player.release();
                    player = null;
                }
            }
        });

        // set view related settings for ripple view
        voiceRipple.setRippleSampleRate(Rate.HIGH);
        voiceRipple.setRippleDecayRate(Rate.LOW);
        voiceRipple.setBackgroundRippleRatio(1.49);
        // set inner icon
        voiceRipple.setRecordDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_mic_none_black_48dp), ContextCompat.getDrawable(getActivity(), R.drawable.ic_mic_black_48dp));
        voiceRipple.setIconSize(45);
        voiceRipple.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (voiceRipple.isRecording()) {
                    voiceRipple.stopRecording();
                    segmentAudio.setClickable(true);
                    suppressNoise.setClickable(true);
                } else {
                    try {
                        if (directory.exists()) {deleteFilesInDir(directory);}
                        audioAmplitudes = 0;
                        voiceRipple.startRecording();
                    }
                    catch(RuntimeException e){ }
                }
            }
        });

        currentRenderer = new TimerCircleRippleRenderer(getDefaultRipplePaint(), getDefaultRippleBackgroundPaint(), getButtonPaint(), getArcPaint(), 5500.0, 0.0);
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

        finish.setListener(new finishListener.ChangeListener() {
            @Override
            public void onChange() {
                if (finish.isFinish()) {
                    if (audioAmplitudes < 000)
                        Toast.makeText(getActivity(), "Can't hear you!", Toast.LENGTH_SHORT).show();
                    else {
                        try {
                            isLoading = true;
                            voiceRipple.setClickable(false);
                            voiceRipple.setRecordDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.plain), ContextCompat.getDrawable(getActivity(), R.drawable.plain));
                            loading.smoothToShow();
                            progressText.setText("Processing Audio..");
                            if (segment) {
                                processFrame("0", "5");
                            } else {
                                processFrame("0.5", "5.5");
                            }
                        } catch (RuntimeException ex) {
                            Toast.makeText(getActivity(), "Something went wrong", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });
        return rootView;
    }

    private void processFrame(String start, String end){
        fileIndex = start;
        String command = "-i " + directory + "/audio.wav -ss " + start + " -to " + end + " " + directory + "/audioTrim" + fileIndex + ".wav";
        String []cmd = command.split(" ");
        execFFMPEG(cmd, false);
        command = "-i " + directory + "/audioTrim" + fileIndex +".wav -lavfi showspectrumpic=s=224x224:legend=disabled " + directory + "/spectrogram" + fileIndex + ".png";
        cmd = command.split(" ");
        execFFMPEG(cmd, true);
    }

    private Paint getArcPaint() {
        Paint paint = new Paint();
        paint.setColor(ContextCompat.getColor(getActivity(), R.color.colorPrimaryDark));
        paint.setStrokeWidth(20);
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.SQUARE);
        paint.setStyle(Paint.Style.STROKE);
        return paint;
    }

    private Paint getDefaultRipplePaint() {
        Paint ripplePaint = new Paint();
        ripplePaint.setStyle(Paint.Style.FILL);
        ripplePaint.setColor(ContextCompat.getColor(getActivity(), R.color.colorPrimaryDark));
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
        if (isLoading)
            loading.smoothToHide();
        voiceRipple.onStop();
        if (directory.exists()) { deleteFilesInDir(directory); }
        playEnabled = false;
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        voiceRipple.onDestroy();
        if (directory.exists()) { deleteFilesInDir(directory); }
        if ( ((MainActivity)getActivity()).classifier != null)
            ((MainActivity)getActivity()).classifier.close();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isLoading) {
            progressText.setText("Tap to Record");
            voiceRipple.setRecordDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_mic_none_black_48dp), ContextCompat.getDrawable(getActivity(), R.drawable.ic_mic_black_48dp));
            voiceRipple.setClickable(true);
        }
    }

    private boolean deleteFilesInDir(File path) {
        try {
            FileUtils.cleanDirectory(path);
        } catch (Exception ex) {
            Log.e(" Failed to delete: ", ex.getMessage());
        }
        return true;
    }

    private void execFFMPEG(String[] cmd, final Boolean classify){
        FFmpeg ffmpeg = FFmpeg.getInstance(getActivity());
        try {
            ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {
                @Override
                public void onStart() {}
                @Override
                public void onProgress(String message) {
                    //Log.i("FFMPEG", "start");
                    //Log.i("Progress:", message);
                }
                @Override
                public void onFailure(String message) {
                    Log.i("Failure:", message);
                }
                @Override
                public void onSuccess(String message) { Log.i("Sucess:", message); }
                @Override
                public void onFinish() {
                    //Log.i("FFMPEG", "finish");
                    if(classify) {
                        classifyFrame();
                    }
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
                Log.i("FFMPEG", e.toString());
        }
    }

    private void classifyFrame() {
        if (((MainActivity)getActivity()).classifier == null || getActivity() == null) {
            showPredictionDialog("Uninitialized Classifier or invalid context.");
            return;
        }
        if (new File( directory + "/spectrogram" + fileIndex + ".png").exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile( directory + "/spectrogram" + fileIndex + ".png");
            //bitmap = Bitmap.createScaledBitmap(bitmap, 244, 244, false);
            final List<Classifier.Recognition> results = ((MainActivity)getActivity()).classifier.recognizeImage(bitmap);
            Log.i("PREDICTION",results.get(0).getTitle() + " " + results.get(0).getConfidence());
            if (segment){
                segmentedResults[Integer.parseInt(results.get(0).getId())] = segmentedResults[Integer.parseInt(results.get(0).getId())] + 1;
                int sum = 0;
                for (int i = 0; i<4; i++){
                    sum = sum + segmentedResults[i];
                }
                if (sum == 1)
                    processFrame("1", "6");
                if (sum == 2)
                    processFrame("2", "7");
                if (sum == 3){
                    int maxAt = 0;
                    for (int i = 0; i < segmentedResults.length; i++) {
                        maxAt = segmentedResults[i] > segmentedResults[maxAt] ? i : maxAt;
                    }
                    Arrays.fill(segmentedResults, 0);
                    switch(maxAt){
                        case 0:
                            showPredictionDialog("Egyptian");
                            break;
                        case 1:
                            showPredictionDialog("Gulf");
                            break;
                        case 2:
                            showPredictionDialog("Levantine");
                            break;
                        case 3:
                            showPredictionDialog("North African");
                            break;
                    }
                }
            }
            else
                showPredictionDialog(results.get(0).getTitle());
        }
        else
            Log.i("Image Classifier", "Spectrogram not found");
    }

    private void showPredictionDialog(String prediction) {
        String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        ((MainActivity)getActivity()).DBHelper.addHistory(prediction, date);
        ((MainActivity)getActivity()).arrayOfHistory = ((MainActivity)this.getActivity()).DBHelper.getAllHistories();
        ((MainActivity)getActivity()).adapter = new historyAdapter(getActivity(), ((MainActivity)getActivity()).arrayOfHistory);
        ((MainActivity)getActivity()).listView.setAdapter(((MainActivity)getActivity()).adapter);
        ((MainActivity)getActivity()).listView.setMenuCreator(((MainActivity)getActivity()).creator);
        isLoading = false;
        loading.smoothToHide();
        progressText.setText("Tap to Record");
        voiceRipple.setRecordDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_mic_none_black_48dp), ContextCompat.getDrawable(getActivity(), R.drawable.ic_mic_black_48dp));
        voiceRipple.setClickable(true);

        com.example.karim.lahga.OpenSansSBTextView predictionText = (com.example.karim.lahga.OpenSansSBTextView)predictionDialog.findViewById(R.id.textView2);
        predictionText.setText(prediction);
        Vibrator v = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(100);
        predictionDialog.show();
        playEnabled = true;
        playButton.setVisibility(View.VISIBLE);
        segmentAudio.setClickable(true);
        suppressNoise.setClickable(true);
        finish.setFinish(false);
    }
}