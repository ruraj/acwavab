package com.dteam.acwavab;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Settings extends PreferenceActivity{

	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
	}
}
