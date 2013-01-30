package org.ros.android.android_app_chooser;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.ros.node.NodeConfiguration;
import org.ros.android.robotapp.RobotDescription;
import org.ros.android.robotapp.RobotId;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.Spannable.Factory;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.ros.android.robotapp.RobotsContentProvider;
import org.yaml.snakeyaml.Yaml;

import com.google.common.base.Preconditions;

public class RobotMasterChooser extends Activity {
	  /**
	   * The key with which the last used {@link URI} will be stored as a
	   * preference.
	   */
	  private static final String PREFS_KEY_NAME = "URI_KEY";
	  private static final int ADD_URI_DIALOG_ID = 0;
	  private static final int ADD_DELETION_DIALOG_ID = 1;
	  public static final String ROBOT_DESCRIPTION_EXTRA = "org.ros.android.robotapp.RobotDescription";

	  /**
	   * Package name of the QR code reader used to scan QR codes.
	   */
	  private static final String BAR_CODE_SCANNER_PACKAGE_NAME =
	      "com.google.zxing.client.android.SCAN";

	  private String masterUri;
	  private EditText uriText;
	  private List<RobotDescription> robots;
	  //private MasterChooser currentRobotAccessor;
	  private boolean[] selections;

	  public RobotMasterChooser() {
	    robots = new ArrayList<RobotDescription>();
	    
	  }
	  
	  private void readRobotList() {
		    String str = null;
		    Cursor c = getContentResolver().query(RobotsContentProvider.CONTENT_URI, null, null, null, null);
		    if (c == null) {
		      robots = new ArrayList<RobotDescription>();
		      Log.e("MasterChooserActivity", "Content provider failed!!!");
		      return;
		    }
		    if (c.getCount() > 0) {
		      c.moveToFirst();
		      str = c.getString(c.getColumnIndex(RobotsContentProvider.TABLE_COLUMN));
		      Log.i("MasterChooserActivity", "Found: " + str);
		    }
		    if (str != null) {
		      Yaml yaml = new Yaml();
		      robots = (List<RobotDescription>) yaml.load(str);
		    } else {
		      robots = new ArrayList<RobotDescription>();
		    }
	  }
	  
	  public void writeRobotList() {
		    Log.i("MasterChooserActivity", "Saving robot...");
		    Yaml yaml = new Yaml();
		    String txt = null;
		    final List<RobotDescription> robot = robots; //Avoid race conditions
		    if (robot != null) { 
		      txt = yaml.dump(robot);
		    }
		    ContentValues cv = new ContentValues();
		    cv.put(RobotsContentProvider.TABLE_COLUMN, txt);
		    Uri newEmp = getContentResolver().insert(RobotsContentProvider.CONTENT_URI, cv);
		    if (newEmp != RobotsContentProvider.CONTENT_URI) {
		      Log.e("MasterChooserActivity", "Could not save, non-equal URI's");
		    }
	  }
	  
	  private void refresh() {
		    //currentRobotAccessor.loadCurrentRobot();
		    readRobotList();
		    updateListView();
	  }
	  
	  private void updateListView() {
		    setContentView(R.layout.robot_master_chooser);
		    ListView listview = (ListView) findViewById(R.id.master_list);
		    listview.setAdapter(new MasterAdapter(this, robots));
		    registerForContextMenu(listview);

		    
		    listview.setOnItemClickListener(new OnItemClickListener() {
		      @Override
		      public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		        choose(position);
		      }
		    });
		    
