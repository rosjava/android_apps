package com.github.rosjava.android_apps.listener;

import java.net.URI;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

// RosJava Imports
import org.ros.node.ConnectedNode;
import org.ros.node.NodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.address.InetAddressFactory;

// RosJava Messages
import std_msgs.String;

// Android Core Imports
import org.ros.android.MessageCallable;
import org.ros.android.view.RosTextView;

// Android App Imports
import com.github.rosjava.android_apps.application_management.RosAppActivity;

import std_msgs.*;

public class Listener extends RosAppActivity
{
    private Toast    lastToast;
    private ConnectedNode node;
    private RosTextView<std_msgs.String> rosTextView;

    public Listener()
    {
        super("Listener", "Listener");
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        setDefaultMasterName(getString(R.string.default_robot));
        setDefaultAppName(getString(R.string.paired_app_name));
        setDashboardResource(R.id.top_bar);
        setMainWindowResource(R.layout.main);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor)
    {
        super.init(nodeMainExecutor);
        NodeConfiguration nodeConfiguration =
                NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(), getMasterUri());
        rosTextView = (RosTextView<std_msgs.String>) findViewById(R.id.text);
        rosTextView.setTopicName(getAppNameSpace().resolve("chatter").toString());
        rosTextView.setMessageType(std_msgs.String._TYPE);
        rosTextView.setMessageToStringCallable(new MessageCallable<java.lang.String, String>() {
            @Override
            public java.lang.String call(std_msgs.String message) {
                return message.getData();
            }
        });
        nodeMainExecutor.execute(rosTextView, nodeConfiguration);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0,0,0,R.string.stop_app);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()){
            case 0:
                finish();
                break;
        }
        return true;
    }

//    /**
//     * Call Toast on UI thread.
//     * @param message Message to show on toast.
//     */
//    private void showToast(final String message)
//    {
//        runOnUiThread(new Runnable()
//        {
//            @Override
//            public void run() {
//                if (lastToast != null)
//                    lastToast.cancel();
//
//                lastToast = Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG);
//                lastToast.show();
//            }
//        });
//    }

}
