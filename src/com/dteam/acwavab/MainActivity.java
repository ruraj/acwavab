package com.dteam.acwavab;

import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.PublicKey;
import java.util.Date;
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
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
	
    String user="pi", sshHost ="10.10.10.1", password="raspberry";
    String video_host = "10.0.0.3";
    
    ToggleButton toggleVideo = null;
    ToggleButton togglePwm = null;
    
    SSHClient sshClient = new SSHClient();
    Session sshSession = null;
    Thread sshThread = null;
    
    final String INIT_COMMAND = "touch /home/ruraj/i_was_here";

    Timer cameraTimer = new Timer();    
    Thread getImage = null;
    
    boolean pause = true, videoOn = false, controlEnabled = false;
    
    SensorManager mSensorManager;
	private SensorEventListener mSensorListener;

    Double refX;
    Double refY;
    Double refZ;

	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		Log.i("ACW", "Creating and starting thread...");		
		
		//Initialize sensors and sensor listeners
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mSensorListener = new SensorEventListener() {
			@Override
			public void onAccuracyChanged(Sensor arg0, int arg1) {
			}

			@Override
			public void onSensorChanged(SensorEvent event) {
				Sensor sensor = event.sensor;
				if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
					controlRobot(event.values);
				}
			}
		};		
		
		
		final ImageView camera = (ImageView) findViewById(R.id.camera);
		camera.setOnDragListener(new OnDragListener(){
			@Override
			public boolean onDrag(View arg0, DragEvent arg1) {
				if (arg1.getAction() == DragEvent.ACTION_DRAG_STARTED){
					//TODO: Camera Panning
					if (sshClient.isAuthenticated()){
						//TODO: Send camera pan signals
					}
				}
				return false;
			}
			
		});
		
		//Stop SSH connection Button
		Button stop = (Button) findViewById(R.id.stop_ssh);
		stop.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if (sshClient.isConnected()){
					try {
                        if (sshSession != null) {
                            sshSession.close();
                        }
						sshClient.disconnect();
					} catch (TransportException e) {
						e.printStackTrace();
					} catch (ConnectionException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else{
					Toast.makeText(getApplicationContext(), "You aren't connected yet!", Toast.LENGTH_LONG).show();
				}
			}
		});
		
		//Start SSH connection Button
		Button start = (Button) findViewById(R.id.start_ssh);
		start.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if(!sshClient.isConnected()){
                    sshClient = new SSHClient();
					sshThread = new Thread(new Runnable(){
						@Override
						public void run(){
							EditText x = (EditText) findViewById(R.id.ssh_host);
							sshHost = x.getText().toString();
							x = (EditText) findViewById(R.id.ssh_user);
							user = x.getText().toString();
							x = (EditText) findViewById(R.id.ssh_password);
							password = x.getText().toString();	
							
							sshClient.addHostKeyVerifier(
								    new HostKeyVerifier() {
								    	@Override
								        public boolean verify(String arg0, int arg1, PublicKey arg2) {  
								            return true;  // don't bother verifying  
								        } 
								    }  
								);  
							try {
								sshClient.connect(sshHost);
								sshClient.authPassword(user, password);
								
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
					sshThread.start();
                    Toast.makeText(getApplicationContext(), "Connecting now", Toast.LENGTH_LONG).show();
				} else {
                    Toast.makeText(getApplicationContext(), "Already Connected", Toast.LENGTH_LONG).show();
                }
			}
		});
		
		//Send SSH INIT_COMMAND button
		Button send = (Button) findViewById(R.id.ssh_send);
		send.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg) {
				if (sshClient.isAuthenticated() && sshClient.isConnected()) {
					EditText commandT = (EditText) findViewById(R.id.ssh_command);
					sendCMD(commandT.getText().toString());
                    Toast.makeText(getApplicationContext(), commandResult, Toast.LENGTH_LONG).show();
				} else {
                    Toast.makeText(getApplicationContext(), "Not connected or not authenticated", Toast.LENGTH_LONG).show();
                }
			}
		});		
		
		//Toggle video on/off button
		toggleVideo = (ToggleButton) findViewById(R.id.toggleVideo);
		toggleVideo.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if (!arg1){
					pause = true;
					videoOn = false;					
				}
				else{					
					if (pause){
						EditText video_hostT = (EditText) findViewById(R.id.video_host);
						video_host = video_hostT.getText().toString();
						
						pause = false;
						videoOn = true;

						synchronized(getImage){
							getImage.notify();
						}
					}					
				}
			}
			
		});

        togglePwm = (ToggleButton) findViewById(R.id.pwmToggle);

		//Toggle robot control on/off button
		ToggleButton toggleControls = (ToggleButton) findViewById(R.id.toggleControls);
		toggleControls.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if (arg1){
					if (sshClient.isConnected() && sshClient.isAuthenticated()){
                        if (!togglePwm.isChecked()) {
                            sendCMD("gpio mode 1 out; gpio mode 2 out; gpio mode 3 out; gpio mode 4 out");
                        } else {
                            sendCMD("gpio mode 1 pwm; gpio mode 2 pwm; gpio mode 3 pwm; gpio mode 4 pwm");
                        }
						controlEnabled = true;
						mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
						mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_UI);
					}
					else{
						Toast.makeText(getApplicationContext(), "SSH not connected.", Toast.LENGTH_LONG).show();
					}
				}
				else{
                    sendCMD("gpio mode 1 out; gpio mode 2 out; gpio mode 3 out; gpio mode 4 out");
                    sendCMD("gpio write 1 0; gpio write 2 0; gpio write 3 0; gpio write 4 0");

					controlEnabled = false;
					mSensorManager.unregisterListener(mSensorListener);
                    refX = null;
                    refY = null;
                    refZ = null;
				}
			}
		});
		
		//Main Streaming thread
		getImage = new Thread(new Runnable(){
			@Override
			public void run(){
				cameraTimer.schedule(new TimerTask(){
					@Override
					public void run(){						
						try {
							  
							if (pause){							
								try {
									runOnUiThread(new Runnable(){
										@Override
										public void run(){									
											camera.setImageResource(R.drawable.ic_launcher);
										}
									});
									synchronized(getImage){
										getImage.wait();
									}
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							
							URL	thumb_u = new URL("http://"+video_host+":8080/shot.jpg");
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
				}, 0, 60);
			}
		});
		
		//Start streaming
		getImage.start();
	}

    private int prevCommandBits;

    private Date lastReceived = null;

	protected void controlRobot(float[] values) {
		Double x = (double)values[0];
        Double y = (double)values[1];

        if (refX == null) {
            refX = x;
            refY = y;
        }

        Double diffX = x - refX;
        Double diffY = y - refY;

        if (!togglePwm.isChecked()) {
            int accel;
            int steer;

            if (diffX < -2) {
                accel = 0x0A;
            } else if (diffX > 2) {
                accel = 0x05;
            } else {
                accel = 0x00;
            }

            if (diffY < -2) {
                steer = 0x02;
            } else if (diffY > 2) {
                steer = 0x08;
            } else {
                steer = 0x00;
            }

            int controlBits = (accel|steer);

            if (prevCommandBits != controlBits) {
                String command = "gpio write 1 "+((controlBits >> 3)&1)+";gpio write 2 "+((controlBits >> 2)&1)+";gpio write 3 "+((controlBits >> 1)&1)+";gpio write 4 "+(controlBits&1);
                sendCMD(command);
                Log.i("ACW", command);
                prevCommandBits = controlBits;
            }
        } else {
            if (lastReceived == null || System.currentTimeMillis() - lastReceived.getTime() > 50) {
                lastReceived = new Date();
                int[] accel = new int[4];

                int factor = Integer.valueOf(((EditText) findViewById(R.id.pwmFactor)).getText().toString());

                boolean fwd;
                boolean bkd;

                if (diffX < -2) {
                    accel[1] = accel[3] = Math.abs(diffX.intValue() * factor) < 1023 ? Math.abs(diffX.intValue() * factor): 1023;
                    accel[0] = accel[2] = 0;
                    fwd = true;
                    bkd = false;
                } else if (diffX > 2) {
                    accel[0] = accel[2] = Math.abs(diffX.intValue() * factor) < 1023 ? Math.abs(diffX.intValue() * factor): 1023;
                    accel[1] = accel[3] = 0;
                    fwd = false;
                    bkd = true;
                } else {
                    accel[1] = accel[3] = accel[0] = accel[2] = 0;
                    fwd = false;
                    bkd = false;
                }

                if (diffY < -2) {
                    if (fwd) {
                        accel[1] = (accel[1] < 1023)?(accel[1] + Math.abs(diffY.intValue() * factor) > 1023)?1023:(accel[1] + Math.abs(diffY.intValue() * factor)): 1023;
                    } else if(bkd) {
                        accel[0] = (accel[0] < 1023)?(accel[0] + Math.abs(diffY.intValue() * factor) > 1023)?1023:(accel[0] + Math.abs(diffY.intValue() * factor)): 1023;
                    } else {
                        accel[1] = Math.abs(diffY.intValue() * factor) < 1023 ? Math.abs(diffY.intValue() * factor): 1023;
                    }
                } else if (diffY > 2) {
                    if (fwd) {
                        accel[3] = (accel[3] < 1023)?(accel[3] + Math.abs(diffY.intValue() * factor) > 1023)?1023:(accel[3] + Math.abs(diffY.intValue() * factor)): 1023;
                    } else if(bkd) {
                        accel[2] = (accel[2] < 1023)?(accel[2] + Math.abs(diffY.intValue() * factor) > 1023)?1023:(accel[2] + Math.abs(diffY.intValue() * factor)): 1023;
                    } else {
                        accel[3] = Math.abs(diffY.intValue() * factor) < 1023 ? Math.abs(diffY.intValue() * factor): 1023;
                    }
                }

                ((TextView) findViewById(R.id.b3)).setText(String.valueOf(accel[3]));
                ((TextView) findViewById(R.id.b2)).setText(String.valueOf(accel[2]));
                ((TextView) findViewById(R.id.b1)).setText(String.valueOf(accel[1]));
                ((TextView) findViewById(R.id.b0)).setText(String.valueOf(accel[0]));

                String command = "gpio pwm 1 "+accel[3]+";gpio pwm 2 "+accel[2]+";gpio pwm 3 "+accel[1]+";gpio pwm 4 "+accel[0];
                sendCMD(command);
                Log.i("ACW", command);
            }
        }
	}

    String commandResult;

	//Function to send SSH commands
	public void sendCMD(String command)
	{
        if (sshClient.isConnected() && sshClient.isAuthenticated()) {
            try
            {
                sshSession = sshClient.startSession();
                final Command cmd = sshSession.exec(command);

                commandResult = IOUtils.readFully(cmd.getInputStream()).toString();

                cmd.join(5, TimeUnit.SECONDS);
                System.out.println("\n** exit status: " + cmd.getExitStatus());

                if(Thread.interrupted())
                {
                    Log.i("ACW", "Shutting down");
                    return;
                }
                sshSession.close();
            }
            catch (Exception e)
            {
                Log.i("ACW", e.toString());
                //Log.i("ACW", System.err.toString());
                System.exit(2);
            }
        }
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item){
		switch (item.getItemId()){
		case R.id.action_settings:
			startActivity(new Intent(this, Settings.class));
		    return true;
		}
		return true;
	}
	
	@Override
	public void onPause(){		

		videoOn = false;
		toggleVideo.setChecked(false);

		super.onPause();
	}
	
	@Override
	public void onResume(){		
		
		if (videoOn)
			toggleVideo.setChecked(true);
		
		super.onResume();
	}
}
