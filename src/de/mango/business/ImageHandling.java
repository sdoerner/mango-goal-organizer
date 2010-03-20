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

package de.mango.business;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Class to provide images from local memory and www, save or resize them and so
 * on.
 */
public class ImageHandling
{
	/**
	 * Enable/disable logging. The compiler will strip all logging if disabled.
	 */
	public static final boolean DEBUG = false;
	public static final String TAG = "Mango";

	// Bitmaps received from async network threads
	private final Vector<Bitmap> bitmaps = new Vector<Bitmap>();
	// number of Bitmaps wanted (to know when we're done)
	private int requestedCount;
	// number of finished download threads
	private int receivedCallbacks = 0;
	// UI thread activity to perform ui actions on

	private SearchTask searchTask;
	private Set<DownloadImageTask> imageDownloadTasks = new HashSet<DownloadImageTask>();

	private ImageDownloadCompleteCallback imageDownloadCompleteCallback;

	/**
	 * Debug Method to print the contents of a given URL (should be a text file)
	 * 
	 * @param sUrl
	 *            the URL for the text file, which is to be printed
	 */
	public static void printURLContent(String sUrl)
	{
		try
		{
			URL url = new URL(sUrl);
			BufferedReader in = new BufferedReader(new InputStreamReader(url
					.openStream()));

			String inputLine;

			while ((inputLine = in.readLine()) != null)
				Log.i(TAG, inputLine);

			in.close();

		} catch (MalformedURLException e)
		{
			if (DEBUG)
			{
				Log.w(TAG, "Malformed URL Exception in ImageHandling");
				Log.w(TAG, Log.getStackTraceString(e));
			}
		} catch (IOException e)
		{
			if (DEBUG)
			{
				Log.w(TAG, "IOException in ImageHandling");
				Log.w(TAG, Log.getStackTraceString(e));
			}
		}

	}

	/**
	 * Retrieves a file from the Web and saves it to the local file system
	 * 
	 * @param url
	 *            The Web URL to the file
	 * @param filename
	 *            The local filename the file shall be saved to
	 * @param context
	 *            Context to get a Handle to the local file system
	 */
	public static void retrieveWebFile(String url, String filename,
			Context context)
	{
		try
		{
			URL imageUrl = new URL(url);
			byte[] b = new byte[4096];
			int length;
			InputStream is = imageUrl.openStream();
			BufferedInputStream bis = new BufferedInputStream(is, 8);

			FileOutputStream os = filename.contains("/") ? new FileOutputStream(
					filename)
					: context.openFileOutput(filename,
							Context.MODE_WORLD_READABLE);
			BufferedOutputStream bos = new BufferedOutputStream(os);
			while (bis.available() > 0)
			{
				length = bis.read(b);
				bos.write(b, 0, length);
			}
			bis.close();
			bos.close();

		} catch (MalformedURLException e)
		{
			if (DEBUG)
			{
				Log
						.w(TAG, "Given file URL \"" + url
								+ "\"is invalid.");
				Log.w(TAG, Log.getStackTraceString(e));
			}
		} catch (IOException e)
		{
			if (DEBUG)
			{
				Log
						.w(TAG, "Could not write to local file "
								+ filename);
				Log.w(TAG, Log.getStackTraceString(e));
			}
		}

	}

	/**
	 * Loads an image from the local file system.
	 * 
	 * @param filename
	 *            Name of the file to be opened. Must be either absolute or
	 *            without any path separator.
	 * @param context
	 *            Application context for opening images private to our
	 *            application.
	 * @return The Bitmap corresponding to the given image file.
	 */
	public static Bitmap loadLocalBitmap(String filename, Context context)
	{
		try
		{
			FileInputStream is = filename.contains("/") ? new FileInputStream(
					filename) : context.openFileInput(filename);
			BufferedInputStream bis = new BufferedInputStream(is, 8);
			Bitmap bm = BitmapFactory.decodeStream(bis);
			bis.close();
			return bm;
		} catch (FileNotFoundException e)
		{
			Log.w(TAG, "loadLocalBitmap: File " + filename
					+ " not found!");
			Log.w(TAG, Log.getStackTraceString(e));
		} catch (IOException e)
		{
			Log.w(TAG, "IOException in loadLocalBitmap");
			Log.w(TAG, Log.getStackTraceString(e));
		}
		return null;
	}

	/**
	 * Loads a Bitmap specified by a given URI.
	 * @param contentResolver The contentResolver to resolve the URI.
	 * @param uri The URI to resolve. Must start with content://
	 * @return a Bitmap representing the content of the URI.
	 */
	public static Bitmap loadBitmapFromContentUri(ContentResolver contentResolver, Uri uri) {
		Bitmap photo =  null;
		try {
			BufferedInputStream bis = new BufferedInputStream(contentResolver.openInputStream(uri));
			photo = BitmapFactory.decodeStream(bis);
			bis.close();
		} catch (Exception e) {
			if (DEBUG)
				Log.e(TAG, "loadPhotoFromUri: " + e.getMessage());
		}
	    return photo;
	}


