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

import android.content.Context;

/**
 * Class to operate on goals and encapsulate a current working set of goals.
 */

public class GoalCrud
{
	/**
	 * Field to be used temporarily to pass goal references to the next screen
	 * without serialization
	 */
	public static Goal currentGoal = null;
	// --------------Singleton stuff------------------------
	private static GoalCrud instance = null;

	/**
	 * Get unique instance of the global GoalCrud object
	 * 
	 * @param context
	 *            Context to use for reading Goals from disk
	 * @return The GoalCrud instance.
	 */
	public static GoalCrud getInstance(Context context)
	{
		if (instance == null)
		{
			instance = new GoalCrud();
			ImExport.importFromXML(instance, "goals.mango", context);
			instance.mDataChanged = false;
		}
		return instance;
	}

	/**
	 * Private constructor to prevent creation of more than one GoalCrud object
	 */
	private GoalCrud()
	{
		topLevelGoals = new Vector<Goal>();
	}

	// --------------Singleton stuff ends--------------------

	/**
	 * First hierarchy level of our Goals. All subgoals will only be reachable
	 * through the child-pointers of these.
	 */
	private Vector<Goal> topLevelGoals;
	/**
	 * True if the data in this object is not equivalent to the standard XML
	 * file any more.
	 */
	private boolean mDataChanged;

	/**
	 * Mark GoalCrud contents as changed to ensure changes are written to disk
	 * OnPause.
	 */
	public void setDataChanged()
	{
		this.mDataChanged = true;
	}

	/**
	 * Save current GoalCrud state to standard XML file in on permanent memory.
	 * 
	 * @param context
	 *            Context used to write out the file.
	 */
	public void saveToDisk(Context context)
	{
		if (mDataChanged)
			if (ImExport.exportToXML(this, "goals.mango", context))
				mDataChanged = false;
	}

	/**
	 * Get the top level of our goal hierachy.
	 * @return The top level goal vector
	 */
	public Vector<Goal> getTopLevelGoals()
	{
		return topLevelGoals;
	}

	/**
	 * Checks of there are any goals present in this GoalCrud.
	 * @return True if the GoalCrud object doesn't contain any Goals.
	 */
	public boolean isEmpty()
	{
		return topLevelGoals.isEmpty();
	}

	/**
	 * Find all leaves in the GoalCrud.
	 * @return A Vector with all leaves.
	 */
	public Vector<Goal> getLeaves()
	{
		Vector<Goal> v = new Vector<Goal>();
		return addLeaves(topLevelGoals, v);
	}

	/**
	 * Removes a goal from the tree, caring for all cleanup stuff.
	 * @param g Goal that should be deleted
	 * @param c Context to access the app's file system
	 * @return true if g was a top level goal
	 */
	public boolean removeFromTree(Goal g, Context c)
	{
		this.setDataChanged();
		g.wipeImages(c);
		Goal p = g.getParent();
		if (p == null)// g is toplevel Goal
		{
			this.getTopLevelGoals().remove(g);
			return true;
		}
		else
		{
			p.removeChild(g);
			return false;
		}
	}

	/**
	 * Clears the goal tree
	 * @param c context to access the app only file system
	 * @param withImages true if the images on disk shall be deleted, too
	 */
	public void clear(Context c, boolean withImages)
	{
		if (withImages)
			for (Goal g:topLevelGoals)
				g.wipeImages(c);
		topLevelGoals.clear();
	}

	/**
	 * Adds all leaves of the roots vector to the leaves vector.
	 * @param roots
	 *            Vector of roots of goal trees whose leaves are sought
	 * @param leaves
	 *            Vector the leaves should be added to
	 * @return All leaves of a vector of goals
	 */
	private Vector<Goal> addLeaves(Vector<Goal> roots, Vector<Goal> leaves)
	{
		if (roots.isEmpty())
			return leaves;
		for (Goal currentGoal:roots)
		{
			if (currentGoal.getChildren() == null || currentGoal.getChildren().isEmpty())
			{
				leaves.add(currentGoal);
			} else
			{
				leaves = addLeaves(currentGoal.getChildren(), leaves);
			}
		}
		return leaves;
	}

	/**
	 * Determines whether node is valid and actually has children.
	 * 
	 * @param node
	 *            Goal to be checked.
	 * @return True if node has at least one child.
	 */
	private boolean hasChildren(Goal node)
	{
		return node != null && node.getChildren() != null
				&& !node.getChildren().isEmpty();
	}

	/**
	 * Get all Goals using post order traversal.
	 * 
	 * @return Vector containing all Goals of this GoalCrud.
	 */
	public Vector<Goal> getAllGoals()
	{
		if (!topLevelGoals.isEmpty())
		{

			return postOrderTraverse(topLevelGoals, new Vector<Goal>());
		}
		return topLevelGoals;
	}

	/**
	 * Adds all Goals beneath roots to currentResults in Post Order. 
	 * @param roots
	 *            Roots of the trees to be traversed
	 * @param currentResults
	 *            Results gathered until now, new results will be added to this
	 *            vector
	 * @return CurrentResults complemented by all goals underneath the root
	 *         nodes
	 */
	private Vector<Goal> postOrderTraverse(Vector<Goal> roots,
			Vector<Goal> currentResults)
	{
		for (Goal current : roots)
		{
			if (hasChildren(current))
				postOrderTraverse(current.getChildren(), currentResults);
			currentResults.add(current);
		}
		return currentResults;
	}

}
