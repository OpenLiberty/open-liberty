/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws390.ola.jca;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import javax.resource.ResourceException;

import org.omg.CORBA.SystemException;

import com.ibm.websphere.ras.TraceComponent;                     /* @F003691A*/
import com.ibm.websphere.ras.Tr;                                 /* @F003691A*/

import com.ibm.ejs.util.ByteArray;                               /* @F003691A*/

public class XAResourceImpl implements XAResource
{
  /**
   * TR tracing 
   */
  private static final TraceComponent tc = Tr.register(                /* @F003691A*/
    XAResourceImpl.class,
    "OLA",
    "com.ibm.ws390.ola.resources.olaMessages");
  private static final TraceUtil tu = new TraceUtil(tc);

	/**
	 * Trace header.
	 */
	private String header = 
    new String(" !!XAResourceImpl " + System.identityHashCode(this) + ": ");

  /**
   * Length of a serialized XID
   */
  public static final int MAX_SERIALIZED_XID_LEN = 140;

  /**
   * Debug flag
   */
  private boolean _debug = false;

  /**
   * Associated ManagedConnection used for start/end calls
   */
  private ManagedConnectionImpl _mc = null;

  /**
   * The register name specified on the ManagedConnectionFactory object.
   * This is the name that is logged in the XA partner log and must be used
   * for all transactional work by this XAResource object.
   */
  private String _registerNameFromMCF = null;

  /**
   * The register name in byte form, Cp1047
   */
  private byte[] _registerNameFromMCFBytes = null;

  /**
   * Constructor
   */
  public XAResourceImpl(ManagedConnectionImpl mc, 
                        String registerNameFromMCF,
                        boolean debug)
  {
    _mc = mc;
    _registerNameFromMCF = registerNameFromMCF;
    _debug = debug;

    try
    {
      if (_registerNameFromMCF != null)
      {
        _registerNameFromMCFBytes = _registerNameFromMCF.getBytes("Cp1047");
      }
    }
    catch (Throwable t)
    {
      throw new IllegalArgumentException("Could not serialize register name", t);
    }
  }

  public void commit(Xid xid, boolean onePhase) throws XAException
  {
    tu.debug(header + "commit enter, " + onePhase + 
                      ", " + ((xid == null) ? "null" : xid.toString()), _debug);

    /*---------------------------------------------------------------------*/
    /* Make sure an XID was specified.                                     */
    /*---------------------------------------------------------------------*/
    if (xid == null)
    {
      throw new OLAXAException(XAException.XAER_INVAL,
                               new IllegalArgumentException("Xid was null"));
    }

    /*---------------------------------------------------------------------*/
    /* Serialize the XID.                                                  */
    /*---------------------------------------------------------------------*/
    byte[] xidBytes = serializeXid(xid);

    /*---------------------------------------------------------------------*/
    /* Send the commit request to the CR for processing.                   */
    /*---------------------------------------------------------------------*/
    try
    {
      if ((_mc.isZOS() == true) &&                             /*  @703982C*/
          (_mc.getRemoteProxyInformation() == null))           /*  @703982A*/
      {                                                        /* @F003705A*/
        OptConnOutboundUtil.notifyTransaction(
          _registerNameFromMCFBytes.length, 
          _registerNameFromMCFBytes, 
          0, /* Connection ID */
          xidBytes.length,
          xidBytes,
          true,  /* Commit */
          onePhase);  /* Prepare */
      }                                                        /* @F003705A*/
    }
    catch (OLARollbackException rbe)
    {
      Tr.error(tc, "CWWKB0381E", new Object[]                    /* @F003691A*/
        {"Committing", _registerNameFromMCF});                 /* @F003691A*/
      Tr.error(tc, "CWWKB0390E", new Object[] { xid });          /* @PM88131A*/

      /*-------------------------------------------------------------------*/
      /* Backout is a valid outcome for a one phase commit.  We notify the */
      /* transaction service of this by using a rollback exception code.   */
      /* For a two phase commit, this is an error case and we don't know   */
      /* if our partner has, or can, complete the work, so we use the      */
      /* generic RMFAIL exception code.                                    */
      /*-------------------------------------------------------------------*/
      if (onePhase == true)
      {
        throw new OLAXAException(XAException.XA_RBROLLBACK, rbe);
      }
      else
      {
        throw new OLAXAException(XAException.XAER_RMFAIL, rbe);
      }
    }
    catch (OLAHeuristicException hme)
    {
      Tr.error(tc, "CWWKB0381E", new Object[]                    /* @F003691A*/
        {"Committing", _registerNameFromMCF});                 /* @F003691A*/
      Tr.error(tc, "CWWKB0390E", new Object[] { xid });          /* @PM88131A*/

      throw new OLAXAException(XAException.XA_HEURMIX, hme);
    }
    catch (OLATidNotFoundException tnfe)                       /* @F003691A*/
    {                                                          /* @F003691A*/
      /*-------------------------------------------------------------------*/
      /* Do not print the error message for TID not found, as this is an   */
      /* expected case in the recovery path.                               */
      /*-------------------------------------------------------------------*/
      throw new OLAXAException(XAException.XAER_NOTA, tnfe);   /* @F003691A*/
    }                                                          /* @F003691A*/
    catch (OLANativeException one)
    {
      Tr.error(tc, "CWWKB0381E", new Object[]                    /* @F003691A*/
        {"Committing", _registerNameFromMCF});                 /* @F003691A*/
      Tr.error(tc, "CWWKB0390E", new Object[] { xid });          /* @PM88131A*/

      throw new OLAXAException(XAException.XAER_RMFAIL, one);
    }
    catch (SystemException syse)
    {
      Tr.error(tc, "CWWKB0381E", new Object[]                    /* @F003691A*/
        {"Committing", _registerNameFromMCF});                 /* @F003691A*/
      Tr.error(tc, "CWWKB0390E", new Object[] { xid });          /* @PM88131A*/

      throw new OLAXAException(XAException.XAER_RMFAIL, syse);
    }

    tu.debug(header + "commit exit", _debug);
  }

