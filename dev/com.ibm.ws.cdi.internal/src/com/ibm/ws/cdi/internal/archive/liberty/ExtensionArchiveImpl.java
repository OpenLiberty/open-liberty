/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.internal.archive.liberty;

import java.util.Set;

import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.internal.interfaces.ArchiveType;
import com.ibm.ws.cdi.internal.interfaces.ExtensionArchive;

public class ExtensionArchiveImpl extends CDIArchiveImpl implements ExtensionArchive {

    private final ExtensionContainerInfo extensionContainerInfo;
    private Set<String> spiExtensions = null;

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
        return extensionClasses;
    }

    @Override
    public Set<String> getSPIExtensionClasses() {
        return spiExtensions;
    }
}
