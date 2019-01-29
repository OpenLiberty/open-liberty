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
 *  Reason         Date     Origin   Description
 * --------------- -------- -------- ------------------------------------------
 * 182347          10/11/03 gareth   Add Persistent Transaction ID
 * 341158          13/03/06 gareth   Make better use of LoggingTestCase
 * SIB0048c.ms.2   05/02/07 gareth   Resolve wide XID problem
 * ============================================================================
 */

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.transactions.PersistentTranId;

public class PersistentTranIdBlackBoxTest extends MessageStoreTestCase
{
    public PersistentTranIdBlackBoxTest(String name)
    {
        super(name);

        //turnOnTrace();
    }

    public static TestSuite suite()
    {
        return new TestSuite(PersistentTranIdBlackBoxTest.class);
    }

    /*
     * public void testPersistentTranIdBlackBox()
     * {
     * print("|-----------------------------------------------------");
     * print("| PersistentTranIdBlackBox:");
     * print("|--------------------------");
     * print("|");
     * 
     * PersistentTranId xid1 = null;
     * PersistentTranId xid2 = null;
     * PersistentTranId xid3 = null;
     * PersistentTranId xid4 = null;
     * 
     * // Create a new PersistentTranId, this should SUCCEED
     * try
     * {
     * xid1 = new PersistentTranId(-1, new String("gtrid").getBytes(), new String("bqual").getBytes());
     * 
     * print("| Created new Xid");
     * }
     * catch (Exception e)
     * {
     * print("| Create new Xid  !!!FAILED!!!");
     * e.printStackTrace();
     * fail("Exception caught creating new Xid!");
     * }
     * 
     * 
     * if (xid1 != null)
     * {
     * // Get the current component values of the XID, this should SUCCEED
     * try
     * {
     * print("| Get XID1 Values:");
     * 
     * print("| - FormatID    = "+xid1.getFormatId());
     * print("| - Global TID  = "+toHexString(xid1.getGlobalTransactionId()));
     * print("| - Branch Qual = "+toHexString(xid1.getBranchQualifier()));
     * }
     * catch (Exception e)
     * {
     * print("| Get XID1 Values   !!!FAILED!!!");
     * fail("Exception caught getting values from XID1!");
     * }
     * 
     * 
     * // Get the current component values of the XID, this should SUCCEED
     * try
     * {
     * print("| Get Persistent Data:");
     * 
     * print("| - "+xid1.getPersistentData());
     * }
     * catch (Exception e)
     * {
     * print("| Get Persistent Data   !!!FAILED!!!");
     * fail("Exception caught extracting Persistent Data!");
     * }
     * 
     * 
     * // Create a new PersistentTranId from persistent data, this should SUCCEED
     * try
     * {
     * xid2 = new PersistentTranId(xid1.getPersistentData());
     * 
     * print("| Created new Xid from persistent data");
     * }
     * catch (Exception e)
     * {
     * print("| Create new Xid from persistent data   !!!FAILED!!!");
     * e.printStackTrace();
     * fail("Exception caught creating new Xid from persistent data!");
     * }
     * 
     * 
     * if (xid2 != null)
     * {
     * // Get the current component values of the XID, this should SUCCEED
     * try
     * {
     * print("| Get XID2 Values:");
     * 
     * print("| - FormatID    = "+xid2.getFormatId());
     * print("| - Global TID  = "+toHexString(xid2.getGlobalTransactionId()));
     * print("| - Branch Qual = "+toHexString(xid2.getBranchQualifier()));
     * }
     * catch (Exception e)
     * {
     * print("| Get XID2 Values   !!!FAILED!!!");
     * fail("Exception caught getting values from XID2!");
     * }
     * }
     * 
     * 
     * try
     * {
     * print("| Get toString() Data (XID2):");
     * 
     * print("| - "+xid2.toString());
     * }
     * catch (Exception e)
     * {
     * print("| Get toString() Data (XID2)   !!!FAILED!!!");
     * fail("Exception caught extracting String Data!");
     * }
     * 
     * 
     * // Create a new PersistentTranId from Xid1, this should SUCCEED
     * try
     * {
     * xid3 = new PersistentTranId(xid1);
     * 
     * print("| Created new Xid from XID1");
     * }
     * catch (Exception e)
     * {
     * print("| Create new Xid from XID1   !!!FAILED!!!");
     * fail("Exception caught creating new Xid from XID1!");
     * }
     * 
     * 
     * if (xid3 != null)
     * {
     * // Get the current component values of the XID, this should SUCCEED
     * try
     * {
     * print("| Get XID3 Values:");
     * 
     * print("| - FormatID    = "+xid3.getFormatId());
     * print("| - Global TID  = "+toHexString(xid3.getGlobalTransactionId()));
     * print("| - Branch Qual = "+toHexString(xid3.getBranchQualifier()));
     * }
     * catch (Exception e)
     * {
     * print("| Get XID2 Values   !!!FAILED!!!");
     * fail("Exception caught getting values from XID2!");
     * }
     * }
     * 
     * 
     * try
     * {
     * print("| Get toString() Data (XID3):");
     * 
     * print("| - "+xid3.toString());
     * }
     * catch (Exception e)
     * {
     * print("| Get toString() Data (XID3)   !!!FAILED!!!");
     * fail("Exception caught extracting String Data!");
     * }
     * 
     * 
     * // Create a new PersistentTranId from String data, this should SUCCEED
     * try
     * {
     * xid4 = new PersistentTranId(xid1.toString());
     * 
     * print("| Created new Xid from String data");
     * }
     * catch (Exception e)
     * {
     * print("| Create new Xid from String data   !!!FAILED!!!");
     * fail("Exception caught creating new Xid from String data!");
     * }
     * 
     * 
     * if (xid4 != null)
     * {
     * // Get the current component values of the XID, this should SUCCEED
     * try
     * {
     * print("| Get XID4 Values:");
     * 
     * print("| - FormatID    = "+xid4.getFormatId());
     * print("| - Global TID  = "+toHexString(xid4.getGlobalTransactionId()));
     * print("| - Branch Qual = "+toHexString(xid4.getBranchQualifier()));
     * }
     * catch (Exception e)
     * {
     * print("| Get XID4 Values   !!!FAILED!!!");
     * fail("Exception caught getting values from XID4!");
     * }
     * }
     * 
     * 
     * try
     * {
     * print("| Get toString() Data (XID4):");
     * 
     * print("| - "+xid4.toString());
     * }
     * catch (Exception e)
     * {
     * print("| Get toString() Data (XID4)   !!!FAILED!!!");
     * fail("Exception caught extracting String Data!");
     * }
     * 
     * 
     * // Check if equality of all the XID's, this should SUCCEED
     * boolean retval = xid1.equals(xid2);
     * if (retval)
     * {
     * print("| XID1 equal to XID2");
     * }
     * else
     * {
     * print("| XID1 equal to XID2   !!!FAILED!!!");
     * fail("XID1 not equal to XID2!");
     * }
     * 
     * 
     * retval = xid1.equals(xid3);
     * if (retval)
     * {
     * print("| XID1 equal to XID3");
     * }
     * else
     * {
     * print("| XID1 equal to XID3   !!!FAILED!!!");
     * fail("XID1 not equal to XID3!");
     * }
     * 
     * 
     * retval = xid1.equals(xid4);
     * if (retval)
     * {
     * print("| XID1 equal to XID4");
     * }
     * else
     * {
     * print("| XID1 equal to XID4   !!!FAILED!!!");
     * fail("XID1 not equal to XID4!");
     * }
     * 
     * 
     * // List the hashCodes of all three TranId's, all should be the SAME
     * print("| Check hash code equality:");
     * print("| - TranId1 = "+xid1.hashCode());
     * print("| - TranId2 = "+xid2.hashCode());
     * print("| - TranId3 = "+xid3.hashCode());
     * print("| - TranId4 = "+xid4.hashCode());
     * if ((xid1.hashCode() == xid2.hashCode()) && (xid1.hashCode() == xid3.hashCode()) && (xid1.hashCode() == xid4.hashCode()))
     * {
     * print("| Hash codes are equal");
     * }
     * else
     * {
     * print("| Check hash code equality   !!!FAILED!!!");
     * fail("Hash codes not equal!");
     * }
     * }
     * 
     * 
     * print("|");
     * print("|------------------------ END ------------------------");
     * }
     */

