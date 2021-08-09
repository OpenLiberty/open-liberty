/*******************************************************************************
 * Copyright (c) 2003, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.server;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.List;

import com.ibm.websphere.channelfw.CFEndPoint;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.ConversationReceiveListener;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * An abstraction of the essential Channel Framework / TCP Channel network
 * services wrapped into a JetStream friendly class. In essence, this class
 * attempts to hide the complexities required to play in the channel
 * framework from the rest of the communications code.
 */
@SuppressWarnings("unchecked")
public abstract class ServerConnectionManager
{
    private static final TraceComponent tc = SibTr.register(ServerConnectionManager.class,
                                                            JFapChannelConstants.MSG_GROUP,
                                                            JFapChannelConstants.MSG_BUNDLE);

    static
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.server/src/com/ibm/ws/sib/jfapchannel/ServerConnectionManager.java, SIB.comms, WASX.SIB, aa1225.01 1.34");

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "<clinit>");
        try
        {
            Class serverImpl = Class.forName(JFapChannelConstants.SERVER_MANAGER_CLASS);
            Constructor serverImplConstructor = serverImpl.getConstructor(new Class[] {});
            instance = (ServerConnectionManager) serverImplConstructor.newInstance(new Object[] {});
        } catch (Exception e)
        {
            FFDCFilter.processException(e, "com.ibm.ws.sib.jfapchannel.ServerConnectionManager.initialise",
                                        JFapChannelConstants.SRVRCONNMGR_STATICCONS_01);

            SibTr.error(tc, "EXCP_DURING_INIT_SICJ0004", new Object[] { "<init>", JFapChannelConstants.SERVER_MANAGER_CLASS, e });
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.exception(tc, e);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "<clinit>");

    }

    private static ServerConnectionManager instance;

    private static boolean initialised = false;

    /**
     * Initialises the channel framework. This must be called before any
     * other method may be invoked in this class. Don't worry - you can call
     * this multiple times without anything bad happening.
     */
    public static void initialise(AcceptListenerFactory _acceptListenerFactory)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "initialise");

        if (!initialised)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "not previously initialised");
            Class clientImpl = instance.getClass();
            Method initialiseMethod;
            try
            {
                initialiseMethod = clientImpl.getMethod("initialise", new Class[] { AcceptListenerFactory.class });
                initialiseMethod.invoke(clientImpl, new Object[] { _acceptListenerFactory });
            } catch (Exception e)
            {
                FFDCFilter.processException
                                (e, "com.ibm.ws.sib.jfapchannel.ServerConnectionManager.{}",
                                 JFapChannelConstants.SRVRCONNMGR_INITIALISE_01);

                // Start D249068.1
                // Ensure the exception we dump to the user is the root exception if it was an
                // InvocationTargetException
                Throwable displayedException = e;
                if (e instanceof InvocationTargetException)
                    displayedException = e.getCause();

                SibTr.error(tc, "EXCP_DURING_INIT_SICJ0003", new Object[] { clientImpl, displayedException });
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.exception(tc, e);
                // End D249068.1
            }
            initialised = true;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "initialise");
    }

    /**
     * Sets an AcceptListenerFactory on the full implementation of the ServerConnectionManager implementation.
     * 
     * @param _acceptListenerFactory
     */
    public static void initialiseAcceptListenerFactory(AcceptListenerFactory _acceptListenerFactory)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "initialiseAcceptListenerFactory", _acceptListenerFactory);

        Class clientImpl = instance.getClass();
        Method initialiseAcceptListenerFactoryMethod;

        try
        {
            initialiseAcceptListenerFactoryMethod = clientImpl.getMethod("initialiseAcceptListenerFactory", new Class[] { AcceptListenerFactory.class });
            initialiseAcceptListenerFactoryMethod.invoke(clientImpl, new Object[] { _acceptListenerFactory });
        } catch (Exception e)
        {
            FFDCFilter.processException(e, "com.ibm.ws.sib.jfapchannel.ServerConnectionManager.initialiseAcceptListenerFactory", JFapChannelConstants.SRVRCONNMGR_INITIALISE_ALF_01);

            //Make sure we allow for the fact this could be an InvocationTargetException
            Throwable displayedException = e;
            if (e instanceof InvocationTargetException)
                displayedException = e.getCause();

            SibTr.error(tc, "EXCP_DURING_INIT_SICJ0081", new Object[] { clientImpl, displayedException });
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.exception(tc, e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "initialiseAcceptListenerFactory");
    }

    /**
     * Returns a reference to the single instance of this class in existence.
     * The class must have been previously initilised by a call to the
     * "initialise" method - otherwise invoking this method will generate a
     * runtime exception.
     * This class implements the singleton design pattern.
     * 
     * @return ChannelFramework A reference to the only instance of this class
     *         which exists.
     */
    public static synchronized ServerConnectionManager getRef()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getRef");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getRef");
        return instance;
    }

    /**
     * Sends a close notification to all clients using this server. This is typically only useful at
     * application server shutdown time as this is the only point all connections can safely be
     * closed irrespective of the ME each individual conversation is talking to
     * <p>
     * It is worth noting that it is possible that two conversations to different ME's may be sharing
     * the same physical connection, hence it is implosible to issue this call at ME shutdown time.
     * <p>
     * This call is "advisory" - it's only affect is to notify a client that the server has request a
     * close. The server (and client) are still responsible for invoking close on individual
     * conversations to ensure the (eventual) closing of the underlying sockets.
     * <p>
     * From our peers perspective, the effect of issuing closeAll is for each Conversations
     * asynchronous receive listener to have its "closeReceive" method invoked.
     * 
     * @param quiesce
     * @throws CommsException
     */
    public abstract void closeAll(boolean quiesce);

    /**
     * Attempts a TCP connection to the specified ME. A new connection object
     * is returned. If successful, this method will return once a connection
     * has been established. The calling code needs to arrange for the initial
     * flows to take place.
     * <p>
     * The implementation of the conversation interface returned by this
     * method call (unlike that returned from the ClientConnectionManager)
     * will have the capability to manage capacity.
     * 
     * @param remoteHost The remote host to connect to.
     * @param conversationReceiveListener The asynchronous receive listener to
     *            be notified when a asynchronous request (ie. one we didn't
     *            immediately solicit) is received from our peer.
     * @param chainName The name of the CF chain to use to establish the connection.
     *            This should be taken from the constants provided by
     *            this class.
     * @return Conversation An object representing the conversation established.
     * 
     * @throws SIResourceException
     */
    public abstract Conversation connect(InetSocketAddress remoteHost, // F171173
                                         ConversationReceiveListener conversationReceiveListener, // F171173
                                         String chainName) // F171173
    throws SIResourceException; // F171173, // F174602

    // begin F182479
    /**
     * Attempts to establish an outbound network connection with the target
     * WLM end point. The end point data supplied is an opaque "cookie" which
     * is passed through the the Channel Framework.
     * <p>
     * The implementation of the conversation interface returned by this
     * method call (unlike that returned from the ClientConnectionManager)
     * will have the capability to manage capacity.
     * 
     * @param wlmEndPoint
     * @param conversationReceiveListener
     * @return Conversation
     * @throws SIResourceException
     */
    public abstract Conversation connect(CFEndPoint wlmEndPoint, // F189000
                                         ConversationReceiveListener conversationReceiveListener)
                    throws SIResourceException;

    // end F182479

    /**
     * Obtains a list of active outbound ME to ME conversations in this JVM.
     * 
     * @return Returns a list of Conversations
     */
    public abstract List getActiveOutboundMEtoMEConversations();
}
