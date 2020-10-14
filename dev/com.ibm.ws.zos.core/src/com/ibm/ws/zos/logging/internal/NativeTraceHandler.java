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
package com.ibm.ws.zos.logging.internal;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.TraceComponentChangeListener;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.zos.core.utils.DirectBufferHelper;
import com.ibm.ws.zos.core.utils.internal.DoubleGutterImpl;
import com.ibm.ws.zos.jni.NativeMethodManager;

/**
 * This class is responsible for transforming native <code>TraceRecord</code>s
 * into something the logging component can consume.
 */
@Trivial
public class NativeTraceHandler extends DoubleGutterImpl implements TraceComponentChangeListener {

    /**
     * TraceComponent for this class.
     */
    private final static TraceComponent tc = Tr.register(NativeTraceHandler.class);

    /**
     * The maximum size of the native C stack. No variable argument list
     * can exceed this size.
     */
    private final static int MAX_STACK_SIZE = 4 * 1024 * 1024;

    /**
     * The name of the native trace specification prefix
     *
     */
    private final static String ZOS_NATIVE_PREFIX = "zos.native";

    /**
     * The name of the native trace high-level default trace group
     *
     */
    private final static String ZOS_NATIVE_TRACE_GROUP = "zNative";

    /**
     * The name of the native traces trace specification prefix.
     * This is used to emit trace records for the trace code.
     *
     */
    private final static String ZOS_TRACE_PREFIX = "zos.trace";

    /**
     * The name of the native trace high-level default trace group
     *
     */
    private final static String ZOS_TRACE_GROUP = "zTrace";

    /**
     * The component number reserved for the native tracing implementation
     *
     */
    private final static String ZOS_TRACE_COMPONENT = "06";

    /**
     * The aggregate highest trace level of all active zos.native trace specifications.
     * This level needs to match what is in server_tracing_functions.c and should not
     * be changed.
     *
     */
    private static int AGGREGATE_NATIVE_TRACE_LEVEL = 3;

    /**
     * Indicator if this is being driven in an environment that supports native code.
     */
    private boolean nativeAvailable = true;

    /**
     * Native Trace Keys supported
     */
    static final int TRC_KEY_RAW_DATA = 1;
    static final int TRC_KEY_EBCDIC_STRING = 2;
    static final int TRC_KEY_INT = 3;
    static final int TRC_KEY_DOUBLE = 4;
    static final int TRC_KEY_POINTER = 5;
    static final int TRC_KEY_LONG = 6;
    static final int TRC_KEY_SHORT = 7;
    static final int TRC_KEY_CHAR = 8;
    static final int TRC_KEY_HEX_INT = 9;
    static final int TRC_KEY_HEX_LONG = 10;

    /**
     * Native trace levels
     */
    private static final int TRC_LEVEL_NONE = 0;
    private static final int TRCE_LEVEL_EXCEPTION = 1;
    private static final int TRC_LEVEL_BASIC = 2;
    private static final int TRC_LEVEL_DETAILED = 3;

    /**
     * HashMap of Trace Components registered for the native components, keyed by derived component name
     */
    private static ConcurrentHashMap<String, TraceComponent> traceComponents = new ConcurrentHashMap<String, TraceComponent>();

    /**
     * HashMap of Native Trace Components definitions, keyed by component/module id
     */
    private static ConcurrentHashMap<Integer, NativeTraceComponentDefinition> traceComponentDefs = new ConcurrentHashMap<Integer, NativeTraceComponentDefinition>();

    /**
     * Mock Trace Component Definitions used for the non-native test path only.
     */
    private static NativeTraceComponentDefinition[] testDefs = null;

    /**
     * The instance of the thread that processes native traces.
     */
    private TraceListenerThread thread = null;

    /**
     * The expected EBCDIC encoding of character sequences is native main memory.
     */
    private final Charset ebcdicCharset = Charset.forName("IBM-1047");

