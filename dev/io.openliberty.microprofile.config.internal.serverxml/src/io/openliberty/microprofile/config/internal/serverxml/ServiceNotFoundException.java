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
package io.openliberty.microprofile.config.internal.serverxml;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.config.internal.common.ConfigException;

/**
 *
 */
public class ServiceNotFoundException extends ConfigException {

    private static final TraceComponent tc = Tr.register(ServiceNotFoundException.class);

    public ServiceNotFoundException(Class<?> serviceClass) {
        super(Tr.formatMessage(tc, "service.not.found.CWMCG0204E", serviceClass.getName()));
    }

    /**  */
    private static final long serialVersionUID = 1L;

}
