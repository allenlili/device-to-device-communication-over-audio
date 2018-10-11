package com9336.transimitter.util;
// reference  https://github.com/nisrulz/zentone

import com9336.transimitter.MainActivity;

import android.os.Handler;
import android.util.Log;

import java.util.List;

public class MyTone {
	private PlayToneThread playToneThread;
	private boolean isThreadRunning = false;
	private final Handler stopThread;
	
	private static final MyTone INSTANCE = new MyTone();
	
	private MyTone(){
		stopThread = new Handler();
	}
	
	public static MyTone getInstance(){
		return INSTANCE;
	}
	
	public void generate(int freq, int duration, float volume, final MainActivity activity, ToneStoppedListener toneStoppedListener){
		if (!isThreadRunning) {
			stop();
			playToneThread = new PlayToneThread(freq, duration, volume, toneStoppedListener);
			playToneThread.start();
			isThreadRunning = true;
			stopThread.postDelayed(new Runnable() {
				@Override
				public void run() {
					stop();
					Log.i("INFO", "play done");
					activity.button01.setText("Play");
				}
			}, duration * 1000);
		}
	}

	public void generateDual(int freq, int freq2, int duration, float volume, final MainActivity activity, ToneStoppedListener toneStoppedListener){
		if (!isThreadRunning) {
			stop();
			playToneThread = new PlayToneThread(freq, freq2, duration, volume, toneStoppedListener);
			playToneThread.start();
			isThreadRunning = true;
			stopThread.postDelayed(new Runnable() {
				@Override
				public void run() {
					stop();
					Log.i("INFO", "play done");
					activity.button01.setText("Play");
				}
			}, duration * 1000);
		}
	}

    public void generateMsg(List freqs, List freq2, int duration, float volume, final MainActivity activity, ToneStoppedListener toneStoppedListener){
        if (!isThreadRunning) {
            stop();
            playToneThread = new PlayToneThread(true, freqs, freq2, duration, volume, toneStoppedListener);
            playToneThread.start();
            isThreadRunning = true;
            stopThread.postDelayed(new Runnable() {
                @Override
                public void run() {
					stop();
					Log.i("INFO", "play done");
					activity.button01.setText("Play");
                }
            }, duration * 1000);
        }
    }
	
	public void stop(){
		if (playToneThread != null) {
			playToneThread.stopTone();
			playToneThread.interrupt();
			playToneThread = null;
			isThreadRunning = false;
		}
	}
}
