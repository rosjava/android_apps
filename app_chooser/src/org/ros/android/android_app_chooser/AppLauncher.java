package org.ros.android.android_app_chooser;


import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;

import org.ros.android.robotapp.AppManager;
import org.ros.android.robotapp.RosAppActivity;

import android.net.Uri;
import app_manager.ClientApp;

import java.net.URI;
import java.util.ArrayList;

public class AppLauncher {
  static private final String CLIENT_TYPE = "android";

  /** Launch a client app for the given robot app. */
  static public boolean launch(final RosAppActivity parentActivity, app_manager.App app, URI uri) {
    ArrayList<ClientAppData> android_apps = new ArrayList<ClientAppData>();
    
    if (parentActivity instanceof AppChooser) {
      ((AppChooser)parentActivity).onAppClicked(app, app.getClientApps().size() > 0);
    } else {
      Log.i("RosAndroid", "Could not launch becase parent is not an appchooser");
      if (app.getClientApps().size() == 0) {
        Log.e("RosAndroid", "Not launching application!!!");
        return false;
      }
    }

    if (app.getClientApps().size() == 0) {
      return false;
    }
    
    Log.i("RosAndroid", "launching robot app " + app.getName() + ". Found " + app.getClientApps().size()
        + " client apps.");

    // Loop over all possible client apps to find the android ones.
    for (int i = 0; i < app.getClientApps().size(); i++) {
      ClientApp client_app = app.getClientApps().get(i);
      if (client_app.getClientType() != null && client_app.getClientType().equals(CLIENT_TYPE)) {
        //android_apps.add(new ClientAppData(client_app));
     ClientAppData data = new ClientAppData(client_app);
     android_apps.add(data);
      }
    }

    Log.i("RosAndroid", "launching robot app " + app.getName() + ". Found " + android_apps.size()
        + " android apps.");

    // TODO: filter out android apps which are not appropriate for
    // this device by looking at specific entries in the manager_data_
    // map of each app in android_apps.
    ArrayList<ClientAppData> appropriateAndroidApps = android_apps;


    // TODO: support multiple android apps
    if (appropriateAndroidApps.size() != 1) {
      AlertDialog.Builder dialog = new AlertDialog.Builder(parentActivity);
      dialog.setTitle("Wrong Number of Android Apps");
      dialog.setMessage("There are " + appropriateAndroidApps.size() + " valid android apps, not 1 as there should be.");
      dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dlog, int i) {
            dlog.dismiss();
          }});
      dialog.show();
      return false;
    }

    //TODO: this installs the last android app in the set.
    String className = "";

    // Loop over all android apps, trying to launch one.
    for (int i = 0; i < appropriateAndroidApps.size(); i++) {
      ClientAppData appData = appropriateAndroidApps.get(i);
      Intent intent = appData.createIntent();
      intent.putExtra(AppManager.PACKAGE + ".robot_app_name", app.getName());
      intent.putExtra("ChooserURI", uri.toString());
      try {
        className = intent.getAction();
        Log.i("RosAndroid", "trying to startActivity( action: " + intent.getAction() + " )");
        //parentActivity.startActivity(intent);
        parentActivity.startActivityForResult(intent, 1);


        return true;
      } catch (ActivityNotFoundException e) {
        Log.i("RosAndroid", "activity not found for action: " + intent.getAction());
      }
    }

    final String installPackage = className.substring(0, className.lastIndexOf("."));

    Log.i("RosAndroid", "showing not-installed dialog.");

    // TODO:
    // Loop over all android apps, trying to install one. (??)
    // For now, just show a failure dialog.
    AlertDialog.Builder dialog = new AlertDialog.Builder(parentActivity);
    dialog.setTitle("Android app not installed.");
    dialog
        .setMessage("This robot app requires a client user interface app, but none of the applicable android apps are installed. Would you like to install the app from the market place?");
    dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dlog, int i) {
        Uri uri = Uri.parse("market://details?id=" + installPackage);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        parentActivity.startActivity(intent);
      }
    });
    dialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dlog, int i) {
        dlog.dismiss();
      }
    });
    dialog.show();
    return false;
  }
}