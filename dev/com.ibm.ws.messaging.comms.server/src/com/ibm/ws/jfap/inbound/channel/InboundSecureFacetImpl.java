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
package com.ibm.ws.jfap.inbound.channel;

import static com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled;
import static com.ibm.ws.sib.utils.ras.SibTr.entry;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;

import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.channelfw.osgi.ChannelFactoryProvider;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.wsspi.channelfw.ChannelConfiguration;

/**
 * This component shares its configuration pid with {@link CommsServerServiceFacade}.
 * This component will deal with the SSL configuration of a <code>wasJmsEndpoint</code>. 
 * 
 */
@Component(
        configurationPid = "com.ibm.ws.messaging.comms.server",
        configurationPolicy = REQUIRE, 
        property = "service.vendor=IBM")
public class InboundSecureFacetImpl implements InboundSecureFacet {
    private static final TraceComponent tc = Tr.register(InboundSecureFacetImpl.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);
    private final ChannelConfiguration sslOptions;
    private final boolean secureSocketsEnabled;

    @Activate
    public InboundSecureFacetImpl(
            @Reference(name = "sslOptions", target = "(id=unbound)", cardinality = OPTIONAL) // target to be overwritten by metatype
            ChannelConfiguration sslOptions,
            /* We have preserved the original behaviour of using defaultSSLOptions 
             * If we want to use ${defaultSSLVar}, use (id=unbound) here and set it in metatype */
            @Reference(name="defaultSSLOptions", target="(id=defaultSSLOptions)", cardinality=OPTIONAL)
            ChannelConfiguration defaultSSLOptions,
            @Reference(name = "sslFactoryProvider", target = "(type=SSLChannel)", cardinality = OPTIONAL)
            ChannelFactoryProvider sslFactoryProvider,
            Map<String, Object> properties) {
        final String methodName = "<init>";
        if (isAnyTracingEnabled() && tc.isEntryEnabled())
            entry(this, tc, methodName, new Object[]{sslOptions, defaultSSLOptions, sslFactoryProvider, properties});

        this.sslOptions = Optional.ofNullable(sslOptions).orElse(defaultSSLOptions);
        this.secureSocketsEnabled = sslFactoryProvider != null;
    }

    public ChannelConfiguration getOptions() { return sslOptions; }
    public boolean areSecureSocketsEnabled() { return secureSocketsEnabled; }
}
