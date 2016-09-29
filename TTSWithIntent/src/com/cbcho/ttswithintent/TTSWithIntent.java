package com.cbcho.ttswithintent;

import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class TTSWithIntent extends Activity {
	
	private final String DEBUG_TAG = "TTSWithIntent";
	
	private int TTS_DATA_CHECK = 12; // It is an integer value to be used as a checksum
	private TextToSpeech tts = null;
	
	private EditText inputText;
	private Button speakButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tts_intent);
		
		Log.d(DEBUG_TAG, "[TTSWithIntent]onCreate()...");
		// set up the speak button
		setButton();
		// Invoke the method to initialize text to speech
		initTTS();
		
		inputText = (EditText)findViewById(R.id.input_text);
	}
	
	private void setButton() {
		
		Log.d(DEBUG_TAG, "[TTSWithIntent]setButton()...");
		speakButton = (Button)findViewById(R.id.speak_button);
		
		speakButton.setOnClickListener(speackButtonOnClickListener);
	}
	
	private final OnClickListener speackButtonOnClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Log.d(DEBUG_TAG, "[TTSWithIntent]onClick()...");
			String text = inputText.getText().toString();
			
			if(text != null && text.length() > 0) {
				tts.speak(text, TextToSpeech.QUEUE_ADD, null);
			}
		}
	};
	
	private void initTTS() {
		Log.d(DEBUG_TAG, "[TTSWithIntent]initTTS()...");
		disableSpeakButton();
		
		// TTS DATA 사용 확인
		Intent checkIntent = new Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		startActivityForResult(checkIntent, TTS_DATA_CHECK);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		Log.d(DEBUG_TAG, "[TTSWithIntent]onActivityResult():requestCode=" + requestCode + ", resultCode=" + resultCode);
		
		if(requestCode == TTS_DATA_CHECK) {
			if(resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) { // 정상적인 경우
				tts = new TextToSpeech(this, new OnInitListener() {
					
					@Override
					public void onInit(int status) {
						
						if(status == TextToSpeech.SUCCESS) {
							Toast.makeText(TTSWithIntent.this, "TTS initialized", Toast.LENGTH_LONG).show();
							
							if(tts.isLanguageAvailable(Locale.KOREA) >= 0) {
								tts.setLanguage(Locale.KOREA);
							}
						}
						
						enableSpeakButton();
					}
				});
			} else {
				PackageManager pm = getPackageManager();
				
				Intent installIntent = new Intent();
				installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				
				ResolveInfo resolveInfo = pm.resolveActivity(installIntent, PackageManager.MATCH_DEFAULT_ONLY);
				
				if(resolveInfo == null) {
					Toast.makeText(TTSWithIntent.this, "There is no TTS installed, please download it from Google Play", Toast.LENGTH_LONG).show();
				} else {
					startActivity(installIntent);
				}
			}
		}
	}
	
	private void disableSpeakButton() {
		speakButton.setEnabled(false);
	}
	
	private void enableSpeakButton() {
		speakButton.setEnabled(true);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
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
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if(tts != null) {
			tts.stop();
			tts.shutdown();
		}
	}
}
