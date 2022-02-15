package com.ibm.ws.jaxws;

import com.ibm.wsspi.adaptable.module.Container;

/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * This class is implemented by JaxWsImplBeanCDICustomizer. Registered, Unregistered,
 * and Published by the …jaxws.webcontainer.WebEndpointPublisher.
 * Initialized by the …web.POJOJaxWsWebEndpoint
 */
public interface ImplBeanCustomizer {
    <T> T onPrepareImplBean(Class<T> cls, Container container);

}
