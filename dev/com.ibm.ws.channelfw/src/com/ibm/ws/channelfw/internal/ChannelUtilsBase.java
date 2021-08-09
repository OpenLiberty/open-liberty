/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channelfw.internal;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChainGroupData;
import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.wsspi.channelfw.ChannelFramework;

/**
 * Do not use this class directly - use ChannelUtils, or ChannelUtilsLogger
 * instead.
 * 
 * <p>
 * Utility methods (primarily for Debug) for use by Channels.
 * <ul>
 * <li>printDebugStackTrace - for pretty-printing a debug stack trace
 * </ul>
 * 
 * <p>
 * Utility methods for displaying Channel and Channel Chain configuration
 * <ul>
 * <li>displayChannels - displays all configured channels
 * <li>displayChains - several variants with different parameters for tracing
 * configured channel chains.
 * </ul>
 * 
 */
abstract public class ChannelUtilsBase {
    final static String EOLN = System.getProperty("line.separator");

    /**
     * Display configured channel chains.
     * 
     * @param logTool
     *            Caller's LogTool (should be Logger OR TraceComponent)
     * @param cfw
     *            Reference to channel framework service
     * @param factory
     *            Factory class that chains to be traced are associated with
     *            (e.g. ORBInboundChannelFactory.. )
     * @param message
     *            Description to accompany trace, e.g. "CFW Channel
     *            Configuration"
     * @param prefix
     *            Component-type prefix used to associate traces together, e.g.
     *            "XMEM", "ZIOP", "TCP", "SOAP"
     */
    protected final void traceChains(Object logTool, ChannelFramework cfw, Class<?> factory, String message, String prefix) {
        ChainData[] chains = null;
        String fstring = "(" + (factory == null ? "no factory specified" : factory.getName()) + ")";

        if (cfw == null) {
            debugTrace(logTool, prefix + " - No cfw to test factory " + fstring);
            return;
        }

        try {
            if (factory != null)
                chains = cfw.getAllChains(factory);
            else
                chains = cfw.getAllChains();
        } catch (Exception e) {
            debugTrace(logTool, "Caught Exception while trying to display configured chains: ", e);
            return;
        }

        if (chains == null || chains.length <= 0)
            debugTrace(logTool, prefix + " - No chains found for factory " + fstring);
        else
            traceChains(logTool, Arrays.asList(chains), message, prefix);
    }

    /**
     * Display configured channel chains.
     * 
     * @param logTool
     *            Caller's LogTool (should be Logger OR TraceComponent)
     * @param cfw
     *            Reference to channel framework service
     * @param groupName
     *            Name of chain group to display (e.g. WebContainer.. )
     * @param message
     *            Description to accompany trace, e.g. "CFW Channel
     *            Configuration"
     * @param prefix
     *            Component-type prefix used to associate traces together, e.g.
     *            "XMEM", "ZIOP", "TCP", "SOAP"
     */
    protected final void traceChains(Object logTool, ChannelFramework cfw, String groupName, String message, String prefix) {
        String gstring = "(" + (groupName == null ? "no group specified" : groupName) + ")";

        if (cfw == null)
            debugTrace(logTool, prefix + " - No cfw to test group " + gstring);
        else if (groupName == null)
            debugTrace(logTool, prefix + " - No group specified");

        if (cfw == null || groupName == null)
            return;

        ChainGroupData chainGroup = null;
        ChainData[] chains = null;

        try {
            chainGroup = cfw.getChainGroup(groupName);
            if (chainGroup != null)
                chains = chainGroup.getChains();
        } catch (Exception e) {
            debugTrace(logTool, "Caught Exception while trying to display configured chains: ", e);
            return;
        }

        if (chains == null || chains.length <= 0)
            debugTrace(logTool, prefix + " - No chains defined for group " + gstring);
        else
            traceChains(logTool, Arrays.asList(chains), message, prefix);
    }

