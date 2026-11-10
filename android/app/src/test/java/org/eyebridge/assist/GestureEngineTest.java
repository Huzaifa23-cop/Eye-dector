package org.eyebridge.assist;
import org.junit.Test;
import static org.junit.Assert.*;
public class GestureEngineTest {
    private GestureEngine engine(){GestureEngine e=new GestureEngine();e.calibrate(.95f,.93f,.08f,.10f);return e;}
    private GestureEngine.Event perform(GestureEngine e,long t,long hold){e.update(.01f,.01f,t);return e.update(.99f,.99f,t+hold);}
    @Test public void ignoreNaturalBlink(){assertEquals(GestureEngine.Event.NONE,perform(engine(),1000,190));}
    @Test public void coldRequest(){assertEquals(GestureEngine.Event.COLD,perform(engine(),1000,1100));}
    @Test public void helpRequest(){assertEquals(GestureEngine.Event.HELP,perform(engine(),1000,2500));}
    @Test public void urgentNeedsTwoClosures(){GestureEngine e=engine();assertEquals(GestureEngine.Event.EMERGENCY_PENDING,perform(e,1000,4500));assertEquals(GestureEngine.Event.EMERGENCY,perform(e,7200,4500));}
    @Test public void urgentExpires(){GestureEngine e=engine();assertEquals(GestureEngine.Event.EMERGENCY_PENDING,perform(e,1000,4500));assertEquals(GestureEngine.Event.EMERGENCY_PENDING,perform(e,21000,4500));}
    @Test public void lossCancelsGesture(){GestureEngine e=engine();e.update(.01f,.01f,1000);e.reset();assertEquals(GestureEngine.Event.NONE,e.update(.99f,.99f,6000));}
    @Test public void noCalibrationNoGestures(){assertEquals(GestureEngine.Event.NONE,perform(new GestureEngine(),1000,5000));}
    @Test public void longSleepIgnored(){assertEquals(GestureEngine.Event.NONE,perform(engine(),1000,9000));}
    @Test(expected=IllegalArgumentException.class)public void badCalibrationRejected(){new GestureEngine().calibrate(.5f,.5f,.4f,.4f);}
    @Test public void changingModeRequiresNewCalibration(){GestureEngine e=engine();e.invalidate();assertFalse(e.ready());}
}
