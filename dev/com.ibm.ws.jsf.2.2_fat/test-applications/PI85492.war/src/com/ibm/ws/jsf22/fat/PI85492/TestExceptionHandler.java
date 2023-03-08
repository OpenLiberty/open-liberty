/*
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.ibm.ws.jsf22.fat.PI85492;

import java.util.Iterator;

import javax.faces.FacesException;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.context.FacesContext;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;
import javax.servlet.http.HttpServletResponse;

public class TestExceptionHandler extends ExceptionHandlerWrapper {

    private final ExceptionHandler wrapped;

    public TestExceptionHandler(ExceptionHandler wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void handle() throws FacesException {

        Iterator<ExceptionQueuedEvent> it = getUnhandledExceptionQueuedEvents().iterator();
        while (it.hasNext()) {
            ExceptionQueuedEventContext eventContext = it.next().getContext();
            Throwable exception = eventContext.getException();
            System.out.println("PI85492: " + exception);

            FacesContext facesContext = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
            // Print out to the log whether or not the response has been committed.
            // We don't expect the response to have been committed at this point.
            System.out.println("PI85492 Resonse commited = " + response.isCommitted());
        }
    }

    @Override
    public ExceptionHandler getWrapped() {
        return wrapped;
    }
}
