package com.example.kennai;

import java.util.ArrayList;

import android.content.Context;
import android.location.Location;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * マップに関する全ての重要な情報を格納するためのSingletonクラス。
 */
public class MapOptions {
	//中心地点
	private static MarkerOptions centerPoint;
	
	//円
	private static CircleOptions centerCircle;
	
	//地点のArrayList
	private static ArrayList<MarkerOptions> points = new ArrayList<MarkerOptions>();
	
	//マップの種類
	private static int mapType = 1;
	
	//ニコちゃんモード
	private static boolean smileyMode = false;
	
	//マップ
	private static GoogleMap map;
	
	//円の半径
	private static double circleRadius = 1;

	/**
	 * pointsに地点を追加するためのメソッド。追加する前に、パラメータのMarkerOptions (mo) が円に
	 * 入っているかどうかに基づいて地点の色が変化。後、moと中心地点の距離がmoのsnippitに表示される。
	 * もし、ユーザーが中心地点が表示される前に地点を追加すると、snippitはアップデートされません。
	 *
	 * @param mo　追加する地点
	 * @param context 現在のコンテクスト
	 */
	public static void addPoint(MarkerOptions mo,Context context) {
		MarkerOptions temp = mo;
		LatLng centerPtPos = centerPoint.getPosition();
		
		//追加する地点の色を決める
		if(calculationByDistance(mo.getPosition(),centerPtPos) < circleRadius)
			temp.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
		else
			temp.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

		if(centerPoint.isVisible()) {
			double dtc = (double)Math.round(calculationByDistance(mo.getPosition(),centerPtPos) * 100000) / 100000;
			temp.snippet(context.getString(R.string.dist_to_center)
					+ ": " + String.valueOf(dtc) + " km");
		}
		points.add(0,temp);
	}


	/**
	 * @param position 削除される地点のインデックス
	 */
	public static void removePoint(int position) {
		points.remove(position);
	}


	/**
	 * 全ての地点を削除するためのメソッド
	 */
	public static void clearPoints() {
		points.clear();
	}


	/**
	 * @return 円の中の地点のArrayList
	 */
	public static ArrayList<MarkerOptions> getInsidePoints() {
		ArrayList<MarkerOptions> insidePoints = new ArrayList<MarkerOptions> ();
		for(MarkerOptions m:points) {
			if(calculationByDistance(m.getPosition(),centerPoint.getPosition()) < circleRadius)
				insidePoints.add(m);
		}
		return insidePoints;
	}


	/**
	 * 円の南半分の地点を東から西まで挿入ソートするためのメソッド。
	 * ニコちゃんマーク作成方法。
	 * 
	 * @return  ソート済みリスト
	 */
	public static ArrayList<MarkerOptions> getSortedBottomPoints() {
		ArrayList<MarkerOptions> bottomPoints = new ArrayList<MarkerOptions>();
		LatLng centerPtPos = centerPoint.getPosition();
		
		for(MarkerOptions m:points) {
			if(calculationByDistance(m.getPosition(),centerPtPos) < circleRadius &&
					m.getPosition().latitude < centerPtPos.latitude)
				bottomPoints.add(m);
		}
		
		int i,j;
		MarkerOptions key;
		for(j=1; j<bottomPoints.size(); j++) {
			key = bottomPoints.get(j);
			for(i=j-1; (i >= 0) && (bottomPoints.get(i).getPosition().longitude < 
					key.getPosition().longitude); i--) {
				bottomPoints.set(i+1, bottomPoints.get(i));
			}
			bottomPoints.set(i+1,key);
		}
		return bottomPoints;
	}

	/**
	 * 自動的に全ての地点の色とSnippitを変えるためのメソッド
	 * 
	 * @param context 現在のコンテクスト
	 */
	public static void updatePointColors(Context context){
		for(MarkerOptions m : points) {
			if(calculationByDistance(m.getPosition(),centerPoint.getPosition()) < circleRadius)
				m.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
			else
				m.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
			if(centerPoint.isVisible())
			{
				double distToCenter = (double)Math.round(calculationByDistance(m.getPosition(),centerPoint.getPosition()) * 100000) / 100000;
				m.snippet(context.getString(R.string.dist_to_center)+": " + String.valueOf(distToCenter) + " km");
			}
		}
	}

