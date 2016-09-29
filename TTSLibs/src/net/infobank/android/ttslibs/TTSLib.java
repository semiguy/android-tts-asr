package net.infobank.android.ttslibs;

import java.util.Locale;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;

/**
 * @see http://gl.wikipedia.org/wiki/Singleton
 * @see http://developer.android.com/reference/android/speech/tts/TextToSpeech.html
 * @author cbcho
 *
 */
public class TTSLib implements OnInitListener {

	private static final String LOG_TAG = "TTSLib";
	private static final boolean DEBUG = true;
	
	private TextToSpeech mTTS;
	private static TTSLib singleton;
	
	/**
	 * Creates the single <code>TTSLib</code> instance and initializes the text to speech
	 * engine. It is private so that it cannot be invoked from outside the
	 * class and thus it is not possible to create new <code>TTSLib</code> objects.
	 * 
	 * @param ctx context of the interaction
	 */
	public TTSLib(Context ctx) {
		// TODO Auto-generated constructor stub
		mTTS = new TextToSpeech(ctx, (OnInitListener)this);
	}
	
	/**
	 * Returns the single <code>TTSLib</code> instance. If it did not exist, it creates it beforehand.
	 * 
	 * @param ctx context of the interaction
	 * @return reference to the single <code>TTSLib</code> instance
	 */
	public static TTSLib getInstance(Context ctx) {
		if(singleton == null) {
			singleton = new TTSLib(ctx);
		}
		
		return singleton;
	}
	
	/**
	 * Sets the locale for speech synthesis taking into account the language and country codes
	 * If the <code>countryCode</code> is null, it just sets the language, if the 
	 * <code>languageCode</code> is null, it uses the default language of the device
	 * If any of the codes are not valid, it uses the default language
	 * 
	 * @param languageCode a String representing the language code, e.g. EN
	 * @param countryCode a String representing the country code for the language used, e.g. US. 
	 * @throws Exception when the codes supplied cannot be used and the default locale is selected
	 */
	public void setLocale(String languageCode, String countryCode) throws Exception {
		if(languageCode == null) {
			setLocale(languageCode);
		} else {
			Locale lang = new Locale(languageCode, countryCode);
			if(mTTS.isLanguageAvailable(lang) == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE)				
				mTTS.setLanguage(lang);
			{
				setLocale();
				throw new Exception("Language or country code not supported, using default locale");
			}
		}
	}

	/**
	 * Sets the locale for speech synthesis taking into account the language code
	 * If the code is null or not valid, it uses the default language of the device
	 * 
	 * @param languageCode a String representing the language code, e.g. EN
	 * @throws Exception when the code supplied cannot be used and the default locale is selected
	 */
	public void setLocale(String languageCode) throws Exception {
		if(languageCode == null) {
			setLocale();
			throw new Exception("Language code was not provided, using default locale");
		} else {
			Locale lang = new Locale(languageCode);
			if(mTTS.isLanguageAvailable(lang) != TextToSpeech.LANG_MISSING_DATA 
					&& mTTS.isLanguageAvailable(lang) != TextToSpeech.LANG_NOT_SUPPORTED) {
				mTTS.setLanguage(lang);
			} else {
				setLocale();
				throw new Exception("Language code not supported, using default locale");
			}
		}
	}
	
	/**
	 * Sets the default language of the device as locale for speech synthesis
	 */
	public void setLocale() {
		mTTS.setLanguage(Locale.getDefault());
	}
	
	/**
	 * Synthesizes a text in the language indicated (or in the default language of the device
	 * if it is not available)
	 * 
	 * @param languageCode language for the TTS, e.g. EN
	 * @param text string to be synthesized
	 * @throws Exception when the code supplied cannot be used and the default locale is selected
	 */
	public void speak(String text, String languageCode) throws Exception {
		setLocale(languageCode);
		mTTS.speak(text, TextToSpeech.QUEUE_ADD, null);
	}
	
	/**
	 * Synthesizes a text using the default language of the device
	 * 
	 * @param text string to be synthesized
	 */
	public void speak(String text) {
		setLocale();
		mTTS.speak(text, TextToSpeech.QUEUE_ADD, null);
	}
	
	/**
	 * Stops the synthesizer if it is speaking
	 */
	public void stop() {
		if(mTTS.isSpeaking()) {
			mTTS.stop();
		} else {
			// nothing
		}
	}
	
	/**
	 * Stops the speech synthesis engine. It is important to call it, as
	 * it releases the native resources used.
	 * 
	 * This is necessary in order to force the creation of a new TTS instance after shutdown.
	 * It is useful for handling runtime changes such as a change in the orientation of the device,
	 * as it is necessary to create a new instance with the new context.
	 * See here: http://developer.android.com/guide/topics/resources/runtime-changes.html
	 */
	public void shutdown() {
		mTTS.stop();
		mTTS.shutdown();
		singleton = null;
	}

	/*
	 * A <code>TextToSpeech</code> instance can only be used to synthesize text once 
	 * it has completed its initialization. 
	 * (non-Javadoc)
	 * @see android.speech.tts.TextToSpeech.OnInitListener#onInit(int)
	 */
	@Override
	public void onInit(int status) {
		if(status != TextToSpeech.ERROR) {
			setLocale();
		} else {
			if(DEBUG) Log.e(LOG_TAG, "TTSLib()onInit : Error creating the TTS, status = " + status);
		}
		
	}	
}
