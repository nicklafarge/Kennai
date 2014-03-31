package com.example.kennai;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 *　設定を変化するためのアクティビティ
 */
public class Settings extends Activity{

	/** 
	 * ユーザーインタフェースの要素をインスタンス化する
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 * @param savedInstanceState  現在のBundle
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);


		final Button setCenterPoint = (Button) findViewById(R.id.center_point);
		final Button addPoint = (Button) findViewById(R.id.add_point);
		final Spinner mapTypeSpinner = (Spinner) findViewById(R.id.map_type_spinner);
		final CheckedTextView smileyCheck = (CheckedTextView) findViewById(R.id.smiley_mode);
		final EditText et = (EditText) findViewById(R.id.circle_size_edit);
		final Button b = (Button) findViewById(R.id.circle_size_button);
		final Button resetMap = (Button) findViewById(R.id.reset_map);


		//中心地点を変えるためのアクティビティを実行するためのButton
		setCenterPoint.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				Intent myIntent = new Intent(view.getContext(), CenterCord.class);
				startActivityForResult(myIntent, 0);
			}

		});

		//地点を追加／削除するためのアクティビティを実行するためのButton
		addPoint.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				Intent myIntent = new Intent(view.getContext(), AddPoints.class);
				startActivityForResult(myIntent, 0);
			}
		});

		//マップの種類を変えるためのSpinner
		mapTypeSpinner.setSelection(MapOptions.getMapType()-1);
		mapTypeSpinner.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
				if(MapOptions.getMapType()!=pos+1)
					Toast.makeText(view.getContext(), getString(R.string.map_type_changed), Toast.LENGTH_SHORT).show();
				MapOptions.setMapType(pos+1);
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
			}
		});

		//新規ニコちゃんモードを変えるためのCheckedTextView
		smileyCheck.setChecked(MapOptions.isSmileyMode());
		smileyCheck.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				smileyCheck.toggle();
				MapOptions.setSmileyMode(smileyCheck.isChecked());
			}
		});

		//円の半径を変えるためのEditText
		if(MapOptions.getCenterCircle() != null)
			et.setHint(String.valueOf(MapOptions.getCircleRadius()) + " km");
		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				String text=et.getText().toString();
				InputMethodManager imm = (InputMethodManager)getSystemService(
						Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
				changeRadius(Double.parseDouble(text));
				et.setText("");
				et.setHint(String.valueOf(MapOptions.getCircleRadius()) + " km");
			}
		});

		//マップをリセットするためのButton
		resetMap.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				AlertDialog.Builder alert = new AlertDialog.Builder(Settings.this);
				alert.setTitle(getString(R.string.reset_map));
				alert.setMessage(getString(R.string.are_you_sure_reset));

				alert.setPositiveButton(getString(R.string.yes), new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						resetMap();
					}
				});
				alert.setNegativeButton(getString(R.string.cancel), new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});

				alert.show();
			}
		});

	}
	
	/**
	 * 円の半径を変化する
	 * @param newRadius　新規の半径
	 */
	private void changeRadius(double newRadius) {
		if(newRadius > 0 && newRadius <4000 && newRadius*1000.0 != MapOptions.getCenterCircle().getRadius()) {
			MapOptions.getCenterCircle().radius(newRadius * 1000.0);
			MapOptions.setCircleRadius(newRadius);
			Toast.makeText(this, getString(R.string.radius_change), Toast.LENGTH_SHORT).show();
		}
		else if(newRadius <= 0) {
			Toast.makeText(this, getString(R.string.radius_greater_than_zero), Toast.LENGTH_SHORT).show();
		}
		else
			Toast.makeText(this, getString(R.string.radius_too_large), Toast.LENGTH_SHORT).show();
		MapOptions.updatePointColors(getApplicationContext());
	}

	/**
	 * マップをリセットする
	 */
	private void resetMap() {
		MapOptions.setMapType(1);
		MapOptions.setSmileyMode(false);
		MapOptions.setCircleRadius(1);
		MapOptions.clearPoints();
		MapOptions.setCenterPoint(new MarkerOptions()
		.visible(false)
		.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
		.position(new LatLng(0,0)));
		MapOptions.setCenterCircle(new CircleOptions()
		.center(MapOptions.getCenterPoint().getPosition())
		.radius(MapOptions.getCircleRadius() * 1000.0)
		.fillColor(0x95c2ffc2)
		.strokeWidth(2)
		.visible(false));
		MainActivity.setReset(true);
		AddPoints.setReset(true);
		EditText et = (EditText) findViewById(R.id.circle_size_edit);
		et.setText("");
		et.setHint(String.valueOf(MapOptions.getCircleRadius()) + " km");
		Spinner mapTypeSpinner = (Spinner) findViewById(R.id.map_type_spinner);
		mapTypeSpinner.setSelection(MapOptions.getMapType()-1);
		CheckedTextView smileyCheck = (CheckedTextView) findViewById(R.id.smiley_mode);
		smileyCheck.setChecked(false);
		
	}

}