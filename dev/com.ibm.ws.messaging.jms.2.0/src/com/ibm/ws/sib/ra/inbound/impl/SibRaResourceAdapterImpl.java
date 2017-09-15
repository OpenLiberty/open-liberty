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

package com.ibm.ws.sib.ra.inbound.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.ra.impl.SibRaUtils;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointConfigurationProvider;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointInvoker;
import com.ibm.ws.sib.utils.RuntimeInfo;
import com.ibm.ws.sib.utils.ras.SibMessage;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SICoreConnectionFactory;
import com.ibm.wsspi.sib.core.SICoreConnectionFactorySelector;
import com.ibm.wsspi.sib.core.selector.FactoryType;
import com.ibm.wsspi.sib.core.trm.SibTrmConstants;

/**
 * Resource adapter implementation class for the WebSphere Platform Messaging
 * component. This class is common to both the core SPI and JMS resource
 * adapters. The class is not final as, currently, the admin commands rely on
 * the JMS resource adapter having its own implementation class.
 */
public class SibRaResourceAdapterImpl implements ResourceAdapter, SibMessage.Listener {

    private static final String CLASS_NAME = SibRaResourceAdapterImpl.class.getName();
    private static final TraceComponent TRACE = SibRaUtils .getTraceComponent(SibRaResourceAdapterImpl.class);
    private static TraceNLS NLS = SibRaUtils.getTraceNls();

    //@start_class_string_prolog@
    public static final String $sccsid = "@(#) 1.30 SIB/ws/code/sib.ra.impl/src/com/ibm/ws/sib/ra/inbound/impl/SibRaResourceAdapterImpl.java, SIB.ra, WASX.SIB, aa1225.01 08/05/14 03:57:12 [7/2/12 06:13:52]";
    //@end_class_string_prolog@

