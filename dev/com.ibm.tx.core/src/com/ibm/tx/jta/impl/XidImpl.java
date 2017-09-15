package com.ibm.tx.jta.impl;

/*******************************************************************************
 * Copyright (c) 2002, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.io.Serializable;
import java.security.PrivilegedAction;
import java.util.Arrays;

import javax.transaction.xa.Xid;

import com.ibm.tx.TranConstants;
import com.ibm.tx.config.ConfigurationProvider;
import com.ibm.tx.config.ConfigurationProviderManager;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.Transaction.JTA.Util;
import com.ibm.ws.Transaction.JTS.Configuration;

/**
 * 
 * The XidImpl class provides an implementation of the X/Open transaction
 * identifier. It implements JTA javax.transaction.xa.Xid interface.
 * 
 */
public class XidImpl implements Xid, Serializable {
    private static final TraceComponent tc = Tr.register(XidImpl.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    private static final long serialVersionUID = -4291259441393350686L;

    private static final String nl = java.security.AccessController.doPrivileged(new PrivilegedAction<String>() {
        @Override
        public String run() {
            return System.getProperty("line.separator");
        }
    });

    //
    // The format identifier for the Xid. A value of -1 indicates 
    // that the NULL Xid
    //
    final protected int _formatId;

    //
    // Holds the Global Transaction ID for this Xid
    //
    final protected byte[] _gtrid;

    //
    // Holds the Branch Qualifier for this Xid
    //
    final protected byte[] _bqual;

    /**
     * Holds the otid style byte array representing this Xid.
     */
    private byte[] _otid;

    /**
     * Holds the primary key for this transaction identifier.
     */
    private TxPrimaryKey _primaryKey;

    //
    // Special assigned formatIDs for WAS
    //
    public final static int ZOS_FID_CB390 = 0xC3C20186; // 'CB' in EBCDIC, 390 in hex
    public final static int ZOS_FID_CBLT = 0xC3C2D3E3; // 'CBLT' in EBCDIC
    private final static int ZOS_FID_WASZ = 0xE6C1E2E9; // 'WASZ' in EBCDIC

    public final static int WAS_FID_WASC = 0x57415343; // 'WASC' in ASCII (Client)
    protected final static int WAS_FID_WASD = 0x57415344; // 'WASD' in ASCII (Distributed Server)
    private final static int WAS_FID_WASZ = 0x5741535A; // 'WASZ' in ASCII (z/OS)

    //
    // Server based formatID used for generating Xids for JTA transactions and XA resources
    // The zOS native layer uses CB390, distributed uses WASD
    //
    protected final static int WAS_FORMAT_ID = WAS_FID_WASD;

    private static ConfigurationProvider _configProvider = ConfigurationProviderManager.getConfigurationProvider();

    /**
     * Initialize an XidImpl using an existing gtrid and a new branch.
     * 
     * @param gtrid The gtrid of the original transaction branch
     * @param pk The new primary key
     * @param index The new branch index
     */
    public XidImpl(byte[] gtrid, TxPrimaryKey pk, int index) /* @LI3187A */
    {
        this(gtrid, pk, index, WAS_FORMAT_ID);
    }

    /**
     * Initialize an XidImpl using an existing gtrid and a new branch
     * with the formatId specified.
     * 
     * @param gtrid The gtrid of the original transaction branch
     * @param pk The new primary key
     * @param index The new branch index
     * @param formatID The formatId
     */
    public XidImpl(byte[] gtrid, TxPrimaryKey pk, int index, int formatID) /* @LI3187A */
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "XidImpl", new Object[] { Util.toHexString(gtrid), pk, index });

        _formatId = formatID;

        _bqual = new byte[BQUAL_JTA_GTRID_LENGTH];

        byte[] theApplid = Configuration.getApplId();

        if (tc.isDebugEnabled())
            Tr.debug(tc, "Creating XID using applid " + Util.toHexString(theApplid));

        System.arraycopy(pk.toBytes(), 0, _bqual, BQUAL_PKEY_OFFSET, BQUAL_PKEY_LENGTH);
        System.arraycopy(theApplid, 0, _bqual, BQUAL_UUID_OFFSET, BQUAL_UUID_LENGTH);
        _bqual[BQUAL_BRANCH_INDEX_OFFSET] = (byte) index;