  public void end(Xid xid, int flags) throws XAException
  {
    tu.debug(header + "end enter, " + Integer.toHexString(flags) + 
             ", " + xid.toString(), _debug);

    /*---------------------------------------------------------------------*/
    /* Tell the managed connection that we are leaving the transaction.    */
    /* If this transaction is not associated with the managed connection,  */
    /* an exception will be thrown.                                        */
    /*---------------------------------------------------------------------*/
    try
    {
      _mc.setXid(xid, false);
    }
    catch (ResourceException rex)
    {
      throw new OLAXAException(XAException.XAER_PROTO, rex);
    }

    tu.debug(header + "end exit", _debug);
  }

  public void forget(Xid xid)
  {
    tu.debug(header + "forget enter, " + xid.toString(), _debug);

    /*---------------------------------------------------------------------*/
    /* CICS does not remember heuristic outcomes (as far as we can tell)   */
    /* so there is nothing for us to do here.                              */
    /*---------------------------------------------------------------------*/

    tu.debug(header + "forget exit", _debug);
  }

  
  /**
   * Gets the transaction timeout value for transactions created using this
   * XAResource.  This function is not supported.
   */
  public int getTransactionTimeout()
  {
    return -1;
  }


  public boolean isSameRM(XAResource xares)
  {
    boolean sameRM = false;

    tu.debug(header + "isSameRM enter, " + xares.toString(), _debug);

    /*---------------------------------------------------------------------*/
    /* The WAS transaction manager will not drive isSameRM unless we tell  */
    /* it to, by specifying our own XAResourceInfo class (not part of the  */
    /* JTA specification) which implements a marker interface provided by  */
    /* WAS.  There are optimizations which could be performed if we were   */
    /* to keep track of the various transaction branches used by different */
    /* XAResource instances which are the "same" RM, but the simplest      */
    /* thing to do is to just log once per XAResource instance, which is   */
    /* what every other XAResource currently in use by WAS does.           */
    /*---------------------------------------------------------------------*/

    tu.debug(header + "isSameRM exit, " + sameRM, _debug);

    return sameRM;
  }