    static {
      if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) SibTr.debug(TRACE, "Source Info: " + $sccsid);
    }

    /**
     * A map from message endpoint factories to endpoint activations. Entries
     * are added by <code>endpointActivation</code> and removed by
     * <code>endpointDeactivation</code> or <code>stop</code>.
     */
    private final Map<MessageEndpointFactory,SibRaEndpointActivation> _endpointActivations = new HashMap<MessageEndpointFactory,SibRaEndpointActivation>();

    /**
     * The bootstrap context passed on <code>start</code> and made available
     * to endpoint activations by the <code>getBootstrapContext</code> method.
     */
    private BootstrapContext _bootstrapContext;

    /**
     * The maximum number of endpoints
     */
    private static final int MAX_ENDPOINTS = 10;
    
    private static final String FFDC_PROBE_1 = "1";
    private static final String FFDC_PROBE_2 = "2";
    private static final String FFDC_PROBE_3 = "3";

    /**
     * Constructor
     */
    public SibRaResourceAdapterImpl () {
      // In the "standalone" thin client environment we need to register a listener with SibMessage so that our listener gets
      // informed of all Sib Messages and can echo them to stdout/stderr. Without this listener these messages would not be
      // seen by the customer or at least they would have to run trace to find out what is going wrong.

      if (RuntimeInfo.isThinClient()) {
        SibMessage.addListener(this);
      }
    }

    /**
     * Method required by the SibMessage.Listener interface - as this is a trace/messaging listener don't add trace statements
     * to this method - this listener is used to 'echo' SIB messages to stdout/stderr
     */
    public void message (SibMessage.Listener.MessageType type, String me, TraceComponent tc, String msgKey, Object objs, Object[] formattedMessage) {
      switch(type) {
        case AUDIT:
        case INFO:
        case SERVICE:
        case WARNING: System.out.println(formattedMessage[1]);
                      break;
        case ERROR:
        case FATAL:   System.err.println(formattedMessage[1]);
                      break;
      }
    }

    /**
     * Called by the application server on server startup or when the resource
     * adapter is explicitly started. Caches the passed bootstrap context.
     *
     * @param bootstrapContext
     *            a bootstrap context used to obtain services such as the work
     *            manager
     */
    public final void start(final BootstrapContext bootstrapContext) {

        final String methodName = "start";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, bootstrapContext);
        }

        _bootstrapContext = bootstrapContext;

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Called by the application on server shutdown or when the resource adapter
     * is explicitly stopped. Deactivates any currently active endpoints.
     */
    public final void stop() {

        final String methodName = "stop";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        /*
         * Deactivate any currently active endpoints
         */

        for (final Iterator iterator = _endpointActivations.keySet().iterator(); iterator
                .hasNext();) {

            final SibRaEndpointActivation endpointActivation = _endpointActivations.get((MessageEndpointFactory) iterator.next());
            endpointActivation.deactivate();

        }

        _endpointActivations.clear();
        _bootstrapContext = null;

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Activates an endpoint. Obtains the endpoint configuration and invoker
     * from the activation specification and creates an endpoint activation of
     * the appropriate type dependent on whether a destination name has been
     * specified in the endpoint configuration.
     *
     * @param messageEndpointFactory
     *            the factory from which message endpoints can be created
     * @param activationSpec
     *            the activation specification
     * @throws NotSupportedException
     *             if the configuration is invalid or the activation
     *             specification is of an unrecognised type
     * @throws ResourceException
     *             if the activation fails
     */
    public final void endpointActivation(
            final MessageEndpointFactory messageEndpointFactory,
            final ActivationSpec activationSpec) throws NotSupportedException,
            ResourceException {

        final String methodName = "endpointActivation";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] {
                    messageEndpointFactory, activationSpec });
        }

        if (activationSpec instanceof SibRaEndpointConfigurationProvider) {

            final SibRaEndpointConfigurationProvider configurationProvider = (SibRaEndpointConfigurationProvider) activationSpec;

            /*
             * Obtain the configuration for the endpoint
             */

            final SibRaEndpointConfiguration configuration;
            try {

                // The following call should always succeed if the application
                // server has already called validate on the activation spec
                configuration = configurationProvider
                        .getEndpointConfiguration();

            } catch (final InvalidPropertyException exception) {

                FFDCFilter.processException(exception, CLASS_NAME + "."
                        + methodName, FFDC_PROBE_1, this);
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw new NotSupportedException(NLS.getFormattedMessage(
                        "INVALID_ACTIVATION_SPEC_CWSIV0452", new Object[] {
                                activationSpec, exception }, null), exception);

            }
            

            /*
             * Obtain the invoker for the endpoint
             */

            final SibRaEndpointInvoker invoker = configurationProvider
                    .getEndpointInvoker();

            SibRaEndpointActivation endpointActivation = null;
           

            if (configuration.getDestination() == null) {

                endpointActivation = new SibRaDynamicDestinationEndpointActivation(
                        this, messageEndpointFactory, configuration,
                        invoker);

            } else {

                    if (configuration.getAlwaysActivateAllMDBs().booleanValue())
                    {
                        endpointActivation = new SibRaScalableEndpointActivation(
                                this, messageEndpointFactory, configuration,
                                invoker);
                    }
                    else
                    {
                        endpointActivation = new SibRaColocatingEndpointActivation(
                                this, messageEndpointFactory, configuration,
                                invoker);
                    }

            }

            
            _endpointActivations
                    .put(messageEndpointFactory, endpointActivation);

        } else {

            /*
             * Unrecognised activation specification
             */

            final NotSupportedException exception = new NotSupportedException(
                    NLS.getFormattedMessage(
                            "UNEXPECTED_ACTIVATION_SPEC_CWSIV0451",
                            new Object[] { activationSpec,
                                    SibRaEndpointConfigurationProvider.class },
                            null));
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw exception;

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Deactivates an endpoint.
     *
     * @param messageEndpointFactory
     *            the same factory passed on the activation of the endpoint
     * @param activationSpec
     *            the same activation specification passed on the activation of
     *            the endpoint
     */
    public final void endpointDeactivation(
            final MessageEndpointFactory messageEndpointFactory,
            final ActivationSpec activationSpec) {

        final String methodName = "endpointDeactivation";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] {
                    messageEndpointFactory, activationSpec });
        }

        final SibRaEndpointActivation endpointActivation = (SibRaEndpointActivation) _endpointActivations
                .remove(messageEndpointFactory);

        if (endpointActivation != null) {
            endpointActivation.deactivate();
        } else {
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                SibTr.debug(this, TRACE,
                        "Endpoint deactivation called for unknown endpoint "
                                + messageEndpointFactory);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Called by application server during transaction recovery. 
     *
     * @param activationSpecs
     *            an array of activation specs for which <code>XAResource</code>
     *            instances are required
     * @return an array of associated <code>XAResource</code> objects
     */
    public XAResource[] getXAResources(final ActivationSpec[] activationSpecs) {
    	final String methodName = "getXAResources";
    	if (TRACE.isAnyTracingEnabled() && TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) SibTr.entry(TRACE, methodName,"activationSpecs="+Arrays.toString(activationSpecs));

    	XAResource[] rc = null;

    	rc = new XAResource[activationSpecs.length];

    	for (int i=0; i <activationSpecs.length; i++) {
    		rc[i] = null;

    		SibRaEndpointConfiguration activationSpec = null;

    		if (activationSpecs[i] instanceof SibRaEndpointConfigurationProvider) {
    			try {
    				activationSpec = ((SibRaEndpointConfigurationProvider)activationSpecs[i]).getEndpointConfiguration();
    			} catch (InvalidPropertyException e) {
    				FFDCFilter.processException(e, CLASS_NAME + "."+ methodName, FFDC_PROBE_2, this);
    				if (TRACE.isAnyTracingEnabled() && TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) SibTr.exception(this, TRACE, e);
    			} catch (ResourceAdapterInternalException f) {
    				FFDCFilter.processException(f, CLASS_NAME + "."+ methodName, FFDC_PROBE_3, this);
    				if (TRACE.isAnyTracingEnabled() && TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) SibTr.exception(this, TRACE, f);
    			}
    		} else {              // Can't throw an exception so just log a trace comment
    			if (TRACE.isAnyTracingEnabled() && TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) SibTr.debug(TRACE, "Unsupported ActivationSpec object encountered: "+ activationSpecs[i]);
    		}

    		if (activationSpec == null) continue; // Skip this entry if we didn't extract a valid activationSpec

    		if (TRACE.isAnyTracingEnabled() && TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) SibTr.debug(TRACE, "Using activationSpec: "+activationSpec);

    		Map<String,String> map = new HashMap<String,String>();

    		// Necessary parametes to connect to ME
    		// TRM will then decide if it has to connect to local or Remote ME
    		map.put(SibTrmConstants.PROVIDER_ENDPOINTS, activationSpec.getProviderEndpoints());
    		map.put(SibTrmConstants.TARGET_TRANSPORT_TYPE,activationSpec.getTargetTransport());
    		map.put(SibTrmConstants.BUSNAME,activationSpec.getBusName());

    		if (TRACE.isAnyTracingEnabled() && TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) SibTr.debug(TRACE, "Recovering using connection properties: " + map);

    		try {
    			final SICoreConnectionFactory factory = SICoreConnectionFactorySelector.getSICoreConnectionFactory(FactoryType.TRM_CONNECTION);
    			final SICoreConnection connection = factory.createConnection(activationSpec.getUserName(), activationSpec.getPassword(), map);
    			if (connection != null) rc[i] = connection.getSIXAResource();
    		} catch (SIException e) {
    			// No FFDC code needed
    			// There is nothing we can do if the connection fails, error msgs should already have been issued by TRM, authentication failure etc
    			if (TRACE.isAnyTracingEnabled() && TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) SibTr.debug(TRACE, "Exception returned from create connection:"+e);
    		}

    		if (TRACE.isAnyTracingEnabled() && TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) SibTr.debug(TRACE, "XAResource="+rc[i]);
    	}


    	if (TRACE.isAnyTracingEnabled() && TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) SibTr.exit(TRACE, methodName, "rc="+rc);
    	return rc;
    }

    /**
     * Returns the bootstrap context passed on <code>start</code>.
     *
     * @return the bootstrap context
     */
    final BootstrapContext getBootstrapContext() {

        return _bootstrapContext;

    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    public final String toString() {

        final SibRaStringGenerator generator = new SibRaStringGenerator(this);
        generator.addField("endpointActivations", _endpointActivations);
        generator.addField("bootstrapContext", _bootstrapContext);
        return generator.getStringRepresentation();

    }
}
