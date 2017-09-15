/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.logging.hpel.reader;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.logging.Level;

import com.ibm.ws.logging.hpel.Guid;
import com.ibm.ws.logging.internal.WsLogRecord;

/**
 * A HpelFormatter subclass implementation that provides formatting of RepositoryLogRecord to a CBE format. 
 */
public class HpelCBEFormatter extends HpelFormatter{

	private final static String CBE_HEADER_STRING = "<CommonBaseEvents xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"commonbaseevent1_0.xsd\">";
	private final static String CBE_FOOTER_STRING = "</CommonBaseEvents>";

	private static final String GUID_PREFIX = "CE";
	private final static SimpleDateFormat CBE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	private static final String INDENT[] = { "\t", "\t\t", "\t\t\t", "\t\t\t\t", "\t\t\t\t\t" };	

	private static final String STR_DEFAULT_LOCALIZATION        = "default";
	private static final String STR_REQUIRES_LOCALIZATION       = "yes";
	private static final String STR_REQUIRES_NO_LOCALIZATION    = "no";

	
	/**
	 * Creates a HpelCBEFormatter instance
	 */
	public HpelCBEFormatter() {}

	/**
	 * Formats a RepositoryLogRecord into a localized CBE format output String.
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

		return getFormattedRecord(record, locale);
	}

	@Override
	public String getFooter(){    
		return CBE_FOOTER_STRING;
	}

	@Override
	public String[] getHeader(){    
		return new String[]{CBE_HEADER_STRING};
	}

	/**
	 * Gets a String representation of a log record as a CBEEvent XML element
	 * 
	 * @param record of the event.
	 * @param locale the locale to use when printing the output.
	 * 
	 * @return String representation of the record in CBE format.
	 */ 
	public String getFormattedRecord(RepositoryLogRecord record, Locale locale) {
		StringBuilder sb = new StringBuilder(300);

		//create opening tag for the CBE Event
		createEventOTag(sb, record, locale);
		
		//add the CBE elements
		createExtendedElement(sb, record);
		createExtendedElement(sb, "CommonBaseEventLogRecord:sequenceNumber", "long", String.valueOf(record.getSequence()));
		createExtendedElement(sb, "CommonBaseEventLogRecord:threadID", "int", String.valueOf(record.getThreadID()));
		if (record.getLoggerName() != null){
			createExtendedElement(sb, "CommonBaseEventLogRecord:loggerName", "string", record.getLoggerName());
		}

		for (String name: record.getExtensions().keySet()) {
			// Don't report pthreadId here since it is reported as part of the sourceComponentId.
			if (!RepositoryLogRecord.PTHREADID.equals(name)) {
				createExtendedElement(sb, record, name);
			}
		}
		
		if (headerProps.getProperty(ServerInstanceLogRecordList.HEADER_VERSION) != null)
			createExtendedElement(sb, "version", "string", headerProps.getProperty(ServerInstanceLogRecordList.HEADER_VERSION));
		if (headerProps.getProperty(ServerInstanceLogRecordList.HEADER_PROCESSID) != null)
			createExtendedElement(sb, "processId", "string", headerProps.getProperty(ServerInstanceLogRecordList.HEADER_PROCESSID));
		if (headerProps.getProperty(ServerInstanceLogRecordList.HEADER_SERVER_NAME) != null)
			createExtendedElement(sb, "processName", "string", headerProps.getProperty(ServerInstanceLogRecordList.HEADER_SERVER_NAME));

		
		switch (record.getLocalizable()) {
		case WsLogRecord.REQUIRES_LOCALIZATION :
			createExtendedElement(sb, "localizable", "string", STR_REQUIRES_LOCALIZATION);     //yes
			break;
		case WsLogRecord.REQUIRES_NO_LOCALIZATION:
			createExtendedElement(sb, "localizable", "string", STR_REQUIRES_NO_LOCALIZATION);  //no	
			break;
		default :
			createExtendedElement(sb, "localizable", "string", STR_DEFAULT_LOCALIZATION);  //default
		break;  
		}

		createSourceElement(sb, record);
		createMessageElement(sb, record);
		createSituationElement(sb);
		
		//create closing tag for the CBE Event
		createEventCTag(sb);
		
		return sb.toString();
	}
	
