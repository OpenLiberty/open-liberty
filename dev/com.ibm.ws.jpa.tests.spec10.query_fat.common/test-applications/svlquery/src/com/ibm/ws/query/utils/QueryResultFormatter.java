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

package com.ibm.ws.query.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceUnitUtil;

public class QueryResultFormatter {
    public String _query;
    public String[] _typeStringArray;
    public Object[] _typeArray;
    public BufferedReader _sqlFileInBR;
    public ArrayList<String> _sqls;
    public ArrayList<String> _ansArys;

    //needed for DQProcessQueryOrAnsList(boolean)
    public String _rootDirStr;
    public String _inFBasePrefix;
    public String _inF2BaseName;
    public ArrayList _ans1Arys;

    static private SimpleDateFormat _tmFmt = new SimpleDateFormat("kk:mm:ss");
    static private SimpleDateFormat _dtFmt = new SimpleDateFormat("yyyy-MM-dd");
    static private GregorianCalendar _cal = new GregorianCalendar();
    static private TimeZone _tz = TimeZone.getDefault();

    private Date _fstTm = null;
    private Date _begTm = null;
    private Date _endTm = null;
    private boolean _isFinder = false;
    private boolean _ans2ansCompare = false;
    PrintWriter _logFilePW = null; //log
    PrintWriter _outFilePW = null; //out
    PrintWriter _diffFilePW = null; //diff
    BufferedWriter _statsBOW = null;

    private EntityManager em;

    public QueryResultFormatter(EntityManager em) {
        this.em = em;
    }

    /**
     * Insert the method's description here.
     * Creation date: (02/02/12 2:23:19 PM)
     *
     * @param row       int
     * @param resultItr com.ibm.ObjectQuery.EexQueryIterator
     */
    public String prtAryTuple(Object result, String sqlStr, int testCnt) {
        ArrayList tuples = (new ArrayList());
        tuples.add(result);
        return prtAryTuple(tuples, sqlStr, testCnt);
    }

    /**
     * Insert the method's description here.
     * Creation date: (02/02/12 2:23:19 PM)
     *
     * @param row       int
     * @param resultItr com.ibm.ObjectQuery.EexQueryIterator
     */
    public String prtAryTuple(List result, String sqlStr) {
        return prtAryTuple(result, sqlStr, 0);
    }

    public static <T> ArrayList<T> stringToArrayList(T[] t, String a) throws Exception {
        ArrayList<T> al = new ArrayList();
        if (t instanceof Byte[]) {
            byte b[] = a.getBytes(); //new byte[a.length()];
            for (int i = 0; i < b.length; i++) {
                t[i] = (T) Byte.valueOf(b[i]);
            }

            al = arrayToArrayList(t);
        } else if (t instanceof Character[]) {
            char b[] = a.toCharArray(); //new byte[a.length()];
            for (int i = 0; i < b.length; i++) {
                t[i] = (T) Character.valueOf(b[i]);
            }
            al = arrayToArrayList(t);
        } else {
            System.out.println(t[0]);
            throw new Exception("illegal argument type: must be Byte or Character");
        }
        return al;
    }

    public static <T> ArrayList<T> arrayToArrayList(T[] a) {
        if (a == null) {
            return null;
        } else {
            ArrayList<T> al = new ArrayList();
            for (int i = 0; i < a.length; i++) {
                al.add(a[i]);
            }
            return al;

        }
    }

    public static Byte[] makeByteArray(byte[] a) throws Exception {
        if (a == null) {
            return null;
        } else {
            Byte[] b = new Byte[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = new Byte(a[i]);
            }
            return b;
        }
    }

    public static Character[] makeCharacterArray(char[] a) throws Exception {
        if (a == null) {
            return null;
        } else {
            Character[] b = new Character[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = new Character(a[i]);
            }
            return b;
        }
    }

    public static Double[] makeDoubleArray(double[] a) throws Exception {
        if (a == null) {
            return null;
        } else {
            Double[] b = new Double[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = new Double(a[i]);
            }
            return b;
        }
    }

    public static Float[] makeFloatArray(float[] a) throws Exception {
        if (a == null) {
            return null;
        } else {
            Float[] b = new Float[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = new Float(a[i]);
            }
            return b;
        }
    }

