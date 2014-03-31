package com.example.kennai;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * 中心地点を変化するためのアクティビティ。中心地点の住所の記録を書き込んで、
 * 記録のリストから住所を選ぶことも可能。
 */
public class CenterCord extends ListActivity {
	//中心地点の記録
	private ArrayList<MarkerOptions> searchHistory = new ArrayList<MarkerOptions>();

	//ユーザーが見ることのできるリスト
	private static ArrayList<String> disp = new ArrayList<String>(); 

	//dispをアップデートするAdapter
	private ArrayAdapter<String> adapter;

	//書き込まれるテキストファイルのタグ
	private static final String TAG = CenterCord.class.getName();

	//書き込まれるテキストファイルの名前
	private static final String CENTER_CORD_FILE = "centercord.txt";

	//住所を探すための変数
	private Geocoder geocoder;

	//設定のアクティビティに「マップのリセット」を押すとdispもリセットする
	private static boolean reset = false; 



	/** 
	 * クラス変数をインスタンス化して、記録を読み込み、
	 * ユーザーインタフェースの要素をインスタンス化する
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 * @param savedInstanceState  現在のBundle
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.set_center);

		//クラス変数をインスタンス化する
		geocoder = new Geocoder(this);
		MapsInitializer.initialize(getApplicationContext());
		adapter=new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1,
				disp);			
		setListAdapter(adapter);
		final EditText et = (EditText) findViewById(R.id.search_address);
		final Button b = (Button) findViewById(R.id.search_button);
		final Button cl = (Button) findViewById(R.id.current_location);
		final ListView lv = (ListView) findViewById(android.R.id.list);


		//テキストファイルを読み込む
		searchHistory = parseString(readFromFile());
		writeToFile("");
		disp.clear();
		for(MarkerOptions m : searchHistory) {
			disp.add(m.getSnippet());
		}
		if(reset)
		{
			disp.clear();
			searchHistory.clear();
			reset = false;
		}

		//ユーザーインタフェースの要素をインスタンス化する
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
				setCenterFromList(position);
				Toast.makeText(view.getContext(),getString(R.string.center_point_changed), Toast.LENGTH_SHORT).show();
			}
		});

		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				InputMethodManager imm = (InputMethodManager)getSystemService(
						Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
				setAddress(et.getText().toString());
				et.setText("");

				
			}
		});

		cl.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				InputMethodManager imm = (InputMethodManager)getSystemService(
						Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
				et.setText("");

				setCenterFromCurrentLocation();
			}
		});

	}
	
	/** 
	 * 中心地点の記録をテキストファイルに書き込む
	 * @see android.app.Activity#onStop()
	 */
	@Override
	public void onStop() {
		super.onStop();
		for(int i = searchHistory.size()-1;i>=0;i--) {
			writeToFile(String.valueOf(searchHistory.get(i).getPosition().latitude) + " " + 
					String.valueOf(searchHistory.get(i).getPosition().longitude) + " " + 
					searchHistory.get(i).getSnippet() + "||" + readFromFile());
		}
	}
	
	/**
	 * 中心地点に現在地を格納する
	 */
	public void setCenterFromCurrentLocation() {
		if(MapOptions.getCurrentLocation() != null) {
			LatLng pos = new LatLng(MapOptions.getCurrentLocation().getLatitude(),MapOptions.getCurrentLocation().getLongitude());
			MapOptions.setCenterPoint(new MarkerOptions()
			.title(getString(R.string.center))
			.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
			.position(pos)
			.snippet(getString(R.string.current_loc)+": (" + String.valueOf(Math.round(pos.latitude*1000.0)/1000.0) + ", " + 
					String.valueOf(Math.round(pos.longitude*1000.0)/1000.0) +")"));
			MapOptions.setCenterCircle(MapOptions.getCenterCircle().center(pos));
			//同じ現在地はリストに一回だけ入っているかチェックをする
			for(int i=0; i<searchHistory.size(); i++) {
				if(searchHistory.get(i).getSnippet().contains(getString(R.string.current_loc)+": (")) {
					double histLat = Math.round(searchHistory.get(i).getPosition().latitude*1000.0)/1000.0;
					double histLon = Math.round(searchHistory.get(i).getPosition().longitude*1000.0)/1000.0;
					double currentLat = Math.round(MapOptions.getCenterPoint().getPosition().latitude*1000.0)/1000.0;
					double currentLon = Math.round(MapOptions.getCenterPoint().getPosition().longitude*1000.0)/1000.0;
					if(histLat == currentLat && histLon == currentLon) {
						searchHistory.remove(i);
						disp.remove(i);
						adapter.notifyDataSetChanged();
						adapter.notifyDataSetInvalidated();
					}
				}
			}
			searchHistory.add(0,MapOptions.getCenterPoint());
			disp.add(0,MapOptions.getCenterPoint().getSnippet());


			adapter.notifyDataSetChanged();
			Toast.makeText(this, getString(R.string.center_point_changed), Toast.LENGTH_SHORT).show();
			if(!MapOptions.getCenterPoint().isVisible()) {
				MapOptions.getCenterPoint().visible(true);
			}
			if(!MapOptions.getCenterCircle().isVisible()) {
				MapOptions.getCenterCircle().visible(true);
			}
		}
	}

