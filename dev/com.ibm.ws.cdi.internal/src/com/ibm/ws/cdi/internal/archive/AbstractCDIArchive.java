/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.internal.archive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.weld.bootstrap.spi.BeanDiscoveryMode;
import org.jboss.weld.bootstrap.spi.BeansXml;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.internal.interfaces.Application;
import com.ibm.ws.cdi.internal.interfaces.ArchiveType;
import com.ibm.ws.cdi.internal.interfaces.CDIArchive;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.CDIUtils;
import com.ibm.ws.cdi.internal.interfaces.Resource;
import com.ibm.ws.cdi.internal.interfaces.ResourceInjectionBag;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration.ReferenceFlowKind;
import com.ibm.wsspi.injectionengine.InjectionEngine;
import com.ibm.wsspi.injectionengine.JNDIEnvironmentRefBindingHelper;
import com.ibm.wsspi.injectionengine.ReferenceContext;

/**
 * Some common Archive implementation methods
 *
 */
public abstract class AbstractCDIArchive implements CDIArchive {
    private static final TraceComponent tc = Tr.register(AbstractCDIArchive.class);
    private final CDIRuntime cdiRuntime;
    private final String name;
    private Set<String> annotationNames;

    private ReferenceContext referenceContext;

    /**
     * @param name The name of the archive, if the archive represents a module then it should be the same as the module name
     * @param cdiRuntime
     */
    public AbstractCDIArchive(String name, CDIRuntime cdiRuntime) {
        this.cdiRuntime = cdiRuntime;
        this.name = name;
    }

