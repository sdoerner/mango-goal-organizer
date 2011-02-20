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

import android.graphics.Bitmap;

/**
 * Interface implementing an action to be taken if a requested number of Images
 * has been downloaded from the web.
 */
public interface ImageDownloadCompleteCallback
{
	/**
	 * Method to be called when all image downloads have completed.
	 *
	 * @param bitmaps
	 *            The resulting Bitmaps.
	 */
	public void action(Vector<Bitmap> bitmaps);
}
