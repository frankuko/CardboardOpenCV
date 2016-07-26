package com.tfg.javier.cardboardopencv;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class FdActivity extends Activity implements CvCameraViewListener2 {

    private static final String    TAG                 = "FaceboardActivity";

    private static final Scalar    FACE_ALLY           = new Scalar(0, 255, 0, 255);
    private static final Scalar    FACE_HOSTILE        = new Scalar(255, 255, 255, 255);

    public static final int        JAVA_DETECTOR       = 0;
    public static final int        NATIVE_DETECTOR     = 1;
    private static final Random    mRand               = new Random();

    private Mat                    mRgba;
    private Mat                    mGray;
    private Mat salida;
    private Mat red;
    private Mat green;
    private Mat blue;
    private Mat maxGb;

    private int cam_anchura = 1920; // 960 x 720
    private int cam_altura = 1080;


    Mat hierarchy;
    //private File                   mCascadeFile;
   // private CascadeClassifier      mJavaDetector;
    //private DetectionBasedTracker  mNativeDetector;

    private int                    mDetectorType       = JAVA_DETECTOR;
    private String[]               mDetectorName;

    private float                  mRelativeFaceSize   = 0.2f;
    private int                    mAbsoluteFaceSize   = 0;

    private CameraBridgeViewBase   mOpenCvCameraView;

    private TextView			   mTextViewFaceTextRight;
    private TextView			   mTextViewRandomNumbers;

    //For brightness settings
    private float                  mBrightness;

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.setMaxFrameSize(cam_anchura , cam_altura);
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    public FdActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";

        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        //Screen flags
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
        setContentView(R.layout.face_detect_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        //mTextViewFaceTextRight = (TextView) findViewById(R.id.textViewFacesRight);
       // mTextViewRandomNumbers = (TextView) findViewById(R.id.textViewRandomNumbers);

        /*Typeface typeface_terminator = Typeface.createFromAsset(getAssets(), "fonts/terminator_real_nfi.ttf");
        mTextViewFaceTextRight.setTypeface(typeface_terminator);
        mTextViewRandomNumbers.setTypeface(typeface_terminator);*/

    }

    final Handler facesHandler = new Handler() {
        public void handleMessage(Message msg) {
           // setFacesNumber(Integer.parseInt((String) msg.obj));
           // generateVeryImportantNumbers();
        }
    };

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null){
            mOpenCvCameraView.disableView();
        }
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        mBrightness = layout.screenBrightness;
        layout.screenBrightness = mBrightness;
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        //Screen brightness
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        mBrightness = layout.screenBrightness;
        layout.screenBrightness = 1F;
        getWindow().setAttributes(layout);
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
        salida = new Mat();
        red = new Mat();
        green = new Mat();
        blue = new Mat();
        maxGb = new Mat();
        hierarchy = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
        salida.release();
        red.release();
        green.release();
        blue.release();
        maxGb.release();
        hierarchy.release();
    }

    public Mat procesaRojos(){

        Core.extractChannel(mRgba,red,0);

        Core.extractChannel(mRgba,green,1);

        Core.extractChannel(mRgba,blue,2);


        Core.max(green,blue,maxGb);


        //sacamos el color rojo
        Core.subtract(red, maxGb,salida);

        Core.MinMaxLocResult minMax = Core.minMaxLoc(salida);

        int maximun = (int) minMax.maxVal;
        int thresh = maximun / 4;

        Imgproc.threshold(salida,salida,thresh,255,Imgproc.THRESH_BINARY);


        List<MatOfPoint> blobs = new ArrayList<MatOfPoint>();

        Imgproc.findContours(salida, blobs, hierarchy, Imgproc.RETR_CCOMP,
                Imgproc.CHAIN_APPROX_NONE);

        int minimunHeight = 30;
        float maxratio = (float) 0.75;

        for (int c = 0; c<blobs.size(); c++){

            double[] data = hierarchy.get(0,c);
            int parent = (int)data[3];

            /*if(parent < 0) //contorno exterior: rechazar
                continue;*/

            Rect BB = Imgproc.boundingRect(blobs.get(c));

            //Comprobamos el tamaño del rectangulo contenedor

            float hf = BB.height;
            float wf = BB.width;
            float ratio = wf / hf;

            if(wf < minimunHeight || hf < minimunHeight){
                continue;
            }

            //Imgproc.

            float estimatedArea = (float)Math.PI * (hf/2) * (wf/2);

            if(estimatedArea < minimunHeight)
                continue;

            //Comprobamos que la anchura es similar a la altura

            if(ratio < maxratio || ratio > 1.0 /maxratio){
                continue;
            }

            // Comprobar que no esta cerca del borde

            if(BB.x < 2 || BB.y < 2){
                continue;
            }

            //if(mRgba.width() - )

            Point P1 = new Point(BB.x,BB.y);
            Point P2 = new Point(BB.x+BB.width, BB.y+BB.height);


            //Imgproc.rectangle();


            Imgproc.rectangle(mRgba,P1,P2,new Scalar(255,255,0));
            //Imgproc.circ
            //Imgproc.circle(mRgba,P2,BB.width,new Scalar(255,255,0));

        }


        return mRgba;
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {


        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        /*Mat paso_bajo = new Mat();
        Size s1 = new Size(7,7);
        Imgproc.blur(mGray,paso_bajo,s1);

        Mat paso_alto = new Mat();

        Core.subtract(mGray,paso_bajo,paso_alto);


        return paso_alto;*/





        return procesaRojos();
    }
    /*private void generateVeryImportantNumbers(){
        if(mTextViewRandomNumbers != null){
            mTextViewRandomNumbers.setText(mostImportantMethod());
        }
    }*/

    /*private String mostImportantMethod(){
        String result = "";
        for(int i = 0; i < 6; i++){
            result += notRandomNumber() + " " + notRandomNumber() + " " + notRandomNumber() + "\n";
        }
        return result;
    }*/

    /*private int notRandomNumber(){
        int x = mRand.nextInt(801) + 100;
        return x;
    }*/

    /*private void setFacesNumber(int i){
        if(mTextViewFaceTextRight != null){
            mTextViewFaceTextRight.setText("Faces: " + i);
        }
    }*/
}

