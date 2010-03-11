/*
 *  Copyright (C) 2009 Sebastian Dörner, Julius Müller, Johannes Steffen
 *
 *  This file is part of Mango.
 *
 *  Mango is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Mango is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Mango.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package de.mango.gui;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnKeyListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import de.mango.R;
import de.mango.business.Goal;
import de.mango.business.GoalCrud;
import de.mango.business.ImExport;
import de.mango.business.ImageHandling;

public class Main extends Activity implements OnClickListener,
		OnCreateContextMenuListener
{
	/**
	 * Enable/disable logging. The compiler will strip all logging if disabled.
	 */
	public static final boolean DEBUG = false;
	public static final String TAG = "Mango";

	private static final int CreateGoalMenu = Menu.FIRST;
	private static final int CreateSubGoalMenu = Menu.FIRST + 1;
	private static final int ModifyGoalMenu = Menu.FIRST + 2;
	private static final int DeleteGoalMenu = Menu.FIRST + 3;
	private static final int ExportToXMLMenu = Menu.FIRST + 4;
	private static final int ExportToICSMenu = Menu.FIRST + 5;
	private static final int SendXMLMenu = Menu.FIRST + 6;
	private static final int SendICSMenu = Menu.FIRST + 7;
	private static final int ExportToCalendarMenu = Menu.FIRST + 8;
	private static final int ImportFromXMLMenu = Menu.FIRST + 9;
	private static final int ShowDetails = Menu.FIRST + 10;
	private static final int AboutWindow = Menu.FIRST + 11;
	private static final int REQUEST_CODE_PICK_XML_EXPORT_FILE = 1;
	private static final int REQUEST_CODE_PICK_ICS_FILE = 2;
	private static final int REQUEST_CODE_PICK_XML_IMPORT_FILE = 3;
	private GoalCrud crud;
	private ImageAdapter mAdapter;
	private AlertDialog.Builder mAlertDialogBuilder;
	/**
	 * Indicates whether the openintents.org file picker intent is available
	 */
	private boolean mPickFileAvailable;
	/**
	 * True if emptymain.xml is shown because GoalCrud is empty. 
	 */
	private boolean mEmptyScreen = true;

	/**
	 * Checks if a handler for a given intent action is available. 
	 * @param context Context to get the package manager.
	 * @param action Intent Action, e.g. org.openintents.action.PICK_FILE
	 * @return True if the given Action can be handled.
	 */
	public static boolean isIntentAvailable(Context context, String action)
	{
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
				PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		//this class variable is needed in onOptionsItemSelected, so make sure to set it here
		mPickFileAvailable = isIntentAvailable(this,"org.openintents.action.PICK_FILE");

		menu.add(Menu.NONE, CreateGoalMenu, Menu.NONE,
				R.string.Menu_create_new_goal);
		SubMenu submenu = menu.addSubMenu(R.string.Menu_export);
		submenu.add(Menu.NONE, ExportToXMLMenu,
				Menu.NONE, R.string.Menu_export_xml);
		submenu.add(Menu.NONE, ExportToICSMenu, Menu.NONE,
				R.string.Menu_export_ics);
		submenu.add(Menu.NONE, SendXMLMenu, Menu.NONE, R.string.Menu_send_xml);
		submenu.add(Menu.NONE, SendICSMenu, Menu.NONE, R.string.Menu_send_ics);
		menu.add(Menu.NONE, ImportFromXMLMenu, Menu.NONE,R.string.Menu_import_xml);
		menu.add(Menu.NONE,AboutWindow, Menu.NONE, R.string.Menu_about);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		Intent i = null;
		switch (item.getItemId())
		{
		case CreateGoalMenu:
			//ensure we create a new tlg
			GoalCrud.currentGoal = null;
			i = new Intent(this, Create.class);
			startActivityForResult(i, 0);
			break;
		case ExportToXMLMenu:
			if (mPickFileAvailable)
			{
				i = new Intent("org.openintents.action.PICK_FILE");
				i.setData(Uri.parse("file:///sdcard/goals.mango"));
				i.putExtra("org.openintents.extra.TITLE",
						R.string.Please_select_a_file);
				startActivityForResult(i, REQUEST_CODE_PICK_XML_EXPORT_FILE);
			}
			else
			{
				if (ImExport.exportToXML(crud, "/sdcard/goals.mango", this))
					Toast.makeText(this, getResources().getString(R.string.Main_goals_exported_to)+"/sdcard/goals.mango", Toast.LENGTH_LONG).show();
			}
			break;
		case ExportToICSMenu:
			if (mPickFileAvailable)
			{
				i= new Intent("org.openintents.action.PICK_FILE");
				i.setData(Uri.parse("file:///sdcard/goals.ics"));
				i.putExtra("org.openintents.extra.TITLE", R.string.Please_select_a_file);
				startActivityForResult(i, REQUEST_CODE_PICK_ICS_FILE);
			}
			else
			{
				if (ImExport.exportToICS(crud, this, "/sdcard/goals.ics", true))
					Toast.makeText(this, getResources().getString(R.string.Main_goals_exported_to) +"/sdcard/goals.ics", Toast.LENGTH_LONG).show();
			}
			break;
		case SendXMLMenu:
			// make sure the file is recent
			ImExport.exportToXML(crud, "/sdcard/goals.mango", this);
			i = new Intent(Intent.ACTION_SEND);
			i.setType("text/xml");
			i.putExtra(Intent.EXTRA_STREAM, Uri
					.parse("file:///sdcard/goals.mango"));
			i = Intent.createChooser(i, getString(R.string.How_to_send_the_goals));
			startActivity(i);
			break;
		case SendICSMenu:
			// make sure the file is recent
			ImExport.exportToICS(crud, this, "/sdcard/goals.ics", true);
			i = new Intent(Intent.ACTION_SEND);
			i.setType("text/calendar");
			i.putExtra(Intent.EXTRA_STREAM, Uri
					.parse("file:///sdcard/goals.ics"));
			i= Intent.createChooser(i, getString(R.string.How_to_send_the_goals));
			startActivity(i);
			break;
		case ImportFromXMLMenu:
			if (mPickFileAvailable)
			{
				i = new Intent("org.openintents.action.PICK_FILE");
				i.setData(Uri.parse("file:///sdcard/"));
				i.putExtra("org.openintents.extra.TITLE",
					R.string.Please_select_a_file);
				startActivityForResult(i, REQUEST_CODE_PICK_XML_IMPORT_FILE);
			}
			else
			{
				if (ImExport.importFromXML(crud, "/sdcard/goals.mango", this))
				{
					crud.setDataChanged();
					if (mEmptyScreen)
						inflateGridView();
					else
						mAdapter.notifyDataSetChanged();
					Toast.makeText(this, R.string.Main_import_successful, Toast.LENGTH_SHORT).show();
				}
				else
					Toast.makeText(this, getString(R.string.Main_import_failed,"/sdcard/goals.mango"), Toast.LENGTH_LONG).show();
			}
			break;
		case AboutWindow:
			AlertDialog dialog = mAlertDialogBuilder.create();
			dialog.setTitle(R.string.Menu_about);
			dialog.setMessage(getString(R.string.Main_About_Text));
			dialog.setButton(Dialog.BUTTON_NEUTRAL, getString(R.string.Button_ok), new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which)
				{
					dialog.dismiss();
				}
			});
			dialog.show();
			break;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(Menu.NONE, ShowDetails, Menu.NONE, R.string.Menu_show_details);
		menu.add(Menu.NONE, CreateSubGoalMenu, Menu.NONE, R.string.Menu_create_subgoal);
		menu.add(Menu.NONE, ModifyGoalMenu, Menu.NONE, R.string.Menu_modify_goal);
		menu.add(Menu.NONE, DeleteGoalMenu, Menu.NONE, R.string.Menu_delete_goal);
		menu.add(Menu.NONE, ExportToCalendarMenu, Menu.NONE,R.string.Menu_export_to_calendar);

	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		final Goal g = crud.getTopLevelGoals().get(
				(Integer) ((ViewHolder) (info.targetView
						.getTag())).textView.getTag());
		switch (item.getItemId())
		{
			case CreateSubGoalMenu:
				Intent i = new Intent(this, Create.class);
				GoalCrud.currentGoal = g;
				startActivityForResult(i, 0);
				break;
			case ModifyGoalMenu:
				Intent i2 = new Intent(this, Create.class);
				i2.putExtra("modify", true);
				GoalCrud.currentGoal = g;
				startActivityForResult(i2, 0);
				break;
			case DeleteGoalMenu:
				mAlertDialogBuilder.setCancelable(true);
				mAlertDialogBuilder
						.setMessage(getString(R.string.Really_delete, g.getName()));
				AlertDialog dialog = mAlertDialogBuilder.create();
				dialog.setButton(AlertDialog.BUTTON_POSITIVE,
						getString(R.string.Yes_delete_it),
						new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog,
									int which)
							{
								if (which == AlertDialog.BUTTON_POSITIVE)
								{
									crud.removeFromTree(g, Main.this);
									mAdapter.notifyDataSetChanged();
								}
							}
						});
				dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.No_keep_it),
						(DialogInterface.OnClickListener) null);
				dialog.show();
				break;
			case ShowDetails:
				GoalCrud.currentGoal = g;
				Intent i3 = new Intent(this, Detail.class);
				startActivity(i3);
				break;
			case ExportToCalendarMenu:
				Intent i4 = new Intent("android.intent.action.EDIT");
				i4.setType("vnd.android.cursor.item/event");
				g.putCalendarExtras(i4);
				i4 = Intent.createChooser(i4, getString(R.string.Menu_choose_calendar_application));
				startActivity(i4);
				break;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mAlertDialogBuilder = new AlertDialog.Builder(this);

		crud = GoalCrud.getInstance(this);

		if (mEmptyScreen = crud.isEmpty())
		{
			setContentView(R.layout.emptymain);
		}
		else
			inflateGridView();
	}
	/**
	 * Replace the current Layout with standard Main Grid Layout.
	 */
	public void inflateGridView()
	{
		setContentView(de.mango.R.layout.main);
		GridView gridview = (GridView) findViewById(R.main.goalGridview);
		mAdapter = new ImageAdapter(this, crud);
		gridview.setAdapter(mAdapter);
		registerForContextMenu(gridview);
		mEmptyScreen = false;

		gridview.setOnKeyListener(new OnKeyListener()
		{
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && event.getAction()==KeyEvent.ACTION_UP)
				{
					GridView gv = (GridView) v;
					LinearLayout ll = (LinearLayout) gv.getSelectedView();
					if (ll==null)
						return false;
					Main.this.onClick(ll.getChildAt(0));
					return true;
				}
				return false;
			}
		});
	}

	public void onClick(View v)
	{
		if (DEBUG)
			Log.d(TAG, "Tag of the clicked view: " + ((Integer) v.getTag()).toString());
		Intent i = new Intent(this, Hierarchy.class);
		i.putExtra("topLevelGoal", ((Integer) v.getTag()));
		startActivityForResult(i, 0);

	}

	@Override
	protected void onPause()
	{
		crud.saveToDisk(this);
		super.onPause();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch (requestCode)
		{
			case REQUEST_CODE_PICK_XML_EXPORT_FILE:
				if (resultCode == RESULT_OK)
				{
					String uri = data.getData().getEncodedPath();
					if (ImExport.exportToXML(crud, uri, this))
						Toast.makeText(this, R.string.Main_goals_saved_to+uri, Toast.LENGTH_LONG).show();
				}
				break;
			case REQUEST_CODE_PICK_ICS_FILE:
				if (resultCode == RESULT_OK)
				{
					String uri = data.getData().getEncodedPath();
					if (ImExport.exportToICS(crud, this, uri, true))
						Toast.makeText(this, R.string.Main_goals_exported_to+uri, Toast.LENGTH_LONG).show();
				}
				break;
			case REQUEST_CODE_PICK_XML_IMPORT_FILE:
				if (resultCode == RESULT_OK)
				{
					Uri uri = data.getData();
					if (ImExport.importFromXML(crud, uri.getEncodedPath(), this))
					{
						crud.setDataChanged();
						if (mEmptyScreen)
							inflateGridView();
						else
							mAdapter.notifyDataSetChanged();
						Toast.makeText(this, R.string.Main_import_successful, Toast.LENGTH_SHORT).show();
					}
				}
				break;
			default:
				switch (resultCode)
				{
					case RESULT_CANCELED:
						break;
					case Create.RESULT_CREATED:
						if (mEmptyScreen)
							inflateGridView();
						else
							mAdapter.notifyDataSetChanged();
						break;
					case Create.RESULT_MODIFIED:
					case Hierarchy.RESULT_TOP_LEVEL_GOALS_CHANGED:
						mAdapter.notifyDataSetChanged();
						break;
				}
		}
	}

	/**
	 * Class to encapsulate the contents of one cell of the GridView or of one Line of Hierarchy List View.
	 * 
	 */
	static class ViewHolder
	{
		TextView textView;
		/**
		 * Expandbutton in Hierarchy. Visualization Image in Main.
		 */
		ImageButton expandButton;
	}

	public class ImageAdapter extends BaseAdapter
	{
		private Main mMainDialog;
		private GoalCrud mCrud;
		// inflater for rolling out the layout
		private LayoutInflater mInflater;
		private Bitmap mNoPic;

		public ImageAdapter(Main mainActivity, GoalCrud crud)
		{
			mMainDialog = mainActivity;
			mCrud = crud;
			mInflater = getLayoutInflater();
			mNoPic = BitmapFactory.decodeResource(getResources(), R.drawable.nopic);
		}

		public int getCount()
		{
			return mCrud.getTopLevelGoals().size();
		}

		public Object getItem(int position)
		{
			return mCrud.getTopLevelGoals().get(position);
		}

		public long getItemId(int position)
		{
			return position;
		}

		// create a new ImageView for each item referenced by the Adapter
		public View getView(int position, View convertView, ViewGroup parent)
		{
			ViewHolder viewHolder;
			if (convertView == null)
			{ // if it's not recycled, initialize some general attributes
				LinearLayout ll = (LinearLayout) mInflater.inflate(
						R.layout.maingridcell, null);

				viewHolder = new ViewHolder();
				viewHolder.expandButton = (ImageButton) ll
						.findViewById(R.mainGridCell.imageButton);
				viewHolder.textView = (TextView) ll
						.findViewById(R.mainGridCell.textView);
				// set size for the image, the TextView will be automatically
				// adapted
				viewHolder.expandButton.setLayoutParams(new LinearLayout.LayoutParams(85, 85));

				viewHolder.expandButton.setOnClickListener(mMainDialog);
				viewHolder.textView.setOnClickListener(mMainDialog);
				viewHolder.expandButton.setLongClickable(true);
				viewHolder.textView.setLongClickable(true);

				ll.setTag(viewHolder);
				convertView = ll;
			} else
			{
				viewHolder = (ViewHolder) convertView.getTag();
			}
			// load goal information
			Goal goal = mCrud.getTopLevelGoals().get(position);
			viewHolder.textView.setTag(position);
			viewHolder.expandButton.setTag(position);

			viewHolder.textView.setText(goal.getName());
			Bitmap bitmap = ((goal.getImageName().equals(""))) ? mNoPic : ImageHandling.loadLocalBitmap(goal.getImageName(), getBaseContext());
			viewHolder.expandButton.setImageBitmap(bitmap);
			return convertView;
		}
	}

}
