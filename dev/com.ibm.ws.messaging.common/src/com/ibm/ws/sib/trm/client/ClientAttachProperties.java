/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.trm.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.ws.sib.trm.impl.TrmConstantsImpl;
import com.ibm.ws.sib.utils.comms.ProviderEndPoint;
import com.ibm.ws.sib.utils.comms.ProviderEndPoint.IncorrectCallException;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.trm.SibTrmConstants;

/**
 * Provides a connection properties parser for client attachment
 *
 * NB. Class must be public since the static method is accessed from Bridge.java, which is
 * in a different package.
 */
public final class ClientAttachProperties {

 
  private static final TraceComponent tc = SibTr.register(ClientAttachProperties.class, TrmConstantsImpl.MSG_GROUP, TrmConstantsImpl.MSG_BUNDLE);
  private static final TraceNLS nls = TraceNLS.getTraceNLS(TrmConstantsImpl.MSG_BUNDLE);

   // Local variables

  private Map    props                = null;
  private String busName              = null;
  private String targetGroupName      = null;
  private String targetGroupType      = null;
  private String targetSignificance   = null;
  private String targetTransportChain = null;
  private String connectionProximity  = null;
  private List<ProviderEndPoint> endpoints = new ArrayList<ProviderEndPoint>();
  private String connectionMode       = null;                                                                 //LIDB3645

  // Indicate whether the EPs described on this object are user defined.
  private boolean userDefinedEPs = false;

