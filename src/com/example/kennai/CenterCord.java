package com.example.kennai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
 * 中心地点を変更するためのアクティビティ。中心地点の住所の記録を書き込んで、
 * 記録のリストから住所を選ぶことも可能。
 */
public class CenterCord extends ListActivity {
	//中心地点の記録
	private ArrayList<MarkerOptions> searchHistory = new ArrayList<MarkerOptions>();

	//ユーザーが見ることのできるリスト
	private static ArrayList<String> disp = new ArrayList<String>(); 

	//dispをアップデートするAdapter
	private ArrayAdapter<String> adapter;

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
		
		//ユーザインタフェースの変数をインスタンス化する
		final EditText searchAddressEditText = (EditText) findViewById(R.id.search_address);
		final Button searchButton = (Button) findViewById(R.id.search_button);
		final Button currentLocationButton = (Button) findViewById(R.id.current_location);
		final ListView centerCordHistoryListView = (ListView) findViewById(android.R.id.list);


		//記録を読み込む
		loadSavedHistory();
		
		//表示されている記録をクリアして、記録から読み込んで、表示する
		disp.clear();
		for(MarkerOptions m : searchHistory) {
			disp.add(m.getSnippet());
		}
		
		//設定でリセットボタンを押したら記録をリセットする
		if(reset) {
			disp.clear();
			searchHistory.clear();
			reset = false;
		}

		
		//中心地点を変更するためのButtonのListenerをインスタンス化する
		centerCordHistoryListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
				setCenterFromList(position);
				Toast.makeText(view.getContext(),getString(R.string.center_point_changed), Toast.LENGTH_SHORT).show();
			}
		});

		//入力された住所を検索するためのButton
		searchButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				InputMethodManager imm = (InputMethodManager)getSystemService(
						Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(searchAddressEditText.getWindowToken(), 0);
				setAddress(searchAddressEditText.getText().toString());
				searchAddressEditText.setText("");
			}
		});

		//中心地点の記録のリストから選ばれた住所を中心地点に格納するためのButton
		currentLocationButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				InputMethodManager imm = (InputMethodManager)getSystemService(
						Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(searchAddressEditText.getWindowToken(), 0);
				searchAddressEditText.setText("");
				setCenterFromCurrentLocation();
			}
		});
	}

	/** 
	 * 中心地点の記録をSharedPreferencesに書き込む
	 * @see android.app.Activity#onStop()
	 */
	@Override
	public void onStop() {
		super.onStop();
		savePreferences(searchHistory);
	}

	/**
	 * 中心地点に現在地を格納する
	 */
	public void setCenterFromCurrentLocation() {
		if(MapOptions.getCurrentLocation() != null) {
			//格納されている現在地を読み込んで、中心地点に格納する
			LatLng pos = new LatLng(MapOptions.getCurrentLocation().getLatitude(),
					MapOptions.getCurrentLocation().getLongitude());
			MapOptions.setCenterPoint(new MarkerOptions()
			.title(getString(R.string.center))
			.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
			.position(pos)
			.snippet(getString(R.string.current_loc)+": (" + String.valueOf(Math.round(pos.latitude*1000.0)/1000.0) + ", " + 
					String.valueOf(Math.round(pos.longitude*1000.0)/1000.0) +")"));
			MapOptions.setCenterCircle(MapOptions.getCenterCircle().center(pos));
			
			//同じ現在地が重複していないかチェックをする
			LatLng centerPointPos = MapOptions.getCenterPoint().getPosition();
			for(int i=0; i<searchHistory.size(); i++) {
				MarkerOptions tempMO = searchHistory.get(i);
				if(tempMO.getSnippet().contains(getString(R.string.current_loc)+": (")) {
					double histLat = Math.round(tempMO.getPosition().latitude*1000.0)/1000.0;
					double histLon = Math.round(tempMO.getPosition().longitude*1000.0)/1000.0;
					double currentLat = Math.round(centerPointPos.latitude*1000.0)/1000.0;
					double currentLon = Math.round(centerPointPos.longitude*1000.0)/1000.0;
					if(histLat == currentLat && histLon == currentLon) {
						//重複した現在地を削除する
						searchHistory.remove(i);
						disp.remove(i);
						adapter.notifyDataSetChanged();
						adapter.notifyDataSetInvalidated();
					}
				}
			}
			
			//searchHistoryとdispに格納して、ユーザーに知らせる
			searchHistory.add(0,MapOptions.getCenterPoint());
			disp.add(0,MapOptions.getCenterPoint().getSnippet());
			adapter.notifyDataSetChanged();
			Toast.makeText(this, getString(R.string.center_point_changed), Toast.LENGTH_SHORT).show();
			
			//中心地点と円を表示する
			MapOptions.getCenterPoint().visible(true);
			MapOptions.getCenterCircle().visible(true);
		}
	}

	/**
	 * EditTextに入力された住所をdispとMapOptionsのpointsに追加する
	 * @param input　入力された住所
	 */
	private void setAddress(String input){	
		if(input != null && !input.isEmpty()) {
			try {
				List<Address> addresses = geocoder.getFromLocationName(input,5);
				if(addresses != null && addresses.size() > 0) {
					//中心地点と円をアップデートする
					Address address = addresses.get(0);
					MapOptions.setCenterPoint(new MarkerOptions()
					.title(getString(R.string.center))
					.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
					.position(new LatLng(address.getLatitude(),address.getLongitude()))
					.snippet(address.getAddressLine(0) + " " + address.getAddressLine(1)));
					MapOptions.setCenterCircle(MapOptions.getCenterCircle().center(
							MapOptions.getCenterPoint().getPosition()));
					
					//入力された住所がすでにdispに入っていたら、重複した要素を削除する
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

					//記録が長くなり過ぎないように２０個以上あったら一番古い要素を削除する
					if(searchHistory.size() >= 20) {
						searchHistory.remove(searchHistory.size()-1);
						disp.remove(searchHistory.size()-1);
					}
					adapter.notifyDataSetChanged();
				}
				else
					Toast.makeText(this, getString(R.string.address_not_found), Toast.LENGTH_SHORT).show();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		MapOptions.getCenterPoint().visible(true);
		MapOptions.getCenterCircle().visible(true);

	}

	
	/**
	 * 中心の記録のリストから選ばれた住所を中心地点に格納する
	 * @param position 選ばれた住所のインデックス
	 */
	private void setCenterFromList(int position) {
		MarkerOptions newCenter = searchHistory.get(position);
		
		//選ばれた地点のインデックスに０を格納する
		searchHistory.remove(position);
		searchHistory.add(0,newCenter);
		
		//表示されている地点をリロードする
		disp.clear();
		for(MarkerOptions m : searchHistory) {
			disp.add(m.getSnippet());
		}
		adapter.notifyDataSetInvalidated();
		adapter.notifyDataSetChanged();

		//選ばれた地点をMapOptionsに格納する
		MapOptions.setCenterPoint(searchHistory.get(0).visible(true));
		MapOptions.setCenterCircle(MapOptions.getCenterCircle().center(
				MapOptions.getCenterPoint().getPosition()).visible(true));
	}

	
	/**
	 * dispをリセットするためのブーリアンをアップデート
	 * @param newReset　新規のリセットのブーリアン
	 */
	public static void setReset(boolean newReset){
		reset = newReset;
	}
	
	/**
	 * 現在の利用者選考を保存する
	 * @param hist 保存するリスト
	 */

	private void savePreferences(final ArrayList<MarkerOptions> hist) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString("historySize",String.valueOf(hist.size()));
		
		//中心地点の記録のリストを読み込む
		for(Integer i = 0; i < hist.size(); i++) {
			editor.putString(i+"_SearchHistorySnipp", hist.get(i).getSnippet());
			editor.putString(i+"_SearchHistoryLat", String.valueOf(searchHistory.get(i).getPosition().latitude));
			editor.putString(i+"_SearchHistoryLon", String.valueOf(searchHistory.get(i).getPosition().longitude));
		}
		editor.commit();
		System.out.println("SAVED CENTER HISTORY");
		System.out.println(sharedPreferences.getAll());
	}
	
	/**
	 * 保存されている利用者選考を読み込む
	 */

	private void loadSavedHistory() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		String str = sharedPreferences.getString("historySize", "0");
		int histSize = Integer.parseInt(str);
		MarkerOptions m;
		double lat,lng=0;
		
		//保存されている中心地点の記録のリスト
		for(Integer i = 0; i<histSize; i++) {
			lat = Double.parseDouble(sharedPreferences.getString(String.valueOf(i) + "_SearchHistoryLat","0.0"));
			lng = Double.parseDouble(sharedPreferences.getString(String.valueOf(i) + "_SearchHistoryLon","0.0"));
			String snippet = sharedPreferences.getString(i + "_SearchHistorySnipp","");
			m = new MarkerOptions();
			m.position(new LatLng(lat,lng));
			m.snippet(snippet);
			m.title(getString(R.string.center));
			m.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
			searchHistory.add(m);
		}
	}


}
