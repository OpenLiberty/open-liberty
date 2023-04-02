/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
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

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.wsspi.ejbcontainer.JITDeploy;

public class JIT_StubTest
                extends AbstractStubTestBase
{
    @Test
    public void testGetStubClassName()
    {
        Assert.assertEquals("_Intf_Stub", JIT_Stub.getStubClassName("Intf"));
        Assert.assertEquals("Cls$_Intf_Stub", JIT_Stub.getStubClassName("Cls$Intf"));
        Assert.assertEquals("pkg._Intf_Stub", JIT_Stub.getStubClassName("pkg.Intf"));
        Assert.assertEquals("pkg.Cls$_Intf_Stub", JIT_Stub.getStubClassName("pkg.Cls$Intf"));
    }

    @Override
    protected int[] getRMICCompatible()
    {
        return JITDEPLOY_RMIC_COMPATIBLE;
    }

    @Override
    protected byte[] getStubBytes(Class<?> remoteInterface, String stubClassName, int rmicCompatible)
    {
        try
        {
            return JITDeploy.generateStubBytes(remoteInterface, rmicCompatible);
        } catch (EJBConfigurationException ex)
        {
            throw new IllegalStateException(ex);
        }
    }
}
