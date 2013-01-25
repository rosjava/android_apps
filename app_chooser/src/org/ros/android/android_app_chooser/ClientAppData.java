package org.ros.android.android_app_chooser;


import android.content.Intent;

import java.util.HashMap;
import java.util.List;

import app_manager.ClientApp;
import app_manager.KeyValue;

/**
* Convenience class which populates HashMaps with manager_data and app_data
* from the corresponding KeyValue arrays in the ClientApp message.
*/
public class ClientAppData {
  public HashMap<String, String> managerData;
  public List<app_manager.KeyValue> appData;

  public ClientAppData(app_manager.ClientApp clientApp) {
    managerData = keyValueListToMap(clientApp.getManagerData());
    appData = clientApp.getAppData();
  }

  public Intent createIntent() {
    Intent intent = new Intent();

    // Set up standard intent fields.
    if( managerData.get("intent-action" ) != null ) {
      intent.setAction(managerData.get("intent-action"));
    }
    if( managerData.get("intent-category") != null ) {
      intent.addCategory(managerData.get("intent-category"));
    }
    if( managerData.get("intent-type") != null ) {
      intent.setType(managerData.get("intent-type"));
    }
    // Can we handle classname and package name?

    // Copy all app data to "extra" data in the intent.
    for (int i = 0; i < appData.size(); i++) {
      KeyValue kv = appData.get(i);
      intent.putExtra(kv.getKey(), kv.getValue());
    }

    return intent;
  }

  private HashMap<String, String> keyValueListToMap(List<KeyValue> kvl) {
    HashMap<String, String> map = new HashMap<String, String>();
    for (int i = 0; i < kvl.size(); i++) {
      KeyValue kv = kvl.get(i);
      map.put(kv.getKey(), kv.getValue());
    }
    return map;
  }
}