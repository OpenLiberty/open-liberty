package com.ibm.tx.jta.ut.util;

import java.io.Serializable;

import javax.transaction.xa.Xid;

/**
 * @author mamwl1
 * 
 *         This class is used to represent an XID.
 *         Unlike the javax.transaction.Xid interface it is serializable which helps!
 */
@SuppressWarnings("serial")
public class XID implements Serializable, Xid
{
    private byte[] gid = null;
    private byte[] branchQualifier = null;
    private int formatId = -1;

    @Override
    public byte[] getGlobalTransactionId()
    {
        return gid;
    }

    @Override
    public byte[] getBranchQualifier()
    {
        return branchQualifier;
    }

    @Override
    public int getFormatId()
    {
        return formatId;
    }

    public XID(int formatId, byte[] gid, byte[] branchQualifier)
    {
        this.branchQualifier = branchQualifier;
        this.formatId = formatId;
        this.gid = gid;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof XID && o != null)
        {
            XID x = (XID) o;
            if (formatId != x.getFormatId())
                return false;

            if (gid == null && x.gid == null)
            {
            }
            else if (gid == null || x.gid == null)
            {
                return false;
            }
            else if (gid.length == x.getGlobalTransactionId().length)
            {
                for (int i = 0; i < gid.length; i++)
                {
                    if (gid[i] != (x.getGlobalTransactionId())[i])
                    {
                        return false;
                    }
                }
            }

            if (branchQualifier == null && x.getBranchQualifier() == null)
            {
            }
            else if (branchQualifier == null || x.getBranchQualifier() == null)
            {
                return false;
            }
            else if (branchQualifier.length == x.getBranchQualifier().length)
            {
                for (int i = 0; i < branchQualifier.length; i++)
                {
                    if (branchQualifier[i] != (x.getBranchQualifier())[i])
                    {
                        return false;
                    }
                }
            }
            return true;
        }
        else
        {
            return false;
        }
    }

    /*
     * long version
     * 
     * Global ID
     * Bqual
     * Format ID
     */
    public String toLongString()
    {
        if (gid == null)
        {
            return null;
        }

        StringBuffer buf = new StringBuffer("Global ID: ");
        toHexString(buf, gid);

        buf.append("\n");

        buf.append("Branch qualifier: ");
        toHexString(buf, branchQualifier);

        buf.append("\n");

        buf.append("Format id: ");
        buf.append(formatId);

        return buf.toString();
    }

    @Override
    public String toString()
    {
        if (gid == null)
        {
            return null;
        }

        StringBuffer buf = new StringBuffer("Global ID: ");

        toHexString(buf, gid);

        return buf.toString();
    }

    public static XID[] convertXidArrayToXIDArray(Xid[] id)
    {

        //The array of Xids that we need to return
        XID[] retVal = new XID[id.length];

        for (int i = 0; i < id.length; i++)
        {
            retVal[i] =
                            new XID(
                                            id[i].getFormatId(),
                                            id[i].getGlobalTransactionId(),
                                            id[i].getBranchQualifier());
        }

        return retVal;
    }

    /**
     * Convert a javax.transaction.Xid to a fvt.hursley.tx.ResourceManager.XID
     */
    public static XID convertXidToXID(Xid xid)
    {
        return new XID(
                        xid.getFormatId(),
                        xid.getGlobalTransactionId(),
                        xid.getBranchQualifier());
    }

    //Convert a byte array to a hex string.
    private void toHexString(StringBuffer buf, byte[] array)
    {
        String digits = "0123456789abcdef";

        for (int i = 0; i < array.length; i++)
        {
            buf.append(digits.charAt((array[i] >> 4) & 0xf));
            buf.append(digits.charAt(array[i] & 0xf));
        }
    }
}