    /**
     * Holder for a thread specific {@code CharsetDecoder} to help with
     * conversions from EBCDIC characters to Java unicode-based strings.
     */
    private final ThreadLocal<CharsetDecoder> ebcdicDecoder = new ThreadLocal<CharsetDecoder>() {
        @Override
        @Trivial
        public CharsetDecoder initialValue() {
            return ebcdicCharset.newDecoder();
        }

        @Override
        @Trivial
        public CharsetDecoder get() {
            return super.get().reset();
        }
    };

    /**
     * Used to dynamically obtain the DirectBufferHelper service
     */
    private static volatile ServiceTracker<DirectBufferHelper, DirectBufferHelper> DirectBufferHelperTracker = null;

    /**
     * Returns an instance of the DirectBufferHelper when it becomes activated.
     */
    protected synchronized DirectBufferHelper getDirectBufferInstance() {
        if (DirectBufferHelperTracker == null) {
            Bundle bundle = FrameworkUtil.getBundle(DirectBufferHelper.class);
            if (bundle == null) {
                return null;
            }
            BundleContext bc = bundle.getBundleContext();
            ServiceTracker<DirectBufferHelper, DirectBufferHelper> tmp = new ServiceTracker<DirectBufferHelper, DirectBufferHelper>(bc, DirectBufferHelper.class.getName(), null);
            tmp.open();
            DirectBufferHelperTracker = tmp;
        }
        return DirectBufferHelperTracker.getService();
    }

    /**
     * Create a new native trace handler.
     *
     * @param bufferHelper the buffer management object used to read main memory.
     */
    public NativeTraceHandler(NativeMethodManager nativeMethodManager) {
        Object[] callbackData = new Object[] { this, "writeNativeTrace" };
        nativeMethodManager.registerNatives(NativeTraceHandler.class, callbackData);
        this.initializeTraceDefinitions();
        TrConfigurator.addTraceComponentListener(this);
    }

    /**
     * Package private constructor to facilitate test.
     */
    NativeTraceHandler(DirectBufferHelper bufferHelper, NativeTraceComponentDefinition[] testDefs) {
        //TODO: remove the "bufferHelper" parm and update the fallout in the test package
        //this.bufferHelper = bufferHelper;
        NativeTraceHandler.testDefs = testDefs;
        this.nativeAvailable = false;
        this.initializeTraceDefinitions();
        TrConfigurator.addTraceComponentListener(this);
    }

    /**
     * Start the thread that records trace points written from environments
     * where Java callbacks can not be made.
     */
    public void startTraceHandlerThread() {
        // Create thread for processing trace
        thread = AccessController.doPrivileged(new PrivilegedAction<TraceListenerThread>() {
            @Override
            public TraceListenerThread run() {
                return new TraceListenerThread(NativeTraceHandler.this);
            }
        });

        thread.start();
        return;
    }

    /**
     * Stop the thread that records trace points written from environments
     * where Java callbacks can not be made.
     */
    public void stopTraceHandlerWriter() {
        // Stop thread that processes trace
        thread.end();
        return;
    }

    /**
     * Extract an EBCDIC string from main memory. The code page of the string
     * in memory is expected to be {@code IBM-1047}.
     *
     * @param address the address of the string
     * @param length  the number of bytes in the string
     *
     * @return the Java string that is equivalent to the source EBCDIC string
     */
    String getString(long address, int stringLength) {
        CharsetDecoder decoder = ebcdicDecoder.get();
        DirectBufferHelper bufferHelper = getDirectBufferInstance();
        ByteBuffer bb = bufferHelper.getSlice(address, stringLength);
        String string = null;
        try {
            string = decoder.decode(bb).toString();
        } catch (CharacterCodingException cce) {
            string = "<DECODE ERROR>";
        }
        return string;
    }

