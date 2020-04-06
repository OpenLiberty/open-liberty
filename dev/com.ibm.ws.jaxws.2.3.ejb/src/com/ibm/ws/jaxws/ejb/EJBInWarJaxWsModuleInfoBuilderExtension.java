/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.ejb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.EJBEndpoint;
import com.ibm.ws.ejbcontainer.EJBEndpoints;
import com.ibm.ws.jaxws.JaxWsConstants;
import com.ibm.ws.jaxws.metadata.EndpointInfo;
import com.ibm.ws.jaxws.metadata.EndpointType;
import com.ibm.ws.jaxws.metadata.JaxWsModuleInfo;
import com.ibm.ws.jaxws.metadata.JaxWsModuleType;
import com.ibm.ws.jaxws.metadata.JaxWsServerMetaData;
import com.ibm.ws.jaxws.metadata.builder.AbstractJaxWsModuleInfoBuilderExtension;
import com.ibm.ws.jaxws.metadata.builder.EndpointInfoBuilder;
import com.ibm.ws.jaxws.metadata.builder.EndpointInfoBuilderContext;
import com.ibm.ws.jaxws.metadata.builder.JaxWsModuleInfoBuilderContext;
import com.ibm.ws.jaxws.support.JaxWsMetaDataManager;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * The builder extension builds the Web service endpoints based on the EJBs in a WAR module.
 */
public class EJBInWarJaxWsModuleInfoBuilderExtension extends AbstractJaxWsModuleInfoBuilderExtension {

    private static final TraceComponent tc = Tr.register(EJBInWarJaxWsModuleInfoBuilderExtension.class);

    protected final AtomicServiceReference<EndpointInfoBuilder> endpointInfoBuilderSRRef = new AtomicServiceReference<EndpointInfoBuilder>("endpointInfoBuilder");

    public EJBInWarJaxWsModuleInfoBuilderExtension() {
        super(JaxWsModuleType.WEB);
    }

    @Override
    public void preBuild(JaxWsModuleInfoBuilderContext jaxWsModuleInfoBuilderContext, JaxWsModuleInfo jaxWsModuleInfo) throws UnableToAdaptException {

        EndpointInfoBuilder endpointInfoBuilder = endpointInfoBuilderSRRef.getService();
        if (endpointInfoBuilder == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to locate EndpointInfoBuilder, EJB JaxWsModuleInfo builder will be skipped");
            }
            return;
        }

        EJBEndpoints ejbEndpoints = jaxWsModuleInfoBuilderContext.getContainer().adapt(EJBEndpoints.class);
        if (ejbEndpoints == null || ejbEndpoints.getEJBEndpoints().size() == 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No EJB Web Services is found");
            }
            return;
        }

        JaxWsServerMetaData jaxWsServerMetaData = JaxWsMetaDataManager.getJaxWsServerMetaData(jaxWsModuleInfoBuilderContext.getModuleMetaData());

        EndpointInfoBuilderContext endpointInfoBuilderContext = jaxWsModuleInfoBuilderContext.getEndpointInfoBuilderContext();

        // Need to scan the web.xml, because an EJB may be configured a servlet name and mapping
        @SuppressWarnings("unchecked")
        Map<String, String> servletNameClassPairs = (Map<String, String>) jaxWsModuleInfoBuilderContext.getContextEnv(JaxWsConstants.SERVLET_NAME_CLASS_PAIRS_FOR_EJBSINWAR);

        // if the ejb based webservice is configured as servlet, just ignore here
        Map<String, EJBEndpoint> ejbWSBeanInWebXML = new HashMap<String, EJBEndpoint>();
        List<EJBEndpoint> ejbEndpointList = new ArrayList<EJBEndpoint>();
        ejbEndpointList.addAll(ejbEndpoints.getEJBEndpoints()); //ejbEndpoints.getEJBEndpoints() is an UnmodifiableCollection
        if (servletNameClassPairs != null && !servletNameClassPairs.isEmpty()) {
            for (Iterator<EJBEndpoint> it = ejbEndpointList.iterator(); it.hasNext();) {
                EJBEndpoint ejbEndpoint = it.next();
                if (servletNameClassPairs.containsValue(ejbEndpoint.getClassName())) {
                    ejbWSBeanInWebXML.put(ejbEndpoint.getClassName(), ejbEndpoint);
                    it.remove();
                }
            }
        }

        // build endpoint infos for the pure ejb based webservices
        EJBJaxWsModuleInfoBuilderHelper.buildEjbWebServiceEndpointInfos(endpointInfoBuilder, endpointInfoBuilderContext, jaxWsServerMetaData, ejbEndpointList, jaxWsModuleInfo);

        // build endpoint infos for the ejb based webservices which are also configured as servlet
        Set<String> presentedServices = jaxWsModuleInfo.getEndpointImplBeanClassNames();
        if (servletNameClassPairs != null && !servletNameClassPairs.isEmpty()) {
            //note, maybe two different servlet names map to same ejb impl class,
            //so, we have to iterate servletNameClassPairs, but not ejbWSBeanInWebXML
            for (Entry<String, String> entry : servletNameClassPairs.entrySet()) {
                String servletName = entry.getKey();
                String servletClassName = entry.getValue();
                if (presentedServices.contains(servletClassName)) {
                    continue;
                }
                if (ejbWSBeanInWebXML.containsKey(servletClassName)) {
                    EJBEndpoint ejbEndpoint = ejbWSBeanInWebXML.get(servletClassName);
                    if (!ejbEndpoint.isWebService()) {
                        continue;
                    }

                    String ejbName = ejbEndpoint.getJ2EEName().getComponent();
                    endpointInfoBuilderContext.addContextEnv(JaxWsConstants.ENV_ATTRIBUTE_ENDPOINT_BEAN_NAME, ejbName);
                    endpointInfoBuilderContext.addContextEnv(JaxWsConstants.ENV_ATTRIBUTE_ENDPOINT_SERVLET_NAME, servletName);

                    EndpointInfo endpointInfo = endpointInfoBuilder.build(endpointInfoBuilderContext, servletClassName, EndpointType.EJB);
                    if (endpointInfo != null) {
                        jaxWsModuleInfo.addEndpointInfo(servletName, endpointInfo);
                        jaxWsServerMetaData.putEndpointNameAndJ2EENameEntry(servletName, ejbEndpoint.getJ2EEName());
                    }
                }
            }
        }
    }

    @Override
    public void postBuild(JaxWsModuleInfoBuilderContext jaxWsModuleInfoBuilderContext, JaxWsModuleInfo jaxWsModuleInfo) throws UnableToAdaptException {
    }

    protected void setEndpointInfoBuilder(ServiceReference<EndpointInfoBuilder> ref) {
        endpointInfoBuilderSRRef.setReference(ref);
    }

    protected void unsetEndpointInfoBuilder(ServiceReference<EndpointInfoBuilder> ref) {
        endpointInfoBuilderSRRef.unsetReference(ref);
    }

    protected void activate(ComponentContext cc) {
        endpointInfoBuilderSRRef.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        endpointInfoBuilderSRRef.deactivate(cc);
    }

}