		    /*int index = 0;
		    for( RobotDescription robot: robots ) {
		      if( robot != null && robot.equals( currentRobotAccessor.getCurrentRobot() )) {
		        Log.i("MasterChooserActivity", "Highlighting index " + index);
		        listview.setItemChecked(index, true);
		        break;
		      }
		      index++;
		    }*/
		    
		  }
	  
	  private void choose(int position) {
		  RobotDescription robot = robots.get(position);
	      if (robot == null || robot.getConnectionStatus() == null
	          || robot.getConnectionStatus().equals(robot.ERROR)) {
		    	AlertDialog d = new AlertDialog.Builder(RobotMasterChooser.this).setTitle("Error!").setCancelable(false)
		    			.setMessage("Failed: Cannot contact robot")
		    			.setNeutralButton("OK", new DialogInterface.OnClickListener() {
		    				public void onClick(DialogInterface dialog, int which) { }
		    			}).create();
		    	d.show();
		    }
		    else {
			    masterUri = robots.get(position).getRobotId().toString();
			    SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
			    editor.putString(PREFS_KEY_NAME, masterUri);
			    editor.commit();
			    // Package the intent to be consumed by the calling activity.
			    Intent intent = new Intent();
			    intent.putExtra("ROS_MASTER_URI", masterUri);
			    setResult(RESULT_OK, intent);
			    finish();
		    }
		  /*
		    Intent resultIntent = new Intent();
		    resultIntent.putExtra(ROBOT_DESCRIPTION_EXTRA, robots.get(position));
		    setResult(RESULT_OK, resultIntent);
		    finish();*/
	  }
	  
	  private void addMaster(RobotId robotId) {
		    addMaster(robotId, false);
		  }
	  
	  private void addMaster(RobotId robotId, boolean connectToDuplicates)  {
		    Log.i("MasterChooserActivity", "addMaster ["+robotId.toString()+"]");
		    if (robotId == null || robotId.getMasterUri() == null) {

		    } else {
		      for (int i = 0; i < robots.toArray().length; i++) {
		        RobotDescription robot = robots.get(i);
		        if (robot.getRobotId().equals(robotId)) {
		          if (connectToDuplicates) {
		            choose(i);
		            return;
		          } else {
		            Toast.makeText(this, "That robot is already listed.", Toast.LENGTH_SHORT).show();
		            return;
		          }
		        }
		      }
		      Log.i("MasterChooserActivity", "creating robot description: "+robotId.toString());
		      robots.add(RobotDescription.createUnknown(robotId));
		      Log.i("MasterChooserActivity", "description created");
		      onRobotsChanged();
		    }
		  }
	  
	  private void onRobotsChanged() {
		    writeRobotList();
		    updateListView();
		  }
	  
	  private void deleteAllRobots() {
		    robots.clear();
		    onRobotsChanged();
		    //currentRobotAccessor.setCurrentRobot( null );
		    //currentRobotAccessor.saveCurrentRobot();
		  }
	  
	  
	  private void deleteSelectedRobots(boolean[] array) {
		    int j=0;
		    for (int i=0; i<array.length; i++) {
		      if (array[i]) {
		        /*if( robots.get(j).equals( currentRobotAccessor.getCurrentRobot() )) {
		          currentRobotAccessor.setCurrentRobot( null );
		          currentRobotAccessor.saveCurrentRobot();
		        }*/
		        robots.remove(j);
		      }
		      else {
		        j++;
		      }
		    }
		    onRobotsChanged();
		  }
	  
	  private void deleteUnresponsiveRobots() {
		    Iterator<RobotDescription> iter = robots.iterator();
		    while (iter.hasNext()) {
		      RobotDescription robot = iter.next();
		      if (robot == null || robot.getConnectionStatus() == null
		          || robot.getConnectionStatus().equals(robot.ERROR)) {
		        Log.i("RosAndroid", "Removing robot with connection status '" + robot.getConnectionStatus()
		            + "'");
		        iter.remove();
		        /*if( robot != null && robot.equals( currentRobotAccessor.getCurrentRobot() )) {
		          currentRobotAccessor.setCurrentRobot( null );
		          currentRobotAccessor.saveCurrentRobot();
		        }*/
		      }
		    }
		    onRobotsChanged();
		  }
	  
	  @Override
	  protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    //uriText = (EditText) findViewById(R.id.master_chooser_uri);
	    // Get the URI from preferences and display it. Since only primitive types
	    // can be saved in preferences the URI is stored as a string.
	    //masterUri =
	    //    getPreferences(MODE_PRIVATE).getString(PREFS_KEY_NAME,
	    //        NodeConfiguration.DEFAULT_MASTER_URI.toString());
	    //uriText.setText(masterUri);
	    readRobotList();
	    updateListView();
	  }

	  @Override
	  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
	    // If the Barcode Scanner returned a string then display that string.
	    if (requestCode == 0) {
	      if (resultCode == RESULT_OK) {
	        Preconditions.checkState(intent.getStringExtra("SCAN_RESULT_FORMAT").equals("TEXT_TYPE"));
	        String contents = intent.getStringExtra("SCAN_RESULT");
	        uriText.setText(contents);
	      }
	    }
	  }


	  @Override
	  protected Dialog onCreateDialog(int id) {
	    readRobotList();
	    final Dialog dialog;
	    Button button;
	    switch (id) {
	      case ADD_URI_DIALOG_ID:
	        dialog = new Dialog(this);
	        dialog.setContentView(R.layout.add_uri_dialog);
	        dialog.setTitle("Add a robot");
	        dialog.setOnKeyListener(new DialogKeyListener());
	        EditText uriField = (EditText) dialog.findViewById(R.id.uri_editor);
	        EditText controlUriField = (EditText) dialog.findViewById(R.id.control_uri_editor); 
	        uriField.setText("http://localhost:11311/",TextView.BufferType.EDITABLE );
	        //controlUriField.setText("http://prX1.willowgarage.com/cgi-bin/control.py",TextView.BufferType.EDITABLE );
	        button =(Button) dialog.findViewById(R.id.enter_button);
	        button.setOnClickListener(new View.OnClickListener() {
	          public void onClick(View v) {
	            enterRobotInfo(dialog);
	            removeDialog(ADD_URI_DIALOG_ID);
	          }
	        });
	        button = (Button) dialog.findViewById(R.id.scan_robot_button);
	        button.setOnClickListener(new View.OnClickListener() {
	          @Override
	          public void onClick(View v) {
	            scanRobotClicked(v);
	          }
	        });
	        button = (Button) dialog.findViewById(R.id.cancel_button);
	        button.setOnClickListener(new View.OnClickListener() {
	          @Override
	          public void onClick(View v) {
	            removeDialog(ADD_URI_DIALOG_ID);
	          } 
	        });
	        break;
	      case ADD_DELETION_DIALOG_ID:
	        AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        String newline = System.getProperty("line.separator");
	        if (robots.size()>0) {
	          selections = new boolean[robots.size()];
	          Spannable[] robot_names = new Spannable[robots.size()];
	          Spannable name;
	          for (int i=0; i<robots.size(); i++) {
	            name = Factory.getInstance().newSpannable(robots.get(i).getRobotName() + newline + robots.get(i).getRobotId());
	            name.setSpan(new ForegroundColorSpan(0xff888888), robots.get(i).getRobotName().length(), name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	            name.setSpan(new RelativeSizeSpan(0.8f), robots.get(i).getRobotName().length(), name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	            robot_names[i] = name;
	          }
	          builder.setTitle("Delete a robot");
	          builder.setMultiChoiceItems(robot_names, selections, new DialogSelectionClickHandler());
	          builder.setPositiveButton( "Delete Selections", new DialogButtonClickHandler() ); 
	          builder.setNegativeButton( "Cancel", new DialogButtonClickHandler());
	          dialog = builder.create();
	        }
	       else {
	         builder.setTitle("No robots to delete.");
	         dialog = builder.create();
	         final Timer t = new Timer();
	         t.schedule(new TimerTask() {
	           public void run() {
	             removeDialog(ADD_DELETION_DIALOG_ID);
	           }
	         }, 2*1000);
	       }
	        break;
	      default: 
	        dialog = null;
	    }
	    return dialog;
	  }
	  
	  public class DialogSelectionClickHandler implements DialogInterface.OnMultiChoiceClickListener {
		    public void onClick( DialogInterface dialog, int clicked, boolean selected ) {
		      return;
		      }
		    }
		     
		  public class DialogButtonClickHandler implements DialogInterface.OnClickListener {
		    public void onClick( DialogInterface dialog, int clicked ) {
		      switch( clicked ) {
		        case DialogInterface.BUTTON_POSITIVE:
		          deleteSelectedRobots(selections);
		          removeDialog(ADD_DELETION_DIALOG_ID);
		          break;
		        case DialogInterface.BUTTON_NEGATIVE:
		          removeDialog(ADD_DELETION_DIALOG_ID);
		          break;
		      }
		    }
		  }

	  
	  
	  public void enterRobotInfo(Dialog dialog) {
		    EditText uriField = (EditText) dialog.findViewById(R.id.uri_editor);
		    String newMasterUri = uriField.getText().toString();
		    EditText controlUriField = (EditText) dialog.findViewById(R.id.control_uri_editor);
		    String newControlUri = controlUriField.getText().toString();
		    EditText wifiNameField = (EditText) dialog.findViewById(R.id.wifi_name_editor);
		    String newWifiName = wifiNameField.getText().toString();
		    EditText wifiPasswordField = (EditText) dialog.findViewById(R.id.wifi_password_editor);
		    String newWifiPassword = wifiPasswordField.getText().toString();
		    if (newMasterUri != null && newMasterUri.length() > 0) {
		      Map<String, Object> data = new HashMap<String, Object>();
		      data.put("URL", newMasterUri);
		      if (newControlUri != null && newControlUri.length() > 0) {
		        data.put("CURL", newControlUri);
		      }
		      if (newWifiName != null && newWifiName.length() > 0) {
		        data.put("WIFI", newWifiName);
		      }
		      if (newWifiPassword != null && newWifiPassword.length() > 0) {
		        data.put("WIFIPW", newWifiPassword);
		      }
		      try {
		        addMaster(new RobotId(data));
		      } catch (Exception e) { 
		        Toast.makeText(RobotMasterChooser.this, "Invalid Parameters.", Toast.LENGTH_SHORT).show();
		      }
		    }
		    else {
		      Toast.makeText(RobotMasterChooser.this, "Must specify Master URI.", Toast.LENGTH_SHORT).show();
		  }
		}
	  
	  public class DialogKeyListener implements DialogInterface.OnKeyListener {
		    @Override
		    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
		      if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
		        Dialog dlg = (Dialog) dialog;
		        enterRobotInfo(dlg);
		        removeDialog(ADD_URI_DIALOG_ID);
		        return true;
		      }
		      return false;
		    }
		  }
	  
	  public void addRobotClicked(View view) {
		    showDialog(ADD_URI_DIALOG_ID);
	  }
	  public void refreshClicked(View view) {
		    refresh();
	  }

	  public void scanRobotClicked(View view) {
		    dismissDialog(ADD_URI_DIALOG_ID);
		    Intent intent = new Intent(BAR_CODE_SCANNER_PACKAGE_NAME);
		    intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
		    // Check if the Barcode Scanner is installed.
		    if (!isQRCodeReaderInstalled(intent)) {
		      // Open the Market and take them to the page from which they can download
		      // the Barcode Scanner app.
		      startActivity(new Intent(Intent.ACTION_VIEW,
		          Uri.parse("market://details?id=com.google.zxing.client.android")));
		    } else {
		      // Call the Barcode Scanner to let the user scan a QR code.
		      startActivityForResult(intent, 0);
		    }
	  }
	  
	  @Override
	  public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.robot_master_chooser_option_menu, menu);
	    return true;
	  }
	  @Override
	  public boolean onOptionsItemSelected(MenuItem item) {
	    int id = item.getItemId();
	    if (id == R.id.add_robot) {
	      showDialog(ADD_URI_DIALOG_ID);
	      return true;
	    } else if (id == R.id.delete_selected) {
	      showDialog(ADD_DELETION_DIALOG_ID);
	      return true;
	    } else if (id == R.id.delete_unresponsive) {
	      deleteUnresponsiveRobots();
	      return true;
	    } else if (id == R.id.delete_all) {
	      deleteAllRobots();
	      return true;
	    } else if (id == R.id.kill) {
		    Intent intent = new Intent();
		    setResult(RESULT_CANCELED, intent);
		    finish();
		    return true;
	    } else {
	      return super.onOptionsItemSelected(item);
	    }
	  }
	  
	  /**
	   * Check if the specified app is installed.
	   * 
	   * @param intent
	   *          The activity that you wish to look for.
	   * @return true if the desired activity is install on the device, false
	   *         otherwise.
	   */
	  private boolean isQRCodeReaderInstalled(Intent intent) {
	    List<ResolveInfo> list =
	        getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
	    return (list.size() > 0);
	  }
}
