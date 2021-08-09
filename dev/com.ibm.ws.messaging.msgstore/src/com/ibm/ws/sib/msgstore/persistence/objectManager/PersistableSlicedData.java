package com.ibm.ws.sib.msgstore.persistence.objectManager;
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.objectManager.ManagedObject;
import com.ibm.ws.objectManager.ObjectManagerException;
import com.ibm.ws.objectManager.ObjectManagerState;
import com.ibm.ws.objectManager.SimplifiedSerialization;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.SibTr;


public class PersistableSlicedData extends ManagedObject implements SimplifiedSerialization
{
    private static final long serialVersionUID = 1876240915914565653L;

    private static TraceComponent tc = SibTr.register(PersistableSlicedData.class, 
                                                      MessageStoreConstants.MSG_GROUP, 
                                                      MessageStoreConstants.MSG_BUNDLE);

    private List<DataSlice> _dataSlices;
    private long            _estimatedLength = -1;

    // Defect 453327
    // To get an accurate size for a serialized version of the object 
    // before our custom serialization takes place we can instantiate
    // an empty object and ask the ManagedObject implementation to 
    // calculate its serialized size.
    private static long _estimatedLengthHeader; 
    static 
    {
        try
        {
            _estimatedLengthHeader =  new PersistableSlicedData().getSerializedBytesLength();

            // Lets give ourselves some legroom in case this 
            // value isn't as accurate as we hoped. It is better
            // to "waste" 64 bytes here than under-estimate on a 
            // 100MB message and end up allocating a 200MB array.
            _estimatedLengthHeader += 64;
        }
        catch (Exception e)
        {
            // No FFDC Code Needed.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "Exception caught initialising _estimatedLengthHeader!", e);
            // If we can't get an accurate number then lets give a number 
            // that if anything is a bit of an over estimation as this should 
            // hopefully avoid us under-sizing the array created at serialize
            // time and cut down on re-allocations.
            _estimatedLengthHeader = 150;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "_estimatedLengthHeader="+_estimatedLengthHeader);
    } // static initializer. 


    public void setData(List<DataSlice> dataSlices)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setData", "DataSlices="+dataSlices);

        _dataSlices = dataSlices;

        _estimatedLength = -1;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setData");
    }

    public List<DataSlice> getData()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getData");
            SibTr.exit(this, tc, "getData", "return="+_dataSlices);
        }
        return _dataSlices;
    }

    /**
     * Replace the state of this object with the same object in some other state.
     * Used for to restore the before image if a transaction rolls back.
     * @param other is the object this object is to become a clone of.
     */
    public void becomeCloneOf(ManagedObject clone)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "becomeCloneOf", "Clone="+clone);

        PersistableSlicedData object = (PersistableSlicedData)clone;

        _dataSlices = object._dataSlices;

        _estimatedLength = -1;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "becomeCloneOf");
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer(super.toString());

        // Defect 292187
        // don't output the byte array on toString as this causes
        // excessive output in trace.
        buffer.append("(PersistableSlicedData[ BINARYDATA ])");

        return buffer.toString();
    }

    /**
     * 
     * @return The combined length of all DataSlice objects contained
     *         in this object.
     */
    public long estimatedLength() 
    {
        if (_estimatedLength == -1)
        {
            // Defect 453327
            // Start with the value for the header that our
            // superclass will create and then add on the size
            // for our header:
            //    Superclass header
            //  + SerialVersionUID:   long = 8 bytes
            //  + DataSlice count:    int  = 4 bytes
            _estimatedLength = _estimatedLengthHeader + 12;

            if (_dataSlices != null)
            {
                for (DataSlice slice : _dataSlices)
                {
                    // Add 4 bytes for the int used to store the
                    // length of the slice.
                    _estimatedLength += 4;

                    // Add the slice length.
                    _estimatedLength += slice.getLength();
                }
            }
        }
        return _estimatedLength; 
    }

    /**
     * Simplified serialization.
     * 
     * @param dataOutputStream
     *               to write the serialized data to.
     * 
     * @exception ObjectManagerException
     * @exception IOException
     */
    public void writeObject(DataOutputStream dataOutputStream) throws ObjectManagerException, IOException
    {
        super.writeObject(dataOutputStream);

        // Write out our serialVersionUID
        dataOutputStream.writeLong(serialVersionUID);

        if (_dataSlices != null)
        {
            // Write the number of slices we have
            dataOutputStream.writeInt(_dataSlices.size());
    
            // Write each slice out
            for (DataSlice slice : _dataSlices)
            {
                dataOutputStream.writeInt(slice.getLength());
                dataOutputStream.write(slice.getBytes(), slice.getOffset(), slice.getLength());
            }
        }
        else
        {
            // We have no slices so just write a 
            // zero so we don't try to read any in
            // at readObject time.
            dataOutputStream.writeInt(0);
        }
    }

    /**
     * Simplified deserialization.
     * 
     * @param dataInputStream
     *               containing the serialized Object.
     * @param objectManagerState
     *               of the objectManager reconstructing the serialized Object.
     * 
     * @exception ObjectManagerException
     * @exception IOException
     */
    public void readObject(DataInputStream dataInputStream, ObjectManagerState objectManagerState) throws ObjectManagerException, IOException
    {
        super.readObject(dataInputStream, objectManagerState);

        // Read in the serialVersionUID
        dataInputStream.readLong();

        // Read in the number of slices we have
        int count = dataInputStream.readInt();

        _dataSlices = new ArrayList<DataSlice>(count);

        for (int i = 0; i < count; i++)
        {
            int length = dataInputStream.readInt();

            byte[] bytes = new byte[length];

            dataInputStream.readFully(bytes, 0, length);

            DataSlice slice = new DataSlice(bytes, 0, length);

            _dataSlices.add(slice);
        }
    }
}