        _gtrid = Util.duplicateByteArray(gtrid);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "XidImpl", this);
    }

    /**
     * Mainline constructor. Build an XidImpl from the native Xid as defined
     * in the XA specification.
     * 
     * <pre>
     * struct xid_t {
     * long formatID; // format identifier
     * long gtrid_length; // value 1-64
     * long bqual_length; // value 1-64
     * char data[XIDDATASIZE];
     * };
     * </pre>
     */
    public XidImpl(byte[] nativeXid, int offset) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "XidImpl", new Object[] {
                                                  Util.toHexString(nativeXid),
                                                  offset });

        _formatId = Util.getIntFromBytes(nativeXid, offset, 4);

        final int gtridLength = Util.getIntFromBytes(nativeXid, offset + 4, 4);
        _gtrid = Util.duplicateByteArray(nativeXid, offset + 12, gtridLength);

        final int bqualLength = Util.getIntFromBytes(nativeXid, offset + 8, 4);
        // Dont extend the bqual else it causes problems on distributed
        // when logging and recovery - z/OS logs the RRS Xid which is shorter
        // _bqual = new byte[BQUAL_JTA_BQUAL_LENGTH];
        _bqual = new byte[bqualLength];
        System.arraycopy(nativeXid, offset + 12 + gtridLength,
                         _bqual, 0,
                         bqualLength);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "XidImpl", this);
    }

    /**
     * Xid copy constructor that is used during recovery
     * and only used for Xids with our formatId.
     */
    public XidImpl(Xid xid) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "XidImpl", xid);

        this._formatId = xid.getFormatId();
        this._gtrid = Util.duplicateByteArray(xid.getGlobalTransactionId());
        this._bqual = Util.duplicateByteArray(xid.getBranchQualifier());

        if (tc.isEntryEnabled())
            Tr.exit(tc, "XidImpl", this);
    }

    /**
     * XidImpl new branch constructor.
     * 
     * Creates a new Xid based on the Xid that was passed in. The only
     * field that differs is the sequence number. A new sequence number
     * should be passed in by the unit of work indicating that this xid
     * represents a new branch of the transaction.
     * 
     * @param oldXid The XidImpl to clone
     * @param sequenceNumber The sequence number to use for the new
     *            Xid. This should be one higher than the previous sequence
     *            number used by the UnitOfWork, which is keeping track of
     *            this.
     */
    public XidImpl(Xid oldXid, int sequenceNumber) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "XidImpl", new Object[] {
                                                  oldXid,
                                                  sequenceNumber });
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Creating XID for a resource branch");

        this._formatId = oldXid.getFormatId();
        this._gtrid = Util.duplicateByteArray(oldXid.getGlobalTransactionId());
        this._bqual = new byte[BQUAL_JTA_BQUAL_LENGTH];

        final byte[] oldBqual = oldXid.getBranchQualifier();

        System.arraycopy(
                         oldBqual, 0,
                         this._bqual, 0,
                         oldBqual.length);

        setSequenceNumber(sequenceNumber);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "XidImpl", this);
    }

    /**
     * XidImpl constructor. This constructor builds an Xid using the native
     * Xid.
     * 
     * @param nativeXid The Xid generated by the native TM
     * @param sequence The sequence number for this transaction branch.
     * @param stoken The stoken to be logged in this Xid. If null,
     *            use the stoken for this space.
     */
    public XidImpl(byte[] nativeXid,
                   int sequence,
                   byte[] stoken) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "XidImpl", new Object[] { Util.toHexString(nativeXid), sequence, Util.toHexString(stoken) });

        _formatId = Util.getIntFromBytes(nativeXid, 0, 4);

        final int gtridLength = Util.getIntFromBytes(nativeXid, 4, 4);
        _gtrid = Util.duplicateByteArray(nativeXid, 12, gtridLength);

        final int bqualLength = Util.getIntFromBytes(nativeXid, 8, 4);
        // The nativeXid is shorter than the resource Xid so allow
        // room for the extra fields.
        _bqual = new byte[BQUAL_JTA_BQUAL_LENGTH];
        System.arraycopy(nativeXid, 12 + gtridLength,
                         _bqual, 0,
                         bqualLength);

        setSequenceNumber(sequence);

        setStoken(stoken);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "XidImpl", this);
    }

    /**
     * Initialize an XidImpl using the primary key for a transaction.
     * 
     * @param pk The primary key used for the generated XidImpl
     */
    public XidImpl(TxPrimaryKey pk) // @LI3187-3C
    {
        this(WAS_FORMAT_ID, pk);
    }

    /**
     * Initialize an XidImpl using the primary key for a transaction. This is the Xid associated
     * with an imported global transaction. The Xid consists of both a gtrid and
     * bqual portion, the gtrid is a copy of the imported gtrid.
     * 
     * @param oldXid The old Xid used as a base for the generated XidImpl
     *            id The local ID used for the generated XidImpl
     */
    public XidImpl(Xid oldXid, TxPrimaryKey pk) {
        this(oldXid.getGlobalTransactionId(), pk, 1); /* @LI3187C */
    }

    /**
     * yet another contructor.
     * 
     * Initialize an XidImpl using the primary key for a transaction. This is the Xid associated
     * with a locally created global transaction. The Xid consists of both a gtrid and
     * bqual portion, the bqual is a copy of the gtrid but with a branch index of 1.
     * 
     * @param formatId The format ID used for the generated XidImpl
     *            pk The primary key used for the generated XidImpl
     */
    protected XidImpl(int formatId, TxPrimaryKey pk) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "XidImpl", new Object[] { Integer.toHexString(formatId), pk });
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Creating XID for a global transaction");

        this._formatId = formatId;

        // Build a bqual for this Xid
        this._bqual = new byte[BQUAL_JTA_GTRID_LENGTH];

        byte[] theApplid = Configuration.getApplId();

        if (tc.isDebugEnabled())
            Tr.debug(tc, "Creating XID using applid " + Util.toHexString(theApplid));

        System.arraycopy(pk.toBytes(), 0, _bqual, BQUAL_PKEY_OFFSET, BQUAL_PKEY_LENGTH);
        System.arraycopy(theApplid, 0, _bqual, BQUAL_UUID_OFFSET, BQUAL_UUID_LENGTH);
        _bqual[BQUAL_BRANCH_INDEX_OFFSET] = 1;

        // Make the gtrid the same as the bqual less the branch index
        this._gtrid = new byte[GTRID_JTA_GTRID_LENGTH];
        System.arraycopy(_bqual, 0, _gtrid, 0, GTRID_JTA_GTRID_LENGTH);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "XidImpl", this);
    }

    /**
     * Determine whether or not two objects of this type are equal.
     * 
     * @param o the object to be compared with this XidImpl.
     * 
     * @return Returns true of the supplied object represents the same
     *         global transaction as this, otherwise returns false.
     */
    @Override
    public final boolean equals(Object o) {

        if (o == this)
            return true;

        if (!(o instanceof XidImpl))
            return false;

        final XidImpl other = (XidImpl) o;

        // If fids are not equal then XidImpls are not equal
        if (_formatId != other._formatId)
            return false;

        return (Arrays.equals(_bqual, other._bqual) && Arrays.equals(_gtrid, other._gtrid));
    }

    /**
     * Return a string representing this XidImpl for debuging
     * 
     * @return the string representation of this Xid
     */
    @Override
    public String toString() {
        return new String("{XidImpl: formatId(" + Integer.toHexString(_formatId) +
                          "), gtrid_length(" + (_gtrid == null ? 0 : _gtrid.length) +
                          "), bqual_length(" + (_bqual == null ? 0 : _bqual.length) +
                          ")," + nl + "data(" + (_gtrid == null ? "" : Util.toHexString(_gtrid)) +
                          (_bqual == null ? "" : Util.toHexString(_bqual)) +
                          ")}");
    }

    //-------------------------------------------
    // JTA javax.transaction.xa.Xid interface
    //-------------------------------------------

    /**
     * Obtain the format identifier part of the XidImpl.
     * 
     * @return Format identifier.
     */
    @Override
    public final int getFormatId() {
        return _formatId;
    }

    /**
     * Returns the global transaction identifier for this XidImpl.
     * 
     * @return the global transaction identifier
     */
    @Override
    public final byte[] getGlobalTransactionId() {
        return _gtrid;
    }

    /**
     * Returns the branch qualifier for this XidImpl.
     * 
     * @return the branch qualifier
     */
    @Override
    public final byte[] getBranchQualifier() {
        return _bqual;
    }

    /**
     * Build a native Xid form suitable for recreating with the mainline
     * constructor as in the XA specification.
     * 
     * <pre>
     * struct xid_t {
     * long formatID; // format identifier
     * long gtrid_length; // value 1-64
     * long bqual_length; // value 1-64
     * char data[XIDDATASIZE];
     * };
     * </pre>
     */
    public byte[] toBytes() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "toBytes");

        final int gtridLength = (_gtrid == null ? 0 : _gtrid.length);
        final int bqualLength = (_bqual == null ? 0 : _bqual.length);
        final int nbytes = 12 + gtridLength + bqualLength;
        final byte[] result = new byte[nbytes];

        Util.setBytesFromInt(result, 0, 4, _formatId);
        Util.setBytesFromInt(result, 4, 4, gtridLength);
        Util.setBytesFromInt(result, 8, 4, bqualLength);
        if (gtridLength > 0)
            System.arraycopy(_gtrid, 0, result, 12, gtridLength);
        if (bqualLength > 0)
            System.arraycopy(_bqual, 0, result, 12 + gtridLength, bqualLength);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "toBytes", Util.toHexString(result));
        return result;
    }

    /**
     * Gets the otid.tid style raw byte representation of this XidImpl.
     * 
     * <pre>
     * struct otid_t {
     * long formatID;
     * long bqual_length;
     * sequence &lt;octet&gt; tid;
     * };
     * </pre>
     * 
     * This is not a huge performer. It is only called when someone
     * has registered a SynchronizationCallback via the LI850 SPIs or if
     * the Activity service needs the tx identifier.
     */
    public byte[] getOtidBytes() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getOtidBytes");

        if (_otid == null) {
            final int total_length = _gtrid.length + _bqual.length;
            _otid = new byte[total_length];

            System.arraycopy(_gtrid, 0, _otid, 0, _gtrid.length);
            System.arraycopy(_bqual, 0, _otid, _gtrid.length, _bqual.length);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getOtidBytes", _otid);

        return Util.duplicateByteArray(_otid);
    }

    final static String digits = "0123456789ABCDEF";

    public String printOtid() {
        final int total_length = _gtrid.length * 2 + _bqual.length * 2 + 1;
        StringBuilder result = new StringBuilder(total_length);
        for (int i = 0; i < _gtrid.length; i++) {
            result.append(digits.charAt((_gtrid[i] >> 4) & 0xf));
            result.append(digits.charAt(_gtrid[i] & 0xf));
        }
        result.append(':');
        for (int i = 0; i < _bqual.length; i++) {
            result.append(digits.charAt((_bqual[i] >> 4) & 0xf));
            result.append(digits.charAt(_bqual[i] & 0xf));
        }
        return (result.toString());
    }

    /**
     * Get the cruuid associated with this Xid. For distributed this is
     * the same as the server applid.
     * 
     * @return The cruuid for this Xid in byte array format.
     */
    public byte[] getCruuid() {
        byte[] theCruuid =
                        Util.duplicateByteArray(
                                                _bqual,
                                                BQUAL_UUID_OFFSET,
                                                BQUAL_UUID_LENGTH);
        return theCruuid;
    }

    /**
     * Get the stoken associated with this Xid
     * 
     * @return The stoken for this Xid in byte array format.
     */
    public byte[] getStoken() {
        return Util.duplicateByteArray(
                                       _bqual,
                                       BQUAL_STOKEN_OFFSET,
                                       BQUAL_STOKEN_LENGTH);
    }

    /**
     * Set the stoken associated with this Xid.
     */
    protected void setStoken(byte[] stoken) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setStoken", stoken);

        if (stoken != null)
            System.arraycopy(
                             stoken,
                             0,
                             _bqual,
                             BQUAL_STOKEN_OFFSET,
                             BQUAL_STOKEN_LENGTH);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "setStoken");
    }

    /**
     * Get the sequence number assigned to this transaction branch.
     * 
     * @return The sequence number assigned to this transaction branch,
     *         stored in a byte array of size 2.
     */
    public int getSequenceNumber() {
        return Util.getIntFromBytes(
                                    _bqual,
                                    BQUAL_RM_SEQ_OFFSET,
                                    BQUAL_RM_SEQ_LENGTH);
    }

    /**
     * Set the sequence number assigned to this branch.
     */
    protected void setSequenceNumber(int sequenceNumber) {
        Util.setBytesFromInt(
                             _bqual,
                             BQUAL_RM_SEQ_OFFSET,
                             BQUAL_RM_SEQ_LENGTH,
                             sequenceNumber);
    }

    /**
     * Get the primary key for this transaction out of the bqual.
     */
    public TxPrimaryKey getPrimaryKey() {
        if (_primaryKey == null) {
            _primaryKey = new TxPrimaryKey(_bqual, BQUAL_PKEY_OFFSET);
        }

        return _primaryKey;
    }

    /**
     * Get the epoch number associated with this Xid.
     * 
     * @return The epoch number for this Xid in integer format.
     */
    public int getEpoch() {
        return this.getPrimaryKey().getEpochNumber();
    }

    /**
     * Get the primary key sequence number.
     */
    public int getPrimaryKeySequenceNumber() {
        return this.getPrimaryKey().getSequenceNumber();
    }

    /**
     * Compute the hash code.
     * 
     * @return the computed hashcode
     */
    @Override
    public final int hashCode() {
        // Distributed may generate XIDs with no bqual
        if (_bqual != null)
            return getPrimaryKeySequenceNumber();

        return 0;
    }

    /**
     * Checks to see if this formatId is one of our generated formatIds.
     * This is a first pass check on recovery to filter XIDs from RMs.
     * We do not need to validate platform as the next phase check is for
     * UUID which will be different for each application server.
     * 
     * @return true iff the format ID is 0xC9C20186 or 0xC9C2D3E3 and z/OS
     *         or 0x57415344 for distributed.
     */
    public static boolean isOurFormatId(int formatIdentifier) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "isOurFormatId", formatIdentifier);

        final boolean result;

        switch (formatIdentifier) { // Current format IDs
            case ZOS_FID_CB390:
            case ZOS_FID_CBLT:
            case WAS_FID_WASD:
                result = true;
                break;

            // Possible format IDs in the future
            case WAS_FID_WASC:
            case WAS_FID_WASZ:
            case ZOS_FID_WASZ:
            default:
                result = false;
                break;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "isOurFormatId", Boolean.valueOf(result));

        return result;
    }

    /**
     * Returns the bquals branch index.
     * 
     * @return the global transaction identifier
     */
    public final int getBqualBranchIndex() {
        // Bit wise & means most significant bit of bqual branch index no longer signifies -128 as it is no longer the MSB 
        return 0xff & _bqual[BQUAL_BRANCH_INDEX_OFFSET];
    }

    protected final static int BQUAL_PKEY_OFFSET = 0;
    protected final static int BQUAL_PKEY_LENGTH = 16;
