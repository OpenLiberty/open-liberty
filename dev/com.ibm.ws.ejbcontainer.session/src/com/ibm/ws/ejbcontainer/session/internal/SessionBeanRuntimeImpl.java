/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.session.internal;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.ejbcontainer.osgi.SessionBeanRuntime;

/**
 * Provides the session bean runtime environment which enables session beans
 * in the core container.
 */
@Component(service = SessionBeanRuntime.class,
           name = "com.ibm.ws.ejbcontainer.session.runtime",
           property = "service.vendor=IBM")
public class SessionBeanRuntimeImpl implements SessionBeanRuntime {
    // Nothing currently needs to be done.  The presence of this class in the
    // service registry enables session beans.
}
