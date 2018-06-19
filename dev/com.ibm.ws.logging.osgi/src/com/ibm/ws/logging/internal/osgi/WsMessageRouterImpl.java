/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.osgi;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ibm.ws.logging.RoutedMessage;
import com.ibm.ws.logging.WsLogHandler;
import com.ibm.ws.logging.WsMessageRouter;

/**
 * This class handles all logic associated with routing messages to various
 * WsLogHandler services.
 *
 * The logic in MessageRouterImpl handles routing messages to SPI LogHandlers.
 *
 * WsMessageRouterImpl extends MessageRouterImpl to also handle routing message
 * to internal WsLogHandlers.
 *
 * We have to separate the internal stuff from the SPI stuff so that we can
 * be more nimble/flexible/agile with the internal interfaces (the SPI is tricky
 * to change, especially since third-party code may implement the SPI).
 */
public class WsMessageRouterImpl extends MessageRouterImpl implements WsMessageRouter {

    private static final ReentrantReadWriteLock RERWLOCK = new ReentrantReadWriteLock(true);
    /**
     * Map of LogHandlerIDs to WsLogHandlers.
     */
    private final ConcurrentMap<String, WsLogHandler> wsLogHandlerServices = new ConcurrentHashMap<String, WsLogHandler>();

    /**
     * Earlier messages issued before WsLogHandler(s) are registered.
     *
     * When a new WsLogHandler is registered the earlier messages are delivered to it.
     *
     * Only the previous 100 messages are kept (can't keep them all since there's no telling
     * when or if a WsLogHandler will be registered).
     */
    private Queue<RoutedMessage> earlierMessages;

    /**
     * CTOR, protected.
     */
    protected WsMessageRouterImpl() {}

    /**
     * @param earlierMessages a queue of messages that were issued prior to this
     *            WsMessageRouter getting activated.
     */
    @Override
    public void setEarlierMessages(Queue<RoutedMessage> earlierMessages) {
        RERWLOCK.writeLock().lock();
        try {
        	this.earlierMessages = earlierMessages;
        } finally {
        	RERWLOCK.writeLock().unlock();
        }
    }

    /**
     * @return true if the message should be routed to the given LogHandler.
     */
    protected boolean shouldRouteMessageToLogHandler(RoutedMessage routedMessage, String logHandlerId) {
        if (routedMessage == null) {
            return false;
        }

        Set<String> logHandlerIds = getLogHandlersForMsgId("*");
        if (logHandlerIds != null && logHandlerIds.contains(logHandlerId)) {
            return true;
        } else {
            logHandlerIds = getLogHandlersForMessage(routedMessage.getFormattedMsg());
            return (logHandlerIds == null) ? false : logHandlerIds.contains(logHandlerId);
        }
    }

    /**
     * Add the WsLogHandler ref. 1 or more LogHandlers may be set.
     */
    public void setWsLogHandler(String id, WsLogHandler ref) {
        if (id != null && ref != null) {
            //There can be many Reader locks, but only one writer lock.
            //This ReaderWriter lock is needed to avoid duplicate messages when the class is passing on EarlyBuffer messages to the new WsLogHandler.
            RERWLOCK.writeLock().lock();
            try {
                wsLogHandlerServices.put(id, ref);
                /*
                 * Route prev messages to the new LogHandler.
                 *
                 * This is primarily for solving the problem during server init where the WsMessageRouterImpl
                 * is registered *after* we've already issued some early startup messages. We cache
                 * these early messages in the "earlierMessages" queue in BaseTraceService, which then
                 * passes them to WsMessageRouterImpl once it's registered.
                 */
                if (earlierMessages == null) {
                    return;
                }

                for (RoutedMessage earlierMessage : earlierMessages.toArray(new RoutedMessage[earlierMessages.size()])) {
                    if (shouldRouteMessageToLogHandler(earlierMessage, id)) {
                        routeTo(earlierMessage, id);
                    }
                }
            } finally {
                RERWLOCK.writeLock().unlock();
            }

        }
    }
    
    
    /**
     * Remove the LogHandler ref.
     */
    public void unsetWsLogHandler(String id, WsLogHandler ref) {
        if (id != null) {
            if (ref == null) {
                wsLogHandlerServices.remove(id);
            } else {
                wsLogHandlerServices.remove(id, ref);
            }
        }
    }

//    /**
//     * @return true if the message is valid (not null, long enough to have an ID);
//     *         false otherwise (ignore it).
//     */
//    protected boolean isValidMessage(RoutedMessage routedMessage) {
//        return (routedMessage != null && isValidMessage(routedMessage.getFormattedMsg()));
//    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean route(RoutedMessage routedMessage) {

        if (routedMessage == null) {
            return true;
        }
        //There can be many Reader locks, but only one writer lock.
        //This ReaderWriter lock is needed to avoid duplicate messages when the class is passing on the EarlyBuffer messages to the new WsLogHandler.
        RERWLOCK.readLock().lock();
        try {
            // Cache message for WsLogHandlers that haven't registered yet.
            if (earlierMessages != null) {
                earlierMessages.add(routedMessage);
            }

            Set<String> routeAllMsgsToTheseLogHandlers = getLogHandlersForMsgId("*");
            if (routeAllMsgsToTheseLogHandlers != null) {
                routeToAll(routedMessage, routeAllMsgsToTheseLogHandlers);
            }
            Set<String> logHandlerIds = getLogHandlersForMessage(routedMessage.getFormattedMsg());
            if (logHandlerIds == null) {
                // There are no routing requirements for this msgId.
                // Return true to tell the caller to log the msg normally.
                return true;
            } else {
                // Ensure console doesn't duplicate printing specific message that included in wtoMessages=*
            		if(routeAllMsgsToTheseLogHandlers != null) {
            			if(!logHandlerIds.equals(routeAllMsgsToTheseLogHandlers)) {
	            			Set<String> tempLogHandlerIds = new HashSet<String>();
	            			
	            			for(String id : logHandlerIds) {
	            				if(id.equals("DEFAULT") || id.equals("-DEFAULT") || id.equals("+DEFAULT") || !routeAllMsgsToTheseLogHandlers.contains(id)) {
	            					tempLogHandlerIds.add(id);
	            				}
	            			}
	            			return routeToAll(routedMessage, tempLogHandlerIds);
            			}else {
            				return true;
            			}
            		}else {
                        // Route to all LogHandlers in the wsLogHandlerService ConcurrentMap.
                        return routeToAll(routedMessage, logHandlerIds);
            		}
            }
        } finally {
            RERWLOCK.readLock().unlock();
        }
    }

    /**
     * Route the message to all LogHandlers in the set.
     *
     * @return true if the set contained DEFAULT, which means the msg should be logged
     *         normally as well. false otherwise.
     */
    protected boolean routeToAll(RoutedMessage routedMessage, Set<String> logHandlerIds) {

        boolean logNormally = false;

        for (String logHandlerId : logHandlerIds) {
            if (logHandlerId.equals("DEFAULT")) {
                // DEFAULT is still in the list, so we should tell the caller to also log
                // the message normally.
                logNormally = true;
            } else {
                routeTo(routedMessage, logHandlerId);
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
    protected void routeTo(RoutedMessage routedMessage, String logHandlerId) {
        WsLogHandler wsLogHandler = wsLogHandlerServices.get(logHandlerId);
        if (wsLogHandler != null) {
            wsLogHandler.publish(routedMessage);
        }
    }

}
