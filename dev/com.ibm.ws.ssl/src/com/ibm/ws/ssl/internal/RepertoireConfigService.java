/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ssl.internal;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ssl.config.SSLConfigManager;

/**
 * Class use to uniquely identify SSL configuration Repertoires
 */
@Component(configurationPid = "com.ibm.ws.ssl.repertoire",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           service = { RepertoireConfigService.class, com.ibm.wsspi.ssl.SSLConfiguration.class },
           property = "service.vendor=IBM")
public class RepertoireConfigService extends GenericSSLConfigService implements com.ibm.wsspi.ssl.SSLConfiguration {
    private static final TraceComponent tc = Tr.register(RepertoireConfigService.class);

    private String id = "unknownConfig";

    private String servicePid = "unknownConfig"; // also store service pid, issue 876

    private KeystoreConfig keyStore;
    private KeystoreConfig trustStore;

    @Activate
    protected void activate(Map<String, Object> properties) {
        id = (String) properties.get(LibertyConstants.KEY_ID);
        servicePid = (String) properties.get("service.pid");

        // Put SSL properties in the map.
        try {
            SSLConfigManager.getInstance().addSSLPropertiesToMap(properties, true);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "SSL properties not added to map " + properties.get("id"), e.getMessage());
            }
        }
        super.activate(id, properties);
    }

    @Modified
    protected void modified(Map<String, Object> properties) {
        try {
            SSLConfigManager.getInstance().addSSLPropertiesToMap(properties, true);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "SSL properties not added to map " + properties.get("id"), e.getMessage());
            }
        }
        super.modified(id, properties);
    }

    @Deactivate
    protected void deactivate(int reason) {
        SSLConfigManager.getInstance().removeSSLPropertiesFromMap(id, true);
        super.deactivate(id, reason);
    }

    /**
     * Returns the alias for this SSL configuration.
     *
     * @return the alias for this SSL configuration.
     */
    @Override
    public String getAlias() {
        return id;
    }

    public String getPID() {
        return servicePid;
    }

    @Reference
    protected void setKeyStore(KeystoreConfig ref, Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "set keystore " + properties.get("id"), properties);
        }
        this.keyStore = ref;
    }

    KeystoreConfig getKeyStore() {
        return keyStore;
    }

    @Reference
    protected void setTrustStore(KeystoreConfig ref, Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "set truststore " + properties.get("id"), properties);
        }
        this.trustStore = ref;
    }

    KeystoreConfig getTrustStore() {
        return trustStore;
    }

}
