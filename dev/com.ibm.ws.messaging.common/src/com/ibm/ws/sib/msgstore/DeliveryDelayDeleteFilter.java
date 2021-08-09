/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.msgstore;

/**
 * Prior to delivery delay feature when the destination is deleted,
 * the AsyncDeletionThread internally passes null as the filter and
 * used to delete only the available messages which are not expired.
 * 
 * But with the introduction of delivery delay feature items
 * locked for delivery delay also must be deleted
 * or reallocated to exception destination.
 * 
 * This filter is just used as a marker filter
 */
public class DeliveryDelayDeleteFilter implements Filter {

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.Filter#filterMatches(com.ibm.ws.sib.msgstore.AbstractItem)
     */
    @Override
    public boolean filterMatches(AbstractItem abstractItem) throws MessageStoreException {
        // TODO Auto-generated method stub
        return true;
    }

}
