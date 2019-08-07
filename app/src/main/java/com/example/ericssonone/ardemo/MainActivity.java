package com.example.ericssonone.ardemo;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.app.Activity;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.util.DisplayMetrics;

import android.os.AsyncTask;

import org.opencv.android.JavaCameraView;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.*;
import org.opencv.videoio.*;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static org.opencv.core.CvType.CV_8UC1;

// OpenCV Classes

public class MainActivity extends Activity {

    // Used for logging success or failure messages
    private static final String TAG = "OCVSample::Activity";

    
    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    // Class used to run the task continuously. We needed a background task because Android wouldn't let us run the code in the main activity
    private class DownloadUDPData extends AsyncTask<Integer, Integer, Integer> {
        @Override
        protected Integer doInBackground(Integer... anInt) {
            Log.i(TAG, "Asynch background task started");
            // Create DatagramSocket and UDP setup variables

            int BUF_LEN = 65540;
            byte[] buffer = new byte[BUF_LEN];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            Log.i(TAG, "Datagram packet created");
            byte sizeofint = 8;
            int PACK_SIZE = 4096;
            //int FRAME_INTERVAL = (1000/30);
            int packetlength;

            // UDP RX Code starts here
            // Listen to the socket and recieve data. This is an RX only program
            
            //Core program loop
            try {
                DatagramSocket dsocket = new DatagramSocket(1997);  //Socket listens constantly 
                Log.i(TAG, "Datagram socket created");
                while (true) {

                    do {
                        dsocket.receive(packet);
                        Log.i(TAG, "socket recieve");
                        packetlength = packet.getLength();

                        Log.i(TAG, "Size of int pack = " + packetlength);
                    } while (packetlength > 4);
                    final ByteArrayInputStream bytein = new ByteArrayInputStream(packet.getData()); //Create byte array from packet
                    final int total_pack = bytein.read(); //read from ByteArrayInputStream
 
                    Log.i(TAG, "Size of int pack outside = " + packet.getLength());
                    Log.i(TAG, "Total Pack size:" + total_pack);
                    //read packet into a long buffer
                    byte[] longbuf = new byte[PACK_SIZE * total_pack];
                    for (int i = 0; i < total_pack; i++) {
                        dsocket.receive(packet);
                        for (int j = 0; j < packet.getLength(); j++) {
                            longbuf[j + i * PACK_SIZE] = buffer[j];
                        }

                    }
                    Log.i(TAG, "Received packet from" + packet.getAddress() + ":" + packet.getPort());

                    //Packet has now been recieved, end of UDP section
                    
                    // START OF OPENCV side of things
                    
                    //In open CV, data is stored in Matrix objects
                    Mat rawData = new Mat(1, PACK_SIZE * total_pack, CvType.CV_8UC1);   //create new Mat object
                    rawData.put(0, 0, longbuf); // Put the buffer data into the Mat
                    Mat frame = Imgcodecs.imdecode(rawData, Imgcodecs.CV_LOAD_IMAGE_COLOR); //Decode data into new mat object
                    if (frame.size().width == 0) { //If width is zero then no data was recieved that could populate the mat 
                        System.err.println("Empty Mat object recieved");
                        continue;
                    }
                    // Create Stereovision bny splitting the image into 2 identical images
                    
                    //Fit the data to the size of the screen
                    DisplayMetrics displayMetrics = new DisplayMetrics(); 
                    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics); //read the screen's display data

                    int Phone_Width = displayMetrics.widthPixels; //Width of phone in pixels
                    int Phone_Height = displayMetrics.heightPixels;//Height of phone in pixels


                    Mat frame_out = frame.clone(); 
                    Imgproc.resize(frame, frame, new Size((Phone_Width-1)/2, Phone_Height)); //resize to fill half the screen
                    //Imgproc.resize(frame, frame, new Size(Phone_Height-1/2, Phone_Width));
                    //Core.rotate(frame, frame, Core.ROTATE_90_CLOCKWISE);
                    Core.hconcat(Arrays.asList(frame, frame), frame);

                    // Create bitmap to show the frame
                    final Bitmap bm = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888); //Bitmap same size as frame
                    Utils.matToBitmap(frame, bm);   //Convert frame to bitmap

                    //ImageView show = (ImageView) findViewById(R.id.screen);
                    //show.setImageBitmap(bm);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // do UI updates here
                            ImageView show = (ImageView) findViewById(R.id.screen);
                            show.setImageBitmap(bm);
                        }
                    });


                }
            } catch (Exception e) {
                System.err.println(e);
                e.printStackTrace();
            }
            return anInt[0];
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {

        }

    }

    //onCreate method that runs the program when the app is opened
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.loadLibrary("opencv_java3");
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);

        // Starting shit

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        View decorView = getWindow().getDecorView();
        // Hide both the navigation bar and the status bar.
        // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
        // a general rule, you should design your app to hide the status bar whenever you
        // hide the navigation bar.
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        setContentView(R.layout.show_camera);


        //Test Executor thread
        //getUDPData test = new getUDPData();
        //test.execute(getData);

        //Test Asynch thread
        //Make Java happy and run the program!
        Integer x = new Integer(3);
        new DownloadUDPData().execute(x, x, x);



        /*
        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.show_camera_activity_java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
        */
    }
/*
    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {

        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        // Rotate mRgba 90 degrees
        Core.transpose(mRgba, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0,0, 0);
        Core.flip(mRgbaF, mRgba, 1 );

        return mRgba; // This function must return
    }*/
}

/*

    <org.opencv.android.JavaCameraView
        android:layout_width="fill_parent"
        android:layout_height="fill_parent"
        android:visibility="gone"
        android:id="@+id/show_camera_activity_java_surface_view"
        opencv:show_fps="true"
        opencv:camera_id="any" />
 */
