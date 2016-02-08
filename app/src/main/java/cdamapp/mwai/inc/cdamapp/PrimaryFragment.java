package cdamapp.mwai.inc.cdamapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.cardiomood.android.controls.gauge.SpeedometerGauge;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import cdamapp.mwai.inc.cdamapp.AndroidMultiPartEntity.ProgressListener;
import cdamapp.mwai.inc.library.JSONParser;

public class PrimaryFragment extends Fragment {


    private SpeedometerGauge speedometer;
    // LogCat tag
    private static final String TAG = MainActivity.class.getSimpleName();
    // ALL JSON node names
    private static final String TAG_FILENAME = "filename";
    private static final String TAG_LEVEL = "level";
    ArrayList<HashMap<String, String>> resultList;
    String classified_file = "";
    String file_level = "";

    private ProgressBar progressBar;
    private ProgressDialog pDialog;
    private String filePath = Environment.getExternalStorageDirectory()
            + "/test2wave.wav";
    private TextView txtPercentage;
    private ImageView imgPreview;
    private VideoView vidPreview;
    private Button btnUpload;
    long totalSize = 0;

    static InputStream is = null;
    // Creating JSON Parser object
    JSONParser jsonParser = new JSONParser();
    // products JSONArray
    JSONArray jcontent = null;

