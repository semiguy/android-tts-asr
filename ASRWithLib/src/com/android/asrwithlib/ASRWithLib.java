package com.android.asrwithlib;

import java.util.ArrayList;

import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import net.infobank.android.asrlibs.ASRLib;

public class ASRWithLib extends ASRLib {
	
	private static final String LOG_TAG = "ASRWithLib";
	
	private final static int DEFAULT_NUMBER_RESULTS = 10;
	private final static String DEFAULT_LANG_MODEL = RecognizerIntent.LANGUAGE_MODEL_FREE_FORM; 
	
	// Attributes
	private int numberRecoResults = DEFAULT_NUMBER_RESULTS; 
	private String languageModel = DEFAULT_LANG_MODEL; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.asrlib);
		
		showDefaultValues();
		setSpeakButton();
		createRecognizer(getApplicationContext());
	}
	
	private void showDefaultValues() {
		//Show the default number of results in the corresponding EditText
		((EditText) findViewById(R.id.numResults_editText)).setText(""+DEFAULT_NUMBER_RESULTS);
		
		//Show the default number of 
		if(DEFAULT_LANG_MODEL.equals(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)) {			
			((RadioButton) findViewById(R.id.langModelFree_radio)).setChecked(true);
		} else {			
			((RadioButton) findViewById(R.id.langModelFree_radio)).setChecked(true);
		}
	}
	
	private void setRecognitionParams() {
		
		String numResults = ((EditText) findViewById(R.id.numResults_editText)).getText().toString();
		
		//Converts String into int, if it is not possible, it uses the default value
		try{
			numberRecoResults = Integer.parseInt(numResults);
		} catch(Exception e) {	
			numberRecoResults = DEFAULT_NUMBER_RESULTS;	
		}
		//If the number is <= 0, it uses the default value
		if(numberRecoResults <= 0) {			
			numberRecoResults = DEFAULT_NUMBER_RESULTS;
		}
		
		RadioGroup radioG = (RadioGroup) findViewById(R.id.langModel_radioGroup);
		
		switch(radioG.getCheckedRadioButtonId()){
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
		//Gain reference to speak button
		Button speak = (Button)findViewById(R.id.speech_btn);
		
		changeButtonAppearanceToDefault();
		
		speak.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//Speech recognition does not currently work on simulated devices,
				//it the user is attempting to run the app in a simulated device
				//they will get a Toast
				if("generic".equals(Build.BRAND.toLowerCase())){
					Toast toast = Toast.makeText(getApplicationContext(),"ASR is not supported on virtual devices", Toast.LENGTH_SHORT);
					toast.show();
					Log.d(LOG_TAG, "ASR attempt on virtual device");						
				} else{
					try {
						setRecognitionParams(); //Read ASR parameters
						listen(languageModel, numberRecoResults); //Start listening
					} catch (Exception e) {
						Toast toast = Toast.makeText(getApplicationContext(),"ASR could not be started: invalid params", Toast.LENGTH_SHORT);
						toast.show();
						Log.e(LOG_TAG, "ASR could not be started: invalid params");
					}
				}
				
			}
		});
	}
	
	/**
	 *  Shows the formatted best of N best recognition results (N-best list) from
	 *  best to worst in the <code>ListView</code>. 
	 *  For each match, it will render the recognized phrase and the confidence with 
	 *  which it was recognized.
	 *  
	 *  @param nBestList	    list of matches
	 *  @param nBestConfidence	confidence values (from 0 = worst, to 1 = best) for each match
	 */
	@Override
	public void processAsrResults(ArrayList<String> nBestList, float[] nBestConfidences) {
		
		changeButtonAppearanceToDefault(); //Button has its default appearance (so that the user knows the app is not listening anymore)
		
		//Creates a collection of strings, each one with a recognition result and its confidence, e.g. "Phrase matched (conf: 0.5)"
		ArrayList<String>nBestView = new ArrayList<String>();
		
		if(nBestList!=null){
			for(int i=0; i<nBestList.size(); i++){
				if(nBestConfidences!=null){
					if(nBestConfidences[i]>=0)
						nBestView.add(nBestList.get(i) + " (conf: " + String.format("%.2f", nBestConfidences[i]) + ")");
					else
						nBestView.add(nBestList.get(i) + " (no confidence value available)");
				}
			}
		}
		
		//Includes the collection in the ListView of the GUI
		setListView(nBestView);
				
		//Adds information to log
		Log.d(LOG_TAG, "There were : "+ nBestView.size()+" recognition results");
	}
	
	/**
	 * Includes the recognition results in the list view
	 * @param nBestView list of matches
	 */
	private void setListView(ArrayList<String> nBestView){
		
		// Instantiates the array adapter to populate the listView
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, nBestView);
        ListView listView = (ListView) findViewById(R.id.nbest_listview);
        listView.setAdapter(adapter);
	}
	
	/**
	 *  When the ASR is ready to listen, this method changes the appearance of the GUI button so that the user receives feedback
	 */
	@Override
	public void processAsrReadyForSpeech() {
		
		changeButtonAppearanceToListen();
	}
	
	/**
	 * Changes the background color and text of the speech button to show that the app is listening
	 */
	private void changeButtonAppearanceToListen() {
		Button button = (Button) findViewById(R.id.speech_btn); //Obtains a reference to the button
		button.setEnabled(false); //Deactivates the button so that the user cannot press it while the app is recognizing
		button.setText(getResources().getString(R.string.speechbtn_listening)); //Changes the button's message to the text obtained from the resources folder
		button.setBackgroundColor(getResources().getColor(R.color.speechbtn_listening)); //Changes the button's background to the color obtained from the resources folder
	}
	
	/**
	 * Changes the background color and text of the speech button to show that the app is not listening
	 */
	private void changeButtonAppearanceToDefault() {
		Button button = (Button) findViewById(R.id.speech_btn); //Obtains a reference to the button
		button.setEnabled(true); //Deactivates the button so that the user cannot press it while the app is recognizing
		button.setText(getResources().getString(R.string.speechbtn_default)); //Changes the button's message to the text obtained from the resources folder
		button.setBackgroundColor(getResources().getColor(R.color.speechbtn_default));	//Changes the button's background to the color obtained from the resources folder
	}
	
	/**
	 * Provides feedback to the user (by means of a Toast) when the ASR encounters an error
	 */
	@Override
	public void processAsrError(int errorCode) {
		changeButtonAppearanceToDefault();
		
		String errorMessage;
		switch (errorCode) 
        {
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
		
		Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
		Log.e(LOG_TAG, "Error when attempting listen: "+ errorMessage);
		
	}
}
