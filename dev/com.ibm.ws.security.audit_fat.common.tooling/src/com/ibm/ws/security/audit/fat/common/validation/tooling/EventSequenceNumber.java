/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.audit.fat.common.validation.tooling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/*
 * This class may be built separated via javac EventSequnceNumber and then
 * run via java EventSequenceNumber <directory path to audit logs>
 * This class merely serves as a easy tool to validate that the sequence numbers
 * in all of the audit logs in a particular directory are sequentially correct.
 * This is especially important when running in stress environments with 
 * multiple threads, where a manual check would be quite difficult.
 * 
 */

public class EventSequenceNumber {

    public static void main(String[] args) {

        String dirName = null;
        String fileName = null;
        List <File> listOfAuditLogs = null;
        String line = null;
        String seq = null;
        int seqNum = 0;
        TreeMap <Date, String> tm = new TreeMap <Date,String>();

        dirName = args[0];
        System.out.println();
        System.out.println();
        System.out.println("Processing the audit logs from dir: " + dirName);
        System.out.println();

        // This will reference one line at a time

        try {

            listOfAuditLogs = returnListOfAuditLogs(dirName);

            // now order them
            Iterator<File> logsIterator = listOfAuditLogs.iterator();
            while (logsIterator.hasNext()) {
                fileName = logsIterator.next().getAbsolutePath();
                if (fileName.contains("_")) {   
                    int beginTimeStamp = fileName.indexOf("_") + 1;
                    int endTimeStamp = fileName.indexOf(".log") - 1;
                    String timeStamp = fileName.substring(beginTimeStamp, endTimeStamp - 1);

                    // now we have a format of YY.MM.DD_HR.MIN.SEC.MS

                    String date = timeStamp.substring(0, timeStamp.indexOf("_"));
                    String time = timeStamp.substring(timeStamp.indexOf("_") + 1);

                    int year = (new Integer(date.substring(0, date.indexOf("."))).intValue());
                    date = date.substring(date.indexOf(".") + 1);
                    int month = (new Integer(date.substring(0, date.indexOf("."))).intValue());
                    date = date.substring(date.indexOf(".") + 1);
                    int day = (new Integer(date).intValue());

                    int hour = (new Integer(time.substring(0, time.indexOf("."))).intValue());
                    time = time.substring(time.indexOf(".") + 1);
                    int minutes = (new Integer(time.substring(0, time.indexOf("."))).intValue());
                    time = time.substring(time.indexOf(".") + 1);
                    int seconds = (new Integer(time).intValue());

                    @SuppressWarnings("deprecation")
					Date newDate = new Date(year, month, day, hour, minutes, seconds);

                    tm.put(newDate, fileName);

                } 

            }

            // now process each, ending with the newest, audit.log

            // and save the last sequence number which will become the first sequence number in the next log


            for (Map.Entry<Date, String> entry : tm.entrySet()) {
                    fileName = entry.getValue();
                    System.out.println("   Processing audit log: " +  fileName);
                    FileReader fileReader = 
                        new FileReader(fileName);

                    boolean found = false;
                    // Always wrap FileReader in BufferedReader.
                    BufferedReader bufferedReader = 
                        new BufferedReader(fileReader);

                    while((line = bufferedReader.readLine()) != null) {
                        if (line.contains("eventSequenceNumber")) {
                             seq = line.substring(line.indexOf(":") + 2);
                             seq = seq.substring(0, seq.indexOf(","));
                             Integer sn = new Integer(seq);
                             if (sn.intValue() != seqNum) {
                                 found = true;
                                 System.out.println("ERROR!!!!  EventSequenceNumber: " + sn.intValue() + " does NOT equal expected sequence number of: " + seqNum);
                                 System.out.println();
                             }
                             seqNum++;
                        }
                    } 
                    if (!found) {
                        System.out.println(" ..... all sequence numbers are in order");
                        System.out.println();
                    }

                    // Always close files.
                    bufferedReader.close();         
	    }            // FileReader reads text files in the default encoding.

            fileName = dirName.concat("audit.log");
            
            System.out.println("   Processing audit log: " +  fileName);
            
            FileReader fileReader = 
                new FileReader(fileName);

            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = 
                new BufferedReader(fileReader);

            boolean found = false;
            while((line = bufferedReader.readLine()) != null) {
                if (line.contains("eventSequenceNumber")) {
                     seq = line.substring(line.indexOf(":") + 2);
                     seq = seq.substring(0, seq.indexOf(","));
                     Integer sn = new Integer(seq);
                     if (sn.intValue() != seqNum) {
                         found = true;
                         System.out.println("ERROR!!!!  EventSequenceNumber: " + sn.intValue() + " does NOT equal expected sequence number of: " + seqNum);
                         System.out.println();
                     }
                     seqNum++;
                }
            } 
            if (!found) {
                System.out.println(" ..... all sequence numbers are in order");
                System.out.println();
            }

            // Always close files.
            bufferedReader.close();  


        }
        catch(FileNotFoundException ex) {
            System.out.println(
                "Unable to open file '" + 
                fileName + "'");                
        }
        catch(IOException ex) {
            System.out.println(
                "Error reading file '" 
                + fileName + "'");                  
            // Or we could just do this: 
            // ex.printStackTrace();
        }
    }


    public static List<File> returnListOfAuditLogs(String dirName) throws IOException {
    	List<File> list = new ArrayList<File>();
        java.nio.file.Path dir = java.nio.file.Paths.get(dirName);
        try {
           java.nio.file.DirectoryStream<java.nio.file.Path> dirStream = java.nio.file.Files.newDirectoryStream(dir);
           for (java.nio.file.Path entry: dirStream) {
               if (entry.getFileName().toString().startsWith("audit")) {
                   list.add(entry.toFile());
               }
           }
           dirStream.close();
        } catch (java.nio.file.DirectoryIteratorException ex) {
                   // I/O error encountered during the iteration, the cause is an IOException
                   throw ex.getCause();
        }
        return list;

    }
    

    
}

