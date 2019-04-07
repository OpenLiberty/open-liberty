/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.session.async.err.error1.ejb;

import static javax.ejb.TransactionAttributeType.NEVER;

import java.util.logging.Logger;

import javax.ejb.Asynchronous;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;

import com.ibm.ws.ejbcontainer.session.async.err.shared.AsyncError1;

/**
 * Bean implementation class for Enterprise Bean: asyncError1Bean
 **/
@Stateless
@Asynchronous
@Local(AsyncError1.class)
public class AsyncError1Bean {
    public final static String CLASSNAME = AsyncError1Bean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    @TransactionAttribute(NEVER)
    public void test_fireAndForget() {
        svLogger.warning("AsyncError1 test failed.  Asynchronous does not support transaction attribute NEVER.");
        return;
    }

    public AsyncError1Bean() {}
}