	/**
	 * Appends the closing tag of a CommonBaseEvent XML element to a string buffer
	 *  
	 * @param sb       the string buffer the tag will be added to 
	 */
	private void createEventCTag(StringBuilder sb){
		//create closing tag for the CBE Event
		sb.append(lineSeparator).append("</CommonBaseEvent>");			
	}
	
	/**
	 * Appends the opening tag of a CommonBaseEvent XML element to a string buffer
	 *  
	 * @param sb       the string buffer the tag will be added to 
	 * @param record   the record that represents the common base event
	 * @param locale   locale to be used for formatting the log record into CBE	 
	 */
	private void createEventOTag(StringBuilder sb, RepositoryLogRecord record, Locale locale){
		sb.append("<CommonBaseEvent creationTime=\"");
		// create the XML dateTime format
		// TimeZone is UTC, but since we are dealing with Millis we are already in UTC.
		sb.append(CBE_DATE_FORMAT.format(record.getMillis()));
		sb.append("\"");

		// create and add the globalInstanceId
		sb.append(" globalInstanceId=\"").append(GUID_PREFIX).append(Guid.generate()).append("\"");

		// write out the msg
		sb.append(" msg=\"").append(formatMessage(record, locale)).append("\"");

		short severity = 0;
		if (record.getLevel().intValue() >= Level.SEVERE.intValue()) {
			severity = 50;
		} else if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
			severity = 30;
		} else {
			severity = 10;
		}

