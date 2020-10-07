/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package user.feature;

import java.util.Map;

import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.AuthConfigProvider;

import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.jaspi.test.AuthProvider;
import com.ibm.wsspi.security.jaspi.ProviderService;

/**
 * blah
 **/
public class JaspiProviderService implements ProviderService {

    private static final TraceComponent tc = Tr.register(JaspiProviderService.class, "Security", null);

    public JaspiProviderService() {}

    Map<String, String> props = null;

    protected void activate(ComponentContext cc) {
        // Read provider config properties here then pass them
        // to the AuthConfigProvider ctor when getAuthConfigProvider
        // is called
        System.out.println("JaspiProviderService " + cc.getProperties());
        props = (Map<String, String>) cc.getProperties();
    }

    protected void deactivate(ComponentContext cc) {}

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.security.jaspi.ProviderService#getAuthConfigProvider(java.util.Map, javax.security.auth.message.config.AuthConfigFactory)
     */
    @Override
    public AuthConfigProvider getAuthConfigProvider(AuthConfigFactory factory) {
        return new AuthProvider(props, factory);
    }
}
