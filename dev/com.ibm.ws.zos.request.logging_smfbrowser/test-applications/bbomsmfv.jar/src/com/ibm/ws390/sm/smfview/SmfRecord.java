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

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

//------------------------------------------------------------------------------
/**
 * Data container for SMF data related to a SmfRecord.
 * This is the base class for all Smf record classes.
 * It constructs itself from an SmfStream.
 * After the basic record related stuff is read
 * the specific Smf record subtype is known and
 * a new subtype specific record must be created.
 * For that reason subtype specific constructors derived from SmfRecord
 * need to provide a copy constructor to catch the data from the initial
 * SmfRecord instance, typically by calling che copy constructor
 * of SmfRecord. The subtype specific copy constructor then continues
 * to setup itself by reading subtype specific data from the record.
 * This is most often achieved by constructing contained entities
 * which are able to construct theirselves from a SmfStream.
 */
public class SmfRecord extends SmfEntity {

    /** Supported version of this implementation. */
    public final static int s_supportedVersion = 1;

    /** Smf record type for WebSphere for z/OS. */
    public final static int SmfRecordType = 120;

    /** Unknown Smf record type enum value. */
    public final static int UnknownSmfRecordSubtype = 0;

    /** Server activity Smf record type enum value. */
    public final static int ServerActivitySmfRecordSubtype = 1;

    /** Container activity Smf record type enum value. */
    public final static int ContainerActivitySmfRecordSubtype = 2;

    /** Server interval Smf record type enum value. */
    public final static int ServerIntervalSmfRecordSubtype = 3;

    /** Container interval Smf record type enum value. */
    public final static int ContainerIntervalSmfRecordSubtype = 4;

    /** J2ee container activity Smf record type enum value. */
    public final static int J2eeContainerActivitySmfRecordSubtype = 5;

    /** J2ee container interval Smf record type enum value. */
    public final static int J2eeContainerIntervalSmfRecordSubtype = 6;

    /** Web container activity Smf record type enum value. */
    public final static int WebContainerActivitySmfRecordSubtype = 7;

    /** Web container interval Smf record type enum value. */
    public final static int WebContainerIntervalSmfRecordSubtype = 8;

    /** Request Activity Smf record type enum value. */
    public final static int RequestActivitySmfRecordSubtype = 9;

    /** Outbound Request Smf record type enum value. */
    public final static int OutboundRequestSmfRecordSubtype = 10;

    /** Liberty Request SMF record */
    public final static int LibertyRequestActivitySmfRecordSubtype = 11;

    /** Liberty Batch SMF record */
    public final static int LibertyBatchSmfRecordSubtype = 12;

    /** Bit mask for bit indicating subtype validity in SmfRecord. */
    public final static byte SubTypeValidFlag = (byte) 0x40;

    /** SmfStream where the SmfRecord constructs from. */
    protected SmfStream m_stream = null;

    /** Raw byte array of record */
    private byte[] raw_record;

    /** Record number. */
    public static int my_recordN = 0; //@SUa
    /** Set this to your page length */
    public static int my_pageLength = 64;

    /** Flag word. */
    public int m_flag;

    /** Record type. */
    public int m_type;

    /** Date of the record. */
    public Date m_date;

    /** System id. */
    public String m_sid;

    /** Subsystem id. */
    public String m_subsysid;

    /** Record subtype. */
    public int m_subtype = UnknownSmfRecordSubtype;

    /** hours for PerformanceSummary. */ //@SUa
    public static int my_hours; //@SUa
    /** Minutes for PerformanceSummary */
    public static int my_mins; //@SUa
    /** Seconds for PerformanceSummary */
    public static int my_secs; //@SUa
    //----------------------------------------------------------------------------

    /**
     * SmfRecord constructor from a SmfStream.
     * The instance is filled from the provided SmfStream.
     * 
     * @param aSmfStream Smf stream to create this instance of SmfRecord.
     * @throws UnsupportedVersionException  Exception thrown when the requested version is higher than the supported version.
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is encountered during Smf stream parse.
     */
    public SmfRecord(SmfStream aSmfStream) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(s_supportedVersion); // preliminary record version

        m_stream = aSmfStream;

        m_flag = m_stream.read();

        m_type = m_stream.read();

        ++my_recordN; /* Increment my_recordN(umber) */ //@SUa

        /* get the time field and calculate the hours, mins, secs */
        int time = m_stream.getInteger(4); //    @L1C

        int secs = time / 100;
        int mins = secs / 60;
        secs -= (mins * 60);
        int hours = mins / 60;
        mins -= (hours * 60);

        /* get the date */
        int century = (m_stream.read() == 1) ? 2000 : 1900;

        int year = m_stream.read();
        int y2 = year >> 4;
        int y1 = year - y2 * 16;
        year = century + y2 * 10 + y1;

