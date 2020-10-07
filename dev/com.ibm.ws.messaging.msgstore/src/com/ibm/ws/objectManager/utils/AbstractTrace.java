package com.ibm.ws.objectManager.utils;

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * @author Andrew_Banks
 * 
 *         Assist in implementing the utilis.Trace.
 */
public abstract class AbstractTrace
                implements Trace {

    private Class sourceClass;
    private String traceGroup;
    TraceFactory traceFactory;

    boolean debugEnabled = false;
    boolean eventEnabled = false;
    boolean entryEnabled = false;

    /**
     * Create a trace instance.
     * 
     * @param sourceClass being registered.
     * @param traceGroup to which the class belongs.
     * @param traceFactory creating this AbstractTrace.
     */
    public AbstractTrace(Class sourceClass,
                         String traceGroup,
                         TraceFactory traceFactory) {
        this.sourceClass = sourceClass;
        this.traceGroup = traceGroup;
        this.traceFactory = traceFactory;
    } // AbstractTrace().

    /**
     * Enable tracing of this component.
     * 
     * @param traceLevel levels to be turned on.
     */
    protected final void setLevel(int traceLevel) {

        switch (traceLevel) {
            case Trace.Level_All:
                debugEnabled = true;
                eventEnabled = true;
                entryEnabled = true;
                break;

            case Trace.Level_Entry:
                debugEnabled = false;
                eventEnabled = false;
                entryEnabled = true;
                break;

            case Trace.Level_Debug:
                debugEnabled = true;
                eventEnabled = false;
                entryEnabled = false;
                break;

            case Trace.Level_Event:
                debugEnabled = false;
                eventEnabled = true;
                entryEnabled = false;
                break;

            case Trace.Level_None:
                debugEnabled = false;
                eventEnabled = false;
                entryEnabled = false;
                break;

            default:
        } // switch (traceLevel).

    } // setLevel().

    /**
     * @return Class which is being traced.
     */
    public Class getSourceClass() {
        return sourceClass;
    }

    /**
     * @return String the traceGroup to which this trace belongs.
     */
    public String getTraceGroup() {
        return traceGroup;
    }

    /*
     * Test to see if debug tracing is enabled.
     * 
     * @returns true is method entry tracing is enabled.
     */
    public final boolean isDebugEnabled()
    {
        return debugEnabled;
    } // isDebugEnabled().  

    /*
     * Test to see if method entry tracing is enabled.
     * 
     * @returns true is method entry tracing is enabled.
     */
    public final boolean isEntryEnabled()
    {
        return entryEnabled;
    } // isEntryEnabled().

    /*
     * Test to see if event tracing is enabled.
     * 
     * @returns true is method entry tracing is enabled.
     */
    public final boolean isEventEnabled()
    {
        return eventEnabled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Trace#bytes(java.lang.Class, byte[])
     */
    public final void bytes(Class sourceClass, byte[] data)
    {
        if (data == null)
            bytes(null, sourceClass, data, 0, 0);
        else
            bytes(null, sourceClass, data, 0, data.length);
    } // bytes().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Trace#bytes(java.lang.Class, byte[], int)
     */
    public final void bytes(Class sourceClass, byte[] data, int start)
    {
        if (data == null)
            bytes(null, sourceClass, data, start, 0);
        else
            bytes(null, sourceClass, data, start, data.length);
    } // bytes().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Trace#bytes(java.lang.Class, byte[], int, int)
     */
    public final void bytes(Class sourceClass, byte[] data, int start, int count)
    {
        bytes(null, sourceClass, data, start, count);
    } // bytes().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Trace#bytes(java.lang.Object, java.lang.Class, byte[])
     */
    public final void bytes(Object source, Class sourceClass, byte[] data)
    {
        if (data == null)
            bytes(source, sourceClass, data, 0, 0);
        else
            bytes(source, sourceClass, data, 0, data.length);
    } // bytes().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Trace#bytes(java.lang.Object, java.lang.Class, byte[], int)
     */
    public final void bytes(Object source, Class sourceClass, byte[] data, int start)
    {
        if (data == null)
            bytes(source, sourceClass, data, start, 0);
        else
            bytes(source, sourceClass, data, start, data.length);
    } // bytes().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Trace#bytes(java.lang.Object, java.lang.Class, byte[], int, int)
     */
    public abstract void bytes(Object source, Class sourceClass, byte[] data, int start, int count);

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Trace#debug(java.lang.Class, java.lang.String)
     */
    public final void debug(Class sourceClass, String methodName)
    {
        debug(null, sourceClass, methodName, (Object[]) null);
    } // debug().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Trace#debug(java.lang.Class, java.lang.String, java.lang.Object)
     */
    public final void debug(Class sourceClass, String methodName, Object object)
    {
        debug(null, sourceClass, methodName, new Object[] { object });
    } // debug().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Trace#debug(java.lang.Class, java.lang.String, java.lang.Object[])
     */
    public final void debug(Class sourceClass, String methodName, Object[] objects)
    {
        debug(null, sourceClass, methodName, objects);
    } // debug().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Trace#debug(java.lang.Object, java.lang.Class, java.lang.String)
     */
    public final void debug(Object source, Class sourceClass, String methodName)
    {
        debug(source, sourceClass, methodName, (Object[]) null);
    } // debug().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Trace#debug(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Object)
     */
    public final void debug(Object source, Class sourceClass, String methodName, Object object)
    {
        debug(source, sourceClass, methodName, new Object[] { object });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Trace#debug(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Object[])
     */
    public abstract void debug(Object source, Class sourceClass, String methodName, Object[] objects);

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Trace#entry(java.lang.Class, java.lang.String)
     */
    public final void entry(Class sourceClass, String methodName)
    {
        entry(null, sourceClass, methodName, (Object[]) null);
    } // entry().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Trace#entry(java.lang.Class, java.lang.String, java.lang.Object)
     */
    public final void entry(Class sourceClass, String methodName, Object object)
    {
        entry(null, sourceClass, methodName, new Object[] { object });
    } // entry().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Trace#entry(java.lang.Class, java.lang.String, java.lang.Object[])
     */
    public final void entry(Class sourceClass, String methodName, Object[] objects)
    {
        entry(null, sourceClass, methodName, objects);
    } // entry().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Trace#entry(java.lang.Object, java.lang.Class, java.lang.String)
     */
    public final void entry(Object source, Class sourceClass, String methodName)
    {
        entry(source, sourceClass, methodName, (Object[]) null);
    } // entry().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Trace#entry(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Object)
     */
    public final void entry(Object source, Class sourceClass, String methodName, Object object)
    {
        entry(source, sourceClass, methodName, new Object[] { object });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Trace#entry(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Object[])
     */
    public abstract void entry(Object source, Class sourceClass, String methodName, Object[] objects);

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Trace#exit(java.lang.Class, java.lang.String)
     */
    public final void exit(Class sourceClass, String methodName)
    {
        exit(null, sourceClass, methodName, null);
    } // exit().

    public final void exit(Class sourceClass, String methodName, Object object)
    {
        exit(null, sourceClass, methodName, new Object[] { object });
    } // exit().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Trace#exit(java.lang.Class, java.lang.String, java.lang.Object[])
     */
    public final void exit(Class sourceClass, String methodName, Object[] objects)
    {
        exit(null, sourceClass, methodName, objects);
    } // exit().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Trace#exit(java.lang.Object, java.lang.Class, java.lang.String)
     */
    public final void exit(Object source, Class sourceClass, String methodName)
    {
        exit(source, sourceClass, methodName, null);
    } // exit().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Trace#exit(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Object)
     */
    public final void exit(Object source, Class sourceClass, String methodName, Object object)
    {
        exit(source, sourceClass, methodName, new Object[] { object });
    } // exit().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Trace#exit(java.lang.Object, java.lang.Class, java.lang.String, java.lang.Object[])
     */
    public abstract void exit(Object source, Class sourceClass, String methodName, Object[] objects);

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Trace#event(java.lang.Class, java.lang.String, java.lang.Throwable)
     */
    public final void event(Class sourceClass, String methodName, Throwable throwable)
    {
        event(null, sourceClass, methodName, throwable);
    } // event().

    public abstract void event(Object source, Class sourceClass, String methodName, Throwable throwable);

    public final void info(Class sourceClass, String methodName, String messageIdentifier, Object object)
    {
        info(null, sourceClass, methodName, messageIdentifier, new Object[] { object });
    } // entry().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Trace#info(java.lang.Object, java.lang.Class, java.lang.String, java.lang.String, java.lang.Object)
     */
    public final void info(Object source, Class sourceClass, String methodName, String messageIdentifier, Object object)
    {
        info(source, sourceClass, methodName, messageIdentifier, new Object[] { object });
    } // info().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Trace#info(java.lang.Class, java.lang.String, java.lang.String, java.lang.Object[])
     */
    public final void info(Class sourceClass, String methodName, String messageIdentifier, Object[] objects)
    {
        info(null, sourceClass, methodName, messageIdentifier, objects);
    } // info().

    public void info(Object source,
                     Class sourceClass,
                     String methodName,
                     String messageIdentifier,
                     Object[] objects) {
        java.io.PrintWriter printWriter = traceFactory.getPrintWriter();
        if (printWriter != null)
            printWriter.println(new java.util.Date()
                                + " "
                                + sourceClass.getName()
                                + ":"
                                + methodName
                                + "\n"
                                + traceFactory.nls.format(messageIdentifier, objects));
    } // info().

    /*
     * Byte array formatting methods
     */
    final static String ls = System.lineSeparator();
    final static String DEAD_CHAR = ".";

    private static String pad(String s, int l)
    {
        return pad(s, l, null);
    }

    private static String pad(String s, int l, String p)
    {
        String rc;

        if (p == null)
            p = "0";

        if (s.length() < l)
        {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < l - s.length(); i++)
                sb.append(p);
            rc = new String(sb.toString() + s);
        }
        else
            rc = s.substring(s.length() - l);

        return rc;
    }

    private static String dup(int i)
    {
        return new String("                          " + i + " duplicate line(s) suppressed" + ls);
    }

    /**
     * Produce a formatted view of a byte array. Duplicate output lines are
     * suppressed to save space.
     * Formatting of the byte array starts at the specified position and continues
     * for the specified number of bytes or the end of the data.
     * The bytes will also be converted to into it's String equivalent using the
     * platform default character set. This will be displayed if the
     * <code>displayCharRepresentation</code> flag is set to true.
     * <p>
     * 
     * @param data the byte array to be formatted
     * @param start position to start formatting the byte array
     * @param count of bytes from start position that should be formatted
     * @param displayCharRepresentations Whether to display the character representation
     * @return the formatted byte array
     */
    public static String formatBytes(byte[] data, int start, int count, boolean displayCharRepresentations)
    {
        StringBuffer sb = new StringBuffer();

        if (data != null)
        {
            int len = data.length;
            sb.append(ls + "Length = 0x" + Integer.toHexString(len) + " (" + len + ") bytes start=" + start + " count=" + count + ls + ls);
            if (displayCharRepresentations)
                sb.append("        offset        : 0 1 2 3  4 5 6 7  8 9 A B  C D E F     0 2 4 6 8 A C E " + ls);
            else
                sb.append("        offset        : 0 1 2 3  4 5 6 7  8 9 A B  C D E F" + ls);

            int t;
            boolean skip;
            int suppress = 0;
            int end = start + count;
            String c[] = new String[16]; // Current line bytes
            String p[] = new String[16]; // Previous line bytes
            String str[] = new String[16];// The string representation

            for (int j = 0; j < 16; j++)
            {
                c[j] = null;
                str[j] = null;
            }

            for (int i = 0; i < len; i = i + 16)
            {
                skip = true;

                for (int j = 0; j < 16; j++)
                {
                    t = i + j;
                    if ((t >= start) && (t < end) && (t < len))
                    {
                        c[j] = pad(Integer.toHexString(data[t]), 2);
                        // Strip out some known 'bad-guys' (these are consistent across ASCII / EBCIDIC)
                        // and replace them with the dead character
                        if (c[j].equalsIgnoreCase("00") || // Null 
                            c[j].equalsIgnoreCase("09") || // Tab
                            c[j].equalsIgnoreCase("0a") || // LF
                            c[j].equalsIgnoreCase("0b") || // VertTab
                            c[j].equalsIgnoreCase("0c") || // FF
                            c[j].equalsIgnoreCase("0d") || // CR
                            c[j].equalsIgnoreCase("07")) // Bell
                        {
                            str[j] = DEAD_CHAR;
                        }
                        else
                        {
                            str[j] = new String(data, t, 1); // Conversion is done here using the default
                            // character set of the platform
                        }
                        skip = false;
                    }
                    else
                    {
                        c[j] = "  ";
                        str[j] = DEAD_CHAR;
                    }
                }

                if (skip)
                {
                    if (suppress > 0)
                        sb.append(dup(suppress));
                    suppress = 0;
                    c[0] = null; // Force a line difference
                }
                else
                {
                    if (c[0].equals(p[0]) && c[1].equals(p[1]) && c[2].equals(p[2]) && c[3].equals(p[3]) &&
                        c[4].equals(p[4]) && c[5].equals(p[5]) && c[6].equals(p[6]) && c[7].equals(p[7]) &&
                        c[8].equals(p[8]) && c[9].equals(p[9]) && c[10].equals(p[10]) && c[11].equals(p[11]) &&
                        c[12].equals(p[12]) && c[13].equals(p[13]) && c[14].equals(p[14]) && c[15].equals(p[15]))
                    {
                        suppress++;
                    }
                    else
                    {
                        if (suppress > 0)
                            sb.append(dup(suppress));
                        sb.append("0x" + pad(Integer.toHexString(i), 8) + " (" + pad(new Integer(i).toString(), 8, " ") + ") : ");
                        sb.append(c[0] + c[1] + c[2] + c[3] + " " + c[4] + c[5] + c[6] + c[7] + " " + c[8] + c[9] + c[10] + c[11] + " " + c[12] + c[13] + c[14] + c[15]);
                        if (displayCharRepresentations)
                        {
                            sb.append("  | ");
                            sb.append(str[0] + str[1] + str[2] + str[3] + str[4] + str[5] + str[6] + str[7] + str[8] + str[9] + str[10] + str[11] + str[12] + str[13] + str[14]
                                      + str[15]);
                        }
                        sb.append(ls);
                        for (int j = 0; j < 16; j++)
                            p[j] = c[j];
                        suppress = 0;
                    }
                }
            }

            if (suppress > 0)
                sb.append(dup(suppress));
        }

        return sb.toString();
    } // formatBytes().

} // class AbstractTrace.
