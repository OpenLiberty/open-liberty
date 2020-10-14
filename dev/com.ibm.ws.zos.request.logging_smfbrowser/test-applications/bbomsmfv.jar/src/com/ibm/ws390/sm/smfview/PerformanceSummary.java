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
package com.ibm.ws390.sm.smfview;

import java.io.FileDescriptor;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Specialized Summary File Writer methods.
 * Creation date: (10/6/2001 10:36:43 AM) @author: Hutch
 * Update for WAS V5 (04/07/2002) Hutch
 */
public class PerformanceSummary implements SMFFilter {

    /** Target writer file. */
    public static FileWriter fout = null;
    private SmfPrintStream smf_printstream = null;

    /** Switch to write summary report file */
    public static boolean summaryReportSwitch;

    /** page number */
    public static int pageNumber = 0; //@SV@
    /** line number */
    public static int lineNumber = 1; //@SU9

    /** total elapsed time */
    public static long TotalElapsedTime = 0; //@SU99
    /** total elapsed micro seconds */
    public static long TotalElapsedUsec = 0; //@SU99
    /** total cpu time */
    public static long TotalCPUTime = 0; //@SU99
    /** total time on zAAP */
    public static long TotalZaapTime = 0; //@SU99
    /** total time on zIIP */
    public static long TotalZiipTime = 0; //@SU99
    /** total bytes received */
    public static long TotalBytesRecvd = 0; //@SU99
    /** total bytes sent */
    public static long TotalBytesSent = 0; //@SU99
    /** total transaction count */
    public static int TotalTranCount = 0; //@SU99

    /** average elapsed time */
    public static long AveElapsedTime = 0; //@SU99
    /** averaged elapsed micro seconds */
    public static long AveElapsedUsec = 0; //@SU99
    /** average cpu time */
    public static long AveCPUTime = 0; //@SU99
    /** average zAAP time */
    public static long AveZaapTime = 0; //@SU99
    /** average zIIP time */
    public static long AveZiipTime = 0; //@SU99
    /** average bytes received */
    public static long AveBytesRecvd = 0; //@SU99
    /** average bytes sent */
    public static long AveBytesSent = 0; //@SU99

    /** String buffer for print line */
    private static StringBuffer lineBuf = new StringBuffer(100); //@SV
// Key to 120 record SubTypes:
    private static final String hdr0a = "\n - Record subtypes: 1:Svr_Act. 3:Svr_Int. "; //@SWa
    private static final String hdr0b = "5:EJB_Act. 6:EJB_Int. 7:Web_Act. 8:Web_Int. 9:Request\n"; //@SWa
//Key to 120.9 record Sections:
    private static final String hdr00a = "   - subtype 9 Sections: CPU:CPU, N:Network, "; //@SW9
    private static final String hdr00b = "Cl: Classification, S:Security, T:Timestamps, U:UserData\n"; //@SW9
// Column Heading Strings . . .
    private static final String hdr1a = "\nSMF -Record Time     Server   Bean/WebAppName "; //@SWa
    private static final String hdr2a = "Numbr -Type hh:mm:ss Instance  Method/Servlet "; //@SWa
    private static final String hdrxa = "1---+----1----+----2----+----3----+----4----+-"; //@SWa

    private static final String hdr1b = " Bytes   Bytes  # of  El.Time  CPU_Time(uSec)     Other SMF 120.9\n"; //@Su99
    private static final String hdr2b = " toSvr   frSvr Calls   (msec)   Tot-CPU      zAAP Sections Present\n"; //@SU99
    private static final String hdrxb = "---5----+----6----+----7----+----8----+----9----+ ----------------\n"; //@SU99

    private static final String hdr0 = hdr0a + hdr0b; //@SWa
    private static final String hdr00 = hdr00a + hdr00b; //@SU9
    private static final String hdr1 = hdr1a + hdr1b;
    private static final String hdr2 = hdr2a + hdr2b;
    private static final String hdrx = hdrxa + hdrxb;

    private static final String b10 = "          "; //Ten blanks
    private static final String blnks100 = b10 + b10 + b10 + b10 + b10
                                           + b10 + b10 + b10 + b10 + b10; //100 blanks

    /** Array of blanks for padding columns of numbers converted to strings */
    private static final String[] padBlanks = { "", " ", "  ", "   ", "    ", "     ", "      ", "       ", "        ", "         ", "          " };

    private long startTimeMills = 0;
    private long endTimeMills = 0;
    private long runTimeMills = 0;

    /**
     * PerformanceSummary Constructor (no parms).
     */
    public PerformanceSummary() {
    }

    @Override
    public boolean initialize(String parms) {
        String out_filename;
        try {
            summaryReportSwitch = true;
            startTimeMills = System.currentTimeMillis();
            smf_printstream = new SmfPrintStream(); // set up dummy print stream
            if ((parms.equalsIgnoreCase("STDOUT")) | (parms.length() == 0)) {
                PerformanceSummary.openFile("STDOUT");
            } else {
                out_filename = parms;
                PerformanceSummary.openFile(out_filename);
            }
        } catch (Throwable e) {
            System.out.println("Exception opening summary file: " + e.getMessage());
            e.printStackTrace(); //@SUa
        }
        boolean return_value = true;
        if (fout == null)
            return_value = false;
        return return_value;
    }

