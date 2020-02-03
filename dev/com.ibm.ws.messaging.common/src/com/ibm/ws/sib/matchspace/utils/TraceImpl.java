/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.matchspace.utils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * @author Neil Young
 *
 * Implements the ObjectManager trace interface to integrate with WAS tracing.
 *
 */
public class TraceImpl implements Trace
{
    private static final Class cclass = TraceImpl.class;

    // Create the resource bundle for the trace templates.
    private static java.util.ResourceBundle traceTemplates = null;

    // The following static initializers may throw exceptions, which cause tracing in their turn
    // so they must follow the static initialisation above.
    static
    {
        String templateName = cclass.getPackage().getName()+".TraceTemplates";
        try
        {
            traceTemplates = java.util.ResourceBundle.getBundle(templateName);
        }
        catch (java.util.MissingResourceException exception)
        {
            // No FFDC Code Needed.
            FFDC.processException(cclass,"<static.init>",exception,"1:56:1.7",new Object[]{templateName});
        }
    } // static initializer.

    public static java.io.PrintWriter printWriter = null;

    //  printWriter = new java.io.PrintWriter(System.out);
    //
    //  static {
    //    try {
    //      java.io.FileWriter traceFileWriter = new java.io.FileWriter("C:\\temp\\trace.txt");
    //      printWriter = new java.io.PrintWriter(traceFileWriter);
    //    } catch (java.io.IOException e){}
    //  }

    // Compile time switch to force trace on.
    public static final boolean usePrintWriterForTrace = false;

    private TraceComponent traceComponent;

    /**
     * Create a trace instance.
     *
     * @param class being registered.
     */
    public TraceImpl(Class sourceClass, String traceGroup)
    {
        traceComponent = Tr.register(sourceClass
                                     ,traceGroup
                                     ,MatchSpaceConstants.MSG_BUNDLE);
    } // constructor()..

    /*
     * Test to see if debug tracing is enabled.
     * @returns true is method entry tracing is enabled.
     */
    public final boolean isDebugEnabled()
    {
        if (usePrintWriterForTrace) return true;
        return traceComponent.isDebugEnabled();
    } // isDebugEnabled().

    /*
     * Test to see if method entry tracing is enabled.
     * @returns true is method entry tracing is enabled.
     */
    public final boolean isEntryEnabled()
    {
        if (usePrintWriterForTrace) return true;
        return traceComponent.isEntryEnabled();
    } // isEntryEnabled().

    /*
     * Test to see if event tracing is enabled.
     * @returns true is method entry tracing is enabled.
     */
    public final boolean isEventEnabled()
    {
        if (usePrintWriterForTrace) return true;
        return traceComponent.isEventEnabled();
    } // isEventEnabled().

    /*
     * Test to see if any tracing is enabled.
     * @returns true if any tracing is enabled.
     */
    public final boolean isAnyTracingEnabled()
    {
      if (usePrintWriterForTrace) return true;
      return TraceComponent.isAnyTracingEnabled();
    } // isAnyTracingEnabled().

    /**
     * Byte data trace for static classes.
     *
     * @param sourceClass
     * @param data
     */
    public void bytes(Class sourceClass, byte[] data)
    {
        internalBytes(null, sourceClass, data, 0, 0);
    } // bytes().

    /**
     * Byte data trace for static Objects.
     *
     * @param sourceClass
     * @param data
     * @param start
     */
    public void bytes(Class sourceClass, byte[] data, int start)
    {
        internalBytes(null, sourceClass, data, start, 0);
    } // bytes().

    /**
     * Byte data trace for static Objects.
     *
     * @param sourceClass
     * @param data
     * @param start
     * @param count
     */
    public void bytes(Class sourceClass, byte[] data, int start, int count)
    {
        internalBytes(null, sourceClass, data, start, count);
    } // bytes().

    /**
     * Byte data trace.
     *
     * @param source
     * @param sourceClass
     * @param data
     */
    public void bytes(Object source, Class sourceClass, byte[] data)
    {
        internalBytes(source, sourceClass, data, 0, 0);
    } // bytes().

