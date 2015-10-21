package com.example.sensordisplay;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;

import org.apache.http.util.ByteArrayBuffer;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity {

	//private static final String TAG = "FTPSClient";
	private TextView tv;
	protected Handler handler;
	protected String DownloadUrl = " ";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		handler=new Handler();
		handler.postDelayed(r, 1000);
	}
	
	// Setup a runnable to read the sdcard logs and display on the screen
	final Runnable r = new Runnable() {
	    public void run() {
             DownloadFromUrl("http://192.168.1.109/sda1/last_wlog",  "last_wlog");
             DownloadFromUrl("http://192.168.1.109/sda1/last_wulog", "last_wulog");
             DownloadFromUrl("http://192.168.1.109/sda1/last_frlog", "last_frlog");             
   			 readSDFile();
   			 handler.postDelayed(this, 10000); 
	        }		    
	};
		
	    // stop the synclogs and sdcard read tasks	    
	    @Override
	    public void onStop() {
	        super.onStop();
	        //task.cancel(true);
	        handler.removeCallbacks(r);
	    }
	     		
	// Method to write ascii text characters to file on SD card. Note that
    // you must add a WRITE_EXTERNAL_STORAGE permission to the manifest file
    // or this method will throw a FileNotFound Exception because you won't
    // have write permission.

	private void readSDFile(){

		// Find the root of the external storage.
		// See http://developer.android.com/guide/topics/data/data-storage.html#filesExternal
		Typeface font_view02 = Typeface.createFromAsset(getAssets(), "fonts/courbd.ttf");
		File sdroot = android.os.Environment.getExternalStorageDirectory(); 
		File sddir = new File (sdroot.getAbsolutePath() + "/SensorData");
		sddir.mkdirs();
		File mywFile = new File(sddir, "last_wlog");
	    Log.d("WLOG", "Reading last_wlog");
		File myuFile = new File(sddir, "last_wulog");
		File myfFile = new File(sddir, "last_frlog");

		
		String wDataRow = "";String wBuffer = "";
		String uDataRow = "";String uBuffer = "";
		String fDataRow = "";String fBuffer = "";		
		int FcolorStatus = Color.GREEN;
		int BcolorStatus = Color.BLACK;
		
		//***
		//*** Get log files
		//***
		
		// Format of last_wulog
		// "${cloc},${sloc},${tempF},${rh},${dpF},${bp},${wd},${wgm},${flF},${cw},${wm}"
		// Grafton,WI,66.6,70%,56,30.02,SE,6,66.6,Mostly Cloudy,1
		try {
			FileInputStream fuIn = new FileInputStream(myuFile);
			BufferedReader myuReader = new BufferedReader(
					new InputStreamReader(fuIn));
			while ((uDataRow = myuReader.readLine()) != null) {
				uBuffer += uDataRow + "\n";
			}
			myuReader.close();
		} catch (IOException e) {
			Log.e("TextView", " " + e);
	    	}
		//String[] uList = uBuffer.split("[\\s,]+");
		String[] uList = uBuffer.split(",");

		// Format of last_wlog:
		// 20150709,10:48,25,3846,4081,54,70.3,53
		try {
			FileInputStream fwIn = new FileInputStream(mywFile);
			BufferedReader mywReader = new BufferedReader(
					new InputStreamReader(fwIn));
			while ((wDataRow = mywReader.readLine()) != null) {
				wBuffer += wDataRow + "\n";
			}
			mywReader.close();
		} catch (IOException e) {
			Log.e("TextView", " " + e);
	    	}
		String[] wList = wBuffer.split("[\\s,]+");
		String wbatt = "Good";
			
		try {
			FileInputStream ffIn = new FileInputStream(myfFile);
			BufferedReader myfReader = new BufferedReader(
					new InputStreamReader(ffIn));
			while ((fDataRow = myfReader.readLine()) != null) {
				fBuffer += fDataRow + "\n";
			}
			myfReader.close();
		} catch (IOException e) {
			Log.e("TextView", " " + e);
	    	}
						
		if (wBuffer != "" && uBuffer != "" && fBuffer != "") {
			// If battery level getting low show level in yellow
			if ((Integer.valueOf(wList[4])) <= 3900) {
				FcolorStatus = Color.YELLOW;
			}
			// If battery level getting very low show level in red
			if ((Integer.valueOf(wList[4])) <= 3800) {
				FcolorStatus = Color.RED;
				wbatt = "Low";
			}

			//Convert outsideTemp String to a double
			double outsideTempDBL = Double.parseDouble(wList[6]);
	  
			//Convert windSpeed String to a double
			double windSpeedDBL = Double.parseDouble(uList[10]);
			
			// Note that the Wind Chill is only defined for temperatures in the range
			// -45oF to +45oF and wind speeds 3 to 60 mph
			//  WC = 35.74 + 0.6215*T - 35.75*(V^(0.16)) + 0.4275*T*(V^(0.16)) 
			double windChill = 35.74 + (0.6215 * outsideTempDBL) - (35.75 * (Math.pow(windSpeedDBL,0.16))) + (0.4275 * (outsideTempDBL*(Math.pow(windSpeedDBL,0.16))));
			 
			//Format to keep the windChill to 2 digits after decimal
			DecimalFormat df = new DecimalFormat("00");
			//windChill = (int)(windChill*100)/100.0;
	
			//*** Display date and time
			tv = (TextView) findViewById(R.id.TextView01);
			tv.setText("");
			tv.setText("            "+wList[0]+"    "+wList[1]+"                                          Battery Level:  "+wbatt);
			setColorForWordInTextView(FcolorStatus, BcolorStatus, wbatt, tv);
			
			//*** Display weather data 
		    Log.d("WLOG", "Contents:" + wBuffer);
			tv = (TextView) findViewById(R.id.TextView02);
			tv.setTypeface(font_view02);
			tv.setText(""+"\n");
			tv.append("   Current Conditions: "+uList[9]+"\n");
			tv.append("   Temperature:        "+wList[6]+"\n");
			tv.append("   Feels Like:         "+df.format(windChill)+"\n");
			tv.append("   Humidity:           "+wList[5]+"\n");
			tv.append("   Barometer:          "+uList[5]+"\n");
			tv.append("   Dew Point:          "+wList[7]+"\n");
			tv.append("   Winds:              "+uList[6]+" "+uList[10].trim()+" mph \n");
			tv.append("   Wind Gusts:         "+uList[7]+" mph \n");
			tv.append("                        "+"\n");			
	    }
	}

	// Method use to get the sensor logs from the Arduino Yun using an http call
	public void DownloadFromUrl(final String DownloadUrl, final String fileName) {			
		  try {
	           File root = android.os.Environment.getExternalStorageDirectory();
	           File dir = new File (root.getAbsolutePath() + "/SensorData");
	           URL url = new URL(DownloadUrl); //you can write here any link
	           File file = new File(dir, fileName);

	           // Write message to log about what is happening
	           Log.d("WLOG", "Download begining");
	           Log.d("WLOG", "Download url:" + url);
	           Log.d("WLOG", "Downloaded file name:" + fileName);

	           // Open a connection to that URL
	           URLConnection ucon = url.openConnection();

	           // Define InputStreams to read from the URLConnection
	           InputStream is = ucon.getInputStream();
	           BufferedInputStream bis = new BufferedInputStream(is);

	           // Read bytes to the Buffer until there is nothing more to read(-1)
	           ByteArrayBuffer baf = new ByteArrayBuffer(100);
	           int current = 0;
	           while ((current = bis.read()) != -1) {
	              baf.append((byte) current);
	           }

	           // Convert the Bytes read to a String
	           FileOutputStream fos = new FileOutputStream(file);
	           fos.write(baf.toByteArray());
	           fos.flush();
	           fos.close();
		   } catch (IOException e) {
		       Log.d("WLOG", "Download Error: " + e);
		   }
	}

	// Method to set the text colors in a textview
    public void setColorForWordInTextView(int fcolor, int bcolor, String word, TextView tv) {
        String str = tv.getText().toString();
        Spannable span = new SpannableString(tv.getText());

        span.setSpan(new BackgroundColorSpan(bcolor), str.indexOf(word), str.indexOf(word) + word.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(new ForegroundColorSpan(fcolor), str.indexOf(word), str.indexOf(word) + word.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        tv.setText(span);
    }
}