  public ClientAttachProperties (Map ps, boolean isPasswordSpecified) throws SIIncorrectCallException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "ClientAttachProperties", new Object[]{ ps, isPasswordSpecified });

    // Obtain the bus name - this is a mandatory property so throw an exception if it doesn't exist

    props = ps;
    busName = (String)props.get(SibTrmConstants.BUSNAME);

    if (busName == null) {
      throw new SIIncorrectCallException(nls.getFormattedMessage("NULL_PROPERTY_CWSIT0003", new Object[]{ SibTrmConstants.BUSNAME }, null));
    }

    // Obtain the target group

    targetGroupName = (String)props.get(SibTrmConstants.TARGET_GROUP);

    if (targetGroupName != null && targetGroupName.trim().equals("")) {
      targetGroupName = null;
    }

    // Obtain the target type

    targetGroupType = (String)props.get(SibTrmConstants.TARGET_TYPE);

    if (targetGroupType != null && targetGroupType.trim().equals("")) {
      targetGroupType = null;
    }

    if (targetGroupType == null) {
      targetGroupType = SibTrmConstants.TARGET_TYPE_DEFAULT;
    }

    if (!targetGroupType.equals(SibTrmConstants.TARGET_TYPE_BUSMEMBER)   &&
        !targetGroupType.equals(SibTrmConstants.TARGET_TYPE_DESTINATION) &&
        !targetGroupType.equals(SibTrmConstants.TARGET_TYPE_CUSTOM)      &&
        !targetGroupType.equals(SibTrmConstants.TARGET_TYPE_ME)          &&
        !targetGroupType.equals(SibTrmConstants.TARGET_TYPE_MEUUID)) {

      String correctValues = SibTrmConstants.TARGET_TYPE_BUSMEMBER + "," + SibTrmConstants.TARGET_TYPE_DESTINATION + "," + SibTrmConstants.TARGET_TYPE_CUSTOM + "," + SibTrmConstants.TARGET_TYPE_ME + "," + SibTrmConstants.TARGET_TYPE_MEUUID;
      throw new SIIncorrectCallException(nls.getFormattedMessage("INVALID_TARGET_TYPE_CWSIT0055", new Object[] {targetGroupType, SibTrmConstants.TARGET_TYPE, correctValues}, null));

    }

    // Obtain the target significance

    targetSignificance =  (String)props.get(SibTrmConstants.TARGET_SIGNIFICANCE);

    if (targetSignificance != null && targetSignificance.trim().equals("")) {
      targetSignificance = null;
    }

    if (targetSignificance == null) {
      targetSignificance = SibTrmConstants.TARGET_SIGNIFICANCE_PREFERRED;
    }

    if (!targetSignificance.equals(SibTrmConstants.TARGET_SIGNIFICANCE_REQUIRED) &&
        !targetSignificance.equals(SibTrmConstants.TARGET_SIGNIFICANCE_PREFERRED)) {
      String correctValues = SibTrmConstants.TARGET_SIGNIFICANCE_REQUIRED + "," + SibTrmConstants.TARGET_SIGNIFICANCE_PREFERRED;
      throw new SIIncorrectCallException(nls.getFormattedMessage("INVALID_TARGET_TYPE_CWSIT0055", new Object[] {targetSignificance, SibTrmConstants.TARGET_SIGNIFICANCE, correctValues}, null));
    }

    // Obtain the connection proximity

    connectionProximity = (String)props.get(SibTrmConstants.CONNECTION_PROXIMITY);

    if (connectionProximity != null && connectionProximity.trim().equals("")) {
      connectionProximity = null;
    }

    if (connectionProximity == null) {
      connectionProximity = SibTrmConstants.CONNECTION_PROXIMITY_DEFAULT;
    }

    if (!connectionProximity.equals(SibTrmConstants.CONNECTION_PROXIMITY_SERVER)   &&
        !connectionProximity.equals(SibTrmConstants.CONNECTION_PROXIMITY_CLUSTER)  &&
        !connectionProximity.equals(SibTrmConstants.CONNECTION_PROXIMITY_HOST)     &&
        !connectionProximity.equals(SibTrmConstants.CONNECTION_PROXIMITY_BUS)) {

      String correctValues = SibTrmConstants.CONNECTION_PROXIMITY_SERVER + "," + SibTrmConstants.CONNECTION_PROXIMITY_CLUSTER + "," + SibTrmConstants.CONNECTION_PROXIMITY_HOST + "," + SibTrmConstants.CONNECTION_PROXIMITY_BUS;
      throw new SIIncorrectCallException(nls.getFormattedMessage("INVALID_TARGET_TYPE_CWSIT0055", new Object[] {connectionProximity, SibTrmConstants.CONNECTION_PROXIMITY, correctValues}, null));
    }

    // Obtain the provider end point data. Provider endpoints use the format
    // <host>:<port>:<chain>,<host>:<port>:<chain>,...
    String p  = (String)props.get(SibTrmConstants.PROVIDER_ENDPOINTS);

    // Call the helper method to do the actual parsing
    // The return value indicates whether the user specified their own entries.
    try
    {
      userDefinedEPs = ProviderEndPoint.parseProviderEndpoints(p, endpoints, isPasswordSpecified);
    }
    catch (IncorrectCallException e)
    {
      // No FFDC Code Needed
      //we'll let our caller sort it out
      throw new SIIncorrectCallException(nls.getFormattedMessage("INVALID_TARGET_TYPE_CWSIT0055",
          e.getInserts(), null));
    }

    // Obtain the transport chain string (d313487)
    // It is possible to specify multiple bootstrap endpoints with different bootstrap
    // transport chains, which leads to the question of which one we should match as the
    // default choice of target chain. The conclusion we reached is that generally a customer
    // would never 'fall back' to a less secure transport chain and thus we should default
    // based on the first element in the list.

    targetTransportChain = (String)props.get(SibTrmConstants.TARGET_TRANSPORT_CHAIN);

    if (targetTransportChain != null && targetTransportChain.trim().equals("")) {
      targetTransportChain = null;
    }

    if (targetTransportChain == null) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "No targetTransportChain specified");

      targetTransportChain = SibTrmConstants.TARGET_TRANSPORT_CHAIN_BASIC;

      if (!endpoints.isEmpty()) {
         ProviderEndPoint pEp = endpoints.get(0);
         if (!pEp.getChain().equalsIgnoreCase(SibTrmConstants.BOOTSTRAP_TRANSPORT_CHAIN_BASIC))
            targetTransportChain = SibTrmConstants.TARGET_TRANSPORT_CHAIN_SECURE;
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Defaulting to " + targetTransportChain);
    }

    // Obtain the connection mode                                                                               LIDB3645
    connectionMode = (String)props.get(SibTrmConstants.CONNECTION_MODE);

    if (connectionMode != null && connectionMode.trim().equals("")) {
      connectionMode = null;
    }

    if (connectionMode == null) {
      connectionMode = SibTrmConstants.CONNECTION_MODE_DEFAULT;
    }

    if (!connectionMode.equals(SibTrmConstants.CONNECTION_MODE_NORMAL)   &&
        !connectionMode.equals(SibTrmConstants.CONNECTION_MODE_RECOVERY)) {

      String correctValues = SibTrmConstants.CONNECTION_MODE_NORMAL + "," + SibTrmConstants.CONNECTION_MODE_RECOVERY;
      throw new SIIncorrectCallException(nls.getFormattedMessage("INVALID_TARGET_TYPE_CWSIT0055", new Object[] {connectionMode, SibTrmConstants.CONNECTION_MODE, correctValues}, null));
    }                                                                                                         //LIDB3645

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "ClientAttachProperties", this);
  }

  Map getProperties () {
    return props;
  }

  String getBusName () {
    return busName;
  }

  String getTargetGroupName () {
    return targetGroupName;
  }

  String getTargetGroupType () {
    return targetGroupType;
  }

  String getTargetSignificance () {
    return targetSignificance;
  }

  String getTargetTransportChain () {
    return targetTransportChain;
  }

  String getConnectionProximity () {
    return connectionProximity;
  }

  List getProviderEPs () {
    return endpoints;
  }

  String getConnectionMode () {                                                                               //LIDB3645
    return connectionMode;
  }                                                                                                           //LIDB3645

  /**
   * This method allows us to query whether the provider endpoints on this object
   * were specified by the calling application, or were defaulted by the constructor.
   *
   * This is used in TrmSICoreConectionFactoryImpl when deciding whether to try a
   * remote bootstrap connect following failure of WLM to obtain a suitable connection.
   *
   * p  230062  remote communication from a server fails
   */
  boolean isUserDefinedProviderEPs()
  {

    return userDefinedEPs;

  }//isUserDefinedProviderEP

}