        int byte2 = m_stream.read();
        int byte1 = m_stream.read();
        int d1 = byte1 >> 4;
        int d3 = byte2 >> 4;
        int d2 = byte2 - d3 * 16;
        int day = d3 * 100 + d2 * 10 + d1;

        Calendar calendar = new GregorianCalendar(year, 1, 1, hours, mins, secs);
        calendar.set(Calendar.DAY_OF_YEAR, day);

        m_date = calendar.getTime();

        /* get the sid */
        m_sid = m_stream.getString(4, SmfUtil.EBCDIC);

        if (m_stream.available() >= 4) {
            m_subsysid = m_stream.getString(4, SmfUtil.EBCDIC);
        }

        if (m_stream.available() >= 2) {
            m_subtype = m_stream.getInteger(2); //    @L1C     
        }

        my_hours = hours; //@SUa
        my_mins = mins; //@SUa
        my_secs = secs; //@SUa 

    } // SmfRecord(...)

    //----------------------------------------------------------------------------
    /**
     * Constructs a SmfRecord from a SmfRecord (Copy Constructor).
     * The instance is intialized from the provided SmfRecord
     * and typically continues to build from the contained SmfStream.
     * 
     * @param aSmfRecord SmfRecord to construct from.
     * @throws UnsupportedVersionException Exception thrown when the requested version is higher than the supported version.
     */
    protected SmfRecord(SmfRecord aSmfRecord) throws UnsupportedVersionException {

        super(s_supportedVersion); // preliminary record version

        m_stream = aSmfRecord.m_stream;

        m_flag = aSmfRecord.m_flag;
        m_type = aSmfRecord.m_type;

        m_date = aSmfRecord.m_date;

        m_sid = aSmfRecord.m_sid;

        m_subsysid = aSmfRecord.m_subsysid;
        m_subtype = aSmfRecord.m_subtype;

    } // SmfRecord(...)

    /**
     * Set raw record into object
     * 
     * @param b raw SMF record data
     */
    public void rawRecord(byte[] b) {
        raw_record = b;
    }

    /**
     * Get raw record data
     * 
     * @return raw record data
     */
    public byte[] rawRecord() {
        return raw_record;
    }

    //----------------------------------------------------------------------------
    /**
     * Returns the supported version of this class.
     * 
     * @return supported version of this class.
     */
    @Override
    public int supportedVersion() {

        return s_supportedVersion;

    } // supportedVersion()

    //----------------------------------------------------------------------------
    /**
     * Returns the flag word.
     * 
     * @return Flag word.
     */
    public int flag() {
        return m_flag;
    } // flag()

    //----------------------------------------------------------------------------
    /**
     * Returns the record type.
     * 
     * @return Record type.
     */
    public int type() {
        return m_type;
    } // type()

    //----------------------------------------------------------------------------
    /**
     * Returns the date when the record was moved into the SMF buffer
     * 
     * @return Date when the record was moved into the SMF buffer
     */
    public Date date() {
        return m_date;
    } // date()

    //----------------------------------------------------------------------------
    /**
     * Returns the system id as a String
     * 
     * @return System id as a String
     */
    public String sid() {
        return m_sid;
    } // sid()

    //----------------------------------------------------------------------------
    /**
     * Returns the subsystem id as a String.
     * 
     * @return subsystem id as a String.
     */
    public int subtype() {
        return m_subtype;
    } // subType()

    //----------------------------------------------------------------------------
    /**
     * Dump the Smf record into a print stream.
     * 
     * @param aPrintStream print stream to dump to.
     */
    public void dump(SmfPrintStream aPrintStream) {

        dumpProlog(aPrintStream);

        dumpEpilog(aPrintStream);

    } // dump(...)

    //----------------------------------------------------------------------------
    /**
     * Dump the Smf record epilog into a print stream.
     * 
     * @param aPrintStream print stream to dump to.
     */
    public void dumpEpilog(SmfPrintStream aPrintStream) {

    } // dumpEpilog(...)

    //----------------------------------------------------------------------------
    /**
     * Dump the Smf record prolog into a print stream.
     * 
     * @param aPrintStream print stream to dump to.
     */
    public void dumpProlog(SmfPrintStream aPrintStream) {

        String subsysid;
        if (m_subsysid == null || m_subsysid.length() == 0) {
            subsysid = "null";
        } else {
            subsysid = m_subsysid;
        }

        int recordS = m_stream.size() + 4;

        aPrintStream.push();

        aPrintStream.printKeyValue("Type", m_type);
        aPrintStream.printKeyValue("Size", recordS);
        aPrintStream.printlnKeyValue("Date", m_date.toString());

        aPrintStream.printKeyValue("SystemID", m_sid);
        aPrintStream.printKeyValue("SubsystemID", subsysid);
        aPrintStream.printlnKeyValue("Flag", m_flag);

        aPrintStream.printlnKeyValueString("Subtype", subtype(), subtypeToString());

        aPrintStream.pop();

        //** Write SMF Time Info out to summaryReport.                      //@SUa   
        //@SUa
        if (m_type == SmfRecordType) { //@SUa

            // Write Heading info only if pageNumber == 0 - then turn off.      //@SUa
            if (PerformanceSummary.pageNumber == 0) { //@SU@
                ++PerformanceSummary.pageNumber; //@SUa
                //PerformanceSummary.writeNewLine();                              //@SUa
                PerformanceSummary.writeString("             Date: ", 20); //@SU@ 
                PerformanceSummary.writeString(m_date.toString(), 29); //@SU@
                PerformanceSummary.writeString("  SysID:", 9); //@SU@
                PerformanceSummary.writeString(m_sid, 4); //@SU@
                PerformanceSummary.writeString(", Page", 7); //@SU9
                PerformanceSummary.writeInt(PerformanceSummary.pageNumber, 1); //@SU9        
                PerformanceSummary.writeNewLine(); //@SU@        	
                PerformanceSummary.writeHdr(); //@SUa
            } // endIf firstPageSwitch                                       //@SUa
            else { //@SU9
                ++PerformanceSummary.lineNumber; //@SU9
                if (PerformanceSummary.lineNumber > my_pageLength) { //@SU9 Hdr every xx Pgs
                    PerformanceSummary.lineNumber = 0; //@SU9
                    PerformanceSummary.writeNewLine(); //@SU9        	
                    PerformanceSummary.writeString("\n============================================", 45); //@SU9 
                    PerformanceSummary.writeString(" = Date:", 8); //@SU9 
                    PerformanceSummary.writeString(m_date.toString(), 29); //@SU9
                    PerformanceSummary.writeString("  SysID:", 9); //@SU9
                    PerformanceSummary.writeString(m_sid, 4); //@SU9
                    PerformanceSummary.writeString(", Page:", 7); //@SU9
                    PerformanceSummary.writeInt(PerformanceSummary.pageNumber, 4); //@SU9        
                    PerformanceSummary.writeNewLine(); //@SU9        	
                    PerformanceSummary.writeHdrShort(); //@SU9
                }
            } // ?

            // Write record#, Type & Subtype                                    //@SUa
            PerformanceSummary.writeNewLine(); //@SUa
            if (my_recordN < 100000) {
                PerformanceSummary.writeInt(my_recordN, 5); //up to '99999' is ok //@SUa
            } else {
                PerformanceSummary.writeString(".", 1); // otherwise print a '.'//@SUa
                PerformanceSummary.writeInt(my_recordN % 10000, 4); // & remainder//@SUa
            } //@SUa

            PerformanceSummary.writeString(" ", 1); //@SUa
            PerformanceSummary.writeInt(m_type, 3); //@SUa
            PerformanceSummary.writeString(".", 1); //@SUa
            PerformanceSummary.writeInt(m_subtype, 1); //@SUa
            PerformanceSummary.writeString(" ", 1); //@SUa

            PerformanceSummary.writeTime(my_hours, my_mins, my_secs); //@SUa
        } // Endif m_type                                                   //@SUa

    } // dumpProlog(...)

    //----------------------------------------------------------------------------
    /**
     * Returns the record subtype as a String.
     * 
     * @return record subtype as a String.
     */
    public String subtypeToString() {

        if (m_type == SmfRecordType) {

            switch (m_subtype) {
                case ServerActivitySmfRecordSubtype:
                    return "SERVER ACTIVITY";
                case ContainerActivitySmfRecordSubtype:
                    return "CONTAINER ACTIVITY";
                case ServerIntervalSmfRecordSubtype:
                    return "SERVER INTERVAL";
                case ContainerIntervalSmfRecordSubtype:
                    return "CONTAINER INTERVAL";
                case J2eeContainerActivitySmfRecordSubtype:
                    return "J2EE CONTAINER ACTIVITY";
                case J2eeContainerIntervalSmfRecordSubtype:
                    return "J2EE CONTAINER INTERVAL";
                case WebContainerActivitySmfRecordSubtype:
                    return "WEB CONTAINER ACTIVITY";
                case WebContainerIntervalSmfRecordSubtype:
                    return "WEB CONTAINER INTERVAL";
                case RequestActivitySmfRecordSubtype:
                    return "REQUEST ACTIVITY";
                case OutboundRequestSmfRecordSubtype:
                    return "OUTBOUND REQUEST";
                case LibertyRequestActivitySmfRecordSubtype:
                    return "Liberty Request Activity";
                case LibertyBatchSmfRecordSubtype:
                    return "Liberty Batch Record";
                default:
                    return "Unknown WebSphere SMF record subtype";
            }
        } else {
            return "Unknown SMF Record type/subtype combination";
        }

    } // subtypeAsString()

} // SmfRecord