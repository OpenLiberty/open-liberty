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
public class LoginToContinuePropertiesImpl implements LoginToContinueProperties {
    private static final TraceComponent tc = Tr.register(LoginToContinuePropertiesImpl.class);
    private final Properties props;

    public LoginToContinuePropertiesImpl(Properties props) {
        this.props = props;
    }

    @Override
    public Properties getProperties() {
        return props;
    }

}
