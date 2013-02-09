package org.ros.android.map_manager;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

public class MapListArrayAdapter extends ArrayAdapter<MapListData> {

	private class ViewHolder {
		TextView textView;
		RadioButton radioButton;
	}

	private OnTouchListener listener;
	private List<MapListData> mapList = null;
	private LayoutInflater inflator;
	private MainActivity context;

	public MapListArrayAdapter(MainActivity context, int resourceId,
			List<MapListData> mapList, OnTouchListener listener) {
		super(context, resourceId, mapList);
		this.listener = listener;
		this.mapList = mapList;
		this.context = context;

	}

	@Override
	public MapListData getItem(int position) {
		return mapList.get(position);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;

		if (convertView == null) {
			inflator = (LayoutInflater) getContext().getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflator.inflate(R.layout.map_list_item, null);
			viewHolder = new ViewHolder();
			viewHolder.textView = (TextView) convertView
					.findViewById(R.id.map_list_text);
			viewHolder.radioButton = (RadioButton) convertView
					.findViewById(R.id.map_list_button);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		final MapListData mapListData = (MapListData) getItem(position);
		viewHolder.textView.setText(mapListData.getText());
		viewHolder.radioButton.setChecked(mapListData.isChecked());
		viewHolder.radioButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				for (int i = 0; i < mapList.size(); i++) {
					mapList.get(i).setChecked(false);
				}
				mapListData.setChecked(true);
				notifyDataSetChanged();
				context.updateMapView(mapListData.getId());

			}

		});
		convertView.setId(position);
		convertView.setOnTouchListener(listener);
		return convertView;
	}

}