    /**
     * Load classes from a bean deployment archive (BDA)
     * <p>
     * Classes in the internalClassNames set will only be loaded if they are loaded directly by the BDA's classloader.
     * <p>
     * Classes in the additionalClassNames will be loaded unconditionally, as long as the BDA can access them.
     * <p>
     * If a class name cannot be found, it is ignored.
     *
     * @param archive the BDA
     * @param internalClassNames classes to load only if they're loaded directly by the BDA's classloader
     * @param additionalClassNames additional classes to load, which might not actually be in the BDA but will be loaded anyway
     * @return the map of loaded Class objects
     * @throws CDIException
     */
    @FFDCIgnore({ ClassNotFoundException.class, LinkageError.class })
    public Map<String, Class<?>> loadBdaClasses(Set<String> internalClassNames, Set<String> additionalClassNames) throws CDIException {
        Map<String, Class<?>> classes = new HashMap<String, Class<?>>();

        ClassLoader classLoader = getClassLoader();

        for (String className : additionalClassNames) {
            try {
                Class<?> clazz = classLoader.loadClass(className);
                classes.put(className, clazz);
            } catch (ClassNotFoundException e) {
                // skip classes that can't be loaded
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Skipping additional class which can't be loaded", className, classLoader);
                }
            } catch (LinkageError e) {
                // skip classes that have linkage errors
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Skipping additional class which can't be loaded", className, classLoader);
                }
            }
        }

        for (String className : internalClassNames) {
            try {
                Class<?> clazz = classLoader.loadClass(className);
                // only add the class if it was loaded by the archive class loader
                if (clazz.getClassLoader() == classLoader) {
                    classes.put(className, clazz);
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Skipping class which is external to the BDA", clazz, clazz.getClassLoader(), classLoader);
                    }
                }
            } catch (ClassNotFoundException e) {
                //skip over classes that can't be loaded
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Skipping class which can't be loaded", className, classLoader);
                }
            } catch (LinkageError e) {
                // skip classes that have linkage errors
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Skipping class which can't be loaded", className, classLoader);
                }
            }
        }
        return classes;
    }

    /**
     * Get hold of the extension class names from the file of META-INF\services\javax.enterprise.inject.spi.Extension
     *
     * @return a set of classes mentioned in the extension file
     *
     */
    @Override
    public Set<String> getExtensionClasses() {

        Resource metaInfServicesEntry = getResource(CDIUtils.META_INF_SERVICES_CDI_EXTENSION);
        Set<String> serviceClazz = CDIUtils.parseServiceSPIExtensionFile(metaInfServicesEntry);
        return serviceClazz;
    }

    @Override
    public Resource getBeansXml() {
        Resource beansXmlEntry = null;

        if (getType() == ArchiveType.WEB_MODULE) {
            Resource webInfClassesMetaInfBeansXmlEntry = getResource(CDIUtils.WEB_INF_CLASSES_META_INF_BEANS_XML);
            Resource webInfBeansXmlEntry = getResource(CDIUtils.WEB_INF_BEANS_XML);

            if (webInfBeansXmlEntry != null) {
                beansXmlEntry = webInfBeansXmlEntry;
                if (webInfClassesMetaInfBeansXmlEntry != null) {
                    if (tc.isWarningEnabled()) {
                        Tr.warning(tc, "multiple.beans.xml.warning.CWOWB1001W", getApplication().getName() + "#" + getName(), CDIUtils.WEB_INF_BEANS_XML,
                                   CDIUtils.WEB_INF_CLASSES_META_INF_BEANS_XML);
                    }
                }
            } else {
                beansXmlEntry = webInfClassesMetaInfBeansXmlEntry;
            }
        } else {
            beansXmlEntry = getResource(CDIUtils.META_INF_BEANS_XML);
        }

        return beansXmlEntry;
    }

    /**
     * Determine the bean deployment archive scanning mode
     * If there is a beans.xml, the bean discovery mode will be used.
     * If there is no beans.xml, the mode will be annotated, unless the enableImplicitBeanArchives is configured as false via the server.xml.
     * If there is no beans.xml and the enableImplicitBeanArchives attribute on cdi12 is configured to false, the scanning mode is none.
     *
     * @return
     */
    public BeanDiscoveryMode getBeanDiscoveryMode(CDIRuntime cdiRuntime, BeansXml beansXml) {
        BeanDiscoveryMode mode = BeanDiscoveryMode.ANNOTATED;
        if (beansXml != null) {
            mode = beansXml.getBeanDiscoveryMode();
        } else if (cdiRuntime.isImplicitBeanArchivesScanningDisabled(this)) {
            // If the server.xml has the configuration of enableImplicitBeanArchives sets to false, we will not scan the implicit bean archives
            mode = BeanDiscoveryMode.NONE;
        }
        return mode;
    }

    @Override
    public ReferenceContext getReferenceContext(Set<Class<?>> injectionClasses) throws CDIException {
        //TODO WASt throws an exception if the injection engine is not available, check that Liberty does too
        if (referenceContext == null) {
            InjectionEngine injectionEngine = cdiRuntime.getInjectionEngine();

            Application cdiApplication = getApplication();
            ApplicationMetaData applicationMetaData = cdiApplication.getApplicationMetaData();

            MetaData metaData = getMetaData();
            ModuleMetaData moduleMetaData = null;
            if (metaData != null && metaData instanceof ModuleMetaData) {
                moduleMetaData = (ModuleMetaData) metaData;
            }

            if (moduleMetaData != null && getType() != ArchiveType.CLIENT_MODULE) {
                referenceContext = injectionEngine.getCommonReferenceContext(moduleMetaData);
            } else {
                referenceContext = injectionEngine.createReferenceContext(applicationMetaData);
            }

            J2EEName j2eeName = getJ2EEName();
            if (j2eeName == null) {
                j2eeName = cdiApplication.getJ2EEName();
            }

            // don't seem to have access to the display name here, the called method will determine a name if we pass
            // null, so pass null for now.
            String displayName = null;
            ComponentNameSpaceConfiguration compNSConfig = new ComponentNameSpaceConfiguration(displayName, j2eeName);

            ResourceInjectionBag bag = getAllBindings();

            if (bag != null) {
                try {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Looking for env entries");
                    JNDIEnvironmentRefBindingHelper.setAllBndAndExt(compNSConfig, bag.allBindings, bag.envEntryValues, bag.resourceRefConfigList);
                } catch (Exception e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Caught Exception looking for Env Entries: " + e);
                }
            }
            compNSConfig.setClassLoader(getClassLoader());

            if (moduleMetaData != null) {
                compNSConfig.setModuleMetaData(moduleMetaData);
            }
            compNSConfig.setApplicationMetaData(applicationMetaData);

            // always false for CDI when initializing
            compNSConfig.setMetaDataComplete(false);
            compNSConfig.setOwningFlow(ReferenceFlowKind.MANAGED_BEAN);

            compNSConfig.setInjectionClasses(new ArrayList<Class<?>>(injectionClasses));

            referenceContext.add(compNSConfig);
        }
        return referenceContext;
    }

    /** {@inheritDoc} */
    @Override
    public ReferenceContext getReferenceContext() throws CDIException {
        return referenceContext;
    }

    /**
     * Get the CDIRuntime associated with this Archive
     */
    @Override
    public CDIRuntime getCDIRuntime() {
        return cdiRuntime;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "CDIArchive: " + name;
    }

    @Override
    public Set<String> getBeanDefiningAnnotations() throws CDIException {
        if (this.annotationNames == null) {
            this.annotationNames = getAnnotatedClasses(CDIUtils.BEAN_DEFINING_META_ANNOTATION_NAMES);
        }

        return annotationNames;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractCDIArchive other = (AbstractCDIArchive) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

}
