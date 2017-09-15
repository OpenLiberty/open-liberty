/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.api.jms.impl;

import javax.jms.JMSException;
import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.websphere.sib.api.jms.JmsQueue;
import com.ibm.ws.sib.utils.ras.SibTr;

public class JmsQueueImpl extends JmsDestinationImpl implements JmsQueue, ResourceAdapterAssociation
{

    private static final long serialVersionUID = -5199495499235437304L;

    // ************************** TRACE INITIALISATION ***************************

    private static TraceComponent tc = SibTr.register(JmsQueueImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);

    // ******************************* CONSTANTS *********************************

    public static final String QUEUE_PREFIX = "queue" + JmsDestinationImpl.NAME_SEPARATOR;

    private transient ResourceAdapter _resourceAdapter;

    // ***************************** CONSTRUCTORS ********************************

    public JmsQueueImpl() {
        super();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    public JmsQueueImpl(Reference reference)
    {
        super(reference);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", reference);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    // *************************** INTERFACE METHODS *****************************

    /**
     * CAUTION This method is used both to implement JMS getQueueName
     * and also to provide a Java bean accessor for the property
     * 'queueName'. Thiis is ok provided that both functions can be
     * met by mapping to JmsDestinationImpl.getDestName().
     * 
     * @see javax.jms.Queue#getQueueName()
     * @see JmsQueueImpl#setQueueName
     */
    @Override
    public String getQueueName() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getQueueName");

        String queueName = getDestName();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getQueueName", queueName);
        return queueName;
    }

    /**
     * Set the QueueName.
     * Note that this method is used to provide a Java Bean interface to
     * the property 'queueName', and is not a JMS method as such.
     * 
     * @param qName
     * @throws JMSException
     */
    @Override
    public void setQueueName(String qName) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setQueueName", qName);

        setDestName(qName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setQueueName");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.sib.api.jms.JmsQueue#setGatherMessages(java.lang.String)
     */
    @Override
    public void setGatherMessages(String gatherMessages) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setGatherMessages", gatherMessages);

        // Make sure null and empty are treated equally.
        if ((gatherMessages == null) || ("".equals(gatherMessages.trim())))
            gatherMessages = null;

        if ((gatherMessages == null) ||
            (ApiJmsConstants.GATHER_MESSAGES_OFF.equals(gatherMessages) ||
            (ApiJmsConstants.GATHER_MESSAGES_ON.equals(gatherMessages)))) {

            updateProperty(GATHER_MESSAGES, gatherMessages);
        }
        else {
            // bad value, throw exception
            String key = "INVALID_VALUE_CWSIA0281";
            Object[] inserts = new Object[] { GATHER_MESSAGES, gatherMessages };
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class, key, inserts, tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setGatherMessages");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.sib.api.jms.JmsQueue#getGatherMessages()
     */
    @Override
    public String getGatherMessages() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getGatherMessages");

        String gatherMessages = (String) properties.get(GATHER_MESSAGES);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getGatherMessages");
        return gatherMessages;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.sib.api.jms.JmsQueue#setProducerBind(java.lang.String)
     */
    @Override
    public void setProducerBind(String bind) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setProducerBind", bind);

        // Make sure null and empty are treated equally.
        if ((bind == null) || ("".equals(bind.trim())))
            bind = null;

        if ((bind == null) ||
            (ApiJmsConstants.PRODUCER_BIND_OFF.equals(bind) ||
            (ApiJmsConstants.PRODUCER_BIND_ON.equals(bind)))) {

            updateProperty(PRODUCER_BIND, bind);
        }
        else {
            // bad value, throw exception
            String key = "INVALID_VALUE_CWSIA0281";
            Object[] inserts = new Object[] { PRODUCER_BIND, bind };
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class, key, inserts, tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setProducerBind");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.sib.api.jms.JmsQueue#getProducerBind()
     */
    @Override
    public String getProducerBind() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getProducerBind");

        String producerBind = (String) properties.get(PRODUCER_BIND);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getProducerBind", producerBind);
        return producerBind;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.sib.api.jms.JmsQueue#setProducerPreferLocal(java.lang.String)
     */
    @Override
    public void setProducerPreferLocal(String preferLocal) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setProducerPreferLocal", preferLocal);

        // Make sure null and empty are treated equally.
        if ((preferLocal == null) || ("".equals(preferLocal.trim())))
            preferLocal = null;

        if ((preferLocal == null) ||
            (ApiJmsConstants.PRODUCER_PREFER_LOCAL_OFF.equals(preferLocal) ||
            (ApiJmsConstants.PRODUCER_PREFER_LOCAL_ON.equals(preferLocal)))) {

            updateProperty(PRODUCER_PREFER_LOCAL, preferLocal);
        }
        else {
            // bad value, throw exception
            String key = "INVALID_VALUE_CWSIA0281";
            Object[] inserts = new Object[] { PRODUCER_PREFER_LOCAL, preferLocal };
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class, key, inserts, tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setProducerPreferLocal");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.sib.api.jms.JmsQueue#getProducerPreferLocal()
     */
    @Override
    public String getProducerPreferLocal() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getProducerPreferLocal");

        String producerPreferLocal = (String) properties.get(PRODUCER_PREFER_LOCAL);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getProducerPreferLocal", producerPreferLocal);
        return producerPreferLocal;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.sib.api.jms.JmsQueue#setScopeToLocalQP(java.lang.String)
     */
    @Override
    public void setScopeToLocalQP(String scopeToLocalQP) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setScopeToLocalQP", scopeToLocalQP);

        // Clear the cached destinationAddresses, as a change to this value would make them out-of-date
        clearCachedProducerDestinationAddress();
        clearCachedConsumerDestinationAddress();

        // Make sure null and empty are treated equally.
        if ((scopeToLocalQP == null) || ("".equals(scopeToLocalQP.trim())))
            scopeToLocalQP = null;

        if ((scopeToLocalQP == null) ||
            (ApiJmsConstants.SCOPE_TO_LOCAL_QP_OFF.equals(scopeToLocalQP) ||
            (ApiJmsConstants.SCOPE_TO_LOCAL_QP_ON.equals(scopeToLocalQP)))) {

            updateProperty(SCOPE_TO_LOCAL_QP, scopeToLocalQP);
        }
        else {
            // bad value, throw exception
            String key = "INVALID_VALUE_CWSIA0281";
            Object[] inserts = new Object[] { SCOPE_TO_LOCAL_QP, scopeToLocalQP };
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class, key, inserts, tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setScopeToLocalQP");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.sib.api.jms.JmsQueue#getScopeToLocalQP()
     */
    @Override
    public String getScopeToLocalQP() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getScopeToLocalQP");

        String scopeToLocalQP = (String) properties.get(SCOPE_TO_LOCAL_QP);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getScopeToLocalQP", scopeToLocalQP);
        return scopeToLocalQP;
    }

    // ************************* IMPLEMENTATION METHODS **************************

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jms.impl.JmsDestinationImpl#isLocalOnly()
     */
    @Override
    protected boolean isLocalOnly() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isLocalOnly");
        boolean ret = false;

        String scopeToLocalQP = (String) properties.get(SCOPE_TO_LOCAL_QP);
        if (ApiJmsConstants.SCOPE_TO_LOCAL_QP_ON.equals(scopeToLocalQP)) {
            ret = true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isLocalOnly", ret);
        return ret;
    }

    @Override
    public ResourceAdapter getResourceAdapter() {

        return _resourceAdapter;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter resourceAdapter) throws ResourceException {
        _resourceAdapter = resourceAdapter;

    }

}
