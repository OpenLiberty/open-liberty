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
package com.ibm.ws.sib.msgstore.test.transactions;
/*
 * Change activity:
 *
 * Reason            Date   Origin   Description
 * --------------- -------- -------- ------------------------------------------
 * 184032          27/11/03 gareth   Change indoubt Xid storage method 
 * 272110          10/05/05 schofiel 602:SVT: Malformed messages bring down the AppServer
 * SIB0112b.ms.1   07/08/06 gareth   Large message support.
 * ============================================================================
 */

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.utils.DataSlice;

public class NullItem extends Item
{
    String _data;

    public NullItem() {}

    public NullItem(String data)
    {
        _data = data;
    }

    public List<DataSlice> getPersistentData()
    {
        List<DataSlice> list = new ArrayList<DataSlice>(1);
        list.add(new DataSlice(_data.getBytes()));
        return list;
    }

    public int getStorageStrategy()
    {
        return STORE_ALWAYS;
    }
}



