/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.MessageDestinationRef;

/**
 *
 */
public class MessageDestinationRefImpl extends AbstractResourceGroup implements MessageDestinationRef {

    private List<Description> descriptions;

    private String type;
    private String link;
    private int usageValue;

    public MessageDestinationRefImpl(MessageDestinationRef messageDestRef) {
        super(messageDestRef);
        this.descriptions = new ArrayList<Description>(messageDestRef.getDescriptions());
        this.type = messageDestRef.getType();
        this.usageValue = messageDestRef.getUsageValue();
        this.link = messageDestRef.getLink();
    }

    /** {@inheritDoc} */
    @Override
    public List<Description> getDescriptions() {
        return Collections.unmodifiableList(descriptions);
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.javaee.dd.common.MessageDestinationRef#getType()
     */
    @Override
    public String getType() {
        return type;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.javaee.dd.common.MessageDestinationRef#getUsageValue()
     */
    @Override
    public int getUsageValue() {
         return usageValue;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.javaee.dd.common.MessageDestinationRef#getLink()
     */
    @Override
    public String getLink() {
        return link;
    }
}