	/**
	 * ソースから目的地の緯度経度をソースとの間の距離、角度を使って計算する
	 * 
	 * @param source  ソース
	 * @param range  ソースとの間の距離
	 * @param bearing  ソースからの角度（０は北）
	 * @return  新規の緯度経度
	 */
	public static LatLng CalculateDerivedPosition (LatLng source, double range, double bearing)
	{
		double latA = Math.toRadians(source.latitude);
		double lonA = Math.toRadians(source.longitude);
		double angularDistance = range / 6378137.0;
		double trueCourse = Math.toRadians(bearing);

		double lat = Math.asin(Math.sin(latA) * Math.cos(angularDistance) + 
				Math.cos(latA) * Math.sin(angularDistance) * Math.cos(trueCourse));

		double dlon = Math.atan2(Math.sin(trueCourse) * Math.sin(angularDistance) * Math.cos(latA), 
				Math.cos(angularDistance) - Math.sin(latA) * Math.sin(lat));

		double lon = ((lonA + dlon + Math.PI) % (2*Math.PI)) - Math.PI;

		return new LatLng(Math.toDegrees(lat), Math.toDegrees(lon));
	}


	/**
	 * 二つの地点の間の距離を計算するためのメソッド
	 * 
	 * @param StartP  最初の地点の緯度と経度
	 * @param EndP   最後の地点の緯度と経度
	 * @return StartPとEndPの距離（km）
	 */
	public static double calculationByDistance(LatLng StartP, LatLng EndP) {
		int Radius=6371; //地球の半径
		double lat1 = StartP.latitude;
		double lat2 = EndP.latitude;
		double lon1 = StartP.longitude;
		double lon2 = EndP.longitude;
		double dLat = Math.toRadians(lat2-lat1);
		double dLon = Math.toRadians(lon2-lon1);
		double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
				Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
				Math.sin(dLon/2) * Math.sin(dLon/2);
		double c = 2 * Math.asin(Math.sqrt(a));
		return Radius * c;
	}

	/**
	 * @return 現在の円の半径（km）
	 */
	public static double getCircleRadius() {
		return circleRadius;
	}

	/**
	 * @param circleRadius 新規の円の半径（km）
	 */
	public static void setCircleRadius(double circleRadius) {
		MapOptions.circleRadius = circleRadius;
	}

	/**
	 * @return 現在のマップ
	 */
	public static GoogleMap getMap() {
		return map;
	}


	/**
	 * @param map 現在のマップ
	 */
	public static void setMap(GoogleMap map) {
		MapOptions.map = map;
	}

	/**
	 * @return　ユーザーの現在地
	 */
	public static Location getCurrentLocation() {
		return map.getMyLocation();
	}

	/**
	 * @return 現在のニコちゃんモード
	 */
	public static boolean isSmileyMode() {
		return smileyMode;
	}


	/**
	 * @param smileyMode 新規ニコちゃんモード
	 */
	public static void setSmileyMode(boolean smileyMode) {
		MapOptions.smileyMode = smileyMode;
	}


	/** 
	 * @return 現在のマップの種類
	 */
	public static int getMapType() {
		return mapType;
	}


	/**
	 * @param type 新しいマップの種類
	 * new map type
	 */
	public static void setMapType(int type) {
		mapType = type;
	}

	/**
	 * @return 現在の円
	 */
	public static CircleOptions getCenterCircle() {
		return centerCircle;
	}

	/**
	 * @param centerCircle　新規の円
	 */
	public static void setCenterCircle(CircleOptions centerCircle) {
		MapOptions.centerCircle = centerCircle;
	}


	/**
	 * @return 現在の中心地点
	 */
	public static MarkerOptions getCenterPoint() {
		return centerPoint;
	}


	/**
	 * @param centerPoint 新規の中心地点
	 */
	public static void setCenterPoint(MarkerOptions centerPoint) {
		MapOptions.centerPoint = centerPoint;
	}


	/**
	 * @return 一番最後にPointsに追加した地点
	 */
	public static MarkerOptions getMostRecentPoint() {
		return (points.size() <= 0) ? null : points.get(0);
	}


	/**
	 * @return 全ての地点のArrayList
	 */
	public static ArrayList<MarkerOptions> getPoints() {
		return points;
	}


}
