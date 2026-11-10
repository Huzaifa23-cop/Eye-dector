package org.eyebridge.assist;

/** Experimental, calibrated, deterministic gesture parser. Not medical-grade. */
public final class GestureEngine {
    public enum Event { NONE, COLD, HELP, EMERGENCY_PENDING, EMERGENCY }
    private float leftThreshold = Float.NaN, rightThreshold = Float.NaN;
    private boolean eyesClosed;
    private long startedAt = -1, lastCommandAt = -100000, emergencyPendingUntil = -1;

    public void calibrate(float leftOpen, float rightOpen, float leftClosed, float rightClosed) {
        if (!Float.isFinite(leftOpen) || !Float.isFinite(rightOpen) ||
            !Float.isFinite(leftClosed) || !Float.isFinite(rightClosed) ||
            leftOpen-leftClosed < .24f || rightOpen-rightClosed < .24f ||
            leftClosed < 0 || rightClosed < 0 || leftOpen > 1 || rightOpen > 1)
            throw new IllegalArgumentException("Repeat calibration with better lighting");
        leftThreshold=(leftOpen+leftClosed)/2f;
        rightThreshold=(rightOpen+rightClosed)/2f;
        reset();
    }
    public boolean ready(){return Float.isFinite(leftThreshold) && Float.isFinite(rightThreshold);}
    public void invalidate(){leftThreshold=Float.NaN;rightThreshold=Float.NaN;reset();}
    public void reset(){eyesClosed=false;startedAt=-1;emergencyPendingUntil=-1;}
    public long holdMs(long now){return eyesClosed && startedAt>=0 ? Math.max(0,now-startedAt):0;}
    public Event update(float left,float right,long now){
        if(!ready() || !Float.isFinite(left) || !Float.isFinite(right) ||
           left<0 || left>1 || right<0 || right>1){reset();return Event.NONE;}
        if(emergencyPendingUntil>0 && now>emergencyPendingUntil)emergencyPendingUntil=-1;
        boolean closed=left<leftThreshold-.06f && right<rightThreshold-.06f;
        boolean open=left>leftThreshold+.06f && right>rightThreshold+.06f;
        if(!eyesClosed && closed){eyesClosed=true;startedAt=now;}
        else if(eyesClosed && open){
            eyesClosed=false;
            long duration=now-startedAt;
            startedAt=-1;
            if(now-lastCommandAt < 1400)return Event.NONE;
            if(duration>=900 && duration<1900){lastCommandAt=now;emergencyPendingUntil=-1;return Event.COLD;}
            if(duration>=2100 && duration<3700){lastCommandAt=now;emergencyPendingUntil=-1;return Event.HELP;}
            if(duration>=4000 && duration<=7000){
                lastCommandAt=now;
                if(emergencyPendingUntil>0 && now<=emergencyPendingUntil){emergencyPendingUntil=-1;return Event.EMERGENCY;}
                emergencyPendingUntil=now+12000;return Event.EMERGENCY_PENDING;
            }
        }
        return Event.NONE;
    }
}