    /**
     * Byte data trace for static Objects.
     *
     * @param source
     * @param sourceClass
     * @param data
     * @param start
     */
    public void bytes(Object source, Class sourceClass, byte[] data, int start)
    {
        internalBytes(source, sourceClass, data, start, 0);
    } // bytes().

    /**
     * Byte data trace.
     *
     * @param source
     * @param sourceClass
     * @param data
     * @param start
     * @param count
     */
    public void bytes(Object source, Class sourceClass, byte[] data, int start, int count)
    {
        internalBytes(source, sourceClass, data, start, count);
    }

    /**
     * Internal implementation of byte data trace.
     *
     * @param source
     * @param sourceClass
     * @param data
     * @param start
     * @param count
     */
    private void internalBytes(Object source, Class sourceClass, byte[] data, int start, int count)
    {
        StringBuffer stringBuffer = new StringBuffer();

        stringBuffer.append(sourceClass.getName());
        stringBuffer.append(" [");

        if (source != null)
        {
            stringBuffer.append(source);
        }
        else
        {
            stringBuffer.append("Static");
        }

        stringBuffer.append("]");
        stringBuffer.append(ls);

        if (data != null)
        {
            if (count > 0)
            {
                stringBuffer.append(formatBytes(data, start, count, true));
            }
            else
            {
                stringBuffer.append(formatBytes(data, start, data.length, true));
            }
        }
        else
        {
            stringBuffer.append("data is null");
        }

        Tr.debug(traceComponent, stringBuffer.toString());

        if (usePrintWriterForTrace)
        {
            if (printWriter != null)
            {
                printWriter.print(new java.util.Date()+" B ");
                printWriter.println(stringBuffer.toString());
                printWriter.flush();
            }
        }
    } // bytes().

    /**
     * Method debug tracing for static classes.
     *
     * @param sourceClass
     * @param message
     */
    public final void debug(Class sourceClass, String message)
    {
        internalDebug(null, sourceClass, message, null);
    } // debug().

    /**
     * Method debug tracing for static classes.
     *
     * @param sourceClass
     * @param message
     * @param object
     */
    public final void debug(Class sourceClass, String message, Object object)
    {
        internalDebug(null, sourceClass, message, object);
    } // debug().

    /**
     * Method debug tracing for static classes.
     *
     * @param sourceClass
     * @param message
     * @param objects
     */
    public final void debug(Class sourceClass, String message, Object[] objects)
    {
        internalDebug(null, sourceClass, message, objects);
    } // debug().

    /**
     * Method debug tracing.
     *
     * @param source
     * @param sourceClass
     * @param message
     */
    public final void debug(Object source, Class sourceClass, String message)
    {
        internalDebug(source, sourceClass, message, null);
    } // debug().

    /**
     * Method debug tracing.
     *
     * @param source
     * @param sourceClass
     * @param message
     * @param object
     */
    public final void debug(Object source, Class sourceClass, String message, Object object)
    {
        internalDebug(source, sourceClass, message, object);
    }

    /**
     * Method debug tracing.
     *
     * @param source
     * @param sourceClass
     * @param methodName
     * @param objects
     */
    public final void debug(Object source, Class sourceClass, String message, Object[] objects)
    {
        internalDebug(source, sourceClass, message, objects);
    }

    /**
     * Internal implementation of method debug tracing.
     *
     * @param source
     * @param sourceClass
     * @param message
     * @param object
     */
    private final void internalDebug(Object source, Class sourceClass, String message, Object object)
    {
        StringBuffer stringBuffer = new StringBuffer();

        stringBuffer.append(message);
        stringBuffer.append(" [");

        if (source != null)
        {
            stringBuffer.append(source);
        }
        else
        {
            stringBuffer.append("Static");
        }

        stringBuffer.append("]");

        if (object != null)
        {
            Tr.debug(traceComponent, stringBuffer.toString(), object);
        }
        else
        {
            Tr.debug(traceComponent, stringBuffer.toString());
        }

        if (usePrintWriterForTrace)
        {
            if (printWriter != null)
            {
                printWriter.print(new java.util.Date()+" D ");
                printWriter.print(sourceClass.getName());
                printWriter.print(".");
                printWriter.println(stringBuffer.toString());

                if (object != null)
                {
                    if (object instanceof Object[])
                    {
                        Object[] objects = (Object[])object;
                        for (int i=0;i<objects.length;i++)
                        {
                            printWriter.println("\t\t"+objects[i]);
                        }
                    }
                    else
                    {
                        printWriter.println("\t\t"+object);
                    }
                }

                printWriter.flush();
            }
        }
    } // internalDebug().

