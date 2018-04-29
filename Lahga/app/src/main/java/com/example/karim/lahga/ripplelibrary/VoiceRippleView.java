package com.example.karim.lahga.ripplelibrary;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import java.io.File;
import com.example.karim.lahga.R;
import com.example.karim.lahga.ripplelibrary.listener.RecordingListener;
import com.example.karim.lahga.ripplelibrary.renderer.Renderer;
import com.example.karim.lahga.ripplelibrary.renderer.TimerCircleRippleRenderer;
import omrecorder.AudioChunk;
import omrecorder.AudioRecordConfig;
import omrecorder.OmRecorder;
import omrecorder.PullTransport;
import omrecorder.PullableSource;
import omrecorder.Recorder;
import omrecorder.WriteAction;

import static com.example.karim.lahga.MainActivity.audioAmplitudes;

/**
 * Created by jihyokkim on 2017. 8. 24..
 */

public class VoiceRippleView extends View implements TimerCircleRippleRenderer.TimerRendererListener {
  private static final String TAG = "VoiceRippleView";
  private static final double AMPLITUDE_REFERENCE = 32767.0;
  private static int MIN_RADIUS;
  private static int MIN_ICON_SIZE;
  private static int MIN_FIRST_RIPPLE_RADIUS;
  private static final int INVALID_PARAMETER = -1;
  private File directory;
  private int buttonRadius;
  private int rippleRadius;
  private int backgroundRadius;
  private int iconSize;
  private boolean isRecording;
  private int rippleDecayRate = INVALID_PARAMETER;
  private int thresholdRate = INVALID_PARAMETER;
  private double backgroundRippleRatio = INVALID_PARAMETER;
  private Recorder Omrecorder;
  private Drawable recordIcon;
  private Drawable recordingIcon;
  private OnClickListener listener;
  private Handler handler;  // Handler for updating ripple effect
  private RecordingListener recordingListener;
  private Renderer currentRenderer;
  private int currentRecordedTime = 0;


  public void setRenderer(Renderer currentRenderer) {
    this.currentRenderer = currentRenderer;
    if (currentRenderer instanceof TimerCircleRippleRenderer) {
      ((TimerCircleRippleRenderer) currentRenderer).setTimerRendererListener(this);
    }
    invalidate();
  }

  public VoiceRippleView(Context context) {
    super(context);
    init(context, null);
  }

