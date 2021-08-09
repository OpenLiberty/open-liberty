/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.jitdeploy;

import org.omg.CORBA.Context;
import org.omg.CORBA.ContextList;
import org.omg.CORBA.ExceptionList;
import org.omg.CORBA.InterfaceDef;
import org.omg.CORBA.NVList;
import org.omg.CORBA.NamedValue;
import org.omg.CORBA.Request;
import org.omg.CORBA.portable.ApplicationException;
import org.omg.CORBA.portable.Delegate;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;

import junit.framework.Assert;

public class TestDelegateImpl extends Delegate {
    TestMethodCalls ivCalls;

    TestDelegateImpl(TestMethodCalls calls) {
        ivCalls = calls;
    }

    @Override
    public org.omg.CORBA.Object get_interface_def(org.omg.CORBA.Object self) {
        throw new UnsupportedOperationException();
    }

    @Override
    public org.omg.CORBA.Object duplicate(org.omg.CORBA.Object obj) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void release(org.omg.CORBA.Object obj) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean is_a(org.omg.CORBA.Object obj, String repository_id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean non_existent(org.omg.CORBA.Object obj) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean is_equivalent(org.omg.CORBA.Object obj, org.omg.CORBA.Object other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hash(org.omg.CORBA.Object obj, int max) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Request request(org.omg.CORBA.Object obj, String operation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Request create_request(org.omg.CORBA.Object obj, Context ctx, String operation, NVList arg_list, NamedValue result) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Request create_request(org.omg.CORBA.Object obj, Context ctx, String operation, NVList arg_list, NamedValue result, ExceptionList exclist, ContextList ctxlist) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OutputStream request(org.omg.CORBA.Object self, String operation, boolean responseExpected) {
        if (ivCalls.ivOperation != null) {
            Assert.assertEquals(ivCalls.ivOperation, operation);
        }
        Assert.assertTrue(responseExpected);

        return new TestOutputStreamImpl(ivCalls);
    }

    @SuppressWarnings("resource")
    @Override
    public InputStream invoke(org.omg.CORBA.Object self, OutputStream output) throws ApplicationException {
        if (ivCalls.ivApplicationException) {
            throw new ApplicationException(null, new TestInputStreamImpl(ivCalls));
        }

        return new TestInputStreamImpl(ivCalls);
    }

    @Override
    public void releaseReply(org.omg.CORBA.Object self, InputStream input) { /* empty */}

    public InterfaceDef get_interface(org.omg.CORBA.Object arg0) {
        throw new UnsupportedOperationException();
    }
}
