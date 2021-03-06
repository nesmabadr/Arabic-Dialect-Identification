package com.example.karim.lahga;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;

import com.example.karim.lahga.SwipeMenuList.SwipeMenuCreator;
import com.example.karim.lahga.SwipeMenuList.SwipeMenuListView;
import com.example.karim.lahga.fragments.about_frag;
import com.example.karim.lahga.fragments.history_frag;
import com.example.karim.lahga.fragments.record_frag;
import com.example.karim.lahga.tensorFlow.Classifier;
import com.example.karim.lahga.tensorFlow.TensorFlowImageClassifier;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    public DataBaseHelper DBHelper;
    public  ArrayList<history_item> arrayOfHistory = new ArrayList<>();
    public historyAdapter adapter;
    public SwipeMenuListView listView;
    public SwipeMenuCreator creator;
    private static final int PERMISSIONS = 200;
    private boolean permissionToRecordAccepted = false;
    private boolean permissionToReadAccepted = false;
    private boolean permissionToWriteAccepted = false;

    private String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private String titles[] = {"Record", "History", "About"};
    public Classifier classifier;
    private static final int INPUT_SIZE = 244;
    private static final String INPUT_NAME = "input_1";
    private static final String OUTPUT_NAME = "output_node0";

    private static final String MODEL_FILE = "file:///android_asset/optimized_graph.pb";
    private static final String LABEL_FILE = "file:///android_asset/labels.txt";
    public static finishListener finish;
    public static int audioAmplitudes = 0;
    public static Boolean NoNoise = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        viewPager = findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            OpenSansSBTextView title = (OpenSansSBTextView) LayoutInflater.from(this).inflate(R.layout.tab_title,null);
            title.setText(titles[i]);
            tabLayout.getTabAt(i).setCustomView(title);
        }

        DBHelper = new DataBaseHelper(getApplicationContext());
        loadFFMPEG();
        classifier = TensorFlowImageClassifier.create(
                getAssets(),
                MODEL_FILE,
                LABEL_FILE,
                INPUT_SIZE,
                INPUT_NAME,
                OUTPUT_NAME);

        finish = new finishListener();
        ActivityCompat.requestPermissions(this, permissions, PERMISSIONS);
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new record_frag(), "Record");
        adapter.addFragment(new history_frag(), "History");
        adapter.addFragment(new about_frag(), "About");
        viewPager.setAdapter(adapter);
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }
        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    private void loadFFMPEG(){
        FFmpeg ffmpeg = FFmpeg.getInstance(this);
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onStart() {}
                @Override
                public void onFailure() {
                    showUnsupportedExceptionDialog();
                }
                @Override
                public void onSuccess() {}
                @Override
                public void onFinish() {}
            });
        } catch (FFmpegNotSupportedException e) { showUnsupportedExceptionDialog(); }
    }

    private void showUnsupportedExceptionDialog() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.device_not_supported))
                .setMessage(getString(R.string.device_not_supported_message))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .create()
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case PERMISSIONS:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                permissionToReadAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                permissionToWriteAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }

        if (!permissionToRecordAccepted || !permissionToReadAccepted || !permissionToWriteAccepted)
            finish();
        else {
            loadFFMPEG();
            classifier = TensorFlowImageClassifier.create(
                    getAssets(),
                    MODEL_FILE,
                    LABEL_FILE,
                    INPUT_SIZE,
                    INPUT_NAME,
                    OUTPUT_NAME);
        }
    }
}