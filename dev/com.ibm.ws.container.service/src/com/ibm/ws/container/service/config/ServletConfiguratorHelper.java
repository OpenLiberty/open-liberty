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
package com.ibm.ws.container.service.config;

import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.webbnd.WebBnd;
import com.ibm.ws.javaee.dd.webext.WebExt;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 *
 */
public interface ServletConfiguratorHelper {

    public void configureInit() throws UnableToAdaptException;

    public void configureFromWebApp(WebApp webApp) throws UnableToAdaptException;

    public void configureFromWebFragment(WebFragmentInfo webFragmentItem) throws UnableToAdaptException;

    public void configureFromAnnotations(WebFragmentInfo webFragmentItem) throws UnableToAdaptException;

    public void configureDefaults() throws UnableToAdaptException;

    public void configureWebBnd(WebBnd webBnd) throws UnableToAdaptException;

    public void configureWebExt(WebExt webExt) throws UnableToAdaptException;

    public void finish() throws UnableToAdaptException;
}
