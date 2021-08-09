package com.ibm.ws.sib.transactions;
/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import javax.transaction.xa.Xid;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.sib.utils.Base64Utils;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class is used as an interface between a transaction manager
 * and the MessageStores datastore. In one direction it allows the MS 
 * to write an XID to the datastore in an efficient format. In the 
 * other direction it allows the MS to construct a list of XIDs from
 * the data in the datastore no matter how it was stored.
 * 
 * Note: It is invalid to have a null for any parts of the XID so
 *       all checks that catered for that possibility have been removed
 *       to improve performance. Any presence of null will now cause NPEs
 */
public class PersistentTranId implements Xid
{
  
    private static final TraceNLS nls = TraceNLS.getTraceNLS(Constants.MSG_BUNDLE);
    private static TraceComponent tc = SibTr.register(PersistentTranId.class, 
                                                      Constants.MSG_GROUP, 
                                                      Constants.MSG_BUNDLE);

    private int _hashCode = -1;
    private final int _formatId;
    private final byte[] _gtrid;
    private final byte[] _bqual;
    private String _persistentData = null;


    /**
     * Constructor used for local transaction PersistentTranIds
     * 
     * @param tranid The local transaction id
     */
    public PersistentTranId(int tranid)
    {
        if (tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", "TranId="+tranid);

        _formatId = tranid;
        _hashCode = tranid;
        _gtrid    = intToBytes(tranid);
        _bqual    = intToBytes(tranid);

        if (tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
    }

    public PersistentTranId(int formatId, byte[] gtrid, byte[] bqual)
    {
        if (tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[]{"FormatId="+formatId, "GTRID="+Base64Utils.encodeBase64(gtrid), "BQUAL="+Base64Utils.encodeBase64(bqual)});

        _formatId = formatId;
        _gtrid    = gtrid;
        _bqual    = bqual;

        // Defect 372178
        // If the bqual is long enough then we should extract the 
        // sequenceNumber from it to use as the hash code. This is 
        // basically a local tranId that is inserted in the Xid by
        // the tran manager so is different for each tran.
        if (_bqual.length > 16)
        {
            _hashCode = ((_bqual[12]&0xff) << 24) +
                        ((_bqual[13]&0xff) << 16) +
                        ((_bqual[14]&0xff) << 8) +
                        (_bqual[15]&0xff);
        }
        else if (_bqual.length > 3)
        {
            _hashCode = ((_bqual[_bqual.length - 4]&0xff) << 24) +
                        ((_bqual[_bqual.length - 3]&0xff) << 16) +
                        ((_bqual[_bqual.length - 2]&0xff) << 8) +
                        (_bqual[_bqual.length - 1]&0xff);
        }

        if (tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
    }

    /**
     * Constructor to create a PersistentTranId from an 
     * existing XID.
     * 
     * @param xid    The Xid to convert into MS persistent format
     */
    public PersistentTranId(Xid xid)
    {
        if (tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", "Xid="+xid);

        _formatId = xid.getFormatId();
        _gtrid    = xid.getGlobalTransactionId();
        _bqual    = xid.getBranchQualifier();

        // Defect 372178
        // If the bqual is long enough then we should extract the 
        // sequenceNumber from it to use as the hash code. This is 
        // basically a local tranId that is inserted in the Xid by
        // the tran manager so is different for each tran.
        if (_bqual.length > 16)
        {
            _hashCode = ((_bqual[12]&0xff) << 24) +
                        ((_bqual[13]&0xff) << 16) +
                        ((_bqual[14]&0xff) << 8) +
                        (_bqual[15]&0xff);
        }
        else if (_bqual.length > 3)
        {
            _hashCode = ((_bqual[_bqual.length - 4]&0xff) << 24) +
                        ((_bqual[_bqual.length - 3]&0xff) << 16) +
                        ((_bqual[_bqual.length - 2]&0xff) << 8) +
                        (_bqual[_bqual.length - 1]&0xff);
        }

        if (tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
    }

    /**
     * Construct a new MS Xid from the byte array returned from 
     * our persistent store.
     * 
     * @param bytes  The peristent data retrieved from the database.
     */
    public PersistentTranId(byte[] bytes)
    {
        if (tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", "Byte Data="+Base64Utils.encodeBase64(bytes));

        byte[] tempint = new byte[4];
        System.arraycopy(bytes, 0, tempint, 0, 4);
        _formatId = bytesToInt(tempint);

        // We are rebuilding a global tran XID
        System.arraycopy(bytes, 4, tempint, 0, 4);
        int gtridlen = bytesToInt(tempint);

        _gtrid = new byte[gtridlen];
        System.arraycopy(bytes, 8, _gtrid, 0, gtridlen);

        System.arraycopy(bytes, (8 + gtridlen), tempint, 0, 4);
        int bquallen = bytesToInt(tempint);

        _bqual = new byte[bquallen];
        System.arraycopy(bytes, (12 + gtridlen), _bqual, 0, bquallen);

        // Defect 372178
        // If the bqual is long enough then we should extract the 
        // sequenceNumber from it to use as the hash code. This is 
        // basically a local tranId that is inserted in the Xid by
        // the tran manager so is different for each tran.
        if (_bqual.length > 16)
        {
            _hashCode = ((_bqual[12]&0xff) << 24) +
                        ((_bqual[13]&0xff) << 16) +
                        ((_bqual[14]&0xff) << 8) +
                        (_bqual[15]&0xff);
        }
        else if (_bqual.length > 3)
        {
            _hashCode = ((_bqual[_bqual.length - 4]&0xff) << 24) +
                        ((_bqual[_bqual.length - 3]&0xff) << 16) +
                        ((_bqual[_bqual.length - 2]&0xff) << 8) +
                        (_bqual[_bqual.length - 1]&0xff);
        }

        if (tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
    }

    /**
     * Construct a new MS Xid from the String returned from 
     * our persistent store.
     * 
     * @param data  The persistent data retrieved from the database.
     */
    public PersistentTranId(String data)
    {
        if (tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", "String Data="+data);

        _persistentData = data;
        
        byte[] bytes;

        // Feature SIB0048c.ms.2
        // If we are using a newly encoded xid string then we can
        // decode using Base64 if not then we need to use the old 
        // Hex encoding.
        if (data.charAt(0) == Base64Utils.HEADER)
        {
            bytes = Base64Utils.decodeBase64(data);
        }
        else
        {
            bytes = fromHexString(data);
        }

        byte[] tempint = new byte[4];
        System.arraycopy(bytes, 0, tempint, 0, 4);
        _formatId = bytesToInt(tempint);

        // We are rebuilding a global tran XID
        System.arraycopy(bytes, 4, tempint, 0, 4);
        int gtridlen = bytesToInt(tempint);

        _gtrid = new byte[gtridlen];
        System.arraycopy(bytes, 8, _gtrid, 0, gtridlen);

        System.arraycopy(bytes, (8 + gtridlen), tempint, 0, 4);
        int bquallen = bytesToInt(tempint);

        _bqual = new byte[bquallen];
        System.arraycopy(bytes, (12 + gtridlen), _bqual, 0, bquallen);

        // Defect 372178
        // If the bqual is long enough then we should extract the 
        // sequenceNumber from it to use as the hash code. This is 
        // basically a local tranId that is inserted in the Xid by
        // the tran manager so is different for each tran.
        if (_bqual.length > 16)
        {
            _hashCode = ((_bqual[12]&0xff) << 24) +
                        ((_bqual[13]&0xff) << 16) +
                        ((_bqual[14]&0xff) << 8) +
                        (_bqual[15]&0xff);
        }
        else if (_bqual.length > 3)
        {
            _hashCode = ((_bqual[_bqual.length - 4]&0xff) << 24) +
                        ((_bqual[_bqual.length - 3]&0xff) << 16) +
                        ((_bqual[_bqual.length - 2]&0xff) << 8) +
                        (_bqual[_bqual.length - 1]&0xff);
        }

        if (tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
    }

    public int getFormatId()
    {
        return _formatId;
    }

    public byte[] getGlobalTransactionId()
    {
        return _gtrid;
    }

    public byte[] getBranchQualifier()
    {
        return _bqual;
    }

    public String getPersistentData()
    {
        if (tc.isEntryEnabled()) SibTr.entry(this, tc, "getPersistentData");

        if (_persistentData == null)
        {       
          // Feature SIB0048c.ms.2
          // To ensure that a maximum length XID will fit in our database
          // column we need to use Base 64 encoding instead of Hex (Base 16)
          _persistentData = Base64Utils.encodeBase64(toByteArray());
        }

        if (tc.isEntryEnabled()) SibTr.exit(this, tc, "getPersistentData", "return="+_persistentData);
        return _persistentData;
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof PersistentTranId))
        {
            return false;             
        }

        PersistentTranId ptid = (PersistentTranId)obj;               

        int formatId = ptid.getFormatId();
        byte[] gtrid = ptid.getGlobalTransactionId();
        byte[] bqual = ptid.getBranchQualifier();

        // If formatId or lengths are not equal then 
        // XIDs are not equal
        if (_formatId != formatId || _gtrid.length != gtrid.length || _bqual.length != bqual.length)
        {
            return false;
        }

        // Now match each byte of gtrid and bqual in reverse 
        // order as WAS uses a common bqual stem
        for (int i = _bqual.length; --i >= 0; )
        {
            if (_bqual[i] != bqual[i])
                return false;
        }

        for (int i = _gtrid.length; --i >= 0; )
        {
            if (_gtrid[i] != gtrid[i])
                return false;
        }

        return true;
    }

    public int hashCode()
    {
        return _hashCode;
    }

    public String toString()
    {
        return getPersistentData();
    }

    public String toTMString()
    {
        StringBuffer retval = new StringBuffer();

        retval.append(toHexString(_gtrid));
        retval.append(toHexString(_bqual));

        return retval.toString();
    }

    public byte[] toByteArray()
    {
        byte[] retval  = new byte[12 + _gtrid.length + _bqual.length];
        byte[] tempint = new byte[4];

        tempint = intToBytes(_formatId);
        System.arraycopy(tempint, 0, retval, 0, 4);

        tempint = intToBytes(_gtrid.length);
        System.arraycopy(tempint, 0, retval, 4, 4);

        System.arraycopy(_gtrid, 0, retval, 8, _gtrid.length);

        tempint = intToBytes(_bqual.length);
        System.arraycopy(tempint, 0, retval, (8 + _gtrid.length), 4);

        System.arraycopy(_bqual, 0, retval, (12 + _gtrid.length), _bqual.length);

        return retval;
    }

    /**
     * A helper function which extracts an int from a byte array representation
     */
    private int bytesToInt(byte[] bytes)
    {
        int result = -1;
        if (bytes.length >= 4)
        {
            result = ((bytes[0]&0xff) << 24) +
                     ((bytes[1]&0xff) << 16) +
                     ((bytes[2]&0xff) << 8) +
                     (bytes[3]&0xff);
        }
        return result;
    } 

    /**
     * A helper function which transfers an int to a byte array in big endian format.
     */
    private byte[] intToBytes(int Int)
    {
        return new byte[] { (byte)(Int>>24), (byte)(Int>>16), (byte)(Int>>8), (byte)(Int)};
    }

    private String toHexString(byte [] b)
    {
        String digits = "0123456789abcdef";
        StringBuffer retval = new StringBuffer(b.length*2);

        for (int i = 0; i < b.length; i++)
        {
            retval.append(digits.charAt((b[i] >> 4) & 0xf));
            retval.append(digits.charAt(b[i] & 0xf));
        }

        return(retval.toString());
    }

    private byte[] fromHexString(String hex)
    {
        String digits = "0123456789abcdef";
        byte[] retval = new byte[hex.length()/2];

        int position = 0;
        for (int i = 0; i < hex.length(); i = i + 2)
        {
            int highorderbits = digits.indexOf(hex.charAt(i));
            int loworderbits  = digits.indexOf(hex.charAt(i + 1));

            if (highorderbits < 0 || highorderbits > 15 || loworderbits < 0 || loworderbits > 15)
            {
                if (tc.isEntryEnabled()) SibTr.event(this, tc, "Invalid values found restoring Xid from String data. HO="+highorderbits+", LO="+loworderbits);
                throw new SIErrorException(nls.getString("UNRECOVERABLE_ERROR_CWSJS0007"));
            }

            retval[position] = (byte)(highorderbits << 4 | loworderbits);

            position++;
        }

        return retval;
    }
}
