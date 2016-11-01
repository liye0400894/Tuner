package com.example.li.tuner;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 *  Created by Li on 2016/8/19.
 */
public class SoundRecorder {
    // default settings to construct AudioRecord object
    private static final int DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;

    private int DEFAULT_SAMPLE_RATE_IN_HZ = 44100;
    private int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private int DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private static final int bytesPerFrame = 2;

    // AudioRecorder object
    private AudioRecord m_AudioRecord = null;
    private int m_BufferSize = 0;
    private static final int m_Frame_Period = 160; // define every 160 frames as 1 Period
    private boolean m_IsRecording = false;
    private boolean m_IsPlaying = false;
    private boolean m_RecordingCompleted = false; // to identify if a sound has been recorded and is ready to be played

    // file to contain recorded sound
    private File m_RecordFile; // TODO: make it a list of Files to contain multiple sound

    // AudioTrack object, used to play sound
    private AudioTrack track = null;

    // frequency of standard tunes, from C0 to B9, A4 = 440
    private static final double m_StandardFrequency[][] = {
            {16.352, 32.703, 65.406, 130.81, 261.63, 523.25, 1046.5, 2093.0, 4186.0, 8372.0}, // C
            {17.324, 34.648, 69.296, 138.59, 277.18, 554.37, 1108.7, 2217.5, 4434.9, 8869.8}, // C#/Db
            {18.354, 36.708, 73.416, 146.83, 293.66, 587.33, 1174.7, 2349.3, 4698.6, 9397.3}, // D
            {19.445, 38.891, 77.782, 155.56, 311.13, 622.25, 1244.5, 2489.0, 4978.0, 9956.1}, // D#/Eb
            {20.602, 41.203, 82.407, 164.81, 329.63, 659.26, 1318.5, 2637.0, 5274.0, 10548}, // E
            {21.827, 43.654, 87.307, 174.61, 349.23, 698.46, 1396.9, 2793.8, 5587.7, 11175}, // F
            {23.125, 46.249, 92.499, 185.00, 369.99, 739.99, 1480.0, 2960.0, 5919.9, 11840}, // F#/Gb
            {24.500, 48.999, 97.999, 196.00, 392.00, 783.99, 1568.0, 3136.0, 6271.9, 12544}, // G
            {25.957, 51.913, 103.83, 207.65, 415.30, 830.61, 1661.2, 3322.4, 6644.9, 13290}, // G#/Ab
            {27.500, 55.000, 110.00, 220.00, 440.00, 880.00, 1760.0, 3520.0, 7040.0, 14080}, // A
            {29.135, 58.270, 116.54, 233.08, 466.16, 932.33, 1864.7, 3729.3, 7458.6, 14917}, // A#/Bb
            {30.868, 61.735, 123.47, 246.94, 493.88, 987.77, 1975.5, 3951.1, 7902.1, 15804}}; // B

    public FFT m_FFT; // Fast Fourier Transform object
    public static int FFT_N = 4096;
    private static double m_NoiseThreshold = 1.00e-19;

    /*
    * Constructor
     */
    SoundRecorder() {
        // initial AudioRecord object
        initAudioRecord();

        m_FFT = new FFT(FFT_N);
    }

