package com.mcohnen.uberphotos;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

public class MainActivity extends Activity {
	public final static String TAG = "MainActivity";
	public static final String PREFS_NAME = "Searches";
	private final int PER_PAGE = 6;

	private GridView gridView;
	private AutoCompleteTextView editText;
	private ImageAdapter adapter;
	private ArrayAdapter<String> autocompleteAdapter;
	private int start;
	private int numColumns;
	private boolean searching;
	private SortedSet<String> queries;

	// This should probably go in a custom Application class, but I add it here for simplicity
	public void initImageLoader() {
		if (!ImageLoader.getInstance().isInited()) {
			DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder().cacheInMemory().cacheOnDisc().build();
			ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext()).defaultDisplayImageOptions(
					defaultOptions).build();
			ImageLoader.getInstance().init(config);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initImageLoader();

		setContentView(R.layout.activity_main);

		numColumns = getResources().getInteger(R.integer.num_columns);

		gridView = (GridView) findViewById(R.id.gridView);
		editText = (AutoCompleteTextView) findViewById(R.id.editText);
		
		autocompleteAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		loadSearches();
		editText.setAdapter(autocompleteAdapter);
		
		adapter = new ImageAdapter(this);
		gridView.setAdapter(adapter);

		gridView.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {

			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				// If true, we are seeing the loader cells, so try to load more items
				if (totalItemCount > 0 && totalItemCount < firstVisibleItem + visibleItemCount + numColumns) {
					doSearch();
				}
			}
		});

		editText.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String q = autocompleteAdapter.getItem(position);
				editText.setText(q);
				doSearchClear();
			}
		});
		editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEARCH) {
					doSearchClear();
					return true;
				}
				return false;
			}
		});
	}
	
	// Hide keyboard and do a new search
	public void doSearchClear() {
		InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(editText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
		editText.clearFocus();
		clearSearch();
		saveSearch(editText.getText().toString());
		doSearch();
	}

	// Clear current search: request, offset, adapter
	public void clearSearch() {
		HttpClient.getInstance().cancelRequests(this, true);
		searching = false;
		start = 0;
		adapter.clear();
	}

	// helper to get IP Address
	public String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex) {
			Log.e(TAG, ex.toString());
		}
		return "";
	}
	
	// Load prev searches into mem
	public void loadSearches() {
		SharedPreferences prefs = getSharedPreferences(PREFS_NAME, 0);
		queries = new TreeSet<String>(prefs.getStringSet("queries", new TreeSet<String>()));
		updateAutocompleteAdapter();
	}
	
	// Update autocomplete adapter with searches
	public void updateAutocompleteAdapter() {
		autocompleteAdapter.clear();
		autocompleteAdapter.addAll(queries);
		autocompleteAdapter.notifyDataSetChanged();
	}

	// Save search to preferences
	public void saveSearch(String q) {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		queries.add(q);
		editor.putStringSet("queries", queries);
		editor.commit();
		updateAutocompleteAdapter();
	}

	// Do request. Only 1 at a time
	public void doSearch() {
		if (searching || (!adapter.canShowMore && start != 0)) {
			return;
		}
		searching = true;
		RequestParams params = new RequestParams();
		params.put("v", "1.0");
		params.put("q", editText.getText().toString());
		params.put("start", Integer.toString(start));
		params.put("imgsz", "medium");
		params.put("userip", getLocalIpAddress());
		params.put("rsz", "" + PER_PAGE);
		HttpClient.getInstance().get("https://ajax.googleapis.com/ajax/services/search/images", params, new JsonHttpResponseHandler() {
			@Override
			public void onSuccess(int statusCode, JSONObject response) {
				searching = false;
				try {
					JSONObject responseData = response.getJSONObject("responseData");
					JSONArray results = responseData.getJSONArray("results");
					if (results.length() == 0) {
						Toast.makeText(MainActivity.this, "No results", Toast.LENGTH_LONG).show();
						return;
					}
					JSONObject cursor = responseData.getJSONObject("cursor");
					JSONArray pages = cursor.getJSONArray("pages");
					int currPage = cursor.getInt("currentPageIndex");
					if (currPage >= pages.length() - 1) {
						// last page
						Toast.makeText(MainActivity.this, "NO SHOW MORE", Toast.LENGTH_LONG).show();
						adapter.canShowMore = false;
					} else {
						start = pages.getJSONObject(currPage + 1).getInt("start");
						adapter.canShowMore = true;
					}

					adapter.addJSONArray(results);
					adapter.notifyDataSetChanged();

				} catch (JSONException e) {
					// Sometimes google API returns a usage error
					String details = response.optString("responseDetails");
					int status = response.optInt("responseStatus");
					if (status == 503) {
						// retry in 1 sec
						new Timer().schedule(new TimerTask() {
							@Override
							public void run() {
								doSearch();
							}
						}, 1000);
					}
					Toast.makeText(MainActivity.this, details != null ? details : "json is not valid", Toast.LENGTH_LONG).show();
				}

			}

			@Override
			public void onFailure(Throwable arg0, String arg1) {
				searching = false;
			}
		});
	}

}
