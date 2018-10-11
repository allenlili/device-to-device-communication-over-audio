package com9336.receiver;
// Written by Li Li, z3447294

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com9336.receiver.util.Goertzel;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import static android.R.id.list;
import static android.R.id.summary;

public class MainActivity extends AppCompatActivity {
    // UI
    static final String TAG = "ASYNC_TASK";
    Button taskButton = null;
    EditText editText1 = null;
    TextView textView1 = null;
    Button button01 = null;
    Button button02 = null;
    RadioGroup rg;
    RadioGroup rg2;
    RadioButton radioButtonDefault;
    static final String initialValue = "350";
    static final String TASK_1 = "Task 1";
    static final String TASK_2 = "Task 2";
    static final String TASK_3 = "Task 3";
    static final String TASK_4 = "Task 4";
    static final String TASK_5 = "Task 5";
    String result = null;

    // Goertzel
    float sampleRate = 44100.0F;
    float targetFreq;
    Goertzel goertzel = null;

    // Task
    MyTask myTask = null;
    String type = null;

    // Audio
    AudioRecord recorder = null;
    int bufferSize = 0;
    Thread recordingThread = null;
    boolean isRecording = false;
    int RECORDER_SAMPLE_RATE = 8000; // at least 2 times
    @SuppressWarnings("deprecation")
    static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    // frequency
    float lower = 0;
    float higher = 4000;