	/**
	 * Downloads an image from the given URL and returns it as a bitmap.
	 * 
	 * @param url
	 *            The URL for the image
	 * @return The image as a Bitmap.
	 */
	public static Bitmap retrieveWebBitmap(String url)
	{
		try
		{
			URL imageUrl = new URL(url);
			InputStream is = imageUrl.openStream();
			BufferedInputStream bis = new BufferedInputStream(is, 8);
			Bitmap bm = BitmapFactory.decodeStream(bis);
			bis.close();
			is.close();
			return bm;
		} catch (MalformedURLException e)
		{
			if (DEBUG)
			{
				Log.w(TAG, "Given file URL \"" + url
								+ "\"is invalid.");
				Log.w(TAG, Log.getStackTraceString(e));
			}
		} catch (IOException e)
		{
			if (DEBUG)
			{
				Log.w(TAG, "IOException in retrieveWebBitmap");
				Log.w(TAG, Log.getStackTraceString(e));
			}
		}
		return null;
	}

	/**
	 * Retrieves a unique, unused filename based on the desired filename. Only
	 * works for local file names.
	 * 
	 * @param filename
	 *            The base file name we would like to use.
	 * @param context
	 *            Application context for reading local files.
	 * @return The new filename, which is not used yet.
	 */
	public static String uniqueFilename(String filename, Context context)
	{
		// split name to generate new names
		int sep = filename.lastIndexOf('.');
		String basename = filename.substring(0, sep) + "-";
		String suffix = filename.substring(sep);

		String[] localFiles = context.fileList();

		String newname = filename;
		int n = localFiles.length;
		int index = 0;
		// TODO: this loop is kinda brute force, but works for now
		for (int i = 0; i < n; i++)
		{
			if (newname.compareTo(localFiles[i]) == 0)
			{
				index++;
				newname = basename + index + suffix;
				i = -1;
			}
		}
		return newname;
	}

	/**
	 * Retrieves a number of bitmaps for a given key word.
	 * 
	 * @param query
	 *            The Keyword to search for.
	 * @param count
	 *            The number of results wanted.
	 * @param page
	 *            The page of the web search (results will start at index
	 *            page*count).
	 * @param search
	 *            The search Provider to use for the image search.
	 * @param activity
	 *            Activity in the UI-Thread for the result callbacks
	 * @param callback
	 *            The callback to be executed when image download is complete
	 * @return True if we successfully started the search. False if the last
	 *         search hasn't finished yet.
	 */
	public boolean fetchBitmapsForQuery(String query, int count, int page,
			ImageSearchProvider search, final Activity activity,
			ImageDownloadCompleteCallback callback)
	{
		if (!this.allThreadsFinished())
			return false;
		bitmaps.clear();// maybe we could do that after the start of our thread,
		// since networking
		// should be slower than clearing an array
		// run the search in another thread
		searchTask = new SearchTask(this);
		Params p = new Params(query, count, page, search);
		// these are needed for the callback-receiving addBitmap-Method to
		// display results
		this.requestedCount = count;
		this.imageDownloadCompleteCallback = callback;
		// now go to work
		searchTask.execute(p);
		return true;
	}

	/**
	 * Class to encapsulate the parameters needed to download the images for a
	 * given query.
	 */
	private class Params
	{
		// public fields since
		// a) we only need it as a struct, we cannot have multiple parameters
		// for Async Tasks
		// b) the class is private and will only be used here
		public String query;
		public int count;
		public int page;
		public ImageSearchProvider searchProvider;

		public Params(String query, int count, int page,
				ImageSearchProvider searchProvider)
		{
			this.query = query;
			this.count = count;
			this.page = page;
			this.searchProvider = searchProvider;
		}
	}

