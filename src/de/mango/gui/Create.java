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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;
import de.mango.R;
import de.mango.business.Goal;
import de.mango.business.GoalCrud;
import de.mango.business.ImageHandling;

public class Create extends Activity implements OnClickListener
{
	// first result type used by Hierarchy
	public static final int RESULT_CREATED = RESULT_FIRST_USER + 1;
	public static final int RESULT_MODIFIED = RESULT_FIRST_USER + 2;
	private AlertDialog.Builder mAlertDialogBuilder;
	private Bitmap goalImage = null;
	PopupWindow pw = null;
	// if true, modify g
	// if false
	// if g==null, create new toplevel goal
	// else create subgoal for g
	boolean modify = false;
	Goal g = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{

		super.onCreate(savedInstanceState);
		setContentView(de.mango.R.layout.create);

		
		DatePicker date = (DatePicker) findViewById(R.create.deadlineDatePicker);
		if (savedInstanceState!=null)
		{
			date.updateDate(savedInstanceState.getInt("dlYear"), savedInstanceState
					.getInt("dlMonth"), savedInstanceState.getInt("dlDay"));
			goalImage = (Bitmap)savedInstanceState.getParcelable("image");
		}

		ImageButton imageButton = (ImageButton) findViewById(R.create.imagebutton);
		imageButton.setOnClickListener(this);

		ImageButton helpButton = (ImageButton) findViewById(R.create.helpButton);
		helpButton.setOnClickListener(this);

		Button save = (Button) findViewById(R.create.saveButton);
		save.setOnClickListener(this);

		Button cancel = (Button) findViewById(R.create.cancelButton);
		cancel.setOnClickListener(this);

		HorizontalSlide progress = (HorizontalSlide) findViewById(R.create.progressBar);

		// Check if a new goal will be created or an existing goal will be
		// modified
		modify = getIntent().getBooleanExtra("modify", false);

		g = GoalCrud.currentGoal;
		// currentGoal should only be used within Activity-Launches since static
		// variables may
		// become null when our process is killed and restarted
		GoalCrud.currentGoal = null;

		setTitle(modify ? R.string.ActivityTitle_Modify : R.string.ActivityTitle_Create);

		// If an existing goal is being modified
		if (modify)
		{
			if (g == null)
			{
				setResult(RESULT_CANCELED);
				finish();
			}
			else
			{
				progress.setModifiable(g.getChildren() == null || g.getChildren().isEmpty());
				progress.setProgress(g.getCompletion());

				EditText text = (EditText) findViewById(R.create.nameEditField);
				text.setText(g.getName());

				text = (EditText) findViewById(R.create.descriptionEditField);
				text.setText(g.getDescription());

				RadioGroup rgroup = (RadioGroup) findViewById(R.create.weightRadioGroup);
				switch (g.getCompletionWeight())
				{
				case 1:
					rgroup.check(R.create.weightRadioButtonLow);
					break;
				case 2:
					rgroup.check(R.create.weightRadioButtonMedium);
					break;
				case 3:
					rgroup.check(R.create.weightRadioButtonHigh);
					break;
				}
				
				GregorianCalendar deadline = g.getDeadline();
				if (savedInstanceState==null)
					date.updateDate(deadline.get(Calendar.YEAR), deadline.get(Calendar.MONTH), deadline
							.get(Calendar.DAY_OF_MONTH));
				
				if (goalImage==null && !g.getImageName().equals(""))
					goalImage = ImageHandling.loadLocalBitmap(g.getImageName(), this);

				save.setText("Modify");
			}
		}
		if (goalImage != null)
		{
			ImageButton iv = (ImageButton) findViewById(R.create.imagebutton);
			iv.setImageBitmap(goalImage);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) iv
					.getLayoutParams();
			params.height = goalImage.getHeight() > 218 ? 218 : LayoutParams.WRAP_CONTENT;
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		GoalCrud.currentGoal = g;

		DatePicker datePicker = (DatePicker) findViewById(R.create.deadlineDatePicker);
		outState.putInt("dlYear", datePicker.getYear());
		outState.putInt("dlMonth", datePicker.getMonth());
		outState.putInt("dlDay", datePicker.getDayOfMonth());
		if (goalImage!=null)
			outState.putParcelable("image", goalImage);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	public void onClick(View v)
	{
		if (v == findViewById(R.create.helpButton))
		{
			Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("How to define a goal...");
			builder.setIcon(R.drawable.help);
			builder.setMessage(R.string.Create_Helptext);
			builder.setPositiveButton("ok", null);
			builder.show();
		}
		else if (v == findViewById(R.create.imagebutton))
		{
			ConnectivityManager cm = (ConnectivityManager) this
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected())
			{
				Intent i = new Intent(this, Picture.class);

				if (goalImage != null)
					i.putExtra("bitmap", goalImage);
				startActivityForResult(i, 0);
			}
			else
				Toast.makeText(this, "No network connection available.", Toast.LENGTH_SHORT).show();
		}
		else if (v == findViewById(R.create.saveButton))
		{
			// read data to be validated
			DatePicker datePicker = (DatePicker) findViewById(R.create.deadlineDatePicker);
			final GregorianCalendar deadline = new GregorianCalendar();
			deadline.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(),
					23, 59, 59);
			final String name = ((EditText) this.findViewById(R.create.nameEditField)).getText()
					.toString();
			final String description = ((EditText) this.findViewById(R.create.descriptionEditField))
					.getText().toString();

			if (verify(name, description, deadline))
			{
				// checks if deadlines of parental goals are in conflict with
				// current goal
				if (g == null) // new Top Level Goal
				{
					save(name, description, deadline);
				}
				else
				{
					// Check Parents' deadlines (new subgoal -> p is g; modified
					// subgoal -> p is g.getParent)
					boolean parentDeadlineConflicting = false;
					// parent of current/new goal
					Goal traverserUp = modify ? g.getParent() : g;
					while (traverserUp != null && !parentDeadlineConflicting)
					{
						if (deadline.compareTo(traverserUp.getDeadline()) == 1)
							parentDeadlineConflicting = true;
						traverserUp = traverserUp.getParent();
					}

					if (parentDeadlineConflicting)
					{
						mAlertDialogBuilder = new AlertDialog.Builder(this);
						mAlertDialogBuilder.setCancelable(true);
						mAlertDialogBuilder.setMessage("Deadline is behind parent's deadline."
								+ "\n\nDo you want to change parent's deadline automatically?");
						AlertDialog dialog = mAlertDialogBuilder.create();
						dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
								new DialogInterface.OnClickListener()
								{
									public void onClick(DialogInterface dialog, int which)
									{
										if (which == AlertDialog.BUTTON_POSITIVE)
										{
											// adjust parent's deadlines
											Goal traverserUp = modify ? g.getParent() : g;
											while (traverserUp != null)
											{
												if (deadline.compareTo(traverserUp.getDeadline()) == 1)
													traverserUp.setDeadline(deadline);
												traverserUp = traverserUp.getParent();
											}
											save(name, description, deadline);
										}
									}
								});
						dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
								(DialogInterface.OnClickListener) null);
						dialog.show();
					}
					else if (modify && !checkChildren(g, deadline))
					{
						// ChangeChildren
						mAlertDialogBuilder = new AlertDialog.Builder(this);
						mAlertDialogBuilder.setCancelable(true);
						mAlertDialogBuilder
								.setMessage("Deadline is after one or more child(ren) deadline(s)."
										+ "\n\nDo you want to change child(dren) deadlines automatically?");
						AlertDialog dialog = mAlertDialogBuilder.create();
						dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
								new DialogInterface.OnClickListener()
								{
									public void onClick(DialogInterface dialog, int which)
									{
										if (which == AlertDialog.BUTTON_POSITIVE)
										{
											changeChildren(g, deadline);
											save(name, description, deadline);
										}
									}
								});
						dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
								(DialogInterface.OnClickListener) null);
						dialog.show();

					}
					else
					// parents' and children's deadlines ok
					{
						save(name, description, deadline);
					}
				}
			}
		}
		else if (v == findViewById(R.create.cancelButton))
		{
			setResult(RESULT_CANCELED);
			finish();
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
		// insert image chosen in Picture Acitivity
		if (resultCode == RESULT_OK)
		{
			ImageButton iv = (ImageButton) findViewById(R.create.imagebutton);
			goalImage = (Bitmap) data.getExtras().get("image");
			if (goalImage != null)
			{
				iv.setImageBitmap(goalImage);
				RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) iv
						.getLayoutParams();
				params.height = goalImage.getHeight() > 218 ? 218 : LayoutParams.WRAP_CONTENT;
			}
		}
	}

	/**
	 * Verify user input
	 * 
	 * @param name
	 *            name of the goal
	 * @param description
	 *            description of the goal
	 * @param deadline
	 *            deadline of the goal
	 * @return
	 */
	private boolean verify(final String name, final String description,
			final GregorianCalendar deadline)
	{

		Vector<String> errors = new Vector<String>();
		if (name.equals(""))
			errors.add("- Name is empty!");
		if (description.equals(""))
			errors.add("- Description is empty!");
		if (deadline.compareTo(new GregorianCalendar()) == -1)
			errors.add("- Deadline is in past!");

		if (!errors.isEmpty())
		{
			String errMsg = "";
			for (int i = 0; i < errors.size() - 1; i++)
			{
				errMsg = errMsg + errors.get(i) + "\n\n";
			}
			errMsg = errMsg + errors.lastElement();
			Toast t = Toast.makeText(this, errMsg, Toast.LENGTH_LONG);
			t.setGravity(Gravity.CENTER, 0, 0);
			t.show();
			return false;
		}

		return true;
	}

	/**
	 * Save given Goal data to current or new Goal and exit Activity.
	 * 
	 * @param name
	 *            New name of the goal.
	 * @param description
	 *            New description of the goal.
	 * @param deadline
	 *            New deadline of the goal.
	 */
	private void save(String name, String description, GregorianCalendar deadline)
	{
		// compose new goal information
		Goal goal = modify ? g : new Goal();
		goal.setName(name);
		goal.setDescription(description);
		goal.setDeadline(deadline);

		// important: set Weight before Progress, because the
		// calculation needs the new weight
		RadioGroup rgroup = (RadioGroup) findViewById(R.create.weightRadioGroup);
		switch (rgroup.getCheckedRadioButtonId())
		{
		case R.create.weightRadioButtonLow:
			goal.setCompletionWeight(1, false);
			break;
		case R.create.weightRadioButtonMedium:
			goal.setCompletionWeight(2, false);
			break;
		case R.create.weightRadioButtonHigh:
			goal.setCompletionWeight(3, false);
			break;
		}

		HorizontalSlide progress = (HorizontalSlide) findViewById(R.create.progressBar);
		goal.setCompletion((progress.getProgress() * 100) / progress.getMax());

		if (goalImage != null)
			goal.setImageName(ImageHandling.saveLocalBitmap(this, goalImage));

		// insert goal into tree
		GoalCrud crud = GoalCrud.getInstance(this);
		crud.setDataChanged();
		if (modify)
		{
			setResult(RESULT_MODIFIED);
		}
		else
		{
			if (g != null) // create subgoal branch
				g.addChild(goal);
			else
				crud.getTopLevelGoals().add(goal);
			setResult(RESULT_CREATED);
		}
		finish();
	}

	/**
	 * Checks if deadline of any direct child is behind given deadline. Grand
	 * children will also fit if we expect validity of children.
	 * 
	 * @param g
	 *            Goal, whose children are to be considered
	 * @param deadline
	 *            deadline to be compared with
	 * @return true if all children have deadline before given deadline
	 */
	private boolean checkChildren(Goal g, GregorianCalendar deadline)
	{
		Vector<Goal> children = g.getChildren();
		if (children == null || children.isEmpty())
			return true;
		for (Goal c : children)
		{
			if (deadline.compareTo(c.getDeadline()) == -1)
				return false;
		}
		return true;
	}

	/**
	 * Sets all deadlines below g to deadline if they are too high
	 * 
	 * @param g
	 *            root of the goal tree to be adjusted
	 * @param deadline
	 *            dealine to which the children should be set
	 */
	private void changeChildren(Goal g, GregorianCalendar deadline)
	{
		Vector<Goal> children = g.getChildren();
		// method is only called if there actually are children, thus children
		// is NOT null
		for (Goal c : children)
		{
			if (deadline.compareTo(c.getDeadline()) == -1)
				c.setDeadline(deadline);
			if (c.getChildren() != null && !c.getChildren().isEmpty())
				changeChildren(c, deadline);
		}
	}
}