  public int prepare(Xid xid) throws XAException
  {
    int prepareRC = XAResource.XA_OK;

    tu.debug(header + "prepare enter, " + ((xid != null) ? xid.toString() : "null"), _debug);

    /*---------------------------------------------------------------------*/
    /* Make sure an XID was specified.                                     */
    /*---------------------------------------------------------------------*/
    if (xid == null)
    {
      throw new OLAXAException(XAException.XAER_INVAL,
                               new IllegalArgumentException("Xid was null"));
    }

    /*---------------------------------------------------------------------*/
    /* Serialize the XID.                                                  */
    /*---------------------------------------------------------------------*/
    byte[] xidBytes = serializeXid(xid);

    /*---------------------------------------------------------------------*/
    /* Send the prepare request to the CR for processing.                  */
    /*---------------------------------------------------------------------*/
    boolean readOnly = false;

    try
    {
      if ((_mc.isZOS() == true) &&                             /*  @703982C*/
          (_mc.getRemoteProxyInformation() == null))           /*  @703982A*/
      {                                                        /* @F003705A*/
        readOnly = OptConnOutboundUtil.notifyTransaction(
          _registerNameFromMCFBytes.length, 
          _registerNameFromMCFBytes, 
          0, /* Connection ID */
          xidBytes.length,
          xidBytes,
          false,  /* Commit */
          true);  /* Prepare */
      }                                                        /* @F003705A*/

      if (readOnly == true) prepareRC = XAResource.XA_RDONLY;
    }
    catch (OLARollbackException rbe)
    {
      Tr.error(tc, "CWWKB0381E", new Object[]                    /* @PM88131A*/
        {"Preparing", _registerNameFromMCF});                  /* @PM88131A*/
      Tr.error(tc, "CWWKB0390E", new Object[] { xid });          /* @PM88131A*/

      throw new OLAXAException(XAException.XA_RBROLLBACK, rbe);
    }
    catch (OLAHeuristicException hme)
    {
      Tr.error(tc, "CWWKB0381E", new Object[]                    /* @PM88131A*/
        {"Preparing", _registerNameFromMCF});                  /* @PM88131A*/
      Tr.error(tc, "CWWKB0390E", new Object[] { xid });          /* @PM88131A*/

      throw new OLAXAException(XAException.XA_HEURMIX, hme);
    }
    catch (OLANativeException one)
    {
      Tr.error(tc, "CWWKB0381E", new Object[]                    /* @PM88131A*/
        {"Preparing", _registerNameFromMCF});                  /* @PM88131A*/
      Tr.error(tc, "CWWKB0390E", new Object[] { xid });          /* @PM88131A*/

      throw new OLAXAException(XAException.XAER_RMFAIL, one);
    }
    catch (SystemException syse)
    {
      Tr.error(tc, "CWWKB0381E", new Object[]                    /* @PM88131A*/
        {"Preparing", _registerNameFromMCF});                  /* @PM88131A*/
      Tr.error(tc, "CWWKB0390E", new Object[] { xid });          /* @PM88131A*/

      throw new OLAXAException(XAException.XAER_RMFAIL, syse);
    }

    tu.debug(header + "prepare exit, " + prepareRC, _debug);

    return prepareRC;
  }

