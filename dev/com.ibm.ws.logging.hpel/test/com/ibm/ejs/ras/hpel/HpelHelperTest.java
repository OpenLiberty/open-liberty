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
package com.ibm.ejs.ras.hpel;

import org.junit.Assert;
import org.junit.Test;
import org.omg.CORBA.portable.UnknownException;

public class HpelHelperTest {
    @Test
    public void testThrowableToStringUnknownException() {
        UnknownException e = new UnknownException(new Throwable("original"));
        String s = HpelHelper.throwableToString(e);
        Assert.assertTrue(s, s.contains(e.originalEx.getMessage()));
    }
}
