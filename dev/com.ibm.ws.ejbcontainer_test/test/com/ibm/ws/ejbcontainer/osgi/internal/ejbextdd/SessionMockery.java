/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.ejbextdd;


import org.jmock.Expectations;
import org.jmock.Mockery;

import com.ibm.ws.javaee.dd.ejbext.Session;

public class SessionMockery extends EnterpriseBeanMockery<SessionMockery> {

    SessionMockery(Mockery mockery, String name) {
        super(mockery, name);
    }

    public Session mock() {
        final Session session = mockEnterpriseBean(Session.class);
        mockery.checking(new Expectations() {
            {
            }
        });
        return session;
    }
}
