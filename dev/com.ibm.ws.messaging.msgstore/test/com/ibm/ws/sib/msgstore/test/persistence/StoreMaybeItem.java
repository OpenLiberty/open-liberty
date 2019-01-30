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
package com.ibm.ws.sib.msgstore.test.persistence;
/*
 * Change activity:
 *
 *  Reason         Date     Origin   Description
 * --------------- -------- -------- ------------------------------------------
 * SIB0112i.ms.1   22/02/07 gareth   Changes to handling of STORE_MAYBE Items
 * SIB0112d.ms.2   04/07/07 gareth   MemMgmt: SpillDispatcher improvements - datastore
 * 496154          11/04/08 gareth   Spilling performance improvements
 * 538096          24/07/08 susana   Use getInMemorySize for spilling & persistence
 * ============================================================================
 */

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.sib.msgstore.*;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.utils.DataSlice;

public class StoreMaybeItem extends Item
{
    private byte[] _data;

    public StoreMaybeItem()
    {
        super();

        _data = new byte[0];
    }

    // Defect 496154
    // New constructor allows us to have items with different payload
    // sizes. This should help us make sure that spill batch size limits 
    // are triggered.
    public StoreMaybeItem(int size)
    {
        super();

        _data = new byte[size];
    }

    // Defect 496154
    // Allows us to modify the data in the Item so that updates
    // can be tested.
    public void setPersistentData(byte[] data)
    {
        _data = data;
    }

    public List<DataSlice> getPersistentData() 
    {
        List<DataSlice> list = new ArrayList<DataSlice>(1);
        list.add(new DataSlice(_data));
        return list;
    }

    public int getInMemoryDataSize() 
    {
        return _data.length * MessageStoreTestCase.ITEM_SIZE_MULTIPLIER;
    }

    public int getStorageStrategy()
    {
        return STORE_MAYBE;
    }

    public void restore(final List<DataSlice> dataSlices) 
    {
        DataSlice slice = dataSlices.get(0);
        _data = slice.getBytes();
    }

    public String toString()
    {
        return super.toString()+"(StoreMaybeItem)";
    }
}

