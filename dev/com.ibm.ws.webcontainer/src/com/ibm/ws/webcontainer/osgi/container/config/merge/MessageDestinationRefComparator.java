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
package com.ibm.ws.webcontainer.osgi.container.config.merge;

import com.ibm.ws.javaee.dd.common.MessageDestinationRef;

/**
 *
 */
public class MessageDestinationRefComparator extends ResourceGroupComparator<MessageDestinationRef> {

    @Override
    public boolean compare(MessageDestinationRef o1, MessageDestinationRef o2) {
        if (!super.compare(o1, o2)) {
            return false;
        }
        if (o1.getType() == null) {
            if (o2.getType() != null)
                return false;
        } else if (!o1.getType().equals(o2.getType())) {
            return false;
        }
        
        if (o1.getLink() == null) {
            if (o2.getLink() != null)
                return false;
        } else if (!o1.getLink().equals(o2.getLink())) {
            return false;
        }
        
        if (o1.getUsageValue() != o2.getUsageValue()) {
                return false;
        }
        
        return compareDescriptions(o1.getDescriptions(), o2.getDescriptions());
    }

}
