package com9336.transimitter.util;
// reference  https://github.com/nisrulz/zentone

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.util.List;

public class PlayToneThread extends Thread{
	private boolean isPlaying = false;
	private int freqOfTone;
    private int freqOfTone2;
	private final int duration;
	private AudioTrack audioTrack = null;
	private final ToneStoppedListener toneStoppedListener;
	private float volume = 0f;
	private boolean dual;
	private List freqs;
	private List freqs2;
    private boolean msg;
	
	/**
	 * Instantiates a new play tone thread.
	 * @param freqOfTone
	 * @param duration
	 * @param volume
	 * @param toneStoppedListener
	 */
	public PlayToneThread(int freqOfTone, int duration, float volume, ToneStoppedListener toneStoppedListener) {
		this.freqOfTone = freqOfTone;
		this.duration = duration;
		this.volume = volume;
        this.dual = false;
		this.toneStoppedListener = toneStoppedListener;
	}

    public PlayToneThread(int freqOfTone, int freqOfTone2, int duration, float volume, ToneStoppedListener toneStoppedListener) {
		this.freqOfTone = freqOfTone;
		this.freqOfTone2 = freqOfTone2;
		this.duration = duration;
		this.volume = volume;
		this.dual = true;
		this.toneStoppedListener = toneStoppedListener;
	}

	public PlayToneThread(boolean msg, List freqs, List freqs2, int duration, float volume, ToneStoppedListener toneStoppedListener) {
		this.msg = msg;
        this.freqs = freqs;
		this.freqs2 = freqs2;
		this.duration = duration;
		this.volume = volume;
		this.dual = true;
		this.toneStoppedListener = toneStoppedListener;
	}
	
	@Override
	public void run() {
		super.run();
        if (msg == true){
			playToneMsg();
        } else {
            playTone();
        }
	}
	
