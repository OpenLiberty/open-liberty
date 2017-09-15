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

import java.util.List;

import com.ibm.ws.sib.mfp.*;
import com.ibm.ws.sib.mfp.schema.TrmAccess;
import com.ibm.ws.sib.mfp.trm.*;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *  TrmRouteDataImpl extends TrmMessageImpl and hence JsMessageImpl,
 *  and is the implementation class for the TrmRouteData interface.
 */
final class TrmRouteDataImpl extends TrmMessageImpl implements TrmRouteData {

  private final static long serialVersionUID = 1L;

  private static TraceComponent tc = SibTr.register(TrmRouteDataImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);


  /* **************************************************************************/
  /* Constructors                                                             */
  /* **************************************************************************/

  /**
   *  Constructor for a new TRM Route Data Message.
   *
   *  This constructor should never be used except by JsMessageImpl.createNew().
   *  The method must not actually do anything.
   */
  TrmRouteDataImpl() {
  }

  /**
   *  Constructor for a new TRM Route Data Message.
   *  To be called only by the TrmMessageFactory.
   *
   *  @param flag No-op flag to distinguish different constructors.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  TrmRouteDataImpl(int flag) throws MessageDecodeFailedException {
    super(flag);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>");
    setSubtype(TrmMessageType.ROUTE_DATA_INT);
  }


  /**
   *  Constructor for an inbound message.
   *  (Only to be called by a superclass make method.)
   *
   *  @param inJmo The JsMsgObject representing the inbound method.
   */
  TrmRouteDataImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>, inbound jmo ");
  }


  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /*
   *  Get the originator SIBUuid from the message.
   *
   *  Javadoc description supplied by TrmRouteData interface.
   */
  public final SIBUuid8 getOriginator () {
    byte[] a = (byte[])getPayload().getField(TrmAccess.BODY_ROUTEDATA_ORIGINATOR);

    if (a != null) {
      return new SIBUuid8(a);
    }
    else {
      return null;
    }
  }

  /*
   *  Get the route data cellules
   *
   *  Javadoc description supplied by TrmRouteData interface.
   */
  public final List getCellules () {
    // Note: Caller does not modify the returned list, so there is
    //       no need to copy it.
    return (List)getPayload().getField(TrmAccess.BODY_ROUTEDATA_ROUTECOST_CELLULES);
  }

  /*
   *  Get the route data costs
   *
   *  Javadoc description supplied by TrmRouteData interface.
   */
  public final List getCosts () {
    // Note: Caller does not modify the returned list, so there is
    //       no need to copy it.
    return (List)getPayload().getField(TrmAccess.BODY_ROUTEDATA_ROUTECOST_COSTS);
  }

  /*
   * Summary information for TRM messages
   */
  public void getTraceSummaryLine(StringBuilder buff) {
    super.getTraceSummaryLine(buff);
    
    buff.append(",originator=");
    buff.append(getOriginator());
    
    appendList(buff,"cellules",getCellules());
    
    appendList(buff,"costs",getCosts());
    
  }

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /*
   *  Set the originator SIBUuid in the message.
   *
   *  Javadoc description supplied by TrmRouteData interface.
   */
  public final void setOriginator (SIBUuid8 value) {
    if (value != null) {
      byte[] a = value.toByteArray();
      getPayload().setField(TrmAccess.BODY_ROUTEDATA_ORIGINATOR, a);
    } else {
      getPayload().setField(TrmAccess.BODY_ROUTEDATA_ORIGINATOR, null);
    }
  }

  /*
   *  Set the route data cellules
   *
   *  Javadoc description supplied by TrmRouteData interface.
   */
  public final void setCellules (List value) {
    getPayload().setField(TrmAccess.BODY_ROUTEDATA_ROUTECOST_CELLULES, value);
  }

  /*
   *  Set the route data costs
   *
   *  Javadoc description supplied by TrmRouteData interface.
   */
  public final void setCosts (List value) {
    getPayload().setField(TrmAccess.BODY_ROUTEDATA_ROUTECOST_COSTS, value);
  }
}
