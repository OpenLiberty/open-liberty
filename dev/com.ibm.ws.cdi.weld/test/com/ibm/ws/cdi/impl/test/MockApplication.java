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
package com.ibm.ws.cdi.impl.test;

import java.util.Collection;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.internal.interfaces.Application;
import com.ibm.ws.cdi.internal.interfaces.ApplicationType;
import com.ibm.ws.cdi.internal.interfaces.CDIArchive;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;

public class MockApplication implements Application {

    private class MockJ2EEName implements J2EEName {

        @Override
        public String getApplication() {
            // TODO Auto-generated method stub
            return "mock";
        }

        @Override
        public byte[] getBytes() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getComponent() {
            // TODO Auto-generated method stub
            return "mock";
        }

        @Override
        public String getModule() {
            // TODO Auto-generated method stub
            return "mock";
        }

    }

    @Override
    public J2EEName getJ2EEName() {
        return new MockJ2EEName();
    }

    @Override
    public boolean hasModules() throws CDIException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ClassLoader getClassLoader() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ApplicationType getType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<CDIArchive> getLibraryArchives() throws CDIException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<CDIArchive> getModuleArchives() throws CDIException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ApplicationMetaData getApplicationMetaData() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean getUseJandex() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setTCCL(ClassLoader tccl) {
        // TODO Auto-generated method stub

    }

    @Override
    public ClassLoader getTCCL() {
        // TODO Auto-generated method stub
        return null;
    }

}
