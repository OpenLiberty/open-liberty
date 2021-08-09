/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.token.mapping;

import java.rmi.RemoteException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

import com.ibm.wsspi.security.oauth20.UserCredentialResolver;

public class Activator implements BundleActivator/*, ManagedServiceFactory*/ {
    private ServiceRegistration<UserCredentialResolver> serviceConfigRef;

    String _cfg_pid = "OAuth20TokenMapping";
   
    BundleContext _context = null;
    Hashtable<String, ServiceRegistration<UserCredentialResolver>> userIdentityResolverRef =
                    new Hashtable<String, ServiceRegistration<UserCredentialResolver>>(10, 5);

    Dictionary<String, Object> _properties;

    public Activator() throws RemoteException {}

    Hashtable<String, Object> getDefaults()
    {
        System.out.println("getDefaults");
        Hashtable<String, Object> defaults = new Hashtable<String, Object>();
        defaults.put(org.osgi.framework.Constants.SERVICE_PID, _cfg_pid);
        defaults.put("id", "spi_resolved");
        defaults.put("service.vendor", "IBM");
        return defaults;
    }

    @Override
    public void start(BundleContext context)
                    throws Exception
    {
        System.out.println("OAuth20 token mapping service starts:" + context.getClass().getName());
        _context = context;
        OAuth20TokenMappingResolver tokenMapper = new OAuth20TokenMappingResolver(getDefaults());
        serviceConfigRef = _context.registerService(UserCredentialResolver.class, tokenMapper, getDefaults());
        //serviceConfigRef = context.registerService(ManagedServiceFactory.class, this, getDefaults());
        System.out.println("OAuth20 token mapping service (configRef):" + serviceConfigRef);
        userIdentityResolverRef.put(_cfg_pid, serviceConfigRef);
    }

    @Override
    public void stop(BundleContext context)
                    throws Exception
    {
        System.out.println("service stop");
        if (serviceConfigRef != null)
        {
            serviceConfigRef.unregister();
            serviceConfigRef = null;
        }
        _context = null;
    }

    //@Override
    public void updated(String pid, Dictionary<String, ?> properties)
                    throws ConfigurationException
    {
        System.out.println("Updating configuration properties:" + properties);
        String id = (String) properties.get("id");
        _properties = cloneDictionary(properties);
        System.out.println("pid:" + pid + " id:" + id);

        if (_context != null) {
            System.out.println("register User Identity Resolver service upon pid:" + pid);
            Dictionary<String, Object> serviceProps = cloneDictionary(properties);
            String vendor = (String) serviceProps.get("service.vendor");
            if (vendor == null || vendor.isEmpty()) {
                vendor = "IBM";
            }
            serviceProps.put("service.vendor", vendor);
            // Lets registered the service
            UserCredentialResolver idService = new OAuth20TokenMappingResolver(serviceProps);
            ServiceRegistration<UserCredentialResolver> sr = _context.registerService(UserCredentialResolver.class,
                                                                                      idService, serviceProps);
            userIdentityResolverRef.put(pid, sr);
        }
    }

    /** {@inheritDoc} */
    //@Override
    public void deleted(String pid) {
        // we should
        //   1) unregister the service
        //   2) remove the resources in this method
        //   3) close or remove the resources in use 
        System.out.println("ManagedServiceFactory: deleted() pid:" + pid + " unregister it");
        ServiceRegistration<UserCredentialResolver> sr = userIdentityResolverRef.get(pid);
        sr.unregister();
        userIdentityResolverRef.remove(pid);
    }

    /** {@inheritDoc} */
    //@Override
    public String getName() {
        // return the name of this fatcory
        System.out.println("ManagedServiceFactory: getName():");
        return _cfg_pid;
    }

    public Hashtable<String, Object> cloneDictionary(Dictionary<String, ?> original) {
        Enumeration<String> keys = original.keys();
        Hashtable<String, Object> result = new Hashtable<String, Object>();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            result.put(key, original.get(key));
        }
        return result;
    }
}
