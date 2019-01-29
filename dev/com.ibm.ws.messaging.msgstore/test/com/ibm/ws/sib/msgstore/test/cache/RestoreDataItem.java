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
package com.ibm.ws.sib.msgstore.test.cache;
/*
 * Change activity:
 *
 *  Reason         Date     Origin   Description
 * --------------- -------- -------- ------------------------------------------
 * SIB0112le.ms.1  07/02/07 gareth   Add restoreData() method to Item
 * 538096          24/07/08 susana   Use getInMemorySize for spilling & persistence
 * ============================================================================
 */

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.utils.DataSlice;

public class RestoreDataItem extends Item
{
    private byte _data[];

    public RestoreDataItem()
    {
        super();
    }

    public RestoreDataItem(String data)
    {
        super();

        if (data != null)
        {
            _data = data.getBytes();
        }
    }

    public List<DataSlice> getPersistentData() 
    {
        List<DataSlice> list = new ArrayList<DataSlice>(1);
        list.add(new DataSlice(_data));
        return list;
    }

    public int  getInMemoryDataSize()
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

    public void releaseData()
    {
        _data = null;
    }

    public void restore(final List<DataSlice> dataSlices) 
    {
        DataSlice slice = dataSlices.get(0);
        _data = slice.getBytes();
    }

    public String toString()
    {
        StringBuffer out = new StringBuffer(super.toString());

        out.append("(");
        if (_data != null)
        {
            out.append(new String(_data));
        }
        out.append(")");

        return out.toString();
    }
}

