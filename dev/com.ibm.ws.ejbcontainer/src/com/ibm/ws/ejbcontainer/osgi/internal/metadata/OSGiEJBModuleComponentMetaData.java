/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.osgi.internal.metadata;

import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.container.service.metadata.extended.IdentifiableComponentMetaData;
import com.ibm.ws.ejbcontainer.osgi.internal.EJBRuntimeImpl;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataImpl;
import com.ibm.ws.runtime.metadata.ModuleMetaData;

/**
 * A ComponentMetaData implementation that provides access to the java:global, java:app, and java:module
 * naming contexts of an EJB module. Generally available resources bound in java:comp, such as
 * java:comp/UserTransaction, will also be accessible. Access to the naming contexts for specific
 * components (beans) in the module will not be available (i.e., java:comp/env).
 */
public class OSGiEJBModuleComponentMetaData extends MetaDataImpl implements ComponentMetaData, IdentifiableComponentMetaData {

    private final EJBModuleMetaDataImpl ejbMMD;

    public OSGiEJBModuleComponentMetaData(EJBModuleMetaDataImpl ejbModuleMetaData) {
        super(0);
        this.ejbMMD = ejbModuleMetaData;
    }

    @Override
    public ModuleMetaData getModuleMetaData() {
        return ejbMMD;
    }

    @Override
    @Trivial
    public J2EEName getJ2EEName() {
        return ejbMMD.getJ2EEName();
    }

    @Override
    @Trivial
    public String getName() {
        return ejbMMD.getName();
    }

    @Override
    @Trivial
    public String getPersistentIdentifier() {
        return EJBRuntimeImpl.getMetaDataIdentifierImpl(ejbMMD.getJ2EEName());
    }
}
