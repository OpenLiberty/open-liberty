/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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

public class JITUtilsTest
{
    @Test
    public void testGetClassConstantFieldName()
                    throws Exception
    {
        Assert.assertEquals("class$byte", JITUtils.getClassConstantFieldName(byte.class));
        Assert.assertEquals("array$B", JITUtils.getClassConstantFieldName(byte[].class));
        Assert.assertEquals("array$$B", JITUtils.getClassConstantFieldName(byte[][].class));

        Assert.assertEquals("class$java$lang$Object", JITUtils.getClassConstantFieldName(Object.class));
        Assert.assertEquals("array$Ljava$lang$Object", JITUtils.getClassConstantFieldName(Object[].class));
        Assert.assertEquals("array$$Ljava$lang$Object", JITUtils.getClassConstantFieldName(Object[][].class));
    }
}
