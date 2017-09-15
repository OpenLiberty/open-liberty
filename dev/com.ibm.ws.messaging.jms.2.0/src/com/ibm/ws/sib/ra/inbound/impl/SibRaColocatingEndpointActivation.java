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

/**
 * This class is used to provide compatible behavious with WAS 6.1. If there are local MEs defined in the desired bus
 * then the MDB will ONLY connect to one of the local MEs (if the local ME is not started the MDB will wait for the
 * ME to start). This differs from SibRaScalableEndpointActivation as SibRaScalableEndpointActivation will connect
 * remotely if a local ME is defined but not running (this class will not connect remotely but will wait for the local
 * ME to start).
 * If there are no local MEs defined then it will behave in the same way as SibRaScalableEndpointActivation in that
 * it will connect remotely.
 */
package com.ibm.ws.sib.ra.inbound.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
//Sanjay Liberty Changes
import javax.resource.ResourceException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.ra.SibRaEngineComponent;
import com.ibm.ws.sib.ra.impl.SibRaUtils;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointInvoker;
import com.ibm.ws.sib.utils.ras.SibTr;

public class SibRaColocatingEndpointActivation extends SibRaCommonEndpointActivation
{

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaColocatingEndpointActivation.class);

    /**
     * The name of this class.
     */
    private static final String CLASS_NAME = SibRaColocatingEndpointActivation.class.getName();

    private static final String FFDC_PROBE_1 = "1";

    /**
     * Constructor. Just calls the base class constructor
     * @param resourceAdapter
     *            the resource adapter on which the activation was created
     * @param messageEndpointFactory
     *            the message endpoint factory for this activation
     * @param endpointConfiguration
     *            the endpoint configuration for this activation
     * @param endpointInvoker
     *            the endpoint invoker for this activation
     * @throws ResourceException
     *             if the activation fails
     */
    public SibRaColocatingEndpointActivation (SibRaResourceAdapterImpl resourceAdapter,
                                              MessageEndpointFactory messageEndpointFactory,
                                              SibRaEndpointConfiguration endpointConfiguration,
                                              SibRaEndpointInvoker endpointInvoker) throws ResourceException
    {
        super(resourceAdapter, messageEndpointFactory, endpointConfiguration,
                endpointInvoker);
        final String methodName = "SibRaColocatingEndpointActivation";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName);
            SibTr.exit(this, TRACE, methodName);
        }
    }

    /**
     * To maintain compability we do not restrict ourselves to just connecting to the DSH
     */
    public boolean onlyConnectToDSH ()
    {
      return false;
    }

    /**
     * All Messaging engines, including those that are not running, should be considered part of the list
     * that the MDB should look at before trying a remote connection.
     * @return The list of MEs that should be looked at by the RA to see if a connection can be made to them.
     */
    JsMessagingEngine[] getMEsToCheck()
    {
        final String methodName = "getMEsToCheck";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName);
        }

        JsMessagingEngine[] retVal = SibRaEngineComponent.getMessagingEngines (_endpointConfiguration.getBusName ());

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(this, TRACE, methodName, retVal);
        }
        return retVal;
    }

    /**
     * This method will remove non running MEs from the supplied array of MEs
     * @param MEList The list of MEs
     * @return A new array of MEs which only contain the running MEs from the supplied Array.
     */
    JsMessagingEngine[] removeStoppedMEs (JsMessagingEngine[] MEList)
    {
        final String methodName = "removeStoppedMEs";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName, MEList);
        }
        JsMessagingEngine[] startedMEs = SibRaEngineComponent.getActiveMessagingEngines(_endpointConfiguration.getBusName());
        List<JsMessagingEngine> runningMEs = Arrays.asList(startedMEs);
        List<JsMessagingEngine> newList = new ArrayList<JsMessagingEngine> ();
        for (int i = 0; i < MEList.length; i++)
        {
          JsMessagingEngine nextME = MEList [i];
          if (runningMEs.contains(nextME))
          {
            newList.add (nextME);
          }
        }

        JsMessagingEngine[] retVal = new JsMessagingEngine[newList.size ()];
        retVal = newList.toArray (retVal);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(this, TRACE, methodName, retVal);
        }
        return retVal;
    }

    /**
     * If a messaging engine is destroyed and there are now no local messaging engines
     * on the server, then kick off a check to see if we can connect to one.
     */
    public void messagingEngineDestroyed(JsMessagingEngine messagingEngine)
    {
        final String methodName = "messagingEngineDestroyed";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName, messagingEngine);
        }

        /*
         * If there are no longer any local messaging engines on the required
         * bus, switch to a remote messaging engine
         */

        final JsMessagingEngine[] localMessagingEngines = SibRaEngineComponent
                .getMessagingEngines(_endpointConfiguration.getBusName());

        if (0 == localMessagingEngines.length)
        {
            /*
             * The last local messaging engine for the required bus has been
             * destroyed; we may be able to now connect remotely so kick off
             * a check.
             */

            SibTr.info(TRACE, "ME_DESTROYED_CWSIV0779", new Object[] {
                    messagingEngine.getName(),
                    _endpointConfiguration.getBusName() });

          try
          {
                clearTimer ();
                timerLoop ();
            }
          catch (final ResourceException exception)
          {

                FFDCFilter.processException(exception, CLASS_NAME + "."
                        + methodName, FFDC_PROBE_1, this);
                SibTr.error(TRACE, "MESSAGING_ENGINE_STOPPING_CWSIV0765",
                        new Object[] { exception, messagingEngine.getName(),
                                messagingEngine.getBusName() });
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(this, TRACE, methodName);
        }
    }

}
