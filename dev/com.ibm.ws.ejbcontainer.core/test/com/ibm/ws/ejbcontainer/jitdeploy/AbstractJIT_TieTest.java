/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.jitdeploy;

import javax.rmi.CORBA.Tie;

import org.omg.PortableServer.Servant;

import com.ibm.ejs.container.EJBConfigurationException;

public abstract class AbstractJIT_TieTest extends AbstractTieTestBase {
    @Override
    protected int[] getRMICCompatible() {
        return JITDEPLOY_RMIC_COMPATIBLE;
    }

    protected abstract boolean isPortableServer();

    @Override
    protected Class<?> defineTieClass(Class<?> targetClass, Class<?> remoteInterface, int rmicCompatible, TestClassLoader loader) {
        try {
            return JITDeploy.generate_Tie(loader, targetClass.getName(), remoteInterface, null, new ClassDefiner(), rmicCompatible, isPortableServer());
        } catch (EJBConfigurationException ex) {
            throw new IllegalStateException(ex);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    protected String[] ids(Tie tie) {
        return isPortableServer() ? ((Servant) tie)._all_interfaces(null, null) : super.ids(tie);
    }
}
