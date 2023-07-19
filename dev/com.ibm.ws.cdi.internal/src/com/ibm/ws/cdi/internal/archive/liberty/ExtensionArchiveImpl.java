/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.internal.archive.liberty;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import javax.enterprise.inject.spi.Extension;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.CDIRuntimeException;
import com.ibm.ws.cdi.internal.interfaces.ArchiveType;
import com.ibm.ws.cdi.internal.interfaces.ExtensionArchive;

public class ExtensionArchiveImpl extends CDIArchiveImpl implements ExtensionArchive {

    protected static final TraceComponent tc = Tr.register(ExtensionArchiveImpl.class);

    private final ExtensionContainerInfo extensionContainerInfo;
    private Set<String> spiExtensions = null;

    //Store these seperately so we don't try to make regular extensions out of them.
    private Set<String> spiBuildCompatibleExtensions = new HashSet<String>();
    
    //Extensions we cannot store as classnames for some reason.
    //Currently should only ever contain a org.jboss.weld.lite.extension.translator.LiteExtensionTranslator
    //Because LiteExtensionTranslator contain vital state in the object (namely the BCEs it translates) that is not part of the class itself.
    private Set<Supplier<Extension>> extraSPIExtensionSuppliers = new HashSet<Supplier<Extension>>();

    public ExtensionArchiveImpl(ExtensionContainerInfo extensionContainerInfo,
                                RuntimeFactory factory, Set<String> spiExtensions) throws CDIException {
        super(null, extensionContainerInfo, ArchiveType.RUNTIME_EXTENSION, extensionContainerInfo.getClassLoader(), factory);
        this.extensionContainerInfo = extensionContainerInfo;
        this.spiExtensions = spiExtensions;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getExtraClasses() {
        return extensionContainerInfo.getExtraClasses();
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getExtraBeanDefiningAnnotations() {
        return extensionContainerInfo.getExtraBeanDefiningAnnotations();
    }

    /** {@inheritDoc} */
    @Override
    public boolean applicationBDAsVisible() {
        return extensionContainerInfo.applicationBDAsVisible();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isExtClassesOnly() {
        return extensionContainerInfo.isExtClassesOnly();
    }

    //This uses Strings rather than class because we want to load the classes as late as possible.
    @Override
    public Set<String> getExtensionClasses() {
        Set<String> extensionClasses = super.getExtensionClasses();
        extensionClasses.addAll(spiExtensions);
        extensionClasses.addAll(spiBuildCompatibleExtensions);
        return extensionClasses;
    }

    @Override
    public Set<Supplier<Extension>> getSPIExtensionSuppliers() {
        if (spiExtensions.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Supplier<Extension>> result = new HashSet<>();
        for (String className : spiExtensions) {
            result.add(() -> {
                try {
                    Class<?> clazz = getClassLoader().loadClass(className);
                    Constructor<?> constructor = clazz.getConstructor();
                    Object extension = constructor.newInstance();
                    if (!(extension instanceof Extension)) {
                        throw new IllegalArgumentException(extension.getClass().getCanonicalName()
                                                           + " was registered as an extension via the WebSphereCDIExtensionMetaData interface. But it does not implement javax.enterprise.inject.spi.Extension");
                    }
                    return (Extension) extension;
                } catch (Exception e) {
                    Tr.error(tc, "spi.extension.failed.to.construct.CWOWB1010E", className, e.toString());
                    throw new CDIRuntimeException(Tr.formatMessage(tc, "spi.extension.failed.to.construct.CWOWB1010E", className, e.toString()), e);
                }
            });
        }

        result.addAll(extraSPIExtensionSuppliers);

        return result;
    }

    @Override
    public void addLiteExtensionTranslator(Extension liteExtensionTranslator, Set<String> buildCompatibleExtensionClassNames) {
        extraSPIExtensionSuppliers.add(() -> {return liteExtensionTranslator;});
        spiBuildCompatibleExtensions.addAll(buildCompatibleExtensionClassNames);
    }
}