    /**
     * Method entry tracing for static classes.
     *
     * @param sourceClass
     *                   The type of class which called this method
     * @param methodName The method name to trace
     */
    public final void entry(Class sourceClass, String methodName)
    {
        internalEntry(null, sourceClass, methodName, null);
    } // entry().

    /**
     * Method entry tracing for static classes.
     *
     * @param sourceClass
     *                   The type of class which called this method
     * @param methodName The method name to trace
     * @param object    An object to trace with the entry point i.e. method parameter
     */
    public final void entry(Class sourceClass, String methodName, Object object)
    {
        internalEntry(null, sourceClass, methodName, object);
    } // entry().

    /**
     * Method entry tracing for static classes.
     *
     * @param sourceClass
     *                   The type of class which called this method
     * @param methodName The method name to trace
     * @param objects    An array of objects to trace with the entry point i.e. method parameters
     */
    public final void entry(Class sourceClass, String methodName, Object[] objects)
    {
        internalEntry(null, sourceClass, methodName, objects);
    } // entry().

    /**
     * Method entry tracing.
     *
     * @param source     The class instance which called this method
     * @param sourceClass
     *                   The type of class which called this method
     * @param methodName The method name to trace
     */
    public final void entry(Object source, Class sourceClass, String methodName)
    {
        entry(source, sourceClass, methodName, null);
    } // entry().

    /**
     * Method entry tracing.
     *
     * @param source     The class instance which called this method
     * @param sourceClass
     *                   The type of class which called this method
     * @param methodName The method name to trace
     * @param objects    An object to trace with the entry point i.e. method parameter
     */
    public final void entry(Object source, Class sourceClass, String methodName, Object object)
    {
        internalEntry(source, sourceClass, methodName, object);
    }

    /**
     * Method entry tracing.
     *
     * @param source     The class instance which called this method
     * @param sourceClass
     *                   The type of class which called this method
     * @param methodName The method name to trace
     * @param objects    An array of objects to trace with the entry point i.e. method parameters
     */
    public final void entry(Object source, Class sourceClass, String methodName, Object[] objects)
    {
        internalEntry(source, sourceClass, methodName, objects);
    } // entry().

    /**
     * Internal implementation of method entry tracing.
     *
     * @param source     The class instance which called this method
     * @param sourceClass
     *                   The type of class which called this method
     * @param methodName The method name to trace
     * @param object     An object to trace with the entry point i.e. method parameter
     */
    private final void internalEntry(Object source, Class sourceClass, String methodName, Object object)
    {
        StringBuffer stringBuffer = new StringBuffer();

        stringBuffer.append(methodName);
        stringBuffer.append(" [");

        if (source != null)
        {
            stringBuffer.append(source);
        }
        else
        {
            stringBuffer.append("Static");
        }

        stringBuffer.append("]");

        if (object != null)
        {
            Tr.entry(traceComponent, stringBuffer.toString(), object);
        }
        else
        {
            Tr.entry(traceComponent, stringBuffer.toString());
        }

        if (usePrintWriterForTrace)
        {
            if (printWriter != null)
            {
                printWriter.print(new java.util.Date()+" > ");
                printWriter.print(sourceClass.getName());
                printWriter.print(".");
                printWriter.println(stringBuffer.toString());

                if (object != null)
                {
                    if (object instanceof Object[])
                    {
                        Object[] objects = (Object[])object;
                        for (int i=0;i<objects.length;i++)
                        {
                            printWriter.println("\t\t"+objects[i]);
                        }
                    }
                    else
                    {
                        printWriter.println("\t\t"+object);
                    }
                }

                printWriter.flush();
            }
        }
    } // internalEntry().

    /**
     * Method exit tracing for static methods.
     *
     * @param sourceClass
     *                   The type of class which called this method
     * @param methodName The method name to trace
     */
    public final void exit(Class sourceClass, String methodName)
    {
        internalExit(null, sourceClass, methodName, null);
    } // exit().