    /**
     * Extract an EBCDIC string from the buffer. The buffer's position should
     * point to two 8-byte words. The first should be the length of the string
     * in bytes and the second should be a pointer to the string data.
     *
     * @param bb buffer positioned at two 8 byte words. The first is the
     *               length of the string in bytes and the second word is the
     *               pointer to the string
     */
    String getString(ByteBuffer bb) {
        int length = (int) bb.getLong();
        long address = bb.getLong();

        return getString(address, length);
    }

    /**
     * Return true if we have a reference to the DirectBufferHelper to map
     * native storage.
     */
    boolean readyToMapNative() {
        return getDirectBufferInstance() != null;
    }

    /**
     * Format and write native trace records to Java trace.
     * <p>
     * Each native {@code TraceRecord} consists of an ordered set of trace
     * n-tuples. Each tuple contains five elements:
     * <ol>
     * <li>A trace key (short)
     * <li>The length of the traced data (short)
     * <li>The data to be traced (pointer or primitive)
     * <li>The length of the description (short)
     * <li>A pointer to the null-terminated description string in EBCDIC
     * </ol>
     * Each tuple consists of a set of slots where each slots is 4 byes (31 bit)
     * or 8 bytes (64 bit). This method is written for 64 bit callers.
     *
     * @param nativeTraceLevel an integer containing the native trace level value
     * @param tracePoint       an integer encapsulating the component, module, and tp values
     * @param vaListPointer    a pointer to the variable argument list
     * @param createTime       time the trace was created
     * @param tcbAddress       address of the tcb that created the trace
     * @param state            state the thread was in when the trace was created 1 problem state 0 supervisor state
     * @param key              key the thread was in when the trace was created
     *
     * @return 0 if the trace was successfully written or a non-zero return code
     *         if a failure occurs
     */
    public int writeNativeTrace(int nativeTraceLevel,
                                int tracePoint,
                                long vaListPointer,
                                long createTime,
                                int tcbAddress,
                                int state,
                                int key) {

        if (nativeTraceLevel == 0 || tracePoint == 0 || vaListPointer == 0) {
            // We should never get a null argument list
            return -2;
        }

        String hexString = Integer.toHexString(tracePoint);
        if (hexString.length() == 7) //this will drop a leading zero so account for it
            hexString = "0" + hexString;

        String componentId = hexString.substring(0, 2); //first 2 positions are component
        String moduleId = hexString.substring(2, 5); // position 3  thru 5 are the module
        String tracePointId = hexString.substring(5, 8); // position 6  thru 8 are the trace point
        int tpNum = Integer.parseInt(tracePointId, 16);
        int tracePointKey = (tracePoint - tpNum);

        String tcName = this.mapNativeComponentToNamedLogger(componentId, moduleId);
        TraceComponent tcNative = this.getTraceComponent(tcName, tracePointKey);

        if (tcNative == null) {
            // We should never get a null trace component
            return -3;
        }

        //See if tracing we care about is enabled for this trace component
        boolean isLoggable = tcNative.isDebugEnabled() || tcNative.isEventEnabled() || tcNative.isEntryEnabled();

        if (isLoggable && readyToMapNative()) {
            try {
                Object[] traceData = getTraceObjects(vaListPointer).toArray();
                NativeTraceHeader nativeTraceHeader = new NativeTraceHeader(tracePoint, tcbAddress, key, state, createTime);
                logTrace(nativeTraceLevel, tcNative, nativeTraceHeader.toString(), traceData);
            } catch (Throwable t) {
                return -1;
            }
        }
        return 0;
    }

