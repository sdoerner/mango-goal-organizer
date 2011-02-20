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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

/**
 * Class implementing a web search for Images using the Google REST interface
 * (restricted to a maximum of 64 results by Google).
 */
public class GoogleSearchProvider implements ImageSearchProvider
{
	/**
	 * Enable/disable logging. The compiler will strip all logging if disabled.
	 */
	public static final boolean DEBUG = false;
	public static final String TAG = "Mango";

	private boolean moreResults = false;
	// only show results from openclipart.org
	private static final boolean restrictToOpenClipart = false;
	private static final String openClipart = " site:openclipart.org";
	// URL to transmit to Google during the search (shall be adapted to the
	// Project URL when present)
	private static final String refererUrl = "http://www.mango-android.de/";

	// additional search arguments to the search
	// see:
	// http://code.google.com/intl/de-DE/apis/ajaxsearch/documentation/reference
	// .html
	// v : search API version, only 1.0 possible atm
	// rsz: number of results (small = 4, large = 8)
	// hl: language
	private static final String searchArguments = "v=1.0&rsz=large&imgsz=small|medium&hl=";
	private String hostLanguage;

	// --------------------Singleton implementation--------------------
	private static GoogleSearchProvider instance;


	private GoogleSearchProvider()
	{

	}

	public static GoogleSearchProvider getInstance(String Google_hl)
	{
		if (instance == null)
			instance = new GoogleSearchProvider();
		instance.hostLanguage = Google_hl;
		return instance;
	}

	// ------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 *
	 * @see de.mango.business.ImageSearchProvider#hasMoreResults()
	 */
	public boolean hasMoreResults()
	{
		return this.moreResults;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.mango.business.ImageSearchProvider#search(java.lang.String, int,
	 * int)
	 */
	public Vector<String> search(String query, int count, int page)
	{
		Vector<String> results = new Vector<String>();
		// Prepare the query
		try
		{
			query = "http://ajax.googleapis.com/ajax/services/search/images?"
					+ GoogleSearchProvider.searchArguments + this.hostLanguage
					+ "&q="
					+ URLEncoder.encode(
							GoogleSearchProvider.restrictToOpenClipart ? query
									+ GoogleSearchProvider.openClipart : query,
							"UTF-8") + "&start=";
		} catch (UnsupportedEncodingException e)
		{
			if (DEBUG)
			{
				Log.w(TAG, "Unsupported Encoding Exception:"
						+ e.getMessage());
				Log.w(TAG, Log.getStackTraceString(e));
			}
			return results;
		}

		// start argument to pass to google
		int firstindex = count * page;
		// count of results to skip before adding them to the result array
		int skip = 0;
		// start indices > 56 are skipped by Google, so we
		// ask for results from 56, but skip the unwanted $skip indices
		if (firstindex > 63)
			return results;
		if (firstindex > 56)
		{
			skip = firstindex - 56;
			firstindex = 56;
		}

		boolean readMore = true; // do we need more queries and are they
									// possible?
		while (readMore)
		{
			// add start index to the query
			String currentQuery = query + firstindex;
			if (DEBUG)
				Log.d(TAG, "Searching: " + currentQuery);
			try
			{
				// prepare the connection
				URL url = new URL(currentQuery);
				URLConnection connection = url.openConnection();
				connection.addRequestProperty("Referer",
						GoogleSearchProvider.refererUrl);

				connection.setConnectTimeout(2000);
				connection.setReadTimeout(2000);
				connection.connect();

				// receive the results
				StringBuilder builder = new StringBuilder();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(connection.getInputStream()));
				String line;
				while ((line = reader.readLine()) != null)
				{
					builder.append(line);
				}
				// parse the results
				JSONObject json = new JSONObject(builder.toString());
				int responseStatus = json.getInt("responseStatus");
				if (responseStatus == 200)// successful search
				{
					json = json.getJSONObject("responseData");
					JSONArray res = json.getJSONArray("results");
					if (res.length() == 0)
						return results;
					String s;
					int limit = Math.min(res.length(), count - results.size()
							+ skip);
					for (int i = skip; i < limit; i++)
					{
						s = res.getJSONObject(i).getString("unescapedUrl");
						if (s != null)
							results.addElement(s);
					}
					// see if there are "more Results"
					JSONObject cursor = json.getJSONObject("cursor");
					JSONArray pages = cursor.getJSONArray("pages");
					int pageCount = pages.length();
					int currentPageIndex = cursor.getInt("currentPageIndex");
					this.moreResults = readMore = (pageCount - 1) > currentPageIndex;
				} else
				{
					if (DEBUG)
						Log.w(TAG, "Goole Search Error (Code "
								+ responseStatus + "):"
								+ json.getString("responseDetails"));
					this.moreResults = readMore = false;// prevent for (;;) loop
														// on errors
				}
			} catch (MalformedURLException e)
			{
				if (DEBUG)
				{
					Log.w(TAG, "MalformedURLException:"
							+ e.getMessage());
					Log.w(TAG, Log.getStackTraceString(e));
				}
				this.moreResults = readMore = false;
			} catch (IOException e)
			{
				if (DEBUG)
				{
					Log.w(TAG, "IOException:" + e.getMessage());
					Log.w(TAG, Log.getStackTraceString(e));
				}
				this.moreResults = readMore = false;
			} catch (JSONException e)
			{
				if (DEBUG)
				{
					Log.w(TAG, "JSONException:" + e.getMessage());
					Log.w(TAG, Log.getStackTraceString(e));
				}
				this.moreResults = readMore = false;
			}

			// read more only if we can read more AND want to have more
			readMore = readMore && results.size() < count;
			if (readMore)
			{
				firstindex += 8;
				if (firstindex > 56)// the last pages always need to start
									// querying at index 56 (or google returns
									// errors)
				{
					skip = firstindex - 56;
					firstindex = 56;
				}
			}
		}
		return results;
	}
}
