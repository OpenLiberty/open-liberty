/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.internal;

import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.config.CommonConfigUtils;
import com.ibm.ws.security.social.TraceConstants;

public class OkdServiceLoginImpl {
    public static final TraceComponent tc = Tr.register(OkdServiceLoginImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public static final String KEY_UNIQUE_ID = "id";
    protected String uniqueId = null;

    private CommonConfigUtils configUtils = new CommonConfigUtils();

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) {
        initProps(cc, props);
        Tr.info(tc, "SOCIAL_LOGIN_CONFIG_PROCESSED", uniqueId);
    }

    @Modified
    protected void modified(ComponentContext cc, Map<String, Object> props) {
        initProps(cc, props);
        Tr.info(tc, "SOCIAL_LOGIN_CONFIG_MODIFIED", uniqueId);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        Tr.info(tc, "SOCIAL_LOGIN_CONFIG_DEACTIVATED", uniqueId);
    }

    public void initProps(ComponentContext cc, Map<String, Object> props) {
        uniqueId = configUtils.getConfigAttribute(props, KEY_UNIQUE_ID);
        // TODO
    }

}