	public void playTone(){
		if (!isPlaying) {
			isPlaying = true;
			int sampleRate = 44100; // 44.1 KHz
			double dnumSamples = (double) duration * sampleRate;
			dnumSamples = Math.ceil(dnumSamples);
			int numSamples = (int)dnumSamples;
			byte[] generatedSnd = new byte[2 * numSamples];
            double[] sample = new double[numSamples];
            if (dual == false) {
                for (int i = 0; i < numSamples; ++i) {      // Fill the sample array
                    sample[i] = Math.sin(freqOfTone * 2 * Math.PI * i / (sampleRate));
                }
            } else {
                for (int i = 0; i < numSamples; ++i) {      // Fill the sample array
                    double s1 = Math.sin(freqOfTone * 2 * Math.PI * i / (sampleRate));
                    double s2 = Math.sin(freqOfTone2 * 2 * Math.PI * i / (sampleRate));
                    sample[i] =  (s1 + s2) / 2;
                }
            }
			// convert to 16 bit pcm sound array
			// assumes the sample buffer is normalized.
			int idx = 0;
			int i;
			// Amplitude ramp as a percent of sample count
			int ramp = numSamples / 20;  
			
			// Ramp amplitude up (to avoid clicks)
			for (i = 0; i < ramp; ++i) {  
				// Ramp up to maximum
				final short val = (short) (sample[i] * 32767 * i / ramp);
				// in 16 bit wav PCM, first byte is the low order byte
				generatedSnd[idx++] = (byte) (val & 0x00ff);
				generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
			}
			
			// Max amplitude for most of the samples
			for (i = ramp; i < numSamples - ramp; ++i) {
				// // scale to maximum amplitude
				final short val = (short) (sample[i] * 32767);
				// in 16 bit wav PCM, first byte is the low order byte
				generatedSnd[idx++] = (byte) (val & 0x00ff);
				generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
			}
			
			// Ramp amplitude down
			for (i = numSamples - ramp; i < numSamples; ++i) { 
		        // Ramp down to zero
		        final short val = (short) (sample[i] * 32767 * (numSamples - i) / ramp);
		        // in 16 bit wav PCM, first byte is the low order byte
		        generatedSnd[idx++] = (byte) (val & 0x00ff);
		        generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
			}
			
			try {
				int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
				audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
				audioTrack.setNotificationMarkerPosition(numSamples);
				audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
					@Override
					public void onPeriodicNotification(AudioTrack track) {
						// pass
					}
					@Override
					public void onMarkerReached(AudioTrack track) {
						toneStoppedListener.onToneStopped();
					}
				});
				// Sanity Check for max volume, set after write method to handle issue in android
				// v 4.0.3
				float maxVolume = AudioTrack.getMaxVolume();
				if (volume > maxVolume) {
					volume = maxVolume;
				} else if (volume < 0) {
					volume = 0;
				}
				// > LOLLIPOP
				audioTrack.setVolume(volume);
				audioTrack.play();
				audioTrack.write(generatedSnd, 0, generatedSnd.length);
			} catch (Exception e) {
				e.printStackTrace();
			}
			stopTone();
		}
	}

    public void playToneMsg(){
        int sampleRate = 44100; // 44.1 KHz
        double dnumSamples = (double) duration * sampleRate * 1/3;
        dnumSamples = Math.ceil(dnumSamples);
        int numSamples = (int)dnumSamples;
        byte[] generatedSnd = new byte[2 * numSamples];
        for (int q = 0; q < freqs.size(); q++) {
            double[] sample = new double[numSamples];
            if (dual == false) {
                for (int i = 0; i < numSamples; ++i) {      // Fill the sample array
                    sample[i] = Math.sin(freqOfTone * 2 * Math.PI * i / (sampleRate));
                }
            } else {
                freqOfTone = (int)freqs.get(q);
                freqOfTone2 = (int)freqs2.get(q);
                for (int i = 0; i < numSamples; ++i) {      // Fill the sample array
                    double s1 = Math.sin(freqOfTone * 2 * Math.PI * i / (sampleRate));
                    double s2 = Math.sin(freqOfTone2 * 2 * Math.PI * i / (sampleRate));
                    sample[i] =  (s1 + s2) / 2;
                }
            }
            // convert to 16 bit pcm sound array
            // assumes the sample buffer is normalized.
            int idx = 0;
            int i;
            // Amplitude ramp as a percent of sample count
            int ramp = numSamples / 20;
            // Ramp amplitude up (to avoid clicks)
            for (i = 0; i < ramp; ++i) {
                // Ramp up to maximum
                final short val = (short) (sample[i] * 32767 * i / ramp);
                // in 16 bit wav PCM, first byte is the low order byte
                generatedSnd[idx++] = (byte) (val & 0x00ff);
                generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
            }
            // Max amplitude for most of the samples
            for (i = ramp; i < numSamples - ramp; ++i) {
                // // scale to maximum amplitude
                final short val = (short) (sample[i] * 32767);
                // in 16 bit wav PCM, first byte is the low order byte
                generatedSnd[idx++] = (byte) (val & 0x00ff);
                generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
            }
            // Ramp amplitude down
            for (i = numSamples - ramp; i < numSamples; ++i) {
                // Ramp down to zero
                final short val = (short) (sample[i] * 32767 * (numSamples - i) / ramp);
                // in 16 bit wav PCM, first byte is the low order byte
                generatedSnd[idx++] = (byte) (val & 0x00ff);
                generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
            }
            try {
                int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
                audioTrack.setNotificationMarkerPosition(numSamples);
                audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                    @Override
                    public void onPeriodicNotification(AudioTrack track) {
                        // pass
                    }
                    @Override
                    public void onMarkerReached(AudioTrack track) {
                        toneStoppedListener.onToneStopped();
                    }
                });
                // Sanity Check for max volume, set after write method to handle issue in android
                // v 4.0.3
                float maxVolume = AudioTrack.getMaxVolume();
                if (volume > maxVolume) {
                    volume = maxVolume;
                } else if (volume < 0) {
                    volume = 0;
                }
                // > LOLLIPOP
                audioTrack.setVolume(volume);
                audioTrack.play();
                audioTrack.write(generatedSnd, 0, generatedSnd.length);
			}
			catch (Exception e) {
                e.printStackTrace();
            }
        }
        stopTone();
    }

	public void stopTone() {
		if (audioTrack != null && audioTrack.getState() == AudioTrack.PLAYSTATE_STOPPED) {
			Log.i("INFO", "stopTone!");
			try {
				audioTrack.pause();
				isPlaying = false;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
