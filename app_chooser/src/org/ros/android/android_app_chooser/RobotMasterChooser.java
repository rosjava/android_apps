/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2011, Willow Garage, Inc.
 * Copyright (c) 2013, OSRF.
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *  * Neither the name of Willow Garage, Inc. nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *   
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.ros.android.android_app_chooser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
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

import org.ros.android.robotapp.RobotDescription;
import org.ros.android.robotapp.RobotId;
import org.ros.android.robotapp.zxing.IntentIntegrator;
import org.ros.android.robotapp.zxing.IntentResult;
import org.ros.android.robotapp.RobotsContentProvider;

import org.yaml.snakeyaml.Yaml;
/**
 * @author hersh@willowgarage.com
 * @author murase@jsk.imi.i.u-tokyo.ac.jp (Kazuto Murase)
 */
public class RobotMasterChooser extends Activity {

	private static final int ADD_URI_DIALOG_ID = 0;
	private static final int ADD_DELETION_DIALOG_ID = 1;
	public static final String ROBOT_DESCRIPTION_EXTRA = "org.ros.android.robotapp.RobotDescription";

	private List<RobotDescription> robots;
	private boolean[] selections;

	public RobotMasterChooser() {
		robots = new ArrayList<RobotDescription>();

	}

	private void readRobotList() {
		String str = null;
		Cursor c = getContentResolver().query(
				RobotsContentProvider.CONTENT_URI, null, null, null, null);
		if (c == null) {
			robots = new ArrayList<RobotDescription>();
			Log.e("MasterChooserActivity", "Content provider failed!!!");
			return;
		}
		if (c.getCount() > 0) {
			c.moveToFirst();
			str = c.getString(c
					.getColumnIndex(RobotsContentProvider.TABLE_COLUMN));
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
		final List<RobotDescription> robot = robots; // Avoid race conditions
		if (robot != null) {
			txt = yaml.dump(robot);
		}
		ContentValues cv = new ContentValues();
		cv.put(RobotsContentProvider.TABLE_COLUMN, txt);
		Uri newEmp = getContentResolver().insert(
				RobotsContentProvider.CONTENT_URI, cv);
		if (newEmp != RobotsContentProvider.CONTENT_URI) {
			Log.e("MasterChooserActivity", "Could not save, non-equal URI's");
		}
	}

	private void refresh() {
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
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				choose(position);
			}
		});
	}

	private void choose(int position) {
		RobotDescription robot = robots.get(position);
		if (robot == null || robot.getConnectionStatus() == null
				|| robot.getConnectionStatus().equals(robot.ERROR)) {
			AlertDialog d = new AlertDialog.Builder(RobotMasterChooser.this)
					.setTitle("Error!")
					.setCancelable(false)
					.setMessage("Failed: Cannot contact robot")
					.setNeutralButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
								}
							}).create();
			d.show();
		} else {

			Intent resultIntent = new Intent();
			resultIntent
					.putExtra(ROBOT_DESCRIPTION_EXTRA, robots.get(position));
			setResult(RESULT_OK, resultIntent);
			finish();
		}
	}

	private void addMaster(RobotId robotId) {
		addMaster(robotId, false);
	}

	private void addMaster(RobotId robotId, boolean connectToDuplicates) {
		Log.i("MasterChooserActivity", "addMaster [" + robotId.toString() + "]");
		if (robotId == null || robotId.getMasterUri() == null) {
		} else {
			for (int i = 0; i < robots.toArray().length; i++) {
				RobotDescription robot = robots.get(i);
				if (robot.getRobotId().equals(robotId)) {
					if (connectToDuplicates) {
						choose(i);
						return;
					} else {
						Toast.makeText(this, "That robot is already listed.",
								Toast.LENGTH_SHORT).show();
						return;
					}
				}
			}
			Log.i("MasterChooserActivity", "creating robot description: "
					+ robotId.toString());
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
	}

	private void deleteSelectedRobots(boolean[] array) {
		int j = 0;
		for (int i = 0; i < array.length; i++) {
			if (array[i]) {
				robots.remove(j);
			} else {
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
				Log.i("RosAndroid", "Removing robot with connection status '"
						+ robot.getConnectionStatus() + "'");
				iter.remove();
			}
		}
		onRobotsChanged();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		readRobotList();
		updateListView();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		IntentResult scanResult = IntentIntegrator.parseActivityResult(
				requestCode, resultCode, intent);
		if (scanResult != null && scanResult.getContents() != null) {
			Yaml yaml = new Yaml();
			Map<String, Object> data = (Map<String, Object>) yaml
					.load(scanResult.getContents().toString());
			Log.i("MasterChooserActivity", "OBJECT: " + data.toString());
			try {
				addMaster(new RobotId(data), false);
			} catch (Exception e) {
				Toast.makeText(this,
						"Invalid robot description: " + e.getMessage(),
						Toast.LENGTH_SHORT).show();
			}
		} else {
			Toast.makeText(this, "Scan failed", Toast.LENGTH_SHORT).show();
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
			EditText controlUriField = (EditText) dialog
					.findViewById(R.id.control_uri_editor);
			uriField.setText("http://localhost:11311/",
					TextView.BufferType.EDITABLE);
			// controlUriField.setText("http://prX1.willowgarage.com/cgi-bin/control.py",TextView.BufferType.EDITABLE
			// );
			button = (Button) dialog.findViewById(R.id.enter_button);
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
			if (robots.size() > 0) {
				selections = new boolean[robots.size()];
				Spannable[] robot_names = new Spannable[robots.size()];
				Spannable name;
				for (int i = 0; i < robots.size(); i++) {
					name = Factory.getInstance().newSpannable(
							robots.get(i).getRobotName() + newline
									+ robots.get(i).getRobotId());
					name.setSpan(new ForegroundColorSpan(0xff888888), robots
							.get(i).getRobotName().length(), name.length(),
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					name.setSpan(new RelativeSizeSpan(0.8f), robots.get(i)
							.getRobotName().length(), name.length(),
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					robot_names[i] = name;
				}
				builder.setTitle("Delete a robot");
				builder.setMultiChoiceItems(robot_names, selections,
						new DialogSelectionClickHandler());
				builder.setPositiveButton("Delete Selections",
						new DialogButtonClickHandler());
				builder.setNegativeButton("Cancel",
						new DialogButtonClickHandler());
				dialog = builder.create();
			} else {
				builder.setTitle("No robots to delete.");
				dialog = builder.create();
				final Timer t = new Timer();
				t.schedule(new TimerTask() {
					public void run() {
						removeDialog(ADD_DELETION_DIALOG_ID);
					}
				}, 2 * 1000);
			}
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	public class DialogSelectionClickHandler implements
			DialogInterface.OnMultiChoiceClickListener {
		public void onClick(DialogInterface dialog, int clicked,
				boolean selected) {
			return;
		}
	}

	public class DialogButtonClickHandler implements
			DialogInterface.OnClickListener {
		public void onClick(DialogInterface dialog, int clicked) {
			switch (clicked) {
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
		EditText controlUriField = (EditText) dialog
				.findViewById(R.id.control_uri_editor);
		String newControlUri = controlUriField.getText().toString();
		EditText wifiNameField = (EditText) dialog
				.findViewById(R.id.wifi_name_editor);
		String newWifiName = wifiNameField.getText().toString();
		EditText wifiPasswordField = (EditText) dialog
				.findViewById(R.id.wifi_password_editor);
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
				Toast.makeText(RobotMasterChooser.this, "Invalid Parameters.",
						Toast.LENGTH_SHORT).show();
			}
		} else {
			Toast.makeText(RobotMasterChooser.this, "Must specify Master URI.",
					Toast.LENGTH_SHORT).show();
		}
	}

	public class DialogKeyListener implements DialogInterface.OnKeyListener {
		@Override
		public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
			if (event.getAction() == KeyEvent.ACTION_DOWN
					&& keyCode == KeyEvent.KEYCODE_ENTER) {
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
		IntentIntegrator.initiateScan(this, IntentIntegrator.DEFAULT_TITLE,
				IntentIntegrator.DEFAULT_MESSAGE, IntentIntegrator.DEFAULT_YES,
				IntentIntegrator.DEFAULT_NO, IntentIntegrator.QR_CODE_TYPES);
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
}
