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
 * Reason          Date        Origin       Description
 * --------------- ----------  -----------  --------------------------------------------
 *                 27/10/2003  van Leersum  Original
 * ============================================================================
 */
package com.ibm.ws.sib.msgstore.test.cursor.nonlocking;

/**
 * @author DrPhill
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Item extends com.ibm.ws.sib.msgstore.Item {
    private static int _nextSequence = 0; 
    public static final void resetCounter() {
        _nextSequence = 0;
    }
    private final int _mySequence = _nextSequence++;

    private int _priority = 0;
    private int _storageStrategy = STORE_NEVER;
    private boolean _marked = false;
    /**
     * 
     */
    public Item() {
        super();
    }

    /**
     * 
     */
    public Item(int priority) {
        super();
        _priority = priority;
    }
    /**
     * @return
     */
    public int getMySequence() {
        return _mySequence;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.AbstractItem#getPriority()
     */
    public int getPriority() {
        return _priority;
    }


    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.AbstractItem#getStorageStrategy()
     */
    public final int getStorageStrategy() {
        return _storageStrategy;
    }

    public final boolean isAfter(Item item) {
        boolean after = false;
        final int itemPriority = item.getPriority(); 
        if (itemPriority > _priority) {
            after = true;
        } else if (itemPriority == _priority) {
            if (item.getMySequence() < _mySequence) {
                after = true;
            }
        }
        return after;
    }

    public final void setStoreAlways() {
        _storageStrategy = STORE_ALWAYS;
    }
    
    /**
     * @return
     */
    public final boolean isMarked() {
        return _marked;
    }

    /**
     * @param b
     */
    public final void setMarked(boolean b) {
        _marked = b;
    }

}
