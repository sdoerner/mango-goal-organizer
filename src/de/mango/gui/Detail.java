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

import java.util.Vector;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.mango.R;
import de.mango.business.Goal;
import de.mango.business.GoalProvider;
import de.mango.business.ImageHandling;

/**
 * Activity showing the details of a Goal. All Children are shown as pictures
 * and accessible through these. Progress is the only changable property.
 */
public class Detail extends Activity implements OnClickListener
{
	public static final int RESULT_TOP_LEVEL_GOALS_CHANGED = RESULT_FIRST_USER;
	/**
	 * Layout containing the pictures of the goal's children.
	 */
	private ViewGroup subgoalsLayout;
	/**
	 * The currently shown goal (which is only one in details screen).
	 */
	private Goal goal;
	private static Goal currentGoal; 
	
	/**
	 * Children of the currently shown goal in presented order.
	 */
	private Vector<Goal> children;
	private GoalProvider goalProvider;

	// widgets for later access
	private TextView nameTextView;
	private TextView completionTextView;
	private Vector<ImageButton> childButtons = new Vector<ImageButton>();
	private HorizontalSlide progress;
	private Button changeProgress;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	public void onCreate(Bundle savedInstanceState)
	{
		// init dialog
		super.onCreate(savedInstanceState);
		setContentView(de.mango.R.layout.detail);
		subgoalsLayout = (ViewGroup) findViewById(R.detail.subgoalsLayout);
		setResult(RESULT_CANCELED);
		goalProvider = new GoalProvider(this);

		// get and check goal to be shown
		Intent i = getIntent();
		final long id = i.getLongExtra("goalId", -1);
		if (id == -1) {
			finish();
		}
		goal = goalProvider.getGoalWithId(id);
		if (goal == null)
		{
			goal = Detail.currentGoal;
			Detail.currentGoal = null;
		}
		if (goal==null)
			finish();
		else
		{
			// load goal information
			TextView text = (TextView) findViewById(R.detail.name);
			text.setText(goal.getName());
			text.setBackgroundColor(getResources().getColor(
					goal.getCompletionColor()));
			this.nameTextView = text;

			text = (TextView) findViewById(R.detail.deadline);
			text.setText(goal.getFormattedDeadline());

			text = (TextView) findViewById(R.detail.completion);
			text.setText(getResources().getString(R.string.Progress)+": "
					+ Integer.toString(goal.getCompletion()) + "%");
			this.completionTextView = text;

			text = (TextView) findViewById(R.detail.description);
			text.setText(goal.getDescription());

			progress = (HorizontalSlide) findViewById(R.detail.progress);
			changeProgress = (Button) findViewById(R.detail.progressChangeButton);
			changeProgress.setOnClickListener(this);
			
			if (savedInstanceState!=null && savedInstanceState.getBoolean("ChangeButtonPressed"))
			{
				progress.setModifiable(true);
				progress.setProgress(savedInstanceState.getInt("currentProgress"));
				changeProgress.setText(R.string.Button_accept);
			}
			else
			{
				progress.setModifiable(false);
				progress.setProgress(goal.getCompletion());
			}
				

			if (goal.getChildren() != null && !goal.getChildren().isEmpty())
				changeProgress.setVisibility(View.INVISIBLE);

			ImageView iv = (ImageView) findViewById(R.detail.image);
			iv.setImageBitmap((goal.getImageName().equals("")) ? BitmapFactory
					.decodeResource(getResources(), R.drawable.nopic)
					: ImageHandling.loadLocalBitmap(goal.getImageName(), this));
			// draw picture for each child
			drawChildren();
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		Detail.currentGoal = goal;
		boolean slideActiveState = changeProgress.getText().equals(R.string.Button_accept);
		outState.putBoolean("ChangeButtonPressed", slideActiveState);
		if (slideActiveState)
			outState.putInt("currentProgress", progress.getProgress());
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause()
	{
		super.onPause();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	public void onClick(View v)
	{
		if (v == findViewById(R.detail.progressChangeButton))
		{
			Button changeButton = (Button) v;
			HorizontalSlide progress = (HorizontalSlide) findViewById(R.detail.progress);
			if (changeButton.getText().equals(getResources().getString(R.string.Button_change)))
			{
				// make Slide draggable
				changeButton.setText(R.string.Button_accept);
				progress.setModifiable(true);
			}
			else
			{
				// Accept new Progress
				progress.setModifiable(false);
				changeButton.setText(R.string.Button_change);
				// change data
				goal.setCompletion(progress.getProgress());
				setResult(Create.RESULT_MODIFIED);
				// refresh view
				nameTextView.setBackgroundColor(getResources().getColor(
						goal.getCompletionColor()));
				completionTextView.setText(getResources().getString(R.string.Progress)+": "
						+ goal.getCompletion() + "%");
			}
		}
		else
		{
			// clicked on a subgoal -> show new Details screen for it
			int index = (Integer) v.getTag();
			Intent i = new Intent(this, Detail.class);
			i.putExtra("goalId", goal.getId());
			startActivityForResult(i, index);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onActivityResult(int, int,
	 * android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (resultCode == Create.RESULT_MODIFIED)
		{
			// returned from another Details-Screen, in which the progress has
			// been adjusted
			
			// update progress and progress colors
			this.childButtons.get(requestCode).setBackgroundColor(
					getResources()
							.getColor(
									this.children.get(requestCode)
											.getCompletionColor()));
			nameTextView.setBackgroundColor(getResources().getColor(
					goal.getCompletionColor()));
			this.completionTextView.setText(getResources().getString(R.string.Progress)+": "
					+ goal.getCompletion() + "%");
			HorizontalSlide progress = (HorizontalSlide) findViewById(R.detail.progress);
			progress.setProgress(goal.getCompletion());
			// propagate changes up
			this.setResult(Create.RESULT_MODIFIED);
		}
	}

	/**
	 * Draws Images for all children of the current goal.
	 * 
	 * @param goal
	 */
	private void drawChildren()
	{
		Goal goal = this.goal;
		Vector<Goal> subgoals = goal.getChildren();
		// TODO: Need of improvement - Maybe a Grid View
		if (subgoals != null && !subgoals.isEmpty())
		{
			TextView header = (TextView) findViewById(R.detail.subgoalsCaption);
			header.setVisibility(View.VISIBLE);

			this.children = subgoals;
			int i = subgoals.size() / 5;
			int j = subgoals.size() % 5;

			Goal currentChild;
			LinearLayout layout;
			Bitmap bitmap;
			int z = 0;
			
			// Pass through all subgoals and draw 5 subgoals per row
			for (int x = 0; x < i; x++)
			{
				layout = new LinearLayout(subgoalsLayout.getContext());

				for (int y = 0; y < 5; y++)
				{
					currentChild = subgoals.get(z);
					ImageButton ib = new ImageButton(subgoalsLayout
							.getContext());
					ib.setBackgroundColor(getResources().getColor(
							currentChild.getCompletionColor()));

					bitmap = (currentChild.getImageName().equals("")) ? BitmapFactory
							.decodeResource(getResources(), R.drawable.nopic)
							: ImageHandling.loadLocalBitmap(currentChild
									.getImageName(), this);

					ib.setImageBitmap(ImageHandling
							.resizeBitmap(bitmap, 40, 40));
					ib.setTag(z++);
					ib.setOnClickListener(this);
					layout.addView(ib);
					childButtons.add(ib);
				}
				subgoalsLayout.addView(layout);
			}
			
			// fill last line (which is not full)
			if (j != 0)
			{
				layout = new LinearLayout(subgoalsLayout.getContext());
				for (int x = 0; x < j; x++)
				{
					currentChild = subgoals.get(z);
					ImageButton ib = new ImageButton(subgoalsLayout
							.getContext());
					ib.setBackgroundColor(getResources().getColor(
							currentChild.getCompletionColor()));

					bitmap = (currentChild.getImageName().equals("")) ? BitmapFactory
							.decodeResource(getResources(), R.drawable.nopic)
							: ImageHandling.loadLocalBitmap(currentChild
									.getImageName(), this);

					ib.setImageBitmap(ImageHandling
							.resizeBitmap(bitmap, 40, 40));
					ib.setTag(z++);
					ib.setOnClickListener(this);
					layout.addView(ib);
					childButtons.add(ib);
				}
				subgoalsLayout.addView(layout);
			}
		}
	}
}
