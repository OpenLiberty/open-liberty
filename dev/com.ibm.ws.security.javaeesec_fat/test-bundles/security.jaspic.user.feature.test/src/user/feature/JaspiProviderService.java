/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
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
