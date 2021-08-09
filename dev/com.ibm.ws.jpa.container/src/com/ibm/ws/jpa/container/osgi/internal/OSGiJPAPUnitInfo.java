/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.container.osgi.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jpa.JPAPuId;
import com.ibm.ws.jpa.container.osgi.internal.url.WSJPAUrlUtils;
import com.ibm.ws.jpa.management.JPAPUnitInfo;
import com.ibm.ws.jpa.management.JPAPXml;
import com.ibm.ws.jpa.management.JPAPuScope;
import com.ibm.ws.jpa.management.JPAScopeInfo;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.classloading.ClassTransformer;

public class OSGiJPAPUnitInfo extends JPAPUnitInfo implements ClassTransformer {
    private static final TraceComponent tc = Tr.register(OSGiJPAPUnitInfo.class);

    // Check if embedding wsjar:file URLs within wsjpa URLs has been disabled.
    private static boolean disableWsJpaUrlProcessing = AccessController.doPrivileged(
                    new PrivilegedAction<Boolean>() {
                        @Override
                        public Boolean run() {
                            return Boolean.getBoolean("com.ibm.websphere.persistence.DisableJpaFormatUrlProtocol");
                        }
                    });
    private static final String ECLIPSELINK_TARGET_SERVER = "WebSphere_Liberty";

