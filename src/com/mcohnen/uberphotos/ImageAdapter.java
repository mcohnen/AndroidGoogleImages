package com.mcohnen.uberphotos;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

public class ImageAdapter extends ArrayAdapter<JSONObject> {
	public boolean canShowMore = true;
	
	public ImageAdapter(Context context) {
		super(context, 0);
	}
	
	@Override
	public int getViewTypeCount() {
		return 1;
	}
	
	// 0: imageView
	// 1: loaderView
	@Override
	public int getItemViewType(int position) {
		if (position < super.getCount()) {
			return 0;
		}
		return 1;
	}
	
	// Add a row of items to act as loader at the bottom
	@Override
	public int getCount() {
		int ret = super.getCount();
		if (canShowMore && ret > 0) {
			ret += getContext().getResources().getInteger(R.integer.num_columns);
		}
		return ret;
	}
	
	private View getLoaderView(View convertView) {
		if (convertView == null) {
			convertView = new EmptyView(getContext());
			convertView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.MATCH_PARENT));
		}
		return convertView;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (getItemViewType(position) > 0) {
			return getLoaderView(convertView);
		}
		ImageView imageView = (ImageView) convertView;
		if (imageView == null) {
			imageView = new SquareImageView(getContext());
			imageView.setScaleType(ScaleType.CENTER_CROP);
			imageView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.MATCH_PARENT));
		}
		JSONObject json = getItem(position);
		String url = json.optString("tbUrl");
		// If we are loading the same image, do not try to reload it
		if (imageView.getTag() != null && url.equals(imageView.getTag())) {
			return imageView;
		}
		imageView.setTag(url);
		ImageLoader.getInstance().cancelDisplayTask(imageView);
		DisplayImageOptions options = new DisplayImageOptions.Builder().showImageForEmptyUri(R.drawable.placeholder)
				.showStubImage(R.drawable.placeholder).cacheInMemory().cacheOnDisc().build();
		ImageLoader.getInstance().displayImage(url, imageView, options);
		
		return imageView;
	}
	
	// Helper to deal with JSONArray
	public void addJSONArray(JSONArray array) {
		ArrayList<JSONObject> list = new ArrayList<JSONObject>(array.length());
		if (array != null) {
			int len = array.length();
			for (int i = 0; i < len; i++) {
				try {
					list.add(array.getJSONObject(i));
				} catch (JSONException e) {
					
				}
			}
		}
		addAll(list);
	}
	
	private static class SquareImageView extends ImageView {

		public SquareImageView(Context context) {
			super(context);
		}
		
		@Override
		public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		    super.onMeasure(widthMeasureSpec, widthMeasureSpec);
		}
	}
	
	// View that is positioned at the bottom when there is more stuff to load
	private static class EmptyView extends View {
		
		public EmptyView(Context context) {
			super(context);
			setBackgroundColor(context.getResources().getColor(android.R.color.darker_gray));
		}

		@Override
		public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		    super.onMeasure(widthMeasureSpec, widthMeasureSpec);
		}
	}

}