    // handler
    MyHandler myHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initial task1
        taskButton = (Button) findViewById(R.id.task);
        button01 = (Button) findViewById(R.id.button01);
        button02 = (Button) findViewById(R.id.button02);
        textView1 = (TextView) findViewById(R.id.textView1);
        rg = (RadioGroup) findViewById(R.id.rg);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                checkRadioBox(checkedId);
            }
        });
        rg.setVisibility(View.VISIBLE);

        rg2 = (RadioGroup) findViewById(R.id.rg2);
        rg2.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radioButton21){
                    type = "audible";
                } else {
                    type = "inaudible";
                }
            }
        });
        rg2.setVisibility(View.INVISIBLE);
        radioButtonDefault = (RadioButton) findViewById(R.id.radioButton21);
        radioButtonDefault.setChecked(true);

        // audio
        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLE_RATE,
                RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING);

        // start
        button01.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("start", "Start Recording");
                textView1.setText("");
                deleteTempFile();
                button01.setEnabled(false);
                button02.setEnabled(true);
                button01.requestFocus();
                startRecording();
            }
        });

        // cancel
        button02.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("stop", "Stop recording");
                button01.setEnabled(true);
                button02.setEnabled(false);
                button01.requestFocus();
                stopRecording();
            }
        });

        taskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("task", "task switch");
                String taskName = taskButton.getText().toString();
                if (TASK_1.equals(taskName)){
                    taskButton.setText(TASK_2);
                    rg.setVisibility(View.INVISIBLE);
                    rg2.setVisibility(View.VISIBLE);
                    radioButtonDefault.setChecked(true);
                    textView1.setText("digit 1-9 result");
                } else if (TASK_2.equals(taskName)) {
                    taskButton.setText(TASK_3);
                    rg.setVisibility(View.INVISIBLE);
                    rg2.setVisibility(View.VISIBLE);
                    radioButtonDefault.setChecked(true);
                    textView1.setText("dual digit 1-9 result");
                } else if (TASK_3.equals(taskName)) {
                    taskButton.setText(TASK_4);
                    rg.setVisibility(View.INVISIBLE);
                    rg2.setVisibility(View.INVISIBLE);
                    textView1.setText("Hi");
                } else if (TASK_4.equals(taskName)) {
                    taskButton.setText(TASK_1);
                    rg.setVisibility(View.VISIBLE);
                    rg2.setVisibility(View.INVISIBLE);
                    textView1.setText("result");
                } else {

                }
            }
        });


        myHandler = new MyHandler();
    }

    private void divideTask(String indicator){
        if (indicator.equals(TASK_1)){
            myTask = new MyTask();
            myTask.execute(TASK_1);
        } else if (indicator.equals(TASK_2)) {
            myTask = new MyTask();
            myTask.execute(TASK_2);
        } else if (indicator.equals(TASK_3)) {
            myTask = new MyTask();
            myTask.execute(TASK_3);
        } else if (indicator.equals(TASK_4)){
            myTask = new MyTask();
            myTask.execute(TASK_4);
        }
    }

    private String action(String indicator){
        if (indicator.equals(TASK_1)){
            return task1();
        } else if (indicator.equals(TASK_2)) {
            return task2();
        } else if (indicator.equals(TASK_3)) {
            return task3();
        } else if (indicator.equals(TASK_4)) {
            return task4();
        }
        return null;
    }

    private float detect(File file) {
        float maxFreq = 0.0f;
        double maxPower = 0.0d;
        double power = 0.0;
        FileInputStream is;
        Goertzel goertzel;
        byte[] data;
        try {
            for (float freq = lower; freq <= higher; freq++) {
                is = new FileInputStream(file);
                data = new byte[bufferSize];
                goertzel = new Goertzel(RECORDER_SAMPLE_RATE, freq, bufferSize);
                goertzel.initGoertzel();
                double max = 0.0d;
                int counter = 0;
                while (is.read(data) != -1) {
                    if (counter <= 1) {
                        counter ++;
                        continue;
                    }
                    if (counter == 6){
                        break;
                    }
                    counter ++;
                    ShortBuffer sbuf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
                    short[] audioShorts = new short[sbuf.capacity()];
                    sbuf.get(audioShorts);
                    float[] audioFloats = new float[audioShorts.length];
                    for (int j = 0; j < audioShorts.length; j++) {
                        audioFloats[j] = ((float) audioShorts[j]) / 0x8000;
                    }
                    for (int index = 0; index < audioFloats.length; index++) {
                        goertzel.processSample(audioFloats[index]);
                    }
                    power = goertzel.getMagnitudeSquared();
                    if (power >= max) {
                        max = power;
                    }
                    goertzel.resetGoertzel();
//                    Log.d("decode", "Relative freq  = " + freq);
//                    Log.d("decode", "Relative power  = " + power);
                }
                if (max > maxPower) {
                    maxPower = max;
                    maxFreq = freq;
                }
                is.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return maxFreq;
    }

    private float detectByGivenFreqList(File file, List<String> list) {
        float maxFreq = 0.0f;
        double maxPower = 0.0d;
        double power = 0.0;
        FileInputStream is;
        Goertzel goertzel;
        byte[] data;
        try {
            for (int i = 0; i < list.size(); i++) {
                float freq = Float.valueOf(list.get(i));
                is = new FileInputStream(file);
                data = new byte[bufferSize];
                goertzel = new Goertzel(RECORDER_SAMPLE_RATE, freq, bufferSize);
                goertzel.initGoertzel();
                double max = 0.0d;
                while (is.read(data) != -1) {
                    ShortBuffer sbuf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
                    short[] audioShorts = new short[sbuf.capacity()];
                    sbuf.get(audioShorts);
                    float[] audioFloats = new float[audioShorts.length];
                    for (int j = 0; j < audioShorts.length; j++) {
                        audioFloats[j] = ((float) audioShorts[j]) / 0x8000;
                    }
                    for (int index = 0; index < audioFloats.length; index++) {
                        goertzel.processSample(audioFloats[index]);
                    }
                    power = goertzel.getMagnitudeSquared();
                    if (power > max) {
                        max = power;
                    }
                    goertzel.resetGoertzel();
                }
                Log.d("decode", "Relative freq  = " + freq);
                Log.d("decode", "Relative power  = " + power);
                if (max > maxPower) {
                    maxPower = max;
                    maxFreq = freq;
                }
                is.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return maxFreq;
    }

    private float detectByBufferSize(byte[] data, List<String> list){
        float maxFreq = 0.0f;
        double maxPower = 0.0d;
        double power;
        float freq;
        Goertzel goertzel;
        try {
            for (int i = 0; i < list.size(); i++) {
                freq = Float.valueOf(list.get(i));
                goertzel = new Goertzel(RECORDER_SAMPLE_RATE, freq, bufferSize);
                goertzel.initGoertzel();
                ShortBuffer sbuf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
                short[] audioShorts = new short[sbuf.capacity()];
                sbuf.get(audioShorts);
                float[] audioFloats = new float[audioShorts.length];
                for (int j = 0; j < audioShorts.length; j++) {
                    audioFloats[j] = ((float) audioShorts[j]) / 0x8000;
                }
                for (int index = 0; index < audioFloats.length; index++) {
                    goertzel.processSample(audioFloats[index]);
//                    if (goertzel.getMagnitudeSquared() > 40) {
//                        Log.d("decode", "Relative power  = " + goertzel.getMagnitudeSquared());
//                    }
                }
                power = goertzel.getMagnitudeSquared();
                goertzel.resetGoertzel();
                if (power <= 3){
                    continue;
                }
                if (power > maxPower) {
                    maxPower = power;
                    maxFreq = freq;
                }
            }
//            Log.d("decode", "Relative freq  = " + maxFreq);
//            Log.d("decode", "Relative power  = " + maxPower);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return maxFreq;
    }

    private float detectByThreshold(byte[] data) {
        float maxFreq = 0.0f;
        double maxPower = 0.0d;
        double power = 0.0;
        Goertzel goertzel = null;
        try {
            for (float freq = lower; freq <= higher; freq++) {
                goertzel = new Goertzel(RECORDER_SAMPLE_RATE, freq, bufferSize);
                goertzel.initGoertzel();
                double max = 0.0d;
                ShortBuffer sbuf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
                short[] audioShorts = new short[sbuf.capacity()];
                sbuf.get(audioShorts);
                float[] audioFloats = new float[audioShorts.length];
                for (int j = 0; j < audioShorts.length; j++) {
                    audioFloats[j] = ((float) audioShorts[j]) / 0x8000;
                }
                for (int index = 0; index < audioFloats.length; index++) {
                    goertzel.processSample(audioFloats[index]);
                }
//                power = Math.log(goertzel.getMagnitudeSquared());
                power = goertzel.getMagnitudeSquared();
                if (power > max) {
                    max = power;
                }
                goertzel.resetGoertzel();
                //Log.d("decode", "Relative freq  = " + freq);
                //Log.d("decode", "Relative power  = " + power);
                if (max > maxPower) {
                    maxPower = max;
                    maxFreq = freq;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return maxFreq;
    }

    private String task1(){
        float maxFreq = detect(new File(getTempFilename()));
        result = String.valueOf(String.format("%.2f", maxFreq));
        return result;
    }

    private String task2(){
        Map<String, String> map = task2Map();
        List list = new ArrayList<String>(map.keySet());
        float maxFreq;
        String num;
        if ("audible".equals(type)) {
            maxFreq = detectByGivenFreqList(new File(getTempFilename()), list);
            num = map.get(String.valueOf(maxFreq));
        } else {
            maxFreq = detect(new File(getTempFilename()));
            int confidence = 149;
            if (maxFreq > 16000 - confidence && maxFreq < 16000 + confidence) {
                num =  map.get("16000.0");
            } else if (maxFreq > 16300 - confidence && maxFreq < 16300 + confidence) {
                num = map.get("16300.0");
            } else if (maxFreq > 16600 - confidence && maxFreq < 16600 + confidence) {
                num = map.get("16600.0");
            } else if (maxFreq > 16900 - confidence && maxFreq < 16900 + confidence) {
                num = map.get("16900.0");
            } else if (maxFreq > 17200 - confidence && maxFreq < 17200 + confidence) {
                num = map.get("17200.0");
            } else if (maxFreq > 17500 - confidence && maxFreq < 17500 + confidence) {
                num = map.get("17500.0");
            } else if (maxFreq > 17800 - confidence && maxFreq < 17800 + confidence) {
                num = map.get("17800.0");
            } else if (maxFreq > 18100 - confidence && maxFreq < 18100 + confidence) {
                num = map.get("18100.0");
            } else if (maxFreq > 18400 - confidence && maxFreq < 18400 + confidence) {
                num = map.get("18400.0");
            } else {
                num = "try again";
            }
        }
        Log.d("decode", "freq  = " + maxFreq);
        Log.d("decode", "num  = " + num);
        return num;
    }

    private Map task2Map(){
        Map<String, String> map = new HashMap<String, String>();
        if ("audible".equals(type)){
            Log.d("decode", "audible");
            map.put("500.0", "1");
            map.put("600.0", "2");
            map.put("700.0", "3");
            map.put("800.0", "4");
            map.put("900.0", "5");
            map.put("1000.0", "6");
            map.put("1100.0", "7");
            map.put("1200.0","8");
            map.put("1300.0","9");
            RECORDER_SAMPLE_RATE = 8000;
        } else {
            Log.d("decode", "inaudible");
            map.put("16000.0", "1");
            map.put("16300.0", "2");
            map.put("16600.0", "3");
            map.put("16900.0", "4");
            map.put("17200.0", "5");
            map.put("17500.0", "6");
            map.put("17800.0", "7");
            map.put("18100.0", "8");
            map.put("18400.0", "9");
            lower = 16000;
            higher = 18400;
            RECORDER_SAMPLE_RATE = 41000;
        }
        return map;
    }

    private String task3(){
        Map<String, String> map = task3Map();
        float maxFreqHigh;
        float maxFreqLow;
        String freqPair;
        String num;
        if ("audible".equals(type)) {
            String[] lows = {"697", "770", "852"};
            String[] highs = {"1209", "1336", "1477"};
            List listLows = Arrays.asList(lows);
            List listHighs = Arrays.asList(highs);
            maxFreqLow = detectByGivenFreqList(new File(getTempFilename()), listLows);
            maxFreqHigh = detectByGivenFreqList(new File(getTempFilename()), listHighs);
            freqPair = String.valueOf(maxFreqLow) + ":" + String.valueOf(maxFreqHigh);
            num = map.get(freqPair);
        } else {
            String[] lows = {"16100", "16700", "17600"};
            String[] highs = {"18110", "18700", "19325"};
            List listLows = Arrays.asList(lows);
            List listHighs = Arrays.asList(highs);
            maxFreqLow = detectByGivenFreqList(new File(getTempFilename()), listLows);
            maxFreqHigh = detectByGivenFreqList(new File(getTempFilename()), listHighs);
            freqPair = String.valueOf(maxFreqLow) + ":" + String.valueOf(maxFreqHigh);
            num = map.get(freqPair);
        }
        Log.d("decode", "freq  = " + freqPair);
        Log.d("decode", "num  = " + num);
        return num;
    }

    private Map task3Map(){
        Map<String, String> map = new HashMap<String, String>();
        if ("audible".equals(type)){
            Log.d("decode", "audible");
            map.put("697.0:1209.0", "1");
            map.put("697.0:1336.0", "2");
            map.put("697.0:1477.0", "3");
            map.put("770.0:1209.0", "4");
            map.put("770.0:1336.0", "5");
            map.put("770.0:1477.0", "6");
            map.put("852.0:1209.0", "7");
            map.put("852.0:1336.0","8");
            map.put("852.0:1477.0","9");
            RECORDER_SAMPLE_RATE = 8000;
        } else {
            // 16000 : 18000
            Log.d("decode", "inaudible");
            map.put("16100.0:18110.0", "1");
            map.put("16100.0:18700.0", "2");
            map.put("16100.0:19325.0", "3");
            map.put("16700.0:18110.0", "4");
            map.put("16700.0:18700.0", "5");
            map.put("16700.0:19325.0", "6");
            map.put("17600.0:18110.0", "7");
            map.put("17600.0:18700.0", "8");
            map.put("17600.0:19325.0", "9");
            RECORDER_SAMPLE_RATE = 41000;
            lower = 16000;
            higher = 19425;
        }
        return map;
    }

    private String task4(){
        Map<String, String> map = task4Map();
        float maxFreqHigh;
        float maxFreqLow;
        String freqPair;
        String num;
        String[] lows = {"697", "770", "852", "941"};
        String[] highs = {"1209", "1336", "1477", "1633"};
        List listLows = Arrays.asList(lows);
        List listHighs = Arrays.asList(highs);
        FileInputStream fileInputStream = null;
        byte[] data = new byte[bufferSize];
        int[] counter = new int[10];;
        StringBuffer string = new StringBuffer();
        StringBuffer chDigit = new StringBuffer();
        boolean startFlag = false;
        boolean digitFlag = true;
        boolean chFlag = true;
        try {
            fileInputStream = new FileInputStream(new File(getTempFilename()));
            while (fileInputStream.read(data) != -1) {
                maxFreqLow = detectByBufferSize(data, listLows);
                maxFreqHigh = detectByBufferSize(data, listHighs);
                freqPair = String.valueOf(maxFreqLow) + ":" + String.valueOf(maxFreqHigh);
                num = map.get(freqPair);
                //Log.d("decode", "freq  = " + freqPair);
                //Log.d("decode", "num  = " + num);
                if (num != null){
                    // ^9#7#!9#8#!9#9#!$
                    if (num == "^" && startFlag != true) {
                        // start
                        Log.d("decode", "num  = " + num);
                        startFlag = true;
                        chDigit = new StringBuffer();
                        string = new StringBuffer();
                        counter = new int[10];
                    } else if (num == "$" && chFlag == true) {
                        // end
                        Log.d("decode", "num  = " + num);
                        Log.d("decode", "result  = " + string.toString());
                        return string.toString();
                    } else if (num == "#" && digitFlag != true) {
                        // cal get a bit digit
                        Log.d("decode", "num  = " + num);
                        chDigit.append(getDigitHighestFreq(counter));
                        counter = new int[10];
                        digitFlag = true;
                    } else if (num == "!" && chFlag != true && digitFlag == true) {
                        // convert a letter or a digit and store
                        Log.d("decode", "num  = " + num);
                        Log.d("decode", "convertToChar  = " + chDigit.toString());
                        if (chDigit.toString() != ""){
                            string.append(convertToChar(chDigit.toString()));
                            chDigit = new StringBuffer();
                            chFlag = true;
                        }
                    } else {
                        if (num != null && num != "^" && num != "$" && num != "!" && num != "#"
                                && num != "") {
                            Log.d("decode", "num  = " + num);
                            counter[Integer.parseInt(num)] ++;
                            digitFlag = false;
                            chFlag = false;
                        }
                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return string.toString();
    }

    private Map task4Map() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("697.0:1209.0", "1");
        map.put("697.0:1336.0", "2");
        map.put("697.0:1477.0", "3");
        map.put("697.0:1633.0", "^");

        map.put("770.0:1209.0", "4");
        map.put("770.0:1336.0", "5");
        map.put("770.0:1477.0", "6");
        map.put("770.0:1633.0", "#");

        map.put("852.0:1209.0", "7");
        map.put("852.0:1336.0", "8");
        map.put("852.0:1477.0", "9");
        map.put("852.0:1633.0", "!");
        //map.put("5000.0:6000.0", "!");

        map.put("941.0:1336.0", "0");
        map.put("941.0:1633.0", "$");

        return map;
    }

    private void startRecording() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                                   RECORDER_SAMPLE_RATE,
                                   RECORDER_CHANNELS,
                                   RECORDER_AUDIO_ENCODING, bufferSize);
        recorder.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
//                String taskString = taskButton.getText().toString();
//                boolean stop = false;
//                if (!TASK_1.equals(taskString)) {
//                    writeAudioDataToTempFile();
//                } else {
//                    byte[] data = new byte[bufferSize];
//                    while (isRecording && !stop) {
//                        int read = recorder.read(data, 0, bufferSize);
//                        if (read != AudioRecord.ERROR_INVALID_OPERATION) {
//                            Message msg = new Message();
//                            Bundle bundle = new Bundle();
//                            bundle.putByteArray("data", data);
//                            msg.setData(bundle);
//                            myHandler.sendMessage(msg);
//                            stop = true;
//                        }
//                    }
//                }
                writeAudioDataToTempFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    private void stopRecording() {
        if (recorder != null) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
//        String taskString = taskButton.getText().toString();
//        if (!TASK_1.equals(taskString)) {
//            String indicator = taskButton.getText().toString();
//            divideTask(indicator);
//        }
        String indicator = taskButton.getText().toString();
        divideTask(indicator);
    }

    private void checkRadioBox(int checkedId){
        if (checkedId == R.id.radioButton) {
            lower = 0;
            higher = 4000;
            RECORDER_SAMPLE_RATE = 8000;
        } else if (checkedId == R.id.radioButton2){
            lower = 4001;
            higher = 8000;
            RECORDER_SAMPLE_RATE = 18000;
        } else if (checkedId == R.id.radioButton3){
            lower = 8001;
            higher = 12000;
            RECORDER_SAMPLE_RATE = 26000;
        } else if (checkedId == R.id.radioButton4){
            lower = 12001;
            higher = 16000;
            RECORDER_SAMPLE_RATE = 33000;
        } else {
            lower = 16001;
            higher = 20000;
            RECORDER_SAMPLE_RATE = 41000;
        }
    }

    private String getTempFilename() {
        File file = new File(getFilesDir(), "tempaudio");
        if (!file.exists()) {
            file.mkdirs();
        }
        File tempFile = new File(getFilesDir(), "signal.raw");
        if (tempFile.exists())
            tempFile.delete();
        return (file.getAbsolutePath() + "/" + "signal.raw");
    }

    private void writeAudioDataToTempFile() {
        byte data[] = new byte[bufferSize];
        String filename = getTempFilename();
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        int read = 0;
        if (os != null) {
            while (isRecording) {
                read = recorder.read(data, 0, bufferSize);
                if (read != AudioRecord.ERROR_INVALID_OPERATION) {
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());
        file.delete();
    }

    private String convertToChar(String asciiStr){
        String s = String.valueOf(Integer.toBinaryString(Integer.parseInt(asciiStr)));
        int x = 0;
        int pow = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            x += Math.pow(2, pow) * (s.charAt(i) == '1' ? 1 : 0);
            pow ++;
        }
        return String.valueOf((char)x);
    }

    private String getDigitHighestFreq(int[] digits){
        int max = 0;
        int index = 0;
        for (int i = 0; i< digits.length; i++){
            int number = digits[i];
            if (number > max){
                max = number;
                index = i;
            }
        }
        return String.valueOf(index);
    }

    private class MyTask extends AsyncTask<String, String, String>{
        @Override
        protected String doInBackground(String... params) {
            Log.i(TAG, "doInBackground(String... params) called");
            String indicator = params[0];
            String result = action(indicator);
            return result;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i(TAG, "onPostExecute(Result result) called");
            textView1.setText(result);
        }

    }

    private class MyHandler extends Handler{
        public MyHandler(){

        }
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            byte[] data = (byte[]) bundle.get("data");
            lower = 0;
            higher = 2000;
            float freq = detectByThreshold(data);
            textView1.setText(String.valueOf(String.valueOf(freq)));
        }
    }
}