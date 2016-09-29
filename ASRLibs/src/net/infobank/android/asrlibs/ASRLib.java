package net.infobank.android.asrlibs;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

public abstract class ASRLib extends Activity implements RecognitionListener{
	
	private static final String LOG_TAG = "ASRLib";
	private static final boolean DEBUG = true;
	
	private static SpeechRecognizer mASR;
	Context ctx;
	
	/**
	 * Creates the single SpeechRecognizer instance and assigns a listener
	 * @see CustomRecognitionListener.java
	 * @param ctx context of the interaction
	 * */
	public void createRecognizer(Context ctx) {
		this.ctx = ctx;
		PackageManager packManager = ctx.getPackageManager();
		
		// 음성 지원 여부 확인
		List<ResolveInfo> intActivities = packManager.queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
		if(intActivities.size() != 0) {
			mASR = SpeechRecognizer.createSpeechRecognizer(ctx);
			mASR.setRecognitionListener(this);
		} else {
			mASR = null;
		}
	}
	
	/**
	 * Starts speech recognition
	 * @param languageModel Type of language model used 
	 * @param maxResults Maximum number of recognition results
	 */
	public void listen(String languageModel, int maxResults) throws Exception {
		if(languageModel.equals(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM) 
				|| languageModel.equals(RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH) 
				&& (maxResults >= 0)) {
			
			Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
			// 애플리케이션을 구분하기 위해 함수 호출 패키지를 명시함
			//Caution: be careful not to use: getClass().getPackage().getName());
			intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.getPackageName());
			
			// 언어 모델 명시
			intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, languageModel);
			
			// 결과 데이터의 최대 양을 명시. 음성 인식 정확도 순서대로 목록화
			intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxResults);
			
			// 음성 인식 시작
			mASR.startListening(intent);
		} else {
			if(DEBUG) Log.e(LOG_TAG, "[ASRLib]Invalid params to listen method");
			throw new Exception("Invalid params to listen method"); //If the input parameters are not valid, it throws an exception
		}
	}
	
	/**
	 * Stops listening to the user
	 */
	public void stopListening() {
		mASR.stopListening();
	}
	
	/********************************************************************************************************
	 * This class implements the {@link android.speech.RecognitionListener} interface, 
	 * thus it implements its methods. However not all of them are interesting to us:
	 * ******************************************************************************************************
	 */

	/*
	 * (non-Javadoc)
	 * @see android.speech.RecognitionListener#onResults(android.os.Bundle)
	 */
	@Override
	public void onResults(Bundle results) {
		if(DEBUG) Log.d(LOG_TAG, "[ASRLib]ASR results provided");
		
		if(results != null) {
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				//Checks the API level because the confidence scores are supported only from API level 14:
				//http://developer.android.com/reference/android/speech/SpeechRecognizer.html#CONFIDENCE_SCORES
				//Processes the recognition results and their confidences
				// Attention: It is not RecognizerIntent.EXTRA_RESULTS, that is for intents (see the ASRWithIntent app)
				processAsrResults (results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION), results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES));
			} else {
				processAsrResults(results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION), null);
			}
		} else {
			processAsrError(SpeechRecognizer.ERROR_NO_MATCH);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.speech.RecognitionListener#onReadyForSpeech(android.os.Bundle)
	 */
	@Override
	public void onReadyForSpeech(Bundle params) {
		if(DEBUG) Log.d(LOG_TAG, "[ASRLib]onReadyForSpeech()...");
		
		processAsrReadyForSpeech();
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.speech.RecognitionListener#onError(int)
	 */
	@Override
	public void onError(int errorCode) {
		if(DEBUG) Log.d(LOG_TAG, "[ASRLib]onError() : errorCode = " + errorCode);
		
		processAsrError(errorCode);
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.speech.RecognitionListener#onBeginningOfSpeech()
	 */
	@Override
	public void onBeginningOfSpeech() {
		if(DEBUG) Log.d(LOG_TAG, "[ASRLib]onBeginningOfSpeech()...");
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.speech.RecognitionListener#onRmsChanged(float)
	 */
	@Override
	public void onRmsChanged(float rmsdB) {
		if(DEBUG) Log.d(LOG_TAG, "[ASRLib]onRmsChanged()...");
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.speech.RecognitionListener#onBufferReceived(byte[])
	 */
	@Override
	public void onBufferReceived(byte[] buffer) {
		if(DEBUG) Log.d(LOG_TAG, "[ASRLib]onBufferReceived()...");
		
	}

	/*
	 * (non-Javadoc)
	 * @see android.speech.RecognitionListener#onBeginningOfSpeech()
	 */
	@Override
	public void onEndOfSpeech() {
		if(DEBUG) Log.d(LOG_TAG, "[ASRLib]onEndOfSpeech()...");
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.speech.RecognitionListener#onPartialResults(android.os.Bundle)
	 */	
	@Override
	public void onPartialResults(Bundle partialResults) {
		if(DEBUG) Log.d(LOG_TAG, "[ASRLib]onPartialResults()...");
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.speech.RecognitionListener#onEvent(int, android.os.Bundle)
	 */
	@Override
	public void onEvent(int eventType, Bundle params) {
		if(DEBUG) Log.d(LOG_TAG, "[ASRLib]onEvent()...");
		
	}

	/**
	 * Abstract method to process the recognition results 
	 * @param nBestList	List of the N recognition results
	 * @param nBestConfidences List of the N corresponding confidences
	 */
	public abstract void processAsrResults(ArrayList<String>nBestList, float[] nBestConfidences);
	
	/**
	 * Abstract method to process the situation in which the ASR engine is ready to listen
	 */
	public abstract void processAsrReadyForSpeech();
	
	/**
	 * Abstract method to process error situations
	 * @param errorCode code of the error (constant of the {@link android.speech.SpeechRecognizer} class
	 */
	public abstract void processAsrError(int errorCode);
}
