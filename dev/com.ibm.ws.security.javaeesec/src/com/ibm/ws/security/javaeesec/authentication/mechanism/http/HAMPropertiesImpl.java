/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.authentication.mechanism.http;

import java.util.Properties;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
public class HAMPropertiesImpl implements HAMProperties {
    private static final TraceComponent tc = Tr.register(HAMPropertiesImpl.class);
    private final Class implClass;
    private final Properties props;

    public HAMPropertiesImpl(Class implementationClass, Properties props) {
        this.implClass = implementationClass;
        this.props = props;
    }

    @Override
    public Class getImplementationClass() {
        return implClass;
    }

    @Override
    public Properties getProperties() {
        return props;
    }

}