    @Override
    public boolean preParse(SmfRecord record) {
        boolean ok_to_process = false;
        if (record.type() == SmfRecord.SmfRecordType)
            if ((record.subtype() > SmfRecord.UnknownSmfRecordSubtype) &
                (record.subtype() <= SmfRecord.RequestActivitySmfRecordSubtype))
                ok_to_process = true;

        return ok_to_process;

    }

    @Override
    public SmfRecord parse(SmfRecord record) {
        return DefaultFilter.commonParse(record);
    }

    @Override
    public void processRecord(SmfRecord record) {
        record.dump(smf_printstream); //pass dummy printstream	
    }

    @Override
    public void processingComplete() {

        // Write last print line & Clear Buffer before final header               //@SU99
        PerformanceSummary.writeNewLine(); //@SU99

        //  Write Final (Short) Header:                                             @SU99        
        PerformanceSummary.writeHdrShort(); //@SU99

        // Calculate & Write Average Times & Bytes ...                           //@SU99
        if (PerformanceSummary.TotalTranCount > 0) {
            PerformanceSummary.AveElapsedTime = PerformanceSummary.TotalElapsedTime / PerformanceSummary.TotalTranCount; //@SU99
            PerformanceSummary.AveElapsedUsec = PerformanceSummary.TotalElapsedUsec / PerformanceSummary.TotalTranCount; //@SU99
            PerformanceSummary.AveCPUTime = PerformanceSummary.TotalCPUTime / PerformanceSummary.TotalTranCount; //@SU99
            PerformanceSummary.AveZaapTime = PerformanceSummary.TotalZaapTime / PerformanceSummary.TotalTranCount; //@SU99
            PerformanceSummary.AveBytesRecvd = PerformanceSummary.TotalBytesRecvd / PerformanceSummary.TotalTranCount; //@SU99
            PerformanceSummary.AveBytesSent = PerformanceSummary.TotalBytesSent / PerformanceSummary.TotalTranCount; //@SU99
        }

        PerformanceSummary.writeString("\nREQUEST Recs: Avg Bytes, TranCount & Times =", 46); //@SU99
        PerformanceSummary.writeLong(PerformanceSummary.AveBytesRecvd, 7); //@SU99
        PerformanceSummary.writeLong(PerformanceSummary.AveBytesSent, 8); //@SU99
        PerformanceSummary.writeInt(PerformanceSummary.TotalTranCount, 6); //@SU99
        PerformanceSummary.writeLong(PerformanceSummary.AveElapsedTime, 9); //@SU99
        PerformanceSummary.writeLong(PerformanceSummary.AveCPUTime, 10); //@SU99
        PerformanceSummary.writeLong(PerformanceSummary.AveZaapTime, 10); //@SU99

        // Write Trailer, Flush, and Close Summary file
        writeNewLine();
        endTimeMills = System.currentTimeMillis(); // get end time
        runTimeMills = endTimeMills - startTimeMills; //

        long runTimeSecs = runTimeMills / 1000L;

        writeNewLine(); /* Flush last buffer */
        writeString("\n===SMF=120=V800===== End of Report ======", 43); //@SU9
        writeString("====== End of Report =SMF=V800=JMH= 15July,2011", 49); //@SU9
        writeString("\nRun Time:", 11);
        writeLong(runTimeSecs, 6);
        writeString(" seconds    =", 13);
        writeLong(runTimeMills, 9);
        writeString(" milliseconds", 14);
        writeNewLine(); //@SVa
        closeFile(); // Flushes and Closes            @SUa

    }

    //--------------------------------------------------------------------------
    /**
     * Clear the StringBuffer (no parms)
     */
    public static void clearBuf() { //@SVa 
        if (summaryReportSwitch == true) { //@SVa 

            lineBuf.setLength(0); //@SVa 

        } // end if summaryReportSwitch                                     //@SVa 
    } // clearBuf                                                          //@SVa 

    //--------------------------------------------------------------------------
    /**
     * Close the output file (no parms) (Called from Interperter. )
     */
    public static void closeFile() {
        if (summaryReportSwitch == true) {
            try {
                fout.flush();
                fout.close();
            } // try

            catch (IOException e) {
                System.err.println(e);
            }
        } // end if summaryReportSwitch
    } // closeFile

    //--------------------------------------------------------------------------
    /**
     * Initialize Summary file & write Header line. (Called from Interpreter.)
     * 
     * @param outFile The filename for the summary's output file
     */
    public static void openFile(String outFile) {
        if (summaryReportSwitch == true) {
            try {
                if (outFile.equals("STDOUT"))
                    fout = new FileWriter(FileDescriptor.out);
                else
                    fout = new FileWriter(outFile);
                fout.write("SMF 120 Performance Summary V800 "); //@SU9
            } // try
            catch (IOException e) {
                System.err.println(e);
            }
        } // end if summaryReportSwitch
    } // PerformanceSummary.setFileName

