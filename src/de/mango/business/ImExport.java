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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.content.Context;
import android.util.Log;

/**
 * Class to handle all import and export (currently xml and ics)
 *
 */
public class ImExport
{
	/**
	 * Enable/disable logging. The compiler will strip all logging if disabled.
	 */
	public static final boolean DEBUG = false;
	public static final String TAG = "Mango";

	// ISO date format
	private static final SimpleDateFormat SDF = new SimpleDateFormat(
			"yyyy-MM-dd");
	private static String[] mFileList;

	/**
	 * Imports Goal from an XML file
	 *
	 * @param gp
	 *            GoalProvider accessing the data back end
	 * @param filename
	 *            Name of the XML file to read from
	 * @param context
	 *            Context(Activity) needed to get a file handle
	 * @return
	 */
	public static boolean importFromXml(GoalProvider gp, String filename,
			Context context)
	{
		// assign the file
		FileInputStream in;
		boolean externalFile = filename.contains("/");
		try
		{
			in =  externalFile ? new FileInputStream(filename)
					: context.openFileInput(filename);
		} catch (FileNotFoundException e)
		{
			return false;
		}

		// create DOM document
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder;
		try
		{
			documentBuilder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException ex)
		{
			if (DEBUG)
				Log.w(TAG, ex.getMessage());
			return false;
		}
		// read the file and import its contents into DOM
		Document doc;
		try
		{
			doc = documentBuilder.parse(in);
			in.close();
		} catch (SAXException e)
		{
			if (DEBUG)
			{
				Log.w(TAG, "Not a valid XML file.");
				Log.w(TAG, e.getMessage());
			}
			return false;
		} catch (IOException e)
		{
			if (DEBUG)
				Log.w(TAG, e.getMessage());
			return false;
		}

		// parse DOM
		Element mangoElem = doc.getDocumentElement();
		// System.out.println(mangoElem.getTagName());

		mFileList = context.fileList();
		NodeList goalElements = mangoElem.getChildNodes();
		Node n;
		for (int i = 0; i < goalElements.getLength(); i++)
		{
			n = goalElements.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE)
				traverseFromXml(gp, (Element) n, -1);
		}

