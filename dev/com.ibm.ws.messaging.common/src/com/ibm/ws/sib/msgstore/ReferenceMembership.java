package com.ibm.ws.sib.msgstore;
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

/**
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public interface ReferenceMembership extends Membership 
{
    /*
     * @return owning referenceStream, or null if none. 
     */
    public ReferenceStream getOwningReferenceStream() throws SevereMessageStoreException;

    /*
     * This method is part of the interface between the MessageStoreInterface
     * component and the MessageStoreImplementation component.  It should 
     * only be used by MessageStore Code.<br> 
     * @return the ID of the referrenced {@link Item}
     */
    public long getReferencedID();

    /*
     * This method is part of the interface between the MessageStoreInterface
     * component and the MessageStoreImplementation component.  It should 
     * only be used by MessageStore Code.<br> 
     * @return the item 
     */
    public AbstractItem getItem() throws SevereMessageStoreException;
}
