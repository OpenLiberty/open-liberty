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
 * Reason          Date     Origin   Description
 * --------------- -------- -------- ------------------------------------------
 *                 27/10/03 drphill  Original
 * 272110          10/05/05 schofiel 602:SVT: Malformed messages bring down the AppServer
 * SIB0112b.ms.1   07/08/06 gareth   Large message support.
 * 515543.2        08/07/08 gareth   Change runtime exceptions to caught exception
 * 538096          24/07/08 susana   Use getInMemorySize for spilling & persistence
 * ============================================================================
 */

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.sib.msgstore.PersistentDataEncodingException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.utils.DataSlice;

public final class Item extends com.ibm.ws.sib.msgstore.Item
{
    private int _size;

    private int _storageStrategy;

    public Item() {}

    public Item(int storageStrategy, int size)
    {
        super();
        _storageStrategy = storageStrategy;
        _size = size;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.AbstractItem#getPersistentData()
     */
    public List<DataSlice> getPersistentData()
    {
        byte[] data = new byte[_size];
        data[0] = (byte)_storageStrategy;

        List<DataSlice> list = new ArrayList<DataSlice>(1);
        list.add(new DataSlice(data));
        return list;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.AbstractItem#getInMemoryDataSize()
     */
    public final int getInMemoryDataSize()
    {
        return _size * MessageStoreTestCase.ITEM_SIZE_MULTIPLIER;
    }

    /**
     * @return
     */
    public int getSize()
    {
        return _size;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.AbstractItem#getStorageStrategy()
     */
    public final int getStorageStrategy()
    {
        return _storageStrategy;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.AbstractItem#restore(byte[])
     */
    public void restore(List<DataSlice> dataSlices) throws PersistentDataEncodingException, SevereMessageStoreException 
    {
        DataSlice slice = dataSlices.get(0);
        _size = slice.getLength();
        _storageStrategy = slice.getBytes()[0];
        super.restore(dataSlices);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return "Item(size=" + _size + ", strat="+_storageStrategy+")";
    }
}
