/*******************************************************************************
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
 *******************************************************************************/
package io.openliberty.jaxws.wssecurity;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapMessage;
//import org.apache.cxf.ws.security.SecurityConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.wssecurity.WSSecurityFeatureHelperService;
import net.sf.ehcache.config.Configuration;


/**
 *
 */
@org.osgi.service.component.annotations.Component(service = WSSecurityFeatureHelperService.class, name = "WSSecurityFeatureHelperService", immediate = true)
public class WSSecurityFeatureHelperServiceImpl implements WSSecurityFeatureHelperService {
    public static final String CACHE_CONFIG_FILE = "ws-security.cache.config.file";
    public static final TraceComponent tc = Tr.register(WSSecurityFeatureHelperServiceImpl.class);
    @Override
    public void handleEhcache2Mapping(String key, URL url, @Sensitive SoapMessage message) {
  
        parseehcachefile(key, url, message);
    }
    @FFDCIgnore (Exception.class)
    private void parseehcachefile(String instanceKey, URL configfile, @Sensitive SoapMessage message) {
        net.sf.ehcache.CacheManager cacheManager = null;

        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(net.sf.ehcache.store.DefaultElementValueComparator.class.getClassLoader());
            cacheManager = net.sf.ehcache.CacheManager.create(configfile);
            net.sf.ehcache.config.CacheConfiguration cc = null;
            net.sf.ehcache.config.Configuration config = null;
            config = cacheManager.getConfiguration();
            if (config != null) {
                String cacheKey = instanceKey;
                cc = cacheManager.getConfiguration().getCacheConfigurations().get(cacheKey);

            }
            if (cc == null) {
                cc = cacheManager.getConfiguration().getDefaultCacheConfiguration();
            }
            if (cc != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc,  "success getting cache using oldconfig!!!");
                }
                
                updateMessageCacheMap(instanceKey, message, cc, config);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "removing the cache config file property from the message : ", CACHE_CONFIG_FILE);
                }
                message.remove(CACHE_CONFIG_FILE);
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "cannot parse the file using the old cache apis, instancekey = " + instanceKey + ", url = " + configfile.getFile());
                Tr.debug(tc,  "Exception parsing the old ehcache config format = ", e.getMessage());
            }
            //throw e;
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader); 
        }
        
    }

    /**
     * @param instanceKey 
     * @param message
     * @param cc
     * @param config 
     */
    private void updateMessageCacheMap(String instanceKey, @Sensitive SoapMessage message, net.sf.ehcache.config.CacheConfiguration cc, Configuration config) {
        String key = "liberty:".concat(instanceKey);
        if (cc != null) {
            Map<String, Object> configmap = new HashMap<String, Object>();
            
            configmap.put("getTimeToLiveSeconds", cc.getTimeToLiveSeconds());
            configmap.put("getTimeToIdleSeconds", cc.getTimeToIdleSeconds());
            configmap.put("getMaxEntriesLocalHeap", cc.getMaxEntriesLocalHeap());
            configmap.put("getMaxBytesLocalDisk", cc.getMaxBytesLocalDisk());
            configmap.put("isEternal", cc.isEternal());
            configmap.put("isOverflowToDisk", cc.isOverflowToDisk());
            configmap.put("getMaxElementsOnDisk", cc.getMaxElementsOnDisk());
            configmap.put("isDiskPersistent", cc.isDiskPersistent());
            configmap.put("getDiskExpiryThreadIntervalSeconds", cc.getDiskExpiryThreadIntervalSeconds());
            configmap.put("getMemoryStoreEvictionPolicy", cc.getMemoryStoreEvictionPolicy());
            String path = (String)config.getDiskStoreConfiguration().getOriginalPath();
            if ("java.io.tmpdir".equals(path)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc,  "updating diskstorepath, before the update = " , path);
                }
                Bus bus = message.getExchange().getBus();
                path = path + File.separator
                                + bus.getId();
                config.getDiskStoreConfiguration().setPath(path);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc,  "updating diskstorepath, after the update = " , path);
                }
            }
            configmap.put("getDiskStorePath", path);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc,  "updating message using oldconfig, key =  " , key);
                Tr.debug(tc,  "updating message using oldconfig, ttl, tti =  " , configmap.get("getTimeToLiveSeconds"), configmap.get("getTimeToIdleSeconds"));
                Tr.debug(tc,  "updating message using oldconfig, diskstorepath = " , configmap.get("getDiskStorePath"));
                Tr.debug(tc,  "updating message using oldconfig, diskelements = " , configmap.get("getMaxElementsOnDisk"));
                Tr.debug(tc,  "updating message using oldconfig, diskbytes = " , configmap.get("getMaxBytesLocalDisk"));
            }
            String[] ignored = {"getDiskExpiryThreadIntervalSeconds", "getMemoryStoreEvictionPolicy", "isOverflowToDisk"};
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "using an old ehcache configuration and these properties will be ignored, " +  ignored[0] + 
                        "= " + (long)configmap.get("getDiskExpiryThreadIntervalSeconds") + ", " + ignored[1] + "= " + configmap.get("getMemoryStoreEvictionPolicy") 
                        + ", " + ignored[2] + "= " + configmap.get("isOverflowToDisk")
                       ); // TODO : information
            }

            message.setContextualProperty(key, configmap);
        }
        
    }
    
    @Activate
    protected void activate(ComponentContext cc) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "WSSecurityFeatureHelperService (impl) is activated");
        }
    }

    @Modified
    protected void modified(Map<String, Object> props) {
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "WSSecurityFeatureHelperService (impl) is deactivated");
        }
    }
}