    /**
     * Extract the data to be traced that's referenced by the variable
     * argument list to TraceWriteV.
     *
     * @param vaListPointer the start of the variable argument list
     */
    List<TracedData> getTraceObjects(long vaListPointer) {

        // Map the variable argument list and set its initial position like va_start
        // does in native
        DirectBufferHelper bufferHelper = getDirectBufferInstance();
        ByteBuffer vaList = bufferHelper.getSlice(vaListPointer, MAX_STACK_SIZE);

        // Build the list of items to trace
        List<TracedData> traceObjects = new ArrayList<TracedData>();
        for (int traceKey = (int) vaList.getLong(); traceKey > 0; traceKey = (int) vaList.getLong()) {
            short itemLength = (short) vaList.getLong();
            long item = vaList.getLong();
            String description = getString(vaList);

            TracedData data = null;
            switch (traceKey) {
                case TRC_KEY_RAW_DATA:
                    byte[] rawData = new byte[item == 0 ? 0 : itemLength];
                    bufferHelper.get(item, rawData);
                    data = new TracedData(traceKey, description, rawData, asDoubleGutter(item, rawData));
                    break;

                case TRC_KEY_INT:
                    Integer i = Integer.valueOf((int) item);
                    data = new TracedData(traceKey, description, i, i.toString());
                    break;

                case TRC_KEY_LONG:
                    Long l = Long.valueOf(item);
                    data = new TracedData(traceKey, description, l, l.toString());
                    break;

                case TRC_KEY_SHORT:
                    Short s = Short.valueOf((short) item);
                    data = new TracedData(traceKey, description, s, s.toString());
                    break;

                case TRC_KEY_DOUBLE:
                    Double d = Double.longBitsToDouble(item);
                    data = new TracedData(traceKey, description, d, d.toString());
                    break;

                case TRC_KEY_EBCDIC_STRING:
                    String string = getString(item, itemLength);
                    data = new TracedData(traceKey, description, string, string);
                    break;

                case TRC_KEY_HEX_INT:
                    data = new TracedData(traceKey, description, Integer.valueOf((int) item), Integer.toHexString((int) item));
                    break;

                case TRC_KEY_HEX_LONG:
                    data = new TracedData(traceKey, description, Long.valueOf(item), Long.toHexString(item));
                    break;

                case TRC_KEY_POINTER:
                    data = new TracedData(traceKey, description, Long.valueOf(item), String.format("%016x", item));
                    break;

                case TRC_KEY_CHAR:
                    char c = new String(new byte[] { (byte) item }, ebcdicCharset).charAt(0);
                    data = new TracedData(traceKey, description, Character.valueOf(c), Character.toString(c));
                    break;

                default:
                    data = new TracedData(traceKey, "", Long.valueOf(item), "<UNKNOWN TRACE KEY>");
                    break;
            }
            traceObjects.add(data);
        }

        return traceObjects;
    }

    /**
     * Based on the native trace level invoke the appropriate Tr method.
     *
     * @param nativeTraceLevel an integer containing the native trace level value
     *                             TraceComponent the trace component servicing this native trace request
     *                             msg a string indicating this is a native trace entry
     *                             traceObjects the list of objects to log to the trace
     *
     */
    private void logTrace(int nativeTraceLevel, TraceComponent tc, String msg, Object[] traceObjects) {

        switch (nativeTraceLevel) {

            case TRC_LEVEL_NONE:
                break;

            case TRCE_LEVEL_EXCEPTION:
            case TRC_LEVEL_BASIC:
                if (tc.isEventEnabled()) {
                    Tr.event(tc, msg, traceObjects);
                }
                break;

            case TRC_LEVEL_DETAILED:
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, msg, traceObjects);
                } else if (tc.isEntryEnabled()) {
                    Tr.entry(tc, msg, traceObjects);
                }
                break;