    /**
     * Method exit tracing for static methods.
     *
     * @param sourceClass
     *                   The type of class which called this method
     * @param methodName The method name to trace
     * @param object     An object to trace with the exit point i.e. method parameter
     */
    public final void exit(Class sourceClass, String methodName, Object object)
    {
        internalExit(null, sourceClass, methodName, object);
    } // exit().

    /**
     * Method exit tracing for static methods.
     *
     * @param sourceClass
     *                   The type of class which called this method
     * @param methodName The method name to trace
     * @param objects    An array of objects to trace with the exit point i.e. method parameters
     */
    public final void exit(Class sourceClass, String methodName, Object[] objects)
    {
        internalExit(null, sourceClass, methodName, objects);
    } // exit().

    /**
     * Method exit tracing.
     *
     * @param source     The class instance which called this method
     * @param sourceClass
     *                   The type of class which called this method
     * @param methodName The method name to trace
     */
    public final void exit(Object source, Class sourceClass, String methodName)
    {
        internalExit(source, sourceClass, methodName, null);
    } // exit().

    /**
     * Method exit tracing.
     *
     * @param source     The class instance which called this method
     * @param sourceClass
     *                   The type of class which called this method
     * @param methodName The method name to trace
     * @param object     An object to trace with the exit point i.e. method parameter
     */
    public final void exit(Object source, Class sourceClass, String methodName, Object object)
    {
        internalExit(source, sourceClass, methodName, object);
    } // exit().

    /**
     * Method exit tracing.
     *
     * @param source     The class instance which called this method
     * @param sourceClass
     *                   The type of class which called this method
     * @param methodName The method name to trace
     * @param objects    An array of objects to trace with the exit point i.e. method parameters
     */
    public final void exit(Object source, Class sourceClass, String methodName, Object[] objects)
    {
        internalExit(source, sourceClass, methodName, objects);
    } // exit().

    /**
     * Internal implementation of method exit tracing.
     *
     * @param source     The class instance which called this method
     * @param sourceClass
     *                   The type of class which called this method
     * @param methodName The method name to trace
     * @param object     An object to trace with the exit point i.e. method parameter
     */
    private final void internalExit(Object source, Class sourceClass, String methodName, Object object)
    {
        StringBuffer stringBuffer = new StringBuffer();

        stringBuffer.append(methodName);
        stringBuffer.append(" [");

        if (source != null)
        {
            stringBuffer.append(source);
        }
        else
        {
            stringBuffer.append("Static");
        }

        stringBuffer.append("]");

        if (object != null)
        {
            Tr.exit(traceComponent, stringBuffer.toString(), object);
        }
        else
        {
            Tr.exit(traceComponent, stringBuffer.toString());
        }

        if (usePrintWriterForTrace)
        {
            if (printWriter != null)
            {
                printWriter.print(new java.util.Date()+" < ");
                printWriter.print(sourceClass.getName());
                printWriter.print(".");
                printWriter.println(stringBuffer.toString());

                if (object != null)
                {
                    if (object instanceof Object[])
                    {
                        Object[] objects = (Object[])object;
                        for (int i=0;i<objects.length;i++)
                        {
                            printWriter.println("\t\t"+objects[i]);
                        }
                    }
                    else
                    {
                        printWriter.println("\t\t"+object);
                    }
                }

                printWriter.flush();
            }
        }
    } // internalExit()

    /**
     * Event tracing when a throwable is caught in a static class.
     *
     * @param sourceClass
     *                   The type of class which called this method
     * @param methodName The method where the event took place
     * @param throwable  The exception to trace
     */
    public final void event(Class sourceClass, String methodName, Throwable throwable)
    {
        internalEvent(null, sourceClass ,methodName, throwable);
    } // event().

    /**
     * Event tracing.
     *
     * @param source     The class instance which called this method
     * @param sourceClass
     *                   The type of class which called this method
     * @param methodName The method where the event took place
     * @param throwable  The exception to trace
     */
    public final void event(Object source, Class sourceClass, String methodName, Throwable throwable)
    {
        internalEvent(source, sourceClass ,methodName, throwable);
    } // event().

