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
package com.ibm.ws.sib.msgstore.test;
/*
 * Change activity:
 *
 * Reason      Date    Origin   Description
 * ----------  ------  -------  -------------------------------------------
 * 492055      270504  susana   Moved from TestDump
 * ============================================================================
 */
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.PersistentDataEncodingException;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.FormattedWriter;

public class MyItem extends Item
{
    long expiryInterval = 0;
    String name = "NONE";
    int expired = 0;
    int priority = 5;

    public MyItem()
    {
        super();
    }

    MyItem(String name, long interval, int priority)
    {
        super();
        expiryInterval = interval;
        this.name = name;
        this.priority = priority;
    }

    public int getPriority()
    {
        return priority;
    }

    public long getMaximumTimeInStore() {return expiryInterval;}
    public long getExpiryStartTime() {return 0;}
    public boolean canExpireSilently() {return true;}

    public int getStorageStrategy() {
      return STORE_ALWAYS;
    }

    public List<DataSlice> getPersistentData() throws PersistentDataEncodingException {
      byte[] data = name.getBytes();
      List<DataSlice> slices = new ArrayList<DataSlice>();
      slices.add(new DataSlice(data, 0, data.length));
      return slices;
    }

    public void restore(List<DataSlice> slices) throws PersistentDataEncodingException {
      name = new String(slices.get(0).getBytes());
    }

    public void xmlWriteOn(FormattedWriter writer) throws IOException {
      writer.newLine();
      writer.write(toString());
    }

    public String toString() {
      return name;
    }
}