            default:
                break;
        }
    }

    /**
     * Find a Trace component for the tracepoint requesting the trace.
     *
     * @param numericName, the dotted decimal alphanumeric name derived from the tracepoint
     *                         of the module requesting the trace record.
     *
     * @return A trace component instance for this tracepoint
     */
    private TraceComponent getTraceComponent(String numericName, int tracepoint) {

        //So here is the deal. The trace specification can be entered in two different
        //formats.  Why? I guess because we can! The formats are 1) zos.native.<component #>.<module #>
        //and 2) zos.native.<component name>.<module name>.  This code will first get a trace component
        //using the "numeric" format.  If any tracing is enabled then that is the TraceComponent that
        //will be used.  If no tracing is enabled for the numeric format then the non-numeric string
        //format will be used. In a collision where both formats are enabled the numeric format will
        //take precedence.

        TraceComponent tcNative = null;
        NativeTraceComponentDefinition def = null;
        String name = null;

        tcNative = NativeTraceHandler.traceComponents.get(numericName);

        //If this is null then there is a problem, we arn't going to bother checking
        //using the non-numeric string.
        if (tcNative == null)
            return null;

        if (tcNative.isDebugEnabled() || tcNative.isEventEnabled() || tcNative.isEntryEnabled())
            return tcNative;
        else {
            def = NativeTraceHandler.traceComponentDefs.get(tracepoint);
            if (def == null)
                return null;
            name = def.getName();
            if (name == null)
                return null;
            tcNative = NativeTraceHandler.traceComponents.get(name);
        }

        return tcNative;
    }

    /**
     * Map the native component id to a named logger.
     *
     * @param componentId, the native component id
     *
     * @return the name of the logger servicing this native component
     */
    private String mapNativeComponentToNamedLogger(String componentId, String moduleId) {

        String loggerName = null;
        String prefix = null;

        // The native trace implementation contains tracepoints. They are assigned
        // to a unique trace prefix to separate out these trace points from
        // the non trace related tracepoints to avoid duplication of trace records
        // in the log. So if you desire to see the trace records produced from the
        // trace code you need to enable them specifically using the ZOS_TRACE_PREFIX
        // in the trace.specification.

        if (componentId.equalsIgnoreCase(ZOS_TRACE_COMPONENT))
            prefix = ZOS_TRACE_PREFIX;
        else
            prefix = ZOS_NATIVE_PREFIX;

        if (moduleId.equalsIgnoreCase("000"))
            loggerName = prefix + "." + componentId;
        else
            loggerName = prefix + "." + componentId + "." + moduleId;
        return loggerName;
    }

    /**
     * Add each native trace definition to the Hashmap and register the trace component
     */
    private void initializeTraceDefinitions() {

        NativeTraceHandler.traceComponentDefs.clear();
        NativeTraceHandler.traceComponents.clear();

        NativeTraceComponentDefinition[] defs = null;

        if (nativeAvailable) {
            defs = ntv_getNativeTraceComponents();
            if (defs == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "initializeTraceDefinitions, Native trace components are missing");
                }
                return;
            }
        } else {
            //If we are running in an environment that doesn't support native code then
            //we may be given some mock Trace component definition entries.
            if (testDefs == null)
                return;
            else
                defs = testDefs;
        }

        String componentId;
        String moduleId;
        String loggerName;
        TraceComponent nativeTc;

        loggerName = ZOS_NATIVE_PREFIX;
        nativeTc = Tr.register(loggerName, (Class<?>) null, ZOS_NATIVE_TRACE_GROUP);
        NativeTraceHandler.traceComponents.put(loggerName, nativeTc);

        loggerName = ZOS_TRACE_PREFIX;
        nativeTc = Tr.register(loggerName, (Class<?>) null, ZOS_TRACE_GROUP);
        NativeTraceHandler.traceComponents.put(loggerName, nativeTc);

        for (NativeTraceComponentDefinition def : defs) {

            NativeTraceHandler.traceComponentDefs.put(def.getId(), def);

            //derive the named logger to be used for this trace definition
            String hexString = Integer.toHexString(def.getId());
            if (hexString.length() == 7) //this will drop a leading zero so account for it
                hexString = "0" + hexString;
            componentId = hexString.substring(0, 2); //first 2 positions are component
            moduleId = hexString.substring(2, 5); //position 3  thru 5 are the module
            loggerName = this.mapNativeComponentToNamedLogger(componentId, moduleId);
            String[] groups = null;
            if (!def.getGroups().isEmpty())
                groups = def.getGroups().toArray(new String[def.getGroups().size()]);
            //register using the numeric format
            nativeTc = Tr.register(loggerName, (Class<?>) null, groups);
            NativeTraceHandler.traceComponents.put(loggerName, nativeTc);
            //register using the non-numeric name
            if (def.getName().length() > 0) {
                nativeTc = Tr.register(def.getName(), (Class<?>) null, groups);
                NativeTraceHandler.traceComponents.put(def.getName(), nativeTc);
            }
        }

        this.updateAggregateTraceLevel();
    }

    /** {@inheritDoc} */
    @Override
    public void traceComponentRegistered(TraceComponent tc) {

        // This callback is redundant since we aggressively register the
        // trace components and then drive updateAggregateTraceLevel()
        // immediately afterward.

    }

    /** {@inheritDoc} */
    @Override
    public void traceComponentUpdated(TraceComponent tc) {
        this.updateAggregateTraceLevel();
    }

    /**
     * Update the aggregate native trace level
     *
     * @param TraceComponent
     */
    private void updateAggregateTraceLevel() {

        if (traceComponents.isEmpty()) {
            return;
        }

        int highestNativeTraceLevel = 0;
        Enumeration<TraceComponent> tcs = traceComponents.elements();

        while (tcs.hasMoreElements()) {
            TraceComponent nativeTc = tcs.nextElement();
            int nativeTraceLevel = getNativeTraceLevel(nativeTc);
            if (nativeTraceLevel > highestNativeTraceLevel)
                highestNativeTraceLevel = nativeTraceLevel;
        }

        if (highestNativeTraceLevel != AGGREGATE_NATIVE_TRACE_LEVEL) {
            AGGREGATE_NATIVE_TRACE_LEVEL = highestNativeTraceLevel;
            if (nativeAvailable)
                this.ntv_setTraceLevel(AGGREGATE_NATIVE_TRACE_LEVEL);
        }
    }

    /**
     * Get the highest enabled trace level for a given trace component
     *
     * @param TraceComponent
     *
     * @return int the active native trace level
     */
    private int getNativeTraceLevel(TraceComponent tcNative) {

        int nativeTraceLevel = 0;

        if (tcNative.isDebugEnabled())
            nativeTraceLevel = 3;
        else if (tcNative.isEventEnabled())
            nativeTraceLevel = 2;
        else if (tcNative.isEntryEnabled())
            nativeTraceLevel = 3;

        return nativeTraceLevel;
    }

    /**
     * Inner class to store a native Trace component definition
     *
     */
    static class NativeTraceComponentDefinition {

        private int id = 0;
        private String name = "";
        private ArrayList<String> groups;

        NativeTraceComponentDefinition(int id, String name, String groups) {
            this.setId(id);
            this.setName(name);
            this.setGroups(groups);
        };

        private void setId(int id) {
            this.id = id;
        }

        private void setName(String name) {
            if (name != null)
                this.name = name;
        }

        private void setGroups(String nativeGroups) {

            this.groups = new ArrayList<String>();

            if (nativeGroups == null)
                return;

            StringTokenizer st = new StringTokenizer(nativeGroups, ",");
            while (st.hasMoreElements()) {
                String aGroup = (String) st.nextElement();
                this.groups.add(aGroup);
            }
        }

        protected int getId() {
            return this.id;
        }

        protected String getName() {
            return this.name;
        }

        protected ArrayList<String> getGroups() {
            return this.groups;
        }

    }

    protected native long ntv_getThreadData();

    protected native byte[] ntv_getTraces(long threadElementPtr);

    protected native int ntv_stopListeningForTraces(long threadElementPtr);

    protected native int ntv_traceWritten(long threadElementPtr);

    protected native void ntv_setTraceLevel(int nativeTraceLevel);

    protected native NativeTraceComponentDefinition[] ntv_getNativeTraceComponents();
}