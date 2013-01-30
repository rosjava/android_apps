package org.ros.android.android_app_chooser;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import org.ros.android.robotapp.RobotDescription;
import java.util.ArrayList;
import java.util.List;
/**
 * @author hersh@willowgarage.com
 */
public class MasterAdapter extends BaseAdapter {
  private Context context;
  private List<MasterItem> masterItems;
  public MasterAdapter(RobotMasterChooser rmc, List<RobotDescription> robots) {
    context = rmc;
    masterItems = new ArrayList<MasterItem>();
    if (robots != null) {
      for (int i = 0; i < robots.size(); i++) {
        masterItems.add(new MasterItem(robots.get(i), rmc));

      }
    }
  }
  @Override
  public int getCount() {
    if (masterItems == null) {
      return 0;
    }
    return masterItems.size();
  }
  @Override
  public Object getItem(int position) {
    return null;
  }
  @Override
  public long getItemId(int position) {
    return 0;
  }
  // create a new View for each item referenced by the Adapter
  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    return masterItems.get(position).getView(context, convertView, parent);
  }
}