/*
 * private final static int BQUAL_PKEY_STCK_OFFSET = 0;
 * private final static int BQUAL_PKEY_STCK_LENGTH = 8;
 * 
 * private final static int BQUAL_PKEY_EPOCH_OFFSET = 8;
 * private final static int BQUAL_PKEY_EPOCH_LENGTH = 4;
 * 
 * private final static int BQUAL_PKEY_SEQUENCE_OFFSET = 12;
 * private final static int BQUAL_PKEY_SEQUENCE_LENGTH = 4;
 */
    protected final static int BQUAL_UUID_OFFSET = 16;
    protected final static int BQUAL_UUID_LENGTH = 20;

    public final static int GTRID_JTA_GTRID_LENGTH = 36;

    protected final static int BQUAL_BRANCH_INDEX_OFFSET = 39;
//  private final static int BQUAL_BRANCH_INDEX_LENGTH = 1;

    protected final static int BQUAL_JTA_GTRID_LENGTH = 40;

    private final static int BQUAL_STOKEN_OFFSET = 40;
    private final static int BQUAL_STOKEN_LENGTH = 8;

    // These four bytes used to have the recovery log ID for the
    // XAResource (v5.1 and previous).  Since v6 it has been
    // set to 0 since we do not have the recovery log ID at the
    // point of XID creation.  It will always be 0 and at some
    // point in the future should be re-used or removed.
    private final static int BQUAL_RECOVERY_ID_OFFSET_UNUSED = 48;
    private final static int BQUAL_RECOVERY_ID_LENGTH_UNUSED = 4;

    private final static int BQUAL_RM_SEQ_OFFSET = 52;
    private final static int BQUAL_RM_SEQ_LENGTH = 2;

    public final static int BQUAL_JTA_BQUAL_LENGTH = 54;

} // class XidImpl 
