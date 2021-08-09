/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.monitor;

public interface WebAppPerf {
    public void onApplicationAvailableForService();
    public void onApplicationUnavailableForService();
    public void onApplicationStart();
    public void onApplicationEnd();
    public void onServletStartService(String servletName, String url);
    public void onServletFinishService(String servletName, long responseTime, String url);
    public void onServletStartInit(String j2eeName, String servletName);
    public void onServletFinishInit(String servletName);
    public void onServletStartDestroy(String servletName);
    public void onServletFinishDestroy(String servletName);
    public void onServletUnloaded(String servletName);
    public void onServletAvailableForService(String servletName);
    public void onServletUnavailableForService(String servletName);
    public void onServletInitError(String servletName);
    public void onServletServiceError(String servletName);
    public void onServletServiceDenied(String servletName);
    public void onServletDestroyError(String servletName);
    public void onAsyncContextComplete(String servletName, long responseTime , String url);
}
