/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.remote.misc.jitdeploy.ejb;

import java.util.concurrent.Future;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;

@Stateless
public class ExceptionBean {
    public void throwsRuntimeException() throws RuntimeException {
        throw new RuntimeException();
    }

    public void throwsError() throws Error {
        throw new Error();
    }

    public void throwsException() throws Exception {
        throw new RuntimeException();
    }

    @Asynchronous
    public Future<Void> asyncThrowsRuntimeException() throws RuntimeException {
        throw new RuntimeException();
    }

    @Asynchronous
    public Future<Void> asyncThrowsError() throws Error {
        throw new Error();
    }

    @Asynchronous
    public Future<Void> asyncThrowsException() throws Exception {
        throw new RuntimeException();
    }
}