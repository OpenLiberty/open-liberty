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
package com.ibm.ws.session.store.cache;

import java.io.IOException;
import java.security.AccessController;
import java.util.Dictionary;

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.ws.runtime.update.RuntimeUpdateListener;
import com.ibm.ws.runtime.update.RuntimeUpdateManager;
import com.ibm.ws.runtime.update.RuntimeUpdateNotification;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;

/**
 * A RuntimeUpdateListener that detects invalid configuration for the sessionCache feature.
 */
@Component(service = RuntimeUpdateListener.class,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true)
public class RuntimeUpdateListenerImpl implements RuntimeUpdateListener{
    private static final TraceComponent tc = Tr.register(RuntimeUpdateListenerImpl.class);
    
    volatile boolean configChecked = false;
    private ConfigurationAdmin configAdmin = null;
    final static SecureAction priv = AccessController.doPrivileged(SecureAction.get());
    final static String EOL = System.lineSeparator();
    final static String sampleConfig = EOL + EOL + "    <httpSessionCache libraryRef=\"JCacheLib\"/>" + EOL +  EOL + "   <library id=\"JCacheLib\">" + EOL + "        <file name=\"${shared.resource.dir}/jcache/JCacheProvider.jar\"/>" + EOL + "    </library>" + EOL;

    
    @Deactivate
    protected void deactivate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(this, tc, "deactivate", context);
        configChecked = false;
    }

    @Reference
    protected void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        configAdmin = configurationAdmin;
    }

    protected void unsetConfigurationAdmin(ConfigurationAdmin configAdmin) {
        configAdmin = null;
    }

    /**
     * Perform validation checking on the sessionCache feature's configuration.
     */
    @Trivial  //trace is manually added.
    @Override
    public void notificationCreated(RuntimeUpdateManager updateManager, RuntimeUpdateNotification notification) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "notificationCreated", notification.getName());
            
        if (configChecked) {
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "notificationCreated: early return.", notification.getName());
            return;
        }

        if (!configChecked && RuntimeUpdateNotification.APPLICATIONS_STARTING.equals(notification.getName())) {
            configChecked = true;
            
            try {
                String sessionCacheConfigFilter = FilterUtils.createPropertyFilter(Constants.SERVICE_PID, "com.ibm.ws.session.cache");

                Configuration[] sessionCacheConfigurations = configAdmin.listConfigurations(sessionCacheConfigFilter);
                
                if (sessionCacheConfigurations.length != 1) {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "There should always be one exactly one sessionCacheConfiguration. "
                                        + "Number of sessionCacheConfigurations found = " + sessionCacheConfigurations.length);
                }
                
                if (sessionCacheConfigurations != null) {
                    for (Configuration configuration : sessionCacheConfigurations) {
                        if (configuration == null) {
                            continue;
                        }

                        Dictionary<String, Object> props = configuration.getProperties();
                        String[] libraryRefs = (String[]) props.get("libraryRef");

                        if (libraryRefs == null) {               
                            if (!isSessionCacheBellConfigured()) {
                                Tr.error(tc, "ERROR_CONFIG_INVALID_HTTPSESSIONCACHE", Tr.formatMessage(tc, "SESSION_CACHE_CONFIG_MESSAGE", sampleConfig));
                            }
                        } else if (libraryRefs.length == 0) {
                            Tr.debug(tc, "The libraryRef attribute of the httpSessionCache in the server configuration could not be resolved. "
                                            + "Check for possible CWWKG0033W messages.");
                        } else {
                            //verify that the libraryRef points to a valid library configuration object.
                            Configuration libraryConfiguration = configAdmin.getConfiguration(libraryRefs[0]);
                            if(libraryConfiguration != null) {
                                //We found nothing wrong with the configuration.
                                if (trace && tc.isDebugEnabled())
                                    Tr.debug(this, tc, "A httpSessionCache configuration with a valid libraryRef was found.");
                            } else {
                                //This case should not be possible. 
                                //If the libraryRef is missing then libraryRefs is null.
                                //If the libraryRef points to an invalid library, libraryRefs length is 0.
                                //If there is more than one libraryRef, a CWWKG0014E error will have already been thrown.
                                if (trace && tc.isDebugEnabled())
                                    Tr.debug(this, tc, "A httpSessionCache configuration with a valid libraryRef was found, "
                                                    + "but the configuration the libraryRef points to is null. "
                                                    + "This should never happen.");
                            }
                        }
                    }
                }
            } catch ( InvalidSyntaxException | IOException ex) {
                //Something unexpected happened, log the exception as an error.
                Tr.error(tc, "ERROR_SESSION_INIT", ex);
            }
        }
        
        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "notificationCreated", notification.getName());
    }

    /**
     * Check if the server configuration contains a BELL that provides the javax.cache.spi.CachingProvider service.
     * 
     * @return True if a BELL providing the javax.cache.spi.CachingProvider service is configured.
     * @throws IOException
     * @throws InvalidSyntaxException
     */
    private boolean isSessionCacheBellConfigured() throws IOException, InvalidSyntaxException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        boolean sessionCacheBellFound = false;
        String bellConfigFilter = FilterUtils.createPropertyFilter("service.factoryPid", "com.ibm.ws.classloading.bell");
        Configuration[] bellConfigurations = configAdmin.listConfigurations(bellConfigFilter);

        if(bellConfigurations != null) {
            for(Configuration bellConfig : bellConfigurations) {
                if(bellConfig == null) {
                    continue;
                }

                //Check if this BELL defines javax.cache.spi.CachingProvider as a service.
                Dictionary<String, Object> bellProps = bellConfig.getProperties();
                String[] bellServices = (String[]) bellProps.get("service"); 
                for (String service : bellServices) {
                    if("javax.cache.spi.CachingProvider".equals(service)) {
                        sessionCacheBellFound = true;
                        
                        //Check that the bell has a libraryRef that points to an existing library.
                        String bellLibraryRef = (String) bellProps.get("libraryRef");
                        if(bellLibraryRef != null) {
                            
                            //verify that the BELL libraryRef points to a valid library configuration object.
                            Configuration libraryConfiguration = configAdmin.getConfiguration(bellLibraryRef);
                            if(libraryConfiguration != null) {
                                
                                //We found nothing wrong with the configuration.
                                if (trace && tc.isDebugEnabled())
                                    Tr.debug(this, tc, "A javax.cache.spi.CachingProvider BELL with a valid libraryRef was found.");
                            } else {
                                //This case should not be possible. 
                                //If the libraryRef is missing then the BELL will never show up, as libraryRef is required by the BELL metadata.
                                //If the libraryRef points to an invalid library, libraryRef will be null.
                                //If there is more than one libraryRef, a CWWKG0014E error will have already been thrown.
                                if (trace && tc.isDebugEnabled())
                                    Tr.debug(this, tc, "A javax.cache.spi.CachingProvider BELL with a valid libraryRef was found, "
                                                    + "but the configuration the libraryRef points to is null. "
                                                    + "This should never happen.");
                            }
                        } else {
                            Tr.debug(tc, "A BELL with a javax.cache.spi.CachingProvider service was found, "
                                            + "but the libraryRef is invalid. Check for possible CWWKG0033W messages.");
                        }
                    }
                }
            }
        }
        return sessionCacheBellFound;
    }
}