	/**
	 * Task searching for a query and initializing downloads for all results in multiple threads.
	 */
	private class SearchTask extends
			AsyncTask<Params, Void, Vector<String>>
	{
		// needed in onPostExecute and thus must be kept as class variables
		private final ImageHandling imageHandler;

		public SearchTask(ImageHandling imageHandler)
		{
			this.imageHandler = imageHandler;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		protected Vector<String> doInBackground(Params... p)
		{
			// perform a search in a seperate thread
			return p[0].searchProvider
					.search(p[0].query, p[0].count, p[0].page);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		protected void onPostExecute(Vector<String> urls)
		{
			ImageHandling.this.searchTask = null;
			if (DEBUG)
				Log.d(TAG, "Search complete. Fetching images.");
			this.imageHandler.requestedCount = urls.size();
			for (String url : urls)
			{
				DownloadImageTask dl = new DownloadImageTask(this.imageHandler, url);
				ImageHandling.this.imageDownloadTasks.add(dl);
				dl.execute((ImageHandling.Params)null);
			}
			if (urls.size() == 0)
				imageDownloadCompleteCallback.action(null);
		}
	}

	/**
	 * Task for downloading exactly one Bitmap.
	 */
	private class DownloadImageTask extends AsyncTask<Params, Void, Bitmap>
	{
		String mUrl;
		ImageHandling mIh;
		DownloadImageTask(ImageHandling ih, String url)
		{
			mUrl = url;
			mIh = ih;
		}

		@Override
		protected Bitmap doInBackground(Params... params)
		{
			final Bitmap b = ImageHandling.retrieveWebBitmap(mUrl);
			if (b != null)
			{
				if (DEBUG)
					Log.v(TAG, "Successful: " + mUrl);
			} else
			// TODO: still dunno WHY he fails sometimes,
			// shouldn't TCP handle that?
			// maybe we should retry until $maximum_tries has
			// been reached
			if (DEBUG)
				Log.v(TAG, "Failed to retrieve " + mUrl);
			return b;
		}

		@Override
		protected void onPostExecute(Bitmap result)
		{
			if (DEBUG)
			{
				if (result==null)
					Log.d(TAG, "returned Bitmap is null in dl-thread");
			}
			ImageHandling.this.imageDownloadTasks.remove(DownloadImageTask.this);
			mIh.addBitmap(result);
		}
	}

	/**
	 * Adds a Bitmap to our results list. Is (mostly) called asynchronously.
	 * 
	 * @param b
	 *            The Bitmap to add to the list;
	 */
	private void addBitmap(Bitmap b)
	{
		this.receivedCallbacks++;
		if (b != null)
			this.bitmaps.add(b);
		if (this.receivedCallbacks == this.requestedCount // the result list is
				// complete
				&& this.imageDownloadCompleteCallback != null)
		{
			imageDownloadCompleteCallback.action(bitmaps);
		}
	}

	/**
	 * @return Whether all fetching threads are finished.
	 */
	public boolean allThreadsFinished()
	{
		return this.receivedCallbacks == this.requestedCount;
	}

	/**
	 * Returns the Bitmaps, which have been fetched until now. Does NOT ensure
	 * all threads are finished enabling us to retrieve partial results. If a
	 * you only want to react on results of a completed search, use the callback
	 * parameter of fetchBitmapsForQuery
	 * 
	 * @return A Vector containing all Bitmaps, which have been downloaded until
	 *         now.
	 */
	public Vector<Bitmap> getfetchedImages()
	{
		return this.bitmaps;
	}

	/**
	 * Saves a given bitmap to the local file system.
	 * 
	 * 
	 * @param context
	 * @param bitmap
	 * @return Unique filename of saved bitmap
	 */
	public static String saveLocalBitmap(Context context, Bitmap bitmap)
	{
		// we cannot compress colormap bitmaps, so convert to ARGB
		if (bitmap.getConfig() == null)
			bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
		if (bitmap == null)
			return null;

		FileOutputStream fOut = null;
		String name = new String();
		try
		{
			name = uniqueFilename("image.png", context);
			fOut = context.openFileOutput(name, 1);
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}

		bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);

		try
		{
			fOut.flush();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		try
		{
			fOut.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}

		return name;
	}

	/**
	 * Method that resizes a given bitmap and returns it as a result.
	 * 
	 * @param bitmap
	 *            bitmap to be resized
	 * @param wantedWidth
	 * @param wantedHeight
	 * @return Resized bitmap
	 */
	public static Bitmap resizeBitmap(Bitmap bitmap, int wantedWidth,
			int wantedHeight)
	{
		if (bitmap == null)
			return null;
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		int newWidth = wantedWidth;
		int newHeight = wantedHeight;
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;
		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);
		Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height,
				matrix, true);

		return resizedBitmap;
	}

	/**
	 * Cancels all image search threads
	 */
	public void cancelAllThreads()
	{
		if (DEBUG)
			Log.d(TAG,"In Method: cancelAllThreads()");
		if (searchTask!=null)
		{
			searchTask.cancel(true);
			searchTask = null;
		}
		try{
		for (DownloadImageTask t:imageDownloadTasks)
		{
			if (DEBUG)
				Log.d(TAG,"Cancelling Thread");
			t.cancel(true);
		}
		}
		catch (SecurityException e)
		{
			if (DEBUG)
				Log.d(TAG,"Security Exception: " + e.getMessage());
		}
		imageDownloadTasks.clear();
	}

}
