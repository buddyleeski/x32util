package com.theferndaleset.x32;

import com.theferndaleset.osc.OscMessage;

/**
 * Created by Mike on 6/16/2018.
 */

public class XLive {
    private int _status;
    private int _elapsedTime = 0;
    private int _sessionLen = 0;


    public int getStatus(){
        return _status;
    }

    public void setStatus(int status) {
        _status = status;
    }

    public int getElapsedTime(){
        return _elapsedTime;
    }

    public void setElapsedTime(int value){
        _elapsedTime = value;
    }

    public int getTotalTime(){
        return _sessionLen;
    }

    public boolean updateFromMessage(OscMessage message) {
        if (message.getArgumentCount() > 0) {
            switch(message.getAddress()){
                case "/-stat/urec/state":
                    this._status = message.<Integer>getArgument(0);
                    return true;
                case "/-stat/urec/etime":
                    this._elapsedTime = message.<Integer>getArgument(0);
                    return true;
                case "/-urec/sessionlen":
                    this._sessionLen = message.<Integer>getArgument(0);
                    return true;
                //case "/-urec/session/002/name":
                default:
                    return false;
            }
        }

        return false;
    }
}

