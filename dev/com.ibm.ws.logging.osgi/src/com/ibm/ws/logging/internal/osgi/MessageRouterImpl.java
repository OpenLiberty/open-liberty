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
package com.ibm.ws.logging.internal.osgi;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.LogRecord;

import com.ibm.wsspi.logging.LogHandler;
import com.ibm.wsspi.logging.MessageRouter;

/**
 * This class handles all logic associated with routing messages to various
 * LogHandler services.
 */
public class MessageRouterImpl implements MessageRouter {

    /**
     * Map of LogHandlerIDs to LogHandlers.
     */
    private final ConcurrentMap<String, LogHandler> logHandlerServices = new ConcurrentHashMap<String, LogHandler>();

    /**
     * A mapping of msg IDs to a set of LogHandler IDs. Messages that contain the msg ID
     * are routed to all the LogHandlers in the set of LogHandler IDs.
     * 
     * This mapping is populated by all the MessageRouter.properties files.
     */
    private final ConcurrentMap<String, Set<String>> msgIdToLogHandlerIds = new ConcurrentHashMap<String, Set<String>>();

    /**
     * CTOR, protected.
     */
    protected MessageRouterImpl() {}

    /**
     * Add the LogHandler ref. 1 or more LogHandlers may be set.
     */
    public void setLogHandler(String id, LogHandler ref) {
        if (id != null && ref != null) {
            logHandlerServices.put(id, ref);
        }
    }

    /**
     * Remove the LogHandler ref.
     */
    public void unsetLogHandler(String id, LogHandler ref) {
        if (id != null) {
            if (ref == null) {
                logHandlerServices.remove(id);
            } else {
                logHandlerServices.remove(id, ref);
            }
        }
    }

    /**
     * @return The set of LogHandlerId's associated with the given msgId.
     *         If the set doesn't exist in the map, it is created.
     */
    protected Set<String> getOrCreateLogHandlerIdSet(String msgId) {

        Set<String> logHandlerIdSet = msgIdToLogHandlerIds.get(msgId);

        if (logHandlerIdSet == null) {
            logHandlerIdSet = new CopyOnWriteArraySet<String>();
            msgIdToLogHandlerIds.put(msgId, logHandlerIdSet);

            // Every msg gets the default by default. The only way to remove
            // the default is to specify "-DEFAULT" in the prop.
            logHandlerIdSet.add("DEFAULT");
        }

        return logHandlerIdSet;
    }

    /**
     * @return s.split(delim), with null checking.
     */
    protected String[] split(String s, String delim) {
        return (s != null && !s.trim().isEmpty()) ? s.split(delim) : new String[0];
    }

    /**
     * Process the MessageRouter.properties file contents, given by the Properties parm.
     * 
     * A typical MessageRouter.properties entry looks like this:
     * 
     * CWWKF0008I=+WTO,+HARDCOPY,-DEFAULT
     * 
     * The property key is a msg ID. The property value is a list of LogHandler IDs.
     * The +/- indicates whether to add or remove the LogHandler to/from the set of
     * LogHandlers that will receive messages with the given msg ID.
     * 
     * A LogHandler ID represents the "id" property for a LogHandler ServiceReference.
     * 
     * "DEFAULT" is a special LogHandler ID. It does not represent (and cannot be used for)
     * a LogHandler ServiceReference "id" property. Instead it indicates whether or not
     * the message should be logged by the normal logging mechanisms (i.e. console.log).
     * By default, all messages are logged normally (in addition to whatever LogHandlers
     * are specified in the properties file). The only way to prevent a message from being
     * logged normally is to specify "-DEFAULT" for the message in the properties file.
     * 
     * This method updates both the msgIdToLogHandlerIdSet map and the set of LogHandlerIds
     * for each msgId, therefore it is synchronized. Both the map and the set are implemented
     * using classes from the java.util.concurrent package, to allow for concurrent read
     * access to the map/set without blocking.
     * 
     * @param props The contents of the MessageRouter.properties file.
     */
    public synchronized void modified(Properties props) {

        for (Object key : props.keySet()) {
            String msgId = (String) key;
            String logHandlerIds = props.getProperty(msgId);

            Set<String> logHandlerIdSet = getOrCreateLogHandlerIdSet(msgId);

            // "logHandlerIds" is a comma-separated list of LogHandlers IDs. Each LogHandler ID
            // may contain a +/- char in front, to indicate that the LogHandler should be 
            // added/removed to/from the list for this msgId. If the +/- is not specified, 
            // assume +.
            for (String id : split(logHandlerIds, ",")) {

                // Protect against bad input, such as empty IDs.
                id = id.trim();
                if (id.length() > 0) {
                    // Check for the +/- in first char.
                    char plusOrMinus = id.charAt(0);
                    if (plusOrMinus != '+' && plusOrMinus != '-') {
                        plusOrMinus = '+'; // + by default.
                    } else {
                        id = id.substring(1); // skip over the +/-.
                    }

                    // Make sure there's still something left after stripping off the +/-.
                    if (id.length() > 0) {
                        if (plusOrMinus == '+') {
                            logHandlerIdSet.add(id);
                        } else {
                            // '-' means remove.
                            logHandlerIdSet.remove(id);
                        }
                    }
                }
            }

            if (logHandlerIdSet.size() == 1 && logHandlerIdSet.contains("DEFAULT")) {
                // No entries (other than default) for this msgId.  Remove it from the map.
                msgIdToLogHandlerIds.remove(msgId);
            }
        }
    }

