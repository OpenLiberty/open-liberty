/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrent.cdi2.web;

import java.util.concurrent.Future;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ConcurrentCDI2AppScopedBean {
    private Future<Integer> servletContainerInitFuture;
    private Future<Integer> servletContextListenerFuture;

    public Future<Integer> getServletContainerInitFuture() {
        return servletContainerInitFuture;
    }

    public Future<Integer> getServletContextListenerFuture() {
        return servletContextListenerFuture;
    }

    public void setServletContainerInitFuture(Future<Integer> future) {
        servletContainerInitFuture = future;
    }

    public void setServletContextListenerFuture(Future<Integer> future) {
        servletContextListenerFuture = future;
    }
}
