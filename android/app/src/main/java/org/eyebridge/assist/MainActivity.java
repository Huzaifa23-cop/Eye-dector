package org.eyebridge.assist;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Experimental accessibility research prototype only. A human caregiver must
 * remain available; do not use as the sole means of emergency communication.
 */
public final class MainActivity extends ComponentActivity {
    private static final int BG=0xFF08111C, PANEL=0xFF14243A, GREEN=0xFF8CF2CC;
    private static final int WHITE=0xFFF2F7FF, MUTED=0xFFB5C8D6, AMBER=0xFFFFD58A, RED=0xFFFFA1A8;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private final LanAlerts network = new LanAlerts();
    private final GestureEngine engine = new GestureEngine();
    private final ArrayList<String> calibrationCsv = new ArrayList<>();
    private ArrayList<float[]> openSamples = new ArrayList<>(), closedSamples = new ArrayList<>();
    private FaceDetector detector;
    private ProcessCameraProvider cameraProvider;
    private PreviewView cameraView;
    private TextView status, meter, guidance, eventText, ipHint;
    private EditText addressInput, codeInput;
    private boolean patientMode = false, caregiverMode = false, alarmActive=false;
    private String collecting = "";
    private long collectingSince = 0, sessionSince = 0, lastUi = 0;
    private ToneGenerator tone;
    private final Runnable alarmPulse = new Runnable() {
        @Override public void run() {
            if (!alarmActive || !caregiverMode) return;
            if (tone != null) tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 900);
            handler.postDelayed(this, 2500);
        }
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        tone = new ToneGenerator(AudioManager.STREAM_ALARM, 90);
        showHome();
    }
    private int d(int dp) { return Math.round(dp*getResources().getDisplayMetrics().density); }
    private GradientDrawable bg(int fill,int border) {
        GradientDrawable g = new GradientDrawable();g.setColor(fill);g.setCornerRadius(d(15));
        if (border != 0)g.setStroke(d(1),border);
        return g;
    }
    private TextView text(String s,int size,int color,boolean bold) {
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
        t.setLineSpacing(d(3),1f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private Button btn(String name,boolean primary) {
        Button b=new Button(this);b.setText(name);b.setAllCaps(false);b.setTextSize(15);
        b.setTextColor(primary?BG:WHITE);b.setBackground(bg(primary?GREEN:PANEL,primary?0:0xFF39506A));
        b.setPadding(d(14),d(11),d(14),d(11));return b;
    }
    private EditText field(String hint,String initial) {
        EditText t=new EditText(this);t.setSingleLine(true);t.setText(initial);t.setHint(hint);
        t.setTextColor(WHITE);t.setHintTextColor(MUTED);t.setTextSize(16);
        t.setPadding(d(14),d(12),d(14),d(12));t.setBackground(bg(BG,0xFF39506A));
        return t;
    }
    private LinearLayout panel(LinearLayout root) {
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(d(16),d(16),d(16),d(16));c.setBackground(bg(PANEL,0xFF29445B));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=d(13);
        root.addView(c,p);return c;
    }
    private void add(LinearLayout parent,View v,int bottom) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=d(bottom);parent.addView(v,p);
    }
    private LinearLayout base(String title,String subtitle) {
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        LinearLayout root=new LinearLayout(this);root.setPadding(d(16),d(20),d(16),d(28));
        root.setOrientation(LinearLayout.VERTICAL);scroll.addView(root);setContentView(scroll);
        add(root,text("◉ EYE / BRIDGE     •     ASSIST",12,GREEN,true),9);
        add(root,text(title,28,WHITE,true),5);add(root,text(subtitle,12,MUTED,false),17);
        return root;
    }
    private void clearModes() {
        patientMode=false;caregiverMode=false;alarmActive=false;handler.removeCallbacks(alarmPulse);
        network.stopReceiver();if(cameraProvider!=null)cameraProvider.unbindAll();
        collecting="";engine.invalidate();openSamples.clear();closedSamples.clear();calibrationCsv.clear();
    }
    private void showHome() {
        clearModes();LinearLayout root=base("Communication with a blink","An open-source, experimental accessibility research prototype.");
        LinearLayout warning=panel(root);
        add(warning,text("NOT A MEDICAL OR EMERGENCY DEVICE",14,AMBER,true),6);
        add(warning,text("Never rely on this app as the only way to request help. Keep an independent call button and a nearby caregiver. False detections or missed alerts are possible.",13,WHITE,false),0);
        LinearLayout a=panel(root);add(a,text("01 / PATIENT PHONE",12,GREEN,true),6);
        add(a,text("Use the front camera. Calibrate open and closed eyes, then send deliberate eye-gesture requests.",14,WHITE,false),14);
        Button patient=btn("Open patient camera  →",true);add(a,patient,0);patient.setOnClickListener(v->showPatient());
        LinearLayout b=panel(root);add(b,text("02 / CAREGIVER PHONE",12,GREEN,true),6);
        add(b,text("Listen for requests from another phone on the same trusted Wi-Fi. This screen must remain open.",14,WHITE,false),14);
        Button caregiver=btn("Open caregiver receiver  →",true);add(b,caregiver,0);caregiver.setOnClickListener(v->showCaregiver());
        add(root,text("Privacy: no video or face images are transmitted or saved. Calibration samples stay in memory unless you explicitly export a CSV.",12,MUTED,false),0);
    }
    private void back(LinearLayout root){Button b=btn("← Switch mode",false);add(root,b,13);b.setOnClickListener(v->showHome());}
    private void showPatient() {
        clearModes();patientMode=true;sessionSince=SystemClock.elapsedRealtime();
        LinearLayout root=base("Patient camera","Step 1: calibrate. Step 2: enter caregiver phone IP and code.");back(root);
        LinearLayout cameraCard=panel(root);add(cameraCard,text("LIVE / FRONT CAMERA — VIDEO STAYS HERE",11,GREEN,true),8);
        FrameLayout frame=new FrameLayout(this);cameraView=new PreviewView(this);
        cameraView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        frame.addView(cameraView,new FrameLayout.LayoutParams(-1,-1));
        cameraCard.addView(frame,new LinearLayout.LayoutParams(-1,d(245)));
        meter=text("Allow camera permission to begin",13,WHITE,true);add(cameraCard,meter,8);
        guidance=text("Hold phone steady, face the camera in good light.",12,MUTED,false);add(cameraCard,guidance,0);
        LinearLayout calibration=panel(root);
        add(calibration,text("CALIBRATION / REQUIRED",13,GREEN,true),6);
        add(calibration,text("Follow the on-screen instructions. Collect 3 seconds looking at camera with eyes OPEN, then 3 seconds with eyes gently CLOSED.",13,WHITE,false),13);
        Button open=btn("1 · Record eyes OPEN (3 seconds)",false),closed=btn("2 · Record eyes CLOSED (3 seconds)",false);
        add(calibration,open,8);add(calibration,closed,8);
        open.setOnClickListener(v->startCalibration("OPEN"));closed.setOnClickListener(v->startCalibration("CLOSED"));
        Button export=btn("Export only labeled probability CSV",false);add(calibration,export,0);export.setOnClickListener(v->exportCsv());
        LinearLayout connect=panel(root);add(connect,text("CAREGIVER PHONE / SAME WI-FI",13,GREEN,true),8);
        add(connect,text("Caregiver IPv4 address",12,MUTED,false),4);
        addressInput=field("192.168.1.14","");add(connect,addressInput,10);
        add(connect,text("Caregiver's 6-digit code",12,MUTED,false),4);
        codeInput=field("123456","");codeInput.setInputType(2);add(connect,codeInput,10);
        Button test=btn("TEST connection (no alarm)",true);add(connect,test,10);test.setOnClickListener(v->send("PING"));
        Button tryAlert=btn("TEST caregiver notification",false);add(connect,tryAlert,0);tryAlert.setOnClickListener(v->send("TEST"));
        LinearLayout gestures=panel(root);add(gestures,text("EYE GESTURES AFTER CALIBRATION",13,GREEN,true),8);
        add(gestures,text("Close both eyes, then reopen:\n0.9–1.9s = I feel cold\n2.1–3.7s = I need help\n4–7s TWICE within 12s = emergency request\n\nRegular short blinks do nothing. Long sleep closures and lost tracking cancel the gesture. No gesture is guaranteed to be detected.",13,WHITE,false),8);
        eventText=text("No request sent",15,AMBER,true);add(gestures,eventText,0);
        status=text("CAMERA NOT READY",12,AMBER,true);add(root,status,0);
        initCamera();
    }
    private void startCalibration(String phase) {
        if(!patientMode)return;
        collecting=phase;collectingSince=SystemClock.elapsedRealtime();
        if(phase.equals("OPEN"))openSamples.clear();else closedSamples.clear();
        status.setText("Collecting " + phase + " samples… look straight into the camera");
        status.setTextColor(AMBER);
    }
    private void finishCalibration(String phase) {
        List<float[]> samples=phase.equals("OPEN")?openSamples:closedSamples;
        collecting="";
        if(samples.size()<8) {status.setText("Not enough valid frames. Retake " + phase);return;}
        if(openSamples.size()<8 || closedSamples.size()<8) {
            status.setText(phase+" recorded ("+samples.size()+" samples). Record the other phase.");return;
        }
        float[] a=mean(openSamples),b=mean(closedSamples);
        try { engine.calibrate(a[0],a[1],b[0],b[1]);
            status.setText("CALIBRATED ✓ — deliberate eye gestures enabled");status.setTextColor(GREEN);
        } catch(Exception ex){ status.setText("Calibration rejected: "+ex.getMessage());status.setTextColor(RED); }
    }
    private float[] mean(List<float[]> samples) {
        float a=0,b=0;for(float[] s:samples){a+=s[0];b+=s[1];}
        return new float[]{a/samples.size(),b/samples.size()};
    }
    private void exportCsv() {
        if(calibrationCsv.isEmpty()) {status.setText("First collect labeled calibration samples");return;}
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/csv");
        i.putExtra(Intent.EXTRA_TITLE,"eyebridge-opt-in-calibration.csv");
        startActivityForResult(i,200);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=200 || result!=RESULT_OK || data==null || data.getData()==null)return;
        try(OutputStream out=getContentResolver().openOutputStream(data.getData())) {
            if(out==null)throw new Exception("No export stream");
            out.write("ms_from_start,label,left_eye_open_probability,right_eye_open_probability\n".getBytes(StandardCharsets.UTF_8));
            for(String s:calibrationCsv)out.write((s+"\n").getBytes(StandardCharsets.UTF_8));
            if(status!=null)status.setText("CSV exported by your explicit request (no video)");
        }catch(Exception ex){if(status!=null)status.setText("Export failed: "+ex.getMessage());}
    }
    private void initCamera() {
        FaceDetectorOptions opts=new FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(.18f).build();
        if(detector==null)detector=FaceDetection.getClient(opts);
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA},55);
        } else startCamera();
    }
    @Override public void onRequestPermissionsResult(int req,@NonNull String[] perms,@NonNull int[] results) {
        super.onRequestPermissionsResult(req,perms,results);
        if(req==55 && patientMode) {
            if(results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED) startCamera();
            else status.setText("Camera denied. Enable camera in Android app settings.");
        }
    }
    private void startCamera() {
        if(!patientMode)return;
        ListenableFuture<ProcessCameraProvider> future=ProcessCameraProvider.getInstance(this);
        future.addListener(()->{
            try {
                cameraProvider=future.get();cameraProvider.unbindAll();
                Preview p=new Preview.Builder().build();p.setSurfaceProvider(cameraView.getSurfaceProvider());
                ImageAnalysis analysis=new ImageAnalysis.Builder()
                    .setTargetResolution(new android.util.Size(640,480))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
                analysis.setAnalyzer(cameraExecutor,this::analyze);
                cameraProvider.bindToLifecycle(this,CameraSelector.DEFAULT_FRONT_CAMERA,p,analysis);
                status.setText("Camera ready. Calibrate both eye states.");
            } catch(Exception ex){status.setText("Camera error: "+ex.getMessage());}
        },ContextCompat.getMainExecutor(this));
    }
    @androidx.camera.core.ExperimentalGetImage
    private void analyze(@NonNull ImageProxy proxy) {
        android.media.Image media=proxy.getImage();
        if(media==null || !patientMode){proxy.close();return;}
        try {
            InputImage image=InputImage.fromMediaImage(media,proxy.getImageInfo().getRotationDegrees());
            detector.process(image)
                .addOnSuccessListener(faces->processFaces(faces))
                .addOnFailureListener(e->{engine.reset();})
                .addOnCompleteListener(t->proxy.close());
        }catch(Exception ex){proxy.close();}
    }
    private void processFaces(List<Face> faces) {
        if(!patientMode)return;
        long now=SystemClock.elapsedRealtime();
        if(faces.size()!=1 || Math.abs(faces.get(0).getHeadEulerAngleY())>23) {
            engine.reset();
            if(now-lastUi>450){lastUi=now;meter.setText("Face not centered / multiple faces: no gestures accepted");}
            return;
        }
        Float l=faces.get(0).getLeftEyeOpenProbability(),r=faces.get(0).getRightEyeOpenProbability();
        if(l==null || r==null){engine.reset();return;}
        if(!collecting.isEmpty()) {
            String phase=collecting;
            float[] pair={l,r};
            if(phase.equals("OPEN"))openSamples.add(pair);else closedSamples.add(pair);
            calibrationCsv.add((now-sessionSince)+","+phase+","+l+","+r);
            if(now-collectingSince >=3000)finishCalibration(phase);
        } else if(engine.ready()) {
            GestureEngine.Event event=engine.update(l,r,now);
            switch(event) {
                case COLD: send("COLD");break;
                case HELP:send("HELP");break;
                case EMERGENCY_PENDING:
                    eventText.setText("⚠ EMERGENCY NOT SENT. Repeat 4–7s eye closure within 12s to confirm.");break;
                case EMERGENCY:send("EMERGENCY");break;
                default:break;
            }
        }
        if(now-lastUi>180){lastUi=now;
            meter.setText(String.format(Locale.US,"Eye-open probability  L %.2f  |  R %.2f",l,r));
            guidance.setText(!engine.ready()?"Calibration required before gesture alerts":
                "Eye closure held: "+engine.holdMs(now)+" ms  |  Camera active");
        }
    }
    private void send(String type) {
        if(!patientMode || addressInput==null || codeInput==null)return;
        if(!type.equals("PING") && !type.equals("TEST") && !engine.ready()){
            status.setText("CALIBRATE FIRST. No real request sent.");return;
        }
        String address=addressInput.getText().toString().trim(),code=codeInput.getText().toString().trim();
        if(eventText!=null)eventText.setText("Sending: "+type+" ...");
        if(status!=null)status.setText("Waiting for app-level acknowledgment...");
        network.send(address,code,type,(ok,msg)->runOnUiThread(()->{
            if(!patientMode)return;
            status.setText(msg);status.setTextColor(ok?GREEN:RED);
            eventText.setText((ok?"REQUEST RECEIVED ON CAREGIVER APP: ":"UNCONFIRMED REQUEST: ")+type);
            if(!ok)beep();
        }));
    }
    private void showCaregiver() {
        clearModes();caregiverMode=true;
        LinearLayout root=base("Caregiver receiver","Keep this screen open, sound on, phone awake, and on the same trusted Wi-Fi.");back(root);
        LinearLayout device=panel(root);
        add(device,text("THIS PHONE'S WI-FI IPv4",12,GREEN,true),7);
        ipHint=text(LanAlerts.localIps(),19,WHITE,true);add(device,ipHint,9);
        add(device,text("If there are multiple IPs, use the one on the same Wi-Fi network as the patient.",12,MUTED,false),11);
        add(device,text("6-digit pairing code (give privately to patient)",12,MUTED,false),5);
        String random=String.format(Locale.US,"%06d",new SecureRandom().nextInt(1000000));
        codeInput=field("123456",random);codeInput.setInputType(2);add(device,codeInput,10);
        Button listen=btn("START LISTENING · UDP 9876",true);add(device,listen,7);
        listen.setOnClickListener(v->startListening());
        LinearLayout received=panel(root);add(received,text("INCOMING REQUESTS",12,GREEN,true),8);
        eventText=text("No requests received yet",19,WHITE,true);add(received,eventText,8);
        Button silence=btn("ACKNOWLEDGE / SILENCE LOCAL ALARM",false);add(received,silence,0);
        silence.setOnClickListener(v->{alarmActive=false;handler.removeCallbacks(alarmPulse);
            if(tone!=null)tone.stopTone();eventText.setText("Alarm silenced on this phone. Check the person in real life.");});
        status=text("NOT LISTENING YET",12,AMBER,true);add(root,status,10);
        add(root,text("Network receipt confirmation is NOT proof that a caregiver has seen the message. No background notifications; the app must remain visible. Do not depend on this prototype during an emergency.",12,RED,false),0);
    }
    private void startListening() {
        try {
            String pin=codeInput.getText().toString().trim();
            network.startReceiver(pin,new LanAlerts.Listener() {
                @Override public void onAlert(String type,String id,String from) {runOnUiThread(()->{
                    if(!caregiverMode)return;
                    if(type.equals("PAIRING")){
                        status.setText("Patient app connected from "+from);status.setTextColor(GREEN);return;
                    }
                    if(type.equals("TEST")){
                        eventText.setText("TEST notification from "+from);beep();return;
                    }
                    String label=type.equals("COLD")?"PATIENT FEELS COLD":type.equals("HELP")?"PATIENT REQUESTS ASSISTANCE":"URGENT PATIENT REQUEST";
                    eventText.setText(label+"\nFrom: "+from);
                    status.setText("Received by app · caregiver must respond in person");status.setTextColor(type.equals("EMERGENCY")?RED:GREEN);
                    if(type.equals("EMERGENCY")) {
                        alarmActive=true;handler.removeCallbacks(alarmPulse);handler.post(alarmPulse);
                    } else {beep();}
                    try {Vibrator vibe=(Vibrator)getSystemService(VIBRATOR_SERVICE);
                        if(vibe!=null && Build.VERSION.SDK_INT>=26) vibe.vibrate(VibrationEffect.createOneShot(650,230));
                    }catch(Exception ignored) { }
                });}
                @Override public void onError(String msg){runOnUiThread(()->{if(caregiverMode)status.setText(msg);});}
            });
            status.setText("LISTENING ✓ · local Wi-Fi only");status.setTextColor(GREEN);
        } catch(Exception ex){status.setText("Cannot listen: "+ex.getMessage());status.setTextColor(RED);}
    }
    private void beep(){if(tone!=null)tone.startTone(ToneGenerator.TONE_PROP_ACK,450);}
    @Override protected void onDestroy(){clearModes();network.close();cameraExecutor.shutdownNow();
        if(detector!=null)detector.close();if(tone!=null)tone.release();super.onDestroy();}
}
