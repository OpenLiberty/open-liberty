/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt;

import java.util.Optional;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.jwt.config.MpConfigProperties;

public interface MpConfigProxyService {

    static final TraceComponent tc = Tr.register(MpConfigProxyService.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    /**
     * @return
     */
    public String getVersion();

    /**
     * @return
     */
    public default boolean isMpConfigAvailable() {
        return true;
    }

    interface MpConfigProxy {
        <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType);
    }

    public MpConfigProxy getConfigProxy(ClassLoader cl);

    @Sensitive
    public default MpConfigProperties getConfigProperties(ClassLoader cl) {
        MpConfigProxy config = getConfigProxy(cl);
        Set<String> propertyNames = getSupportedConfigPropertyNames();
        MpConfigProperties mpConfigProps = new MpConfigProperties();

        for (String propertyName : propertyNames) {
            Optional<String> value = config.getOptionalValue(propertyName, String.class);
            if (value != null && value.isPresent()) {
                String valueString = value.get().trim();
                if (!valueString.isEmpty()) {
                    mpConfigProps.put(propertyName, valueString);
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, propertyName + " is empty. Ignore it.");
                    }

                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, propertyName + " is not in mpConfig.");
                }
            }
        }

        return mpConfigProps;
    }

    public Set<String> getSupportedConfigPropertyNames();

    public default boolean isAcceptableMpConfigProperty(String propertyName) {
        return getSupportedConfigPropertyNames().contains(propertyName);
    }

}
