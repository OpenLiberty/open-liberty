/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.logging.hpel.reader;

import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;


/**
 * Abstract class for HpelFormatter implementations that have a non-XML based format
 */
public abstract class HpelPlainFormatter extends HpelFormatter {
	
	/**
	 * Gets the file header information.  Implementations of the HpelPlainFormatter class
	 * will have a non-XML-based header.
	 * 
	 * @return  the formatter's header as a String
	 */
	@Override
	public String[] getHeader() {
		ArrayList<String> result = new ArrayList<String>();
		
		if (customHeader.length > 0) {
			for (CustomHeaderLine line: customHeader) {
				String formattedLine = line.formatLine(headerProps);
				if (formattedLine != null) {
					result.add(formattedLine);
				}
			}
		} else {
			for (String prop: headerProps.stringPropertyNames()) {
				result.add(prop + " = " + headerProps.getProperty(prop));
			}
		}
		
		return result.toArray(new String[result.size()]);
	}
	
	/**
	 * Generates the time stamp for the RepositoryLogRecord event. The resulting time stamp is formatted
	 * based on the formatter's locale and time zone.
	 * 
	 * @param record of the event.
	 * @param buffer output buffer where converted timestamp is appended to.
	 */
	protected void createEventTimeStamp(RepositoryLogRecord record, StringBuilder buffer) {
		if (null == record) {
			throw new IllegalArgumentException("Record cannot be null");
		}
		if (null == buffer) {
			throw new IllegalArgumentException("Buffer cannot be null");
		}
		// Create the time stamp
		buffer.append('[');
		Date eventDate = new Date(record.getMillis());
		// set the dateFormat object to the desired timeZone. Allows log output
		// to be presented in different time zones.
		dateFormat.setTimeZone(timeZone); // set the format to
		// the desired
		// time zone.
		buffer.append(dateFormat.format(eventDate));
		buffer.append("] ");
	}
	
	/**
	 * Generates the short type string based off the RepositoryLogRecord's Level.
	 * 
	 * @param logRecord 
	 * 
	 * @return String - The type.
	 */
	protected static String mapLevelToType(RepositoryLogRecord logRecord) {
		if (null == logRecord) {
			return " Z ";
		}
		Level l = logRecord.getLevel();
		if (null == l) {
			return " Z ";
		}
		// In HPEL SystemOut and SystemErr are recorded in custom level but since we want
		// them to be handled specially do it now before checking for custom levels
		String s = logRecord.getLoggerName();
		if (s != null) {
			if (s.equals("SystemOut")) {
				return " O ";
			} else if (s.equals("SystemErr")) {
				return " R ";
			}
		}
		String id = customLevels.get(l);
		if (id != null) {
			return (" " + id + " ");
		}
		// Since we have used the static Level object throughout the logging framework
		// object reference comparisons should work.
		if (l == Level.SEVERE) {
			return " E ";
		}
		if (l == Level.WARNING) {
			return " W ";
		}
		if (l == Level.INFO) {
			return " I ";
		}
		if (l == Level.CONFIG) {
			return " C ";
		}
		if (l == Level.FINE) {
			return " 1 ";
		}
		if (l == Level.FINER) {
			// D198403 Start
			// added check for Entry and Exit messages to return < or >
			String message = logRecord.getRawMessage();
			if (message != null) {
				if (message.indexOf("Entry") != -1 || message.indexOf("ENTRY") != -1) {
					return " > ";
				}
				if (message.indexOf("Exit") != -1 || message.indexOf("RETURN") != -1) {
					return " < ";
				}
			}
			return " 2 ";
			// D198403 End
		}
		if (l == Level.FINEST) {
			return " 3 ";
		}
		return " Z ";
	}	

}
