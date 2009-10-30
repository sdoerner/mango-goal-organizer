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

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;

/**
 * Class implementing a progress bar, which can be dragged by the user. Drag
 * events are passed to the registered listener.
 */
public class HorizontalSlide extends ProgressBar
{
	/**
	 * The (one and only) listener for drag events.
	 */
	private OnProgressChangeListener listener;
	/**
	 * If true, the user interaction is enabled.
	 */
	private boolean modifiable = true;
	private final static int padding = 2;

	/**
	 * Interface for a class listening to drag events of the HorizontalSlide
	 */
	public interface OnProgressChangeListener
	{
		void onProgressChanged(View v, int progress);
	}

	public HorizontalSlide(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public HorizontalSlide(Context context)
	{
		super(context);
	}

	/**
	 * Set the object listening for drag events. The potential old listener is
	 * dropped.
	 * 
	 * @param l
	 *            New Listener for drag events.
	 */
	public void setOnProgressChangeListener(OnProgressChangeListener l)
	{
		listener = l;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View#onTouchEvent(android.view.MotionEvent)
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		if (modifiable)
		{
			int action = event.getAction();
			if (action == MotionEvent.ACTION_DOWN
					|| action == MotionEvent.ACTION_MOVE)
			{
				float x_mouse = event.getX() - padding;
				float width = getWidth() - 2 * padding;
				int progress = Math.round((float) getMax() * (x_mouse / width));

				if (progress < 0)
					progress = 0;

				this.setProgress(progress);

				if (listener != null)
					listener.onProgressChanged(this, progress);
			}
		}
		return true;
	}

	/**
	 * Determine if the user can control the progress of the HorizontalSlide.
	 * 
	 * @param modifiable
	 *            If true, user interaction is enabled; otherwise disabled.
	 */
	public void setModifiable(boolean modifiable)
	{
		this.modifiable = modifiable;
	}
}
