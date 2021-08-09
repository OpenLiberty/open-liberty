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
package com.ibm.ws.sib.msgstore.test.persistence.objectManager;
/*
 * Change activity:
 *
 *  Reason         Date     Origin   Description
 * --------------- -------- -------- ------------------------------------------
 *  327709         06/12/05 gareth   Output NLS messages when OM files are full
 * SIB0112b.ms.1   07/08/06 gareth   Large message support.
 * 538096          24/07/08 susana   Use getInMemorySize for spilling & persistence
 * ============================================================================
 */

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.utils.DataSlice;

public class OneMegItem extends Item
{
    private byte _data[];

    public OneMegItem()
    {
        super();

        _data = new byte[1024*1024];
    }

    public List<DataSlice> getPersistentData() 
    {
        List<DataSlice> list = new ArrayList<DataSlice>(1);
        list.add(new DataSlice(_data));
        return list;
    }

    public int getInMemoryDataSize()
    {
        if (_data != null)
        {
            return _data.length * MessageStoreTestCase.ITEM_SIZE_MULTIPLIER;
        }
        else
        {
            return 0;
        }
    }

    public int getStorageStrategy()
    {
        return STORE_ALWAYS;
    }

    public void restore(final List<DataSlice> dataSlices) 
    {
        DataSlice slice = dataSlices.get(0);
        _data = slice.getBytes();
    }

    public String toString()
    {
        return super.toString()+"(OneMegItem)";
    }
}
