/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.common;

import java.util.Arrays;

import javax.transaction.xa.Xid;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.util.Util;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This is the JetStream comms implementation of an Xid. The main purpose of 
 * this class is to allow the meaningful fields pertaining to an Xid to be 
 * transmitted across a netowrk.
 * <p>
 * The class should be used in the following way:
 * <p>
 * When wishing to send an existing Xid then use the constructor that takes
 * the Xid and then use the <code>serialize</code> method to add the bytes
 * to a <code>WsByteBuffer</code>.
 * <p>
 * When wishing to reconstruct an Xid, use the constructor that takes a
 * <code>WsByteBuffer</code>.
 * <p>
 * The XID Structure is as follows:
 * <p>
 * BIT32    Format ID
 * BIT32    Global transaction ID length
 * BYTE[]   Global transaction ID
 * BIT32    Branch qualifier length
 * BYTE[]   Branch qualifier
 * 
 * @author Gareth Matthews
 */
public class XidProxy implements Xid
{
   /** Register Class with Trace Component */
   private static final TraceComponent tc = SibTr.register(XidProxy.class,
                                                           CommsConstants.MSG_GROUP, 
                                                           CommsConstants.MSG_BUNDLE);
   
   /** The format id */
   private int formatId;
   
   /** The branch qualifier */
   private byte[] branchQualifier;
   
   /** The global transaction id */
   private byte[] globalTransactionId;
   
   /** Log source info on class load */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source info: @(#)SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/common/XidProxy.java, SIB.comms, WASX.SIB, uu1215.01 1.20");
   }
   
   /**
    * Originating side constructor. This constructor is designed to be used when
    * we want to take an existing Xid and serialize it in the future.
    * 
    * @param xid The Xid we want to serialize.
    */
   public XidProxy(Xid xid)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "<init>");
      
      this.formatId = xid.getFormatId();
      this.branchQualifier = xid.getBranchQualifier();
      this.globalTransactionId = xid.getGlobalTransactionId();
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, this.toString());
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
   }
   
   /**
    * Constructor that allows manual creation of an Xid.
    * 
    * @param formatId
    * @param globalTransactionId
    * @param branchQualifier
    */
   public XidProxy(int formatId, byte[] globalTransactionId, byte[] branchQualifier)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "<init>");
      
      this.formatId = formatId;
      if (branchQualifier != null)
      {
         this.branchQualifier = new byte[branchQualifier.length];
         System.arraycopy(branchQualifier, 0, this.branchQualifier, 0, this.branchQualifier.length);
      }
      if (globalTransactionId != null)
      {
         this.globalTransactionId = new byte[globalTransactionId.length];
         System.arraycopy(globalTransactionId, 0, this.globalTransactionId, 0, this.globalTransactionId.length);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, this.toString());
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
   }
   
   /**
    * @return Returns the format Id
    */
   public int getFormatId()
   {
      return formatId;
   }
   
   /**
    * @return Returns the branch qualifier
    */
   public byte[] getBranchQualifier()
   {
      final byte[] result = new byte[branchQualifier.length];
      System.arraycopy(branchQualifier, 0, result, 0, result.length);
      return result;
   }
   
   /**
    * @return Returns the global transaction id
    */
   public byte[] getGlobalTransactionId()
   {
      final byte[] result = new byte[globalTransactionId.length];
      System.arraycopy(globalTransactionId, 0, result, 0, result.length);
      return result;
   }
   
   /**
    * Tests for equality between the object passed in and this object.
    * 
    * @param anOther
    */
   public boolean equals(Object anOther)
   {
      if (anOther == null)
         return false;
      
      if (!(anOther instanceof XidProxy))
         return false;
      
      XidProxy otherXid = (XidProxy) anOther;
      
      if (formatId != otherXid.getFormatId())
         return false;
      
      if (!Arrays.equals(branchQualifier, otherXid.getBranchQualifier()))
         return false;
      
      if (!Arrays.equals(globalTransactionId, otherXid.getGlobalTransactionId()))
         return false;
      
      return true;
   }
   
   /**
    * @return Returns a hashCode for this object.
    */
   public int hashCode()
   {
      // Nothing we can do with BQual less than 4 bytes in length (doesn't normally happen)
      if (branchQualifier.length < 4) return 0;
      
      // Calculate the hascode by getting the last 4 bytes of the branch qualifier 
      int offset = (branchQualifier.length - 4);
      
      return ((0xFF & branchQualifier[offset + 0]) << 24) +
             ((0xFF & branchQualifier[offset + 1]) << 16) +
             ((0xFF & branchQualifier[offset + 2]) << 8)  +
             ((0xFF & branchQualifier[offset + 3]) << 0);
   }
   
   /**
    * @return Returns a String representing this XID
    */
   public String toString()
   {
      return "JS Comms Xid: Format(" + formatId 
             + "), Branch Qualifier[" + Util.toHexString(branchQualifier) 
             + "], Global Transaction ID[" + Util.toHexString(globalTransactionId) + "]"; 
   }
}
