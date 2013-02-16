package com.kedzie.vbox.task;

import android.content.Context;

import com.kedzie.vbox.R;
import com.kedzie.vbox.api.IMachine;
import com.kedzie.vbox.api.jaxb.SessionState;
import com.kedzie.vbox.app.Utils;
import com.kedzie.vbox.machine.PreferencesActivity;
import com.kedzie.vbox.soap.VBoxSvc;

public class LaunchVMProcessTask extends DialogTask<IMachine, IMachine> {
	
	public LaunchVMProcessTask(Context activity, VBoxSvc vmgr) {
		super(LaunchVMProcessTask.class.getSimpleName(), activity, vmgr, R.string.progress_starting);
	}
	
	@Override 
	protected IMachine work(IMachine... m) throws Exception {
		if(!m[0].getSessionState().equals(SessionState.UNLOCKED))
			throw new RuntimeException("Machine session state is " + m[0].getSessionState());
		handleProgress( m[0].launchVMProcess(_vmgr.getVBox().getSessionObject(), IMachine.LaunchMode.headless) );
		_vmgr.getVBox().getPerformanceCollector().setupMetrics(new String[] { "*:" },  
				Utils.getIntPreference(_context.get(), PreferencesActivity.PERIOD), 
				Utils.getIntPreference(_context.get(), PreferencesActivity.COUNT), m[0]);
		return m[0];
	}
}