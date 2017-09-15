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

package com.ibm.wsspi.sib.core;

import java.util.Collections;
import java.util.Map;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.admin.JsAdminService;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.common.service.CommonServiceFacade;
import com.ibm.wsspi.sib.core.selector.PrivateFactoryType;
//import com.ibm.ws.sib.ra.SibRaFactory;
import com.ibm.ws.sib.trm.TrmSICoreConnectionFactory;
import com.ibm.ws.sib.utils.TraceGroups;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnectionFactory;
import com.ibm.wsspi.sib.core.exception.SIInsufficientDataForFactoryTypeException;
import com.ibm.wsspi.sib.core.selector.FactoryType;

/**
 *
 * <p>The SICoreConnectionFactorySelector returns instances of the SICoreConnectionFactory
 *   based on a FactoryType selector.
 * </p>
 *
 * <p>SIB build component: sib.core.selector</p>
 *
 * <p>The SICoreConnectionFactorySelector returns instances of the SICoreConnectionFactory.
 *   It allows the caller to specify which SICoreConnectionFactory to be created using a
 *   FactoryType. There is only a single FactoryType that can be used currently, but this
 *   class has been designed with the ability for more to be added.
 * </p>
 *
 * @author nottinga
 * @version 1.20
 * @since 1.0
 */
public final class SICoreConnectionFactorySelector
{
  /**
   * The Trace Messages Resource Bundle class name
   */
  private static final String TRACE_MESSAGES = "com.ibm.wsspi.sib.core.selector.Messages";
  /**
   * The trace group for this component.
   */
  private static final String TRACE_GROUP = TraceGroups.TRGRP_CORE;

  /**
   * The Trace component for all trace in this class.
   */
  private static final TraceComponent _tc = SibTr.register(
      SICoreConnectionFactorySelector.class, TRACE_GROUP, TRACE_MESSAGES);

  /**
   * Object used to translate all the Exception messages.
   */
  private static final TraceNLS _nls = TraceNLS.getTraceNLS(TRACE_MESSAGES);

