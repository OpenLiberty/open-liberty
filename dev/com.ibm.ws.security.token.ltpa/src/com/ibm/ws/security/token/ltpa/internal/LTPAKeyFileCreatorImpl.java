/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.UserRegistry;
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyFileUtilityImpl;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.location.WsResource.Type;
import com.ibm.wsspi.kernel.service.utils.FileUtils;
import com.ibm.wsspi.security.registry.RegistryHelper;
import org.osgi.service.component.ComponentContext;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import org.osgi.framework.ServiceReference;

/**
 *
 */
public class LTPAKeyFileCreatorImpl extends LTPAKeyFileUtilityImpl implements LTPAKeyFileCreator {

    private static final TraceComponent tc = Tr.register(LTPAKeyFileCreatorImpl.class);
    private volatile ComponentContext cc = null;
    static final String KEY_USER_REGISTRY_SERVICE = "userRegistryService";
    private final AtomicServiceReference<UserRegistryService> userRegistryServiceRef = new AtomicServiceReference<UserRegistryService>(KEY_USER_REGISTRY_SERVICE);

    protected void setUserRegistryService(ServiceReference<UserRegistryService> ref) {
        userRegistryServiceRef.setReference(ref);
    }
    protected void unsetUserRegistryService(ServiceReference<UserRegistryService> ref) {
        userRegistryServiceRef.unsetReference(ref);
    }

    /*
     * When FileMonitor is enabled, its onBaseline method will call performFileBasedAction(baselineFiles)
     * to process key files after loadConfig(props), but before submitTaskToCreateLTPAKeys().
     */
    protected void activate(ComponentContext context) {
        cc = context;
        userRegistryServiceRef.activate(context); 
    }

    protected void deactivate(ComponentContext context) {
        userRegistryServiceRef.deactivate(context);
    }

    /**
     * Obtains the realm name of the configured UserRegistry, if one is available.
     *
     * @return The configured realm name, or "defaultRealm" if no UserRegistry is present
     */
    private String getRealmName() {
        String realm = "defaultRealm";
        try {
            UserRegistry ur = RegistryHelper.getUserRegistry(null);
            if (ur != null) {
                String r = ur.getRealm();
                if (r != null) {
                    realm = r;
                }
            }
        } catch (Exception ex) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Cannot get the UR service since it may not be available so use the default value for the realm.", ex);
            }
        }

        return realm;
    }

    /** {@inheritDoc} */
    @Override
    public Properties createLTPAKeysFile(WsLocationAdmin locService, String keyFile, @Sensitive byte[] keyPasswordBytes) throws Exception {
        String realmName = isUserRegistryAvailable()?getRealmName():"defaultRealm";
        Properties ltpaProps = generateLTPAKeys(keyPasswordBytes, realmName);
        addLTPAKeysToFile(getOutputStream(locService, keyFile), ltpaProps);
        return ltpaProps;
    }

    private OutputStream getOutputStream(WsLocationAdmin locService, final String keyImportFile) throws IOException {
        // Get the WsResource and create the file
        WsResource ltpaFile = locService.resolveResource(keyImportFile);
        ltpaFile.create();
        if (ltpaFile.isType(Type.FILE)) {
            FileUtils.setUserReadWriteOnly(ltpaFile.asFile());
        }

        // Get the output stream form the resource service
        return ltpaFile.putStream();
    }


    /**Checks if the UserRegistry Service is available
     * 
     * @return true if the UserRegistry is available and configured, false otherwise
     */
    private boolean isUserRegistryAvailable(){
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
            Tr.debug(tc, "Checking if UserRegistry is available");
        }

        UserRegistryService userRegistryService = userRegistryServiceRef.getService();
        if (userRegistryService == null){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "UserRegistryService is not available, defaultRealm will be used for LTPA configuration");
            }
            return false;
        }
        return true;
    }
}