    private static URL convertURLToJPAURL(URL url) {
        URL returnURL = url;

        if (url != null) {
            String urlStr = url.toString();
            if (urlStr != null && urlStr.startsWith("wsjar:file:")) {
                try {
                    if (disableWsJpaUrlProcessing) {
                        // Chop off the "ws" in the "wsjar" URL ptcol to produce a "jar" URL.
                        urlStr = urlStr.substring(2);
                        returnURL = new URL(urlStr);
                    } else {
                        // Otherwise, encapsulate in wsjpa URL
                        try {
                            returnURL = WSJPAUrlUtils.createWSJPAURL(url);
                        } catch (IllegalArgumentException iae) {
                            // Could not calculate a wsjpa:wsjar URL, fall back to using jar:
                            urlStr = urlStr.substring(2);
                            returnURL = new URL(urlStr);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "OSGiJPAPUnitInfo.convertURLToJPAURL() failed, falling back to URL", returnURL);
                            }
                        }
                    }
                } catch (MalformedURLException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Failed to convert URL.", e);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "OSGiJPAPUnitInfo.convertURLToJPAURL() converted URL (old,new)", url, returnURL);
        return returnURL;
    }

    private final JPAScopeInfo scopeInfo;

    OSGiJPAPUnitInfo(OSGiJPAApplInfo applInfo, JPAPuId puId, ClassLoader classLoader, JPAScopeInfo scopeInfo) {
        super(applInfo, puId, classLoader);
        this.scopeInfo = scopeInfo;
    }

    @Override
    protected void setPersistenceUnitRootUrl(URL newValue) {
        super.setPersistenceUnitRootUrl(convertURLToJPAURL(newValue));
    }

    @Override
    protected boolean addJarFileUrls(String jarPath, JPAPXml xml) {
        OSGiJPAPXml osgiPXml = (OSGiJPAPXml) xml;
        Container puBase = osgiPXml.getPuRootContainer();
        if (scopeInfo.getScopeType() == JPAPuScope.Web_Scope) {
            //***************************************************************
            // Within a war file the Persistence Unit may be in either the
            // "WEB-INF/classes" directory or the "WEB-INF/lib" directory.
            // The <jar-file> archive may be in either the "WEB-INF/lib"
            // directory or at the ear level.   Since the paths in the
            // <jar-file> stanza are relative to the PU we must handle
            // the to possible PU locations separately.
            //
            // JPA 2.0 Spec examples
            //
            // Example 4: 
            // app.ear
            //    war1.war
            //       WEB-INF/lib/warEntities.jar
            //       WEB-INF/lib/warPUnit.jar (with META-INF/persistence.xml )
            // persistence.xml contains:
            //    <jar-file>warEntities.jar</jar-file>
            //
            // Example 5:
            // app.ear
            //    war2.war
            //    WEB-INF/lib/warEntities.jar
            //    WEB-INF/classes/META-INF/persistence.xml
            // persistence.xml contains:
            //    <jar-file>lib/warEntities.jar</jar-file>
            //
            // Example 6:
            // app.ear
            //    lib/earEntities.jar
            //    war2.war
            //       WEB-INF/classes/META-INF/persistence.xml
            // persistence.xml contains:
            //    <jar-file>../../lib/earEntities.jar</jar-file>
            //
            // Example 7:
            // app.ear
            //    lib/earEntities.jar
            //    war1.war
            //       WEB-INF/lib/warPUnit.jar (with META-INF/persistence.xml )
            // persistence.xml contains:
            //   <jar-file>../../../lib/earEntities.jar</jar-file>
            //****************************************************************
            String puRootURLString = getPersistenceUnitRootUrl().toExternalForm();
            if (puRootURLString.endsWith("/WEB-INF/classes/")) {
                if (jarPath.startsWith("../../")) {
                    OSGiJPAApplInfo osgiApplInfo = (OSGiJPAApplInfo) ivApplInfo;
                    puBase = osgiApplInfo.getContainer();
                    jarPath = jarPath.substring(6);
                } else {
                    // base container is web archive (example 5)
                    puBase = getContainerLocation(puBase, "WEB-INF");
                }
            } else if (puRootURLString.contains("/WEB-INF/lib/")) {
                if (jarPath.startsWith("../../../")) {
                    // base container is ear archive (example 7)
                    // get lib jar container
                    OSGiJPAApplInfo osgiApplInfo = (OSGiJPAApplInfo) ivApplInfo;
                    puBase = osgiApplInfo.getContainer();
                    jarPath = jarPath.substring(9);
                } else {
                    // Navigate to the root of the container and use the parent  (WEB-INF/lib)
                    puBase = getContainerRootParent(puBase);
                }
            }
            // puBase is current base container (example 4) 
        } else {
            // JPA 2.0 Spec examples
            //
            // Example 1: - bogus example, PUs in jars in root are not supported in JPA 2.0
            //
            // Example 2:
            // app.ear
            //    lib/earEntities.jar
            //    lib/earLibPUnit.jar (with META-INF/persistence.xml )
            // persistence.xml contains:
            //    <jar-file>earEntities.jar</jar-file>
            //
            // Example 3:
            // app.ear
            //    lib/earEntities.jar
            //    ejbjar.jar (with META-INF/persistence.xml )
            // persistence.xml contains:
            //    <jar-file>lib/earEntities.jar</jar-file>

            // For other scopes, lib paths are relative to the PU root of the base module
            puBase = getContainerRootParent(puBase);
        }

        Entry path = puBase.getEntry(jarPath);
        if (path == null) {
            return false;
        }

        Container jarContainer = null;
        try {
            jarContainer = path.adapt(Container.class);
            addContainerUrls(jarContainer);
        } catch (Exception e) {
            e.getClass(); // findbugs
            return false;
        }

        return true;
    }

    private Container getContainerLocation(Container base, String location) {
        Container puBase = base;
        while (puBase != null && !location.equals(puBase.getName())) {
            puBase = puBase.getEnclosingContainer();
        }
        return puBase;
    }

    /**
     * Navigates to the root of the base container and returns the parent
     * container
     * 
     * @param base
     * @return
     */
    private Container getContainerRootParent(Container base) {
        Container puBase = base.getEnclosingContainer();
        while (puBase != null && !puBase.isRoot()) {
            puBase = puBase.getEnclosingContainer();
        }
        if (puBase != null && puBase.isRoot()) {
            Container parent = puBase.getEnclosingContainer();
            if (parent != null) {
                puBase = parent;
            }
        }
        return puBase;
    }

    private void addContainerUrls(Container container) throws MalformedURLException {
        if (container != null) {
            // Use the deprecated method which was added for exactly this purpose :)
            for (URL cUrl : container.getURLs()) {
                addJarFileUrl(convertURLToJPAURL(cUrl));
            }
        }
    }

    @Override
    protected boolean registerClassFileTransformer(ClassLoader classLoader) {
        boolean transformerRegistered = false;

        // Compute classloaders for provider
        BundleContext ctx = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        ServiceReference<ClassLoadingService> reference = ctx.getServiceReference(ClassLoadingService.class);
        if (reference != null) {
            ClassLoadingService service = ctx.getService(reference);

            if (service != null) {
                // Register "this" object plugin to the appl/module ClassLoader hierarchy.
                // When class is loaded by this ClassLoader, the transformClass method of this
                // plug-in will be invoked. This allows the persistence providers registered to this
                // PUnitInfo object a chance to class tranform the POJO entities.
                transformerRegistered = service.registerTransformer(this, classLoader);

                ctx.ungetService(reference);
            }
        }

        return transformerRegistered;
    }

    @Override
    protected void unregisterClassFileTransformer(ClassLoader classLoader) {
        BundleContext ctx = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        ServiceReference<ClassLoadingService> reference = ctx.getServiceReference(ClassLoadingService.class);
        if (reference != null) {
            ClassLoadingService service = ctx.getService(reference);

            if (service != null) {
                // Unregister "this" object plugin from the appl/module ClassLoader hierarchy.
                // When class is loaded by this ClassLoader, the transformClass method of this
                // plug-in will be invoked. This allows the persistence providers registered to this
                // PUnitInfo object a chance to class tranform the POJO entities.
                boolean transformerUnregistered = service.unregisterTransformer(this, classLoader);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && transformerUnregistered)
                    Tr.debug(tc, "transformer unregistered from " + ivClassLoader);

                ctx.ungetService(reference);
            }
        }
    }

    @Override
    protected ClassLoader createTempClassLoader(ClassLoader classLoader) {
        BundleContext ctx = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        ServiceReference<ClassLoadingService> reference = ctx.getServiceReference(ClassLoadingService.class);
        if (reference != null) {
            ClassLoadingService service = ctx.getService(reference);

            if (service != null) {
                // Create a temporary classloader with the same class loading characteristics
                // as the input CompoundClassLoader for use by the persistence provider.
                ClassLoader loader = service.getShadowClassLoader(classLoader);
                ctx.ungetService(reference);
                return loader;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getNewTempClassLoader : Could not get a service reference for the classloading service.");
        }
        return null;
    }

    @Override
    protected DataSource lookupDataSource(String dsName) throws NamingException {
        return (DataSource) new InitialContext().lookup(dsName);
    }

    protected String getEclipseLinkTargetServer() {
        return ECLIPSELINK_TARGET_SERVER;
    }

}
