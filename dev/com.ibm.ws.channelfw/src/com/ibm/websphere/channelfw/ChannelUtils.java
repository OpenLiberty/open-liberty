/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.channelfw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.event.ScheduledEventService;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.channelfw.internal.CHFWEventHandler;
import com.ibm.ws.channelfw.internal.ChannelFrameworkConstants;
import com.ibm.ws.channelfw.internal.ChannelFrameworkImpl;
import com.ibm.ws.channelfw.internal.ChannelUtilsBase;
import com.ibm.ws.channelfw.internal.UtilsChainListener;
import com.ibm.ws.channelfw.internal.chains.EndPointMgrImpl;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.ChannelFactoryException;
import com.ibm.wsspi.channelfw.exception.RetryableChannelException;

/**
 * Utility methods (primarily for Debug) for use by Channels.
 * <ul>
 * <li>printDebugStackTrace - for pretty-printing a debug stack trace
 * </ul>
 *
 * Utility methods for displaying Channel and Channel Chain configuration
 * <ul>
 * <li>displayChannels - displays all configured channels
 * <li>displayChains - several variants with different parameters for tracing
 * configured channel chains.
 * </ul>
 */
public final class ChannelUtils extends ChannelUtilsBase {
    /** Trace service */
    private static final TraceComponent tc = Tr.register(ChannelUtils.class,
                                                         ChannelFrameworkConstants.BASE_TRACE_NAME,
                                                         ChannelFrameworkConstants.BASE_BUNDLE);

    /** Configuration items unable to be processed during loadConfig */
    private static Map<String, Object> delayedConfig = new HashMap<String, Object>();
    private static Map<String, Boolean> delayedStarts = new HashMap<String, Boolean>();
    private static boolean delayCheckSignaled = false;

    /**
     * Singleton instance of "this" - implements abstract methods of
     * ChannelUtilsBase
     */
    private static ChannelUtils chTrace = new ChannelUtils();

    /**
     * Print debug stacktrace using given trace component.
     *
     * @param logger
     * @param t
     * @param message
     */
    public static void printDebugStackTrace(TraceComponent logger, Throwable t,
                                            String message) {
        if (logger.isDebugEnabled()) {
            chTrace.traceDebugStack(logger, t, message);
        }
    }

    /**
     * Print debug stacktrace using given trace component.
     *
     * @param logger
     * @param thread
     */
    public static void printThreadStackTrace(TraceComponent logger, Thread thread) {
        if (logger.isDebugEnabled()) {
            chTrace.traceThreadStack(logger, thread);
        }
    }

    /**
     * Print channel configuration - for debug.
     *
     * @param logger
     * @param cfw
     * @param message
     * @param prefix
     */
    public static void displayChannels(TraceComponent logger, ChannelFramework cfw,
                                       String message, String prefix) {
        if (logger.isDebugEnabled()) {
            chTrace.traceChannels(logger, cfw, message, prefix);
        }
    }

    /**
     * Display configured channel chains.
     *
     * @param logger
     * @param cfw
     * @param factory
     * @param message
     * @param prefix
     */
    public static void displayChains(TraceComponent logger, ChannelFramework cfw,
                                     Class<?> factory, String message,
                                     String prefix) {
        if (logger.isDebugEnabled()) {
            chTrace.traceChains(logger, cfw, factory, message, prefix);
        }
    }

    /**
     * Display configured channel chains.
     *
     * @param logger
     * @param cfw
     * @param groupName
     * @param message
     * @param prefix
     */
    public static void displayChains(TraceComponent logger, ChannelFramework cfw,
                                     String groupName, String message,
                                     String prefix) {
        if (logger.isDebugEnabled()) {
            chTrace.traceChains(logger, cfw, groupName, message, prefix);
        }
    }

