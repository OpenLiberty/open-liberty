/*
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.sib.jfapchannel.impl;

import static com.ibm.websphere.ras.Tr.exit;
import static com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled;
import static com.ibm.ws.sib.utils.ras.SibTr.debug;
import static com.ibm.ws.sib.utils.ras.SibTr.entry;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.wsspi.channelfw.ChannelConfiguration;

/**
 * This component shares its configuration pid with {@link CommsOutboundChain}.
 * This component will deal with the SSL configuration of a <code>wasJmsOutbound</code>. 
 * 
 */
@Component(
        configurationPid = "com.ibm.ws.messaging.comms.wasJmsOutbound",
        configurationPolicy = REQUIRE, 
        property = { "sslOptions.cardinality.minimum=1" }
        )
public class OutboundSecureFacetImpl implements OutboundSecureFacet {
    private static final TraceComponent tc = Tr.register(OutboundSecureFacetImpl.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);
    private final String id;
    private final ChannelConfiguration sslOptions;

    @Activate
    public OutboundSecureFacetImpl(
            @Reference(name = "sslOptions", target = "(id=unbound)", cardinality = OPTIONAL) // target to be overwritten by metatype
            ChannelConfiguration sslOptions,
            /* We have preserved the original behaviour of using defaultSSLOptions 
             * If we want to use ${defaultSSLVar}, use (id=unbound) here and set it in metatype */
            @Reference(name="defaultSSLOptions", target="(id=defaultSSLOptions)")
            ChannelConfiguration defaultSSLOptions,
            Map<String, Object> properties) {
        final String methodName = "<init>";
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(this, tc, methodName, new Object[]{sslOptions, defaultSSLOptions, properties});

        this.id = (String) properties.get("id");
        this.sslOptions = Optional.ofNullable(sslOptions).orElse(defaultSSLOptions);

        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(this, tc, methodName, this);
    }

    public Map<Object, Object> copyConfig() {
        final String methodName = "copyConfig";
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(this, tc, methodName, sslOptions);
        Map<?,?> props = sslOptions.getConfiguration();
        final Map<Object, Object> result;
        if (null == props) {
            if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "sslOptions has null config object");
            result = new HashMap<>();
        } else {
            result = new HashMap<>(props);
        }
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(this, tc, methodName, result);
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", super.toString(), id);
    }
}
