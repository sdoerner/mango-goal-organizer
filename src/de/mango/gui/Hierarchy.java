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

import java.util.ArrayList;
import java.util.Vector;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import de.mango.R;
import de.mango.business.Goal;
import de.mango.business.GoalCrud;
import de.mango.business.GoalProvider;
import de.mango.business.ImageHandling;

public class Hierarchy extends ListActivity implements OnClickListener
{
	public static final int RESULT_TOP_LEVEL_GOALS_CHANGED = RESULT_FIRST_USER;
	private static final int CreateSubGoalMenu = Menu.FIRST;
	private static final int ModifyGoalMenu = Menu.FIRST + 1;
	private static final int DeleteGoalMenu = Menu.FIRST + 2;
	private static final int ExportToCalendarMenu = Menu.FIRST + 3;
	// track if we must return RESULT_TOP_LEVEL_GOALS_CHANGED
	private static final int REQUEST_CODE_CHANGE_TLG = 1;
	private Goal mTopLevelGoal;
	private GoalProvider mGoalProvider;
	private HierarchicalListAdapter mAdapter;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu,
	 * android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(Menu.NONE, CreateSubGoalMenu, Menu.NONE, R.string.Menu_create_subgoal);
		menu.add(Menu.NONE, ModifyGoalMenu, Menu.NONE, R.string.Menu_modify_goal);
		menu.add(Menu.NONE, DeleteGoalMenu, Menu.NONE, R.string.Menu_delete_goal);
		menu.add(Menu.NONE, ExportToCalendarMenu, Menu.NONE, R.string.Menu_export_to_calendar);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		final Goal g = (Goal) ((Main.ViewHolder) (info.targetView.getTag())).textView.getTag();
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
			startActivityForResult(i2, g.getParent() == null ? REQUEST_CODE_CHANGE_TLG : 0);
			break;
		case DeleteGoalMenu:
			// show "Are you sure?" dialog
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
			dialogBuilder.setCancelable(true);
			dialogBuilder.setMessage(getString(R.string.Really_delete, g.getName()));
			AlertDialog dialog = dialogBuilder.create();
			dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.Yes_delete_it),
					new DialogInterface.OnClickListener()
					{
						public void onClick(DialogInterface dialog, int which)
						{
							if (which == AlertDialog.BUTTON_POSITIVE)
							{
								// delete the goal
								GoalCrud c = GoalCrud.getInstance(Hierarchy.this);
								if (c.removeFromTree(g, Hierarchy.this))
								{
									setResult(RESULT_TOP_LEVEL_GOALS_CHANGED);
									finish();
								}
								else
									mAdapter.notifyDataSetChanged();
							}
						}
					});
			dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.No_keep_it),
					(DialogInterface.OnClickListener) null);
			dialog.show();
			break;
		case ExportToCalendarMenu:
			Intent i3 = new Intent("android.intent.action.EDIT");
			i3.setType("vnd.android.cursor.item/event");
			g.putCalendarExtras(i3);
			i3 = Intent.createChooser(i3, getString(R.string.Menu_choose_calendar_application));
			startActivity(i3);
			break;
		}
		return super.onContextItemSelected(item);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(de.mango.R.layout.hierarchy);
		mGoalProvider = new GoalProvider(this);
		Intent intent = this.getIntent();

		if (intent.getExtras().containsKey("topLevelGoal")) {
			final long id =(Long) intent.getExtras().get("topLevelGoal");
			mTopLevelGoal = mGoalProvider.getGoalWithId(id);
		}
		else
		{
			setResult(RESULT_CANCELED);
			finish();
		}

		mAdapter = new HierarchicalListAdapter(this, mTopLevelGoal, mGoalProvider);
		setListAdapter(mAdapter);
		registerForContextMenu(getListView());
		getListView().setLongClickable(true);
		getListView().setOnKeyListener(new OnKeyListener()
		{
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && event.getAction()==KeyEvent.ACTION_UP)
				{
					ListView lv = (ListView)v;
					LinearLayout ll = (LinearLayout) lv.getSelectedView();
					if (ll==null)
						return false;
					Hierarchy.this.onClick(ll.getChildAt(1));
					return true;
				}
				if ((keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) && event.getAction()==KeyEvent.ACTION_UP)
				{
					ListView lv = (ListView) v;
					LinearLayout ll = (LinearLayout) lv.getSelectedView();
					if (ll == null)
						return false;
					if (ll.getChildCount()>0)
					{
						ImageButton ib = (ImageButton) ll.getChildAt(0);
						if (ib.getVisibility() == ImageButton.VISIBLE)
						{
							ListEntry entry = (ListEntry) ll.getChildAt(0).getTag();
							if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
									&& entry.expanded == true)
								mAdapter.collapseEntry(entry);
							if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
									&& entry.expanded == false)
								mAdapter.expandEntry(entry);
						}
					}
					return true;
				}
				return false;
			}
		});
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause()
	{
		GoalCrud.getInstance(this).saveToDisk(this);
		super.onPause();
	}

	/* (non-Javadoc)
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	public void onClick(View v)
	{
		//show details for clicked goal
		Intent i = new Intent(this, Detail.class);
		i.putExtra("goalId", (Long) v.getTag());
		startActivityForResult(i, 0);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch (resultCode)
		{
		case RESULT_CANCELED:
			break;
		case Create.RESULT_MODIFIED:
		case Create.RESULT_CREATED:
			// TODO: we could do sth more intelligent here, preserving the
			// current expand/collapse-state but still adapting the
			// information
			// this code would probably belong into
			// HierarchicalListAdapter.notifyDataSetChanged
			mAdapter.notifyDataSetChanged();
			//propagate changes to Main Activity
			if (requestCode == REQUEST_CODE_CHANGE_TLG)
				setResult(RESULT_TOP_LEVEL_GOALS_CHANGED);
			break;
		}
	}

	//---------------------------------hierarchical list stuff------------------------------------
	
	/**
	 * Encapsulates additional information for each line in the list view.
	 */
	static class ListEntry
	{
		public ListEntry(Goal goal, int offset, boolean expanded)
		{
			this.goal = goal;
			this.offset = offset;
			this.expanded = expanded;
		}

		public Goal goal;
		public int offset;
		public boolean expanded;
	}

	class HierarchicalListAdapter extends BaseAdapter implements OnClickListener
	{
		//pixels used as indent for each level 
		final static int INDENT = 20;
		final Bitmap mPlusBitmap;
		final Bitmap mMinusBitmap;
		final BitmapDrawable mNoPicBitmap;
		final LayoutInflater mInflater;

		final Hierarchy mHierarchy;
		final Goal mTopLevelGoal;
		final GoalProvider mGoalProvider;
		/**
		 * Currently show goals in order (direct connection to the list view)
		 */
		Vector<ListEntry> mCurrentlyShownGoals;

		public HierarchicalListAdapter(Hierarchy hierarchy, Goal topLevelGoal, GoalProvider gp)
		{
			mInflater = getLayoutInflater();
			mPlusBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.plus);
			mMinusBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.minus);
			mNoPicBitmap = new BitmapDrawable(BitmapFactory.decodeResource(getResources(),
					R.drawable.nopic));
			mNoPicBitmap.setBounds(0, 0, 50, 50);

			mHierarchy = hierarchy;
			mTopLevelGoal = topLevelGoal;
			mGoalProvider = gp;
			mCurrentlyShownGoals = new Vector<ListEntry>();
			populate();
		}

		/**
		 * Fill the list view with the children of current mTopLevelGoal.
		 */
		private void populate()
		{
			ArrayList<Goal> c = mGoalProvider.getChildGoals(mTopLevelGoal.getId());
			boolean hasChildren = c != null && !c.isEmpty();
			mCurrentlyShownGoals.add(new ListEntry(mTopLevelGoal, 0, hasChildren));
			if (hasChildren)
				for (Goal g : c)
				{
					mCurrentlyShownGoals.add(new ListEntry(g, INDENT, false));
				}
		}

		/* (non-Javadoc)
		 * @see android.widget.Adapter#getCount()
		 */
		public int getCount()
		{
			return mCurrentlyShownGoals.size();
		}

		/* (non-Javadoc)
		 * @see android.widget.Adapter#getItem(int)
		 */
		public Object getItem(int position)
		{
			return mCurrentlyShownGoals.get(position);
		}

		/* (non-Javadoc)
		 * @see android.widget.Adapter#getItemId(int)
		 */
		public long getItemId(int position)
		{
			return position;
		}

		/* (non-Javadoc)
		 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
		 */
		public View getView(int position, View convertView, ViewGroup parent)
		{
			Main.ViewHolder viewHolder;
			if (convertView == null)
			{ // if it's not recycled, initialize some general attributes
				LinearLayout ll = (LinearLayout) mInflater.inflate(R.layout.hierarchyline, null);

				viewHolder = new Main.ViewHolder();
				viewHolder.textView = (TextView) ll.findViewById(R.hierarchyline.textView);
				viewHolder.expandButton = (ImageButton) ll
						.findViewById(R.hierarchyline.extendButton);
				// needed to get the right width if the button is invisible
				viewHolder.expandButton.setImageBitmap(mPlusBitmap);

				viewHolder.expandButton.setOnClickListener(this);
				viewHolder.textView.setOnClickListener(mHierarchy);
				// don't registerForContextMenu() here or the received MenuInfo
				// will be null
				// this way the widget is still longClickable, but the Event
				// comes from the ListView, which fills in the MenuInfo
				viewHolder.textView.setLongClickable(true);

				ll.setTag(viewHolder);
				convertView = ll;
			}
			else
			{
				viewHolder = (Main.ViewHolder) convertView.getTag();
			}
			// load goal information
			ListEntry entry = mCurrentlyShownGoals.get(position);
			Goal goal = entry.goal;
			// expand/collapse stuff
			convertView.setPadding(entry.offset, 0, 0, 0);
			if (mGoalProvider.hasChildren(entry.goal.getId()))
			{
				viewHolder.expandButton.setVisibility(View.VISIBLE);
				viewHolder.expandButton.setImageBitmap(entry.expanded ? mMinusBitmap : mPlusBitmap);
				viewHolder.expandButton.setTag(entry);
			}
			else
			{
				viewHolder.expandButton.setVisibility(View.INVISIBLE);
			}

			// apply color scheme
			viewHolder.textView.setBackgroundColor(getResources().getColor(
					goal.getCompletionColor()));

			// show goal information
			viewHolder.textView.setText(goal.getName());
			viewHolder.textView.setTag(goal.getId());

			BitmapDrawable d;
			if (goal.getImageName().equals("") || goal.getImageName().equals(null))
				d = mNoPicBitmap;
			else
			{
				d = new BitmapDrawable(ImageHandling.loadLocalBitmap(goal.getImageName(),
						mHierarchy));
				d.setBounds(0, 0, 50, 50);
			}
			viewHolder.textView.setCompoundDrawables(d, null, null, null);
			return convertView;
		}

		public void notifyDataSetChanged()
		{
			// initialize shown goals
			mCurrentlyShownGoals.clear();
			this.populate();
			super.notifyDataSetChanged();
		}

		public void onClick(View v)
		{
			// expand or collapse was clicked
			ListEntry entry = (ListEntry) v.getTag();
			if (entry.expanded)
				collapseEntry(entry);
			else
				expandEntry(entry);
		}

		/**
		 * Expands the given list entry
		 * @param entry List entry to be expanded
		 */
		private void expandEntry(ListEntry entry)
		{
			Vector<ListEntry> currentlyShownGoals = mCurrentlyShownGoals;
			ArrayList<Goal> children = mGoalProvider.getChildGoals(entry.goal.getId());
			if (children.isEmpty())
				return;
			entry.expanded = true;
			int newoffset = entry.offset + INDENT;
			int newlocation = currentlyShownGoals.indexOf(entry);
			for (Goal g : children)
			{
				currentlyShownGoals.add(++newlocation, new ListEntry(g, newoffset, false));
			}
			super.notifyDataSetChanged();
		}

		/**
		 * Collapses the given list entry
		 * @param entry List entry to be collapsed
		 */
		private void collapseEntry(ListEntry entry)
		{
			Vector<ListEntry> currentlyShownGoals = mCurrentlyShownGoals;
			// collapse
			entry.expanded = false;
			int index = currentlyShownGoals.indexOf(entry) + 1;
			int offset = entry.offset;
			while (currentlyShownGoals.size() > index
					&& currentlyShownGoals.get(index).offset > offset)
				currentlyShownGoals.remove(index);
			super.notifyDataSetChanged();
		}

	}
}
