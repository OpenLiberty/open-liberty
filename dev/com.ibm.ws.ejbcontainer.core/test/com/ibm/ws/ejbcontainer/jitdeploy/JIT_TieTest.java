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

import org.junit.Assert;
import org.junit.Test;

public class JIT_TieTest extends AbstractJIT_TieTest {
    @Test
    public void testGetTieClassName() {
        Assert.assertEquals("_Impl_Tie", JIT_Tie.getTieClassName("Impl"));
        Assert.assertEquals("Cls$_Impl_Tie", JIT_Tie.getTieClassName("Cls$Impl"));
        Assert.assertEquals("pkg._Impl_Tie", JIT_Tie.getTieClassName("pkg.Impl"));
        Assert.assertEquals("pkg.Cls$_Impl_Tie", JIT_Tie.getTieClassName("pkg.Cls$Impl"));
    }

    @Override
    protected boolean isPortableServer() {
        return false;
    }
}
