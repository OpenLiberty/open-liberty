// IBM Confidential
//
// OCO Source Materials
//
// Copyright IBM Corp. 2013
//
// The source code for this program is not published or otherwise divested 
// of its trade secrets, irrespective of what has been deposited with the 
// U.S. Copyright Office.
//
// Change Log:
//  Date       pgmr       reason       Description
//  --------   -------    ------       ---------------------------------
//  06/11/03   jitang     LIDB2110.31  create - Provide J2C 1.5 resource adapter
//  09/19/03   swai       d177210      create another createXid method which takes
//                                     both globalTransId and BranchId.
//  09/25/03   swai       d177221      Add another method to get the supplied BranchId
//                                     and globalTranId so that verifyIndoubtTrans can 
//                                     show the original BranchId and GlobalTranId if
//                                     necessary.
//
//  --------------------------------------------------------------------

package com.ibm.websphere.ejbcontainer.test.rar.core;

import java.util.logging.Logger;

import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

/**
 * <p>This class implements interface javax.transaction.xa.Xid. This class is used to represent
 * an imported transaction from the message provider.</p>
 */
public class XidImpl implements Xid {
    private final static String CLASSNAME = XidImpl.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** format ID */
    protected int formatId;

    /** global transaction id */
    protected byte gtrid[];

    /** branch qualifier */
    protected byte bqual[];

    public XidImpl(int formatId, byte gtrid[], byte bqual[]) {
        this.formatId = formatId;
        this.gtrid = gtrid;
        this.bqual = bqual;
    }

    /**
     * <p>Obtain the format identifier part of the XID. </p>
     * 
     * @return Format identifier. O means the OSI CCR format.
     */
    @Override
    public int getFormatId() {
        return formatId;
    }

    /**
     * <p>Obtain the global transaction identifier part of XID as an array of bytes. </p>
     * 
     * @return Global transaction identifier.
     */
    @Override
    public byte[] getGlobalTransactionId() {
        return gtrid;

    }

    /**
     * <p>Obtain the transaction branch identifier part of XID as an array of bytes. </p>
     * 
     * @return Global transaction identifier.
     */
    @Override
    public byte[] getBranchQualifier() {
        return bqual;
    }

    /**
     * Create a XidImpl object
     * 
     * @param the brank qualifier id
     * 
     * @return XidImpl the XidImpl object.
     */
    public static XidImpl createXid(int bids) throws XAException {
        byte[] gid = new byte[1];
        gid[0] = (byte) 9;
        byte[] bid = new byte[1];
        bid[0] = (byte) bids;
        byte[] gtrid = new byte[64];
        byte[] bqual = new byte[64];
        System.arraycopy(gid, 0, gtrid, 0, 1);
        System.arraycopy(bid, 0, bqual, 0, 1);
        XidImpl xid = new XidImpl(0x1234, gtrid, bqual);

        return xid;
    }

    // swai: d177210 starts
    /**
     * Create a XidImpl object
     * 
     * @param the global tran id
     * @param the branch qualifier id
     * 
     * @return XidImpl the XidImpl object.
     */
    public static XidImpl createXid(int gTranId, int bids) throws XAException {
        //byte[] gid = new byte[1];
        //gid[0] = (byte) gTranId;
        //byte[] bid = new byte[1];
        //bid[0] = (byte) branchId;
        byte[] gtrid = new byte[64];
        byte[] bqual = new byte[64];
        gtrid[0] = (byte) gTranId;
        bqual[0] = (byte) bids;
        XidImpl xid = new XidImpl(0x1234, gtrid, bqual);

        return xid;
    }

    // swai: d177210 ends

    /**
     * <p>Set the format identifier part of the XID. </p>
     * 
     * @return Format identifier. O means the OSI CCR format.
     */
    public void setFormatId(int formatId) {
        this.formatId = formatId;
    }

    /**
     * <p>Override the equals method in order to see if a supplied Xid is the
     * same as the one in comparison.
     * 
     * <p>Value comparison.</p>
     * 
     * @param Xid Xid object for comparison
     * @return boolean
     */
    @Override
    public boolean equals(Object o) {
        // Ask Jian whether I should override equals method.
        if (!(o instanceof XidImpl))
        {
            svLogger.info("XidImpl.equals: Object o is not an instance of XidImpl.");
            return false;
        }

        byte id = (((XidImpl) o).getBranchQualifier())[0];
        if (id != bqual[0])
        {
            svLogger.info("XidImpl.equals: The branch qualifiers are not the same. It is " + id + ". Should be " + bqual[0]);
            return false;
        }

        id = (((XidImpl) o).getGlobalTransactionId())[0];
        if (id != gtrid[0])
        {
            svLogger.info("XidImpl.equals: The global tran id are not the same. It is " + id + ". Should be " + gtrid[0]);
            return false;
        }

        int fid = ((XidImpl) o).getFormatId();
        if (fid != formatId)
        {
            svLogger.info("XidImpl.equals: The format id are not the same. It is " + fid + ". Should be " + formatId);
            return false;
        }

        return true;
    }

    /**
     * <p>Override the hashCode method as Xid will be added to a Set.
     * The hashcode is (format Id + global tran id) * branch qualifier </p>
     * 
     * @return int hashcode of the object
     */
    @Override
    public int hashCode() {
        return ((formatId + gtrid[0]) * bqual[0]);
    }
    // d177221 ends
}