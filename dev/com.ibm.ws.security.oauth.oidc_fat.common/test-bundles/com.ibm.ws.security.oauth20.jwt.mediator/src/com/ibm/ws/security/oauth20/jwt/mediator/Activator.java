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
package com.ibm.ws.security.oauth20.jwt.mediator;

import java.rmi.RemoteException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

import com.ibm.wsspi.security.oauth20.JwtAccessTokenMediator;

public class Activator implements BundleActivator/*, ManagedServiceFactory*/ {
    private ServiceRegistration<JwtAccessTokenMediator> serviceConfigRef;

    String _cfg_pid = "OAuth20JwtMediator";
   
    BundleContext _context = null;
    Hashtable<String, ServiceRegistration<JwtAccessTokenMediator>> jwtMediatorServiceRef =
                    new Hashtable<String, ServiceRegistration<JwtAccessTokenMediator>>(10, 5);

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
        System.out.println("JWT mediator service starts:" + context.getClass().getName());
        _context = context;
        OAuth20JwtMediator tokenMapper = new OAuth20JwtMediator(getDefaults());
        serviceConfigRef = _context.registerService(JwtAccessTokenMediator.class, tokenMapper, getDefaults());
        //serviceConfigRef = context.registerService(ManagedServiceFactory.class, this, getDefaults());
        System.out.println("jwt mediator service (configRef):" + serviceConfigRef);
        jwtMediatorServiceRef.put(_cfg_pid, serviceConfigRef);
    }

    @Override
    public void stop(BundleContext context)
                    throws Exception
    {
        System.out.println("jwt mediator service stops");
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
            System.out.println("register JWT mediator service upon pid:" + pid);
            Dictionary<String, Object> serviceProps = cloneDictionary(properties);
            String vendor = (String) serviceProps.get("service.vendor");
            if (vendor == null || vendor.isEmpty()) {
                vendor = "IBM";
            }
            serviceProps.put("service.vendor", vendor);
            // Lets register the service
            JwtAccessTokenMediator idService = new OAuth20JwtMediator(serviceProps);
            ServiceRegistration<JwtAccessTokenMediator> sr = _context.registerService(JwtAccessTokenMediator.class,
                                                                                      idService, serviceProps);
            jwtMediatorServiceRef.put(pid, sr);
        }
    }

    /** {@inheritDoc} */
    //@Override
    public void deleted(String pid) {
        // we should
        //   1) unregister the service
        //   2) remove the resources in this method
        //   3) close or remove the resources in use 
        System.out.println("ManagedServiceFactory: deleted() pid:" + pid + " unregister the JWT mediator service");
        ServiceRegistration<JwtAccessTokenMediator> sr = jwtMediatorServiceRef.get(pid);
        sr.unregister();
        jwtMediatorServiceRef.remove(pid);
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