  /**
   * <p>This method returns the SICoreConnectionFactory object requested.</p>
   * <p>The permitted value for the FactoryType is FactoryType.TRM_CONNECTION. This
   *   value will result in an SICoreConnectionFactory being returned where the connections
   *   are created by TRM.
   * </p>
   * <p>Other FactoryType objects may require extra information, in which case the use of this
   *   method will result in an SIInsufficientDataForFactoryTypeException.
   * </p>
   *
   * @param type The FactoryType representing the type of SICoreConnectionFactory the caller wishes to use.
   * @return The SICoreConnectionFactory that was requested, this may be wrapped.
   * @throws SIInsufficientDataForFactoryTypeException if the FactoryType requires extra information.
   * @throws SIIncorrectCallException      Thrown if the call parameters are not valid.
   * @throws SIResourceException           Thrown if the underlying connection factory cannot be obtained.
   */
  public static final SICoreConnectionFactory getSICoreConnectionFactory(
      FactoryType type)
  throws SIInsufficientDataForFactoryTypeException, SIIncorrectCallException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled())
    {
      SibTr.entry(_tc, "getSICoreConnectionFactory", type);
    }

    SICoreConnectionFactory factory = null;

    try
    {
      // Call the other method with an empty map.
      factory = getSICoreConnectionFactory(type, Collections.EMPTY_MAP);
    }
    // Catch and rethrow this exception type.
    catch (SIIncorrectCallException e)
    {
      com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.wsspi.sib.core.selector.SICoreConnectionFactorySelector.getSICoreConnectionFactory", "126");

      if (TraceComponent.isAnyTracingEnabled() && _tc.isEventEnabled())
      {
        SibTr.event(_tc, "rethrowing", e);
      }

      if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled())
      {
        SibTr.exit(_tc, "getSICoreConnectionFactory");
      }

      throw e;
    }
    // Catch and rethrow this exception type.
    catch (SIResourceException e)
    {
      com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.wsspi.sib.core.selector.SICoreConnectionFactorySelector.getSICoreConnectionFactory", "143");

      if (TraceComponent.isAnyTracingEnabled() && _tc.isEventEnabled())
      {
        SibTr.event(_tc, "rethrowing", e);
      }

      if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled())
      {
        SibTr.exit(_tc, "getSICoreConnectionFactory");
      }

      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled())
    {
      SibTr.exit(_tc, "getSICoreConnectionFactory", factory);
    }

    return factory;
  }

  /**
   * <p>This method returns the SICoreConnectionFactory object requested.</p>
   * <p>The permitted value for the FactoryType is FactoryType.TRM_CONNECTION. This
   *   value will result in an SICoreConnectionFactory being returned where the connections
   *   are created by TRM.
   * </p>
   * <p>Other FactoryType objects may require extra information which can be provided in the Map
   *   parameter. If the FactoryType requires information that is not supplied
   *   SIInsufficientDataForFactoryTypeException will be thrown.
   * </p>
   * <p>If an attempt is made to obtain an SICoreConnectionFactory for a specific Messaging Engine
   *   and that Messaging Engine cannot be contacted then an SIMENotFoundException will be thrown.
   * </p>
   *
   * @param type The FactoryType representing the type of SICoreConnectionFactory the caller wishes to use.
   * @param properties The optional properties that the FactoryType may require.
   * @return The SICoreConnectionFactory that was requested, this may be wrapped.
   * @throws SIInsufficientDataForFactoryTypeException if the FactoryType requires extra information.
   * @throws SIIncorrectCallException                  if a precondition fails
   * @throws SIResourceException                       if the SICoreConnectionFactory could not be created.
   */
  public static final SICoreConnectionFactory getSICoreConnectionFactory(
      FactoryType type, Map properties)
      throws SIInsufficientDataForFactoryTypeException, SIIncorrectCallException, SIResourceException
  {
    SICoreConnectionFactory factory = null;

    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled())
    {
      SibTr.entry(_tc, "getSICoreConnectionFactory", new Object[]{type,
        properties});
    }

    // If the supplied properties is null make it an empty map.
    if (properties == null) properties = Collections.EMPTY_MAP;

    // If the type is FactoryType.TRM_CONNECTION
    if (type == FactoryType.TRM_CONNECTION)
    {
      // just set factory to be the TrmSICoreConnectionFactory.
      factory = TrmSICoreConnectionFactory.getInstance();
    }
    // The only other option is PrivateFactoryType.LOCAL_CONNECTION
    else if (type == PrivateFactoryType.LOCAL_CONNECTION)
    {
      // So we get the bus and me name.
      String bus = (String)properties.get(PrivateFactoryType.BUS_NAME);
      String me = (String)properties.get(PrivateFactoryType.ME_NAME);

      // If the supplied bus or me name is null
      if (bus == null || me == null)
      {
        // there is insufficient information to return a local SICoreConnectionFactory
        // The message for the exception
        String errorMessage;
        // The message for debug trace
        String debugMessage;

        // Set the debug message if both are null
        if (bus == null && me == null)
        {
          errorMessage = _nls.getString("BUS_AND_ME_NAMES_ARE_NULL_CWSJC0001E");
          debugMessage = "Both the Bus and ME names are null";
        }
        // Set the debug message if the bus name is null
        else if (bus == null)
        {
          errorMessage = _nls.getFormattedMessage("BUS_NAME_IS_NULL_CWSJC0002E",
                                                  new Object[] {me}, null);
          debugMessage = "The bus name is null";
        }
        // Set the debug message if the me name is null
        else
        {
          errorMessage = _nls.getFormattedMessage("ME_NAME_IS_NULL_CWSJC0003E",
                                                  new Object[]{bus}, null);
          debugMessage = "The ME name is null";
        }

        // Trace the debug message
        if (TraceComponent.isAnyTracingEnabled() && _tc.isDebugEnabled())
        {
          SibTr.debug(_tc, debugMessage);
        }

        // Create the exception with the errorMessage in it.
        SIInsufficientDataForFactoryTypeException newE = new SIInsufficientDataForFactoryTypeException(
            errorMessage);

        // Trace that we are about to throw the exception
        if (TraceComponent.isAnyTracingEnabled() && _tc.isEventEnabled())
        {
          SibTr.event(_tc, "throwing", newE);
        }

        // Trace the method exit
        if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled())
        {
          SibTr.exit(_tc, "getSICoreConnectionFactory");
        }

        throw newE;
      }

      // Get the JsAdminService
      JsAdminService admin = CommonServiceFacade.getJsAdminService();

      // If it is null we are not in the server.
      if (admin == null)
      {
        if (TraceComponent.isAnyTracingEnabled() && _tc.isDebugEnabled())
        {
          SibTr.debug(_tc, "Unable to contact the admin service");
        }

        // No admin service means no local MEs.
        SIResourceException newE = new SIResourceException(
            _nls.getFormattedMessage("ADMIN_SERVICE_NULL_CWSJC0004E", new Object[] {bus, me}, null));

        if (TraceComponent.isAnyTracingEnabled() && _tc.isEventEnabled())
        {
          SibTr.event(_tc, "throwing", newE);
        }

        if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled())
        {
          SibTr.exit(_tc, "getSICoreConnectionFactory");
        }

        throw newE;
      }

      // Get hold of the Messaging Engine
      JsMessagingEngine engine = admin.getMessagingEngine(bus, me);

      // If the engine is null then there is no engine with that name on that bus.
      if (engine == null)
      {
        if (TraceComponent.isAnyTracingEnabled() && _tc.isDebugEnabled())
        {
          SibTr.debug(_tc, "Messaging Engine does not exists");
        }

        SIResourceException newE = new SIResourceException(
            _nls.getFormattedMessage("ME_NOT_FOUND_CWSJC0005E", new Object[] {bus, me}, null));

        if (TraceComponent.isAnyTracingEnabled() && _tc.isEventEnabled())
        {
          SibTr.event(_tc, "throwing", newE);
        }

        if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled())
        {
          SibTr.exit(_tc, "getSICoreConnectionFactory");
        }

        throw newE;
      }

      // Get hold of the message processor. This should never return null.
      factory = (SICoreConnectionFactory)engine.getMessageProcessor();
    }
   /* else if (type == FactoryType.RA_CONNECTION)
    {
      factory =SibRaFactory.getConnectionFactory(properties);
    }*/

    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled())
    {
      SibTr.exit(_tc, "getSICoreConnectionFactory", factory);
    }

    return factory;
  }
}
