package org.ros.android.android_app_chooser;

import org.ros.android.RosActivity;
import org.ros.node.NodeMainExecutor;

import android.os.Bundle;

public class AppChooser extends RosActivity
{
    protected AppChooser(String notificationTicker, String notificationTitle) {
		super(notificationTicker, notificationTitle);
		// TODO Auto-generated constructor stub
	}

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

	@Override
	protected void init(NodeMainExecutor nodeMainExecutor) {
		// TODO Auto-generated method stub
		
	}
}
