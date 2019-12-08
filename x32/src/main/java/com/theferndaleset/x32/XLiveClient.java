package com.theferndaleset.x32;

import android.os.Handler;

import com.theferndaleset.osc.OscMessage;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Mike on 6/16/2018.
 */

public class XLiveClient extends Thread {

    private Handler _handler;
    private boolean _runFlag = true;
    private XLive _xlive;
    final int MAX_DATAGRAM_SIZE = 4000;

    private String _ipAddress = "192.168.1.60";
    private IXLiveManager _manager;
    private LinkedBlockingQueue<Runnable> _queue;
    private int _port = 10023;
    private DatagramChannel _channel;
    private ByteBuffer _buffer;
    private int _jogOffset;
    private int _jogDelayMilliseconds = 300;

    public XLiveClient(String ipAddress, int port, IXLiveManager manager){
        _manager = manager;
        _xlive = new XLive();
        _queue = new LinkedBlockingQueue<Runnable>();
    }

    public void setJogOffset(int jogOffset) {
        _jogOffset = jogOffset;
    }

    public void restartSong(){
        if (_queue == null)
            _queue = new LinkedBlockingQueue<Runnable>();

        _queue.add(new Runnable(){
            public void run(){
                _xlive.setElapsedTime(0);
            }
        });
    }

    public void init(){
        if (_queue == null)
            _queue = new LinkedBlockingQueue<Runnable>();
        _queue.add(new Runnable() {
           public void run() {
               List<OscMessage> messages = new ArrayList<OscMessage>(Arrays.asList(
//                new OscMessage("/-stat/urec/state"),
//                new OscMessage("/-stat/urec/etime"),
//                new OscMessage("/-urec/sessionlen"),
                new OscMessage("/xremote")
               ));

               for(OscMessage m : messages){
                   sendMessage(m);
               }
           }
        });
    }

    private void sendMessage(OscMessage m){
        byte[] mbytes = m.getBytes();
        ByteBuffer mbuf = ByteBuffer.wrap(mbytes);

        try
        {
            _channel.send(mbuf, new InetSocketAddress(_ipAddress, _port));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void pausePlayback() {
        if (_queue == null)
            _queue = new LinkedBlockingQueue<Runnable>();

        _queue.add(new Runnable() {
            public void run() {
                OscMessage m = new OscMessage("/-stat/urec/state");
                m.addArgument("1");
                sendMessage(m);
            }
        });
    }

    public void stopPlayback() {
        if (_queue == null)
            _queue = new LinkedBlockingQueue<Runnable>();

        _queue.add(new Runnable() {
            public void run() {
                OscMessage m = new OscMessage("/-stat/urec/state");
                m.addArgument("0");
                sendMessage(m);
            }
        });
    }
    public void startPlayback() {
        if (_queue == null)
            _queue = new LinkedBlockingQueue<Runnable>();

        _queue.add(new Runnable() {
           public void run() {
               OscMessage m = new OscMessage("/-stat/urec/state");
               m.addArgument("2");
               sendMessage(m);
           }
        });
    }

    @Override
    public void run() {
        _buffer = ByteBuffer.allocate(MAX_DATAGRAM_SIZE);
        _buffer.clear();
        try{
            _channel = DatagramChannel.open();
            _channel.socket().bind(new InetSocketAddress(_port));
            _channel.configureBlocking(false);

            Long asOfTime = System.currentTimeMillis();
            Long lastRefreshTime = System.currentTimeMillis();
            while(_runFlag)
            {
                if (_channel.receive(_buffer) != null){
                    OscMessage m = new OscMessage(_buffer);
                    if (_xlive.updateFromMessage(m))
                        _manager.update(XLiveUpdateMessage.REFRESH, _xlive);
                    _buffer.clear();
                }


                while (_queue != null && !_queue.isEmpty())
                {
                    Runnable r = _queue.remove();
                    r.run();
                }

                if (Math.abs(System.currentTimeMillis() - asOfTime) >= _jogDelayMilliseconds) {
                    jog();
                    asOfTime = System.currentTimeMillis();
                }

                if (Math.abs(System.currentTimeMillis() - lastRefreshTime) >= 9000){
                    OscMessage m = new OscMessage("/xremote");
                    sendMessage(m);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void jog() {
        if (_jogOffset == 0) {
            if (_xlive.getStatus() == 1)
                pausePlayback();
            return;
        }

        if (_xlive.getStatus() == 2)
                pausePlayback();
        _xlive.setElapsedTime(_xlive.getElapsedTime() + _jogOffset*500);
        OscMessage m = new OscMessage("/-action/setposition");
        m.addArgument(_xlive.getElapsedTime());
        sendMessage(m);
    }

    public void kill() {
        try {
            _channel.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        _runFlag = false;
    }
}
