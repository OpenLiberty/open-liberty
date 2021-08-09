/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
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

public class RMICTieTest
                extends AbstractTieTestBase
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
    protected Class<?> defineTieClass(Class<?> targetClass, Class<?> remoteInterface, int rmicCompatible, TestClassLoader loader)
    {
        String tieClassName = JIT_Tie.getTieClassName(targetClass.getName());
        return loader.defineClass(tieClassName, getRMICBytes(targetClass, tieClassName));
    }

    @Override
    @Test
    public void testMutableIds()
    {
        super.testMutableIds();
    }

    @Override
    @Test
    public void testExceptionMangling()
    {
        super.testExceptionMangling();
    }
}
