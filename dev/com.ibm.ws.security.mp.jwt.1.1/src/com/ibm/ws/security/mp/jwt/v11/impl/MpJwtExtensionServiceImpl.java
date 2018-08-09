/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.v11.impl;

import java.util.Map;
import java.util.NoSuchElementException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.mp.jwt.MicroProfileJwtExtensionService;
import com.ibm.ws.security.mp.jwt.v11.TraceConstants;

@Component(service = MicroProfileJwtExtensionService.class, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, property = {"service.vendor=IBM", "version=1.1"}, name = "mpJwtExtensionService")
public class MpJwtExtensionServiceImpl implements MicroProfileJwtExtensionService {

    public static final TraceComponent tc = Tr.register(MpJwtExtensionServiceImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    static private String MP_VERSION = "1.1";
    private final String uniqueId = "MicroProfileJwtExtensionService";

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) {
        Tr.info(tc, "MPJWT_11_CONFIG_PROCESSED", uniqueId);
    }

    @Modified
    protected void modified(ComponentContext cc, Map<String, Object> props) {
        Tr.info(tc, "MPJWT_11_CONFIG_MODIFIED", uniqueId);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        Tr.info(tc, "MPJWT_11_CONFIG_DEACTIVATED", uniqueId);
    }

    /**
     * @return
     */
    public String getVersion(){
        return MP_VERSION;
    }

    /**
     * @return
     */
    public boolean isMpConfigAvailable() {
        return false;
    }

    /**
     * @return
     */
    public <T> T getConfigValue(String propertyName, Class<T> propertyType) throws IllegalArgumentException, NoSuchElementException {
        return null;
    }
}
