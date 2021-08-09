package com.ibm.ws.sib.utils;
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class DataSlice implements java.io.Serializable
{
    private static final long serialVersionUID = 417882903364528383L;

    private byte[] _bytes;
    private int    _offset;
    private int    _length;

    /**
     * De-serialization constructor. This is required for when we store
     * DataSlice objects directly in the ObjectManager as a part of
     * a PersistableRawData object.
     */
    public DataSlice() {}

    /**
     * MessageStore constructor. Takes the raw byte stream chunk which
     * was decoded from the contents of the data on disc.
     * 
     * @param bytes
     */
    public DataSlice(byte[] bytes)
    {
        _bytes  = bytes;
        _offset = 0;

        // Defect 502272
        // We may get a null byte array passed to
        // us so we need to protect against it.
        if (_bytes != null)
        {
            _length = bytes.length;
        }
        else
        {
            _length = 0;
        }
    }

    /**
     * MFP constructor
     * 
     * @param bytes
     * @param offset
     * @param length
     */
    public DataSlice(byte[] bytes, int offset, int length)
    {
        _bytes  = bytes;
        _offset = offset;
        _length = length;
    }

    /*
     * Returns the byte array which contains this encoded message slice.
     */
    public byte[] getBytes()
    {
        return _bytes;
    }

    /*
     * Returns the offset within the byte array at which this encoded slice starts.
     */
    public int getOffset()
    {
        return _offset;
    }

    /*
     * Returns the length of the encoded slice of message.
     */
    public int getLength()
    {
        return _length;
    }

    /**
     * Compact implementation of toString() giving information about the
     * offset and length of data contained in the payload.
     * 
     * For a complete printout of the contained data use toPayloadString()
     * method instead.
     * 
     * @return 
     */
    public String toString()
    {
        StringBuilder retval = new StringBuilder("DataSlice@");

        retval.append(hashCode());
        retval.append("[");
        retval.append(_offset);
        retval.append("/");
        retval.append(_length);
        retval.append("]");

        return retval.toString();
    }

    /**
     * This method will output the contents of the slice designated
     * by the provided offset and length.
     * 
     * @return 
     */
    public String toPayloadString()
    {
        String digits = "0123456789ABCDEF";
        StringBuilder retval = new StringBuilder("DataSlice@");

        retval.append(hashCode());
        retval.append("[");

        for (int i=_offset; i<(_offset+_length); i++)
        {
            retval.append(digits.charAt((_bytes[i] >> 4) & 0xf));
            retval.append(digits.charAt(_bytes[i] & 0xf));
        }

        retval.append("]");

        return retval.toString();
    }

    /**
     * This is a CUSTOM serialisation method for the DataSlice
     * object. 
     * 
     * We only need to write the bytes that are part of the payload.
     * Any bytes that are not contained by the section defined by
     * the offset and length values are not stored to disk. The
     * length is also written for use upon reading the data from
     * disk.
     * 
     * @param out
     * 
     * @exception IOException
     */
    private void writeObject(ObjectOutputStream out) throws IOException
    {
        out.writeInt(_length);
        out.write(_bytes, _offset, _length);
    }

    /**
     * This is a CUSTOM serialisation method for the DataSlice
     * object.
     * 
     * As we have only written the data that we care about we can do
     * a simple read of the length of data and then directly read the
     * byte data into a pre-defined byte array.
     * 
     * @param in
     * 
     * @exception IOException
     * @exception ClassNotFoundException
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        // We never write any superflous data to disk so on a read
        // offset is always going to be set to 0.
        _offset = 0;
        _length = in.readInt();

        _bytes = new byte[_length];
        in.readFully(_bytes, _offset, _length);
    }
}
