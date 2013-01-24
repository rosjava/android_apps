package org.ros.android.android_app_chooser;

import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import app_manager.App;
import java.util.ArrayList;

import org.jboss.netty.buffer.ChannelBuffer;

public class AppAdapter extends BaseAdapter {
  private Context context;
  private ArrayList<App> apps;
  private ArrayList<App> runningApps;

  public AppAdapter(Context c, ArrayList<App> apps, ArrayList<App> runningApps) {
    context = c;
    this.apps = apps;
    this.runningApps = runningApps;
  }

  @Override
  public int getCount() {
    if (apps == null) {
      return 0;
    }
    return apps.size();
  }

  @Override
  public Object getItem(int position) {
    return null;
  }

  @Override
  public long getItemId(int position) {
    return 0;
  }

  boolean isAppRunning(App app) {
    if (app.getName() == null) {
      return false;
    }
    for (App c : this.runningApps) {
      if (app.getName().equals(c.getName())) {
        return true;
      }
    }
    return false;
  }

  /**
* Create a new View for each item referenced by the Adapter.
*/
  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    View view = inflater.inflate(R.layout.app_item, null);
    App app = apps.get(position);
    if( app.getIcon().getData().array().length > 0 && app.getIcon().getFormat() != null &&
        (app.getIcon().getFormat().equals("jpeg") || app.getIcon().getFormat().equals("png")) ) {
    	ChannelBuffer buffer = app.getIcon().getData();
    	Bitmap iconBitmap = BitmapFactory.decodeByteArray( app.getIcon().getData().array(), buffer.arrayOffset(), buffer.readableBytes());

      if( iconBitmap != null ) {
        ImageView iv = (ImageView) view.findViewById(R.id.icon);
        iv.setImageBitmap(iconBitmap);
      }
    }
    TextView tv = (TextView) view.findViewById(R.id.name);
    tv.setText(app.getDisplayName());
    if (isAppRunning(app)) {
      view.setBackgroundResource(R.drawable.highlight);
    } else {
      view.setBackgroundResource(0);
    }
    return view;
  }
}