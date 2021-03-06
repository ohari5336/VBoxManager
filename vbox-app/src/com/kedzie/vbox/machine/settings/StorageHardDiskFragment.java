package com.kedzie.vbox.machine.settings;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.kedzie.vbox.R;
import com.kedzie.vbox.api.IMachine;
import com.kedzie.vbox.api.IMedium;
import com.kedzie.vbox.api.IStorageController;
import com.kedzie.vbox.api.jaxb.IMediumAttachment;
import com.kedzie.vbox.api.jaxb.IMediumAttachment.Slot;
import com.kedzie.vbox.api.jaxb.StorageBus;
import com.kedzie.vbox.app.Utils;
import com.kedzie.vbox.task.ActionBarTask;
import com.kedzie.vbox.task.DialogTask;

/**
 * Hard disk medium details
 * 
 * @apiviz.stereotype fragment
 */
public class StorageHardDiskFragment extends SherlockFragment {

	/**
	 * Load medium details
	 */
	class LoadInfoTask extends ActionBarTask<Void, Void> {

		public LoadInfoTask() { 
			super(getSherlockActivity(), _machine.getAPI()); 
		}

		@Override 
		protected Void work(Void...params) throws Exception {
			Utils.cacheProperties(_attachment.getMedium());
			_controller = _machine.getStorageControllerByName(_attachment.getController());
			_controller.getBus();
			_controller.getMaxPortCount();
			_controller.getMaxDevicesPerPortCount();
			_attachments = _machine.getMediumAttachmentsOfController(_controller.getName());
			return null;
		}
		@Override
		protected void onSuccess(Void result) {
			populate();
		}
	}

	/**
	 * Move the medium to a different slot within controller.
	 */
	class MoveTask extends ActionBarTask<Slot, Void> {

		public MoveTask() { 
			super(getSherlockActivity(), _machine.getAPI()); 
		}

		@Override 
		protected Void work(Slot...params) throws Exception {
			if(params[0].equals(_attachment.getSlot()))
				return null;
			_machine.detachDevice(_controller.getName(), _attachment.getPort(), _attachment.getDevice());
			_machine.attachDevice(_controller.getName(), params[0].port, params[0].device, _attachment.getType(), _attachment.getMedium());
			return null;
		}
	}

	/**
	 * Move the medium to a different slot within controller.
	 */
	class ListMediumsTask extends ActionBarTask<Void, List<IMedium>> {

		public ListMediumsTask() { 
			super(getSherlockActivity(), _machine.getAPI()); 
		}

		@Override 
		protected List<IMedium> work(Void...params) throws Exception {
			List<IMedium> mediums = _vmgr.getVBox().getHardDisks();
			for(IMedium m : mediums) {
				m.getName(); m.getBase();
			}
			return mediums;
		}
		
		@Override
		protected void onSuccess(final List<IMedium> result) {
			super.onSuccess(result);
			final CharSequence []items = new CharSequence[result.size()];
			for(int i=0; i<result.size(); i++)
				items[i] = result.get(i).getName();
			new AlertDialog.Builder(getActivity())
				.setTitle("Select Hard Disk")
				.setItems(items, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						IMedium medium = result.get(item);
						Utils.toastLong(getActivity(), "Clicked: " + medium.getName());
						new MountTask().execute(medium);
					}
				}).show();
		}
	}

	/**
	 * Move the medium to a different slot within controller.
	 */
	class MountTask extends DialogTask<IMedium, Void> {

		public MountTask() { 
			super(getSherlockActivity(), _machine.getAPI(), R.string.progress_mounting_medium); 
		}

		@Override 
		protected Void work(IMedium...params) throws Exception {
			if(_attachment.getMedium()!=null)
				_machine.unmountMedium(_controller.getName(), _attachment.getPort(), _attachment.getDevice(),  false);
			_machine.mountMedium(_controller.getName(), _attachment.getPort(), _attachment.getDevice(), params[0], false);
			_attachment.setMedium(params[0]);
			Utils.cacheProperties(params[0]);
			return null;
		}
		
		@Override
		protected void onSuccess(Void result) {
			super.onSuccess(result);
			populate();
		}
	}

	private IMachine _machine;
	private IMediumAttachment _attachment;
	private IStorageController _controller;
	private ArrayList<IMediumAttachment> _attachments;

	private View _view;
	private Spinner _slotSpinner;
	private ArrayList<Slot> _slots;
	private ArrayAdapter<Slot> _slotAdapter;

	private ImageButton _mountButton;
//	private CheckBox _solidStateCheckbox;
	private TextView _storageTypeText;
	private TextView _virtualSizeText;
	private TextView _actualSizeText;
	private TextView _locationText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		_machine = getArguments().getParcelable(IMachine.BUNDLE);
		_attachment = getArguments().getParcelable(IMedium.BUNDLE);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		_view = inflater.inflate(R.layout.settings_storage_details_harddisk, null);
		_slotSpinner = (Spinner)_view.findViewById(R.id.storage_port);
		_mountButton = (ImageButton)_view.findViewById(R.id.storage_mount);
		_mountButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new ListMediumsTask().execute();
			}
		});
//		_solidStateCheckbox = (CheckBox)_view.findViewById(R.id.storage_solid_state);
		_storageTypeText = (TextView)_view.findViewById(R.id.storage_type);
		_virtualSizeText = (TextView)_view.findViewById(R.id.storage_virtual_size);
		_actualSizeText = (TextView)_view.findViewById(R.id.storage_actual_size);
		_locationText = (TextView)_view.findViewById(R.id.storage_location);
		return _view;
	}

	@Override
	public void onStart() {
		super.onStart();
		new LoadInfoTask().execute();
	}

	private void populate() {
		int devicesPerPort = _controller.getMaxDevicesPerPortCount();
		
		_slots = new ArrayList<Slot>(devicesPerPort*_controller.getMaxPortCount());
		for(int i=0; i<_controller.getMaxPortCount(); i++) {
			for(int j=0; j<devicesPerPort; j++) {
				Slot slot = new Slot(i, j);
				boolean isUsed=false;
				for(IMediumAttachment a : _attachments) {
					if(a.getSlot().equals(slot) && !a.getMedium().equals(_attachment.getMedium())) {
						isUsed=true;
						break;
					}
				}
				if(!isUsed)
					_slots.add(slot);
				if(devicesPerPort==1)
					slot.name = new StringBuffer(_controller.getBus().toString()).append(" Port ").append(i).toString(); 
				else if (_controller.getBus().equals(StorageBus.IDE)) {
					slot.name = (i==0 ? "Primary " : "Secondary ") + (j==0 ? "MASTER" : "SLAVE");
				}
			}
		}
		_slotAdapter = new ArrayAdapter<Slot>(getActivity(), android.R.layout.simple_spinner_item, _slots);
		_slotSpinner.setAdapter(_slotAdapter);
		_slotSpinner.setSelection(_slots.indexOf(_attachment.getSlot()));
		_slotSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override public void onNothingSelected(AdapterView<?> parent) {}
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				new MoveTask().execute(_slotAdapter.getItem(position));
			}
		});
		_storageTypeText.setText( _attachment.getMedium().getType().toString() );
		_virtualSizeText.setText(_attachment.getMedium().getLogicalSize()/1024+" MB");
		_actualSizeText.setText(_attachment.getMedium().getSize()/1024+" MB");
		_locationText.setText(_attachment.getMedium().getBase().getLocation());
	}
}