    private Button btnControl, btnClear;
    private TextView textDisplay, resultTxt;
    private WavAudioRecorder mRecorder;
    private static final String mRcordFilePath = Environment
            .getExternalStorageDirectory() + "/test2wave.wav";
    View v;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.primary_layout, container, false);



        // Customize SpeedometerGauge
        speedometer = (SpeedometerGauge) v.findViewById(R.id.speedometer);
        // configure value range and ticks
        speedometer.setMaxSpeed(100);


        // Add label converter
        speedometer.setLabelConverter(new SpeedometerGauge.LabelConverter() {
            @Override
            public String getLabelFor(double progress, double maxProgress) {

                if ((int) Math.round(progress)==10) {
                    return "Wet";
                }
                else if ((int) Math.round(progress)==50) {
                    return "Mid";
                }
                else if ((int) Math.round(progress)==100) {
                    return "Dry";
                }
                return String.valueOf((int) Math.round(progress));

            }
        });

        // configure value range and ticks
        speedometer.setMaxSpeed(100);
        speedometer.setMajorTickStep(50);
        speedometer.setMinorTicks(0);

        // Configure value range colors
        speedometer.addColoredRange(0, 30, Color.GREEN);
        speedometer.addColoredRange(30, 70, Color.YELLOW);
        speedometer.addColoredRange(70, 100, Color.RED);

        btnControl = (Button) v.findViewById(R.id.btnControl);
        btnControl.setText("Record Audio");
        mRecorder = WavAudioRecorder.getInstance();
        mRecorder.setOutputFile(mRcordFilePath);

        textDisplay = (TextView) v.findViewById(R.id.Textdisplay);

        btnControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // recordFor_5_Seconds();
                if (WavAudioRecorder.State.INITIALIZING == mRecorder.getState()) {
                    clearFile();
                    mRecorder.prepare();
                    mRecorder.start();
                    textDisplay.setText("");
                    btnControl.setText("Get Result");
                    textDisplay
                            .setText("Recording Audio...");

                } else if (WavAudioRecorder.State.ERROR == mRecorder.getState()) {
                    mRecorder.release();
                    mRecorder = WavAudioRecorder.getInstance();
                    mRecorder.setOutputFile(mRcordFilePath);
                    textDisplay
                            .setText("recording saved to: " + mRcordFilePath);
                    new UploadFileToServer().execute();
                    btnControl.setText("Record Audio");
                } else {
                    mRecorder.stop();
                    mRecorder.reset();
                    textDisplay.setText("");
                    new UploadFileToServer().execute();
                    btnControl.setText("Record Audio");
                }

            }
        });

        btnClear = (Button) v.findViewById(R.id.btnClear);
        btnClear.setText("Clear");
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearFile();
            }
        });

        return  v;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mRecorder) {
            mRecorder.release();
        }
    }

    public void clearFile() {
        File pcmFile = new File(mRcordFilePath);
        if (pcmFile.exists()) {
            pcmFile.delete();
            textDisplay.setText("");
        }
    }

    /**
     * Uploading the file to server
     * */
    private class UploadFileToServer extends AsyncTask<Void, Integer, String> {
        @Override
        protected void onPreExecute() {
            // setting progress bar to zero
            // progressBar.setProgress(0);
            super.onPreExecute();
            pDialog = new ProgressDialog(getActivity());
            pDialog.setMessage("Getting Moisture Content ...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            // Making progress bar visible
            // progressBar.setVisibility(View.VISIBLE);

            // updating progress bar value
            // progressBar.setProgress(progress[0]);

            // updating percentage value
            // txtPercentage.setText(String.valueOf(progress[0]) + "%");
        }

        @Override
        protected String doInBackground(Void... params) {
            return uploadFile();
        }

        @SuppressWarnings("deprecation")
        private String uploadFile() {
            String responseString = null;

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(Config.FILE_UPLOAD_URL);

            try {
                AndroidMultiPartEntity entity = new AndroidMultiPartEntity(
                        new ProgressListener() {

                            @Override
                            public void transferred(long num) {
                                publishProgress((int) ((num / (float) totalSize) * 100));
                            }
                        });

                File sourceFile = new File(filePath);

                // Adding file data to http body
                entity.addPart("file", new FileBody(sourceFile));

                // Extra parameters if you want to pass to server
				/*
				 * entity.addPart("website", new
				 * StringBody("www.androidhive.info")); entity.addPart("email",
				 * new StringBody("abc@gmail.com"));
				 */
                totalSize = entity.getContentLength();
                Log.e(TAG, "File Size: " + totalSize);
                httppost.setEntity(entity);

                // Making server call
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity r_entity = response.getEntity();
                is = r_entity.getContent();
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    // Server response
                    //responseString = EntityUtils.toString(r_entity);
                    JSONObject json = jsonParser.parseResponse(is);
                    // Check your log cat for JSON reponse
                    Log.d("Result JSON: ", json.toString());

                    // Storing each json item in variable
                    classified_file = json.getString(TAG_FILENAME);
                    file_level = json.getString(TAG_LEVEL);

                    // creating new HashMap
                    // HashMap<String, String> map = new HashMap<String,
                    // String>();

                    // adding each child node to HashMap key => value
                    // map.put(TAG_FILENAME, classified_file);
                    // map.put(TAG_LEVEL, file_level);

                    // adding HashList to ArrayList
                    // resultList.add(map);

                } else {
                    // Server response
                    // responseString = EntityUtils.toString(r_entity);
                    //responseString = "Error occurred! Http Status Code: "
                    //		+ statusCode;
                }

            } catch (ClientProtocolException e) {
                responseString = e.toString();
            } catch (IOException e) {
                responseString = e.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;

        }

        @Override
        protected void onPostExecute(String result) {
            // dismiss the dialog loading content
            pDialog.dismiss();

            //Log.e(TAG, "Response from server: " + result);

            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    resultTxt =  (TextView) v.findViewById(R.id.resultTxt);
                    speedometer = (SpeedometerGauge) v.findViewById(R.id.speedometer);
                    // configure value range and ticks
                    speedometer.setMaxSpeed(100);

                    // Add label converter
                    speedometer.setLabelConverter(new SpeedometerGauge.LabelConverter() {
                        @Override
                        public String getLabelFor(double progress, double maxProgress) {

                            if ((int) Math.round(progress)==10) {
                                return "Wet";
                            }
                            else if ((int) Math.round(progress)==50) {
                                return "Mid";
                            }
                            else if ((int) Math.round(progress)==100) {
                                return "Dry";
                            }
                            return String.valueOf((int) Math.round(progress));

                        }
                    });

                    // configure value range and ticks
                    speedometer.setMaxSpeed(100);
                    speedometer.setMajorTickStep(50);
                    speedometer.setMinorTicks(0);

                    // Configure value range colors
                    speedometer.addColoredRange(0, 30, Color.GREEN);
                    speedometer.addColoredRange(30, 70, Color.YELLOW);
                    speedometer.addColoredRange(70, 100, Color.RED);

                    if (file_level.equalsIgnoreCase("dry")) {
                        speedometer.setSpeed(100, 1000, 300);

                    }
                    else if (file_level.equalsIgnoreCase("mid")) {
                        speedometer.setSpeed(50, 1000, 300);
                    }
                    else if (file_level.equalsIgnoreCase("wet")) {
                        speedometer.setSpeed(10, 1000, 300);
                    }

                    resultTxt.setText("Inferred Dryness Level: " + file_level);
                }
            });

            // showing the server response in an alert dialog
            //showAlert(classified_file, file_level);
            super.onPostExecute(result);
        }

    }

    /**
     * Method to show alert dialog
     * */
    private void showAlert(String file, String level) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(
                "File Name: " + file + " \nMoisture Level Classification:"
                        + level).setTitle("Moisture Content")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // do nothing
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

}
