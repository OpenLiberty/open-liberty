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
package com.ibm.ws.sib.transactions;

import java.util.Arrays;

import javax.transaction.xa.Xid;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.transactions.Constants;
import com.ibm.ws.sib.utils.ras.SibTr;

public final class XidKey
{
  private static final TraceComponent tc = SibTr.register(XidKey.class,
      Constants.MSG_GROUP, Constants.MSG_BUNDLE);

  private final int hashCode;
  private final int formatId;
  private final byte[] globalTxId;

  private String cachedToString = null;
  private final static String[] hexValues = 
  {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};

  protected XidKey(int formatId, byte[] globalTxId)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "XidKey", new Object[] { new Integer(formatId),
          globalTxId });

    this.formatId = formatId;
    if (globalTxId == null)
    {
      this.globalTxId = null;
    }
    else
    {
      this.globalTxId = new byte[globalTxId.length];
      System.arraycopy(globalTxId, 0, this.globalTxId, 0,
          this.globalTxId.length);
    }

    if (this.globalTxId.length > 3)
    {
      hashCode = ((globalTxId[globalTxId.length - 4] & 0xff) << 24)
          + ((globalTxId[globalTxId.length - 3] & 0xff) << 16)
          + ((globalTxId[globalTxId.length - 2] & 0xff) << 8)
          + (globalTxId[globalTxId.length - 1] & 0xff);
    }
    else
      hashCode = -1;

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "XidKey", this);
  }

  /**
   * Copy constructor.
   * 
   * @param xid
   */
  public XidKey(Xid xid)
  {
    this(xid.getFormatId(), xid.getGlobalTransactionId());
    
    if (tc.isEntryEnabled()) 
        SibTr.entry(tc, "XidKey", xid); 
    if (tc.isEntryEnabled()) 
    	SibTr.exit(tc, "XidKey");  
  }

  public boolean equals(Object o)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "equals", o);

    boolean returnValue = false;
    if (o instanceof XidKey)
    {
      XidKey xidkey = (XidKey) o;
      if (xidkey.formatId == this.formatId
          && Arrays.equals(xidkey.globalTxId, this.globalTxId))
      {
        returnValue = true;
      }
    }
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "equals", new Boolean(returnValue));
    return returnValue;
  }

  public int hashCode()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "hashCode");

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "hashCode", new Integer(hashCode));
    return hashCode;
  }

  /** @see java.Object#toString() */
  public String toString()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "toString");

    if (cachedToString == null)
    {
      StringBuffer sb = new StringBuffer("XIDKey(format:");
      sb.append(Integer.toHexString(formatId));
      if (globalTxId == null)
      {
        sb.append(",global:null");
      }
      else
      {
        sb.append(",global:");
        for (int i = 0; i < globalTxId.length; ++i)
        {
          sb.append(hexValues[(globalTxId[i] >> 4) & 0xf]);
          sb.append(hexValues[globalTxId[i] & 0xf]);
        }
      }
      cachedToString = sb.toString();
    }

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "toString", cachedToString);
    return cachedToString;
  }
}
