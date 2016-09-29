package com.android.asrwithintent;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class ASRWithIntent extends Activity {
	
	private static final String LOGTAG = "ASRWithIntent";
	private static final boolean DEBUG = true;
	
	private final static int DEFAULT_NUMBER_RESULTS = 10;
	private final static String DEFAULT_LANG_MODEL = RecognizerIntent.LANGUAGE_MODEL_FREE_FORM;
	
	private int numberRecoResults = DEFAULT_NUMBER_RESULTS;
	private String languageModel = DEFAULT_LANG_MODEL;
	
	private static int ASR_CODE = 123;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.asrwithintent);
		
		showDefaultValues();
		setSpeakButton();
	}

	private void showDefaultValues() {
		((EditText)findViewById(R.id.numResults_editText)).setText("" + DEFAULT_NUMBER_RESULTS);
		
		if(DEFAULT_LANG_MODEL.equals(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)) {
			((RadioButton)findViewById(R.id.langModelFree_radio)).setChecked(true);
		} else {
			((RadioButton)findViewById(R.id.langModelFree_radio)).setChecked(true);
		}
	}
		
	private void listen() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		// 언어 모델 설정
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, languageModel);
		// 결과는 음성 인식 정확도 순으로 표시됨
		intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, numberRecoResults);
		
		// 음성인식 준비 시작
		startActivityForResult(intent, ASR_CODE);
	}
	
	private void setRecognitionParams() {
		
		String numResults = ((EditText)findViewById(R.id.numResults_editText)).getText().toString();
		
		try {
			numberRecoResults = Integer.parseInt(numResults);
		} catch (Exception e) {
			
			numberRecoResults = DEFAULT_NUMBER_RESULTS;
		}
		
		if(numberRecoResults <= 0) {
			numberRecoResults = DEFAULT_NUMBER_RESULTS;
		}
		
		RadioGroup radioG = (RadioGroup)findViewById(R.id.langModel_radioGroup);
		switch(radioG.getCheckedRadioButtonId()) {
		case R.id.langModelFree_radio:
			languageModel = RecognizerIntent.LANGUAGE_MODEL_FREE_FORM;
			break;
		case R.id.langModelWeb_radio:
			languageModel = RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH;
			break;
		default:
			languageModel = DEFAULT_LANG_MODEL;
			break;
		}
	}
	
	private void setSpeakButton() {
		Button speak = (Button)findViewById(R.id.speech_btn);
		
		speak.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if("generic".equals(Build.BRAND.toLowerCase())) {
					Toast toast = Toast.makeText(getApplicationContext(), "ASR is not supported on virtual devices", Toast.LENGTH_SHORT);
					toast.show();
					Log.d(LOGTAG, "ASR attempt on virtual device");
				} else {
					// GUI를 통해 파라미터 설정
					setRecognitionParams();
					// 음성인식기를 설정하고 음성 인식 준비
					listen();
				}
			}
		});
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if(requestCode == ASR_CODE) {
			if(resultCode == RESULT_OK) {
				if(data != null) {
					// ASR 결과 객체에서 N-best 목록과 음성 인식 정확도 값을 획득
					ArrayList<String> nBestList = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
					float[] nBestConfidences = null;
					
					if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
						nBestConfidences = data.getFloatArrayExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES);
					} 
					/*
					 * 스트링 집합을 생성하는데, 각 스트링은 음성 인식 결과와 음성 인식 정확도를 포함
					 * 예를 들어 매칭되는 문장(정확도:0.5)
					 */
					ArrayList<String> nBestView = new ArrayList<String>();
					
					for(int i=0; i < nBestList.size(); i++) {
						if(nBestConfidences != null) {
							if(nBestConfidences[i] >= 0) {
								nBestView.add(nBestList.get(i) + " (conf: " + String.format("%.2f", nBestConfidences[i]) + ")");
							} else {
								nBestView.add(nBestList.get(i) + " (no confidence value available)");
							}
						}
					}
					
					// GUI 리스트 뷰에 음성 인식 결과 집합을 포함
					setListView(nBestView);
					// 로그에 음성 데이터 정보 포함
					Log.i(LOGTAG, "There were : " + nBestView.size() + " recognition results");
				}
			} else {
				Log.i(LOGTAG, "Recognition was not successful");
			}
		}
	}
	
	private void setListView(ArrayList<String> nBestView) {
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, nBestView);
		ListView listView = (ListView)findViewById(R.id.nbest_listview);
		listView.setAdapter(adapter);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.asrwith_intent, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
