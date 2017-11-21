/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package test.common.zos;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ZWASJoblogReader provides handy methods for reading/parsing joblogs.
 */
public class ZWASJoblogReader {

    // Trace record header pattern.
    // Obviously this is sensitive to format changes.
    // The "01" in the middle there is a "version" field.  If the trace header
    // format changes, the version will change, so we can use the version field
    // to determine the format.  For now, just assume version "01".
    // Trace: 2009/06/27 15:09:10.675 01 t=6C1E88 c=UNK key=S2 (13007002)
    private static final String rx_any = "[\\w\\W]";
    private static final String rx_abnl = "[^\n]"; // "any but newline"
    private static final String rx_date = "\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}";
    private static final String rx_ver = "\\d{2}";
    private static final String rx_tcb = "[A-Fa-f0-9]{6}";
    private static final String rx_tp = "[A-Fa-f0-9]{8}";

    private static final Pattern _traceHeaderPattern =
                    Pattern.compile("[0-9 ]*Trace: (" + rx_date + ") (" + rx_ver + ") t=(" + rx_tcb + ")" + rx_abnl + "*\\((" + rx_tp + ")\\)" + rx_any + "*");

    /**
     * Passed as the second parameter to parseTraceHeader to get the date portion of the header.
     */
    public static final int DATE_GROUP = 1;
    /**
     * Passed as the second parameter to parseTraceHeader to get the version portion of the header.
     */
    public static final int VER_GROUP = 2;
    /**
     * Passed as the second parameter to parseTraceHeader to get the TCB portion of the header.
     */
    public static final int TCB_GROUP = 3;
    /**
     * Passed as the second parameter to parseTraceHeader to get the trace number of the header.
     */
    public static final int TP_GROUP = 4;

    private static final Pattern _traceDataPattern = Pattern.compile("\\s{2,4}([\\w][^:]+):" + rx_any + "*");

    private static final Pattern _quickTraceHeader = Pattern.compile("[0-9 ]*Trace: (" + rx_date + ") .*");
    private static final Pattern _quickBossLogHeader = Pattern.compile("[0-9 ]*BossLog: \\{ \\d{4}\\} (" + rx_date + ") .*");
    private static final Pattern _quickJesmsglgContinuation = Pattern.compile("\\s{4}\\d{3}\\s{11}.*");
    private static final Pattern _jesmsglgPattern = Pattern.compile("\\s(\\d{2}[.]\\d{2}[.]\\d{2})\\sSTC\\d{5}\\s+(.*)");
    private static final Pattern _jesmsglgLogDatePattern = Pattern.compile("\\s\\d{2}[.]\\d{2}[.]\\d{2}\\sSTC\\d{5} ---- [^\\d]*(\\d{2}\\s[A-Z]{3}\\s\\d{4}) ----.*");

    private static final Pattern _bossLogHeader = Pattern.compile("[0-9 ]*BossLog: \\{ \\d{4}\\} (" + rx_date + ") (" + rx_ver + ") (" + rx_any + "*)\\.\\.\\. (" + rx_any + "*)");

    // Buffer size for BufferedReader.mark() purposes.
    // Should be large enough to encompass the largest Trace: record.
    private static final int MARK_BUFLEN = 1024 * 32;

    // Internals.
    private String _rawJoblog = null;
    private BufferedReader _jesmsglg = null;
    private BufferedReader _sysout = null;
    private BufferedReader _sysprint = null;
    private String _sysoutLine = "";
    private String _sysprintLine = "";
    private String _jesmsglgLine = "";
    private static String _jesmsglgLogDate = "";
    private static String _jesmsglgZone = "EST"; // Change to EDT in spring
    private Date _timeConstraint = null;

    /**
     * @param joblog The entire joblog in a String object.
     */
    public ZWASJoblogReader(String joblog) {
        _rawJoblog = joblog;
        init();
    }

