package com.android.voicelaunch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.codec.language.Soundex;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Toast;
import net.infobank.android.asrlibs.ASRLib;
import net.infobank.android.ttslibs.TTSLib;

public class VoiceLaunch extends ASRLib {
	
	private static final boolean DEBUG = true;
	private static final String LOG_TAG = "VoiceLaunch";
	
	private enum SimilarityAlgorithm {
		ORTHOGRAPHIC,
		PHONETIC
	}
	private static float DEFAULT_THRESHOLD = 0; //From 0 to 1
	private static SimilarityAlgorithm DEFAULT_ALGORITHM = SimilarityAlgorithm.ORTHOGRAPHIC;
	
	private float similarityThreshold = DEFAULT_THRESHOLD;
	private SimilarityAlgorithm similarityCalculation = DEFAULT_ALGORITHM;
	
	private TTSLib mTts;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.voicelaunch);
		
		if(DEBUG) Log.d(LOG_TAG, "[VoiceLaunch]indicateListening()...");
		
		initializeGUI();
		
		createRecognizer(getApplicationContext());
		
		mTts = TTSLib.getInstance(this);
		
		setSpeakButton();
	}
	
	/*************************************************************************************************************************************
	 * 			GUI-related methods
	 *************************************************************************************************************************************/
	private void initializeGUI() {
		
		switch (DEFAULT_ALGORITHM) {
		case ORTHOGRAPHIC:
			((RadioButton) findViewById(R.id.orthographic_radio)).setChecked(true);
			break;
		case PHONETIC:
			((RadioButton) findViewById(R.id.phonetic_radio)).setChecked(true);
			break;
			
		default:
			((RadioButton) findViewById(R.id.orthographic_radio)).setChecked(true);
			break;
		}
		
		initializeSeekBar();
		
		changeButtonAppearanceToDefault();
	}
	
	private void initializeSeekBar() {
		
		final SeekBar seekT = (SeekBar)findViewById(R.id.threshold_seekBar);
		seekT.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

				seekT.setThumb(writeOnDrawable(R.drawable.barpointershadow, String.format("%.1f", seekBarValueToFloat(progress))));
			}
		});
		
		seekT.setMax(10); //SeekBar does not admit decimals, so instead of having it from 0 to 1, we will use it from 0 to 10
		seekT.setProgress(floatToSeekBarValue(DEFAULT_THRESHOLD));
	}
	
	/**
	 * Changes from the interval [0, 10] used in the seekbar, to the interval [0, 1] used for the similarity value
	 */
	private float seekBarValueToFloat(int seekValue){
	    return seekValue * 0.1f;
	}
	
	/**
	 * Changes from the interval [0, 1] used for the similarity value, to the interval [0, 10] used in the seekbar
	 */
	private int floatToSeekBarValue(float floatValue){
		return (int) (floatValue*10);
	}
	
	/**
	 * Writes a text in a drawable. We will use this method to show the similarity value in the seekbar
	 *  See stackoverflow http://stackoverflow.com/questions/6264543/draw-on-drawable?rq=1
	 */
	private BitmapDrawable writeOnDrawable(int drawableId, String text) {
		
		Bitmap bm = BitmapFactory.decodeResource(getResources(), drawableId).copy(Bitmap.Config.ARGB_8888, true);
		
		Paint paint = new Paint();
		paint.setStyle(Style.FILL);
		paint.setColor(Color.BLACK);
		paint.setTextSize(10);
		
		Canvas canvas = new Canvas(bm);
		canvas.drawText(text, bm.getWidth()/4, bm.getHeight()/2, paint);
		
		return new BitmapDrawable(bm);
	}
	
	/**
	 * Initializes the search button and its listener. When the button is pressed, a feedback is shown to the user
	 * and the recognition starts
	 */
	
	private void setSpeakButton() {
		Button speak = (Button) findViewById(R.id.speech_btn);
		
		speak.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//If the user is running the app on a virtual device, they get a Toast
				if("generic".equals(Build.BRAND.toLowerCase(Locale.US))){
					Toast toast = Toast.makeText(getApplicationContext(),"ASR is not supported on virtual devices", Toast.LENGTH_SHORT);
					toast.show();
					Log.e(LOG_TAG, "ASR attempt on virtual device");						
				}
				else {
						startListeningForApps();
				}
				
			}
		});
	}
	
	/**
	 * Reads the values for the similarity threshold and algorithm from the GUI
	 */
	private void readGUIParameters() {
		
		//String selectedThreshold = ((EditText) findViewById(R.id.threshold_editText)).getText().toString();
		
		if(DEBUG) Log.d(LOG_TAG, "[VoiceLaunch]readGUIParameters()...");
		try {
			similarityThreshold = seekBarValueToFloat(((SeekBar)findViewById(R.id.threshold_seekBar)).getProgress());
			if(DEBUG) Log.d(LOG_TAG, "[VoiceLaunch]readGUIParameters():similarityThreshold = " + similarityThreshold);
		} catch (Exception e) {
			
			similarityThreshold = DEFAULT_THRESHOLD;
			Log.e(LOG_TAG, "The similarity threshold selected could not be used, using the default value instead");
		}
		
		RadioGroup radioG = (RadioGroup)findViewById(R.id.measure_radioGroup);
		
		switch (radioG.getCheckedRadioButtonId()) {
		case R.id.orthographic_radio:
			similarityCalculation = SimilarityAlgorithm.ORTHOGRAPHIC;
			break;
		case R.id.phonetic_radio:
			similarityCalculation = SimilarityAlgorithm.PHONETIC;
			break;
		default:
			similarityCalculation = DEFAULT_ALGORITHM;
			Log.e(LOG_TAG, "The similarity algorithm selected could not be used, using the default algorithm instead");
			break;
		}
	}
	
	/**
	 * Provides feedback to the user to show that the app is listening:
	 * 		* It changes the color and the message of the speech button
	 *      * It synthesizes a voice message
	 */
	private void indicateListening() {
		
		if(DEBUG) Log.d(LOG_TAG, "[VoiceLaunch]indicateListening()...");
		
		Button button = (Button) findViewById(R.id.speech_btn); //Obtains a reference to the button
		button.setText(getResources().getString(R.string.speechbtn_listening)); //Changes the button's message to the text obtained from the resources folder
		button.setBackgroundColor(getResources().getColor(R.color.speechbtn_listening)); //Changes the button's background to the color obtained from the resources folder
		mTts.speak(getResources().getString(R.string.initial_prompt));
		setListView(new ArrayList<String>()); // clear result list
	}
	
	/**
	 * Provides feedback to the user to show that the app is performing a search:
	 * 		* It changes the color and the message of the speech button
	 *      * It synthesizes a voice message
	 */
	private void indicateLaunch(String appName) {
		changeButtonAppearanceToDefault();
		mTts.speak(appName + "실행 합니다."); 
    	Toast.makeText(getBaseContext(), "Launching "+appName, Toast.LENGTH_LONG).show(); //Show user-friendly name
	}
	
	/**
	 * Provides feedback to the user to show that the app is idle:
	 * 		* It changes the color and the message of the speech button
	 */	
	private void changeButtonAppearanceToDefault(){
		Button button = (Button) findViewById(R.id.speech_btn); //Obtains a reference to the button
		button.setText(getResources().getString(R.string.speechbtn_default)); //Changes the button's message to the text obtained from the resources folder
		button.setBackgroundColor(getResources().getColor(R.color.speechbtn_default));	//Changes the button's background to the color obtained from the resources folder		
	}
	
	/**
	 * Shows the matching apps and their similarity values on the GUI
	 * @param sortedApps It is a tree map in which the last element is the most similar, and the first the least
	 */
	private void showMatchingNames(ArrayList<MyApp> sortedApps) {
		
		ArrayList<String> result = new ArrayList<String>();
		
		for(MyApp app: sortedApps) {
			result.add(app.getName() +" (Similarity: "+String.format("%.2f", app.getSimilarity())+")");
			//Drawable icon = getPackageManager().getApplicationIcon(app[1]);
		}
		
		setListView(result);
	}
	
	/**
	 * Includes the recognition results in the list view
	 * @param nBestView list of matches
	 */
	private void setListView(ArrayList<String> matchingApps) {
		
		// Instantiates the array adapter to populate the listView
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, matchingApps);
		ListView listView = (ListView) findViewById(R.id.matchingapps_listview);
		listView.setAdapter(adapter);
	}
	
	/*************************************************************************************************************************************
	 * Text-processing methods to compare name of apps
	 *************************************************************************************************************************************/
	
	/**
	 * Obtains a collection with information about the apps which name is similar to what was recognized from the user. The collection is sorted
	 * from most to least similar.
	 * @param recognizedName Name of the app recognized from the user input
	 * @return A collection of instances of MyApp. MyApp is an auxiliary class that we have created (see at the bottom of this file), to store
	 * information about the apps retreived, concretely: name, package name and similarity to recognized name. If no apps are found, it returns
	 * an empty list.
	 */
	private ArrayList<MyApp> getSimilarAppsSorted(String recogizedName) {
		MyApp app;
		Double similarity = 0.0;
		
		ArrayList<MyApp> similarApps = new ArrayList<MyApp>();
		
		PackageManager packageManager = getPackageManager();
		List<PackageInfo> apps = getPackageManager().getInstalledPackages(0);
		
		//For all apps installed in the device...
		for(int i=0; i < apps.size(); i++) {
			 PackageInfo packInfo = apps.get(i);
	            
            //Gets application name
            String name = packInfo.applicationInfo.loadLabel(packageManager).toString();
            
            //Gets package name
            String packageName = packInfo.packageName;
                        
            //Measures similarity of the app's name with the user input
            switch(similarityCalculation){       		
            	case ORTHOGRAPHIC:
            		similarity = compareOrthographic(normalize(recogizedName), normalize(name));
            		break;
            	
            	case PHONETIC:
            		similarity = comparePhonetic(normalize(recogizedName), normalize(name));
            		break;
            		
            	default:
            		similarity = compareOrthographic(normalize(recogizedName), normalize(name)); 
            		break;	
            }
            
            //Adds the app to the collection if the similarity is higher than the threshold
            if(similarity > similarityThreshold) {
                app = new MyApp(name, packageName, similarity);
            	similarApps.add(app);
            }
		}
		
		//Sorts apps from least to most similar, in order to do this, we use our own comparator,
        //using the "AppComparator" class, which is defined as a private class at the end of the file
		Collections.sort(similarApps, new AppComparator());
		
		if(DEBUG) {			
			for(MyApp aux : similarApps) {
				Log.i(LOG_TAG, "Similarity: "+aux.getSimilarity()+", Name: "+aux.getName()+", Package: "+aux.getPackageName());
			}
		}
		
		return similarApps;
	}
	
	/**
	 * Normalizes a text
	 * @param text
	 * @return the input text without spaces and in lower case
	 */
	private String normalize(String text){
		return text.trim().toLowerCase(Locale.KOREA);
	}
	
	/**
	 * Compares the names using the Levenshtein distance, which is the minimal number of characters you have to replace, 
	 * insert or delete to transform string a into string b.
	 * We have used a computation of this distance provided by Wikipedia.
	 * @return similarity from 0 (minimum) to 1 (maximum)
	 */
	private double compareOrthographic(String a, String b){
		return LevenshteinDistance.computeLevenshteinDistance(a, b);
	}
	
	/**
	 * Compares the names using their phonetic similarity, using the soundex algorithm.
	 * We have used an implementation of this algorithm provided by Apache.
	 * Attention: it only works for English
	 */
	private double comparePhonetic(String recognizedApp, String nameApp){		
	    Soundex soundex = new Soundex();
	    
	    //Returns the number of characters in the two encoded Strings that are the same. 
	    //This return value ranges from 0 to the length of the shortest encoded String: 0 indicates little or no similarity, 
	    //and 4 out of 4 (for example) indicates strong similarity or identical values. 
	    double sim=0;
		try {
			sim = soundex.difference(recognizedApp, nameApp);
		} catch (Exception e) {
			Log.e(LOG_TAG, "Error during soundex encoding. Similarity forced to 0");
			sim = 0;
		}
	    return sim/4;
	}
	
	/*************************************************************************************************************************************
	 * ASR processing methods
	 *************************************************************************************************************************************/
	
	/**
	 * Listens for names of apps. Depending of the recognition results, the methods processError, processReadyForSpeech 
	 * or processResults will be invoked.
	 * Speech recognition is carried out in US English because it is the only language for which phonetic similarity
	 * works
	 */
	private void startListeningForApps() {
		
		if(DEBUG) Log.d(LOG_TAG, "[VoiceLaunch]startListeningForApps()...");
		
		try{
			indicateListening();
			//Recognition model = Free form, Number of results = 1 (we will use the best result to perform the search)
			listen(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM, 1); //Start listening
		} catch (Exception e) {
			Toast toast = Toast.makeText(getApplicationContext(),"ASR could not be started: invalid params", Toast.LENGTH_SHORT);
			toast.show();
			changeButtonAppearanceToDefault();
			
			if(DEBUG) Log.e(LOG_TAG, "ASR could not be started: invalid params");
		}
	}
	
	/**
	 * When recognition is successful, it obtains the best recognition result (supposedly the name of an app),
	 * and sorts all apps installed on the device according to the similarity of their names to the one recognized
	 * (considering only the ones similar above a threshold).
	 * Then, it launches the app with highest similarity. If the similarities are all bellow the defined threshold, no app is launched and the user
	 * gets a feedback message in a Toast
	 */
	@Override
	public void processAsrResults(ArrayList<String> nBestList, float[] nBestConfidences) {
		if(DEBUG) Log.d(LOG_TAG, "[VoiceLaunch]processAsrResults()...");
			
		if(nBestList != null){
			if(nBestList.size() > 0){
				// 최적의 음성 인식 결과를 획득
				String bestResult = nBestList.get(0);
				if(DEBUG) Log.d(LOG_TAG, "[VoiceLaunch]processAsrResults():bestResult = " + bestResult);
				
				//GUI에서 얻은 유사도 파라미터에서 값을 읽음
				readGUIParameters();
		        
				//사용자 입력으로 내림차순의 유사도를 가진 앱 이름을 획득
				//String[] = [0] = name, [1] = package, [2] = similarity
				ArrayList<MyApp> sortedApps = getSimilarAppsSorted(bestResult);  
				
				//목록에 있는 매칭되는 앱 이름과 유사도 값을 표시
				showMatchingNames(sortedApps);
				        
				//가장 잘 매칭되는 앱을 실행(즉 하나라도 존재하면 실행)
				if(sortedApps.size() <= 0)
				{
					Toast toast = Toast.makeText(getApplicationContext(),"No app found with sufficiently similar name", Toast.LENGTH_SHORT);
					toast.show();
					Log.e(LOG_TAG, "No app has a name with similarity > "+similarityThreshold);
				} else {					
					launchApp(sortedApps.get(0));
				}
			}
		}
	}
	
	/**
	 * Launches the app indicated. 
	 * @param app see the MyApp class defined at the end of this file
	 */
	private void launchApp(MyApp app) {
		
		Intent launchApp = this.getPackageManager().getLaunchIntentForPackage(app.getPackageName());
		
		if (null != launchApp) {
		    try {  
		    	indicateLaunch(app.getName());
		    	Log.i(LOG_TAG, "Launching "+app.getName());
		    	startActivity(launchApp);
		    	//VoiceLaunch.this.finish();
		    } catch (Exception e) {  
		    	Toast.makeText(getBaseContext(), app.getName()+" could not be launched", Toast.LENGTH_LONG).show(); //Show user-friendly name
		    	Log.e(LOG_TAG, app.getName()+" could not be launched");
		    }                       
		}
	}
	
	@Override
	public void processAsrReadyForSpeech() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processAsrError(int errorCode) {
		
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
		changeButtonAppearanceToDefault();
		try {
			mTts.speak(errorMessage, "EN");
		} catch (Exception e) {
			Log.i(LOG_TAG, "Selected language not available, using the device's default");
		}
        Log.e(LOG_TAG, "Error when attempting listen: "+ errorMessage);
	}
	
	@Override
	public void onBackPressed() {

		super.onBackPressed();
		
		Intent intent = new Intent(getBaseContext(), VoiceLaunch.class);
		startActivity(intent);
	}
	
	@Override
	protected void onDestroy() {

		super.onDestroy();
		mTts.shutdown();
	}


	/***************************************************************************************************************************
	 * Auxiliary classes
	***************************************************************************************************************************/
	/**
	 * Represents each app to be considered for launching.
	 */
	private class MyApp {
		private String name;		//User-friendly name
		private String packageName;	//Full name
		private double similarity;	//Similarity of its user-friendly name with the recognized input
		
		MyApp(String name, String packageName, double similarity) {
			
			this.name = name;
			this.packageName = packageName;
			this.similarity = similarity;
		}
		
		String getName() {
			return name;
		}
		
		String getPackageName() {
			return packageName;
		}
		
		double getSimilarity() {
			return similarity;
		}
	}
	
	/**
	 * Comparator for apps considering the similarity of their names to the recognized input.
	 */
	private class AppComparator implements Comparator<MyApp> {

		@Override
		public int compare(MyApp app1, MyApp app2) {

			return (- Double.compare(app1.getSimilarity(), app2.getSimilarity())); // Multiply by -1 to get reverse ordering (from most to least similar)
		}
	}
}
