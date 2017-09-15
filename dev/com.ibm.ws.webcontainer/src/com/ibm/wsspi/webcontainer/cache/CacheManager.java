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
package com.ibm.wsspi.webcontainer.cache;

/**
 *
 */
public interface CacheManager {
    public javax.servlet.Servlet getProxiedServlet(javax.servlet.Servlet s);
    //public javax.servlet.Servlet getSingleThreadModelWrapper(javax.servlet.Servlet s);
    public boolean isStaticFileCachingEnabled(String contextRoot);
}