	/**
	 * EditTextに入力された住所をdispとMapOptionsのpointsに追加する
	 * @param input　入力された住所
	 */
	private void setAddress(String input){	
		if(input != null) {
			try {
				List<Address> addresses = geocoder.getFromLocationName(input,5);
				if(addresses != null && addresses.size() > 0) {
					MapOptions.setCenterPoint(new MarkerOptions()
					.title(getString(R.string.center))
					.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
					.position(new LatLng(addresses.get(0).getLatitude(),addresses.get(0).getLongitude()))
					.snippet(addresses.get(0).getAddressLine(0) + " " + addresses.get(0).getAddressLine(1)));
					MapOptions.setCenterCircle(MapOptions.getCenterCircle().center(
							new LatLng(addresses.get(0).getLatitude(),addresses.get(0).getLongitude())));


					for(int i=0; i<searchHistory.size(); i++)
					{
						if(searchHistory.get(i).getSnippet().equals(MapOptions.getCenterPoint().getSnippet())) {
							searchHistory.remove(i);
							disp.remove(i);
							adapter.notifyDataSetChanged();
						}
					}

					searchHistory.add(0,MapOptions.getCenterPoint());
					disp.add(0,MapOptions.getCenterPoint().getSnippet());

					if(searchHistory.size() >= 20) {
						searchHistory.remove(searchHistory.size()-1);
						disp.remove(searchHistory.size()-1);
					}
					adapter.notifyDataSetChanged();


					Toast.makeText(this,getString(R.string.center_point_changed), Toast.LENGTH_SHORT).show();
				}
				else
					Toast.makeText(this, getString(R.string.address_not_found), Toast.LENGTH_SHORT).show();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(!MapOptions.getCenterPoint().isVisible()) {
			MapOptions.getCenterPoint().visible(true);
		}
		if(!MapOptions.getCenterCircle().isVisible()) {
			MapOptions.getCenterCircle().visible(true);
		}

	}

	/**
	 * 中心の記録のリストから選ばれた住所を中心地点に格納する
	 * @param position 選ばれた住所のインデックス
	 */
	private void setCenterFromList(int position) {

		MarkerOptions newCenter = searchHistory.get(position);

		searchHistory.remove(position);
		searchHistory.add(0,newCenter);
		disp.clear();
		for(MarkerOptions m : searchHistory) {
			disp.add(m.getSnippet());
		}
		adapter.notifyDataSetInvalidated();
		adapter.notifyDataSetChanged();

		MapOptions.setCenterPoint(searchHistory.get(0));
		MapOptions.setCenterCircle(MapOptions.getCenterCircle().center(
				new LatLng(searchHistory.get(0).getPosition().latitude,searchHistory.get(0).getPosition().longitude)));
		if(!MapOptions.getCenterPoint().isVisible()) {
			MapOptions.getCenterPoint().visible(true);
		}
		if(!MapOptions.getCenterCircle().isVisible()) {
			MapOptions.getCenterCircle().visible(true);
		}
	}
	
	/**
	 * dataをテキストファイルに書き込む
	 * @param data  書き込まれる文字列
	 */
	private void writeToFile(String data) {
		try {
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(openFileOutput(CENTER_CORD_FILE, Context.MODE_PRIVATE));
			outputStreamWriter.write(data);
			outputStreamWriter.close();
		}
		catch (IOException e) {
			Log.e(TAG, "File write failed: " + e.toString());
		} 

	}


	/**
	 * テキストファイルから読み込む
	 * @return　読み込まれた文字列
	 */
	private String readFromFile() {

		String ret = "";

		try {
			InputStream inputStream = openFileInput(CENTER_CORD_FILE);

			if ( inputStream != null ) {
				InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
				String receiveString = "";
				StringBuilder stringBuilder = new StringBuilder();

				while ( (receiveString = bufferedReader.readLine()) != null ) {
					stringBuilder.append(receiveString);
				}

				inputStream.close();
				ret = stringBuilder.toString();
			}
		}
		catch (FileNotFoundException e) {
			Log.e(TAG, "File not found: " + e.toString());
		} catch (IOException e) {
			Log.e(TAG, "Can not read file: " + e.toString());
		}

		return ret;
	}

	/**
	 * 読み込まれた文字列を解析する
	 * 
	 * @param text　読み込まれた文字列
	 * @return 読み込まれた地点の記録のリスト
	 */
	private ArrayList<MarkerOptions> parseString (String text) {
		ArrayList<MarkerOptions> centerHistory= new ArrayList<MarkerOptions>();
		double lat,lon;
		String snippit= "";
		LatLng ltng;
		while(text.indexOf("||") != -1)
		{
			lat = Double.parseDouble(text.substring(0,text.indexOf(" ")));
			text = text.substring(text.indexOf(" ")+1);
			lon = Double.parseDouble(text.substring(0,text.indexOf(" ")));
			text = text.substring(text.indexOf(" ")+1);
			ltng = new LatLng(lat,lon);
			snippit = text.substring(0,text.indexOf("||")) + " ";
			text = text.substring(text.indexOf("||") + 2);
			centerHistory.add(new MarkerOptions()
			.title(getString(R.string.center))
			.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
			.position(ltng)
			.snippet(snippit.trim()));
			lat = 0;
			lon = 0;
			snippit = "";

		}
		return centerHistory;
	}

	/**
	 * dispをリセットするためのブーリアンをアップデート
	 * @param newReset　新規のリセットのブーリアン
	 */
	public static void setReset(boolean newReset){
		reset = newReset;
	}

}