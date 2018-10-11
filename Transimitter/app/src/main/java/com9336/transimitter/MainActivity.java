package com9336.transimitter;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com9336.transimitter.util.ToneStoppedListener;
import com9336.transimitter.util.MyTone;


public class MainActivity extends AppCompatActivity {

    public Button taskButton;
    public Button button01;
    public TextView textView1;
    public EditText editText1;
    RadioGroup rg;
    RadioButton radioButtonDefault;
    static final String TASK_1 = "Task 1";
    static final String TASK_2 = "Task 2";
    static final String TASK_3 = "Task 3";
    static final String TASK_4 = "Task 4";
    static final String TASK_5 = "Task 5";

    int freq = 350;
    int duration = 600;
    float volume = 1f;
    boolean isPlaying = false;

    String type = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        taskButton = (Button) findViewById(R.id.task);
        editText1 = (EditText) findViewById(R.id.editText1);
        editText1.setText(String.valueOf(freq));
        textView1 = (TextView) findViewById(R.id.textView1);
        editText1.setText("");

        button01 = (Button) findViewById(R.id.button01);
        button01.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String indicator = taskButton.getText().toString();
                action(indicator);
            }
        });

        taskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("task", "task switch");
                String taskName = taskButton.getText().toString();
                if (TASK_1.equals(taskName)){
                    taskButton.setText(TASK_2);
                    textView1.setText("digit 1-9");
                    rg.setVisibility(View.VISIBLE);
                    radioButtonDefault.setChecked(true);
                    editText1.setText("");
                } else if (TASK_2.equals(taskName)) {
                    taskButton.setText(TASK_3);
                    textView1.setText("dual tone, digit 1-9");
                    rg.setVisibility(View.VISIBLE);
                    radioButtonDefault.setChecked(true);
                    editText1.setText("");
                } else if (TASK_3.equals(taskName)) {
                    taskButton.setText(TASK_4);
                    textView1.setText("message");
                    rg.setVisibility(View.INVISIBLE);
                    editText1.setText("");
                } else if (TASK_4.equals(taskName)) {
                    taskButton.setText(TASK_1);
                    textView1.setText("Frequency(HZ)");
                    rg.setVisibility(View.INVISIBLE);
                    editText1.setText("");
                } else {

                }

            }
        });

        rg = (RadioGroup) findViewById(R.id.rg2);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radioButton21){
                    type = "audible";
                } else {
                    type = "inaudible";
                }
            }
        });
        rg.setVisibility(View.INVISIBLE);
        radioButtonDefault = (RadioButton) findViewById(R.id.radioButton21);
        radioButtonDefault.setChecked(true);
    }

    private String action(String indicator){
        if (indicator.equals(TASK_1)){
            handleTonePlay(this);
        } else if (indicator.equals(TASK_2)) {
            digitTonePlay(this);
        } else if (indicator.equals(TASK_3)) {
            dualDigitTonePlay(this);
        } else if (indicator.equals(TASK_4)) {
            messageTonePlay(this);
        }
        return null;
    }

    private void handleTonePlay(MainActivity activity) {
        String freqString = editText1.getText().toString();
        if (!"".equals(freqString)) {
            if (!isPlaying) {
                button01.setText("Stop");
                freq = Integer.parseInt(freqString);
                // Play Tone
                MyTone.getInstance().generate(freq, this.duration, volume, activity, new ToneStoppedListener() {
                    @Override public void onToneStopped() {
                        isPlaying = false;
                        button01.setText("Play");
                    }
                });
                isPlaying = true;
            } else {
                // Stop Tone
                MyTone.getInstance().stop();
                isPlaying = false;
                button01.setText("Play");
            }
        }
    }

    private void digitTonePlay(MainActivity activity) {
        HashMap<String, String> map = new HashMap<String, String>();
        if ("audible".equals(type)){
            map.put("1", "500");
            map.put("2", "600");
            map.put("3", "700");
            map.put("4", "800");
            map.put("5", "900");
            map.put("6", "1000");
            map.put("7", "1100");
            map.put("8", "1200");
            map.put("9", "1300");
        } else {
            map.put("1", "16000");
            map.put("2", "16300");
            map.put("3", "16600");
            map.put("4", "16900");
            map.put("5", "17200");
            map.put("6", "17500");
            map.put("7", "17800");
            map.put("8", "18100");
            map.put("9", "18400");
        }
        String freqString = map.get(editText1.getText().toString());
        if (!"".equals(freqString)) {
            if (!isPlaying) {
                button01.setText("Stop");
                int freq = Integer.parseInt(freqString);
                int duration = 5;
                // Play Tone
                MyTone.getInstance().generate(freq, duration, volume, activity, new ToneStoppedListener() {
                    @Override public void onToneStopped() {
                        isPlaying = false;
                        button01.setText("Play");
                    }
                });
                isPlaying = true;
            } else {
                // Stop Tone
                MyTone.getInstance().stop();
                isPlaying = false;
                button01.setText("Play");
            }
        }
    }

    private void dualDigitTonePlay(MainActivity activity) {
        HashMap<String, String> map = new HashMap<String, String>();
        if ("audible".equals(type)) {
            map.put("1", "697:1209");
            map.put("2", "697:1336");
            map.put("3", "697:1477");
            map.put("4", "770:1209");
            map.put("5", "770:1336");
            map.put("6", "770:1477");
            map.put("7", "852:1209");
            map.put("8", "852:1336");
            map.put("9", "852:1477");
        } else {
            map.put("1", "16100:18110");
            map.put("2", "16100:18700");
            map.put("3", "16100:19325");
            map.put("4", "16700:18110");
            map.put("5", "16700:18700");
            map.put("6", "16700:19325");
            map.put("7", "17600:18110");
            map.put("8", "17600:18700");
            map.put("9", "17600:19325");
        }
        String[] freqPair = map.get(editText1.getText().toString()).split(":");
        if (freqPair.length == 2) {
            if (!isPlaying) {
                button01.setText("Stop");
                int freqLow = Integer.parseInt(freqPair[0]);
                int freqHigh = Integer.parseInt(freqPair[1]);
                int duration = 5;
                // Play Tone
                MyTone.getInstance().generateDual(freqLow, freqHigh, duration, volume, activity, new ToneStoppedListener() {
                    @Override
                    public void onToneStopped() {
                        isPlaying = false;
                        button01.setText("Play");
                    }
                });
                isPlaying = true;
            } else {
                // Stop Tone
                MyTone.getInstance().stop();
                isPlaying = false;
                button01.setText("Play");
            }
        }
    }

    private void messageTonePlay(MainActivity activity) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("1", "697:1209");
        map.put("2", "697:1336");
        map.put("3", "697:1477");
        map.put("^", "697:1633");

        map.put("4", "770:1209");
        map.put("5", "770:1336");
        map.put("6", "770:1477");
        map.put("#", "770:1633");

        map.put("7", "852:1209");
        map.put("8", "852:1336");
        map.put("9", "852:1477");
        map.put("!", "852:1633");
        //map.put("!", "5000:6000");

        map.put("0", "941:1336");
        map.put("$", "941:1633");

        String msg = editText1.getText().toString();
        Log.i("Msg", msg);
        List list = messageFormat(charToAscii(msg));
        List freqs = new ArrayList();
        List freqs2 = new ArrayList();
        if (!list.isEmpty()) {
            if (!isPlaying) {
                for (int i = 0; i < list.size(); i++){
                    button01.setText("Stop");
                    String[] freqPair = map.get(list.get(i)).split(":");
                    int freqLow = Integer.parseInt(freqPair[0]);
                    freqs.add(freqLow);
                    int freqHigh = Integer.parseInt(freqPair[1]);
                    freqs2.add(freqHigh);
                    Log.i("SingleFormat = ", list.get(i) + "(" + freqLow + ":" + freqHigh + ")");
                }
                // Play Tone
                int duration = 1;
                MyTone.getInstance().generateMsg(freqs, freqs2, duration, volume, activity, new ToneStoppedListener() {
                    @Override
                    public void onToneStopped() {
                        isPlaying = false;
                        button01.setText("Play");
                    }
                });
                isPlaying = true;
            } else {
                // Stop Tone
                MyTone.getInstance().stop();
                isPlaying = false;
                button01.setText("Play");
            }
        }
    }

    private List charToAscii(String str){
        List list = new ArrayList();
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            int intAscii = (int)c;
            list.add(intAscii);
        }
        return list;
    }

    private List messageFormat(List list){
        List formatList = new ArrayList();
        StringBuffer stringBuffer = new StringBuffer();
        // start
        formatList.add("^");
        stringBuffer.append("^");
        for (int i = 0; i < list.size(); i++){
            String asciiStr = String.valueOf(list.get(i));
            char[] chNum = asciiStr.toCharArray();
            for (int j = 0; j < chNum.length; j++){
                formatList.add(String.valueOf(chNum[j]));
                stringBuffer.append(String.valueOf(chNum[j]));
                formatList.add("#");
                stringBuffer.append("#");
            }
            // '#' mark a word or digit, generate a special freq
            formatList.add("!");
            stringBuffer.append("!");
        }
        formatList.add("$");
        stringBuffer.append("$");
        Log.i("MsgFormat = ", stringBuffer.toString());
        return formatList;
    }
}
