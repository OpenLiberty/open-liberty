/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.util;

import java.rmi.RemoteException;

import javax.ejb.EJBException;

import org.junit.Assert;
import org.junit.Test;

public class ExceptionUtilTest
{
    @Test
    public void testGetBaseMessage()
    {
        Assert.assertEquals(null, ExceptionUtil.getBaseMessage(new Exception()));
        Assert.assertEquals("test", ExceptionUtil.getBaseMessage(new Exception("test")));

        Assert.assertEquals(null, ExceptionUtil.getBaseMessage(new RemoteException()));
        Assert.assertEquals("test", ExceptionUtil.getBaseMessage(new RemoteException("test")));
        Assert.assertEquals(null, ExceptionUtil.getBaseMessage(new RemoteException(null, new Exception("test"))));
        Assert.assertEquals("test", ExceptionUtil.getBaseMessage(new RemoteException("", new Exception("test"))));
        Assert.assertEquals("test", ExceptionUtil.getBaseMessage(new RemoteException("test", new Exception("cause"))));

        Assert.assertEquals(null, ExceptionUtil.getBaseMessage(new EJBException()));
        Assert.assertEquals("test", ExceptionUtil.getBaseMessage(new EJBException("test")));
        Assert.assertEquals(null, ExceptionUtil.getBaseMessage(new EJBException(null, new Exception("cause"))));
        Assert.assertEquals("test", ExceptionUtil.getBaseMessage(new EJBException("test", new Exception("cause"))));
    }

    //    @Test
    //    public void testWsEJBExceptionGetCause()
    //    {
    //        assertEquals(null, (new WsEJBException("no cause")).getCause());
    //
    //        Exception cause = new IllegalStateException("the cause");
    //        assertSame(cause, (new WsEJBException("with exception", cause)).getCause());
    //        assertSame(cause, (new WsEJBException("with exception")).initCause(cause).getCause());
    //
    //        Throwable th = new Throwable("the cause");
    //        assertSame(th, (new WsEJBException("with throwable", th)).getCause());
    //        assertSame(th, (new WsEJBException("with throwable")).initCause(th).getCause());
    //    }

    //    @Test
    //    public void testWsEJBExceptionGetCausedByException()
    //    {
    //        assertEquals(null, (new WsEJBException("no cause")).getCausedByException());
    //
    //        Exception cause = new IllegalStateException("the cause");
    //        assertSame(cause, (new WsEJBException("with exception", cause)).getCausedByException());
    //        assertSame(cause, ((EJBException) (new WsEJBException("with exception")).initCause(cause)).getCausedByException());
    //
    //        Throwable th = new Throwable("the cause");
    //        Exception causeBy = (new WsEJBException("with throwable", th)).getCausedByException();
    //        assertTrue(causeBy instanceof WsException);
    //        assertSame(th, causeBy.getCause());
    //
    //        causeBy = ((EJBException) (new WsEJBException("with throwable")).initCause(th)).getCausedByException();
    //        assertTrue(causeBy instanceof WsException);
    //        assertSame(th, causeBy.getCause());
    //    }

    //    @Test
    //    public void testWsEJBExceptionCompatibility() throws Exception
    //    {
    //        Exception cause = new IllegalStateException("the cause");
    //        WsEJBException wsEjbEx = new WsEJBException("with exception", cause);
    //
    //        // Reset 'cause' field on Throwable to make this look like it has
    //        // been deserialized from an older level of JDK.
    //        Field causeFld = Throwable.class.getDeclaredField("cause");
    //        causeFld.setAccessible(true);
    //        causeFld.set(wsEjbEx, wsEjbEx);
    //
    //        assertSame(cause, wsEjbEx.getCause());
    //        assertSame(cause, wsEjbEx.getCausedByException());
    //    }

}
