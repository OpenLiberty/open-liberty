/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.internal;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;

import javax.resource.spi.BootstrapContext;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.extended.AltDDEntryGetter;
import com.ibm.ws.container.service.metadata.extended.DeferredMetaDataFactory;
import com.ibm.ws.javaee.dd.connector.Connector;
import com.ibm.ws.jca.utils.metagen.DeploymentDescriptorParser;
import com.ibm.ws.jca.utils.xml.ra.RaConnector;
import com.ibm.ws.jca.utils.xml.ra.RaDisplayName;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaConnector;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 * */
//as documentation only at this point:
//@Component(ignore)
public final class ConnectorAdapter implements ContainerAdapter<Connector>, DeferredMetaDataFactory {

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter#adapt(com.ibm.wsspi.adaptable.module.Container, com.ibm.wsspi.artifact.overlay.OverlayContainer,
     * com.ibm.wsspi.artifact.ArtifactContainer, com.ibm.wsspi.adaptable.module.Container)
     */
    private static final TraceComponent tc = Tr.register(ConnectorAdapter.class, "WAS.j2c",
                                                         "com.ibm.ws.jca.internal.resources.J2CAMessages");

    @Override
    public Connector adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException {
        NonPersistentCache cache = containerToAdapt.adapt(NonPersistentCache.class);
        Connector connector = (Connector) cache.getFromCache(Connector.class);
        if (connector != null) {
            return connector;
        }
        AltDDEntryGetter altDDGetter = (AltDDEntryGetter) cache.getFromCache(AltDDEntryGetter.class);
        Entry ddEntry = altDDGetter != null ? altDDGetter.getAltDDEntry(ContainerInfo.Type.RAR_MODULE) : null;
        if (ddEntry == null) {
            ddEntry = containerToAdapt.getEntry("META-INF/ra.xml");
        }
        Entry raEntry = ddEntry;
        Entry wlpEntry = containerToAdapt.getEntry("META-INF/wlp-ra.xml");
        Entry beanEntry = containerToAdapt.getEntry("META-INF/beans.xml");
        RaConnector ra = null;
        WlpRaConnector wlpra = null;

        // TODOCJN this needs to be tested for all variations, not sure all of these combinations are being processed correctly
        // If there is just an ra.xml, then ok
        // If there is an ra.xml and a wlp-ra.xml, no annotations, then ok
        // If there is an ra.xml and a wlp-ra.xml, annotations, then ok, but wlp-ra.xml will override annotations later
        // If there is no ra.xml and a wlp-ra.xml, no annotations, then error?  Can't detect that here
        // If there is no ra.xml and a wlp-ra.xml, annotations, then process just the wlp-ra.xml after the annotations
        // If there is no ra.xml and no wlp-ra.xml, must be annotations, then no processing here, detect no annotations later

        try {
            if (beanEntry != null) {
                Tr.warning(tc, "BEAN_ARCHIVE_RESOURCE_ADAPTERS_NOT_SUPPORT_J2CA0241");
            }
            if (raEntry != null) {
                DeploymentDescriptorParser.init();
                ra = (RaConnector) DeploymentDescriptorParser.parseRaDeploymentDescriptor(raEntry);
                if (wlpEntry != null) {
                    wlpra = (WlpRaConnector) DeploymentDescriptorParser.parseRaDeploymentDescriptor(wlpEntry);
                    String displayName = "";
                    for (RaDisplayName raDisplayName : ra.getDisplayName()) {
                        if ("en".equalsIgnoreCase(raDisplayName.getLang())) {
                            displayName = raDisplayName.getValue();
                            break;
                        }
                        displayName = raDisplayName.getValue();
                    }
                    DeploymentDescriptorParser.combineWlpAndRaXmls(displayName, ra, wlpra);
                }
                rootOverlay.addToNonPersistentCache(artifactContainer.getPath(), Connector.class, ra);
            }
        } catch (Exception jex) {
            throw new UnableToAdaptException(jex);
        }
        return ra;
    }

    /**
     * @see com.ibm.ws.container.service.metadata.extended.DeferredMetaDataFactory#createComponentMetaData(java.lang.String)
     */
    @Override
    public ComponentMetaData createComponentMetaData(final String identifier) {
        // examples
        // standalone CONNECTOR#expandedra#ResourceAdapterModule#ResourceAdapter (id=expandedra)
        // embedded CONNECTOR#dropinapp#embExpandRA#ResourceAdapter (id=dropinapp.embExpandRA)

        final StringBuilder filter = new StringBuilder().append("(id=");
        String[] parts = identifier.split("#");
        if (ConnectorModuleMetaDataImpl.RA_MODULE_CONSTANT.equals(parts[2])) // standalone
            filter.append(parts[1]);
        else
            // embedded
            filter.append(parts[1]).append('.').append(parts[2]);
        filter.append(')');

        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<ComponentMetaData>() {
                @Override
                public ComponentMetaData run() throws IllegalArgumentException {
                    BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
                    Collection<ServiceReference<BootstrapContext>> refs;
                    try {
                        refs = bundleContext.getServiceReferences(BootstrapContext.class, filter.toString());
                    } catch (InvalidSyntaxException x) {
                        throw new IllegalArgumentException(identifier, x);
                    }

                    if (refs.isEmpty())
                        return null;

                    ServiceReference<BootstrapContext> ref = refs.iterator().next();
                    BootstrapContext bootstrapContext = bundleContext.getService(ref);

                    try {
                        return ((BootstrapContextImpl) bootstrapContext).getResourceAdapterMetaData();
                    } finally {
                        bundleContext.ungetService(ref);
                    }
                }
            });
        } catch (PrivilegedActionException e) {
            throw (IllegalArgumentException) e.getCause();
        }
    }

    /**
     * @see com.ibm.ws.container.service.metadata.extended.DeferredMetaDataFactory#initialize(com.ibm.ws.runtime.metadata.ComponentMetaData)
     */
    @Override
    public void initialize(ComponentMetaData metadata) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @return null (NO-OP)
     */
    @Override
    public String getMetaDataIdentifier(String appName, String moduleName, String componentName) {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @return null (NO-OP)
     */
    @Override
    public ClassLoader getClassLoader(ComponentMetaData metadata) {
        return null;
    }
}
