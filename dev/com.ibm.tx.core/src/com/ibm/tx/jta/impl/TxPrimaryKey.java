package com.ibm.tx.jta.impl;
/*******************************************************************************
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import com.ibm.tx.TranConstants;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.Transaction.JTA.Util;

/**
 * Java object representation of the WS/390 OTS primary key.  This
 * object is to be used as the "primary key" to all transaction
 * related hash tables.  It can be easily extracted from any
 * interoperable transaction identifier (such as an otid or XID).
 */
public final class TxPrimaryKey
{
    /**
     * TraceComponent for this class.
     */
    private final static TraceComponent tc = Tr.register(TxPrimaryKey.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    /**
     * The eight byte value returned by the MVS STCK instruction.
     * System.currentTimeMillis for distributed.
     */
    final private long _timestamp;

    /**
     * The epoch number for the corresponding transaction.
     */
    final private int _epochNumber;

    /**
     * The sequence number for the corresponding transaction.
     */
    final private int _sequenceNumber;

    /**
     * Printable represntation of this object.
     */
    private String _stringValue;


    /**
     * Mainline z/OS constructor for new transactions.
     */
    public TxPrimaryKey(long epochAndSequence)                  /* @484128.5A*/
    {
        if (tc.isEntryEnabled())
        {
            Tr.entry(tc, "<init>", Long.toHexString(epochAndSequence));
        }

        _epochNumber = (int) (epochAndSequence >>> 32);
        _sequenceNumber = (int) (epochAndSequence & 0x00000000FFFFFFFFL);
        _timestamp = System.nanoTime();

        if (tc.isEntryEnabled())
        {
            Tr.exit(tc, "<init>", this);
        }
    }


    /**
     * Mainline constructor.  Build the object version of the key from
     * the input byte array.
     *
     * <pre>
     * struct search_key_t {
     *     double        timestamp;
     *     unsigned long epochNumber;
     *     unsigned long sequenceNumber;
     * };
     * </pre>
     */
    public TxPrimaryKey(byte[] keyData)
    {
        this(keyData, 0);
    }


    /**
     * Alternate mainline constructor.
     */
    TxPrimaryKey(byte[] keyData, int offset)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "<init>", new Object[] {
                keyData,
                new Integer(offset) } );

        final byte[] _keyData = keyData;

        _epochNumber = Util.getIntFromBytes(_keyData, 8, 4);
        _sequenceNumber = Util.getIntFromBytes(_keyData, 12, 4);
        _timestamp = 
            (((Util.getIntFromBytes(_keyData, 0, 4) & 0xFFFFFFFFL) << 32) |
             (Util.getIntFromBytes(_keyData, 4, 4) & 0xFFFFFFFFL));

        if (tc.isEntryEnabled()) Tr.exit(tc, "<init>", this);
    }


    /**
     * Alternate mainline constructor for distributed.
     */
    TxPrimaryKey(long sequence, int epoch)
    {
        this((int)sequence, epoch);
    }


    /**
     * Alternate mainline constructor for distributed.
     */
    TxPrimaryKey(int sequence, int epoch)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "<init>", new Object[] {
                new Integer(sequence),
                new Integer(epoch) } );

        _epochNumber = epoch;
        _sequenceNumber = sequence;
        _timestamp = System.currentTimeMillis();

        if (tc.isEntryEnabled()) Tr.exit(tc, "<init>", this);
    }

    /**
     * Get the epoch number from this key.
     */
    public int getEpochNumber()
    {
        return _epochNumber;
    }


    /**
     * Get the sequence number from this key.
     */
    public int getSequenceNumber()
    {
        return _sequenceNumber;
    }


    /**
     * Get the timestamp from this key.
     */
    public long getTimeStamp()
    {
        return _timestamp;
    }

    
    /**
     * Calculate an efficient hashCode for this primary key.
     */
    public int hashCode()
    {
        return _sequenceNumber;
    }


    /**
     * Build a native TxPrimaryKey suitable for recreating with the mainline
     * constructor.
     *
     * <pre>
     * struct search_key_t {
     *     double        timestamp;
     *     unsigned long epochNumber;
     *     unsigned long sequenceNumber;
     * };
     * </pre>
     */
    public byte[] toBytes()
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "toBytes");

        final byte[] result = new byte[16];

        final int t1 = (int)(_timestamp >> 32 & 0xFFFFFFFFL);
        final int t2 = (int)(_timestamp & 0xFFFFFFFFL);
        Util.setBytesFromInt(result, 0, 4, t1);
        Util.setBytesFromInt(result, 4, 4, t2);
        Util.setBytesFromInt(result, 8, 4, _epochNumber);
        Util.setBytesFromInt(result,12, 4, _sequenceNumber);

        if (tc.isEntryEnabled()) Tr.exit(tc, "toBytes", Util.toHexString(result));
        return result;
    }


    /**
     * Compares two XIDs to each other.  They are equal if the Format ID,
     * gtrid, and bqual are all identical.
     *
     * @param obj The object to compare this XID to.
     *
     * @return True if they are the same, false if not.
     */
    public boolean equals(Object obj)
    {
        if ((obj != null) && (obj instanceof TxPrimaryKey))
        {
            final TxPrimaryKey that = (TxPrimaryKey) obj;

            if ((this._timestamp == that._timestamp) &&
                (this._sequenceNumber == that._sequenceNumber) &&
                (this._epochNumber == that._epochNumber))
            return true;
        }
        return false;
    }


    /**
     * Generate a pretty String representation of this TxPrimaryKey.
     */
    public String toString()
    {
        if (_stringValue == null)
        {
            _stringValue = super.toString() + ";" +
                Long.toHexString(_timestamp).toUpperCase() + ":" +
                Integer.toHexString(_epochNumber).toUpperCase() + ":" +
                Integer.toHexString(_sequenceNumber).toUpperCase();
        }

        return _stringValue;
    }
}
