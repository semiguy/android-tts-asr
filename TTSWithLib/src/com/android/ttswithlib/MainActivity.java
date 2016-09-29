package com.android.ttswithlib;

import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import net.infobank.android.ttslibs.TTSLib;

public class MainActivity extends Activity {
	
	private static final String LOGTAG = "TTSWithLib";
	
	private TTSLib myTTS;
	String languageCode;
	
	ArrayList<String>locales = new ArrayList<String>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		setLocalesConsidered();
		setButtons();
		
		myTTS = TTSLib.getInstance(this);
	}
	
	private void setLocalesConsidered() {
		
		String defaultLocaleInDevice = Locale.getDefault().getLanguage().toUpperCase();
		Log.d(LOGTAG, "[MainActivity]setLocalesConsidered() : defaultLocaleInDevice = " + defaultLocaleInDevice);
		locales.add(defaultLocaleInDevice);
		
		if(!defaultLocaleInDevice.equals("KO")) {
			locales.add("KO");
		}
		
		if(!defaultLocaleInDevice.equals("EN")) {
			locales.add("EN");
		}
		
		if(!defaultLocaleInDevice.equals("ES")) {
			locales.add("ES");
		}
		
		setLocaleList();
	}
	
	private void setLocaleList() {
		final ListView listView = (ListView)findViewById(R.id.locale_listview);
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, locales);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				
				languageCode = (String)listView.getItemAtPosition(position);
			}
		});
		
		listView.setItemChecked(0, true);
		languageCode = (String)listView.getItemAtPosition(0);
	}
	
	private void setButtons() {
		setSpeakButton();
		setStopButton();
	}
	
	private void setSpeakButton() {
		Button speakButton = (Button)findViewById(R.id.speak_button);
		
		speakButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				EditText inputText = (EditText)findViewById(R.id.input_text);
				String text = inputText.getText().toString();
				
				if(text != null && text.length() > 0) {
					try {
						myTTS.speak(text, languageCode);
					} catch (Exception e) {
						Toast toast = Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT);
						toast.show();
					}
				}
				
			}
		});
	}
	
	private void setStopButton() {
		Button resumeButton = (Button)findViewById(R.id.stop_button);
		
		resumeButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				myTTS.stop();
			}
		});
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		myTTS.shutdown();
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
}
