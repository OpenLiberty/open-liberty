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

package com.ibm.ws390.sm.smfview;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * The default implementation of SMFFilter
 *
 */
public class DefaultFilter implements SMFFilter {

    private SmfPrintStream smf_printstream = null;
    /** record counter */
    public static int recordN = 0;

    /**
     * Calls commonInitialize to create smf printstream
     */
    @Override
    public boolean initialize(String parms) {
        boolean return_value = true;
        smf_printstream = commonInitialize(parms);
        if (smf_printstream == null)
            return_value = false;
        return return_value;
    }

    /**
     * Allow everything
     */
    @Override
    public boolean preParse(SmfRecord record) {
        ++recordN;
        boolean ok_to_process = true;

        return ok_to_process;
    }

    /**
     * Calls commonParse
     */
    @Override
    public SmfRecord parse(SmfRecord smfRecord) {
        return commonParse(smfRecord);
    }

    /**
     * Calls the dump method on the input SMFRecord
     */
    @Override
    public void processRecord(SmfRecord record) {
        commonProcessRecord(record, smf_printstream);
    }

    /**
     * Does nothing..no summary information for the default filter
     * because it just formats everything
     */
    @Override
    public void processingComplete() {

    }

    /**
     * Creates the SmfPrintstream used for all output
     * 
     * @param parms Usually text after the comma on the PLUGIN keyword
     * @return an SmfPrintstream. Either stdout or a file
     */
    public static SmfPrintStream commonInitialize(String parms) {
        SmfPrintStream local_smf_printstream = null;
        if ((parms.equalsIgnoreCase("STDOUT")) | (parms.length() == 0)) {
            try {
                local_smf_printstream = new SmfPrintStream(System.out, System.getProperty("PRINTENCODED", "YES"));
            } catch (Exception e) {
                System.out.println(" Exception during open of stdout, parm=" + parms);
                System.out.println(" Exception data:\n" + e.toString());
            }
        } else {
            try {
                PrintStream ps = new PrintStream(parms);
                local_smf_printstream = new SmfPrintStream(ps, System.getProperty("PRINTENCODED", "YES"));
            } catch (Exception e) {
                System.out.println(" Exception during open " + parms);
                System.out.println(" Exception data:\n" + e.toString());
            }
        }
        return local_smf_printstream;
    }

    /**
     * Determines the SMF type 120 record subtype and calls the appropriate
     * parse routine
     * 
     * @param smfRecord an SMF type 120 record
     * @return A new SmfRecord but of the appropriate subclass, now parsed
     */
    public static SmfRecord commonParse(SmfRecord smfRecord) {
        int type = smfRecord.type();
        int subtype = smfRecord.subtype();
        String formatterPackage = "com.ibm.ws390.smf.formatters";
        String newClassName = "SMFType" + type + "SubType" + subtype;
        SmfRecord newSmfRecord = null;
        // Take a shot at finding a class that implements the right SmfRecord
        // If it blows up in any way, go back to the classic code
        try {
            Class parameterTypes[] = new Class[1];
            parameterTypes[0] = Class.forName("com.ibm.ws390.sm.smfview.SmfRecord");
            Class newClass = Class.forName(formatterPackage + "." + newClassName);
            Constructor ctor = newClass.getConstructor(parameterTypes);
            Object parms[] = new Object[1];
            parms[0] = smfRecord;
            newSmfRecord = (SmfRecord) ctor.newInstance(parms);
        } catch (Throwable t) // just catch everything
        {
            try {
                InvocationTargetException ite = (InvocationTargetException) t;
                Throwable t2 = ite.getCause();
                // try to cast this to a skipped server
                SkipFilteredRecord e = (SkipFilteredRecord) t2;
                //  Hmm...that worked, so just skip this one
                return null;
            } catch (ClassCastException c) {
                // nope...muddle on then
            }
            // No formatter found..or we had problems with it  just format as raw data
            try {
                newSmfRecord = new RawSmfRecord(smfRecord);
            } catch (UnsupportedVersionException e) {
                System.out.println("******************");
                System.out.println(e.getMessage());
                System.out.println("******************");
            } // catch ... UnsupportedVersionException
            catch (UnsupportedEncodingException e) {
                System.out.println("******************");
                System.out.println(e.getMessage());
                System.out.println("******************");
            } // catch ... UnsupportedVersionException
        }
/*
 * if (newSmfRecord==null)
 * {
 * try
 * {
 * switch (subtype)
 * {
 * case SmfRecord.ServerActivitySmfRecordSubtype :
 * smfRecord = new ServerActivitySmfRecord(smfRecord);
 * break;
 * 
 * case SmfRecord.ServerIntervalSmfRecordSubtype :
 * smfRecord = new ServerIntervalSmfRecord(smfRecord);
 * break;
 * 
 * case SmfRecord.ContainerActivitySmfRecordSubtype :
 * smfRecord = new ContainerActivitySmfRecord(smfRecord);
 * break;
 * 
 * case SmfRecord.ContainerIntervalSmfRecordSubtype :
 * smfRecord = new ContainerIntervalSmfRecord(smfRecord);
 * break;
 * 
 * case SmfRecord.J2eeContainerActivitySmfRecordSubtype :
 * smfRecord = new J2eeContainerActivitySmfRecord(smfRecord);
 * break;
 * 
 * case SmfRecord.J2eeContainerIntervalSmfRecordSubtype :
 * smfRecord = new J2eeContainerIntervalSmfRecord(smfRecord);
 * break;
 * 
 * case SmfRecord.WebContainerActivitySmfRecordSubtype :
 * smfRecord = new WebContainerActivitySmfRecord(smfRecord);
 * break;
 * 
 * case SmfRecord.WebContainerIntervalSmfRecordSubtype :
 * smfRecord = new WebContainerIntervalSmfRecord(smfRecord);
 * break;
 * 
 * case SmfRecord.RequestActivitySmfRecordSubtype :
 * smfRecord = new RequestActivitySmfRecord(smfRecord);
 * break;
 * 
 * default :
 * smfRecord = new RawSmfRecord(smfRecord);
 * break;
 * 
 * } // switch ... record subtype
 * }
 * catch (UnsupportedVersionException e)
 * {
 * System.out.println("******************");
 * System.out.println(e.getMessage());
 * System.out.println("******************");
 * } // catch ... UnsupportedVersionException
 * catch (UnsupportedEncodingException e)
 * {
 * System.out.println("******************");
 * System.out.println(e.getMessage());
 * System.out.println("******************");
 * } // catch ... UnsupportedVersionException
 * }
 */
//	 else
//	 {
//      smfRecord = newSmfRecord;
//	 }
        return newSmfRecord;
    }

    /**
     * Increment record count (in case you pre-parsed it yourself)
     * 
     * @return new record count
     */
    public static int incrementRecordCount() {
        ++recordN;
        return recordN;
    }

    /**
     * Calls the dump method on the input SMFRecord
     * 
     * @param record          The record to process
     * @param smf_printstream is the printstream to send output to
     */
    public static void commonProcessRecord(SmfRecord record, SmfPrintStream smf_printstream) {
        smf_printstream.println("");
        smf_printstream.println(
                                "--------------------------------------------------------------------------------");
        smf_printstream.printlnKeyValue("Record#", recordN);

        record.dump(smf_printstream);
    }

}