  /**
   * Returns a list of Xids for transactions that are in-doubt.
   */
  public Xid[] recover(int flag) throws XAException            /* @F003691C*/
  {
    Xid[] xids = null;

    tu.debug(header + "recover enter, " + Integer.toHexString(flag), _debug);

    /*---------------------------------------------------------------------*/
    /* The WebSphere TM will always pass START and END scan flags.  The    */
    /* JTA documentation is unclear about how to specify the begining and  */
    /* ending of a scan -- is each thread allowed to scan at the same time */
    /* or is only one scan allowed per XA resource at a time?  Therefore,  */
    /* only allow the recover call if both START and END are passed.       */
    /*---------------------------------------------------------------------*/
    if (((flag & XAResource.TMSTARTRSCAN) == 0) ||             /* @F003691A*/
        ((flag & XAResource.TMENDRSCAN) == 0))                 /* @F003691A*/
    {
      throw new OLAXAException(XAException.XAER_PROTO,         /* @F003691A*/
        new IllegalArgumentException("Resource manager expects TMSTARTRSCAN " +
        "and TMENDRSCAN but received " + Integer.toHexString(flag)));
    }                                                          /* @F003691A*/
    
    try                                                        /* @F003691A*/
    {                                                          /*2@F003691A*/
      /*-------------------------------------------------------------------*/
      /* Get the list of XIDs, returned in 140 byte segments.              */
      /*-------------------------------------------------------------------*/
      byte[] tidBytes = null;
      
      if ((_mc.isZOS() == true) &&                             /*  @703982C*/
          (_mc.getRemoteProxyInformation() == null))           /*  @703982A*/
      {                                                        /* @F003705A*/
        tidBytes = OptConnOutboundUtil.recover(
          _registerNameFromMCFBytes.length,                    /* @F003691A*/
          _registerNameFromMCFBytes,                           /* @F003691A*/
          0); /* Connection ID */                              /* @F003691A*/
      }                                                        /* @F003705A*/

      int numXids = 0;                                         /* @F003691A*/

      if (tidBytes != null)                                    /* @F003691A*/
      {                                                        /* @F003691A*/
        numXids = tidBytes.length / 140;                       /* @F003691A*/

        tu.debug(header + "Received " + numXids +              /* @F003691A*/
           " XIDs from resource backend", _debug);             /* @F003691A*/

        java.nio.ByteBuffer bigXidBuffer =                     /* @F003691A*/
          java.nio.ByteBuffer.wrap(tidBytes);                  /* @F003691A*/
        
        xids = new Xid[numXids];                               /* @F003691A*/

        for (int x = 0; x < numXids; x++)                      /* @F003691A*/
        {                                                      /* @F003691A*/
          int formatId = bigXidBuffer.getInt();                /* @F003691A*/
          int gtridLen = bigXidBuffer.getInt();                /* @F003691A*/
          int bqualLen = bigXidBuffer.getInt();                /* @F003691A*/
          byte[] gtrid = new byte[gtridLen];                   /* @F003691A*/
          byte[] bqual = new byte[bqualLen];                   /* @F003691A*/
          bigXidBuffer.get(gtrid);                             /* @F003691A*/
          bigXidBuffer.get(bqual);                             /* @F003691A*/
          bigXidBuffer.get(new byte[128 - gtridLen - bqualLen]);/*@F003691A*/
          xids[x] = new OLAXid(formatId, gtrid, bqual);        /* @F003691A*/

          ByteArray gtridByteArray =                           /* @F003691A*/
            new ByteArray(xids[x].getGlobalTransactionId());   /* @F003691A*/
          ByteArray bqualByteArray =                           /* @F003691A*/
            new ByteArray(xids[x].getBranchQualifier());       /*4@F003691A*/
          tu.debug(header + "Xid[" + x + "] Format ID: " +
                   Integer.toHexString(xids[x].getFormatId()), _debug);
          tu.debug(header + "Xid[" + x + "] Gtrid: " +
                   gtridByteArray.toString(), _debug);         /*2@F003691A*/
          tu.debug(header + "Xid[" + x + "] Bqual: " + 
                   bqualByteArray.toString(), _debug);         /* @F003691A*/
        }                                                      /* @F003691A*/
      }                                                        /* @F003691A*/
      else                                                     /* @F003691A*/
      {                                                        /* @F003691A*/
        xids = new Xid[0];                                     /* @F003691A*/
      }                                                        /* @F003691A*/
    }                                                          /* @F003691A*/
    catch (OLANativeException one)                             /* @F003691A*/
    {                                                          /* @F003691A*/
      Tr.error(tc, "CWWKB0380E", _registerNameFromMCF);          /* @F003691A*/

      throw new OLAXAException(XAException.XAER_RMFAIL, one);  /* @F003691A*/
    }                                                          /* @F003691A*/
    catch (SystemException syse)                               /* @F003691A*/
    {                                                          /* @F003691A*/
      Tr.error(tc, "CWWKB0380E", _registerNameFromMCF);          /* @F003691A*/

      throw new OLAXAException(XAException.XAER_RMFAIL, syse); /* @F003691A*/
    }                                                          /* @F003691A*/

    tu.debug(header + "recover exit, " + xids, _debug);

    return xids;
  }

