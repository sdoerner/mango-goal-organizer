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

import java.util.Vector;

/**
 * Interface that offers a utility for searching for images in the web using a given query. 
 */
public interface ImageSearchProvider
{
	/**
	 * Returns the image URLs of an Image search
	 * 
	 * @param query The keyword to search for.
	 * @param count The count of results wanted.
	 * @param page The Page of results. The first result index will be page * count.
	 * @return A Vector with Image URLs.
	 */
	public abstract Vector<String> search(String query, int count, int page);
	
	/**
	 * Tells whether there are more results on the next page (i.e. if there "is" a next page).
	 * @return True if there are more results.
	 */
	public abstract boolean hasMoreResults();
}