    /**
     * Init AudioRecorder
     */
    private void initAudioRecord() {
        Log.d("SoundRecorder", "Initialising recorder");

        // try device settings until a supported format is found
        int[] mSampleRates = new int[]{8000, 11025, 22050, 44100};
        for (int rate : mSampleRates) {
            DEFAULT_SAMPLE_RATE_IN_HZ = rate;
            for (short audioFormat : new short[]{AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT}) {
                DEFAULT_AUDIO_FORMAT = audioFormat;
                for (short channelConfig : new short[]{AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO}) {
                    DEFAULT_CHANNEL_CONFIG = channelConfig;
                    try {
                        m_AudioRecord = null;
                        Log.d("SoundRecorder", "rate: " + rate + "Hz, bits: " + audioFormat + ", channel: "
                                + channelConfig);
                        m_BufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                        if (m_BufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            // re-calculate m_BufferSize so that it contains integer number of Periods
                            // if number of frame is != n*160, increase NumOfFrameInBuffer
                            // so that NumOfFrameInBuffer = 锛坣+1锛?160
                            int NumOfFrameInBuffer = m_BufferSize / bytesPerFrame;
                            if (NumOfFrameInBuffer % m_Frame_Period != 0) {
                                NumOfFrameInBuffer = NumOfFrameInBuffer - NumOfFrameInBuffer % m_Frame_Period + m_Frame_Period;
                                ;
                                m_BufferSize = NumOfFrameInBuffer * bytesPerFrame;
                            }

                            // check if we can instantiate and have a success
                            //AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate, channelConfig, audioFormat, bufferSize);
                            m_AudioRecord = new AudioRecord(DEFAULT_AUDIO_SOURCE, DEFAULT_SAMPLE_RATE_IN_HZ,
                                    DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT, m_BufferSize);

                            if (m_AudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                                IOException e = new IOException();
                                throw e;
                            } else {
                                // if successed, no need to try one more setting
                                break;
                            }
                        } else {
                            IOException e = new IOException();
                            throw e;
                        }
                    } catch (IOException e) {
                        Log.d("SoundRecorder", "rate: " + rate + "Hz, bits: " + audioFormat + ", channel: " + channelConfig + " is not supported, keep trying");
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Start to record sound
     */
    public boolean StartRecord(String filepath) {
        if (IsRecording()) {
            Log.d("SoundRecorder", "Another recording is happening");
            return false;
        }

        // if AudioRecord object is not initialled
        if (m_AudioRecord == null) {
            initAudioRecord();
        }

        if (m_RecordingCompleted == true) {
            m_RecordingCompleted = false;
            m_AudioRecord.release();
            m_AudioRecord = null;
            return false;
        }

        Log.d("SoundRecorder", "Start recording");
        m_IsRecording = true;

        try {
            m_RecordFile = new File(filepath, "test.pcm");
            OutputStream outputStream = new FileOutputStream(m_RecordFile);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);

            // open a new thread to do these
            m_AudioRecord.startRecording();
            short[] audioData = new short[m_BufferSize];
            while (IsRecording()) {
                int numberOfShort = m_AudioRecord.read(audioData, 0, m_BufferSize);
                for (int i = 0; i < numberOfShort; i++) {
                    dataOutputStream.writeShort(audioData[i]); // stream sound data to file
                }
            }
            m_AudioRecord.stop();
            dataOutputStream.close();
        } catch (IOException e) {
            Log.d("SoundRecorder", "Cannot write sound to file");
            e.printStackTrace();
        }
        m_RecordingCompleted = true;

        return true;
    }

    /**
     * Stop recording
     */
    public void StopRecording() {
        Log.d("SoundRecorder", "Stop recording");
        m_IsRecording = false;
        m_AudioRecord.stop();

        m_RecordingCompleted = true;
    }

    public void Close() {
        Log.d("SoundRecorder", "Closing down");
        m_IsRecording = false;
        m_RecordingCompleted = false;
        m_AudioRecord.stop();
        m_AudioRecord.release();
        m_AudioRecord = null;
    }

    // tidy up for next recording
    public void Reset(String filePath) {
        // delete record files
        String filename = filePath + File.separator + "test.pcm";
        File file = new File(filename);
        if(file.exists()) {
            file.delete();
        }

        // clean and rebuild AudioRecord object
        m_IsRecording = false;
        m_RecordingCompleted = false;
        m_AudioRecord.stop();
        m_AudioRecord.release();
        m_AudioRecord = null;
        initAudioRecord();
    }

    /**
     * return if AudioRecord is recoding
     */
    public boolean IsRecording() {
        return m_IsRecording;
    }

    /**
     * return if AudioRecord is playing the recorded sound
     */
    public boolean IsPlaying() {
        return m_IsPlaying;
    }

    public void SetIsPlaying(boolean isPlaying) {
        m_IsPlaying = isPlaying;
    }

    /**
     * this method returns true after "start recording"->"stop recording" and means
     * there is data ready to use
     */
    public boolean IsRecordCompleted() {
        return m_RecordingCompleted;
    }

    /**
     * play recorded sound
     */
    public void PlaySound(String filePath) {

        if (IsPlaying() || !IsRecordCompleted()) {
            return;
        }

        SetIsPlaying(true);

        String path = filePath + File.separator + "test.pcm";
        // read sound data from file
        // TODO: need to delete file somewhere
        try {
            DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(path)));

            // build AudioTrack object
            //int frequency = 11025,channelConfiguration = AudioFormat.CHANNEL_OUT_MONO;
            int frequency = 44100, channelConfiguration = AudioFormat.CHANNEL_OUT_MONO;
            int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

            int myBufferSize = AudioTrack.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
            short[] audiodata = new short[myBufferSize / 4];

            if (myBufferSize != AudioTrack.ERROR_BAD_VALUE) {
                track = new AudioTrack(AudioManager.STREAM_MUSIC, frequency, channelConfiguration, audioEncoding, myBufferSize, AudioTrack.MODE_STREAM);
            }

            if (track.getState() == AudioTrack.STATE_UNINITIALIZED) {
                Log.d("AudioRecord", "AudioTrack Uninit");
                IOException e = new IOException();
                throw e;
            }

            track.play();
            while (IsPlaying() && dis.available() > 0) {
                int i = 0;
                while (dis.available() > 0 && i < audiodata.length) {
                    audiodata[i] = dis.readShort();
                    i++;
                }
                track.write(audiodata, 0, audiodata.length); // write sound data to AudioTrack object to play
            }
            dis.close();

        } catch (IOException e) {
            Log.d("SoundRecorder", "Cannot write sound to file");
            e.printStackTrace();
        }
        SetIsPlaying(false);
    }

    // method to output the tune of
    public void SpectrumAnalysis(double[] in_data, int[] out_octave, int[] out_note, double[] out_biased, double[] out_Hz) {

        // FFT analysis
        double[] im = new double[FFT_N];
        m_FFT.fft(in_data, im); // Fourier Transform to calculate feature spectrum

        // denoise
        double[] modolus = Denoise(in_data, im);

        // find feature spectrum
        double featureSpectrumAmplitude = 0.00;
        for (int spectrumCount = 0; spectrumCount < in_data.length/2; spectrumCount ++){
            //double modulus = in_data[spectrumCount] * in_data[spectrumCount] + im[spectrumCount] * im[spectrumCount];

            // search for feature spectrum
            if (modolus[spectrumCount] > featureSpectrumAmplitude){
                out_Hz[0] = spectrumCount;
                featureSpectrumAmplitude = modolus[spectrumCount];
            }
        } // end for

        out_Hz[0] = out_Hz[0] * (44100.0/FFT_N);

        // search for featureSpectrum in standard frequency list to get octave and note
        SearchInFrequencyList(out_Hz[0], out_octave, out_note, out_biased);

        im = null;
    }

    private void SearchInFrequencyList(double in_featureSpectrum, int[] out_octave, int[] out_note, double[] out_biased){
        // compare feature spectrum with lowest note
        if ((in_featureSpectrum < m_StandardFrequency[0][0]) || (in_featureSpectrum > m_StandardFrequency[11][9])) {
            out_octave[0] = 0;
            out_note[0] = 0;
            out_biased[0] = 0.0;
            return;
        }

        boolean found = false;
        int octaveCount = 0, noteCount = 0;
        // compare feature spectrum with standard frequencies, starting with the 2nd lowest note
        for (; octaveCount < 10; octaveCount++) { // for each octave, C0 to C9, 10 octaves in total

            if (found == true) {break;}
            noteCount = 0;
            for (; noteCount < 12; noteCount++) { // for each note in one octave,  note C to note B, 12 notes in total

                if (found == true) {break;}
                if (in_featureSpectrum <= m_StandardFrequency[noteCount][octaveCount]) {
                    found = true;

                    // if frequency < Cn, then it should be B(n-1)
                    if (noteCount == 0){
                        if (octaveCount == 0) {out_octave[0] = 0; out_note[0] = 0; out_biased[0] = 0.0; return;} // if frequency is lower than the lowest note in table, return as 'cannot find match'

                        out_octave[0] = octaveCount - 1;
                        out_note[0] = 11;
                        out_biased[0] = CalculateBias(in_featureSpectrum, m_StandardFrequency[out_note[0]][out_octave[0]], m_StandardFrequency[0][octaveCount]);
                    }else{
                        out_octave[0] = octaveCount;
                        out_note[0] = noteCount - 1;
                        out_biased[0] = CalculateBias(in_featureSpectrum, m_StandardFrequency[out_note[0]][out_octave[0]], m_StandardFrequency[noteCount][octaveCount]);
                    }

                } // end if
            } // end for
        } // end for

        // if feature spectrum is higher than the highest note
        if ((octaveCount == 10) && (noteCount == 12)){
            out_octave[0] = octaveCount - 1;
            out_note[0] = noteCount - 1;
            out_biased[0] = 0.0;
        }

        // if bias > 0.5, consider the note as the higher one
        if(out_biased[0] > 0.5){
            if (out_note[0] == 11){ // if the note is B, change it to C of the next octave
                out_note[0] = 0;
                out_octave[0] += 1;
            }else{ // for all other notes, change it to the higher one
                out_note[0] += 1;
            }
            out_biased[0] = -1.0 * (1.0 - out_biased[0]);
        }

    }

    // calculate how much the feature spectrum is biased from the correct frequency, represented as a percentage
    private double CalculateBias(double frequency, double lower, double higher){
        if (lower == higher) {return 0.0;}

        return ((frequency - lower)/(higher - lower));
    }

    private double[] Denoise(double[] rl, double[] im){
        double[] ModulusList = new double[FFT_N];
        for (int spectrumCount = 0; spectrumCount < rl.length/2; spectrumCount ++){

            double modulus = rl[spectrumCount] * rl[spectrumCount] + im[spectrumCount] * im[spectrumCount];
            // de-noise
            if (modulus < m_NoiseThreshold) {
                ModulusList[spectrumCount] = 0.0;
            }else{
                ModulusList[spectrumCount] = modulus;
            }
        }

        return ModulusList;
    }
}
