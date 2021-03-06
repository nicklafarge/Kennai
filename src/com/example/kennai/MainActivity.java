package com.example.kennai;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

/**
 * Kennaiで一番最初に実行するアクティビティ。マップと中心地点と円がインスタンス化される。
 */
public class MainActivity extends Activity {
	//マップ
	private GoogleMap map;

	//円
	private Circle centerCircle;

	//ニコちゃんモード
	private boolean smiley;

	//書き込まれるテキストファイルの名前
	private static final String SETTINGS_FILE = "settings.txt";

	//書き込まれるテキストファイルのタグ
	private static final String TAG = Settings.class.getName();

	//一番最初にMainActivityを実行する時だけテキストファイルを読み込む
	private boolean onFirstRunOne = true;

	//一番最初にMainActivityを実行する時だけマップのズームをアップデートしない
	private boolean notOnFirstRun = false;

	//マップをリセットする
	private static boolean reset = false;

	/** 
	 * クラス変数をインスタンス化して、一番最初だけ記録を読み込む
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 * @param savedInstanceState  現在のBundle
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		smiley=MapOptions.isSmileyMode();
		MapsInitializer.initialize(getApplicationContext());
		
		//一番最初に実行する時だけ設定の記録を読み込む
		if(onFirstRunOne) {
			loadSavedPreferences();
			if(MapOptions.getCenterPoint().getSnippet()=="")
				setUpOptions();
			onFirstRunOne = false;
		}
		setUpMapIfNeeded();
	}

	/** 
	 * マップに全ての地点と円を書く
	 * 
	 * @see android.app.Activity#onResume(android.os.Bundle);
	 */
	@Override
	protected void onResume() {
		super.onResume();
		map.setMapType(MapOptions.getMapType());
		map.clear();

		//設定でリセットボタンを押したら、マップのカメラをアップデートする
		if(reset) {
			writeToFile("");
			map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
					MapOptions.getCurrentLocation().getLatitude(),MapOptions.getCurrentLocation().getLongitude()), 1));
			reset = false;
		}


		//マップに全ての地点を追加する
		MapOptions.updatePointColors(getApplicationContext());
		for(MarkerOptions m:MapOptions.getPoints()) {
			map.addMarker(m);
		}

		if(MapOptions.getCenterPoint().isVisible()) { 
			//設定でニコちゃんモードがアップデートされたらここもアップデートされる
			if(smiley != MapOptions.isSmileyMode()) {
				smiley = MapOptions.isSmileyMode();
			}
			//中心地点と円を書く
			if(!MapOptions.isSmileyMode() || !qualifySmiley()) {
				if(MapOptions.getCenterPoint() != null) {
					map.addMarker(MapOptions.getCenterPoint());
					if(centerCircle != null && centerCircle.getRadius() != MapOptions.getCircleRadius() * 1000)
					{
						map.addMarker(MapOptions.getCenterPoint());
					}
					centerCircle = map.addCircle(MapOptions.getCenterCircle());
				}
			}
			//ニコちゃんを書く
			else if(MapOptions.isSmileyMode()) {
				drawSmiley();
			}
		}
		
		//一番最初に実行する時だけズームをアップデートしない
		if(notOnFirstRun) {
			updateZoom();
		}
		notOnFirstRun = true;
		
		//保存する
		saveInfo();
	}

	/** 
	 * MapOptionsに現在のマップを格納する
	 * 
	 * @see android.app.Activity#onPause(android.os.Bundle);
	 */
	@Override
	protected void onPause () {
		super.onPause();
		MapOptions.setMap(map);
	}

	/**
	 * メニューをインスタンス化する
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.os.Bundle);
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * メニューの右を押すと設定を実行する
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.os.Bundle);
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			Intent myIntent = new Intent(this, Settings.class);
			startActivityForResult(myIntent, 0);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * マップをリセットするためのブーリアンをアップデート
	 * @param newReset　新規のリセットのブーリアン
	 */
	public static void setReset(boolean newReset) {
		MainActivity.reset = newReset;
	}

	/**
	 * SharedPreferencesに全ての情報を書き込む
	 */
	private void saveInfo() {
		if(MapOptions.getCenterPoint().isVisible()) {
			savePreferences("mapType",MapOptions.getMapType());
			savePreferences("smileyMode",MapOptions.isSmileyMode());
			savePreferences("circleRadius",String.valueOf(MapOptions.getCircleRadius()));
			
			//中心地点の情報
			savePreferences("centerPointLat",String.valueOf(
					MapOptions.getCenterPoint().getPosition().latitude));
			savePreferences("centerPointLon",String.valueOf(
					MapOptions.getCenterPoint().getPosition().longitude));
			savePreferences("centerPointSnippit",MapOptions.getCenterPoint().getSnippet());
			savePreferences("centerPointTitle",MapOptions.getCenterPoint().getTitle());
			
			//地点のリストの情報
			savePreferences("pointsListSize",MapOptions.getPoints().size());
			ArrayList<MarkerOptions> pts = MapOptions.getPoints();
			for(int i=0; i<pts.size(); i++){
				savePreferences(i+"_PointListTitle", pts.get(i).getTitle());
				savePreferences(i+"_PointListLat", String.valueOf(pts.get(i).getPosition().latitude));
				savePreferences(i+"_PointListLon", String.valueOf(pts.get(i).getPosition().longitude));
			}
		}
	}

	/**
	 * 円の北と西の間にある四分円に地点が一つ、
	 * 円の北と東の間にある四分円に地点が一つ、
	 * 南半球に地点が二つから五つまである場合、
	 * ニコちゃんが可能です
	 * @return　ニコちゃんが可能かどうか
	 */
	private boolean qualifySmiley(){
		ArrayList<MarkerOptions> drawPoints = MapOptions.getInsidePoints();
		
		int topRight,topLeft,bottom;
		topRight = topLeft = bottom = 0;
		MarkerOptions cP = MapOptions.getCenterPoint();
		
		LatLng centerPosition = cP.getPosition();
		LatLng tempPosition;
		
		//四分円によって地点を数える
		for(MarkerOptions m:drawPoints) {
			tempPosition = m.getPosition();
			if(tempPosition.latitude > centerPosition.latitude &&
					tempPosition.longitude > centerPosition.longitude)
				topRight ++;
			else if(tempPosition.latitude > centerPosition.latitude &&
					tempPosition.longitude < centerPosition.longitude)
				topLeft ++;
			else
				bottom ++;
		}
		
		//ニコちゃんが可能かどうかをリターンする
		return topRight == 1 && topLeft == 1 && bottom >1 && bottom<6;
	}

	/**
	 * ニコちゃんを書く
	 */
	private void drawSmiley() {
		map.clear();
		Toast.makeText(this, getString(R.string.smile), Toast.LENGTH_SHORT).show();

		//中心地点
		BitmapDescriptor cBD = MapOptions.getCenterPoint().getIcon();
		MapOptions.setCenterPoint(MapOptions.getCenterPoint().icon(cBD));

		//円
		int cFC = MapOptions.getCenterCircle().getFillColor();
		centerCircle = map.addCircle(MapOptions.getCenterCircle().fillColor(0x95ffff00));
		MapOptions.setCenterCircle(MapOptions.getCenterCircle().fillColor(cFC));

		//他の地点
		BitmapDescriptor temp;
		int i = 0;
		for(MarkerOptions m : MapOptions.getPoints()) {
			temp = m.getIcon();
			map.addMarker(m.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
			MapOptions.getPoints().set(i, m.icon(temp));
			i++;
		}

		//南半球にある地点間に直線を書く
		PolylineOptions rectOptions = new PolylineOptions();
		for(MarkerOptions m : MapOptions.getSortedBottomPoints()) {
			rectOptions.add(m.getPosition());
		}
		rectOptions.color(0x998F00FF);
		rectOptions.width(10);
		map.addPolyline(rectOptions);
	}

	/**
	 * マップのズームをアップデートする
	 */
	private void updateZoom() {
		LatLngBounds.Builder bc = new LatLngBounds.Builder();
		CircleOptions centerCircle = MapOptions.getCenterCircle();

		//中心地点が見える場合は円にズームする
		if(MapOptions.getCenterPoint().isVisible()) {
			for(int i=0;i<4;i++) {
				bc.include(MapOptions.CalculateDerivedPosition(centerCircle.getCenter(),
						MapOptions.getCircleRadius()*1000,90*i));
			}
			map.moveCamera(CameraUpdateFactory.newLatLngBounds(bc.build(), 50));
		}
		
		//中心地点が見えなくて、他の地点がある場合はその地点にズームする
		else if(MapOptions.getPoints().size() > 0){
			for(MarkerOptions m : MapOptions.getPoints()) {
				bc.include(m.getPosition());
			}
			map.moveCamera(CameraUpdateFactory.newLatLngBounds(bc.build(), 50));
		}
	}

	/**
	 * マップをインスタンス化して、規定の設定を格納する
	 */
	private void setUpMapIfNeeded(){
		if (map == null) {
			map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
			if (map != null) {
				map.setMapType(MapOptions.getMapType());
				map.getUiSettings().setAllGesturesEnabled(true);
				map.setMyLocationEnabled(true);
				if(!MapOptions.getCenterPoint().isVisible()) {
					map.moveCamera(CameraUpdateFactory.newLatLngZoom(
							MapOptions.getCenterPoint().getPosition(), 1));
				}
				map.setOnCameraChangeListener(new OnCameraChangeListener() {
					@Override
					public void onCameraChange(CameraPosition arg0) {
						updateZoom();
						map.setOnCameraChangeListener(null);
					}
				});
			}
		}
	}

	/**
	 * 初期設定
	 * 中心地点は見えれなくて、黄色で（0,0）にある
	 * 円の半径は一キロで緑で中心地点のとこりにある
	 * ニコちゃんモードはオフ
	 * 
	 */
	private void setUpOptions() {
		//中心地点
		MapOptions.setCenterPoint(new MarkerOptions()
		.visible(false)
		.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
		.position(new LatLng(0,0)));
		//円
		MapOptions.setCenterCircle(new CircleOptions()
		.center(MapOptions.getCenterPoint().getPosition())
		.radius(MapOptions.getCircleRadius() * 1000.0)
		.fillColor(0x95c2ffc2)
		.strokeWidth(2)
		.visible(false));
		//ニコチャンモード
		MapOptions.setSmileyMode(false);
	}

	/**
	 * dataをテキストファイルに書き込む
	 * @param data  書き込まれる文字列
	 */
	private void writeToFile(String data) {
		try {
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(openFileOutput(SETTINGS_FILE, Context.MODE_PRIVATE));
			outputStreamWriter.write(data);
			outputStreamWriter.close();
		}
		catch (IOException e) {
			Log.e(TAG, "File write failed: " + e.toString());
		} 

	}

	/**
	 * 指定された整数を保存する
	 * @param hist 保存するリスト
	 */
	private void savePreferences(String key, int value) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putInt(key, value);
		editor.commit();
	}
	
	/**
	 * 指定されたStringを保存する
	 * @param hist 保存するリスト
	 */
	private void savePreferences(String key, String value) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString(key, value);
		editor.commit();
	}

	/**
	 * 指定されたBooleanを保存する
	 * @param hist 保存するリスト
	 */
	private void savePreferences(String key, Boolean value) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean(key, value);
		editor.commit();
	}
	
	/**
	 * 保存されている利用者選考を読み込む
	 */
	private void loadSavedPreferences() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		//マップの情報
		MapOptions.setMapType(sharedPreferences.getInt("mapType", 1));
		MapOptions.setSmileyMode(sharedPreferences.getBoolean("smileyMode", false));
		MapOptions.setCircleRadius(Double.parseDouble(sharedPreferences.getString("circleRadius", "1.0f")));
		
		//中心地点の情報
		double centerPointLat = Double.parseDouble(sharedPreferences.getString("centerPointLat", "0.0f"));
		double centerPointLon = Double.parseDouble(sharedPreferences.getString("centerPointLon", "0.0f"));
		String centerPointTitle = sharedPreferences.getString("centerPointTitle", "");
		String centerPointSnippit = sharedPreferences.getString("centerPointSnippit", "");

		//読み込んだ中心地点の情報を格納する
		MapOptions.setCenterPoint(new MarkerOptions()
		.position(new LatLng(centerPointLat,centerPointLon))
		.title(centerPointTitle)
		.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
		.visible(true)
		.snippet(centerPointSnippit));
		
		//円の情報を格納する
		MapOptions.setCenterCircle(new CircleOptions()
		.center(MapOptions.getCenterPoint().getPosition())
		.radius(MapOptions.getCircleRadius() * 1000.0)
		.fillColor(0x95c2ffc2)
		.strokeWidth(2)
		.visible(true));
		
		//マップにある地点
		int pointsSize = sharedPreferences.getInt("pointsListSize", 0);
		MarkerOptions mp;
		double pointLat,pointLon;
		MapOptions.clearPoints();
		for (int i = pointsSize-1; i>=0; i--) {
			mp = new MarkerOptions();
			String title = sharedPreferences.getString(i+"_PointListTitle", "");
			pointLat = Double.parseDouble(sharedPreferences.getString(i+"_PointListLat", "0.0f"));
			pointLon = Double.parseDouble(sharedPreferences.getString(i+"_PointListLon", "0.0f"));
			mp.position(new LatLng(pointLat,pointLon));
			mp.title(title);
			MapOptions.addPoint(mp,getApplicationContext());
		}
		
	}
}

