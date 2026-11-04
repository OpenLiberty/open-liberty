/*******************************************************************************
 * Copyright (c) 2001 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

// ----------------------------------------------
//   SMF Record Layouts
// ----------------------------------------------
//   Server Activity Record:
//
//   1x RecordHeader
//   1x TripletNumberSection
//   1x Triplet (ProductSection)
//   1x Triplet (ServerActivitySection)
//   1x Triplet (CommunicationSessionSection)
//   1x ProductSection
//   1x ServerActivitySection
//   nx CommunicationSessionSection
//
//   Container Activity Record:
//
//   1x RecordHeader
//   1x TripletNumberSection
//   1x Triplet (ProductSection)
//   1x Triplet (ContainerActivitySection)
//   nx Triplet (ClassSection)
//   1x ProductSection
//   1x ContainerActivitySection
//   nx ClassSection
//          1x TripletNumberSection
//          mx Triplet (MethodSection)
//          mx MethodSection
//
//   Server Interval Record:
//
//   1x RecordHeader
//   1x TripletNumberSection
//   1x Triplet (ProductSection)
//   1x Triplet (ServerIntervalSection)
//   1x ProductSection
//   1x ServerIntervalSection
//
//   Container Interval Record:
//
//   1x RecordHeader
//   1x TripletNumberSection
//   1x Triplet (ProductSection)
//   1x Triplet (ContainerIntervalSection)
//   nx Triplet (ClassSection)
//   1x ProductSection
//   1x ContainerIntervalSection
//   nx ClassSection
//          1x TripletNumberSection
//          mx Triplet (MethodSection)
//          mx MethodSection

package com.ibm.ws390.sm.smfview;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * 
 * Driver class to run through the input file
 *
 */
public class Interpreter {

	/**
	  * The main method
	  * @param args The standard input array of strings.  May contain two strings consisting
	  * of INFILE(input file name) and PLUGIN(class,parms).
	  * @deprecated Use the main in the SMF class instead
	  */
	 public static void main(String[] args) 
	 { 
	  System.out.println("The invocation method for the SMF Browser has changed."); 
	  System.out.println("The new invocation syntax is:");
	  System.out.println("java com.ibm.ws390.sm.smfview.SMF INFILE(SMF Dataset Name)");
	  System.out.println("Please see the documentation included in the Documentation directory of this .jar file for details and options");
	 }

	//----------------------------------------------------------------------------
	/** Create a SmfRecord from a stream buffer.
	 * The SmfRecord is specialized as indicated by the type.
	 * createSmfRecord acts as a factory method for SmfRecord.
	 * @param aStream SmfStream to create the SmfRecord from.
	 * @return A SmfRecord parsed from the stream.
	 * @throws UnsupportedVersionException Exception thrown when the requested version is higher than the supported version.
	 * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is encountered during Smf stream parse.
	*/
	private static SmfRecord createSmfRecord(SmfStream aStream, SMFFilter a_filter)
		throws UnsupportedVersionException, UnsupportedEncodingException 
	{
   	 SmfRecord smfRecord = new SmfRecord(aStream);

	 /* check the validity of the record header flag */
	 if (a_filter.preParse(smfRecord))
	 {	
	  smfRecord = a_filter.parse(smfRecord);
	 } 
	 else
		smfRecord=null;

	 return smfRecord;

	} // Interpreter.createSmfRecord()

	//----------------------------------------------------------------------------
	/** Main method of the interpreter
	 * It reads and interprets SmfRecords from the SmfStream one by one
	 * and dumps each record into a SmfPrintStream. 
	 * @param infile input file to parse
	 * @param a_filter input filter to use
	 */
	public static void interpret(ISmfFile infile, SMFFilter a_filter) {

		System.out.println("SMF file analysis starts ...");

		while (true) {
			try 
			{
			 // read record
			 byte recordData[] = infile.read();

			 if (recordData == null)
				break;

			 SmfRecord record = null;

			 SmfStream recordStream = new SmfStream(recordData);

  		     record = createSmfRecord(recordStream,a_filter);
			 if (record!=null) {
				   record.rawRecord(recordData);
	               a_filter.processRecord(record);
				 } 
			 
			} // try

			catch (UnsupportedVersionException e) {

				System.out.println("******************");
				System.out.println(e.getMessage());
				System.out.println("******************");

			} // catch ... UnsupportedVersionException

			catch (IOException e) {
				System.out.println(" IOException during read:");
				System.out.println(" Exception data:\n" + e.toString());
			} // catch ... IOException

			catch (Throwable e) {
				System.out.println(
					"Exception during interpretation: " + e.getMessage());
				e.printStackTrace();
			} // catch ... Throwable

		} // while (true) ... scan file for records

	try
	{
	 infile.close();
	}  
	catch (IOException e) 
	{
	 System.out.println(" IOException during close:");
	 System.out.println(" Exception data:\n" + e.toString());
	}

	a_filter.processingComplete();
	System.out.println("");
	System.out.println("SMF file analysis ended.");
	        
 } // Interpreter.interpret()

} // Interpreter