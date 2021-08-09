/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.sessions;

import jain.protocol.ip.sip.message.Message;

import javax.servlet.sip.SipServletMessage;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.servlets.SipServletMessageImpl;

/**
 * 
 * @author Dedi Hirschfeld, Aug 11, 2003
 *
 * A session identifier.
 * 
 * TODO Rename to dialog id instead of session id. 
 */
public class SessionId
{
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SessionId.class);

    /**
     * Local Tag. 
     */
    private String m_localTag;

    /**
     * Remote Tag. 
     */
    private String m_remoteTag;

    /**
     * Call Id. 
     */
    private String m_callId;

    /**
     * Calculate a session ID for a session associated with this message.
     * @param 
     */
    public static SessionId fromInboundMessage(SipServletMessage message)
    {
        Message jainSIPMsg = ((SipServletMessageImpl)message).getMessage(); 
        return fromInboundMessage(jainSIPMsg);
    }
    
    
    /**
     * Calculate a session ID for a session associated with this message.
     * @param 
     */
    public static SessionId fromInboundMessage(Message message)
    {
        SessionId sessionId = null;

        String localTag =  message.getToHeader().getTag();
        String remoteTag = message.getFromHeader().getTag();

        // old clients might send us requests with no To tag
        if (null == localTag)
            localTag = "";

        // 3261-12.1.1
        // A UAS MUST be prepared to receive a
        // request without a tag in the From field, in which case the tag is
        // considered to have a value of null.
        if (null == remoteTag)
            remoteTag = "";

        String callId = message.getCallIdHeader().getCallId();
        sessionId = new SessionId(callId, localTag, remoteTag);

        if (c_logger.isTraceDebugEnabled())
        {
            StringBuffer b = new StringBuffer(64);
            b.append("Created ");
            b.append(sessionId);
            b.append(" \nFor Message: ");
            b.append(message.getStartLine());
            c_logger.traceDebug("SessionId", "fromMessage", b.toString());
        }

        return sessionId;
    }

    /**
     * Construct a new session identifier.  
     * @param callId
     * @param localParty Address ofthe local party
     * @param remoteParty Address ofthe remote party
     */
    public SessionId(
        String callId,
        String localPartyTag,
        String remotePartyTag)
    {
        m_callId = callId;
        m_localTag = localPartyTag;
        m_remoteTag = remotePartyTag;
    }

    /**
     * Test whether the given obj is a session ID equal to this one.
     */
    public boolean equals(Object obj)
    {
        boolean rc = false;
        if (obj instanceof SessionId)
        {
            SessionId other = (SessionId) obj;
            if (other.m_callId.equals(m_callId)
                && other.m_remoteTag.equals(m_remoteTag)
                && other.m_localTag.equals(m_localTag))
            {
                rc = true;
            }
        }

        return rc;
    }

    /**
     * Returns a hash code value for the object.
     */
    public int hashCode()
    {
        return m_callId.hashCode()
            ^ m_localTag.hashCode()
            ^ m_remoteTag.hashCode();
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer buffer = new StringBuffer(64);
        buffer.append("SessionId, CallId: ");
        buffer.append(m_callId);

        buffer.append(" ,Local  tag: ");
        buffer.append(m_localTag);

        buffer.append(" ,Remote tag: ");
        buffer.append(m_remoteTag);

        return buffer.toString();
    }

}
