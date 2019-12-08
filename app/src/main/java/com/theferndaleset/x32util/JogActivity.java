package com.theferndaleset.x32util;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.theferndaleset.x32.IXLiveManager;
import com.theferndaleset.x32.XLiveClient;
import com.theferndaleset.x32.XLive;

public class JogActivity extends AppCompatActivity {

    private SeekBar _jogBar;
    private ProgressBar _songPositionProgress;
    private Chronometer _songPosition;
    private EditText _jogText;
    private XLiveManager _manager;
    private XLiveClient _client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jog);

        _jogText = (EditText)findViewById(R.id.jogText);
        _jogBar = (SeekBar)findViewById(R.id.jogStrip);
        _songPositionProgress = (ProgressBar)findViewById(R.id.songPositionProgress);
        _songPositionProgress.setProgress(0);
        _manager = new XLiveManager();
        _client = new XLiveClient("1234", 10023, _manager);
        _client.start();
        _client.init();

        _jogBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int value = 0;
            int middlePoint = 15;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                value = progress-middlePoint;
                _client.setJogOffset(progress-middlePoint);
//                _jogText.setText(Integer.toString(progress-middlePoint), TextView.BufferType.EDITABLE);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBar.setProgress(middlePoint);
            }
        });
    }

    @Override
    protected void onStop() {
        _client.kill();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        _client.kill();
        super.onDestroy();
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private class XLiveManager implements IXLiveManager {
        private XLive _xlive;
        Handler mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                XLive _xlive = (XLive) inputMessage.obj;
                int millis = _xlive.getElapsedTime();

                _jogText.setText(String.format("%02d : %02d", (millis/1000)/60, (millis/1000)%60));
//               _jogText.setText(Integer.toString(_xlive.getElapsedTime()));
               _songPositionProgress.setMax(_xlive.getTotalTime());
               if (_xlive.getTotalTime() > _xlive.getElapsedTime())
                   _songPositionProgress.setProgress(_xlive.getElapsedTime());
            }
        };

        public void update(int messageType, XLive xlive)
        {
            Message m = mHandler.obtainMessage(messageType, xlive);
            m.sendToTarget();
        }
    }
}
