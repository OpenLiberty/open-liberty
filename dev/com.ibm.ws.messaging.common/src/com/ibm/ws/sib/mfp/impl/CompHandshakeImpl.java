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

package com.ibm.ws.sib.mfp.impl;

import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.sib.comms.CommsConnection;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.CompHandshake;
//import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * This class implements the Comms ComponentHandshake interface.  A singleton
 * instance of this class is created and can be obtained by Comms using the
 * ComponentHandshakeFactory.  Methods in this instance are called when a
 * connection is created, closed or when schema definition data is sent.
 *
 * We use these events to manage our "who knows about which schema" tables.
 */

public class CompHandshakeImpl implements CompHandshake {
  private static TraceComponent tc = SibTr.register(CompHandshakeImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /*
   * This method will be invoked when a new connection between two JVMs is being
   * established; it is invoked on the sender-side of the connection.
   *
   * We use this oppertunity to send our list of currently known schema ids to
   * the receiver and to get back the receiver's list.
   */
  public boolean compStartHandshake(CommsConnection conn, int version) {
    try {
      SchemaManager.openLink(conn);
      return true;
    } catch (Exception ex) {
      FFDCFilter.processException(ex, "compStartHandshake", "67", this);
      return false;
    }
  }

  /*
   * This method will be invoked when data is received on a connection from a
   * corresponding 'CommsConnection.sendMFPSchema()' method.
   * We use this to transfer lists of schema definitions.
   */
  public boolean compData(CommsConnection conn, int version, byte[] data) {
    try {
      SchemaManager.receiveSchemas(conn, data);
      return true;
    } catch (Exception ex) {
      FFDCFilter.processException(ex, "compData", "82", this);
      return false;
    }
  }

  /*
   * This method will be invoked when data is received on a connection from a
   * corresponding 'CommsConnection.mfpHandshakeExchange()' method.
   * We use this to transfer lists of known schema ids.
   */
  public byte[] compHandshakeData(CommsConnection conn, int version, byte[] data) {
    try {
      return SchemaManager.receiveHandshake(conn, data);
    } catch (Exception ex) {
      FFDCFilter.processException(ex, "compData", "96", this);
      return null;
    }
  }

  /*
   * This method will be invoked when an existing connection between two JVMs is
   * closed; it is invoked at both ends of the connection.
   *
   * We use this oppertunity to tidy up our tables of known schema ids
   */
  public void compClose(CommsConnection conn) {
    SchemaManager.closeLink(conn);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.comms.CompHandshake#compRequest(com.ibm.ws.sib.comms.CommsConnection, int, int, byte[])
   *
   * This method will be invoked when data is received on a connection from a
   * corresponding 'CommsConnection.requestMFPSchemata()' method. The data
   * supplied is an encoded list of schema ids. We return the appropriate encoded
   * schemas.
   */
  public byte[] compRequest(CommsConnection conn, int productVersion, int packetId, byte[] data) {
     //Venu liberty change
     // using CommsConstants instead of JfapChanelConstants
    if(packetId == CommsConstants.SEG_REQUEST_SCHEMA){
      // get the requested schemata from the SchemaManager
      return SchemaManager.getEncodedSchemataByEncodedIDs(data);
    }
    else{
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Invalid packetId");
      SIErrorException e = new SIErrorException();
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.CompHandshakeImpl.compRequest", "123", this);
      throw e;
    }
  }
}
