/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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

public class CORBA_UtilsTest {
    @Test
    public void testGetRemoteTypeId() {
        Assert.assertEquals("RMI:java.lang.Runnable:0000000000000000", CORBA_Utils.getRemoteTypeId(Runnable.class));
        Assert.assertEquals("RMI:java.lang.Runnable:0000000000000000", CORBA_Utils.getRemoteTypeId(Runnable.class.getName()));
    }
}
