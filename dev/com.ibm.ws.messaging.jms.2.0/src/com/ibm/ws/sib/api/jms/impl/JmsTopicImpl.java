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
import com.ibm.websphere.sib.api.jms.JmsTopic;
import com.ibm.ws.sib.utils.ras.SibTr;

public class JmsTopicImpl extends JmsDestinationImpl implements JmsTopic, ResourceAdapterAssociation
{
    private static final long serialVersionUID = -901264079384537654L;

    // ************************** TRACE INITIALISATION ***************************

    private static TraceComponent tc = SibTr.register(JmsTopicImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);

    // ******************************* CONSTANTS *********************************

    public static final String TOPIC_STEM = "topic";
    public static final String TOPIC_PREFIX = TOPIC_STEM + JmsDestinationImpl.NAME_SEPARATOR;

    //The name of the default topic space.
    public static final String DEFAULT_TOPIC_SPACE = "Default.Topic.Space";

    private transient ResourceAdapter _resourceAdapter;

    // ***************************** CONSTRUCTORS ********************************

    public JmsTopicImpl() throws JMSException {
        super();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "JmsTopicImpl");
        // Set up any topic-defaults here.
        setDestName(DEFAULT_TOPIC_SPACE);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "JmsTopicImpl");
    }

    public JmsTopicImpl(Reference reference) {
        super(reference);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "JmsTopicImpl", reference);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "JmsTopicImpl");
    }

    // *************************** INTERFACE METHODS *****************************

    /**
     * CAUTION This method is used both to implement the JMS getTopicName method
     * and to provide a java bean accessor for the property 'topicName'. This is
     * ok provided that both functions can be met by mapping to JmsDestinationImpl.getDestDiscrim().
     * 
     * @see javax.jms.Topic#getTopicName()
     * @see JmsTopicImpl#setTopicName(String)
     */
    @Override
    public String getTopicName() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getTopicName");
        String topicName = getDestDiscrim();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getTopicName", topicName);
        return topicName;
    }

    /**
     * Set the topicName.
     * 
     * @param tName The name of the topic
     * @throws JMSException If setDestDiscrim throws one.
     * @see JmsDestinationImpl#setDestDiscrim
     */
    @Override
    public void setTopicName(String tName) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setTopicName", tName);
        setDestDiscrim(tName);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setTopicName");
    }

    /**
     * Get the topicSpace.
     * 
     * @return the topicSpace
     * @see JmsDestinationImpl#getDestName
     */
    @Override
    public String getTopicSpace() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getTopicSpace");
        String result = getDestName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getTopicSpace", result);
        return result;
    }

    /**
     * Set the topicSpace
     * 
     * @param tSpace
     * @throws JMSException
     * @see JmsDestinationImpl#setDestName
     */
    @Override
    public void setTopicSpace(String tSpace) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setTopicSpace", tSpace);
        setDestName(tSpace);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setTopicSpace");
    }

    @Override
    public ResourceAdapter getResourceAdapter() {

        return _resourceAdapter;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter arg0) throws ResourceException {
        _resourceAdapter = arg0;

    }

}