    //--------------------------------------------------------------------------
    /**
     * Write Column headings. (Called from SmfRecord.)
     */
    public static void writeHdr() {
        if (summaryReportSwitch == true) {
            try {
                fout.write(hdr0);
                fout.write(hdr00);
                fout.write(hdr1);
                fout.write(hdr2);
                fout.write(hdrx);
            } // try
            catch (IOException e) {
                System.err.println(e);
            }
            ++PerformanceSummary.pageNumber; //Increment Page # //@SU9
            PerformanceSummary.lineNumber = PerformanceSummary.lineNumber + 8;//Increment Line # //@SU9
        } // end if summaryReportSwitch
    } // PerformanceSummary.writeHdr

//--------------------------------------------------------------------------
    /**
     * Write Column headings.
     */
    public static void writeHdrShort() {
        if (summaryReportSwitch == true) {
            try {
                fout.write(hdr1);
                fout.write(hdr2);
                fout.write(hdrx);
            } // try
            catch (IOException e) {
                System.err.println(e);
            }
            ++PerformanceSummary.pageNumber; //Increment Page # //@SU9
            lineNumber = lineNumber + 5; //Increment Line # //@SU9
        } // end if summaryReportSwitch
    } // PerformanceSummary.writeHdr

    //--------------------------------------------------------------------------
    /**
     * Write an Integer as a string c.
     * 
     * @param aNumber  Integer number to be written
     * @param colWidth Number of digits to limit the number to
     */
    public static void writeInt(int aNumber, int colWidth) {
        if (summaryReportSwitch == true) {

            String strNumber = Integer.toString(aNumber);

            if (colWidth > strNumber.length()) {
                strNumber = padBlanks[colWidth - strNumber.length()] + strNumber;
            }
            if (colWidth < strNumber.length()) {
                strNumber = strNumber.substring(0, colWidth);
            }

            lineBuf.append(strNumber); //@SV

        } // end if summaryReportSwitch
    } // PerformanceSummary.writeInt

    //--------------------------------------------------------------------------
    /**
     * Write a Long as a string c.
     * 
     * @param lNumber  Long number to be written
     * @param colWidth Number of digits to limit the number to
     */
    public static void writeLong(long lNumber, int colWidth) { //@SW
        if (summaryReportSwitch == true) {

            String stlNumber = Long.toString(lNumber);

            if (colWidth > stlNumber.length()) {
                stlNumber = padBlanks[colWidth - stlNumber.length()] + stlNumber;
            }
            if (colWidth < stlNumber.length()) {
                stlNumber = stlNumber.substring(0, colWidth);
            }

            lineBuf.append(stlNumber); //@SV

        } // end if summaryReportSwitch
    } // PerformanceSummary.writeLong

    //--------------------------------------------------------------------------
    /**
     * Write lineBuf to file & clear lineBuf ... (no parms) //@SVa
     */
    public static void writeNewLine() { //@SVa
        if (summaryReportSwitch == true) { //@SVa
            if (lineBuf.length() > 0) { //@SVa 

                lineBuf.append("\n"); // Add new line char.//@SVa

                try {
                    fout.write(lineBuf.toString());
                } // Write line out.   //@SVa
                catch (IOException e) {
                    System.err.println(e);
                } //@SVa  

                lineBuf.setLength(0); // Clear it out.     //@SVa

                ++PerformanceSummary.lineNumber; // Increment Line #  //@SU9
            } // end if capacity > 0
        } // end if summaryReportSwitch
    } // PerformanceSummary.writeNewLine

    //--------------------------------------------------------------------------
    /**
     * Write a String to summary file (w/ spec'd # of chars). //@SVa
     * 
     * @param sName String name
     * @param aCols Number of chars from string to write (max of 100)
     */
    public static void writeString(String sName, int aCols) {
        if (summaryReportSwitch == true) {
            String paddedString = sName + blnks100;
            paddedString = paddedString.substring(0, aCols);

            lineBuf.append(paddedString); //@SVa

        } // end if summaryReportSwitch
    } // PerformanceSummary.writeString

    //--------------------------------------------------------------------------
    /**
     * Writes the Time in hh:mm:ss format (Called from SmfRecord)
     * 
     * @param hr  Hours value to be written
     * @param min Minutes value to be written
     * @param sec Seconds value to be written
     */
    public static void writeTime(int hr, int min, int sec) {
        if (summaryReportSwitch == true) {
            String s_hrs = Integer.toString(hr);
            String s_min = Integer.toString(min);
            String s_sec = Integer.toString(sec);

            // Pad numbers to two digits (cols)
            if (s_hrs.length() == 1) {
                s_hrs = " " + s_hrs;
            }
            if (s_min.length() == 1) {
                s_min = "0" + s_min;
            }
            if (sec < 10) {
                s_sec = "0" + s_sec;
            }

            String hhmmssTime = s_hrs + ":" + s_min + ":" + s_sec;

            lineBuf.append(hhmmssTime); //@SV

        } // end if summaryReportSwitch
    } // PerformanceSummary.writeTime

} // PerformanceSummary class
