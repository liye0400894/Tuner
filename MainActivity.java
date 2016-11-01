package com.example.li.tuner;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class MainActivity extends AppCompatActivity {
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    public SoundRecorder Tuner; // AudioRecord object

    //public PlaybackTask m_PlaybackTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // create button objects
        ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggle);
        toggleButton.setOnCheckedChangeListener(new Toggle_Listener(this));

        Button resetRecord = (Button)findViewById(R.id.reset);
        resetRecord.setOnClickListener(new Reset_Button_Listener(this));

        Button spectrumAnalysisButton = (Button)findViewById(R.id.analysis);
        spectrumAnalysisButton.setOnClickListener(new Analysis_Button_Listener(this));

        // disable buttons
        enableButton(R.id.reset,false);
        enableButton(R.id.analysis,false);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        Tuner = new SoundRecorder();
    }

    public void enableButton(int id,boolean isEnable){
        ((Button)findViewById(id)).setEnabled(isEnable);
    }

    // TODO: add onResume(), onRestat(), onPause() and onDestroy()

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.li.tuner/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        Tuner.Close();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.li.tuner/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}

/**
* class Toggle_Listener, call AudioRecord method to record sound
* */
class Toggle_Listener implements OnCheckedChangeListener {
    private MainActivity activity;
    private RecordTask m_RecordTask;

    public Toggle_Listener(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        String buttonText;

        if (isChecked)
        {
            Log.d("Record Button","Stop recording");

            activity.Tuner.StopRecording();

            // UI operation
            buttonText = "Start Recording";
            activity.enableButton(R.id.reset, true); // enable reset when recoding is stopped
            activity.enableButton(R.id.analysis, true); // enable button to do spectrum analysis
        }
        else
        {
            Log.d("Record Button","Start recording");

            File recordFileDir = activity.getFilesDir();

            // record sound in new thread
            m_RecordTask = new RecordTask(recordFileDir.toString());
            m_RecordTask.execute();

            // UI operation
            buttonText = "Stop Recording";
            activity.enableButton(R.id.reset,false); // cannot reset during recording
            activity.enableButton(R.id.analysis, false); // cannot analyse during recording
        }
        buttonView.setText(buttonText);
    }

    private class RecordTask extends AsyncTask<String, Integer, String> {
        String pathStr = "";

        public RecordTask(String filePath){
            this.pathStr = filePath;
        }

        @Override
        protected void onPreExecute() {
            Log.d("Record button", "onPreExecute() called");
        }

        @Override
        protected String doInBackground(String... params) {
            Log.d("Record button", "Execute() called");
            activity.Tuner.StartRecord(pathStr);
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d("Record button", "onPostExecute() called");

            //activity.enableButton(R.id.playback, true);
        }
    }
}

/**
* class Playback_Button_Listener, call AudioRecord method to play sound
*/
class Playback_Button_Listener implements View.OnClickListener {
    private MainActivity activity;
    private PlaybackTask m_PlaybackTask;

    public Playback_Button_Listener(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onClick(View v) {
        //activity.Tuner.PlaySound();
        File recordFileDir = activity.getFilesDir();
        // play sound in new thread
        m_PlaybackTask = new PlaybackTask(recordFileDir.toString());
        m_PlaybackTask.execute();
        //activity.enableButton(R.id.playback, false);
        activity.enableButton(R.id.analysis, false);
    }

    private class PlaybackTask extends AsyncTask<String, Integer, String> {
        String pathStr = "";

        public PlaybackTask(String filePath){
            this.pathStr = filePath;
        }

        @Override
        protected String doInBackground(String... params) {
            Log.d("Play button", "Execute() called");
            activity.Tuner.PlaySound(pathStr);
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d("Play button", "onPostExecute() called");
            //textView.setText(result);

            //activity.enableButton(R.id.playback, true);
            activity.enableButton(R.id.analysis, true);
        }
    }
}

/**
 * class Reset_Button_Listener, call AudioRecord method to clear record files and reset AudioRecord object for new recording
 */
class Reset_Button_Listener implements View.OnClickListener {
    private MainActivity activity;

