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
/*
 * Change activity:
 *
 * Reason          Date     OOrigin   Description
 * --------------- -------- -------- --------------------------------------------
 *                 27/10/03 drphill  Original
 * 538096          24/07/08 susana   Use getInMemorySize for spilling & persistence                   
 * ============================================================================
 */
package com.ibm.ws.sib.msgstore.test.streamsize;

import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;

/**
 * @author DrPhill
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
final class Reference extends com.ibm.ws.sib.msgstore.ItemReference {
    private final int _size;
    
    Reference(int size) {
        super();
        _size = size;
    }
    
    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.AbstractItem#getPersistentDataSize()
     */
    public final int ggetInMemoryDataSize() {
        return _size * MessageStoreTestCase.ITEM_SIZE_MULTIPLIER;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.AbstractItem#getStorageStrategy()
     */
    public final int getStorageStrategy() {
        return STORE_NEVER;
    }

}
