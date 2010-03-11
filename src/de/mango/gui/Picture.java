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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import de.mango.R;
import de.mango.business.GoogleSearchProvider;
import de.mango.business.ImageDownloadCompleteCallback;
import de.mango.business.ImageHandling;
import de.mango.business.ImageSearchProvider;

public class Picture extends Activity implements OnClickListener,
		OnKeyListener, DialogInterface.OnKeyListener, ImageDownloadCompleteCallback
{
	/**
	 * Enable/disable logging. The compiler will strip all logging if disabled.
	 */
	public static final boolean DEBUG = false;
	public static final String TAG = "Mango";

	private Vector<Bitmap> imageVector = new Vector<Bitmap>();
	private int counter = 0;
	private ProgressDialog pd;
	private ImageHandling imageHandling;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(de.mango.R.layout.picture);
		
		//set Listeners
		Button search = (Button) findViewById(R.picture.searchButton);
		search.setOnClickListener(this);
		ImageButton next = (ImageButton) findViewById(R.picture.nextButton);
		next.setOnClickListener(this);
		Button attache = (Button) findViewById(R.picture.attacheButton);
		attache.setOnClickListener(this);
		EditText queryField = (EditText) findViewById(R.picture.searchString);
		queryField.setOnKeyListener(this);
		ImageView iv = (ImageView) findViewById(R.picture.mainImageView);

		//handle orientation change
		final Object data = getLastNonConfigurationInstance();
		if (data!=null)
		{
			OrientationChangeContainer c = (OrientationChangeContainer) data;
			this.counter = c.currentPosition;
			for (Bitmap b:c.bitmaps)
				imageVector.add(b);
			if (imageVector.size()>this.counter)
				iv.setImageBitmap(c.bitmaps[this.counter]);
		}
		//normal startup
		else if (this.getIntent().hasExtra("bitmap"))
		{
			Bitmap bm = (Bitmap) this.getIntent().getExtras().get("bitmap");
			iv.setImageBitmap(bm);
			imageVector.add(bm);

			Toast t = Toast.makeText(this, R.string.Picture_type_search_query,
					Toast.LENGTH_LONG);
			t.setGravity(Gravity.TOP, 0, 0);
			t.show();
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance()
	{
		if (imageHandling!=null && !imageHandling.allThreadsFinished())
		{
			imageHandling.cancelAllThreads();
		}
		if (pd!=null)
			pd.cancel();
		Vector<Bitmap> v = imageVector;
	    final OrientationChangeContainer c = new OrientationChangeContainer();
	    c.bitmaps = new Bitmap[v.size()];
	    for (int i=0; i< v.size();i++)
	    	c.bitmaps[i] = v.get(i);
	    c.currentPosition = this.counter;
	    return c;
	}


	/**
	 * Init a picture Search for a given query.
	 * @param searchQuery The search query to search for
	 */
	public void search(String searchQuery)
	{
		ImageSearchProvider imageSearch = GoogleSearchProvider.getInstance(this.getString(R.string.Google_hl));
		imageHandling = new ImageHandling();
		imageHandling.fetchBitmapsForQuery(searchQuery, 8, 0, imageSearch, this, this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	public void onClick(View v)
	{
		if (v == findViewById(R.picture.attacheButton))
		{
			// return chosen image
			Intent i = new Intent();
			if (imageVector == null || imageVector.isEmpty())
				i.putExtra("image", (Bitmap) null);
			else
				i.putExtra("image", imageVector.get(counter));
			setResult(RESULT_OK, i);
			finish();
		}
		else if (v == findViewById(R.picture.nextButton))
		{
			//change to next image
			Vector<Bitmap> bitmaps = imageVector;
			if (bitmaps != null && !bitmaps.isEmpty())
			{
				if (counter == bitmaps.size() - 1)
					counter = 0;
				else
					counter++;
				ImageView iv = (ImageView) findViewById(R.picture.mainImageView);
				iv.setImageBitmap(bitmaps.get(counter));
			}
		}
		else if (v == findViewById(R.picture.searchButton))
		{
			// Search for user input "searchString"
			EditText queryField = (EditText) findViewById(R.picture.searchString);
			//start progress
			pd = new ProgressDialog(this);
			pd.setTitle(R.string.Picture_search_progress_title);
			pd.setMessage(getString(R.string.Picture_search_progress_text));
			pd.setIndeterminate(true);
			pd.setOnKeyListener(this);
			pd.show();
			search(queryField.getText().toString());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.mango.business.ImageDownloadCompleteCallback#action(java.util.Vector)
	 */
	public void action(Vector<Bitmap> bitmaps)
	{
		imageHandling = null;
		//stop progress dialog
		pd.dismiss();
		//present found images as choices for the current goal
		if (bitmaps != null && bitmaps.size() > 0)
		{
			imageVector = bitmaps;
			ImageView iv = (ImageView) findViewById(R.picture.mainImageView);
			iv.setImageBitmap(bitmaps.get(0));
			counter = 0;
		} else
			Toast.makeText(this, R.string.Picture_no_results,
					Toast.LENGTH_SHORT).show();
	}

	private class OrientationChangeContainer
	{
		public Bitmap[] bitmaps;
		public int currentPosition;
	}

	public boolean onKey(View v, int keyCode, KeyEvent event)
	{
		//bound to the EditText component
		if (DEBUG)
			Log.d(TAG, Integer.toString(keyCode));
		if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP)
		{
			this.onClick(this.findViewById(R.picture.searchButton));
			//still let the android system close the software keyboard
			return false;
		}
		return false;
	}

	public boolean onKey(DialogInterface dialog, int keyCode,
			KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN)
		{	//stop potential searches
			if (imageHandling!=null)
				imageHandling.cancelAllThreads();
			pd.dismiss();
			return true;
		}
		return false;
	}
}