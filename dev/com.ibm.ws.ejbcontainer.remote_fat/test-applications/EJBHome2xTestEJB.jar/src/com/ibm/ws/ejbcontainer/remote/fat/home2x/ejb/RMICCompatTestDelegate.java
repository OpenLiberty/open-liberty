/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.home2x.ejb;

import org.omg.CORBA.Context;
import org.omg.CORBA.ContextList;
import org.omg.CORBA.ExceptionList;
import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CORBA.NVList;
import org.omg.CORBA.NamedValue;
import org.omg.CORBA.Request;
import org.omg.CORBA.portable.Delegate;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;

public class RMICCompatTestDelegate extends Delegate {
    // TODO: extra method added by Yoko; should not be required.
    public org.omg.CORBA.InterfaceDef get_interface(org.omg.CORBA.Object obj) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public org.omg.CORBA.Object get_interface_def(org.omg.CORBA.Object obj) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public org.omg.CORBA.Object duplicate(org.omg.CORBA.Object obj) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public void release(org.omg.CORBA.Object obj) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public boolean is_a(org.omg.CORBA.Object obj, String repository_id) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public boolean non_existent(org.omg.CORBA.Object obj) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public boolean is_equivalent(org.omg.CORBA.Object obj, org.omg.CORBA.Object other) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public int hash(org.omg.CORBA.Object obj, int max) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public Request request(org.omg.CORBA.Object obj, String operation) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public Request create_request(org.omg.CORBA.Object obj, Context ctx, String operation, NVList arg_list, NamedValue result) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public Request create_request(org.omg.CORBA.Object obj, Context ctx, String operation, NVList arg_list, NamedValue result, ExceptionList exclist, ContextList ctxlist) {
        throw new NO_IMPLEMENT();
    }

    @Override
    public OutputStream request(org.omg.CORBA.Object self, String operation, boolean responseExpected) {
        return new RMICCompatTestOutputStream();
    }

    @Override
    public InputStream invoke(org.omg.CORBA.Object self, OutputStream output) {
        return new RMICCompatTestInputStream(((RMICCompatTestOutputStream) output).value);
    }

    @Override
    public void releaseReply(org.omg.CORBA.Object self, InputStream input) {
        // Ignore.
    }
}
