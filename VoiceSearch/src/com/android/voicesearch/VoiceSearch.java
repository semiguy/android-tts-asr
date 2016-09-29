package com.android.voicesearch;

import java.util.ArrayList;
import java.util.Locale;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import net.infobank.android.asrlibs.ASRLib;
import net.infobank.android.ttslibs.TTSLib;

public class VoiceSearch extends ASRLib {
	
	private static final String LOG_TAG = "VoiceSearch";
	private TTSLib mTts;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.voicesearch);
		
		setSpeakButton();
		// 음성 인식 initial
		createRecognizer(getApplicationContext());
		// TTS initial
		mTts = TTSLib.getInstance(this);
	}
	
	private void setSpeakButton() {
		
		Button speak = (Button)findViewById(R.id.speech_btn);
		speak.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				if("generic".equals(Build.BRAND.toLowerCase(Locale.KOREA))) {
					Toast toast = Toast.makeText(getApplicationContext(), "ASR is not supported on virtual devices", Toast.LENGTH_SHORT);
					toast.show();
					Log.e(LOG_TAG, "ASR attempt on virtual device");
				} else {
					startListening();
				}
			}
		});
	}
	
	private void startListening() {
		if(deviceConnectedToInternet()) {
			try {
				indicateListening();
				
				listen(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM, -1);
			} catch (Exception e) {
				Toast toast = Toast.makeText(getApplicationContext(), "ASR could not be started: invalid params", Toast.LENGTH_SHORT);
				toast.show();
				Log.e(LOG_TAG, e.getMessage());
			}
		} else {
			Toast toast = Toast.makeText(getApplicationContext(),"Please check your Internet connection", Toast.LENGTH_SHORT);
			toast.show();
			Log.e(LOG_TAG, "Device not connected to Internet");	
		}
	}
	
	private void indicateListening() {
		Button button = (Button)findViewById(R.id.speech_btn);
		button.setText(getResources().getString(R.string.speechbtn_listening));
		button.setBackgroundColor(getResources().getColor(R.color.speechbtn_listening));
		
		mTts.speak(getResources().getString(R.string.initial_prompt));
	}
	
	private void indicateSearch(String criteria) {
		changeButtonAppearanceToDefault();
		mTts.speak(criteria + getResources().getString(R.string.searching_prompt));
	}
	
	private void changeButtonAppearanceToDefault() {
		Button button = (Button) findViewById(R.id.speech_btn);
		button.setText(getResources().getString(R.string.speechbtn_default));
		button.setBackgroundColor(getResources().getColor(R.color.speechbtn_default));
	}
	
	@Override
	public void processAsrResults(ArrayList<String> nBestList, float[] nBestConfidences) {

		if(nBestList != null) {
			if(nBestList.size() > 0) {
				String bestResult = nBestList.get(0);
				indicateSearch(bestResult);
				googleText(bestResult);
			}
		}
	}

	@Override
	public void processAsrReadyForSpeech() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processAsrError(int errorCode) {

		changeButtonAppearanceToDefault();
		
		String errorMessage;
		switch (errorCode) {
		case SpeechRecognizer.ERROR_AUDIO:
			errorMessage = "Audio recording error";
			break;
		 case SpeechRecognizer.ERROR_CLIENT: 
	        	errorMessage = "Client side error"; 
	            break;
        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: 
        	errorMessage = "Insufficient permissions" ; 
            break;
        case SpeechRecognizer.ERROR_NETWORK: 
        	errorMessage = "Network related error" ;
            break;
        case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:                
            errorMessage = "Network operation timeout"; 
            break;
        case SpeechRecognizer.ERROR_NO_MATCH: 
        	errorMessage = "No recognition result matched" ; 
        	break;
        case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: 
        	errorMessage = "RecognitionServiceBusy" ; 
            break;
        case SpeechRecognizer.ERROR_SERVER: 
        	errorMessage = "Server sends error status"; 
            break;
        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: 
        	errorMessage = "No speech input" ; 
            break;
        default:
        	errorMessage = "ASR error";
        	break;
		}
		
		Log.e(LOG_TAG, "Error when attempting to listen: "+ errorMessage);
		Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
		
		try {
			mTts.speak(errorMessage, "EN");
		} catch (Exception e) {
			Log.e(LOG_TAG, "English not available for TTS, default language used instead");
		}
	}
	
	public void googleText(String criterion) {
		if(deviceConnectedToInternet()) {
			PackageManager pm = getPackageManager();
			Intent intent = new Intent();
			intent.putExtra(SearchManager.QUERY, criterion);
			intent.setAction(Intent.ACTION_WEB_SEARCH);
			ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
			
			if(resolveInfo == null) {
				Log.e(LOG_TAG, "Not possible to carry out ACTION_WEB_SEARCH Intent");
			}
		} else {
			Toast.makeText(getApplicationContext(),"Please check your Internet connection", Toast.LENGTH_LONG).show(); //Not possible to carry out the intent
			Log.e(LOG_TAG, "Device not connected to Internet");	
		}
	}
	
	public boolean deviceConnectedToInternet() {
		ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		
		return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mTts.shutdown();
	}
}