  public VoiceRippleView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context, attrs);
  }

  public VoiceRippleView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context, attrs);
  }

  private int minFirstRadius;

  private void init(Context context, AttributeSet attrs) {
    MIN_RADIUS = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());
    MIN_ICON_SIZE = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
    MIN_FIRST_RIPPLE_RADIUS = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics());
    TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.VoiceRippleView, 0, 0);
    try {
      rippleRadius = a.getInt(R.styleable.VoiceRippleView_VoiceRippleView_rippleRadius, MIN_RADIUS);
      iconSize = a.getInt(R.styleable.VoiceRippleView_VoiceRippleView_iconSize, MIN_ICON_SIZE);
    } finally {
      a.recycle();
    }

    backgroundRadius = rippleRadius;
    buttonRadius = backgroundRadius;
    minFirstRadius =  MIN_FIRST_RIPPLE_RADIUS;
    handler = new Handler();

    this.setClickable(true);
    this.setEnabled(true);
    this.setFocusable(true);
    this.setFocusableInTouchMode(true);

    setBackgroundRippleRatio(1.1);
    setRippleDecayRate(Rate.MEDIUM);
    setRippleSampleRate(Rate.LOW);
  }

  public void onStop() throws IllegalStateException {
    if (isRecording) {
        stopRecording();
    }
  }

  public void onDestroy() {
    if (isRecording) {
        stopRecording();
    }
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent event) {
    if(event.getAction() == MotionEvent.ACTION_UP) {
      if(listener != null) listener.onClick(this);
    }
    return super.dispatchTouchEvent(event);
  }


  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    if(event.getAction() == KeyEvent.ACTION_UP && (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
      if(listener != null) listener.onClick(this);
    }
    return super.dispatchKeyEvent(event);
  }

  @Override
  public void setOnClickListener(OnClickListener listener) {
    this.listener = listener;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    int viewWidthHalf = this.getMeasuredWidth() / 2;
    int viewHeightHalf = this.getMeasuredHeight() / 2;

    currentRenderer.render(canvas, viewWidthHalf, viewHeightHalf, buttonRadius, rippleRadius, backgroundRadius);

    if (isRecording) {
      recordingIcon.setBounds(viewWidthHalf - iconSize, viewHeightHalf - iconSize, viewWidthHalf + iconSize, viewHeightHalf + iconSize);
      recordingIcon.draw(canvas);
    } else {
      recordIcon.setBounds(viewWidthHalf - iconSize, viewHeightHalf - iconSize, viewWidthHalf + iconSize, viewHeightHalf + iconSize);
      recordIcon.draw(canvas);
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int minw =  getPaddingLeft() + getPaddingRight();
    int w = resolveSizeAndState(minw, widthMeasureSpec, 0);

    int minh =  getPaddingBottom() + getPaddingTop();
    int h = resolveSizeAndState(minh, heightMeasureSpec, 0);

    setMeasuredDimension(w, h);
  }

  public boolean isRecording() {
    return isRecording;
  }

  public void setIconSize(int dpSize) {
    this.iconSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpSize / 2, getResources().getDisplayMetrics());
    invalidate();
  }

  public void setRippleColor(int color) {
    currentRenderer.changeColor(color);
    invalidate();
  }

  public void setRippleSampleRate(Rate rate) {
    switch (rate) {
      case LOW:
        this.thresholdRate = 5;
        break;
      case MEDIUM:
        this.thresholdRate = 10;
        break;
      case HIGH:
        this.thresholdRate = 20;
        break;
    }
    invalidate();
  }

  public void setRippleDecayRate(Rate rate) {
    switch (rate) {
      case LOW:
        this.rippleDecayRate = 20;
        break;
      case MEDIUM:
        this.rippleDecayRate = 10;
        break;
      case HIGH:
        this.rippleDecayRate = 5;
        break;
    }
    invalidate();
  }

  public void setBackgroundRippleRatio(double ratio) {
    this.backgroundRippleRatio = ratio;
    minFirstRadius = (int) (MIN_FIRST_RIPPLE_RADIUS + (MIN_FIRST_RIPPLE_RADIUS * backgroundRippleRatio));
    invalidate();
  }

  /**
   * Calculating decibels from amplitude requires the following: power_db = 20 * log10(amp / amp_ref);
   * 0db is the maximum, and everything else is negative
   * @param amplitude
   */
  private void drop(int amplitude) {
    int powerDb = (int)(20.0 * Math.log10((double) amplitude / AMPLITUDE_REFERENCE));

    // clip if change is below threshold
    final int THRESHOLD = (-1 * powerDb) / thresholdRate;

    if (THRESHOLD >= 0) {
      if (rippleRadius - THRESHOLD >= powerDb + MIN_RADIUS + minFirstRadius || powerDb + MIN_RADIUS + minFirstRadius >= rippleRadius + THRESHOLD) {
        rippleRadius = powerDb + MIN_RADIUS + minFirstRadius;
        backgroundRadius = (int) (rippleRadius * backgroundRippleRatio);
      } else {
        // if decreasing velocity reached 0, it should simply match with ripple radius
        if (((backgroundRadius - rippleRadius) / rippleDecayRate) == 0) {
          backgroundRadius = rippleRadius;
          rippleRadius = buttonRadius;
        } else {
          backgroundRadius = backgroundRadius - ((backgroundRadius - rippleRadius) / rippleDecayRate);
          rippleRadius = rippleRadius - ((rippleRadius - buttonRadius) / rippleDecayRate);
        }
      }

      invalidate();
    }
  }

  @Override
  public void stopRecording() {
      isRecording = false;
      rippleRadius = 0;
      backgroundRadius = 0;
      if (currentRenderer instanceof TimerCircleRippleRenderer) {
          ((TimerCircleRippleRenderer) currentRenderer).setCurrentTimeMilliseconds(0);
      }
    try {
      Omrecorder.stopRecording();
    } catch (Exception e) {
        Log.e(TAG, "stopRecording(): ", e);
    }
    handler.removeCallbacks(updateRipple);
    currentRecordedTime = 0;
    invalidate();
    if (recordingListener != null) {
        recordingListener.onRecordingStopped();
    }
  }

    @Override
    public void startRecording() {
        isRecording = true;
        try {
            setupRecorder();
            Omrecorder.startRecording();
            handler.post(updateRipple);
            invalidate();
            if (recordingListener != null)
                recordingListener.onRecordingStarted();
        } catch (Exception e) {
        Log.e(TAG, "startRecording(): ", e);
        }
  }

  private Runnable updateRipple = new Runnable() {
    @Override
    public void run() {
      if (isRecording) {
        //drop(1000); //recorder.getMaxAmplitude()
        currentRecordedTime = currentRecordedTime + 25;
        if (currentRenderer instanceof TimerCircleRippleRenderer) {
          ((TimerCircleRippleRenderer)currentRenderer).setCurrentTimeMilliseconds(currentRecordedTime);
        }
        handler.postDelayed(this, 25);  // updates the visualizer every 25 milliseconds
      }
    }
  };

  public void setRecordDrawable(Drawable recordIcon, Drawable recordingIcon) {
    this.recordIcon = recordIcon;
    this.recordingIcon = recordingIcon;
    invalidate();
  }

  public void setRecordingListener(RecordingListener recordingListener) {
    this.recordingListener = recordingListener;
  }

  private void setupRecorder(){
    Omrecorder = OmRecorder.wav(
              new PullTransport.Default(mic(),
                      new PullTransport.OnAudioChunkPulledListener() {
                          @Override public void onAudioChunkPulled(AudioChunk audioChunk) {
                              if (isRecording) {
                                  if (audioChunk.maxAmplitude() > 40) {
                                      drop((int) audioChunk.maxAmplitude() * 8);
                                      audioAmplitudes = audioAmplitudes + (int) audioChunk.maxAmplitude();
                                  }else
                                      drop(10);
                              }
                              else
                                  drop(10);
                          }
                      }), file());
  }

  private PullableSource mic () {
    return new PullableSource.NoiseSuppressor(
        new PullableSource.Default(
            new AudioRecordConfig.Default(
                MediaRecorder.AudioSource.MIC, AudioFormat.ENCODING_PCM_16BIT,
                AudioFormat.CHANNEL_IN_MONO, 48000)));
  }

  public void setDirectory(File directory) {this.directory = directory;}

  @NonNull
  private File file () { return new File(directory, "audio.wav"); }
}
