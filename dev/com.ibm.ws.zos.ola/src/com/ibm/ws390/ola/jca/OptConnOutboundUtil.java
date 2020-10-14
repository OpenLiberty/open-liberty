/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws390.ola.jca;

public final class OptConnOutboundUtil
{
  static boolean issueRarMessage = true;                         /* @F003691A*/
  static boolean issueProxyMessage = true;                       /* @F003705A*/

  public final static void issueProxyMessageMethod()            /* @F003705A*/
  {
    /*----------------------------------------------------------------------*/
    /* Only issue the proxy transactions message once.  This is not tightly */
    /* serialized since issuing the message more than once is not harmful   */
    /* to anyone.                                                           */
    /*----------------------------------------------------------------------*/
    try
    {
      if (issueProxyMessage == true)
      {
    	  // TODO: Figure out how to issue this message in Liberty.
    	  // TODO: Figure out if we need to issue this message in Liberty (if we support the proxy).
/*        Logger logger = Logger.getLogger(OptConnOutboundUtil.class.getName(),
                                         "com.ibm.ejs.resources.olaMessages");
        LoggerHelper.setAttributes(logger, "IBM", "WebSphere", "RAS",
                                   com.ibm.websphere.logging.WsLevel.DETAIL);
        logger.logp(WsLevel.INFO,
                    OptConnOutboundUtil.class.getName(),
                    "driveOutboundJava",
                    "BBOA0037");
        issueProxyMessage = false;
        */
      }
    }
    catch (Throwable t)
    {
      /* Could not trace....... */
      System.err.println(t.toString());
      t.printStackTrace();
    }
  }

  /**
   * Method used OLA to drive a request outbound to an external address space.
   */
  public final static byte[] driveOutbound(int inputLen, byte[] input, int connBytesLen, byte[] connBytes, int servnameLen, byte[] servname, byte[] outputFlags) {
	  // TODO: Liberty: implement native method...
	  throw new RuntimeException("Native method not implemented in Liberty");
  }

  /**
   * Method used OLA to drive transaction syncpoint requests
   */
  public final static boolean notifyTransaction(int regnameLen, byte[] regname, int connectionID, int xidLen, byte[] xidByteArray, boolean commit, boolean prepare)
    throws OLANativeException {   /* @F003691C*/
	  // TODO: Liberty: implement native method...
	  throw new RuntimeException("Native method not implemented in Liberty");
  }

  /**
   * Method used by OLA to drive XA recover with a CICS link server.
   */
  public final static byte[] recover(int regnameLen, byte[] regname, int connectionID)
    throws OLANativeException {   /* @F003691A*/
	  // TODO: Liberty: implement native method...
	  throw new RuntimeException("Native method not implemented in Liberty");
  }
}
