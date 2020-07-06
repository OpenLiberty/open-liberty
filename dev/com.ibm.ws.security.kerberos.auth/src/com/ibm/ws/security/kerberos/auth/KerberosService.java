/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.kerberos.auth;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.service.util.SecureAction;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE,
           service = KerberosService.class,
           configurationPid = "com.ibm.ws.security.kerberos.auth.KerberosService",
           immediate = true,
           property = "service.vendor=IBM")
public class KerberosService {

    private static final TraceComponent tc = Tr.register(KerberosService.class);

    private static final String KRB5_CONFIG_PROPERTY = "java.security.krb5.conf";

    static SecureAction priv = AccessController.doPrivileged(SecureAction.get());

    private Path keytab;
    private Path configFile;

    @Activate
    protected void activate(ComponentContext ctx) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "activate", ctx.getProperties());
        }

        String rawKeytab = (String) ctx.getProperties().get("keytab");
        String rawConfigFile = (String) ctx.getProperties().get("configFile");

        if (rawKeytab != null) {
            keytab = Paths.get(rawKeytab);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Keytab was configured to: " + keytab);
            }
        }
        if (rawConfigFile != null) {
            configFile = Paths.get(rawConfigFile);
            String originalConfigFile = priv.getProperty(KRB5_CONFIG_PROPERTY);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Setting system property " + KRB5_CONFIG_PROPERTY + "=" + configFile.toAbsolutePath().toString() +
                             "  Previous value was: " + originalConfigFile);
            }
            priv.setProperty(KRB5_CONFIG_PROPERTY, configFile.toAbsolutePath().toString());
        }
    }

    public Path getConfigFile() {
        return configFile;
    }

    /**
     * @return The path of the keytab file, or null if unspecified
     */
    public Path getKeytab() {
        return keytab;
    }

}
