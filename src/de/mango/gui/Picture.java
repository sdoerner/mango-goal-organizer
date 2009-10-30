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
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
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
		ImageDownloadCompleteCallback
{
	private Vector<Bitmap> imageVector = new Vector<Bitmap>();
	private int counter = 0;
	private ProgressDialog pd;

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

			Toast t = Toast.makeText(this, "Please type in your search query.",
					Toast.LENGTH_LONG);
			t.setGravity(Gravity.TOP, 0, 0);
			t.show();
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance()
	{
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
		ImageSearchProvider imageSearch = GoogleSearchProvider.getInstance();
		ImageHandling ih = new ImageHandling();
		ih.fetchBitmapsForQuery(searchQuery, 8, 0, imageSearch, this, this);
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
			pd = ProgressDialog.show(Picture.this, "Searching..", "Searching for Images",
					true, false);
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
			Toast.makeText(this, "Sorry, no results for your query.",
					Toast.LENGTH_SHORT).show();
	}

	private class OrientationChangeContainer
	{
		public Bitmap[] bitmaps;
		public int currentPosition;
	}
}