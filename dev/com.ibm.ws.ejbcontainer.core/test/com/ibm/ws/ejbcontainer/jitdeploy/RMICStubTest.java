/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.jitdeploy;

import org.junit.Test;

public class RMICStubTest
                extends AbstractStubTestBase
{
    @Override
    protected boolean isRMIC() {
        return true;
    }

    @Override
    protected int[] getRMICCompatible()
    {
        return RMIC_RMIC_COMPATIBLE;
    }

    @Override
    protected byte[] getStubBytes(Class<?> remoteInterface, String stubClassName, int rmicCompatible)
    {
        return getRMICBytes(remoteInterface, stubClassName);
    }

    @Override
    @Test
    public void testMutableIds()
    {
        super.testMutableIds();
    }

    @Override
    @Test
    public void testPrimitive()
                    throws Exception
    {
        super.testPrimitive();
    }

    @Override
    @Test
    public void testClass()
                    throws Exception
    {
        super.testClass();
    }

    @Override
    @Test
    public void testInterface()
                    throws Exception
    {
        super.testInterface();
    }

    @Override
    protected boolean isExtendsCORBAObjectSupported()
    {
        // TODO: rmic generates uncompilable source for ExtendsCORBAObject.
        return false;
    }

    @Override
    @Test
    public void testExceptionMangling()
                    throws Exception
    {
        super.testExceptionMangling();
    }
}
