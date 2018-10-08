package com.example.jeremy.musicplayer;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private ArrayList<SongInfo> _songs = new ArrayList<SongInfo>();;
    RecyclerView recyclerView;
    SongAdapter songAdapter;
    MediaPlayer mediaPlayer;
    private TextView counterTxt;
    private int currentMusicIndex;
    private int pausePosition;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Thread thread;
    private boolean plotData = true;
    private Boolean isUp = true;
    private Boolean hold = false;
    private Integer downCounter = 0;
    private Float lux;
    Timer timer = new Timer(true);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        songAdapter = new SongAdapter(this,_songs);
        recyclerView.setAdapter(songAdapter);
        mediaPlayer = new MediaPlayer();
        counterTxt = findViewById(R.id.counter);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        timerHandler.postDelayed(timerRunnable, 0);
        if (mSensor != null) {
            mSensorManager.registerListener((SensorEventListener) this, mSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        TimerTask task = new TimerTask() {
            public void run() {
                downCounter = 0;
            }
        };
        timer.schedule(task, 0, 5000);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                linearLayoutManager.getOrientation());
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.addItemDecoration(dividerItemDecoration);
        songAdapter.setOnItemClickListener(new SongAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, ArrayList<SongInfo> obj, int position) {
                currentMusicIndex = position;
                pausePosition = 0;
                play();
            }
        });
        checkUserPermission();
        feedMultiple();


    }








    private void pause() {
        if(mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            pausePosition = mediaPlayer.getCurrentPosition();
        }

    }

    private void CloseVolume(){
        mediaPlayer.setVolume(0.1f, 0.1f);
    }


    private void nextSong(){
            if((++currentMusicIndex)<_songs.size()){
                setPlay(currentMusicIndex);
            }

        }

    private void setPlay(int location){
        mediaPlayer.reset();
        try{
            mediaPlayer.setDataSource(_songs.get(location).getSongUrl());
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void play() {
        mediaPlayer.reset();
        try{
            mediaPlayer.setDataSource(_songs.get(currentMusicIndex).getSongUrl());
            mediaPlayer.prepare();
            mediaPlayer.seekTo(pausePosition);
            mediaPlayer.getCurrentPosition();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void checkUserPermission(){
        if(Build.VERSION.SDK_INT>=23){
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},123);
                return;
            }
        }
        loadSongs();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 123:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    loadSongs();
                }else{
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    checkUserPermission();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        }

    }

    private void loadSongs(){
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC+"!=0";
        Cursor cursor = getContentResolver().query(uri,null,selection,null,MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        if(cursor != null){
            if(cursor.moveToFirst()){
                do{
                    String name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                    String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));

                    SongInfo s = new SongInfo(name,artist,url);
                    _songs.add(s);

                }while (cursor.moveToNext());
            }

            cursor.close();
            songAdapter = new SongAdapter(MainActivity.this,_songs);

        }
    }

    private void addEntry(SensorEvent event) {

            float preLux = 0;
            if (lux != null){
                preLux = lux;
            }
            lux = event.values[0];

            if (preLux < lux && (lux - preLux)/lux >= .1 && !isUp && !hold){


                isUp = true;
                hold = true;
            } else if (preLux > lux && (preLux - lux)/preLux >= .1 && isUp && !hold){
                isUp = false;

                downCounter +=1;



            }
        counterTxt.setText("Counter: "+ downCounter);



        }


    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {

        if(plotData){
            addEntry(event);
            plotData = false;
        }
    }

    private void feedMultiple() {

        if (thread != null){
            thread.interrupt();
        }

        thread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (true){
                    plotData = true;
                    if(downCounter == 1){
                        pause();
                    }else if(downCounter ==2){
                        play();
                    }else if(downCounter ==3){
                        nextSong();
                    }else if(downCounter ==4){
                        CloseVolume();
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        thread.start();
    }

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            if (hold){
                hold = false;
            }

            timerHandler.postDelayed(this, 450);

        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener((SensorEventListener) this, mSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onDestroy() {
        mSensorManager.unregisterListener((SensorEventListener) MainActivity.this);
        thread.interrupt();
        super.onDestroy();
    }
}
