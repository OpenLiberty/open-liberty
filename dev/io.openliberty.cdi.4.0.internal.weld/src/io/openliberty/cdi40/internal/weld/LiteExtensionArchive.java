/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.cdi40.internal.weld;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.jboss.weld.lite.extension.translator.LiteExtensionTranslator;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.internal.archive.AbstractCDIArchive;
import com.ibm.ws.cdi.internal.interfaces.Application;
import com.ibm.ws.cdi.internal.interfaces.ArchiveType;
import com.ibm.ws.cdi.internal.interfaces.CDIArchive;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.ExtensionArchive;
import com.ibm.ws.cdi.internal.interfaces.Resource;
import com.ibm.ws.cdi.internal.interfaces.ResourceInjectionBag;
import com.ibm.ws.runtime.metadata.MetaData;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.spi.Extension;

/**
 * Extension archive which adds the Weld LiteExtensionTranslator
 */
public class LiteExtensionArchive extends AbstractCDIArchive implements ExtensionArchive {
    private static final TraceComponent tc = Tr.register(LiteExtensionArchive.class);
    private final ClassLoader bceClassLoader;
    private final List<Class<? extends BuildCompatibleExtension>> buildCompatibleExtensions;

    public LiteExtensionArchive(CDIRuntime cdiRuntime, ClassLoader bceClassLoader, List<Class<? extends BuildCompatibleExtension>> buildCompatibleExtensions) {
        super(LiteExtensionTranslator.class.getName(), cdiRuntime);
        this.bceClassLoader = bceClassLoader;
        this.buildCompatibleExtensions = buildCompatibleExtensions;
    }

    /** {@inheritDoc} */
    @Override
    public J2EEName getJ2EEName() throws CDIException {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public ArchiveType getType() {
        return ArchiveType.RUNTIME_EXTENSION;
    }

    /** {@inheritDoc} */
    @Override
    public ClassLoader getClassLoader() {
        return LiteExtensionTranslator.class.getClassLoader();
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getClassNames() throws CDIException {
        return Collections.singleton(LiteExtensionTranslator.class.getName());
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getExtensionClasses() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Returning extension translator");
        }

        return Collections.singleton(LiteExtensionTranslator.class.getName());
    }

    /** {@inheritDoc} */
    @Override
    public Set<Supplier<Extension>> getSPIExtensionSuppliers() {
        return Collections.singleton(() -> new LiteExtensionTranslator(buildCompatibleExtensions, bceClassLoader));
    }

    /** {@inheritDoc} */
    @Override
    public Resource getResource(String path) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isModule() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Application getApplication() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getClientModuleMainClass() throws CDIException {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getInjectionClassList() throws CDIException {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public MetaData getMetaData() throws CDIException {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public ResourceInjectionBag getAllBindings() throws CDIException {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getClientAppCallbackHandlerName() throws CDIException {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getPath() throws CDIException {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<CDIArchive> getModuleLibraryArchives() throws CDIException {
        return Collections.emptySet();
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getAnnotatedClasses(Set<String> annotations) throws CDIException {
        return Collections.emptySet();
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getExtraClasses() {
        return Collections.emptySet();
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getExtraBeanDefiningAnnotations() {
        return Collections.emptySet();
    }

    /** {@inheritDoc} */
    @Override
    public boolean applicationBDAsVisible() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isExtClassesOnly() {
        return true;
    }

}
