/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.utils;

import org.junit.Assert;
import org.junit.Test;

public class FilterUtilsTest {

    @Test
    public void testCreatePropertyFilter() {
        Assert.assertEquals("(name=value)", FilterUtils.createPropertyFilter("name", "value"));
        Assert.assertEquals("(name=\\\\value)", FilterUtils.createPropertyFilter("name", "\\value"));
        Assert.assertEquals("(name=value\\\\)", FilterUtils.createPropertyFilter("name", "value\\"));
        Assert.assertEquals("(name=v\\\\a\\*l\\(u\\)e)", FilterUtils.createPropertyFilter("name", "v\\a*l(u)e"));
    }

}