    public static Integer[] makeIntegerArray(int[] a) throws Exception {
        if (a == null) {
            return null;
        } else {
            Integer[] b = new Integer[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = new Integer(a[i]);
            }
            return b;
        }
    }

    public static Long[] makeLongArray(long[] a) throws Exception {
        if (a == null) {
            return null;
        } else {
            Long[] b = new Long[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = new Long(a[i]);
            }
            return b;
        }
    }

    public static Short[] makeShortArray(short[] a) throws Exception {
        if (a == null) {
            return null;
        } else {
            Short[] b = new Short[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = new Short(a[i]);
            }
            return b;
        }
    }

    /**
     * Insert the method's description here.
     * Creation date: (02/02/12 2:23:19 PM)
     *
     * @param row       int
     * @param resultItr com.ibm.ObjectQuery.EexQueryIterator
     */
    public String prtAryTuple(List tuples, String sqlStr, int testCnt) {//int row, int numCol, Iterator resultItr,IQueryTuple outTuple) {
        String fldname = null;
        Object aColumn = null;
        int numCol = 0;
        if (tuples == null)
            return null;
        if (tuples.size() == 0) {
            numCol = 0;
        } else if (tuples.get(0) instanceof Object[]) {
            numCol = ((Object[]) tuples.get(0)).length;
        } else {
            numCol = 1;
        }
        //start header array with projection elements and replace entity elements with entity names
        int fromIdx = (sqlStr.indexOf("from") != -1) ? sqlStr.indexOf("from") : sqlStr.indexOf("FROM");
        String projection = sqlStr.substring(6, fromIdx);
        String[] header = projection.split(","); //allows 1st data row to reuse data captured for col hdr
        String[] tmpCols = new String[header.length]; //used to store temp columns
        int[] maxColsLengths = new int[header.length]; //used to store max column lengths
        ArrayList<String> outAL = new ArrayList();
        StringBuffer sb = new StringBuffer();
        int row = 0;
        try {
            // calc max col length
            Iterator tuplesItr = tuples.iterator();
            while (tuplesItr.hasNext()) {
                if (tuples.get(0) instanceof Object[]) {
                    maxColsLengths = getOneTupleMaxColsLengths((Object[]) tuplesItr.next(), header, maxColsLengths, row); //get data array row
                } else {
                    Object o = tuplesItr.next();
                    Object[] single = new Object[1];
                    single[0] = o;
                    maxColsLengths = getOneTupleMaxColsLengths(single, header, maxColsLengths, row); //get data array row
                }
                row++;
            }
            // accum data array
            tuplesItr = tuples.iterator();
            row = 0;
            while (tuplesItr.hasNext()) {
                if (tuples.get(0) instanceof Object[]) {
                    outAL.add(prtOneTuple((Object[]) tuplesItr.next(), header, maxColsLengths, row)); //add data array row
                } else {
                    Object o = tuplesItr.next();
                    Object[] single = new Object[1];
                    single[0] = o;
                    outAL.add(prtOneTuple(single, header, maxColsLengths, row)); //add data array row
                }
                row++;
                //gfh? sb.delete(0,sb.length());
            }
            outAL.add(0, " TEST" + testCnt + "; " + sqlStr); //insert query statement at top

            //print query results
            StringBuffer sb2 = new StringBuffer();
            String tmp = null;
            if (outAL != null) {
                Iterator outitr = outAL.iterator();
                while (outitr.hasNext()) {
                    tmp = (String) (outitr.next());
                    sb2.append(tmp + "\n");
                }
            }
            if (row != 1) {
                tmp = (" TEST" + testCnt + "; " + row + " tuples" + "\n");
                sb2.append(tmp);
            } else {
                tmp = (" TEST" + testCnt + "; " + row + " tuple" + "\n");
                sb2.append(tmp);
            }
            return new String(sb2);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return "null";

    }

    /**
     * Insert the method's description here.
     * Creation date: (02/02/12 2:23:19 PM)
     *
     * @param row       int
     * @param resultItr com.ibm.ObjectQuery.EexQueryIterator
     */
    private int[] getOneTupleMaxColsLengths(Object[] outTuple, String[] header, int[] maxColsLengths, int row) {//int row, int numCol, Iterator resultItr,IQueryTuple outTuple) {
        String fldname = null;
        Object aColumn = null;
        int numCol = outTuple.length;
        StringBuffer sb = new StringBuffer();
        StringBuffer sbheader = new StringBuffer();
        StringBuffer sbheadsep = new StringBuffer();
        try {

            for (int i = 0; i < numCol; i++) { // accum data array
                PersistenceUnitUtil puu = em.getEntityManagerFactory().getPersistenceUnitUtil();

                QueryEntityInspector qei = new QueryEntityInspector(puu);
                if (outTuple[i] == null) {
                    aColumn = "null";
                } else if (qei.getEntityKeys(outTuple[i])) { //if entity
                    aColumn = qei.getIdValues().toString();
                    header[i] = qei.getEntityName();
                } else {
                    aColumn = outTuple[i];
                }
                fldname = header[i].trim();
                int olen = aColumn.toString().trim().length();
                int hlen = fldname.length();
                int wlen;
                if (olen > hlen) {
                    wlen = olen;
                } else {
                    wlen = hlen;
                }
                if (wlen > maxColsLengths[i]) {
                    maxColsLengths[i] = wlen;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return maxColsLengths;
    }

    /**
     * Insert the method's description here.
     * Creation date: (02/02/12 2:23:19 PM)
     *
     * @param row       int
     * @param resultItr com.ibm.ObjectQuery.EexQueryIterator
     */
    private String prtOneTuple(Object[] outTuple, String[] header, int[] maxColsLengths, int row) {//int row, int numCol, Iterator resultItr,IQueryTuple outTuple) {
        String fldname = null;
        Object aColumn = null;
        int numCol = outTuple.length;
        StringBuffer sb = new StringBuffer();
        StringBuffer sbheader = new StringBuffer();
        StringBuffer sbheadsep = new StringBuffer();
        try {
            for (int i = 0; i < numCol; i++) { // accum data array
                PersistenceUnitUtil puu = em.getEntityManagerFactory().getPersistenceUnitUtil();
                QueryEntityInspector qei = new QueryEntityInspector(puu);
                if (outTuple[i] == null) {
                    aColumn = "null";
                } else if (qei.getEntityKeys(outTuple[i])) { //if entity
                    aColumn = qei.getIdValues().toString();
                    header[i] = qei.getEntityName();
                } else {
                    aColumn = outTuple[i];
                }
                fldname = header[i].trim();
                int olen = aColumn.toString().length();
                int wlen = maxColsLengths[i];
                int hlen = fldname.length();
                int len = 0;
                int hbuf = 0;
                int obuf = 0;
                hbuf = (wlen - hlen); //use to pad fldname to bring to size of aColumn
                obuf = (wlen - olen); //use to pad aColumn to bring to size of fldname
                //headerArray
                if (row == 0) {
                    for (int x = 1; x <= hbuf / 2; x++)
                        sbheader.append(" "); //pre
                    sbheader.append(fldname);
                    for (int x = 1; x <= hbuf - hbuf / 2; x++)
                        sbheader.append(" ");//post (in case hbuf is odd)
                    sbheader.append(" ");
                }
                //headerArray separator
                if (row == 0) {
                    for (int x = 1; x <= wlen; x++)
                        sbheadsep.append("~"); //all
                    sbheadsep.append(" ");
                }
                //dataArray
                for (int x = 1; x <= obuf / 2; x++)
                    sb.append(" "); //pre
                sb.append(aColumn);
                for (int x = 1; x <= obuf - obuf / 2; x++)
                    sb.append(" ");//post (in case obuf is odd)
                sb.append(" ");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (row == 0) {
            String strheadsep = new String(sbheadsep.append("\n"));
            sb.insert(0, strheadsep);
            String strheader = new String(sbheader.append("\n"));
            sb.insert(0, strheader);
        }
        return new String(sb);
    }

    /**
     * this method reads the input file, either sql or ans, to fill the sql and ans arrayLists with arrayList chuncks
     * add to arrays all " from " with ; and w/o --
     * could extend to all " from " to "tuple"
     * assume from sql single line selects/froms, w/o "--"
     * assume from ans tags on selects/froms and tuple with tags in pos1-3 followed by ;
     * Creation date: (02/04/29 9:32:03 AM)
     *
     * @return boolean
     * @param args java.lang.String[]
     */
    public ArrayList<String> DQProcessInputFile(String strName) throws Exception {
        String dataStr = null;
        String tmpStr = null;
        boolean skipSW = true;
        boolean ansSW = false;
        boolean sqlSW = false;

        System.out.println("DQProcessInputFile starting ");
        System.out.println("DQProcessInputFile strName " + strName);

        int cnt = 0;
        ArrayList ansChunkAL = new ArrayList();
        StringBuffer ansChunkSB = new StringBuffer();
        ArrayList<String> outAL = new ArrayList();
        try {
            _sqlFileInBR = new BufferedReader(new FileReader(strName)); //chgd
        } catch (java.io.FileNotFoundException e) {
            throw new java.io.FileNotFoundException("the file specified by " + strName + " must exist");
        } catch (Exception e) {
            throw new Exception(e);
        }
        while ((dataStr = _sqlFileInBR.readLine()) != null) {
            int cmntPos, semiFPos, semiLPos, fromPos, selectPos, tuplePos;
            cmntPos = dataStr.indexOf("--");
            if (cmntPos == -1) { //else skip
                fromPos = dataStr.toLowerCase().indexOf("from ");
                selectPos = dataStr.toLowerCase().indexOf("select ");
                tuplePos = dataStr.indexOf(" tuple");
                semiFPos = dataStr.indexOf(";");
                semiLPos = dataStr.lastIndexOf(";");
                //distinguish between sql input file and ans input file
                if (!ansSW && !sqlSW) {
                    int dotindex = strName.lastIndexOf(".");
                    if (strName.substring(dotindex + 1).equals("sql")) {
                        sqlSW = true;
                    } else {
                        ansSW = true;
                    }
                }
                if (sqlSW) {
                    if (semiFPos != -1) {
                        outAL.add(dataStr.substring(0, semiFPos));
                        cnt++;
                    }
                } else if (ansSW) {
                    if ((selectPos != -1 && semiFPos <= selectPos - 2 && semiFPos != -1) || (fromPos != -1 && semiFPos == fromPos - 2)) {
                        ansChunkSB.append(dataStr); //first entry and tag for ans chunk
                        //              System.out.println("first ansChunkSB.append "+dataStr);
                        skipSW = false;
                    } else { //accum ans array chunk through tuple line     //what if ans has no tag?
                        if (skipSW == false) {
                            ansChunkSB.append("\n" + dataStr); //accum ans array chunk
                            //                      System.out.println("ansChunkSB.append "+dataStr);
                            if (tuplePos != -1) { //found "tuple" meaning found end of chunk
                                skipSW = true;
                                //                       if (ansChunkAL.size()>0) {  //add prev chunk to ansArray
                                ansChunkSB.append("\n"); //put newline at end of chunk, to give better expected close
                                outAL.add(new String(ansChunkSB));
                                cnt++;
                                ansChunkSB = new StringBuffer();
                            }
                        }
                    }
                } else {
                }
            }
        } //end while

        int dotindex = strName.lastIndexOf(".");
        if (strName.substring(dotindex + 1).equals("sql")) {
            _sqls = outAL;
        } else {
            _ansArys = outAL;
        }
        System.out.println("DQProcessInputFile end: ansSW = " + ansSW + " and number of chunks = " + cnt);
        _sqlFileInBR.close();
        return outAL;
    }

    static public void sortStringArray(String[] sarg) {
        sortImpl(sarg, 0, sarg.length - 1);
    }

    static public void swap(String[] sarg, int loc1, int loc2) {
        String tmp = sarg[loc1];
        sarg[loc1] = sarg[loc2];
        sarg[loc2] = tmp;
    }

    static private void sortImpl(String[] sarg, int left, int right) {
        if (right > left) {
            String o1 = sarg[right];
            int i = left - 1;
            int j = right;
            while (true) {
                while (sarg[++i].compareTo(o1) < 0);
                while (j > 0) {
                    if (sarg[--j].compareTo(o1) < 0) {
                        break;
                    }
                }
                if (i >= j) {
                    break;
                }
                swap(sarg, i, j);
            }
            swap(sarg, i, right);
            sortImpl(sarg, left, i - 1);
            sortImpl(sarg, i + 1, right);
        }
    }

    static public String[] sortQueryResult(String[] qResultString, int begSkip, int endSkip) {
        String tmpResult = null;
        if (qResultString != null && !qResultString.equals("null")) {
            String[] tmp = qResultString;
            if (tmp.length > begSkip + endSkip + 1) {
                String[] tmp1 = new String[qResultString.length - begSkip - endSkip];
                for (int x = begSkip, i = 0; x < tmp.length - endSkip; x++, i++) {
                    tmp1[i] = tmp[x];
                }
                sortStringArray(tmp1);
                String[] tmpSrt = new String[tmp.length];
                for (int x = begSkip, i = 0; x < tmp.length - endSkip; x++, i++) {
                    tmpSrt[i] = tmp[i];
                    tmp[x] = tmp1[i];
                }
                return tmp;
            } else {
                return qResultString;
            }
        } else {
            return qResultString;
        }
    }

    // version returning String[]
    // separate query part from parameter string part and turn the parameter string into an array of strings each containing a TypeName, value pair
    // the parameter string part is surrounded by {} and contains TypeName, value pairs seperated by commas.
    // example input   select e from EmpBean e where e.dept.deptno = ?1 and e.isManager = ?2  {Integer 10,  boolean true}
    // example query output   select e from EmpBean e where e.dept.deptno = ?1 and e.isManager = ?2
    // example parameter output   Integer 10
    // example parameter output   boolean true

    public String[] findParamString(String osqlNormalized) {

        //   System.out.println("parseInput() - osqlNormalized = " +  osqlNormalized);
        _typeStringArray = null;
        int typeBegPos = osqlNormalized.indexOf("{");
        int typeEndPos = osqlNormalized.indexOf("}");
        if (typeBegPos == 0) { //parms but no query
        } else if (typeBegPos > 0) { //query and parms
            _query = new String(osqlNormalized.substring(0, typeBegPos));
        } else { //query and no parms
            _query = new String(osqlNormalized);
        }
        if (typeEndPos - typeBegPos > 1) {
            String _parmString = new String(osqlNormalized.substring(typeBegPos + 1, typeEndPos));
            StringTokenizer st = new StringTokenizer(_parmString, ",");
            ArrayList aTypeArrayList = new ArrayList();
            while (st.hasMoreTokens()) {
                aTypeArrayList.add(st.nextToken());
            }
            _typeStringArray = new String[aTypeArrayList.size()];
            for (int i = 0; i < aTypeArrayList.size(); i++) {
                _typeStringArray[i] = ((String) aTypeArrayList.get(i)).trim();
            }
        }
        return _typeStringArray;
    }

    //change the array of strings containing type value pairs  to an array of java objects of the specified type and having the specified value
    public Object[] setParams(String[] parmStringArray) {
        String qstmt = null;
        Object[] temp = null;
        if (parmStringArray != null) {
            int len = parmStringArray.length;
            temp = new Object[len];
            String diag = "";
            for (int k = 0; k < len; k++) {
                qstmt = parmStringArray[k].trim();
                System.out.println("setParams() - parmStringArray[" + k + "] = " + parmStringArray[k]);
                try {
                    if (qstmt.toLowerCase().startsWith("bigdecimal")) {
                        qstmt = qstmt.substring(11).trim();
                        temp[k] = new java.math.BigDecimal(qstmt);
                        System.out.println("setParams() - temp[" + k + "] = " + temp[k]);
                    } else if (qstmt.toLowerCase().startsWith("biginteger")) {
                        qstmt = qstmt.substring(11).trim();
                        temp[k] = new java.math.BigInteger(qstmt);
                        System.out.println("setParams() - temp[" + k + "] = " + temp[k]);
                    } else if (qstmt.toLowerCase().startsWith("boolean")) {
                        qstmt = qstmt.substring(8).trim();
                        temp[k] = Boolean.valueOf(qstmt);
                        System.out.println("setParams() - temp[" + k + "] = " + temp[k]);
                    } else if (qstmt.toLowerCase().startsWith("byte")) {
                        qstmt = qstmt.substring(5).trim();
                        temp[k] = Byte.valueOf(qstmt);
                    } else if (qstmt.toLowerCase().startsWith("calendar")) {
                        qstmt = qstmt.substring(9).trim();
                        java.sql.Timestamp ts = new java.sql.Timestamp(java.sql.Timestamp.valueOf(qstmt).getTime());
                        java.util.Calendar cd = Calendar.getInstance();
                        cd.setTime(ts);
                        temp[k] = cd;
                    } else if (qstmt.toLowerCase().startsWith("character")) {
                        qstmt = qstmt.substring(10).trim();
                        if (qstmt.substring(0, 1).equals("'") || qstmt.substring(0, 1).equals("\"")) {
                            temp[k] = new Character(qstmt.charAt(1));
                        } else {
                            temp[k] = new Character(qstmt.charAt(0));
                        }
                        System.out.println("setParams() - temp[" + k + "] = " + temp[k]);
                    } else if (qstmt.toLowerCase().startsWith("char ")) {
                        qstmt = qstmt.substring(5).trim();
                        if (qstmt.substring(0, 1).equals("'") || qstmt.substring(0, 1).equals("\"")) {
                            temp[k] = new Character(qstmt.charAt(1));
                        } else {
                            temp[k] = new Character(qstmt.charAt(0));
                        }
                        System.out.println("setParams() - temp[" + k + "] = " + temp[k]);
                    } else if (qstmt.toLowerCase().startsWith("dateu")) {
                        qstmt = qstmt.substring(6).trim();
                        if (qstmt.indexOf("'") == 0 && qstmt.lastIndexOf("'") == qstmt.length() - 1) {
                            qstmt = qstmt.substring(1, qstmt.length() - 1);
                        }
                        temp[k] = new java.util.Date(java.sql.Timestamp.valueOf(qstmt).getTime());
                    } else if (qstmt.toLowerCase().startsWith("date")) {
                        qstmt = qstmt.substring(5).trim();
                        if (qstmt.indexOf("'") == 0 && qstmt.lastIndexOf("'") == qstmt.length() - 1) {
                            qstmt = qstmt.substring(1, qstmt.length() - 1);
                        }
                        temp[k] = new java.sql.Date(java.sql.Date.valueOf(qstmt).getTime());
                    } else if (qstmt.toLowerCase().startsWith("double")) {
                        qstmt = qstmt.substring(7).trim();
                        temp[k] = Double.valueOf(qstmt);
                    } else if (qstmt.toLowerCase().startsWith("float")) {
                        qstmt = qstmt.substring(6).trim();
                        temp[k] = Float.valueOf(qstmt);
                    } else if (qstmt.toLowerCase().startsWith("integer")) {
                        qstmt = qstmt.substring(8).trim();
                        temp[k] = Integer.valueOf(qstmt);
                        System.out.println("setParams() - temp[" + k + "] = " + temp[k]);
                    } else if (qstmt.toLowerCase().startsWith("long")) {
                        qstmt = qstmt.substring(5).trim();
                        temp[k] = Long.valueOf(qstmt);
                    } else if (qstmt.toLowerCase().startsWith("short")) {
                        qstmt = qstmt.substring(6).trim();
                        temp[k] = Short.valueOf(qstmt);
                    } else if (qstmt.toLowerCase().startsWith("string")) {
                        qstmt = qstmt.substring(7);
                        temp[k] = qstmt;
                    } else if (qstmt.toLowerCase().startsWith("timestamp")) {
                        qstmt = qstmt.substring(10).trim();
                        if (qstmt.indexOf("'") == 0 && qstmt.lastIndexOf("'") == qstmt.length() - 1) {
                            qstmt = qstmt.substring(1, qstmt.length() - 1);
                        }
                        temp[k] = new java.sql.Timestamp(java.sql.Timestamp.valueOf(qstmt).getTime());
                    } else if (qstmt.toLowerCase().startsWith("time ")) {
                        qstmt = qstmt.substring(5).trim();
                        if (qstmt.indexOf("'") == 0 && qstmt.lastIndexOf("'") == qstmt.length() - 1) {
                            qstmt = qstmt.substring(1, qstmt.length() - 1);
                        }
                        temp[k] = java.sql.Time.valueOf(qstmt);
                    } else {
                        System.out.println("setParams() - temp[" + k + "] = " + " data type entered unknown");
                        return null;
                    }

                } catch (Exception ex) {
                    System.out.println(" System Exception in doQuery::" + diag + " msg=" + ex.toString());
                }

            } // for
        } // if
        return temp;
    }
}