    /**
     * Display configured channel chains.
     * 
     * @param logTool
     *            Caller's LogTool (should be Logger OR TraceComponent)
     * @param lchain
     *            List of chains to trace
     * @param message
     *            Description to accompany trace, e.g. "CFW Channel
     *            Configuration"
     * @param prefix
     *            Component-type prefix used to associate traces together, e.g.
     *            "XMEM", "ZIOP", "TCP", "SOAP"
     */
    protected final void traceChains(Object logTool, List<?> lchain, String message, String prefix) {
        if (lchain == null || lchain.size() <= 0) {
            debugTrace(logTool, prefix + " - Specified chain list is empty");
            return;
        }

        ChainData chain;
        ChannelData channel;
        List<?> lchannel;
        Iterator<?> ichannel, ichain;

        // New StringBuilder for trace of chains in list
        StringBuilder traceMessage = new StringBuilder(300);

        // Prepend message passed as argument to our output
        traceMessage.append(message);
        traceMessage.append(EOLN);

        // Iterate chain
        for (ichain = lchain.iterator(); ichain.hasNext();) {
            chain = (ChainData) ichain.next();

            traceMessage.append(prefix + "-chain ");
            traceMessage.append(chain.getName());
            traceMessage.append(((chain.getType().equals(FlowType.OUTBOUND)) ? " - OUTBOUND" : " - INBOUND"));
            traceMessage.append(EOLN); // end line within
            // buffer

            // Display channel members
            lchannel = Arrays.asList(chain.getChannelList());
            for (ichannel = lchannel.iterator(); ichannel.hasNext();) {
                channel = (ChannelData) ichannel.next();
                traceMessage.append(prefix + "-chain ");
                traceMessage.append(chain.getName());
                traceMessage.append(":       +  ");
                traceMessage.append(channel.getName());
                traceMessage.append(" - ");
                traceMessage.append(channel.getFactoryType());
                traceMessage.append(EOLN);
            }
        }

        debugTrace(logTool, traceMessage.toString());
    }

    /**
     * Print channel configuration - for debug
     * 
     * @param logTool
     *            Caller's LogTool (should be Logger OR TraceComponent)
     * @param cfw
     *            Reference to channel framework service
     * @param message
     *            Description to accompany trace, e.g. "CFW Channel
     *            Configuration"
     * @param prefix
     *            Component-type prefix used to associate traces together, e.g.
     *            "XMEM", "ZIOP", "TCP", "SOAP"
     */
    protected final void traceChannels(Object logTool, ChannelFramework cfw, String message, String prefix) {
        if (cfw == null) {
            debugTrace(logTool, prefix + " - No cfw to trace channels");
            return;
        }

        List<?> lchannel;
        Iterator<?> ichannel;

        ChannelData channel;
        ChannelData[] channelList;

        // Create new String builder for list of configured channels
        StringBuilder traceMessage = new StringBuilder(300);

        // Prepend message passed as argument to our output
        traceMessage.append(prefix + ": Configured Channels - ");

        if (message != null)
            traceMessage.append(message);

        traceMessage.append(EOLN);

        // Get a list of all existing channels (should include XMem)
        channelList = cfw.getAllChannels();

        // Trace all configured channels
        lchannel = Arrays.asList(channelList);
        for (ichannel = lchannel.iterator(); ichannel.hasNext();) {
            channel = (ChannelData) ichannel.next();
            traceMessage.append(prefix + ":    ");
            traceMessage.append(channel.getName());
            traceMessage.append(" - ");
            traceMessage.append(channel.getFactoryType());
            traceMessage.append(EOLN);
        }

        debugTrace(logTool, traceMessage.toString());
    }

    /**
     * Print debug stacktrace using given trace component. -- Use tc of CALLER,
     * so that this appears with it's trace..
     * 
     * @param logTool
     *            Caller's LogTool (should be Logger OR TraceComponent)
     * @param t
     *            Throwable (contains execution stack)
     * @param message
     *            String to accompany stack trace
     */
    protected final void traceDebugStack(Object logTool, Throwable t, String message) {
        StackTraceElement[] stack = t.getStackTrace();

        if (stack != null) {
            // convert stack trace to a string
            StringBuilder stackTrace = new StringBuilder();

            for (int i = 0; i < stack.length; i++) {
                stackTrace.append(EOLN);
                stackTrace.append(stack[i].toString());
            }

            debugTrace(logTool, message + EOLN + stackTrace.toString());
        }
    }

    /**
     * Print debug stacktrace using given trace component.
     * 
     * @param logTool
     * @param thread
     */
    public final void traceThreadStack(Object logTool, Thread thread) {
        StackTraceElement[] stack = thread.getStackTrace();

        if (stack != null) {
            StringBuilder stackTrace = new StringBuilder("TRACEBACK for Thread " + thread.getName() + ": ");

            for (int i = 0; i < stack.length; ++i) {
                stackTrace.append(EOLN);
                stackTrace.append(stack[i].toString());
            }

            debugTrace(logTool, EOLN + stackTrace.toString());
        }
    }

    // ----- Use basetc rather than caller's "log tool" by default  ------------------------

    /**
     * Debug trace using either Tr or Logger
     * 
     * @param logTool
     *            Caller's LogTool (should be Logger OR TraceComponent)
     * @param msg
     *            Debug message
     */
    protected abstract void debugTrace(Object logTool, String msg, Object... parameters);
}