    /**
     * Add the specified log handler to the message ID's routing list.
     */
    protected void addMsgToLogHandler(String msgId, String handlerId) {
        Set<String> logHandlerIdSet = getOrCreateLogHandlerIdSet(msgId);
        logHandlerIdSet.add(handlerId);
    }

    /**
     * Remove the specified log handler from the message ID's routing list.
     */
    protected void removeMsgFromLogHandler(String msgId, String handlerId) {
        Set<String> logHandlerIdSet = getOrCreateLogHandlerIdSet(msgId);
        logHandlerIdSet.remove(handlerId);
    }

    /**
     * @return the message ID for the given message.
     */
    protected String parseMessageId(String msg) {
        if (msg == null)
            return null;

        if (msg.length() >= 10) {
            String msgId = msg.substring(0, 10); // msgID is first 10 chars.
            if (msgId.endsWith(":")) {
                msgId = msgId.substring(0, msgId.indexOf(":")); // unless it's 9 like TRAS msgIds, then read up to the :
            }
            return msgId;
        }
        else {
            return null;
        }
    }

    /**
     * @return the Set of LogHandler IDs to route this msg to
     */
    protected Set<String> getLogHandlersForMessage(String msg) {
        if (msg == null)
            return null;

        return getLogHandlersForMsgId(parseMessageId(msg));
    }

    /**
     * @return the Set of LogHandler IDs to route this msg to
     */
    protected Set<String> getLogHandlersForMsgId(String msgId) {
        if (msgId == null)
            return null;

        return msgIdToLogHandlerIds.get(msgId);
    }

    /**
     * @return true if the message is valid (not null, long enough to have an ID);
     *         false otherwise (ignore it).
     */
    protected boolean isValidMessage(String msg) {
        return (msg != null && msg.length() >= 10);
    }

    /** {@inheritDoc} */
    @Override
    public boolean route(String msg, LogRecord logRecord) {
        if (!isValidMessage(msg)) {
            return true;
        }

        Set<String> logHandlerIdSet = getLogHandlersForMessage(msg);

        if (logHandlerIdSet == null) {
            // There are no routing requirements for this msgId.
            // Return true to tell the caller to log the msg normally.
            return true;
        } else {
            // Route to all LogHandlers in the list.
            return routeToAll(msg, logRecord, logHandlerIdSet);
        }
    }

    /**
     * Route the message to all LogHandlers in the set.
     * 
     * @return true if the set contained DEFAULT, which means the msg should be logged
     *         normally as well. false otherwise.
     */
    protected boolean routeToAll(String msg, LogRecord logRecord, Set<String> logHandlerIds) {

        boolean logNormally = false;

        for (String id : logHandlerIds) {
            if (id.equals("DEFAULT")) {
                // DEFAULT is still in the list, so we should tell the caller to also log 
                // the message normally.
                logNormally = true;
            } else {
                routeTo(msg, logRecord, id);
            }
        }

        return logNormally;
    }

    /**
     * Route the message to the LogHandler identified by the given logHandlerId.
     * 
     * @param msg The fully formatted message.
     * @param logRecord The associated LogRecord, in case the LogHandler needs it.
     * @param logHandlerId The LogHandler ID in which to route.
     */
    protected void routeTo(String msg, LogRecord logRecord, String logHandlerId) {
        LogHandler logHandler = logHandlerServices.get(logHandlerId);
        if (logHandler != null) {
            logHandler.publish(msg, logRecord);
        }
    }

}
