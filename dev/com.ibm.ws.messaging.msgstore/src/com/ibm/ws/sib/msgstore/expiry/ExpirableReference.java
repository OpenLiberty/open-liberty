package com.ibm.ws.sib.msgstore.expiry;
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

import java.lang.ref.SoftReference;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.MessageStoreConstants; 
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Defines the ExpirableReference which is a SoftReference to an
 * Item which contains an expiry time. ExpirableReferences are
 * used to populate the ExpiryIndex.
 */
public class ExpirableReference extends SoftReference
{
    private static TraceComponent tc = SibTr.register(ExpirableReference.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);
    private long expiryTime = 0;
    private long objectID = 0;

    /**
     * Constructor to create the ExpiryReference for the Item. Sets the expiry time to zero.
     * @param expirable The Item for which the ExpiryReference is required.
     * @throws MessageStoreRuntimeException 
     */ 
    public ExpirableReference(Expirable expirable) throws SevereMessageStoreException
    {
        super(expirable);

        if (expirable != null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", "id="+expirable.expirableGetID());
    
            expiryTime = 0;
            objectID   = expirable.expirableGetID();
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", "null");
        }

        if (objectID == AbstractItem.NO_ID)
        {
            // This item is not a member of a stream and therefore the ID is not unique.
        	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
            throw new SevereMessageStoreException("DUPLICATE_EXPIRABLE_SIMS2000");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
    }  

    /**
     * Return the expiry time.
     * @return the expiry time in milliseconds.
     */   
    public long getExpiryTime()
    {
        return expiryTime;
    }

    /**
     * Return the object ID.
     * @return the ID of the object.
     */
    public long getID()
    {
        return objectID;
    }

    /**
     * Set the expiry time.
     * @param expiryTime the expiry time in milliseconds.
     */
    public void setExpiryTime(long expiryTime)
    {
        this.expiryTime = expiryTime;
    }
}
