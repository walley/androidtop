package org.walley.androidtop;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.Process;
import java.lang.Runnable;
import android.widget.EditText;

public class androidtop extends Activity
{
  Handler handler_top;
  TextView tv_top;
  Button b_kill;
  EditText et_pid;
  Runnable runnable_top;
  String version;

//  public void onBackPressed() {
//    finish();
//  }

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    handler_top = new Handler();
    tv_top = (TextView) findViewById(R.id.tv_top);
    b_kill = (Button) findViewById(R.id.b_kill);
    et_pid = (EditText) findViewById(R.id.et_pid);

    new process_executor().execute("/system/bin/top --version");

    b_kill.setOnClickListener(new View.OnClickListener()
    {
      public void onClick(View v) 
      {
        //root_request();
        new process_executor().execute("su", "-c", "kill -9 " + et_pid.getText().toString());
      }
    });

    runnable_top = new Runnable() {
      @Override
      public void run() 
      {
        if (version.equals("old")) {
          new process_executor().execute("/system/bin/top -m 15 -n 1");
        } else {
          new process_executor().execute("/system/bin/top", "-b", "-n 1");
        }
        handler_top.postDelayed(this, 2000);
      }
    };
  }


  @Override
  protected void onPause() 
  {
    handler_top.removeCallbacks(runnable_top);
    super.onPause();
  }

  @Override
  protected void onResume()
  {
    handler_top.postDelayed(runnable_top, 2000);
    super.onResume();
  }

  @Override
  protected void onDestroy() 
  {
    handler_top.removeCallbacks(runnable_top);
    super.onDestroy();
  }

  public void root_request()
  {
    Process p;
    try {
       // Preform su to get root privledges  
       p = Runtime.getRuntime().exec("su");   

       // Attempt to write a file to a root-only   
       DataOutputStream os = new DataOutputStream(p.getOutputStream());
       os.writeBytes("echo \"Do I have root?\" >/data/sd/temporary.txt\n");

       // Close the terminal
       os.writeBytes("exit\n");
       os.flush();
       try {
         p.waitFor();
         if (p.exitValue() != 255) { 
           Log.e("TOP","root");
         } else {
           Log.e("TOP","not root");
         }
       } catch (InterruptedException e) {
         Log.e("TOP","not root");
       }
    } catch (Exception e) {
      Log.e("TOP","not root");
    }
  }

  class process_executor extends AsyncTask<String, String, String>
  {
    Process process;
    TextView mText = (TextView) findViewById(R.id.tv_top);

    public void stop() {
      Process p = process;
      if (p != null) {
        p.destroy();
      }
      Log.e("TOP", "end");
      cancel(true);
    }
 
    @Override
    protected void onPreExecute() {
//          cancel(true);

    }

    @Override
    protected String doInBackground(String... params) 
    {
      try {

        String[] xss = params;

        if (xss.length == 1) {
          process = Runtime.getRuntime().exec(params[0]);
        } else {
          process = Runtime.getRuntime().exec(params);
        }

        for (int i = 0; i < xss.length; i++) {   
          Log.i("TOP", "param: "+ i + " " + params[i]);
        }
      } catch (IOException e) {
        String err = "exec error " + e.toString();
        Log.e("TOP", err);
        return(err);
      } catch (Exception e) {
        Log.e("TOP", e.toString());
      }

      InputStreamReader reader = new InputStreamReader(process.getInputStream());
      BufferedReader buffer = new BufferedReader(reader);

      String line = null;
      String data = "";

      try {
        while ((line = buffer.readLine()) != null) {
          if (line.length() > 0) {
            data += line;
            data += "\n";
          }
        }

        if (data.length() == 0) {
          data = "/system/bin/top returned nothing:(\n";
        }

      } catch (Exception e) {
        Log.e("TOP", e.toString());
      }

      //version
      if (params[0].contains("version")) {
        if (data.contains("nothing")) {
          data = "old";
        }
        version = data.trim();
        Log.i("TOP", "top version:" + version + ".");
      }


      return data;
    }


    @Override
    protected void onPostExecute(String result) 
    {
      tv_top.setText(result);
    }

  }
}