    public Reset_Button_Listener(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onClick(View v) {
        // TODO: add content here
        String filePath = activity.getFilesDir().toString();

        activity.Tuner.Reset(filePath);

        activity.enableButton(R.id.toggle, true);
        activity.enableButton(R.id.analysis, false);
        activity.enableButton(R.id.reset, false);
    }
}

/**
 * class Analysis_Button_Listener, call AudioRecord method to analyse sound sample
 */
class Analysis_Button_Listener implements View.OnClickListener {
    private MainActivity activity;
    private AnalyseSpectrum m_analysis;

    public Analysis_Button_Listener(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onClick(View v) {
        activity.enableButton(R.id.analysis, false);
        activity.enableButton(R.id.reset, false);

        // analyse sound sample in new thread
        m_analysis = new AnalyseSpectrum(activity.getFilesDir().toString());
        m_analysis.execute();

    }

    private class AnalyseSpectrum extends AsyncTask<Void, String, Void> {
        String filePath;

        public AnalyseSpectrum(String path){
            filePath = path;
        }

        @Override
        protected Void doInBackground(Void... para) {
            Log.d("Analysis button", "Execute() called");

            String path = filePath.toString() + File.separator + "test.pcm";

            // buffer to contain sound data
            double audioDataBuffer[] = new double[activity.Tuner.FFT_N];

            // read a bit data from file a time, then send data to sound analysis thread
            try {
                DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(path)));

                // stream data from file, for every m_FFT.FFT_N data, calculate a feature spectrum
                while (dis.available() > 0) {
                    int i = 0;
                    // for each time, read audioDataBuffer.length data and process
                    while (dis.available() > 0 && i < audioDataBuffer.length) {
                        audioDataBuffer[i] = (double)dis.readShort();
                        i++;
                    }

                    int[] m_octave = {0}, m_note = {0};
                    double[] m_biased = {0.0}, m_Hz = {0.0};
                    activity.Tuner.SpectrumAnalysis(audioDataBuffer, m_octave, m_note, m_biased, m_Hz);

                    String noteString, result;
                    // return sound info as string
                    if ((m_octave[0] != 0) || (m_note[0] != 0)) {
                        switch (m_note[0]){
                            case 0:  noteString = "C"; break;
                            case 1:  noteString = "C#Db"; break;
                            case 2:  noteString = "D"; break;
                            case 3:  noteString = "D#Eb"; break;
                            case 4:  noteString = "E"; break;
                            case 5:  noteString = "F"; break;
                            case 6:  noteString = "F#Gb"; break;
                            case 7:  noteString = "G"; break;
                            case 8:  noteString = "G#Ab"; break;
                            case 9:  noteString = "A"; break;
                            case 10: noteString = "A#Bb"; break;
                            case 11: noteString = "B"; break;
                            default: noteString = "C";
                        }
                        result = String.format("%.2f", m_Hz[0]).toString() + " Hz " + System.getProperty("line.separator")
                                + noteString + Integer.toString(m_octave[0]) + System.getProperty("line.separator")
                                + String.format("%.2f", m_biased[0]*100) + "% biased";
                    }else{
                        result = "Cannot find match";
                    }
                    publishProgress(result);
                }
                dis.close();

            } catch (IOException e) {
                Log.d("SoundRecorder", "Cannot write sound to file");
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... updateInfo){
            Log.d("Analysis button", "onProgressUpdate() called");

            // update sound info to TextView at somewhere else
            TextView soundInfoText = (TextView)activity.findViewById(R.id.AnalysisResultText);
            soundInfoText.setText(updateInfo[0]);
        }

        @Override
        protected void onPostExecute(Void values) {
            Log.d("Analysis button", "onPostExecute() called");
            activity.enableButton(R.id.analysis, true);
            activity.enableButton(R.id.reset, true);
        }
    }
}