    /**
     * Internal implementation of event tracing.
     *
     * @param source     The class instance which called this method
     * @param sourceClass
     *                   The type of class which called this method
     * @param methodName The method where the event took place
     * @param throwable  The exception to trace
     */
    private final void internalEvent(Object source, Class sourceClass, String methodName, Throwable throwable)
    {
        StringBuffer stringBuffer = new StringBuffer();

        stringBuffer.append(methodName);
        stringBuffer.append(" [");

        if (source != null)
        {
            stringBuffer.append(source);
        }
        else
        {
            stringBuffer.append("Static");
        }

        stringBuffer.append("]");

        if (throwable != null)
        {
            Tr.event(traceComponent, stringBuffer.toString(), new Object[] {"Exception caught: ", throwable});
        }
        else
        {
            Tr.event(traceComponent, stringBuffer.toString());
        }

        if (usePrintWriterForTrace)
        {
            if (printWriter != null)
            {
                printWriter.print(new java.util.Date()+" E ");
                printWriter.print(sourceClass.getName());
                printWriter.print(".");
                printWriter.println(stringBuffer.toString());

                if (throwable != null)
                {
                    throwable.printStackTrace(printWriter);
                }

                printWriter.flush();
            }
        }
    } // internalEvent()

    /**
     * Method information tracing for static objects.
     *
     * @param Class of the static object making the trace call.
     * @param String name of the method being entered.
     * @param String identifying the message.
     * @param Object to be inserted into the message.
     */
    public final void info(Class sourceClass, String methodName, String messageIdentifier, Object object)
    {
        internalInfo(null, sourceClass ,methodName, messageIdentifier, object);
    } // entry().

    /**
     * Method information tracing for static objects.
     *
     * @param Class of the static object making the trace call.
     * @param String name of the method being entered.
     * @param String identifying the message.
     * @param Object[] containing inserts into the message.
     */
    public final void info(Class sourceClass, String methodName, String messageIdentifier, Object[] objects)
    {
        internalInfo(null, sourceClass ,methodName, messageIdentifier, objects);
    } // info().

    /**
     * Method information tracing.
     *
     * @param Object making the trace call.
     * @param Class of Object making the trace call.
     * @param String name of the method being entered.
     * @param String identifying the message.
     * @param Object to be inserted into the message.
     */
    public final void info(Object source, Class sourceClass, String methodName, String messageIdentifier, Object object)
    {
        internalInfo(source, sourceClass ,methodName, messageIdentifier, object);
    } // info().

    /**
     * Method information tracing.
     *
     * @param Object making the trace call.
     * @param Class of Object making the trace call.
     * @param String name of the method being entered.
     * @param String identifying the message.
     * @param Object[] containing inserts into the message.
     */
    public final void info(Object source, Class sourceClass, String methodName, String messageIdentifier, Object[] objects)
    {
        internalInfo(source, sourceClass ,methodName, messageIdentifier, objects);
    } // info().

    /**
     * Internal implementation of info NLS message tracing.
     *
     * @param source
     * @param sourceClass
     * @param methodName
     * @param messageIdentifier
     * @param object
     */
    private final void internalInfo(Object source, Class sourceClass, String methodName, String messageIdentifier, Object object)
    {
        if (usePrintWriterForTrace)
        {
            if (printWriter != null)
            {
                StringBuffer stringBuffer = new StringBuffer(new java.util.Date().toString());

                stringBuffer.append(" I ");
                stringBuffer.append(sourceClass.getName());
                stringBuffer.append(".");
                stringBuffer.append(methodName);


                printWriter.println(stringBuffer.toString());
                printWriter.println("\t\t"+NLS.format(messageIdentifier, object));
                printWriter.flush();
            }
        }

        if (object != null)
        {
            Tr.info(traceComponent, messageIdentifier, object);
        }
        else
        {
            Tr.info(traceComponent, messageIdentifier);
        }
    }

    /*
     * Byte array formatting methods
     */
    private final static String ls = System.lineSeparator();
    private final static String DEAD_CHAR = ".";

    private static String pad (String s, int l)
    {
        return pad(s,l, null);
    }

