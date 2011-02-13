package de.mango.business;

import java.io.File;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class GoalProvider
{
	public static final boolean DEBUG = false;
	public static final String TAG = "Mango";
	
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME="mango.db";
	private static final String GOALS_TABLE_NAME = "goals";
	private static final SimpleDateFormat SDF = new SimpleDateFormat(
		"yyyy-MM-dd"); 

 	private static class GoalDBOpenHelper extends SQLiteOpenHelper
	{
		GoalDBOpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + GOALS_TABLE_NAME + " (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT," +
					"parent INTEGER REFERENCES " + GOALS_TABLE_NAME + "(id)," +
					"name TEXT NOT NULL," +
					"description TEXT," +
					"imageName TEXT," + 
					"completion INTEGER NOT NULL," +
					"completionWeight INTEGER NOT NULL,"+
					"deadline TEXT NOT NULL," +
					"timestamp TEXT NOT NULL)");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			// initial version needs no upgrade
		}
	}

	private Context context;
	private GoalDBOpenHelper dbHelper;
	private SQLiteDatabase db;

	
	public GoalProvider(Context context) throws SQLiteException {
		dbHelper = new GoalDBOpenHelper(context);
		db = dbHelper.getWritableDatabase();
		this.context = context;
	}
	
	public int getNumTopLevelGoals() {
		Cursor c = db.query(GOALS_TABLE_NAME, new String[]{ "COUNT(id)" }, "parent is NULL", null, null, null, null);
		c.moveToFirst();
		int count = c.getInt(0);
		c.close();
		return count;
	}
	
	public Goal getTopLevelGoal(int position) {
		Cursor results = db.query(GOALS_TABLE_NAME, null, "parent is NULL", null, null, null, null);
		results.moveToPosition(position);
		Goal g = getGoalFromCursor(results);
		results.close();
		return g;
	}
	
	public Goal getGoalWithId(long id) {
		Cursor c = db.query(GOALS_TABLE_NAME, null, "id=" + Long.toString(id),
				null, null, null, null);
		if (!c.moveToFirst()) {
			c.close();
			return null;
		}
		Goal result = getGoalFromCursor(c);
		c.close();
		return result;
	}

	public boolean hasChildren(long parentId) {
		Cursor c = db.query(GOALS_TABLE_NAME, new String[]{ "COUNT(id)" }, "parent=" + Long.toString(parentId),
				null, null, null, null);
		c.moveToFirst();
		int count = c.getInt(0);
		c.close();
		return count > 0;
	}

	/**
	 * @param parentId id of the parent goal
	 * @return possibly empty vector of child goals
	 */
	public ArrayList<Goal> getChildGoals(long parentId) {
		Cursor c = db.query(GOALS_TABLE_NAME, null, "parent=" + Long.toString(parentId),
				null, null, null, null);
		ArrayList<Goal> results = new ArrayList<Goal>();
		for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
			results.add(getGoalFromCursor(c));
		}
		c.close();
		return results;
	}

	/**
	 * @param g the Goal to insert
	 * @param parent ID of the parent, or -1 if topLevelGoal
	 * @return ID of the new Goal
	 */
	public long insertGoal(Goal g, long parent) {
		ContentValues values = extractValues(g);
		if (parent != -1)
			values.put("parent", parent);
		long newId = db.insert(GOALS_TABLE_NAME, null, values);
		updateParentCompletion(newId);
		return newId;
	}

	public boolean updateGoal(long id, Goal g) {
		boolean hasChildren = hasChildren(id);
		final int affectedRows =
			db.update(GOALS_TABLE_NAME, extractValues(g, !hasChildren), "id=" + Long.toString(id), null);
		return (affectedRows == 1 && updateParentCompletion(id));
	}

	public boolean deleteGoal(long id) {
		return deleteGoal(id, null);
	}

	private boolean deleteGoal(long id, String imageName) {
		if (hasChildren(id)) {
			Cursor c = db.query(GOALS_TABLE_NAME, new String[] { "id", "imageName"}, "parent=" + Long.toString(id),
					null, null, null, null);
			c.moveToFirst();
			while (!c.isAfterLast()) {
				deleteGoal(c.getLong(0), c.getString(1));
				c.moveToNext();
			}
			c.close();
		}
		return deleteGoalNonRecursive(id, imageName);
	}

	private boolean deleteGoalNonRecursive(long id, String imageName) {
		if (imageName == null)
			imageName = getImageName(id);
		deleteImage(imageName);
		final int affectedRows =
			db.delete(GOALS_TABLE_NAME, "id=" + id, null);
		return affectedRows == 1;
	}

	private void deleteImage(String imageName) {
		if (imageName == null)
			return;
		File imageFile = context.getFileStreamPath(imageName);
		if (imageFile.exists())
			imageFile.delete();
	}

	/**
	 * Updates the completion of a non-leaf with the completion of all children
	 * @param goalId the id of the goal to update
	 * @return True if successful, false otherwise, in particular if the goal is a leaf.
	 */
	public boolean updateGoalCompletion(long goalId) {
		Cursor c = db.query(GOALS_TABLE_NAME, new String[]{ "completion", "completionWeight" },
				"parent=" + Long.toString(goalId), null, null, null, null);
		c.moveToFirst();
		if (c.isAfterLast()) {
			c.close();
			return false;
		}
		// calculate completion
		int newCompletion = 0;
		int weight = 0;
		while (!c.isAfterLast()) {
			weight += c.getInt(1);
			newCompletion += (c.getInt(0) * c.getInt(1));
			c.moveToNext();
		}
		c.close();
		// normalize the degree of completion
		newCompletion = newCompletion / weight;
		// update entry
		if (!updateGoalCompletionNonRecursive(goalId, newCompletion))
			return false;
		return updateParentCompletion(goalId);
	}

	public boolean updateGoalCompletion(long goalId, int newCompletion) {
		if (hasChildren(goalId))
			return false;
		ContentValues values = new ContentValues();
		values.put("completion", newCompletion);
		final int affectedRows =
			db.update(GOALS_TABLE_NAME, values, "id=" + Long.toString(goalId), null);
		if (affectedRows == 1)
			return updateParentCompletion(goalId);
		return false;
	}

	private boolean updateParentCompletion(long goalId) {
		long parentId = getParentId(goalId);
		if (parentId != -1)
			return updateGoalCompletion(parentId);
		return true;
	}

	private boolean updateGoalCompletionNonRecursive(long goalId, int newCompletion) {
		ContentValues values = new ContentValues();
		values.put("completion", newCompletion);
		final int affectedRows =
			db.update(GOALS_TABLE_NAME, values, "id=" + Long.toString(goalId), null);
		return affectedRows == 1;
	}

	/**
	 * @param goalId ID of the goal, whose parent we are interested in.
	 * @return The ID of the parent or -1 if goalId belongs to TLG.
	 */
	public long getParentId(long goalId) {
		Cursor c = db.query(GOALS_TABLE_NAME, new String[] { "parent" }, "id=" + Long.toString(goalId), 
				null, null, null, null);
		c.moveToFirst();
		if (c.isAfterLast() || c.isNull(0)) {
			c.close();
			return -1;
		}
		long result = c.getLong(0);
		c.close();
		return result;
	}

	private String getImageName(long goalId) {
		Cursor c = db.query(GOALS_TABLE_NAME, new String[] { "imageName" },
				"id=" + Long.toString(goalId), null, null, null, null);
		c.moveToFirst();
		if (c.isAfterLast() || c.isNull(0)) {
			c.close();
			return null;
		}
		String result = c.getString(0);
		c.close();
		return result;
	}

	private ContentValues extractValues(Goal g) {
		return extractValues(g, true);
	}

	private ContentValues extractValues(Goal g, boolean withCompletion) {
		ContentValues values = new ContentValues();
		values.put("name", g.getName());
		values.put("description", g.getDescription().length() > 0 ? g.getDescription() : null);
		values.put("imageName", g.getImageName().length() > 0 ? g.getImageName() : null);
		if (withCompletion)
			values.put("completion", g.getCompletion());
		values.put("completionWeight", g.getCompletionWeight());
		values.put("deadline", SDF.format(g.getDeadline().getTime()));
		values.put("timestamp", SDF.format(g.getTimestamp().getTime()));
		return values;
	}

	private Goal getGoalFromCursor(Cursor c) {
		String description = c.isNull(3) ? "" : c.getString(3);
		GregorianCalendar deadline = new GregorianCalendar();
		try
		{
			deadline.setTime(SDF.parse(c.getString(7)));
		}
		catch (ParseException e)
		{
			if (DEBUG)
				Log.w(TAG, "No deadline found or wrong format.");
		}
		Goal g = new Goal(c.getString(2), description, deadline);
		g.setId(c.getLong(0));
		g.setImageName(c.isNull(4) ? "" : c.getString(4));
		g.setCompletion(c.getInt(5));
		g.setCompletionWeight(c.getInt(6));
		GregorianCalendar timestamp = new GregorianCalendar();
		try
		{
			timestamp.setTime(SDF.parse(c.getString(8)));
		}
		catch (ParseException e)
		{
			if (DEBUG)
				Log.w(TAG, "No timestamp found or wrong format.");
		}
		return g;
	}
	

	//temporary for debug
	
	public void wipe() throws SQLException {
		db.execSQL("DROP TABLE " + GOALS_TABLE_NAME);
		dbHelper.onCreate(db);
	}

	public void execSQL(String sql) throws SQLException{
		db.execSQL(sql);
	}
}