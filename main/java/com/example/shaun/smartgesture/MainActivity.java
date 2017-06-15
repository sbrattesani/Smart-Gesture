package com.example.shaun.smartgesture;

import android.content.Context;
import android.content.Intent;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, GestureOverlayView.OnGesturePerformedListener {

    Context context;
    Button writeButton;
    Button conButton;
    Button setButton;
    TextView mousePad;
    GestureLibrary mGestures;
    private File mStoreFile;

    private boolean isConnected=false;
    private boolean mouseMoved=false;
    private Socket socket;
    private PrintWriter out;

    private float startX =0;
    private float startY =0;
    private float disX =0;
    private float disY =0;

    // For action up click
    static final int MAX_CLICK_DURATION = 200;
    private long startClickTime;

   // private boolean isButtonClicked = false; // flag to record the gesture button on/off state
    private final File mFile = new File(Environment.getExternalStorageDirectory(), "gestures");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this; //save the context to show Toast messages
        mGestures = GestureLibraries.fromFile(mFile);

        //Get references of all buttons and set on click listeners
        writeButton = (Button)findViewById(R.id.gesButton);
        conButton = (Button)findViewById(R.id.conButton);
        setButton = (Button)findViewById(R.id.setButton);

        writeButton.setOnClickListener(this);
        conButton.setOnClickListener(this);
        setButton.setOnClickListener(this);

        //Get reference to the TextView acting as mousepad
        mousePad = (TextView)findViewById(R.id.mousePad);

        // register the OnGesturePerformedListener and gesture library (i.e. this activity)
        mStoreFile = new File(Environment.getExternalStorageDirectory(), "gestures");
        mGestures = GestureLibraries.fromFile(mStoreFile);
        if (!mGestures.load()) {
            finish();
        }

        GestureOverlayView gestures = (GestureOverlayView) findViewById(R.id.gesPad);
        gestures.addOnGesturePerformedListener(this);



        //capture gestures on TextView
        mousePad.setOnTouchListener(new View.OnTouchListener() {

            // Mouse mode functionality
            @Override
            public boolean onTouch(View v, MotionEvent event) {



                if(isConnected && out!=null && writeButton.getText().equals("Gesture")){
                    switch(event.getAction()){
                        case MotionEvent.ACTION_DOWN:
                            //save X and Y positions when user touches the TextView
                            startX =event.getX();
                            startY =event.getY();
                            mouseMoved=false;
                            startClickTime = Calendar.getInstance().getTimeInMillis();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            disX = event.getX()- startX; //movement in x
                            disY = event.getY()- startY; //movement in y

                            startX = event.getX();
                            startY = event.getY();
                            if(disX !=0|| disY !=0){
                                out.println(disX +","+ disY); //send mouse movement to server
                            }
                            mouseMoved=true;
                            break;
                        case MotionEvent.ACTION_UP:

                            long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                            if(clickDuration < MAX_CLICK_DURATION) {
                                out.println(ServerInstructions.MOUSE_LEFT_CLICK);
                            }
                                    }
                            }

                return true;
            }
        });
    }


    //OnClick functions for buttons
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.gesButton:
                // if dsfsdfd
                if (writeButton.getText().equals("Gesture")){
                    writeButton.setBackgroundResource(R.drawable.mousebtn);
                    writeButton.setText("Mouse");
                    mousePad.setVisibility(View.GONE);

                } else if(writeButton.getText().equals("Mouse")) {
                    writeButton.setBackgroundResource(R.drawable.gesbtn);
                    writeButton.setText("Gesture");
                    mousePad.setVisibility(View.VISIBLE);
                }
                break;

            case R.id.conButton:
                Connect connectPhoneTask = new Connect();
                connectPhoneTask.execute(ServerInstructions.SERVER_IP); //try to connect to server in another thread

                break;

            case R.id.setButton:

                // Open gesture builder activity
                Intent intent = new Intent(MainActivity.this, GestureBuilderActivity.class);
                startActivity(intent);
                break;
        }

    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if(isConnected && out!=null) {
            try {
                out.println("exit"); //tell server to exit
                socket.close(); //close socket
            } catch (IOException e) {
            }
        }
    }

    public class Connect extends AsyncTask<String,Void,Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            boolean result = true;
            try {
                InetAddress serverAddr = InetAddress.getByName(params[0]);
                socket = new Socket(serverAddr, ServerInstructions.SERVER_PORT);

            } catch (IOException e) {
                result = false;
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            isConnected = result;
            Toast.makeText(context,isConnected?"Connected to server!":"Error while connecting",Toast.LENGTH_LONG).show();
            try {
                if(isConnected) {
                    out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket
                            .getOutputStream())), true); //create output stream to send data to server
                }
            }catch (IOException e){
                Log.e("remotedroid", "Error while creating OutWriter", e);
                Toast.makeText(context,"Error while connecting",Toast.LENGTH_LONG).show();
            }
        }
    }

    // Gesture overlay method for recognising customer gestures from library
    public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
        ArrayList<Prediction> predictions = mGestures.recognize(gesture);

        // Determine if a gesture was performed.
        Prediction prediction = (predictions.size() != 0) ? predictions.get(0) : null;
        prediction = (prediction.score >= 1.2) ? prediction: null;

        // Call related server instruction as string
        if (prediction != null) {

            Toast.makeText(this, prediction.name, Toast.LENGTH_LONG).show();
            out.println(prediction.name);
        }
    }

    }



