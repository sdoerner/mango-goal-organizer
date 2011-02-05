package de.mango.business;

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

	private GoalDBOpenHelper dbHelper;
	private SQLiteDatabase db;

	
	public GoalProvider(Context context) throws SQLiteException {
		dbHelper = new GoalDBOpenHelper(context);
		db = dbHelper.getWritableDatabase();
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
		c.moveToFirst();
		Goal result = getGoalFromCursor(c);
		c.close();
		return result;
	}

	public ArrayList<Goal> getChildGoals(int parentId) {
		Cursor c = db.query(GOALS_TABLE_NAME, null, "parent=" + Integer.toString(parentId),
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
		ContentValues values = new ContentValues();
		values.put("name", g.getName());
		values.put("description", g.getDescription().length() > 0 ? g.getDescription(): "NULL");
		values.put("imageName", g.getImageName().length() > 0 ? g.getImageName(): "NULL");
		values.put("completion", g.getCompletion());
		values.put("completionWeight", g.getCompletionWeight());
		values.put("deadline", SDF.format(g.getDeadline().getTime()));
		values.put("timestamp", SDF.format(g.getTimestamp().getTime()));
		if (parent != -1)
			values.put("parent", parent);
		return db.insert(GOALS_TABLE_NAME, null, values);
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