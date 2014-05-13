package com.example.kennai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
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
 * マップに地点を追加／削除するためのクラス。
 */
public class AddPoints extends ListActivity {
	//ユーザーが見ることのできるリスト
	private static ArrayList<String> disp = new ArrayList<String>();
	
	//dispをアップデートするAdapter
	private ArrayAdapter<String> adapter;
	
	//住所を探すための変数
	private Geocoder geocoder; 
	
	//設定のアクティビティに「マップのリセット」を押すとdispもリセットする
	private static boolean reset; 

	//一番最初にMainActivityを実行する時だけdispをリセットする
	private boolean firstTimeOnly = true;
	
	/**
	 * AddPointsを実行すると、クラス変数をインスタンス化して、
	 * ユーザーインタフェースの要素をインスタンス化する
	 * 
	 * @param savedInstanceState  現在のBundle
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_point);
		
		//変数をインスタンス化する
		adapter=new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1,
				disp);			
		setListAdapter(adapter);
		geocoder = new Geocoder(this);
		MapsInitializer.initialize(getApplicationContext());
		final EditText searchAddressEditText = (EditText) findViewById(R.id.search_add_address);
		final Button addAddressButton = (Button) findViewById(R.id.add_address);
		final ListView pointsListView = (ListView) findViewById(android.R.id.list);
		
		//一番最初にMainActivityを実行する時だけdispをリセットする
		if(firstTimeOnly) {
			disp.clear();
			for(MarkerOptions m : MapOptions.getPoints()) {
				disp.add(m.getTitle());
			}
			firstTimeOnly = false;
		}
		
		//設定でリセットボタンを押したら記録をリセットする
		if(reset) {
			disp.clear();
			reset = false;
		}
		
		//リストに入っている地点を長押ししたら、その地点を削除するかどうかのダイアログボックスを表示する
		pointsListView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				removeItemFromList(position);
				return true;
			}
		});
		
		//入力された住所を追加するためのButton
		addAddressButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				String text=searchAddressEditText.getText().toString();
				InputMethodManager imm = (InputMethodManager)getSystemService(
						Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(searchAddressEditText.getWindowToken(), 0);
				addAddress(text);
				searchAddressEditText.setText("");
				searchAddressEditText.setHint(getString(R.string.add_point));
				adapter.notifyDataSetChanged();
			}
		});
	}
	

	/**
	 * dispとMapOptionsのpointsから入力されたインデックスの地点を削除する
	 * @param position 削除される地点のインデックス
	 */
	private void removeItemFromList(int position) {
		final int deletePosition = position;

		//ダイアログボックスを作成して表示する
		AlertDialog.Builder alert = new AlertDialog.Builder(AddPoints.this);
		alert.setTitle(getString(R.string.delete));
		alert.setMessage(getString(R.string.do_you_want_to_delete));
		
		//「はい」を押すと削除する
		alert.setPositiveButton(getString(R.string.yes), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				disp.remove(deletePosition);
				MapOptions.removePoint(deletePosition);
				adapter.notifyDataSetChanged();
			}
		});
		//ダイアログボックスを閉じる
		alert.setNegativeButton(getString(R.string.cancel), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		
		alert.show();

	}

	/**
	 * EditTextに入力された住所をdispとMapOptionsのpointsに追加する
	 * @param input　入力された住所
	 */
	private void addAddress(String input){
		if(input != null && !input.isEmpty()) {
			try {
				List<Address> addresses = geocoder.getFromLocationName(input,5);
				if(addresses != null && addresses.size() > 0) {
					LatLng np = new LatLng(addresses.get(0).getLatitude(),addresses.get(0).getLongitude());
					boolean newPoint = true;
					
					//同じ住所はリストに一回だけ入っているかチェックをする
					for(int i=0; i<MapOptions.getPoints().size(); i++) {
						if(MapOptions.getPoints().get(i).getTitle().equals(String.valueOf(addresses.get(0).getAddressLine(0)))) {
							double currentPointLat = Math.round(MapOptions.getPoints().get(i).getPosition().latitude*1000.0)/1000.0;
							double currentPointLon = Math.round(MapOptions.getPoints().get(i).getPosition().longitude*1000.0)/1000.0;
							double currentLat = Math.round(np.latitude*1000.0)/1000.0;
							double currentLon = Math.round(np.longitude*1000.0)/1000.0;
							
							if(currentPointLat == currentLat && currentPointLon == currentLon) {
								Toast.makeText(this, getString(R.string.point_already_in_list), Toast.LENGTH_SHORT).show();
								MapOptions.removePoint(i);
								disp.remove(i);
								adapter.notifyDataSetChanged();
								newPoint=false;
							}
						}
					}

					MapOptions.addPoint(new MarkerOptions()
					.position(np)
					.title(String.valueOf(addresses.get(0).getAddressLine(0)))
					.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)),getApplicationContext());
					disp.add(0,MapOptions.getMostRecentPoint().getTitle());
					if(newPoint)
						Toast.makeText(this, getString(R.string.point_added), Toast.LENGTH_SHORT).show();
				}
				else
					Toast.makeText(this, getString(R.string.address_not_found), Toast.LENGTH_SHORT).show();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * dispをリセットするためのブーリアンをアップデート
	 * @param newReset　新規のリセットのブーリアン
	 */
	public static void setReset(boolean newReset) {
		reset = newReset;
		disp.clear();
	}
}