    private static String pad (String s, int l, String p)
    {
        String rc;

        if (p == null) p = "0";

        if (s.length() < l)
        {
            StringBuffer sb = new StringBuffer();
            for (int i=0; i < l - s.length(); i++) sb.append(p);
            rc = sb.toString()+s;
        }
        else rc = s.substring(s.length()-l);

        return rc;
    }

    private static String dup (int i)
    {
        return "                          " + i + " duplicate line(s) suppressed"+ls;
    }

    /**
     * Produce a formatted view of a byte array.  Duplicate output lines are
     * suppressed to save space.
     * Formatting of the byte array starts at the specified position and continues
     * for the specified number of bytes or the end of the data.
     * The bytes will also be converted to into it's String equivalent using the
     * platform default character set. This will be displayed if the
     * <code>displayCharRepresentation</code> flag is set to true.
     * <p>
     * @param data the byte array to be formatted
     * @param start position to start formatting the byte array
     * @param count of bytes from start position that should be formatted
     * @param displayCharRepresentations Whether to display the character representation
     * @return the formatted byte array
     */

    public static String formatBytes (byte[] data, int start, int count, boolean displayCharRepresentations)
    {
        StringBuffer sb = new StringBuffer();

        if (data != null)
        {
            int len = data.length;
            sb.append(ls+"Length = 0x"+Integer.toHexString(len)+" ("+len+") bytes start="+start+" count="+count+ls+ls);
            if (displayCharRepresentations)
                sb.append("        offset        : 0 1 2 3  4 5 6 7  8 9 A B  C D E F     0 2 4 6 8 A C E " + ls);
            else
                sb.append("        offset        : 0 1 2 3  4 5 6 7  8 9 A B  C D E F" + ls);

            int t;
            boolean skip;
            int suppress = 0;
            int end = start + count;
            String c[] = new String[16];  // Current line bytes
            String p[] = new String[16];  // Previous line bytes
            String str[] = new String[16];// The string representation

            for (int j=0; j < 16; j++)
            {
                c[j] = null;
                str[j] = null;
            }

            for (int i=0; i < len; i = i+16)
            {
                skip = true;

                for (int j=0; j < 16; j++)
                {
                    t = i +  j;
                    if ((t >= start) && (t < end) && (t < len))
                    {
                        c[j] = pad(Integer.toHexString(data[t]),2);
                        // Strip out some known 'bad-guys' (these are consistent across ASCII / EBCIDIC)
                        // and replace them with the dead character
                        if (c[j].equalsIgnoreCase("00") ||      // Null
                            c[j].equalsIgnoreCase("09") ||      // Tab
                            c[j].equalsIgnoreCase("0a") ||      // LF
                            c[j].equalsIgnoreCase("0b") ||      // VertTab
                            c[j].equalsIgnoreCase("0c") ||      // FF
                            c[j].equalsIgnoreCase("0d") ||      // CR
                            c[j].equalsIgnoreCase("07"))        // Bell
                        {
                            str[j] = DEAD_CHAR;
                        }
                        else
                        {
                            str[j] = new String(data, t, 1);     // Conversion is done here using the default
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
                    c[0] = null;              // Force a line difference
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
                        if (suppress > 0) sb.append(dup(suppress));
                        sb.append("0x"+pad(Integer.toHexString(i),8)+" ("+pad(Integer.toString(i),8," ")+") : ");
                        sb.append(c[0]+c[1]+c[2]+c[3]+" "+c[4]+c[5]+c[6]+c[7]+" "+c[8]+c[9]+c[10]+c[11]+" "+c[12]+c[13]+c[14]+c[15]);
                        if (displayCharRepresentations)
                        {
                            sb.append("  | ");
                            sb.append(str[0]+str[1]+str[2]+str[3]+str[4]+str[5]+str[6]+str[7]+str[8]+str[9]+str[10]+str[11]+str[12]+str[13]+str[14]+str[15]);
                        }
                        sb.append(ls);
                        for (int j=0; j < 16; j++) p[j] = c[j];
                        suppress = 0;
                    }
                }
            }

            if (suppress > 0) sb.append(dup(suppress));
        }

        return sb.toString();
    }

} // Of class Trace.