    public void testMigratePersistentTranId()
    {
        print("|-----------------------------------------------------");
        print("| MigratePersistentTranId:");
        print("|-------------------------");
        print("|");

        LongXid xid = new LongXid();
        print("| Create random XID:");
        print("| - FormatID   : " + xid.getFormatId());
        print("| - GTRID (HEX): " + toHexString(xid.getGlobalTransactionId()));
        print("| - BQUAL (HEX): " + toHexString(xid.getBranchQualifier()));

        PersistentTranId tranId1 = new PersistentTranId(xid);
        print("| PersistentTranId created from XID:");
        print("| - " + tranId1);

        // format into one long byte array
        byte[] data = new byte[(12 + xid.getGlobalTransactionId().length + xid.getBranchQualifier().length)];

        byte[] gtrid = xid.getGlobalTransactionId();
        byte[] bqual = xid.getBranchQualifier();

        System.arraycopy(intToBytes(xid.getFormatId()), 0, data, 0, 4);
        System.arraycopy(intToBytes(gtrid.length), 0, data, 4, 4);
        System.arraycopy(gtrid, 0, data, 8, gtrid.length);
        System.arraycopy(intToBytes(bqual.length), 0, data, (8 + gtrid.length), 4);
        System.arraycopy(bqual, 0, data, (12 + gtrid.length), bqual.length);

        print("| Hex String created from components of XID:");
        print("| - " + toHexString(data));

        // Create a new PersistentTranId from the Hex representation
        // of the byte array. This will mirror the behaviour of an 
        // old format XID being read out of the database.
        PersistentTranId tranId2 = new PersistentTranId(toHexString(data));
        print("| PersistentTranId created from Hex String:");
        print("| - " + tranId2);
        print("|");

        // Check equality of persistentTranIds
        if (tranId1.equals(tranId2))
        {
            print("| PersistentTranIds match");
        }
        else
        {
            print("| PersistentTranIds match   !!!FAILED!!!");
            fail("XIDs do not match!");
        }

        print("|");
        print("|------------------------ END ------------------------");
    }

    private byte[] intToBytes(int Int)
    {
        return new byte[] { (byte) (Int >> 24), (byte) (Int >> 16), (byte) (Int >> 8), (byte) (Int) };
    }

    private String toHexString(byte[] b)
    {
        String digits = "0123456789abcdef";
        StringBuffer retval = new StringBuffer(b.length * 2);

        for (int i = 0; i < b.length; i++)
        {
            retval.append(digits.charAt((b[i] >> 4) & 0xf));
            retval.append(digits.charAt(b[i] & 0xf));
        }
        return (retval.toString());
    }
}
