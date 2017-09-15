/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.injectionengine;

import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.naming.Context;

import com.ibm.ws.resource.ResourceRefConfigList;

/**
 * The <code>ReferenceContext</code> drivers the creation of reference related data structures,
 * and then stores these data structures until they are retrieved by the containers
 * that need them.
 *
 * Info objects detailing reference information specific to component(s) are added
 * to a <code>ReferenceContext</code> instance. Then, the <code>ReferenceContext</code>
 * instance is used to drive the process of resolving the reference data for these
 * one to many component level info objects. The net result of this is the generation
 * of the various reference processing data structures used by the containers to
 * support component namespace lookups, injection, EJBContext lookups, etc.
 *
 * These output data structures remain on the <code>ReferenceContext</code> and are
 * retrieved and used by the containers when needed.
 */
public interface ReferenceContext
{
    /**
     * Gets java:comp component Context.
     *
     * The <code>process</code> method must be called first to generate the Context.
     */
    public Context getJavaCompContext(); // F743-17630CodRv

    /**
     * Gets component namespace
     *
     * The <code>process</code> method must be called first to generate the namespace.
     */
    public Object getComponentNameSpace(); // F46994.3

    /**
     * Returns a map of jndi name to InjectionBinding for the 'java:comp/env'
     * name space context associated with the component. <p>
     *
     * The <code>process</code> method must be called first to generate the map.
     */
    public HashMap<String, InjectionBinding<?>> getJavaColonCompEnvMap();

    /**
     * Gets EJBContext 1.0 style data structure.
     *
     * The <code>process</code> method must be called first to generate the Properties.
     */
    public Properties getEJBContext10Properties(); // F743-17630CodRv

    // F743-17630CodRv
    /**
     * Gets list of resolved resource references.
     *
     * The <code>process</code> method must be called first to generate the list.
     *
     * @return A <code>ResRefListImpl</code> instances that contains one or more
     *         <code>ResRefImpl</code> instances, each of which represent the resolved
     *         data for a resource reference.
     */
    public ResourceRefConfigList getResolvedResourceRefs();

    /**
     * Adds a <code>ComponentNameSpaceConfiguration</code> info object to the list
     * that must get processed by the InjectionEngine.
     *
     * One or more <code>ComponentNameSpaceConfiguration</code> objects must be
     * added to the <code>ReferenceContext</code> before the <code>process</code> method is
     * called.
     *
     * @param compNameSpaceConfig
     */
    public void add(ComponentNameSpaceConfiguration compNameSpaceConfig);

    /**
     * Adds a component namespace configuration provider that will be processed
     * by the injection engine. The {@link ComponentNameSpaceConfigurationProvider#getComponentNameSpaceConfiguration} method will only be called once.
     */
    public void add(ComponentNameSpaceConfigurationProvider compNSConfigProvider);

    /**
     * Tells the InjectionEngine to process the list of
     * <code>ComponentNameSpaceConfiguration</code> instances and build the
     * various output data structures.
     *
     * This must be called after the <code>add</code> method has been used to
     * add one or more <code>ComponentNameSpaceConfiguration</code> instances,
     * and before the various get methods are called to return the output data
     * structures.
     */
    // F743-17630CodRv
    public void process()
                    throws InjectionException;

    /**
     * Process additional injection targets from annotations after {@link #process} has been called. This is primarily provided in support
     * of the ServletContext.addServlet, addFilter, and addListener methods. The
     * new injection targets will be available from {@link #getInjectionTargets},
     * but all other methods on this object are unaffected.
     */
    // F85115
    public void processDynamic(ComponentNameSpaceConfiguration compNSConfig)
                    throws InjectionException;

    /**
     * Returns true if the classes are known to the injection engine and false if not.
     * Classes not known to the injection engine can be added using processDynamic.
     */
    // F85115
    public boolean isProcessDynamicNeeded(List<Class<?>> injectionClasses);

    // F743-21481
    /**
     * Gets the <code>InjectionTarget</code> instances visible to the specified Class.
     *
     * If there are no <code>InjectionTarget</code> instances visible to the Class,
     * then an empty (non-null) list is returned.
     */
    public InjectionTarget[] getInjectionTargets(Class<?> classToInjectInto) throws InjectionException;

    /**
     * Get the set of processed injection classes.
     *
     * The <code>process</code> method must be called first to generate the set.
     */
    public Set<Class<?>> getProcessedInjectionClasses();
}