    /**
     * Called by CTOR and reset().
     */
    private void init() {
        _jesmsglg = new BufferedReader(new StringReader(_rawJoblog), MARK_BUFLEN);
        _sysout = new BufferedReader(new StringReader(_rawJoblog), MARK_BUFLEN);
        _sysprint = new BufferedReader(new StringReader(_rawJoblog), MARK_BUFLEN);

        _sysoutLine = "";
        _sysprintLine = "";
        _jesmsglgLine = "";
        _timeConstraint = null;
        _jesmsglgLogDate = "";
        //begins at 2:00 a.m. on the second Sunday of March and
        //ends at 2:00 a.m. on the first Sunday of November
        SimpleTimeZone estTz = new SimpleTimeZone(-5 * 60 * 60 * 1000,
                        "America/New_York",
                        Calendar.MARCH, 8, -Calendar.SUNDAY, 7200000,
                        Calendar.NOVEMBER, 1, -Calendar.SUNDAY, 7200000);
        Date aDate = new Date();
        if (estTz.inDaylightTime(aDate)) {
            Utils.println("init() inDaylightTime true: " + Utils.tsFormatZone.format(aDate));
            _jesmsglgZone = "GMT-04:00";
        }

        try {
            // Try to discover whether we're running on VICOM or EZWAS.  The JESMSGLG timezone
            // is different between the two systems (EZWAS: EDT; VICOM: GMT-05:00). 
            // I don't know if this will change when we switch to EST.
            //
            // 06/12/2014: Verified with Mark Stuckey that all test systems now run the same timezone (EDT).
            _jesmsglgLine = _jesmsglg.readLine();

            // Read until the first "Trace:" entry.
            while (_sysprintLine != null && !_quickTraceHeader.matcher(_sysprintLine).matches())
                _sysprintLine = _sysprint.readLine();

            // Read until the first "BossLog:" entry.
            while (_sysoutLine != null && !_quickBossLogHeader.matcher(_sysoutLine).matches())
                _sysoutLine = _sysout.readLine();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Resets the readers back to the beginning of the joblog.
     */
    public void reset() {
        init();
    }

    /**
     * Return the next Trace record.
     * 
     * @return The next trace record.
     */
    public String readTraceRecord()
                    throws Exception {
        // Read until the next "Trace:" entry.
        while (_sysprintLine != null && !_quickTraceHeader.matcher(_sysprintLine).matches())
            _sysprintLine = _sysprint.readLine();

        if (_sysprintLine == null)
            return null; // next "Trace:" entry not found.  Must be at end of log.

        // If a _timeConstraint is set, check it against the current record.  
        // If the current record has a timestamp AFTER the _timeConstraint, then
        // don't read it.  Return null.
        if (_timeConstraint != null) {
            Matcher m = _quickTraceHeader.matcher(_sysprintLine);
            m.matches();
            Date recordDate = Utils.tsFormatZone.parse(m.group(1) + " (GMT)"); // need to convert to GMT for proper compare.
            if (recordDate.after(_timeConstraint) || recordDate.equals(_timeConstraint))
                return null;
        }

        // Collect the trace record.
        StringBuffer sb = new StringBuffer(_sysprintLine);
        _sysprintLine = _sysprint.readLine();
        while (_sysprintLine != null
               && !_quickTraceHeader.matcher(_sysprintLine).matches()
               && !_quickBossLogHeader.matcher(_sysprintLine).matches()) {
            // Keep reading until the next "Trace:" or "BossLog:" entry
            sb.append("\n" + _sysprintLine);
            _sysprintLine = _sysprint.readLine();
        }

        return sb.toString();
    }

    /**
     * Return the next BossLog record.
     * 
     * @return The next BossLog record.
     */
    public String readBossLogRecord()
                    throws Exception {
        // Read until the next "BossLog:" entry.
        while (_sysoutLine != null && !_quickBossLogHeader.matcher(_sysoutLine).matches())
            _sysoutLine = _sysout.readLine();

        if (_sysoutLine == null)
            return null; // next "BossLog:" entry not found.  Must be at end of log.
        //Utils.println("readBossLogRecord_sysoutLine: " + _sysoutLine);
        // If a _timeConstraint is set, check it against the current record.  
        // If the current record has a timestamp AFTER the _timeConstraint, then
        // don't read it.  Return null.
        if (_timeConstraint != null) {
            Utils.println("using time constraint");
            Matcher m = _quickBossLogHeader.matcher(_sysoutLine);
            m.matches();
            Date recordDate = Utils.tsFormatZone.parse(m.group(1) + " (GMT)"); // need to convert to GMT for proper compare.
            if (recordDate.after(_timeConstraint) || recordDate.equals(_timeConstraint))
                return null;
        }

        // Collect the BossLog record.
        StringBuffer sb = new StringBuffer(_sysoutLine);
        _sysoutLine = _sysout.readLine();
        while (_sysoutLine != null
               && !_quickBossLogHeader.matcher(_sysoutLine).matches()
               && !_quickTraceHeader.matcher(_sysoutLine).matches()) {
            // Keep reading until the next "BossLog:" or "Trace:" entry
            sb.append("\n" + _sysoutLine);
            _sysoutLine = _sysout.readLine();
        }

        return sb.toString();
    }

    /**
     * Read records from the JESMSGLG.
     * 13.45.21 STC00181 BBOJ0077I: com.ibm.CSI.claimStateful = true
     * 13.45.21 STC00181 BBOJ0077I: com.ibm.ws.security.propagationExcludeList = 733
     * 733 com.ibm.security.jgss.*:javax.security.auth.kerberos.KerberosKey:javax
     * 733 .security.auth.kerberos.KerberosTicket
     * 13.45.21 STC00181 BBOJ0077I: com.ibm.CORBA.ServerName = server1
     */
    public String readJesmsglgRecord()
                    throws Exception {
        // Read until the next JESMSGLG entry.

        while (_jesmsglgLine != null && !_jesmsglgPattern.matcher(_jesmsglgLine).matches())
            _jesmsglgLine = _jesmsglg.readLine();

        if (_jesmsglgLine == null)
            return null; // next record not found.  Must be at end of log.

        // Check if this is a LogDate record.  If so, update the _jesmsglgLogDate
        Matcher m = _jesmsglgLogDatePattern.matcher(_jesmsglgLine);
        if (m.matches()) {
            _jesmsglgLogDate = m.group(1);
            Utils.println("Updating jesmsglgLogDate: " + _jesmsglgLogDate);
        }

        // If a _timeConstraint is set, check it against the current record.  
        // If the current record has a timestamp AFTER the _timeConstraint, then
        // don't read it.  Return null.
        if (_timeConstraint != null) {
            Matcher m2 = _jesmsglgPattern.matcher(_jesmsglgLine);
            m2.matches();
            Date recordDate = Utils.tsFormatHMS.parse(_jesmsglgLogDate + " " + m2.group(1) + " (" + _jesmsglgZone + ")"); // add in log date.
            if (recordDate.after(_timeConstraint) || recordDate.equals(_timeConstraint))
                return null;
        }

        // Collect the Jesmsglg record.
        StringBuffer sb = new StringBuffer(_jesmsglgLine);
        _jesmsglgLine = _jesmsglg.readLine();
        while (_jesmsglgLine != null
               && _quickJesmsglgContinuation.matcher(_jesmsglgLine).matches()) {
            // Keep reading continuation lines.
            sb.append("\n" + _jesmsglgLine);
            _jesmsglgLine = _jesmsglg.readLine();
        }

        return sb.toString();
    }

    /**
     * Read the next trace record on the given TCB.
     * 
     * @param tcb
     * @return The next trace record, or null.
     */
    public String readTraceRecordTCB(String tcb)
                    throws Exception {
        String next = readTraceRecord();
        while (next != null && !tcb.equals(parseTraceHeader(next, TCB_GROUP))) {
            next = readTraceRecord();
        }
        return next;
    }

    /**
     * Read the next instance of the given trace point.
     * 
     * @param tp
     * @return The next trace record, or null.
     */
    public String readTraceRecordTP(String tp)
                    throws Exception {
        String next = readTraceRecord();
        while (next != null && !tp.equals(parseTraceHeader(next, TP_GROUP))) {
            next = readTraceRecord();
        }
        return next;
    }

    /**
     * Returns the section of the trace record associated with the given
     * trace data label.
     * 
     * @param traceRecord
     * @param label The trace data label.
     * @return A subsection of the trace record containing only the
     *         data associated with the given label.
     */
    public static String parseTraceData(String traceRecord, String label) {
        if (traceRecord == null)
            return null;

        StringBuffer retMe = new StringBuffer();
        boolean capturing = false;

        String[] lines = traceRecord.split("\n");
        for (int i = 1; i < lines.length; ++i) // always skip first line (Trace header).
        {
            Matcher m = _traceDataPattern.matcher(lines[i]);
            if (m.matches()) {
                if (capturing)
                    break; // if we're already capturing, then we've hit the next piece of data.  so we're done. 
                else if (m.group(1).trim().equals(label))
                    capturing = true;
            }

            if (capturing)
                retMe.append(lines[i] + "\n");
        }

        return retMe.toString();
    }

    /**
     * Find the next trace record that contains all the input strings.
     * 
     * @param matchArgs strings to match.
     */
    public String findTraceRecord(String[] matchArgs)
                    throws Exception {
        String next = readTraceRecord();
        while (next != null && !matches(next, matchArgs)) {
            next = readTraceRecord();
        }
        return next;
    }

    /**
     * @F23267.1A
     * @param t1 the start timestamp
     * @param t2 the end timestamp for the search
     * @param matchArgs strings to match
     * 
     */
    public String findTraceRecord(Date t1, Date t2, String[] matchArgs)
                    throws Exception {

        Utils.println("start timestamp: " + t1 + " end timestamp: " + t2);

        if (t1 != null)
            fastForwardTrace(t1);
        if (t2 != null)
            setTimeConstraintPlus1(t2);

        String next = readTraceRecord();
        while (next != null && !matches(next, matchArgs)) {
            next = readTraceRecord();
        }

        if (t2 != null)
            resetTimeConstraint();

        Utils.println("traceRecord:::::::" + next);
        return next;
    }

    /**
     * Find the next trace record from the given TCB that contains all
     * the input strings.
     * 
     * @param tcb
     * @param matchArgs strings to match.
     */
    public String findTraceRecordTCB(String tcb, String[] matchArgs)
                    throws Exception {
        String next = readTraceRecordTCB(tcb);
        while (next != null && !matches(next, matchArgs)) {
            next = readTraceRecordTCB(tcb);
        }
        return next;
    }

    /**
     * Matches the given array of strings to the given tracerecord.
     * 
     * @param traceRecord
     * @param matchArgs strings to match.
     * @return true, if all matchArgs are contained in traceRecord; otherwise, false.
     */
    public static boolean matches(String traceRecord, String[] matchArgs) {
        if (traceRecord == null)
            return false;

        for (int i = 0; i < matchArgs.length; ++i) {
            if (traceRecord.indexOf(matchArgs[i]) == -1) {
                // Not a match.  
                return false;
            }
        }
        return true; // All matchArgs were found.  The trace record matches.
    }

    /**
     * "Fast-forward" the log reader to the next trace record with a timestamp
     * (Date) newer than the given Date. The read pointer is positioned directly
     * before the "newer" trace record. The next readTraceRecord() will return
     * the "newer" trace record.
     * 
     * @param toHere given Date.
     */
    public void fastForwardTrace(Date toHere)
                    throws Exception {
        Utils.println("Fast-forward joblog to: " + Utils.tsFormatZone.format(toHere));

        // Mark the _sysprint position before reading the next trace record.
        // If the next trace record is subsequent to the "toHere" Date, then 
        // reset the _sysprint position back to the mark, so that the next
        // readTraceRecord will return the first trace record subsequent to 
        // the "toHere" Date.
        //
        // Note: _sysprintLine, if not null, has already read off the header for
        // the next trace record.  Need to preserve that too.
        _sysprint.mark(MARK_BUFLEN);
        String saveSysprintLine = _sysprintLine;

        String next = readTraceRecord();
        String last = next;
        Date d = getTraceDate(next);
        while (d != null && d.before(toHere)) {
            _sysprint.mark(MARK_BUFLEN);
            saveSysprintLine = _sysprintLine;
            last = next;
            next = readTraceRecord();
            d = getTraceDate(next);
        }
        if (d != null) {
            try {
                _sysprint.reset(); // only reset if we actually found a newer trace record.
            } catch (Exception e) {
                Utils.println("Caught exception in _sysprint.reset(): " + e);
                Utils.println("MARK_BUFLEN: " + MARK_BUFLEN + "; Last record read:\n" + next);
                throw e;
            }
            _sysprintLine = saveSysprintLine;
        } else
            Utils.println("Fast-forward failed (joblog may be truncated).  Last trace entry: \n" + last);
    }

    /**
     * "Fast-forward" the log reader to the next jesmsglg record with a timestamp
     * (Date) newer than the given Date. The read pointer is positioned directly
     * before the "newer" jesmsglg record. The next readJesmsglgRecord() will return
     * the "newer" jesmsglg record.
     * 
     * @param toHere given Date.
     */
    public void fastForwardJesmsglg(Date toHere)
                    throws Exception {
        Utils.println("Fast-forward jesmsglg to: " + Utils.tsFormatZone.format(toHere));

        // Mark the _jesmsglg position before reading the next record.
        // If the next record is subsequent to the "toHere" Date, then 
        // reset the _jesmsglg position back to the mark, so that the next
        // readJesmsglgRecord will return the first record subsequent to 
        // the "toHere" Date.
        //
        // Note: _jesmsglgLine, if not null, has already read off the header for
        // the next record.  Need to preserve that too.
        _jesmsglg.mark(MARK_BUFLEN);
        String saveJesmsglgLine = _jesmsglgLine;

        String next = readJesmsglgRecord();
        String last = next;
        Date d = parseJesmsglgDate(next);
        while (d != null && d.before(toHere)) {
            _jesmsglg.mark(MARK_BUFLEN);
            saveJesmsglgLine = _jesmsglgLine;
            last = next;
            next = readJesmsglgRecord();
            d = parseJesmsglgDate(next);
        }
        if (d != null) {
            _jesmsglg.reset(); // only reset if we actually found a newer record.
            _jesmsglgLine = saveJesmsglgLine;
        } else
            Utils.println("Fast-forward failed (joblog may be truncated).  Last jesmsglg entry: \n" + last);
    }

    /**
     * "Fast-forward" the log reader to the next BossLog record with a timestamp
     * (Date) newer than the given Date. The read pointer is positioned directly
     * before the "newer" record. The next readBossLogRecord() will return
     * the "newer" record.
     * 
     * @param toHere given Date.
     */
    public void fastForwardBossLog(Date toHere)
                    throws Exception {
        Utils.println("Fast-forward log to: " + Utils.tsFormatZone.format(toHere));

        // Mark the _sysout position before reading the next record.
        // If the next record is subsequent to the "toHere" Date, then 
        // reset the _sysout position back to the mark, so that the next
        // readBossLogRecord will return the first record subsequent to 
        // the "toHere" Date.
        //
        // Note: _sysoutLine, if not null, has already read off the header for
        // the next record.  Need to preserve that too.
        _sysout.mark(MARK_BUFLEN);
        String saveSysoutLine = _sysoutLine;

        String next = readBossLogRecord();
        String last = next;
        Date d = parseBossLogDate(next);
        while (d != null && d.before(toHere)) {
            _sysout.mark(MARK_BUFLEN);
            saveSysoutLine = _sysoutLine;
            last = next;
            next = readBossLogRecord();
            d = parseBossLogDate(next);
        }
        if (d != null) {
            _sysout.reset(); // only reset if we actually found a newer record.
            _sysoutLine = saveSysoutLine;
        } else
            Utils.println("Fast-forward failed (joblog may be truncated).  Last BossLog entry: \n" + last);
    }

    /**
     * Set a time constraint (_timeConstraint). The _timeConstraint marks an
     * upper-limit timestamp for subsequent calls to readTraceRecord/readBossLogRecord/
     * readJesmsglgRecord. If the next record to be read by one of the read*Record
     * methods has a timestamp that is LATER than the _timeConstraint, then the
     * record will NOT be read. The read*Record method will return null.
     * 
     * Any additional records in the log that have timestamps later than the
     * _timeConstraint will not be read until the _timeConstraint has been lifted
     * via resetTimeConstraint, or by setting the _timeConstraint to a new, later value.
     * 
     * @param timeConstraint The time constraint
     * @return The previous _timeConstraint (null, if none set).
     */
    public Date setTimeConstraint(Date timeConstraint) {
        Date tmp = _timeConstraint;
        _timeConstraint = timeConstraint;
        if (_timeConstraint != null)
            Utils.println("setTimeConstraint to " + Utils.tsFormatZone.format(_timeConstraint));
        else
            Utils.println("setTimeConstraint to null");
        return tmp;
    }

    /**
     * Set the time constraint to the given date plus 1 second (see setTimeConstraint).
     * 
     * This method is useful for methods like Machine.getDate that don't have
     * millisecond precision.
     * 
     * @param timeConstraint The time constraint
     * @return The previous _timeConstraint (null if not set).
     */
    public Date setTimeConstraintPlus1(Date timeConstraint) {
        Calendar c = Calendar.getInstance();
        c.setTime(timeConstraint);
        c.set(Calendar.SECOND, c.get(Calendar.SECOND) + 1);
        return setTimeConstraint(c.getTime());
    }

    /**
     * Clear the time constraint.
     * 
     * @return The previous _timeConstraint (null if not set).
     */
    public Date resetTimeConstraint() {
        return setTimeConstraint(null);
    }

    /**
     * Parse the trace header and return the specified data grouping.
     * 
     * @param traceRecord
     * @param retGroup the data group index (e.g. DATE_GROUP, TCB_GROUP, etc).
     * @return The requested data group, or null if no match.
     */
    public static String parseTraceHeader(String traceRecord, int retGroup) {
        if (traceRecord != null) {
            Matcher m = _traceHeaderPattern.matcher(traceRecord);
            if (m.matches())
                return m.group(retGroup);
        }
        return null;
    }

    /**
     * Parse the trace header and return the Matcher object.
     * 
     * @param traceRecord
     * @return A java.util.regex.Matcher object.
     */
    public static Matcher parseTraceHeader(String traceRecord) {
        if (traceRecord != null)
            return _traceHeaderPattern.matcher(traceRecord);
        else
            return null;
    }

    /**
     * Returns a Date object to represent the date and time specified in the
     * trace record header.
     * 
     * Note: assumes the trace record's timezone is GMT.
     * 
     * @param traceRecord
     * @return Date
     */
    public static Date getTraceDate(String traceRecord)
                    throws Exception {
        if (traceRecord != null) {
            Matcher m = _traceHeaderPattern.matcher(traceRecord);
            if (m.matches())
                return Utils.tsFormatZone.parse(m.group(DATE_GROUP) + " (GMT)"); // need to convert to GMT for proper compare.
        }
        return null;
    }

    /**
     * Returns the TCB from the given traceRecord.
     * 
     * @param traceRecord
     * @return tcb
     */
    public static String parseTraceTCB(String traceRecord) {
        return parseTraceHeader(traceRecord, TCB_GROUP);
    }

    /**
     * Parse the message portion of a JESMSGLG record.
     * 
     * Note: the JESMSGLG record is concatenated first (via concatJesmsglgRecord),
     * so the message will not have any line breaks in it.
     * 
     * @param jesmsglgRecord - a jesmsglgRecord, read via readJesmsglgRecord()
     * @return the message in the record
     */
    public static String parseJesmsglgMsg(String jesmsglgRecord) {
        String concat = concatJesmsglgRecord(jesmsglgRecord);

        if (concat != null) {
            Matcher m = _jesmsglgPattern.matcher(concat);
            if (m.matches())
                return m.group(2);
        }
        return null;
    }

    /**
     * Parse the timestamp portion of a JESMSGLG record.
     * 
     * @param jesmsglgRecord - a jesmsglgRecord, read via readJesmsglgRecord()
     * @return the Date (HH:mm:ss only) of the record
     * @throws Exception
     */
    public static Date parseJesmsglgDate(String jesmsglgRecord)
                    throws Exception {
        String concat = concatJesmsglgRecord(jesmsglgRecord);

        if (concat != null) {
            Matcher m = _jesmsglgPattern.matcher(concat);
            if (m.matches())
                return Utils.tsFormatHMS.parse(_jesmsglgLogDate + " " + m.group(1) + " (" + _jesmsglgZone + ")"); // add in log date.
        }
        return null;
    }

    /**
     * Parse the message ID of a JESMSGLG record.
     * 
     * @param jesmsglgRecord - a jesmsglgRecord, read via readJesmsglgRecord()
     * @return the message ID of the record
     * @throws Exception
     */
    public static String parseJesmsglgMsgId(String jesmsglgRecord)
                    throws Exception {
        String msg = parseJesmsglgMsg(jesmsglgRecord);
        if (msg != null) {
            String[] s = msg.split("[ :]");
            return s[0];
        }
        return null;
    }

    /**
     * Parse the message ID of a JESMSGLG record. Looking for WTOR msgid : *nn XXXXXXXI
     * (same as parsJesmsglgMsgId except msgid is in a different place for WTOR)
     * 
     * @param jesmsglgRecord - a jesmsglgRecord, read via readJesmsglgRecord()
     * @return the message ID of the record
     * @throws Exception
     */
    public static String parseJesmsglgWTORMsgId(String jesmsglgRecord)
                    throws Exception
    {
        String msg = parseJesmsglgMsg(jesmsglgRecord);
        if (msg != null)
        {
            String s = msg.substring(4, 12);
            return s;
        }
        return null;
    }

    /**
     * Convert a JESMSGLG record from this:
     * 
     * 13.45.29 STC00181 BBOO0222I: ZAIO0001I: z/OS asynchronous IO TCP Channel 759
     * 759 TCPInboundChannel_ipcc.Default_IPC_Connector_Name is listening on host
     * 759 localhost port 9633.
     * 
     * to this:
     * 
     * 13.45.29 STC00181 BBOO0222I: ZAIO0001I: z/OS asynchronous IO TCP Channel TCPInboundChannel_ipcc.Default_IPC_Connector_Name is listening on host localhost port 9633.
     */
    public static String concatJesmsglgRecord(String jesmsglgRecord) {
        if (jesmsglgRecord == null)
            return null;

        String[] lines = jesmsglgRecord.split("[\\r\\n]+");
        String retMe = null;
        if (lines.length == 1)
            retMe = lines[0];
        else {
            retMe = lines[0].substring(0, lines[0].length() - 4);
            for (int i = 1; i < lines.length; ++i)
                retMe += lines[i].substring(20);
        }
        return retMe;
    }

    /**
     * Read from the JESMSGLG and return the first matching message--optionally
     * between times "t1" and ("t2" + 1 sec).
     * 
     * @param t1 starting Date within the ZWASJoblogReader to start searching
     *            for a match or null
     * @param t2 ending Date of the search for a match or null
     * @param compareValues an array of Strings to match the target message
     * @return String as returned from ZWASJoblogReader.readJesmsglgRecord or null
     * @throws Exception
     */
    public String findJesmsglgRecord(Date t1,
                                     Date t2,
                                     String[] compareValues) //@F8180.6A
    throws Exception {
        // Position for current search, maybe
        if (t1 != null)
            fastForwardJesmsglg(t1);
        if (t2 != null)
            setTimeConstraintPlus1(t2);

        String jesmsglgRecord = readJesmsglgRecord();
        while (jesmsglgRecord != null) {
            if (ZWASJoblogReader.matches(jesmsglgRecord, compareValues))
                break;

            jesmsglgRecord = readJesmsglgRecord();
        }

        if (t2 != null)
            resetTimeConstraint();

        return jesmsglgRecord;
    }

    /**
     * Parse the timestamp of a BossLog record.
     * 
     * @param bossLogRecord Input BossLog record, read via readBossLogRecord()
     * @return Date object representing timestamp of record.
     * @throws Exception
     */
    public static Date parseBossLogDate(String bossLogRecord)
                    throws Exception {
        if (bossLogRecord != null) {
            Matcher m = _bossLogHeader.matcher(bossLogRecord);
            if (m.matches()) {
                // Utils.println(" match groups: #" + m.group(1) + "# #" + m.group(2) + "# #" + m.group(3) + "# #" + m.group(4) + "#");
                return Utils.tsFormatZone.parse(m.group(1) + " (GMT)"); // add zone
            }
        }
        return null;
    }

    public String getRawJobLog() {
        return _rawJoblog;
    }

    /**
     * Get the message from a boss record.
     * 
     * @param bossLogRecord the record with the message we want
     * @return bosslog record message
     */
    public static String parseBossLogMsg(String bossLogRecord)
                    throws Exception {
        if (bossLogRecord != null) {
            Matcher m = _bossLogHeader.matcher(bossLogRecord);
            if (m.matches()) {
                return m.group(4);
            }
        }
        return null;
    }
}
