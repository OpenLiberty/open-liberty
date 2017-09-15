/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.servicesmanager;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * There are various parts of the batch runtime that need to obtain refs
 * to batch OSGi services statically, outside of the normal OSGI service
 * component lifecycle and dependency management mechanisms.
 * 
 * For example, BatchWorkUnit is a Runnable that gets scheduled via Executors.
 * Once it's running it needs access to various batch service components.
 * We could manually "inject" the needed services into the BatchWorkUnit 
 * (pass them into the CTOR, e.g).  However there's no guarantee the work will
 * run any time soon, and it's unsafe to hold onto service refs for a long
 * time since they may get deactivated or replaced. 
 * 
 * There's also the possibility in the future that work will be scheduled 
 * via persistent schedulers, or possibly via (hypothetical) "distributed" schedulers
 * that schedule the work to other JVMs (think - the batch manager scheduling
 * to another endpoint within the collective).  These scenarios further complicate
 * the matter.
 * 
 * So while good design would have us avoid static at all costs, in this case
 * we're kind of stuck with it for now (barring significant refactoring of code). 
 * 
 */
public class ServicesManagerStaticAnchor {

    /**
     * Static instance of the ServicesManager service object.
     */
    private static ServicesManager servicesManagerStaticInstance;
    
    /**
     * ServiceTracker singleton tracking the ServicesManager.
     */
    private static volatile ServiceTracker<ServicesManager, ServicesManager> serviceTrackerSingleton;

    /**
     * @return The BundleContext for the ServicesManager class.
     */
    private static BundleContext getBundleContext() {
        return FrameworkUtil.getBundle(ServicesManager.class).getBundleContext();
    }

    /**
     * @return a new, opened ServiceTracker for ServicesManager.
     */
    private static synchronized ServiceTracker<ServicesManager, ServicesManager> openServiceTracker() {
        ServiceTracker<ServicesManager, ServicesManager> retMe = 
                    new ServiceTracker<ServicesManager, ServicesManager>(getBundleContext(), 
                                                                         ServicesManager.class, 
                                                                         new MyServiceTrackerCustomizer());
        retMe.open();
        return retMe;
    }
    
    /**
     * @return a lazy-init'ed static ServiceTracker instance.
     */
    private static synchronized ServiceTracker<ServicesManager, ServicesManager> getServiceTracker() {
        if (serviceTrackerSingleton == null) {
            serviceTrackerSingleton = openServiceTracker();
        }
        return serviceTrackerSingleton;
    }
        
    /**
     * Static "injection" point.  Tests may use this method to set a mocked ServicesManager.
     */
    public static void setServicesManager(ServicesManager servicesManager) {
        servicesManagerStaticInstance = servicesManager;
    }
    
    /**
     * @return the ServicesManager service, from which all other services can be retrieved.
     */
    public static ServicesManager getServicesManager() {
        // Set a localRef to try to avoid timing windows and NPEs since the static instance 
        // may be nulled out at any time.
        ServicesManager localRef = servicesManagerStaticInstance;
        if (localRef == null) {
            localRef = servicesManagerStaticInstance = getServiceTracker().getService();
        }
        return localRef;
    }
    
    /**
     * The customizer listens for adding/removing service events and updates
     * the ServicesManager static instance.
     */
    private static class MyServiceTrackerCustomizer implements ServiceTrackerCustomizer<ServicesManager, ServicesManager> {

        /**
         * Update the static instance.
         */
        @Override
        public ServicesManager addingService(ServiceReference<ServicesManager> ref) {
            ServicesManager retMe =  getBundleContext().getService(ref);
            setServicesManager(retMe);
            return retMe;
        }

        @Override
        public void modifiedService(ServiceReference ref, ServicesManager serviceObject) {
            setServicesManager(serviceObject);
        }

        /**
         * The ServicesManager ref is being deactivated.
         */
        @Override
        public void removedService(ServiceReference ref, ServicesManager serviceObject) {
            // DO NOT null out the ServicesManager ref.  This method is called prior to
            // ServicesManagerImpl.deactivate.  Under deactivate, the batch kernel is shutdown
            // and stop requests are sent to all active jobs.  As the jobs shutdown they
            // will require services via ServicesManagerImpl, and in many places they get
            // a ref to the ServicesManager via this static anchor. So if we null it out
            // here, before deactivate, those jobs will suffer NPEs.
            // setServicesManager(null);
        }
    }
}

