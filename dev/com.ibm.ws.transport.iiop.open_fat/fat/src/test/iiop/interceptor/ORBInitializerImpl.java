/*
 * Copyright (c) 2015,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package test.iiop.interceptor;

import org.omg.CORBA.INITIALIZE;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.SystemException;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitializer;
import org.omg.PortableInterceptor.ORBInitInfoPackage.DuplicateName;

public class ORBInitializerImpl extends LocalObject implements ORBInitializer {
    private static final long serialVersionUID = 1L;

    @Override
    public void pre_init(ORBInitInfo info) {}

    @Override
    public void post_init(ORBInitInfo info) {
        try {
            info.add_server_request_interceptor(new ServerRequestInterceptorImpl());
        } catch (DuplicateName e) {
            throw (SystemException) (new INITIALIZE().initCause(e));
        }
    }
}