  public void rollback(Xid xid) throws XAException
  {
    tu.debug(header + "rollback enter, " + ((xid == null) ? "null" : xid.toString()), _debug);

    /*---------------------------------------------------------------------*/
    /* Make sure an XID was specified.                                     */
    /*---------------------------------------------------------------------*/
    if (xid == null)
    {
      throw new OLAXAException(XAException.XAER_INVAL,
                               new IllegalArgumentException("Xid was null"));
    }

    /*---------------------------------------------------------------------*/
    /* Serialize the XID.                                                  */
    /*---------------------------------------------------------------------*/
    byte[] xidBytes = serializeXid(xid);

    /*---------------------------------------------------------------------*/
    /* Send the prepare request to the CR for processing.                  */
    /*---------------------------------------------------------------------*/
    try
    {
      if ((_mc.isZOS() == true) &&                             /*  @703982C*/
          (_mc.getRemoteProxyInformation() == null))           /*  @703982A*/
      {                                                        /* @F003705A*/
        OptConnOutboundUtil.notifyTransaction(
          _registerNameFromMCFBytes.length, 
          _registerNameFromMCFBytes, 
          0, /* Connection ID */
          xidBytes.length,
          xidBytes,
          false,  /* Commit */
          false);  /* Prepare */
      }                                                        /* @F003705A*/
    }
    catch (OLAHeuristicException hme)
    {
      Tr.error(tc, "CWWKB0381E", new Object[]                    /* @F003691A*/
        {"Rolling back", _registerNameFromMCF});               /* @F003691A*/
      Tr.error(tc, "CWWKB0390E", new Object[] { xid });          /* @PM88131A*/

      throw new OLAXAException(XAException.XA_HEURMIX, hme);
    }
    catch (OLATidNotFoundException tnfe)                       /* @F003691A*/
    {                                                          /* @F003691A*/
      /*-------------------------------------------------------------------*/
      /* Do not print the error message for TID not found, as this is an   */
      /* expected case in the recovery path.                               */
      /*-------------------------------------------------------------------*/
      throw new OLAXAException(XAException.XAER_NOTA, tnfe);   /* @F003691A*/
    }                                                          /* @F003691A*/
    catch (OLANativeException one)
    {
      Tr.error(tc, "CWWKB0381E", new Object[]                    /* @F003691A*/
        {"Rolling back", _registerNameFromMCF});               /* @F003691A*/
      Tr.error(tc, "CWWKB0390E", new Object[] { xid });          /* @PM88131A*/

      throw new OLAXAException(XAException.XAER_RMFAIL, one);
    }
    catch (SystemException syse)
    {
      Tr.error(tc, "CWWKB0381E", new Object[]                    /* @F003691A*/
        {"Rolling back", _registerNameFromMCF});               /* @F003691A*/
      Tr.error(tc, "CWWKB0390E", new Object[] { xid });          /* @PM88131A*/

      throw new OLAXAException(XAException.XAER_RMFAIL, syse);
    }

    tu.debug(header + "rollback exit", _debug);
  }


  /**
   * Sets the transaction timeout for transactions created using this
   * XAResource.  This function is not supported for OLA.
   */
  public boolean setTransactionTimeout(int seconds)
  {
    return false;
  }

  
  /**
   * Called by the app server when this resource is enlisted in the current
   * global transaction
   */
  public void start(Xid xid, int flags) throws XAException
  {
    tu.debug(header + "start enter, " + Integer.toHexString(flags) + ", " +
             xid.toString(), _debug);

    /*---------------------------------------------------------------------*/
    /* Tell the managed connection that we are joining the transaction.    */
    /* If there is already a transaction associated with the managed       */
    /* connection, an exception will be thrown.                            */
    /*---------------------------------------------------------------------*/
    try
    {
      _mc.setXid(xid, true);

      if ((_mc.isZOS() == false) ||                            /*  @703982C*/
          (_mc.getRemoteProxyInformation() != null))           /*  @703982C*/
      {                                                        /*2@F003705A*/
        OptConnOutboundUtil.issueProxyMessageMethod();
      }                                                        /* @F003705A*/
    }
    catch (ResourceException rex)
    {
      throw new OLAXAException(XAException.XAER_PROTO, rex);
    }

    tu.debug(header + "start exit", _debug);
  }

  
  /**
   * Serializes an Xid into a byte[]
   * @param xid The Xid to serialize
   * @return A byte array containing the serialized XID.  The byte array
   *         will always be MAX_SERIALIZED_XID_LEN bytes long, regardless of
   *         how big the XID is.
   */
  static byte[] serializeXid(Xid xid)
  {
    byte[] xidBytes = new byte[MAX_SERIALIZED_XID_LEN];
    
    if (xid != null)                               
    {                                              
      java.nio.ByteBuffer xidBB =                  
        java.nio.ByteBuffer.wrap(xidBytes);        
      byte[] gtrid = xid.getGlobalTransactionId(); 
      byte[] bqual = xid.getBranchQualifier();     
      xidBB.putInt(xid.getFormatId());             
      xidBB.putInt(gtrid.length);                  
      xidBB.putInt(bqual.length);                  
      xidBB.put(gtrid);                            
      xidBB.put(bqual);                            
    }                                              
    
    return xidBytes;
  }

  /**
   * Internal XID implementation for XIDs that we deserialize.
   */
  private class OLAXid implements Xid                            /* @F003691A*/
  {
    private int _formatId = 0;
    private byte[] _gtrid = null;
    private byte[] _bqual = null;

    private OLAXid(int formatId, byte[] gtrid, byte[] bqual)
    {
      _formatId = formatId;
      _gtrid = gtrid;
      _bqual = bqual;
    }

    public byte[] getBranchQualifier() { return _bqual; }
    public byte[] getGlobalTransactionId() { return _gtrid; }
    public int getFormatId() { return _formatId; }
  }
}