    /**
     * Display configured channel chains (convert array of chains to list).
     *
     * @param logger
     * @param chains
     *            Array of chains to trace
     * @param message
     * @param prefix
     */
    public static void displayChains(TraceComponent logger, ChainData[] chains,
                                     String message, String prefix) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        if (chains == null || chains.length == 0) {
            chTrace.debugTrace(logger, prefix + ", no chains to trace (" + message + ")");
        } else {
            chTrace.traceChains(logger, Arrays.asList(chains), message, prefix);
        }
    }

    /**
     * Display configured channel chains.
     *
     * @param logger
     * @param lchain
     * @param message
     * @param prefix
     */
    public static void displayChains(TraceComponent logger, List<?> lchain,
                                     String message, String prefix) {
        if (logger.isDebugEnabled()) {
            chTrace.traceChains(logger, lchain, message, prefix);
        }
    }

    // ----- extension of parent trace methods to use Tr ------------------------

    /**
     * Debug trace using either Tr or Logger
     *
     * @param logTool
     *            Caller's LogTool (should be Logger OR TraceComponent)
     * @param msg
     *            Debug message
     * @see ChannelUtilsBase#debugTrace(Object, String)
     */
    @Override
    protected void debugTrace(Object logTool, String msg, Object... parameters) {
        Tr.debug((TraceComponent) logTool, msg, parameters);
    }

    /**
     * Convert the input string to a list of strings based on the
     * provided delimiter.
     *
     * @param input
     * @param delimiter
     * @return String[]
     */
    public static String[] extractList(String input, char delimiter) {
        int end = input.indexOf(delimiter);
        if (-1 == end) {
            return new String[] { input.trim() };
        }
        List<String> output = new LinkedList<String>();
        int start = 0;
        do {
            output.add(input.substring(start, end).trim());
            start = end + 1;
            end = input.indexOf(delimiter, start);
        } while (-1 != end);
        // copy the last value if it exists
        if (start < input.length()) {
            output.add(input.substring(start).trim());
        }
        return output.toArray(new String[output.size()]);
    }

    /**
     * Extract the key of a key=value pair contained withint the
     * provided string. If no equals sign is found, then the input
     * is returned with white space trimmed.
     *
     * @param input
     * @return String
     */
    public static String extractKey(String input) {
        int index = input.indexOf('=');
        if (-1 == index) {
            return input.trim();
        }
        return input.substring(0, index).trim();
    }

    /**
     * Extract the value of a key=value pair contained within the
     * provided string. If no value exists, then an empty string
     * is returned.
     *
     * @param input
     * @return String
     */
    public static String extractValue(String input) {
        int index = input.indexOf('=') + 1;
        if (0 == index || index >= input.length()) {
            return "";
        }
        return input.substring(index).trim();
    }

    /** Supported prefix for a channel definition */
    public static final String CHANNEL_PREFIX = "channel.";
    /** Supported prefix for a chain definition */
    public static final String CHAIN_PREFIX = "chain.";
    /** Supported prefix for a group definition */
    public static final String GROUP_PREFIX = "group.";
    /** Supported prefix for a factory definition */
    public static final String FACTORY_PREFIX = "factory.";
    /** Supported prefix for an endpoint definition */
    public static final String ENDPOINT_PREFIX = "endpoint.";

    /**
     * Extract the various types of objects from the full configuration into
     * separate, discrete groupings.
     *
     * @param config
     * @return Map<String,Map<String,String[]>>
     */
    private static Map<String, Map<String, String[]>> extractConfig(Map<String, Object> config) {
        Map<String, String[]> factories = new HashMap<String, String[]>();
        Map<String, String[]> channels = new HashMap<String, String[]>();
        Map<String, String[]> chains = new HashMap<String, String[]>();
        Map<String, String[]> groups = new HashMap<String, String[]>();
        Map<String, String[]> endpoints = new HashMap<String, String[]>();

        for (Map.Entry<String, Object> entry : config.entrySet()) {
            String key = entry.getKey();
            String name;
            if (key.startsWith(CHANNEL_PREFIX)) {
                name = key.substring(CHANNEL_PREFIX.length()).trim();
                if (0 != name.length()) {
                    channels.put(name, (String[]) entry.getValue());
                }
            } else if (key.startsWith(CHAIN_PREFIX)) {
                name = key.substring(CHAIN_PREFIX.length()).trim();
                if (0 != name.length()) {
                    chains.put(name, (String[]) entry.getValue());
                }
            } else if (key.startsWith(GROUP_PREFIX)) {
                name = key.substring(GROUP_PREFIX.length()).trim();
                if (0 != name.length()) {
                    groups.put(name, (String[]) entry.getValue());
                }
            } else if (key.startsWith(FACTORY_PREFIX)) {
                name = key.substring(FACTORY_PREFIX.length()).trim();
                if (0 != name.length()) {
                    factories.put(name, (String[]) entry.getValue());
                }
            } else if (key.startsWith(ENDPOINT_PREFIX)) {
                name = key.substring(ENDPOINT_PREFIX.length()).trim();
                if (0 != name.length()) {
                    endpoints.put(name, (String[]) entry.getValue());
                }
            }
        }
        Map<String, Map<String, String[]>> rc = new HashMap<String, Map<String, String[]>>();
        rc.put("factories", factories);
        rc.put("channels", channels);
        rc.put("chains", chains);
        rc.put("groups", groups);
        rc.put("endpoints", endpoints);
        return rc;
    }

    /**
     * Load and possibly start the provided configuration information.
     *
     * @param config
     * @param start
     * @param restart
     * @return Map<String,List<String>>
     * @see ChannelUtils#loadConfig(Dictionary)
     * @see ChannelUtils#startConfig(Dictionary, boolean)
     */
    private static synchronized Map<String, List<String>> load(Map<String, Object> config, boolean start, boolean restart) {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        if (bTrace && tc.isDebugEnabled()) {
            Tr.debug(tc, "Loading CHFW config from " + config);
        }

        Map<String, Map<String, String[]>> parsed = extractConfig(config);

        // if a restart is set, then stop and remove existing chains
        // that may be running already
        if (restart) {
            unloadChains(parsed.get("chains").keySet().iterator());
        }

        // handle any factory config first
        List<String> createdFactories = loadFactories(parsed.get("factories"));

        // load any endpoints
        List<String> createdEndpoints = loadEndPoints(parsed.get("endpoints"));

        // now load any channels
        List<String> createdChannels = loadChannels(parsed.get("channels"));

        // now load any chains
        List<String> createdChains = loadChains(parsed.get("chains"), start, restart);

        // now load the chain group definitions
        List<String> createdGroups = loadGroups(parsed.get("groups"), start, restart);

        Map<String, List<String>> rc = new HashMap<String, List<String>>();
        rc.put("factory", createdFactories);
        rc.put("channel", createdChannels);
        rc.put("chain", createdChains);
        rc.put("group", createdGroups);
        rc.put("endpoint", createdEndpoints);
        return rc;
    }

    public static synchronized Map<String, List<String>> loadConfigDelay() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "loadConfigDelay called");
        }

        loadConfig(null);
        return null;
    }

    /**
     * Using the provided configuration, create or update channels,
     * chains, and groups within the channel framework.
     *
     * This will return a map containing lists of created objects. The
     * keys include "factory", "chain", "group", and "channel". The
     * lists will contains the name of the objects such as channel names,
     * chain names, etc. Each list is guarenteed to be non-null; however,
     * might easily be empty.
     *
     * @param config
     * @return Map<String,List<String>>
     */
    public static synchronized Map<String, List<String>> loadConfig(Map<String, Object> config) {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        boolean usingDelayed = false;
        if (null == config) {
            if (delayedConfig.isEmpty()) {
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "loadConfig returning - nothing to do");
                }
                return null;
            }
            // process the delayed configuration values
            if (bTrace && tc.isDebugEnabled()) {
                Tr.debug(tc, "loadConfig using delayed config information");
            }
            config = delayedConfig;
            delayedConfig = new HashMap<String, Object>();

            usingDelayed = true;
        }
        Map<String, List<String>> rc = load(config, false, true);
        if (usingDelayed && !delayedStarts.isEmpty()) {
            // see if we can start any of the chains now

            start(rc, false, false);

        }
        return rc;
    }

    private static Map<String, List<String>> start(Map<String, List<String>> config, boolean start, boolean restart) {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        final ChannelFramework cf = ChannelFrameworkFactory.getChannelFramework();
        List<String> groups = config.get("group");
        // make a list of chains to start
        List<String> chains = new LinkedList<String>(config.get("chain"));
        // add any chains that the groups contain but were not on the list
        for (String group : groups) {
            ChainGroupData cgd = cf.getChainGroup(group);
            if (null != cgd) {
                for (ChainData cd : cgd.getChains()) {
                    if (!chains.contains(cd.getName())) {
                        chains.add(cd.getName());
                    }
                }
            }
        }

        // check each chain on the list to start or restart what needs it,
        // which could be set by the input values or by the delayed start map
        for (String chain : chains) {
            ChainData cd = cf.getChain(chain);
            if (null == cd || cd.getType().equals(FlowType.OUTBOUND)) {
                continue;
            }
            boolean doStart = start;
            boolean doRestart = restart;
            Boolean bRestart = delayedStarts.remove(chain);
            if (null != bRestart) {
                doStart = true;
                doRestart = bRestart.booleanValue();
            }
            if (!doStart) {
                continue;
            }
            try {
                if (cf.isChainRunning(cd)) {
                    if (doRestart) {
                        if (bTrace && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Restarting chain; " + chain);
                        }
                        cf.stopChain(cd, 0L);
                        cf.startChain(cd);
                    }
                } else {
                    cf.startChain(cd);
                }
            } catch (Exception e) {
                FFDCFilter.processException(e, "ChannelUtils.start",
                                            "chain", new Object[] { chain, cf });
                if (bTrace && tc.isEventEnabled()) {
                    Tr.event(tc, "Exception during start; " + chain + " " + e);
                }
            }
        } // end-chains

        Map<String, List<String>> rc = new HashMap<String, List<String>>();
        rc.put("chain", chains);
        rc.put("group", groups);
        return rc;
    }

    /**
     * @param properties
     * @param restart
     */
    public static Map<String, List<String>> startConfig(Dictionary<String, Object> properties, boolean restart) {
        Map<String, Object> map = new HashMap<String, Object>();
        if (properties instanceof Hashtable) {
            map.putAll((Hashtable) properties);
        } else {
            Enumeration<String> keys = properties.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                map.put(key, properties.get(key));
            }
        }
        return startConfig(map, restart);
    }

    /**
     * Load and start the provided configuration of channels, chains, and
     * groups. If a chain is already started, the restart input flag is used
     * to decide whether to stop and restart that chain. Note that some
     * configuration values would require a restart of the chain to take
     * effect (such as TCP bind information).
     *
     * This returns map of the started chains and groups. Each is a list
     * under either the key "chain" or the key "group". The list has the
     * names of the channel framework items, ie. channel names, chains names, etc.
     * Each list is guaranteed to be non-null but might be empty.
     *
     * @param config
     * @param restart
     * @return Map<String,List<String>>
     */
    public static Map<String, List<String>> startConfig(Map<String, Object> config, boolean restart) {
        Map<String, List<String>> rc = load(config, true, restart);
        return start(rc, true, restart);
    }

    private static boolean hasChanged(String[] oldValues, String[] newValues) {
        if (oldValues.length != newValues.length) {
            return true;
        }
        // scan the old values to see if any were removed or changed
        for (String oldattr : oldValues) {
            boolean changed = true;
            // scan the decreasing in size newlist (as matchs are found)
            for (String newattr : newValues) {
                if (oldattr.equalsIgnoreCase(newattr)) {
                    changed = false;
                    break;
                }
            }
            if (changed) {
                return true;
            }
        }
        // note: new attributes either fail on the length comparison or result
        // in a missing old attribute so don't scan the other direction
        return false;
    }

    /**
     * This method will compare the new configuration against a previous configuration
     * and handle changes between the two. Deleted objects will be removed from
     * runtime and updates will stop and restart those specific chains. New chains
     * will be handled as normal.
     *
     * This is same as startConfig(Dictionary,boolean) as to what it returns.
     *
     * @param oldconfig
     * @param newconfig
     * @return Map<String,List<String>>
     * @see ChannelUtils#startConfig(Dictionary, boolean)
     */
    public static synchronized Map<String, List<String>> startConfig(Map<String, Object> oldconfig, Map<String, Object> newconfig) {
        if (null == oldconfig) {
            // if no old config, then assume this is not a restart situation.  If "true" is passed for restart, then
            // other logic that throws an exception while trying to "unload" started chains needs to be modify not to throw
            // those exceptions, since sometimes the chains won't be loaded in the first place.
            return startConfig(newconfig, false);
        }
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        if (bTrace && tc.isEventEnabled()) {
            Tr.event(tc, "startConfig(old,new)");
        }
        final ChannelFramework cf = ChannelFrameworkFactory.getChannelFramework();
        Map<String, Map<String, String[]>> oldc = extractConfig(oldconfig);
        Map<String, Map<String, String[]>> newc = extractConfig(newconfig);

        // TODO handle groups

        List<String> runningChains = new LinkedList<String>();
        Map<String, String[]> oldlist = oldc.get("chains");
        Map<String, String[]> newlist = newc.get("chains");
        List<String> chainsToDelete = new LinkedList<String>();
        List<String> chainsToStop = new LinkedList<String>();
        Map<String, String[]> chainsToStart = new HashMap<String, String[]>();
        // if an old chain name no longer exists, we need to stop it
        for (String oldname : oldlist.keySet()) {
            if (!newlist.containsKey(oldname)) {
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Defunct chain; " + oldname);
                }
                chainsToDelete.add(oldname);
            }
        }
        // check for new or updated chain definitions
        for (Entry<String, String[]> entry : newlist.entrySet()) {
            final String newname = entry.getKey();
            final String[] oldobj = oldlist.get(newname);
            if (null == oldobj) {
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "New chain; " + newname);
                }
                chainsToStart.put(newname, entry.getValue());
            } else {
                // compare attributes
                final String[] newobj = entry.getValue();
                if (hasChanged(oldobj, newobj)) {
                    if (bTrace && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Chain updated; " + newname);
                    }
                    chainsToStop.add(newname);
                    chainsToStart.put(newname, newobj);
                } else {
                    if (bTrace && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Chain unchanged; " + newname);
                    }
                    runningChains.add(newname);
                }
            }
        }

        // check for new or updated channels
        List<String> runningChannels = new LinkedList<String>();
        Map<String, String[]> channelsToCreate = new HashMap<String, String[]>();
        oldlist = oldc.get("channels");
        newlist = newc.get("channels");
        for (String oldname : oldlist.keySet()) {
            if (!newlist.containsKey(oldname)) {
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Defunct channel: " + oldname);
                }
                try {
                    // must delete chains that were using the defunct channel
                    ChainData[] existingchains = cf.getAllChains(oldname);
                    for (ChainData cd : existingchains) {
                        final String name = cd.getName();
                        if (bTrace && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Deleting chain; " + name);
                        }
                        chainsToDelete.add(name);
                        chainsToStart.remove(name);
                        runningChains.remove(name);
                    }
                } catch (ChannelException e) {
                    FFDCFilter.processException(e, "ChannelUtils.startConfig",
                                                "defunctChannel", new Object[] { oldname, cf });
                    if (bTrace && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Unable to query defunct channel; " + e);
                    }
                }
            }
        }
        for (String newname : newlist.keySet()) {
            String[] oldobj = oldlist.get(newname);
            String[] newobj = newlist.get(newname);
            if (null == oldobj) {
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "New channel; " + newname);
                }
                channelsToCreate.put(newname, newobj);
            } else {
                if (hasChanged(oldobj, newobj)) {
                    if (bTrace && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Channel updated; " + newname);
                    }
                    channelsToCreate.put(newname, newobj);
                    try {
                        // since we don't know if the channel supports runtime
                        // changes, stop chains using it
                        ChainData[] c = cf.getAllChains(newname);
                        for (ChainData cd : c) {
                            final String name = cd.getName();
                            if (runningChains.contains(name)) {
                                if (bTrace && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "Restarting chain; " + name);
                                }
                                chainsToStop.add(name);
                                runningChains.remove(name);
                                // if it wasn't already flagged to start, make sure
                                // it restarts after the stop
                                if (!chainsToStart.containsKey(name)) {
                                    chainsToStart.put(name, newc.get("chains").get(name));
                                }
                            }
                        }
                    } catch (ChannelException e) {
                        FFDCFilter.processException(e, "ChannelUtils.startConfig",
                                                    "updatedChannel", new Object[] { newname, cf });
                        if (bTrace && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Unable to query updated channel; " + e);
                        }
                    }
                } else {
                    if (bTrace && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Channel unchanged; " + newname);
                    }
                    runningChannels.add(newname);
                }
            }
        }

        // check factories
        List<String> runningFactories = new LinkedList<String>();
        Map<String, String[]> factoriesToCreate = new HashMap<String, String[]>();
        oldlist = oldc.get("factories");
        newlist = newc.get("factories");
        for (String oldname : oldlist.keySet()) {
            if (!newlist.containsKey(oldname)) {
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Defunct factory; " + oldname);
                }
            }
        }
        for (String newname : newlist.keySet()) {
            String[] oldobj = oldlist.get(newname);
            String[] newobj = newlist.get(newname);
            if (null == oldobj) {
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "New factory; " + newname);
                }
                factoriesToCreate.put(newname, newobj);
            } else {
                if (hasChanged(oldobj, newobj)) {
                    if (bTrace && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Factory updated; " + newname);
                    }
                    factoriesToCreate.put(newname, newobj);
                } else {
                    if (bTrace && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Factory unchanged; " + newname);
                    }
                    runningFactories.add(newname);
                }
            }
        }

        // check for updated endpoints
        List<String> runningEndpoints = new LinkedList<String>();
        Map<String, String[]> endpointsToCreate = new HashMap<String, String[]>();
        List<String> updatedEndpoints = new LinkedList<String>();
        oldlist = oldc.get("endpoints");
        newlist = newc.get("endpoints");
        for (String oldname : oldlist.keySet()) {
            if (!newlist.containsKey(oldname)) {
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Defunct endpoint; " + oldname);
                }
                EndPointMgrImpl.getRef().removeEndPoint(oldname);
            }
        }
        for (String newname : newlist.keySet()) {
            String[] oldobj = oldlist.get(newname);
            String[] newobj = newlist.get(newname);
            if (null == oldobj) {
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "New endpoint; " + newname);
                }
                endpointsToCreate.put(newname, newobj);
            } else {
                if (hasChanged(oldobj, newobj)) {
                    if (bTrace && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Endpoint updated; " + newname);
                    }
                    endpointsToCreate.put(newname, newobj);
                    updatedEndpoints.add(newname);
                } else {
                    if (bTrace && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Endpoint unchanged; " + newname);
                    }
                    runningEndpoints.add(newname);
                }
            }
        }
        // load new endpoint definitions
        runningEndpoints.addAll(loadEndPoints(endpointsToCreate));
        // TCP does not allow endpoint runtime changes, restart necessary chains
        if (!updatedEndpoints.isEmpty()) {
            final Map<String, String[]> newchains = newc.get("chains");
            final Map<String, String[]> newchannels = newc.get("channels");
            for (String chainname : runningChains) {
                ChainData cd = cf.getChain(chainname);
                ChannelData[] channels = (null != cd) ? cd.getChannelList() : null;
                if (null != channels && 0 < channels.length) {
                    String ep = (String) channels[0].getPropertyBag().get("endPointName");
                    if (null != ep && updatedEndpoints.contains(ep)) {
                        if (bTrace && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Chain [" + chainname + "] using updated endpoint " + ep);
                        }
                        chainsToStop.add(chainname);
                        chainsToStart.put(chainname, newchains.get(chainname));
                        String tcp = channels[0].getExternalName();
                        channelsToCreate.put(tcp, newchannels.get(tcp));
                    }
                }
            }
        }

        Map<String, List<String>> rc = new HashMap<String, List<String>>();
        // stop chains that need it
        if (!chainsToDelete.isEmpty()) {
            unloadChains(chainsToDelete.iterator());
        }

        // first stop/quiesce the chains (AND WAIT FOR COMPLETION),
        // then destroy them for updates to happen correctly
        stopChains(chainsToStop, -1L, null);
        for (String chainname : chainsToStop) {
            runningChains.remove(chainname);
            try {
                ChainData cd = cf.getChain(chainname);
                if (null != cd) {
                    cf.destroyChain(cd);
                }
            } catch (Exception e) {
                FFDCFilter.processException(e, "ChannelUtils.startConfig",
                                            "stopChain", new Object[] { chainname, cf });
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Error destroying chain; " + chainname + " " + e);
                }
                chainsToStart.remove(chainname);
            }
        }

        runningFactories.addAll(loadFactories(factoriesToCreate));
        rc.put("factory", runningFactories);
        rc.put("endpoint", runningEndpoints);
        runningChannels.addAll(loadChannels(channelsToCreate));
        rc.put("channel", runningChannels);
        runningChains.addAll(loadChains(chainsToStart, true, true));
        for (String chain : chainsToStart.keySet()) {
            try {
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Starting chain: " + chain);
                }
                cf.startChain(chain);
            } catch (RetryableChannelException rce) {
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Error starting chain; " + rce);
                }
            } catch (Exception e) {
                FFDCFilter.processException(e, "ChannelUtils.startConfig",
                                            "chain", new Object[] { chain, cf });
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Error starting chain; " + e);
                }
            }
        }
        rc.put("chain", runningChains);
        // rc.put("group", loadGroups(newc.get("group"), true, false));
        rc.put("group", new LinkedList<String>());
        return rc;
    }

    /**
     * Utility method that stops any of the chains created by the configuration
     * that might still be running. The provided timeout is used to quiesce any
     * active connections on the chains.
     *
     * @param chains
     * @param timeout in milliseconds - a timeout value of -1 will use the channel framework
     *            default quiesce timeout setting
     */
    public static void stopChains(List<String> chains, long timeout, final Runnable runOnStop) {
        if (null == chains || chains.isEmpty()) {
            return;
        }
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        final ChannelFramework cf = ChannelFrameworkFactory.getChannelFramework();

        final long quiesceTimeout = (-1L == timeout) ? cf.getDefaultChainQuiesceTimeout() : timeout;

        final UtilsChainListener listener = new UtilsChainListener();
        for (String chain : chains) {
            ChainData cd = cf.getChain(chain);
            if (null == cd) {
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Skipping unknown chain; " + chain);
                }
                continue;
            }
            if (FlowType.OUTBOUND.equals(cd.getType()) || !cf.isChainRunning(cd)) {
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Skipping chain; " + chain);
                }
                continue;
            }
            try {
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Stopping chain: " + chain);
                }

                if (quiesceTimeout > 0) {
                    listener.watchChain(cd);
                }
                cf.stopChain(cd, quiesceTimeout);

            } catch (Throwable t) {
                // framework already FFDCs on stopChain failure
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Error stopping chain: " + t);
                }
            }
        }

        if (runOnStop != null) {
            ExecutorService executorService = CHFWBundle.getExecutorService();
            if (null != executorService) {
                // Use a different thread to wait for chain stop and then finish..
                Runnable runner = new Runnable() {
                    @Override
                    public void run() {
                        // wait for the quiesce time to expire
                        listener.waitOnChains(quiesceTimeout);

                        // call the runnable to finish cleanup activity.
                        runOnStop.run();
                    }
                };
                executorService.execute(runner);

                // We've forked waiting for chain stop to a different thread
                return;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Waiting for chains to stop");
        }
        // wait for the quiesce time to expire
        listener.waitOnChains(quiesceTimeout);
    }

    /**
     * Load channel factories in the framework based on the provided factory
     * configurations.
     *
     * @param factories
     * @return List<String> - factories created successfully
     */
    private static List<String> loadFactories(Map<String, String[]> factories) {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        final ChannelFramework cf = ChannelFrameworkFactory.getChannelFramework();
        List<String> createdFactories = new ArrayList<String>(factories.size());

        String key, value;
        for (Entry<String, String[]> entry : factories.entrySet()) {
            Class<?> factoryType = cf.lookupFactory(entry.getKey());
            if (null == factoryType) {
                if (bTrace && tc.isEventEnabled()) {
                    Tr.event(tc, "Delay; missing factory type " + entry.getKey());
                }
                delayedConfig.put(FACTORY_PREFIX + entry.getKey(), entry.getValue());
                createdFactories.add(entry.getKey());
                continue;
            }
            String[] props = entry.getValue();
            for (int i = 0; i < props.length; i++) {
                key = extractKey(props[i]);
                value = extractValue(props[i]);
                if (null != key && null != value) {
                    try {
                        cf.updateChannelFactoryProperty(factoryType, key, value);
                    } catch (ChannelFactoryException e) {
                        FFDCFilter.processException(e, "ChannelUtils.loadFactories",
                                                    "update", new Object[] { entry, cf });
                        if (bTrace && tc.isEventEnabled()) {
                            Tr.event(tc, "Unable to update factory prop; "
                                         + factoryType + " " + key + "=" + value);
                        }
                    }
                }
            }
            try {
                // Note: this is a find or create api, not just "get"
                cf.getChannelFactory(factoryType);
                createdFactories.add(entry.getKey());
            } catch (ChannelFactoryException e) {
                // ignore it
            }
        } // end-factories

        return createdFactories;
    }

    /**
     * Define a new endpoint definition using the input name and list of
     * properties.
     *
     * @param name
     * @param config
     * @return EndPointInfo
     * @throws IllegalArgumentException if input values are incorrect
     */
    private static EndPointInfo defineEndPoint(EndPointMgr epm, String name, String[] config) {
        String host = null;
        String port = null;
        for (int i = 0; i < config.length; i++) {
            String key = ChannelUtils.extractKey(config[i]);
            if ("host".equalsIgnoreCase(key)) {
                host = ChannelUtils.extractValue(config[i]);
            } else if ("port".equalsIgnoreCase(key)) {
                port = ChannelUtils.extractValue(config[i]);
            }
        }
        return epm.defineEndPoint(name, host, Integer.parseInt(port));
    }

    /**
     * Load chain endpoints based on the provided configuration.
     *
     * @param endpoints
     * @return List<String> - endpoints created successfully
     */
    private static List<String> loadEndPoints(Map<String, String[]> endpoints) {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        final EndPointMgr epm = EndPointMgrImpl.getRef();
        List<String> createdEndpoints = new ArrayList<String>(endpoints.size());

        for (Entry<String, String[]> entry : endpoints.entrySet()) {
            try {
                EndPointInfo ep = defineEndPoint(epm, entry.getKey(), entry.getValue());
                createdEndpoints.add(ep.getName());
            } catch (IllegalArgumentException e) {
                FFDCFilter.processException(e, "ChannelUtils.loadEndPoints",
                                            "create", new Object[] { epm, entry });
                if (bTrace && tc.isEventEnabled()) {
                    Tr.event(tc, "Unable to load endpoint: " + e);
                }
            }
        }

        return createdEndpoints;
    }

    /**
     * Load individual channels in the framework based on the provided
     * channel configurations.
     *
     * @param channels
     * @return List<String> - channels created succesfully
     */
    private static List<String> loadChannels(Map<String, String[]> channels) {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        final ChannelFramework cf = ChannelFrameworkFactory.getChannelFramework();
        List<String> createdChannels = new ArrayList<String>(channels.size());
        boolean fireMissingEvent = false;

        final EndPointMgr epm = EndPointMgrImpl.getRef();
        String key, value;
        for (Entry<String, String[]> entry : channels.entrySet()) {
            String channel = entry.getKey();
            Map<Object, Object> props = new HashMap<Object, Object>();
            int weight = -1;
            Class<?> factoryType = null;
            for (String prop : entry.getValue()) {
                key = extractKey(prop);
                value = extractValue(prop);
                if ("type".equalsIgnoreCase(key)) {
                    if (null != factoryType) {
                        // already set
                        continue;
                    }
                    factoryType = cf.lookupFactory(value);
                } else if ("endpoint".equalsIgnoreCase(key)) {
                    if (props.containsKey("hostname")) {
                        // already set
                        continue;
                    }
                    // convert to host/port
                    if (null != epm) {
                        EndPointInfo ep = epm.getEndPoint(value);
                        if (null != ep) {
                            if (bTrace && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Using endpoint: " + ep);
                            }
                            props.put("endPointName", value);
                            props.put("hostname", ep.getHost());
                            props.put("port", String.valueOf(ep.getPort()));
                        } else {
                            if (bTrace && tc.isEventEnabled()) {
                                Tr.event(tc, "Unknown endpoint: " + value);
                            }
                        }
                    } else {
                        if (bTrace && tc.isEventEnabled()) {
                            Tr.event(tc, "Unable to convert endpoint: " + value);
                        }
                    }
                } else if ("weight".equalsIgnoreCase(key)) {
                    if (0 > weight) {
                        // already set
                        continue;
                    }
                    try {
                        weight = Integer.parseInt(value);
                    } catch (NumberFormatException nfe) {
                        FFDCFilter.processException(nfe, "ChannelUtils.loadChannels",
                                                    "weight", new Object[] { entry, cf });
                        if (bTrace && tc.isEventEnabled()) {
                            Tr.event(tc, "Invalid discrimination weight: " + value);
                        }
                    }
                } else {
                    props.put(key, value);
                }
            } // end-props
            if (null == factoryType) {
                if (bTrace && tc.isEventEnabled()) {
                    Tr.event(tc, "Delay; channel missing factory; " + channel);
                }
                delayedConfig.put(CHANNEL_PREFIX + channel, entry.getValue());
                createdChannels.add(channel);
                fireMissingEvent = true;
                continue;
            }
            // change invalid weights to the default number
            if (0 > weight) {
                weight = 10;
            }
            try {
                ChannelData cd = cf.getChannel(channel);
                if (null == cd) {
                    cd = cf.addChannel(channel, factoryType, props, weight);
                } else {
                    cf.updateAllChannelProperties(channel, props);
                }
                createdChannels.add(channel);
            } catch (Exception e) {
                FFDCFilter.processException(e, "ChannelUtils.loadChannels",
                                            "update", new Object[] { entry, cf });
                if (bTrace && tc.isEventEnabled()) {
                    Tr.event(tc, "Unable to add channel: " + channel + "; " + e);
                }
            }
        } // end-channels

        if (fireMissingEvent) {
            fireMissingEvent();
        }

        return createdChannels;
    }

    /**
     * Fire the missing config delayed event.
     */
    private static void fireMissingEvent() {
        if (delayCheckSignaled) {
            // already going...
            return;
        }
        ScheduledEventService scheduler = CHFWBundle.getScheduleService();
        if (null != scheduler) {
            delayCheckSignaled = true;
            ChannelFrameworkImpl cf = (ChannelFrameworkImpl) ChannelFrameworkFactory.getChannelFramework();
            scheduler.schedule(CHFWEventHandler.EVENT_CHECK_MISSING,
                               cf.getMissingConfigDelay(), TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Stop and unload any of the provided chain names that might exist and be
     * currently running.
     *
     * @param chains
     */
    private static void unloadChains(Iterator<String> chains) {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        final ChannelFramework cf = ChannelFrameworkFactory.getChannelFramework();

        List<String> runningChains = new LinkedList<String>();
        while (chains.hasNext()) {
            ChainData cd = cf.getChain(chains.next());
            if (null != cd && FlowType.INBOUND.equals(cd.getType())) {
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unloading chain; " + cd.getName());
                }
                try {
                    if (cf.isChainRunning(cd)) {
                        runningChains.add(cd.getName());
                    } else {
                        cf.destroyChain(cd);
                        cf.removeChain(cd);
                    }
                } catch (Exception e) {
                    FFDCFilter.processException(e, "ChannelUtils",
                                                "unloadChains", new Object[] { cd, cf });
                    if (bTrace && tc.isEventEnabled()) {
                        Tr.event(tc, "Unable to remove chain; " + cd.getName());
                    }
                }
            }
        }

        // Stop chains, and wait for stop to complete...
        stopChains(runningChains, -1L, null);
        for (String name : runningChains) {
            ChainData cd = cf.getChain(name);
            if (bTrace && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unloading stopped chain; " + name);
            }
            try {
                cf.destroyChain(cd);
                cf.removeChain(cd);
            } catch (Exception e) {
                FFDCFilter.processException(e, "ChannelUtils", "unloadChains",
                                            new Object[] { cd, cf });
                if (bTrace && tc.isEventEnabled()) {
                    Tr.event(tc, "Unable to remove chain; " + name);
                }
            }
        }
    }

    /**
     * Load chains within the channel framework based on the provided
     * chain configurations.
     *
     * @param chains
     * @param start
     * @param restart
     * @return List<String> - chains created successfully
     */
    private static List<String> loadChains(Map<String, String[]> chains, boolean start, boolean restart) {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        final ChannelFramework cf = ChannelFrameworkFactory.getChannelFramework();
        List<String> createdChains = new ArrayList<String>(chains.size());

        String key, value;
        for (Entry<String, String[]> entry : chains.entrySet()) {
            String chain = entry.getKey();
            FlowType flow = FlowType.INBOUND;
            boolean enable = true;
            boolean missingChannels = false;
            String[] chanList = null;
            for (String prop : entry.getValue()) {
                key = extractKey(prop);
                value = extractValue(prop);
                if ("enable".equalsIgnoreCase(key)) {
                    enable = Boolean.parseBoolean(value);
                } else if ("channels".equalsIgnoreCase(key)) {
                    // parse the channel list
                    chanList = extractList(value, ',');
                    for (String channel : chanList) {
                        if (null == cf.getChannel(channel)) {
                            if (bTrace && tc.isEventEnabled()) {
                                Tr.event(tc, "Chain contains missing channel: " + channel);
                            }
                            chanList = null;
                            missingChannels = true;
                            break; // out of channel check loop
                        }
                    }
                } else if ("flow".equalsIgnoreCase(key)) {
                    flow = "inbound".equalsIgnoreCase(value) ? FlowType.INBOUND : FlowType.OUTBOUND;
                }
            }
            if (null == chanList) {
                // chain either had no channels configured or it has channels
                // that are waiting on their providers
                if (!missingChannels) {
                    if (bTrace && tc.isEventEnabled()) {
                        Tr.event(tc, "Chain had no channels; " + chain);
                    }
                    continue;
                }
                if (bTrace && tc.isEventEnabled()) {
                    Tr.event(tc, "Delay; chain missing channels; " + chain);
                }
                if (start) {
                    delayedStarts.put(chain, Boolean.valueOf(restart));
                }
                delayedConfig.put(CHAIN_PREFIX + chain, entry.getValue());
                createdChains.add(chain);
                continue;
            }
            try {
                ChainData cd = cf.getChain(chain);
                if (null == cd) {
                    cd = cf.addChain(chain, flow, chanList);
                } else {
                    cd = cf.updateChain(chain, chanList);
                }
                cd.setEnabled(enable);
                createdChains.add(chain);
            } catch (Exception e) {
                FFDCFilter.processException(e, "ChannelUtils.loadChains",
                                            "create", new Object[] { entry, cf });
                if (bTrace && tc.isEventEnabled()) {
                    Tr.event(tc, "Unable to add chain " + chain + "; " + e);
                }
            }
        } // end-chains

        return createdChains;
    }

    /**
     * Load chain groups in the framework based on the provided group
     * configurations.
     *
     * @param groups
     * @param start
     * @param restart
     * @return List<String> - groups created successfully
     */
    private static List<String> loadGroups(Map<String, String[]> groups, boolean start, boolean restart) {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        final ChannelFramework cf = ChannelFrameworkFactory.getChannelFramework();
        List<String> createdGroups = new ArrayList<String>(groups.size());

        for (Entry<String, String[]> entry : groups.entrySet()) {
            String group = entry.getKey();
            List<String> chains = new ArrayList<String>(entry.getValue().length);
            List<String> delayed = new LinkedList<String>();
            // scan for missing chains
            for (String chain : entry.getValue()) {
                if (null != cf.getChain(chain)) {
                    chains.add(chain);
                } else {
                    delayed.add(chain);
                }
            }
            // save off any delayed chains for this group definition
            if (!delayed.isEmpty()) {
                if (bTrace && tc.isEventEnabled()) {
                    Tr.event(tc, "Delay; group missing chains; " + group);
                }
                if (start) {
                    delayedStarts.put(group, Boolean.valueOf(restart));
                }
                delayedConfig.put(GROUP_PREFIX + group, delayed.toArray(new String[delayed.size()]));
                createdGroups.add(group);
            }
            // cannot use an empty list
            if (0 == chains.size()) {
                if (bTrace && tc.isEventEnabled()) {
                    Tr.event(tc, "Group has no current chains; " + group);
                }
                continue;
            }
            try {
                ChainGroupData cgd = cf.getChainGroup(group);
                if (null == cgd) {
                    cgd = cf.addChainGroup(group, chains.toArray(new String[chains.size()]));
                } else {
                    // adding more chains to the group
                    for (String newchain : chains) {
                        cf.addChainToGroup(group, newchain);
                    }
                }
                createdGroups.add(group);
            } catch (Exception e) {
                FFDCFilter.processException(e, "ChannelUtils.loadGroups",
                                            "add", new Object[] { entry, cf });
                if (bTrace && tc.isEventEnabled()) {
                    Tr.event(tc, "Unable to configure group " + group + "; " + e);
                }
            }
        } // end-groups

        return createdGroups;
    }

    /**
     * Check and report on any missing configuration.
     */
    public static synchronized void checkMissingConfig() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Checking for missing config");
        }
        if (delayedConfig.isEmpty()) {
            return;
        }
        delayCheckSignaled = false;
        final ChannelFramework cf = ChannelFrameworkFactory.getChannelFramework();
        Map<String, Map<String, String[]>> parsed = extractConfig(delayedConfig);
        // check for missing factories from channel configs
        for (Entry<String, String[]> entry : parsed.get("channels").entrySet()) {
            for (String prop : entry.getValue()) {
                if ("type".equals(extractKey(prop))) {
                    String factory = extractValue(prop);
                    if (null == cf.lookupFactory(factory)) {
                        if (tc.isWarningEnabled()) {
                            Tr.warning(tc, "missing.factory", factory);
                        }
                    }
                }
            }
        }
    }
}
