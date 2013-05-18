package com.dteam.acwavab;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.PublicKey;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.userauth.UserAuthException;

import android.os.Bundle;
import android.app.Activity;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.VideoView;

public class MainActivity extends Activity {
	
    String user="ruraj", host="192.168.0.101", password="belief";

    SSHClient ssh = new SSHClient();
    Session session;
    Thread sshthread, commandThread;
    
    String command = "touch /home/ruraj/hello";
    Timer cameraTimer = new Timer();    
    
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		Log.i("ACW", "Creating and starting thread...");		
		
		Button stop = (Button) findViewById(R.id.stop_ssh);
		stop.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				sshthread.interrupt();
				try {
					session.close();
					ssh.disconnect();
				} catch (TransportException e) {
					e.printStackTrace();
				} catch (ConnectionException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}				
			}
		});
		
		Button start = (Button) findViewById(R.id.start_ssh);
		start.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				//if(!sshthread.isAlive()){
					sshthread = new Thread(new Runnable(){
						@Override
						public void run(){
							EditText x = (EditText) findViewById(R.id.ssh_host);
							host = x.getText().toString();
							x = (EditText) findViewById(R.id.ssh_user);
							user = x.getText().toString();
							x = (EditText) findViewById(R.id.ssh_password);
							password = x.getText().toString();	
							
							ssh.addHostKeyVerifier(  
								    new HostKeyVerifier() {
								    	@Override
								        public boolean verify(String arg0, int arg1, PublicKey arg2) {  
								            return true;  // don't bother verifying  
								        } 
								    }  
								);  
							try {
								ssh.connect(host);
								ssh.authPassword(user, password);								
								
							} catch (UserAuthException e) {
								e.printStackTrace();
							} catch (TransportException e) {
								e.printStackTrace();
							} catch (ConnectionException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					});
					sshthread.start();
				}				
			//}
		});
				
		Button send = (Button) findViewById(R.id.ssh_send);
		send.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg) {
				EditText commandT = (EditText) findViewById(R.id.ssh_command);
				command = commandT.getText().toString();
//				commandThread.interrupt();
//				commandThread.start();
				sendCMD();
			}
		});		
		
		final ImageView camera = (ImageView) findViewById(R.id.camera);
		cameraTimer.schedule(new TimerTask(){
			@Override
			public void run(){
				Thread getImage = new Thread(new Runnable(){
					@Override
					public void run(){
						try {
							URL	thumb_u = new URL("http://192.168.0.100:8080/shot.jpg");
							final Drawable thumb_d = Drawable.createFromStream(thumb_u.openStream(), "src");
							runOnUiThread(new Runnable(){
								@Override
								public void run(){
									camera.setImageDrawable(thumb_d);
								}
							});							
						} catch (MalformedURLException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
				getImage.start();
			}
		}, 0, 50);
	}

	public void sendCMD()
	{
		try
		{	
			session = ssh.startSession();
			final Command cmd = session.exec(command);
			System.out.println(IOUtils.readFully(cmd.getInputStream()).toString());
			cmd.join(5, TimeUnit.SECONDS);
			System.out.println("\n** exit status: " + cmd.getExitStatus());

			if(Thread.interrupted())
			{
				Log.i("ACW", "Shutting down");
				return;
			}
			session.close();
		}
		catch (Exception e)
		{	            	
			Log.i("ACW", e.toString());
			//Log.i("ACW", System.err.toString());
			System.exit(2);
		}
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
//	@Override
//	public void onPause(){
//		cameraTimer.cancel();
//	}
	
//	@Override
//	public void onResume(){
//		cameraTimer.schedule(new TimerTask(){
//			@Override
//			public void run(){
//				Thread getImage = new Thread(new Runnable(){
//					@Override
//					public void run(){
//						try {
//							URL	thumb_u = new URL("http://192.168.0.100:8080/shot.jpg");
//							final Drawable thumb_d = Drawable.createFromStream(thumb_u.openStream(), "src");
//							runOnUiThread(new Runnable(){
//								@Override
//								public void run(){
//									camera.setImageDrawable(thumb_d);
//								}
//							});							
//						} catch (MalformedURLException e) {
//							e.printStackTrace();
//						} catch (IOException e) {
//							e.printStackTrace();
//						}
//					}
//				});
//				getImage.start();
//			}
//		}, 0, 50);
//	}
}
