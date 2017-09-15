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

public class RMItoIDLTest
{
    @Test
    public void testGetIdlExceptionName()
    {
        Assert.assertEquals("IDL:com/example/TestErrorEx:1.0", RMItoIDL.getIdlExceptionName("com.example.TestError", false));
        Assert.assertEquals("IDL:com/example/TestErrorEx:1.0", RMItoIDL.getIdlExceptionName("com.example.TestError", true));

        Assert.assertEquals("IDL:com/example/TestEx:1.0", RMItoIDL.getIdlExceptionName("com.example.TestException", false));
        Assert.assertEquals("IDL:com/example/TestEx:1.0", RMItoIDL.getIdlExceptionName("com.example.TestException", true));

        Assert.assertEquals("IDL:com/example/exception/TestEx:1.0", RMItoIDL.getIdlExceptionName("com.example.exception.TestException", false));
        Assert.assertEquals("IDL:com/example/_exception/TestEx:1.0", RMItoIDL.getIdlExceptionName("com.example.exception.TestException", true));

        Assert.assertEquals("IDL:com/example/EXCEPTION/TestEx:1.0", RMItoIDL.getIdlExceptionName("com.example.EXCEPTION.TestException", false));
        Assert.assertEquals("IDL:com/example/_EXCEPTION/TestEx:1.0", RMItoIDL.getIdlExceptionName("com.example.EXCEPTION.TestException", true));

        Assert.assertEquals("IDL:com/example/_exception/TestEx:1.0", RMItoIDL.getIdlExceptionName("com.example._exception.TestException", false));
        Assert.assertEquals("IDL:com/example/J_exception/TestEx:1.0", RMItoIDL.getIdlExceptionName("com.example._exception.TestException", true));
    }
}
