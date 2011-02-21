/*
 *  Copyright (C) 2009 Sebastian Dörner, Julius Müller, Johannes Steffen
 *  Copyright (C) 2010-2011 Sebastian Dörner
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

package de.mango.business;

import java.io.File;
import java.text.DateFormat;
import java.util.GregorianCalendar;

import android.content.Context;
import android.content.Intent;
import de.mango.R;

/**
 * Goal Class encapsulating all goal-related information
 *
 */
public class Goal
{
	// these fields may never be null
	private long id;
	private String name;
	private String description;
	private String imageName;
	private int completion; // From 0 to 100
	private int completionWeight; // 1, 2 or 3
	private GregorianCalendar deadline;
	private GregorianCalendar timestamp; // date and time of creation

	/**
	 * Creates a new empty goal
	 */
	public Goal()
	{
		this("", "", null);
	}

	/**
	 * Creates a new goal with the given name
	 *
	 * @param name
	 *            Name of the goal
	 */
	public Goal(String name)
	{
		this(name, "", null);
	}

	/**
	 * Creates a new goal with given name, description and deadline
	 *
	 * @param name
	 *            Name of the goal
	 * @param description
	 *            Description of the goal
	 * @param deadline
	 *            Deadline of the goal (i.e. time when it should be done)
	 */
	public Goal(String name, String description, GregorianCalendar deadline)
	{
		this.name = name != null ? name : "";
		this.description = description != null ? description : "";
		this.imageName = "";
		this.completion = 0;
		this.deadline = deadline != null ? deadline : new GregorianCalendar();
		this.timestamp = new GregorianCalendar();
		this.completionWeight = 1;
	}

	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = (name != null) ? name : "";
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = (description != null) ? description : "";
	}

	public String getImageName()
	{
		return imageName;
	}

	public void setImageName(String imageName)
	{
		this.imageName = (imageName != null) ? imageName : "";
	}

	public int getCompletion()
	{
		return completion;
	}

	/**
	 * Sets the percentage of completion for this goal
	 *
	 * @param completion
	 *            Degree of completion from 0 to 100
	 */
	public void setCompletion(int completion)
	{
		this.completion = completion;
	}

	/**
	 * Gets the id of the color resource corresponding to the current goal
	 * completion.
	 *
	 * @return The resource id of the the color
	 */
	public int getCompletionColor()
	{
		if (completion < 25)
			return R.color.Progress_low;
		else if (completion < 50)
			return R.color.Progress_lowerMiddle;
		else if (completion < 75)
			return R.color.Progress_upperMiddle;
		else if (completion < 100)
			return R.color.Progress_high;
		else
			return R.color.Progress_complete;
	}

	public int getCompletionWeight()
	{
		return completionWeight;
	}

	/**
	 * Sets the completion weight
	 *
	 * @param completionWeight
	 *            New Completion weight ranging from 1 to 3
	 */
	public void setCompletionWeight(int completionWeight)
	{
		this.completionWeight = completionWeight;
	}

	public GregorianCalendar getDeadline()
	{
		return deadline;
	}

	public void setDeadline(GregorianCalendar deadline)
	{
		this.deadline = deadline != null ? deadline : new GregorianCalendar();
	}

	/**
	 * Gets a String representation of the Deadline.
	 * @return The Deadline Formatted like "01. JAN 2009".
	 */
	public String getFormattedDeadline()
	{
		GregorianCalendar dl = this.deadline;
		DateFormat sdf = DateFormat.getDateInstance(DateFormat.MEDIUM);
		return sdf.format(dl.getTime());
	}

	public GregorianCalendar getTimestamp()
	{
		return timestamp;
	}

	public void setTimestamp(GregorianCalendar timestamp)
	{
		this.timestamp = timestamp != null ? timestamp
				: new GregorianCalendar();
	}

	/**
	 * Deletes the image of this goal from hd.
	 */
	public void deleteImage(Context c)
	{
		if (this.imageName.length()==0)
			return;
		File imageFile = c.getFileStreamPath(this.imageName);
		if (imageFile.exists())
			imageFile.delete();
		this.imageName="";
	}

	/**
	 * Inserts all goal information needed for a new calendar entry into the
	 * calendar launch intent.
	 *
	 * @param i
	 *            The Intent to put the goal information in.
	 */
	public void putCalendarExtras(Intent i)
	{
		i.putExtra("beginTime", deadline.getTime().getTime());
		i.putExtra("endTime", deadline.getTime().getTime() + 1);
		i.putExtra("allDay", true);
		i.putExtra("title", this.name);
		i.putExtra("description", this.description);
	}
}
