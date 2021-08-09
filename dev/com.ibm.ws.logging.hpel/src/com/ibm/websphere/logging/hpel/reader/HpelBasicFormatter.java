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

import java.util.Locale;

/**
 * A HpelPlainFormatter subclass implementation that provides formatting of RepositoryLogRecord to a format
 * referred to as "Basic".
 */
public class HpelBasicFormatter extends HpelPlainFormatter {
	private static final int svBasicFormatMaxNameLength = 13;
	private static final String svBasicPadding = "                                 ";   
	private String svLineSeparatorPlusBasicPadding = lineSeparator + svBasicPadding;
	private String svLineSeparatorPlusBasicPaddingPlusNullParamString = svLineSeparatorPlusBasicPadding + nullParamString;

	private static String[] specialLoggers = { "LogService" };
	private static String[] specialLoggerReplacement = { "osgiLog" };

	/**
	 * Formats a RepositoryLogRecord into a localized basic format output String.
	 * 
	 * @param record  the RepositoryLogRecord to be formatted
	 * @param locale  the Locale to use for localization when formatting this record.
	 * 
	 * @return the formated string output.
	 */
	@Override
	public String formatRecord(RepositoryLogRecord record, Locale locale) {
		if (null == record) {
			throw new IllegalArgumentException("Record cannot be null");
		}
		StringBuilder sb = new StringBuilder(300);
		String lineSeparatorPlusPadding = "";


		// Use basic format
		createEventHeader(record, sb);
		lineSeparatorPlusPadding = svLineSeparatorPlusBasicPadding;

		// add the localizedMsg
		sb.append(formatMessage(record, locale));

		// if we have a stack trace, append that to the formatted output.
		if (record.getStackTrace() != null) {
			sb.append(lineSeparatorPlusPadding);
			sb.append(record.getStackTrace());
		}
		return sb.toString();
	}  

    /**
     * Footers do not exist for the Basic format, so return empty string.
     */
	@Override
	public String getFooter() {		
		return "";
	}


	/**
	 * Generates the basic format event header information that is common to all subclasses of RasEvent starting at
	 * position 0 of the specified StringBuffer.
	 * <p>
	 * Basic format specifies that the header information will include a formatted timestamp followed by an 8 (hex)
	 * digit thread Id, followed by a 13 character name field, followed by a type character. All fields will be
	 * separated by blanks and the resultant contents of the StringBuffer will also be terminated by a blank.
	 * <p>
	 * 
	 * @param record RepositoryLogRecord provided by the caller.
	 * @param buffer output buffer where converted entry is appended to.
	 */
	protected void createEventHeader(RepositoryLogRecord record, StringBuilder buffer) {
		if (null == record) {
			throw new IllegalArgumentException("Record cannot be null");
		}
		if (null == buffer) {
			throw new IllegalArgumentException("Buffer cannot be null");
		}

		// Create the time stamp
		createEventTimeStamp(record, buffer);

		// Add the formatted threadID
		formatThreadID(record, buffer);

		// Build the short 13 character name we display first as a convenience mechanism. This is either the name of the
		// JRas logger or the Tr component that registered.
		String name = record.getLoggerName();
		boolean isSpecialLogger = false;
		if (name == null) {
			name = "";
		}

		for (int i = 0; i < specialLoggers.length; i++) {
			if (name.trim().startsWith(specialLoggers[i])) {
				try {
					name = specialLoggerReplacement[i] + name.substring(name.indexOf("-") , name.indexOf("-", name.indexOf("-")+1));
					isSpecialLogger = true;
				}
				catch (StringIndexOutOfBoundsException e) { }
			}
		}

		if (!isSpecialLogger) {
			int index = name.lastIndexOf('.') + 1;
			if ((index + svBasicFormatMaxNameLength) >= name.length()) {
				name = name.substring(index);
			} else {
				name = name
						.substring(index, index + svBasicFormatMaxNameLength);
			}
		}
		buffer.append(name);
		// pad the string buffer out to svBasicFormatMaxNameLength chars with blanks for readability.
		int tmpLen = svBasicFormatMaxNameLength - name.length();
		for (int i = tmpLen; i > 0; --i) {
			buffer.append(' ');
		}
		buffer.append(mapLevelToType(record));

		// for JRas records append the className and methodName. For Orb entries, the methodNam will have the threadName
		// appended to it.
		String classname = record.getSourceClassName();
		String methodname = record.getSourceMethodName();
		if (classname != null) {
			buffer.append(classname);
		}
		buffer.append(' ');
		if (methodname != null) {
			buffer.append(methodname);
		}
		buffer.append(' ');
		// END code extracted from TraceLogFormatter.formatHeaderBasic
	}

	protected String appendUnusedParms(String message, Object[] args) {
		  String returnValue = message;

		  // parms were present but message format won't take them.  Append parms as desired.
		  StringBuilder buffer = new StringBuilder();
		  for (int i = 0; i < args.length; ++i) {
			if (args[i] == null)
				buffer.append(svLineSeparatorPlusBasicPaddingPlusNullParamString);
			else {
				buffer.append(svLineSeparatorPlusBasicPadding);
				buffer.append(args[i].toString());
			}
		  }
		  returnValue = returnValue.concat(buffer.toString());
		  return returnValue;
	}

	@Override
	public void setLineSeparator(String lineSeparator) {
		super.setLineSeparator(lineSeparator);
		svLineSeparatorPlusBasicPadding = lineSeparator + svBasicPadding;
		svLineSeparatorPlusBasicPaddingPlusNullParamString = svLineSeparatorPlusBasicPadding + nullParamString;
	}


}