		sb.append(" severity=\"").append(severity).append("\"");
		sb.append(" version=\"1.0.1\">");		
	}
	
	/**
	 * Appends the CBE Situation XML element of a record to a String buffer
	 *  
	 * @param sb       the string buffer the element will be added to 
	 */
	private void createSituationElement(StringBuilder sb){
		sb.append(lineSeparator).append(INDENT[0]).append("<situation categoryName=\"ReportSituation\">");
		sb.append(lineSeparator).append(INDENT[1]).append(
		"<situationType xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"ReportSituation\" reasoningScope=\"INTERNAL\" reportCategory=\"LOG\"/>");
		sb.append(lineSeparator).append(INDENT[0]).append("</situation>");
	}

	/**
	 * Appends the CBE Source XML element of a record to a String buffer
	 *  
	 * @param sb       the string buffer the element will be added to 
	 * @param record   the record that represents the common base event
	 */
	private void createSourceElement(StringBuilder sb, RepositoryLogRecord record){
		String hostAddr = headerProps.getProperty(ServerInstanceLogRecordList.HEADER_HOSTADDRESS) == null? "": headerProps.getProperty(ServerInstanceLogRecordList.HEADER_HOSTADDRESS);
		String hostType = headerProps.getProperty(ServerInstanceLogRecordList.HEADER_HOSTTYPE) == null? "": headerProps.getProperty(ServerInstanceLogRecordList.HEADER_HOSTTYPE);

					// 660484 ... got rid of string concat since we already had the string buffer to do it better
		sb.append(lineSeparator).append(INDENT[0]).append("<sourceComponentId component=\"Logging\" componentIdType=\"Application\"");
		sb.append(" executionEnvironment=\"Java\" instanceId=\"").append(headerProps.getProperty(ServerInstanceLogRecordList.HEADER_SERVER_NAME)).append("\"");
		sb.append(" location=\"").append(hostAddr).append("\" locationType=\"").append(hostType).append("\"");
		sb.append(" processId=\"").append(headerProps.getProperty(ServerInstanceLogRecordList.HEADER_PROCESSID)).append("\"").append(" subComponent=\"Logger\"");
		sb.append(" threadId=\"").append(record.getExtension(RepositoryLogRecord.PTHREADID)).append("\"");
		sb.append(" componentType=\"Logging_Application\"/>");
	}

	/**
	 * Appends the CBE Message Element XML element of a record to a String buffer
	 *  
	 * @param sb       the string buffer the element will be added to 
	 * @param record   the record that represents the common base event
	 */
	private void createMessageElement(StringBuilder sb, RepositoryLogRecord record){	// 660484 elim string concat
		sb.append(lineSeparator).append(INDENT[0]).append("<msgDataElement msgLocale=\"").append(record.getMessageLocale()).append("\">");
		if (record.getParameters() != null) {
			// how many params do we have?
			for (int c = 0; c < record.getParameters().length; c++) {
				sb.append(lineSeparator).append(INDENT[1]).append("<msgCatalogTokens value=\"").
					append(MessageFormat.format("{" + c + "}", record.getParameters())).append("\"/>");
			}
		}
		// IBM SWG MsgID
		if (record.getMessageID() != null) {
			// Seems to be a IBM SWG ID.
			sb.append(lineSeparator).append(INDENT[1]).append("<msgId>").append(record.getMessageID()).append("</msgId>");
			// IBM SWG MsgType
			sb.append(lineSeparator).append(INDENT[1]).append("<msgIdType>");
			if (record.getMessageID().length() == 10)
				sb.append("IBM5.4.1");
			else
				sb.append("IBM4.4.1");
			sb.append("</msgIdType>");
		}

		if (record.getRawMessage() != null && record.getResourceBundleName() != null) {
			sb.append(lineSeparator).append(INDENT[1]).append("<msgCatalogId>").append(record.getRawMessage()).append("</msgCatalogId>");
			sb.append(lineSeparator).append(INDENT[1]).append("<msgCatalogType>Java</msgCatalogType>");
			sb.append(lineSeparator).append(INDENT[1]).append("<msgCatalog>").append(record.getResourceBundleName()).append("</msgCatalog>");
		}

		sb.append(lineSeparator).append(INDENT[0]).append("</msgDataElement>");
	}

	/**
	 * Appends the CBE Extended Data Element of a record to a String buffer
	 *  
	 * @param sb       the string buffer the element will be added to 
	 * @param record   the record that represents the common base event
	 */
	private void createExtendedElement(StringBuilder sb, RepositoryLogRecord record){
		sb.append(lineSeparator).append(INDENT[0]).append("<extendedDataElements name=\"CommonBaseEventLogRecord:level\" type=\"noValue\">");
		sb.append(lineSeparator).append(INDENT[1]).append("<children name=\"CommonBaseEventLogRecord:name\" type=\"string\">");
		sb.append(lineSeparator).append(INDENT[2]).append("<values>").append(record.getLevel().getName()).append("</values>");
		sb.append(lineSeparator).append(INDENT[1]).append("</children>");
		sb.append(lineSeparator).append(INDENT[1]).append("<children name=\"CommonBaseEventLogRecord:value\" type=\"int\">");
		sb.append(lineSeparator).append(INDENT[2]).append("<values>").append(record.getLevel().intValue()).append("</values>");
		sb.append(lineSeparator).append(INDENT[1]).append("</children>");
		sb.append(lineSeparator).append(INDENT[0]).append("</extendedDataElements>");    	
	}

	/**
	 * Appends the CBE Extended Data Element of a record's extension to a String buffer
	 *  
	 * @param sb             the string buffer the element will be added to 
	 * @param record         the record that represents the common base event
	 * @param extensionId    the extension ID field from the record to use
	 */
	private void createExtendedElement(StringBuilder sb, RepositoryLogRecord record, String extensionID){
		String edeValue = record.getExtension(extensionID);
		if (edeValue != null && !edeValue.isEmpty()){
			createExtendedElement(sb, extensionID, "string", edeValue);
		}
	}

	/**
	 * Prints and extendedDataElement for CBE output
	 * Formatter's time zone.
	 * 
	 * @param sb        the string buffer the element will be added to
	 * @param edeName   the name of the extendedDataElement.
	 * @param edeType   the data type for the extendedDataElement value(s).
	 * @param edeValues the values for this extendedDataElement.
	 */
	private void createExtendedElement(StringBuilder sb, String edeName, String edeType, String edeValues) {
		sb.append(lineSeparator).append(INDENT[0]).append("<extendedDataElements name=\"").append(edeName).append("\" type=\"").append(edeType).append("\">");
		sb.append(lineSeparator).append(INDENT[1]).append("<values>").append(edeValues).append("</values>");
		sb.append(lineSeparator).append(INDENT[0]).append("</extendedDataElements>");
	}    

	protected String appendUnusedParms(String message, Object[] args) {
		  String returnValue = message;

		  // parms were present but message format won't take them.  Append parms as desired.
		  StringBuilder buffer = new StringBuilder();
		  for (int i = 0; i < args.length; ++i) {
			  if (i == 0) buffer.append("; ");
		  	  else buffer.append(", ");
			  if (args[i] == null)								//D218128 start
				  buffer.append(nullParamString);
			  else
				  buffer.append(args[i].toString());			//D218128 end
		  }
		  returnValue = returnValue.concat(buffer.toString());
		  return returnValue;
	}

}
