/*
* Software License Agreement (BSD License)
*
* Copyright (c) 2011, Willow Garage, Inc.
* All rights reserved.
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
* * Redistributions of source code must retain the above copyright
* notice, this list of conditions and the following disclaimer.
* * Redistributions in binary form must reproduce the above
* copyright notice, this list of conditions and the following
* disclaimer in the documentation and/or other materials provided
* with the distribution.
* * Neither the name of Willow Garage, Inc. nor the names of its
* contributors may be used to endorse or promote products derived
* from this software without specific prior written permission.
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

import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Context;
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