		return true;
	}

	private static void traverseFromXml(GoalProvider gp, Element root, long parent)
	{
		// exit if tag is no goal-tag
		if (root.getTagName().compareToIgnoreCase("goal") != 0)
			return;

		// read deadline
		GregorianCalendar cal = new GregorianCalendar();
		Date d;
		try
		{
			d = SDF.parse(root.getAttribute("deadline"));
			cal.setTime(d);
			//new dates are set to this time, so they are not in the past
			//for proper "parent isn't earlier"-check old times need the same values
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
		} catch (ParseException e)
		{
			if (DEBUG)
				Log.w(TAG, "No deadline found or wrong format.");
		}
		Goal g = new Goal(root.getAttribute("name"), root
				.getAttribute("description"), cal);
		g.setCompletionWeight(root.getAttribute("weight").equals("") ? 1
				: Integer.valueOf(root.getAttribute("weight")), false);
		g.setCompletion(root.getAttribute("completion").equals("") ? 0
				: Integer.valueOf(root.getAttribute("completion")));

		// take over image name if image existent
		String imgName = root.getAttribute("imageName");
		g.setImageName(FileExists(imgName) ? imgName : "");
		// read time stamp
		cal = new GregorianCalendar();
		try
		{
			d = SDF.parse(root.getAttribute("timestamp"));
			cal.setTime(d);
		} catch (ParseException e)
		{
			if (DEBUG)
				Log.w(TAG, "No timestamp found or wrong format.");
		}
		g.setTimestamp(cal);
		//insert goal into DB
		long goalid = gp.insertGoal(g, parent);

		// recurse for children
		if (root.hasChildNodes())
		{
			NodeList children = root.getChildNodes();
			Node node;
			for (int i = 0; i < children.getLength(); i++)
			{
				node = children.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE)
					traverseFromXml(gp, (Element) node, goalid);
			}
		}
	}

	/**
	 * Checks if a file with a given name exists in the app directory.
	 *
	 * @param filename
	 *            Name of the file to check.
	 * @return True if there is a file with the given name
	 */
	private static boolean FileExists(String filename)
	{
		String[] localFiles = mFileList;
		for (String s : localFiles)
		{
			if (s.compareTo(filename) == 0)
				return true;
		}
		return false;
	}

	/**
	 * Exports the goals from a given GoalProvider to XML
	 *
	 * @param gp
	 *            GoalProvider giving access to the goals
	 * @param filename
	 *            name of the XML file to save the goals to
	 * @param context
	 *            context(activity) needed to get a file handle
	 * @return
	 */
	public static boolean exportToXml(GoalProvider gp, String filename,
			Context context)
	{
		try
		{
			FileOutputStream os = filename.contains("/") ? new FileOutputStream(
					filename)
					: context.openFileOutput(filename,
							Context.MODE_WORLD_READABLE);
			os.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n".getBytes());
			StringBuilder sb = new StringBuilder();
			sb.append("<mango>\n");
			for (Goal g : gp.getTopLevelGoals())
				traverseToXml(gp, g, sb, 1);
			sb.append("</mango>");
			os.write(sb.toString().getBytes());
			os.close();
		}
		catch (Exception e)
		{
			if (DEBUG)
				Log.w(TAG, "Error exporting to XML:" + e.getMessage());
			return false;
		}

		return true;
	}
	/**
	 * Traverses a tree of goals ands adds their XML representation to a
	 * StringBuilder
	 *
	 * @param gp
	 * 			  GoalProvider giving access to the goals
	 * @param goal
	 *            Root of the goal tree to be converted
	 * @param s
	 *            StringBuilder to pick up the string representation
	 * @param depth
	 *            Depth in the goal tree to control XMl indentation
	 */
	private static void traverseToXml(GoalProvider gp, Goal goal, StringBuilder s, int depth)
	{
		for (int i = 0; i < depth; i++)
			s.append("   ");
		s.append("<goal name=\"");
		s.append(goal.getName());
		s.append("\" description=\"");
		s.append(goal.getDescription());
		s.append("\" imageName=\"");
		s.append(goal.getImageName());

		s.append("\" completion=\"");
		s.append(goal.getCompletion());
		s.append("\" weight=\"");
		s.append(goal.getCompletionWeight());

		s.append("\" deadline=\"");
		s.append(SDF.format(goal.getDeadline().getTime()));
		s.append("\" timestamp=\"");
		s.append(SDF.format(goal.getTimestamp().getTime()));
		s.append("\"");
		ArrayList<Goal> children = gp.getChildGoals(goal.getId());
		if (children.isEmpty())
			s.append(" />\n");
		else
		{
			s.append(">\n");
			for (Goal c : children)
				traverseToXml(gp, c, s, depth + 1);
			for (int i = 0; i < depth; i++)
				s.append("   ");
			s.append("</goal>\n");
		}
	}

	/**
	 * @param tree
	 *            Goals to be exported
	 * @param context
	 *            Context needed to get a file handle.
	 * @param exportLeavesOnly
	 *            If true only leaves will be exported
	 */
	public static boolean exportToICS(GoalCrud tree, Context context,
			String filename, boolean exportLeavesOnly)
	{
		if (tree == null)
			return false;
		Vector<Goal> termine = null;
		// export only leaves
		if (exportLeavesOnly == true)
		{
			termine = tree.getLeaves();
		}
		// export all Goals
		else
			termine = tree.getAllGoals();
		try
		{
			FileOutputStream out = filename.contains("/") ? new FileOutputStream(
					filename)
					: context.openFileOutput(filename,
							Context.MODE_WORLD_READABLE);
			out.write("BEGIN:VCALENDAR".getBytes());
			out.write(System.getProperty("line.separator").getBytes());
			out.write("CALSCALE:GREGORIAN".getBytes());
			out.write(System.getProperty("line.separator").getBytes());
			out.write("METHOD:PUBLISH".getBytes());
			out.write(System.getProperty("line.separator").getBytes());
			out.write("VERSION:2.0".getBytes());
			out.write(System.getProperty("line.separator").getBytes());
			// Parse of calendar entries
			Goal p;
			GregorianCalendar greg = new GregorianCalendar();

			for (int i = 0; i < termine.size(); i++)
			{
				p = termine.get(i);
				out.write("BEGIN:VEVENT".getBytes());
				out.write(System.getProperty("line.separator").getBytes());
				out.write("DTSTART;VALUE=DATE:".getBytes());
				out.write(ImExport.toIcsString(p.getDeadline(), true)
						.getBytes());
				out.write(System.getProperty("line.separator").getBytes());
				out.write("CREATED:".getBytes());
				out.write(ImExport.toIcsString(p.getTimestamp(), false)
						.getBytes());
				out.write(System.getProperty("line.separator").getBytes());
				out.write("SUMMARY:".getBytes());
				out.write(p.getName().getBytes());
				out.write(System.getProperty("line.separator").getBytes());
				out.write("DESCRIPTION:".getBytes());
				out.write(p.getDescription().getBytes());
				out.write(System.getProperty("line.separator").getBytes());
				out.write("DTEND;VALUE=DATE:".getBytes());
				// Goals have a time frame of a full day
				greg = p.getDeadline();
				greg.add(Calendar.DAY_OF_MONTH, 1);
				out.write(ImExport.toIcsString(greg, true).getBytes());
				out.write(System.getProperty("line.separator").getBytes());
				out.write("END:VEVENT".getBytes());
				out.write(System.getProperty("line.separator").getBytes());
			}
			out.write("END:VCALENDAR".getBytes());
		} catch (Exception f)
		{// Catch exception if any
			if (DEBUG)
				Log.w(TAG, "Error in exportToICS: " + f.getMessage());
		}
		return true;
	}

	/**
	 * Gets the ICS time stamp for a given Calendar.
	 * @param cal
	 *            Calendar to be converted.
	 * @param brief
	 *            Use short format (date only)
	 * @return String in form of YYYYMMDD or YYYYMMDDTHHMMSS with T as separator
	 *         between date and time
	 */
	private static String toIcsString(GregorianCalendar cal, boolean brief)
	{
		String s = "";
		s += cal.get(Calendar.YEAR);
		if (cal.get(Calendar.MONTH) < 10)
			s += "0";
		s += cal.get(Calendar.MONTH);
		if (cal.get(Calendar.DAY_OF_MONTH) < 10)
			s += "0";
		s += cal.get(Calendar.DAY_OF_MONTH);

		if (brief == false)
		{
			s += "T";
			if (cal.get(Calendar.HOUR_OF_DAY) < 10)
				s += "0";
			s += cal.get(Calendar.HOUR_OF_DAY);
			if (cal.get(Calendar.MINUTE) < 10)
				s += "0";
			s += cal.get(Calendar.MINUTE);
			if (cal.get(Calendar.SECOND) < 10)
				s += "0";
			s += cal.get(Calendar.SECOND);
		}
		return s;
	}
}
