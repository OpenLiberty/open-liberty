/*******************************************************************************
 * Copyright (c) 2007, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.naming.NameAlreadyBoundException;
import javax.naming.Reference;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.util.AmbiguousEJBRefReferenceFactory;

/**
 * Provides the means to the EJBContainer for generating JNDI binding names. <p>
 *
 * Two instances will be created per EJB (HomeRecord), one for the 'ejblocal:'
 * context, and one for the global (remote) context.
 **/
public class BindingsHelper {
    private static final TraceComponent tc = Tr.register(BindingsHelper.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    /**
     * Server wide map of binding name to a list of EJBs mapped using that
     * name in the global name space (remote context).
     *
     * Every remote interface bound will be in this map.
     **/
    static final HashMap<String, BindingData> cvAllRemoteBindings = new HashMap<String, BindingData>(); // d457053.1

    /**
     * Server wide map of binding name to a list of EJBs mapped using that
     * name in the local name space (ejblocal: context).
     *
     * Every local interface bound will be in this map.
     **/
    static final HashMap<String, BindingData> cvAllLocalBindings = new HashMap<String, BindingData>(); // d457053.1

    /**
     * Per home record list of ejblocal: bindings
     */
    public final List<String> ivEJBLocalBindings = new ArrayList<String>();

    /**
     * Per home record list of local: bindings
     */
    public final List<String> ivLocalColonBindings = new ArrayList<String>();

    /**
     * Per home record list of remote bindings
     */
    public final List<String> ivRemoteBindings = new ArrayList<String>();

    /**
     * HomeRecord this BindingsHelper is associated with.
     **/
    private final HomeRecord ivHomeRecord;

    /**
     * Map of binding name to J2EEName for all bindings for the context
     * (Local/Remote) for the entire server process.
     *
     * This is a reference to one of the context specific structures,
     * either cvAllRemoteBindings or cvAllLocalBindings.
     **/
    private final HashMap<String, BindingData> ivServerContextBindingMap;

    /**
     * Map of interface to binding name for all bindings for the context
     * (Local/Remote) for this specific EJB.
     **/
    private final HashMap<String, String> ivEjbContextBindingMap;

    /**
     * Set of Local or Remote Short-Form Default JNDI Names bound for this
     * specific EJB.
     **/
    private final HashSet<String> ivEjbContextShortDefaultJndiNames;

    /**
     * Map of binding name to Reference for an AmbiguousEJBException for all
     * bindings that are ambiugous for this specific EJB.
     **/
    private final HashMap<String, Reference> ivEjbContextAmbiguousMap;

    /**
     * Prefix unique to the context (Local or Remote).
     **/
    private final String ivContextPrefix;

    /**
     * Cached Long form default binding prefix, which consists of the Location
     * prefix, then either the component-id or J2EEName (with / instead of #)
     * followed by a #. Will be null until first use.
     **/
    private String ivDefaultJNDIPrefix = null;

    /**
     * Constructs a BindingsHelper for either the local or remote naming context.
     *
     * @param homeRecord              the EJB to generate bindings for.
     * @param serverContextBindingMap the local or remote naming context map.
     * @param contextPrefix           the context specific default binding prefix
     **/
    private BindingsHelper(HomeRecord homeRecord,
                           HashMap<String, BindingData> serverContextBindingMap,
                           String contextPrefix) {
        ivHomeRecord = homeRecord;
        ivServerContextBindingMap = serverContextBindingMap;
        ivEjbContextBindingMap = new HashMap<String, String>();
        ivEjbContextShortDefaultJndiNames = new HashSet<String>();
        ivEjbContextAmbiguousMap = new HashMap<String, Reference>();
        ivContextPrefix = contextPrefix;
    }

    /**
     * Add a binding entry to the map of interface to binding names for all
     * bindings for the context (Local/Remote) for this specific EJB.
     *
     * @param interfaceName       is the name of the EJB local/remote interface.
     * @param specificBindingName is the JNDI name used when local/remote interface
     *                                is bound into the naming context.
     *
     * @throws NameAlreadyBoundException if the binding name being added
     *                                       has previously been added.
     **/
    public void addBinding(String interfaceName,
                           String specificBindingName) throws NameAlreadyBoundException // d457053.1
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "addBinding: " + interfaceName + ", " + specificBindingName);

        addToServerContextBindingMap(interfaceName, specificBindingName);
        ivEjbContextBindingMap.put(interfaceName, specificBindingName);
    }

    /**
     * Returns the binding for the specified interface, which was previously
     * added through addBinding or generateBindings.
     *
     * @param interfaceName is the name of the EJB local/remote interface.
     **/
    public String getBinding(String interfaceName) {
        return ivEjbContextBindingMap.get(interfaceName);
    }

    /**
     * Returns a collection of all binding for this contextd (Local/Remote) for
     * this specific EJB, except for the short form default bindings.
     **/
    public Collection<String> getBindings() {
        return ivEjbContextBindingMap.values();
    }

    /**
     * Removes all of the EJB bindings for this EJB from the bean specific
     * and sever wide maps.
     **/
    public void removeEjbBindings() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "removeEjbBindings called");

        // Just loop through the values, not the keys, as the simple-binding-name
        // key may have a # in front of it when the bean was not simple, and that
        // won't match what is in the ServerContextBindingMap.           d457053.1

        for (String bindingName : ivEjbContextBindingMap.values()) {
            removeFromServerContextBindingMap(bindingName, true);
        }

        ivEjbContextBindingMap.clear();
    }

    /**
     * Returns true if a short form default binding is present for the
     * specified interface.
     *
     * @param interfaceName name of the EJB interface to check for a
     *                          short form binding.
     **/
    public boolean hasShortDefaultBinding(String interfaceName) {
        return ivEjbContextShortDefaultJndiNames.contains(interfaceName);
    }

    /**
     * Returns true if a short form default binding is present for the
     * specified interface, and the current bean is currently the
     * only bean with this short form default binding, and there are
     * no explicit bindings.
     *
     * @param interfaceName name of the EJB interface to check for a
     *                          unique short form binding.
     **/
    public boolean isUniqueShortDefaultBinding(String interfaceName) {
        // If there were no explicit bindings, and only one implicit
        // binding, then it is considered uniquie.                       d457053.1
        BindingData bdata = ivServerContextBindingMap.get(interfaceName);

        if (bdata != null &&
            bdata.ivExplicitBean == null &&
            bdata.ivImplicitBeans != null &&
            bdata.ivImplicitBeans.size() == 1) {
            return true;
        }

        return false;
    }

    /**
     * Returns a collection of all short form default binding for this
     * contextd (Local/Remote) for this specific EJB.
     **/
    public Collection<String> getShortDefaultBindings() {
        return ivEjbContextShortDefaultJndiNames;
    }

    /**
     * Removes all of the short form default bindings for this EJB from
     * the bean specific and sever wide maps.
     **/
    public void removeShortDefaultBindings() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "removeShortDefaultBindings called");

        for (String bindingName : ivEjbContextShortDefaultJndiNames) {
            removeFromServerContextBindingMap(bindingName, false);
        }

        ivEjbContextShortDefaultJndiNames.clear();
    }

    /**
     * Returns the map of ambiguous references that was built during
     * generateBindings.
     **/
    public HashMap<String, Reference> getAmbiguousReferenceMap() {
        return ivEjbContextAmbiguousMap;
    }

    /**
     * This is main method utilized by the EJBContainer for picking and
     * generating EJB Binding Names. <p>
     *
     * The algortithm is:
     *
     * <ul>
     * <li> If a non-null specificBindingName is passed, it will be used.
     * <li> If a null specificBindingName is specified and a non-null
     * simpleBindingName is found, the simpleBindName is used.
     * <li> If neither a specificBindingName or simpleBindingName is specified
     * (both null), then Short and Long form Default Names are generated.
     * <li> If both the Short and Long form Default Names are generated, then
     * it is required that the Long form occupy the first entry in the
     * ArrayList.
     * </ul>
     *
     * It is a configuration error to specify both a specificBindingName and a
     * simpleBindingName. This condition is detected in the EJBMDOrchestrator
     * and this method assumes to never be called with this case.
     *
     * @param interfaceName                           A String representing the interface class name.
     * @param specificBindingName                     A String representing a specific-binding-name,
     *                                                    null means no specific binding name specified.
     * @param generateDisambiguatedSimpleBindingNames A boolean, which when true
     *                                                    will cause any generated simple binding names to be
     *                                                    constructed to include "#<interfaceName>" at the end
     *                                                    of the binding name.
     *
     * @return an ArrayList of Strings representing the names to be used by the
     *         EJBContainer for binding into the jndiNameSpace.
     *
     * @throws NameAlreadyBoundException if the binding name being added
     *                                       has previously been added.
     **/
    public ArrayList<String> generateBindings(String interfaceName,
                                              String specificBindingName,
                                              boolean generateDisambiguatedSimpleBindingNames) throws NameAlreadyBoundException // d457053.1
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "generateBindings: " + interfaceName + ", " +
                         specificBindingName + ", " + generateDisambiguatedSimpleBindingNames);

        // Module version is a 3.0 or later.
        ArrayList<String> jndiNamesToBind;

        if (specificBindingName == null) {
            // The simple-binding-name is obtained from the BeanMetaData and
            // is allowed to be null when default binding is desired.
            String simpleBindingName = ivHomeRecord.getBeanMetaData().simpleJndiBindingName;
            if (simpleBindingName == null) {
                // Since neither a specific or simple binding name was specifed
                // generate the Short and Long form Default bindings.
                jndiNamesToBind = generateDefaultEjbBindings(interfaceName);
            } else {
                // We have a simple binding name.
                // In the case where ambigous simple binding names are possible
                // (for instance multiple busineess interfaces or presence of
                // homes and business interfaces), disambiguate them by appending
                // the interfaceName to the end of the binding name.
                if (generateDisambiguatedSimpleBindingNames) {
                    // Before disambiguating by adding #<interface>, create an
                    // AmbiguousEJBReferenceException for the ambiguous form
                    // of the simple binding name, and add it to the list to be
                    // bound later for this EJB. Also add to the EJB Context
                    // map, prepending # to the interface name, so that it will
                    // be unbound during uninstall app.
                    // Only add the AmbiguousReference for the first interface
                    // processed for this simple-binding-name.              d457053.1
                    Reference ambiguousRef = ivEjbContextAmbiguousMap.get(simpleBindingName);
                    if (ambiguousRef == null) {
                        addAmbiguousSimpleBindingName(simpleBindingName);
                        addToServerContextBindingMap(interfaceName, simpleBindingName);
                        ivEjbContextBindingMap.put("#" + interfaceName, simpleBindingName);
                    }

                    StringBuilder sb = new StringBuilder(simpleBindingName);
                    sb.append("#").append(interfaceName);
                    simpleBindingName = sb.toString();
                }

                // Add the name to the list to return, the map for this ejb in the
                // correct context (local/remote) and the server wide map.
                jndiNamesToBind = new ArrayList<String>(1);
                jndiNamesToBind.add(simpleBindingName);
                addToServerContextBindingMap(interfaceName, simpleBindingName);
                ivEjbContextBindingMap.put(interfaceName, simpleBindingName);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "using simple binding : " + simpleBindingName);
            }
        } else {
            // A specific binding name has been specified, use it.
            // Add the name to the list to return, the map for this ejb in the
            // correct context (local/remote) and the server wide map.
            jndiNamesToBind = new ArrayList<String>(1);
            jndiNamesToBind.add(specificBindingName);
            addToServerContextBindingMap(interfaceName, specificBindingName);
            ivEjbContextBindingMap.put(interfaceName, specificBindingName);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "using specific bindings : " + specificBindingName);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "generateBindings: " + jndiNamesToBind);

        return jndiNamesToBind;
    }

    /**
     * Generate default JNDI names that allow a JNDI lookup of a remote home or
     * remote business interface of an EJB in a module version >= 3.0. <p>
     *
     * The returned list of default JNDI names will normally contain 2 entries.
     * (It is critical that this order be maintained since it will be assumed
     * that the first entry in the ArrayList will be disambiguous):
     *
     * 1. The long default (always the first entry in the returned array): <p>
     *
     * <default_jndi_prefix>#<interface_name> <p>
     *
     * <pre>
     * where <default_jndi_prefix> can be:
     *
     * <location><app_name>/<module_file_name>/<bean_name>
     *
     * <location> is 'ejb/' for remote, and nothing for local
     * <app_name> is the name of the JavaEE5 application
     * <module_file_name> is the full path name of the module file,
     * starting at the root within the .ear file
     * <bean_name> is the name of the EJB3 component
     *
     * or
     *
     * An override as specified by the container-id field in the EJB3 binding file.
     *
     * <interface_name> is the fully-qualified Java interface name.
     * </pre>
     *
     * <p>
     *
     * 2. The short default (generation of this may be disabled via property): <p>
     *
     * <interface_name> <p>
     *
     * Another way to think about this is that it's the J2EEName of the bean (with
     * forward slashes substituted for any octothorpes), followed by a "#",
     * followed by the <interface_name> as defined above. <p>
     *
     * @param interfaceName is the fully qualified name of an interface of the component.
     *
     * @return defaultJNDINames in the returned ArrayList
     *
     * @throws NameAlreadyBoundException if the binding name being added
     *                                       has previously been added.
     **/
    private ArrayList<String> generateDefaultEjbBindings(String interfaceName) throws NameAlreadyBoundException // d457053.1
    {
        ArrayList<String> defaultJNDINames = new ArrayList<String>(2);
        StringBuilder sb = new StringBuilder(256);

        if (ivDefaultJNDIPrefix == null) {
            // Add the context specific (local/remote) default binding prefix first.
            if (ivContextPrefix != null)
                sb.append(ivContextPrefix);

            // Then add the component-id or j2eename
            if (ivHomeRecord.bmd.ivComponent_Id != null) { // d445912
                sb.append(ivHomeRecord.bmd.ivComponent_Id).append("#"); // d445912
            } else {
                sb.append(ivHomeRecord.j2eeName.getApplication()).append("/");
                sb.append(ivHomeRecord.j2eeName.getModule()).append("/");
                sb.append(ivHomeRecord.j2eeName.getComponent()).append("#"); // d443702
            }

            // cache away the string we just built up so we can resuse it again the next time
            ivDefaultJNDIPrefix = sb.toString();
        } else {
            // Use the previously cached prefix
            sb.append(ivDefaultJNDIPrefix);
        }

        sb.append(interfaceName); // d443702

        String longBindingName = sb.toString();

        // Add to the list of jndiNames to be returned
        defaultJNDINames.add(longBindingName);
        addToServerContextBindingMap(interfaceName, longBindingName);
        ivEjbContextBindingMap.put(interfaceName, longBindingName);

        // Add the short default (convienence) bindings if not explicitly disabled.
        if (ivHomeRecord.shortDefaultBindingsEnabled()) {
            // Determine if this interface (short binding) has already been bound
            // by another EJB.
            BindingData bdata = ivServerContextBindingMap.get(interfaceName);

            // For both cases, add to the ShortDefaultName map so that it can
            // be unbound during application stop, and to the server wide
            // context map (so other ambiguous references may be found).
            addToServerContextBindingMap(interfaceName);
            ivEjbContextShortDefaultJndiNames.add(interfaceName);

            if (bdata == null) {
                // Has not been bound yet, so just bind this EJB.
                defaultJNDINames.add(interfaceName);
            } else if (bdata.ivExplicitBean == null) // d457053.1
            {
                // Has already been bound, so add an AmbiguousEJBReference to
                // the ambiguous map, which will be bound later.
                addAmbiguousShortDefaultBindingName(interfaceName);
            } else {
                // There is an explicit binding, so just ignore this
                // short-form default (convenience) binding.               d457053.1
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "generateDefaultEjbBindings: " + interfaceName +
                                 " short-form default overridden : " + bdata.ivExplicitBean);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "generateDefaultEjbBindings: " + defaultJNDINames);

        return defaultJNDINames;
    }

    /**
     * Creates a Reference for an AmbiguousEJBReferenceException with detail
     * information about the simple binding name problem, and adds
     * it to the list of AmbiguousEJBReferences to be bound into naming. <p>
     *
     * @param simpleBindingName the simple-binding-name that is ambiguous.
     **/
    private void addAmbiguousSimpleBindingName(String simpleBindingName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "adding AmbiguousEJBReference for simple-binding-name '" +
                         simpleBindingName + "', for EJB " + ivHomeRecord.getJ2EEName());

        Reference ambiguousReference = AmbiguousEJBRefReferenceFactory.createAmbiguousReference(simpleBindingName,
                                                                                                ivHomeRecord.getJ2EEName());

        ivEjbContextAmbiguousMap.put(simpleBindingName, ambiguousReference);
    }

    /**
     * Creates a Reference for an AmbiguousEJBReferenceException with detail
     * information about the short form default binding confilicts, and adds
     * it to the list of AmbiguousEJBReferences to be bound into naming. <p>
     *
     * @param shortBindingName the short form default binding name that is ambiguous.
     **/
    private void addAmbiguousShortDefaultBindingName(String shortBindingName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "adding AmbiguousEJBReference for short default binding name '" +
                         shortBindingName + "', for EJB " + ivHomeRecord.getJ2EEName());

        // Get the list of beans that implement the same interface.      d457053.1
        ArrayList<J2EEName> beans = null;
        BindingData bdata = ivServerContextBindingMap.get(shortBindingName);
        if (bdata != null) {
            beans = bdata.ivImplicitBeans;
        }

        Reference ambiguousReference = AmbiguousEJBRefReferenceFactory.createAmbiguousReference(shortBindingName, beans);

        ivEjbContextAmbiguousMap.put(shortBindingName, ambiguousReference);
    }

    /**
     * Internal method to add an explicit binding name entry into the server
     * wide map of all binding names for the context associated with this
     * helper (i.e. Local or Remote context).
     *
     * @param interfaceName is the name of the EJB local/remote interface.
     * @param bindingName   is the JNDI name used when local/remote interface
     *                          is bound into the naming context.
     *
     * @throws NameAlreadyBoundException if the binding name being added
     *                                       has previously been added.
     **/
    // d457053.1
    private void addToServerContextBindingMap(String interfaceName,
                                              String bindingName) throws NameAlreadyBoundException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "addToServerContextBindingMap : " + interfaceName +
                         ", binding : " + bindingName);

        BindingData bdata = ivServerContextBindingMap.get(bindingName);

        if (bdata == null) {
            bdata = new BindingData();
            ivServerContextBindingMap.put(bindingName, bdata);
        }

        if (bdata.ivExplicitBean == null) {
            bdata.ivExplicitBean = ivHomeRecord.j2eeName;
            bdata.ivExplicitInterface = interfaceName;

            if (bdata.ivImplicitBeans != null &&
                bdata.ivImplicitBeans.size() > 1) {
                ivEjbContextAmbiguousMap.remove(interfaceName);
            }
        } else {
            J2EEName j2eeName = ivHomeRecord.j2eeName;
            if (bdata.ivExplicitBean.equals(j2eeName)) {
                Tr.error(tc, "NAME_ALREADY_BOUND_FOR_SAME_EJB_CNTR0173E",
                         new Object[] { interfaceName,
                                        j2eeName.getComponent(),
                                        j2eeName.getModule(),
                                        j2eeName.getApplication(),
                                        bindingName,
                                        bdata.ivExplicitInterface }); // d479669
                String message = "The " + interfaceName + " interface of the " +
                                 j2eeName.getComponent() + " bean in the " +
                                 j2eeName.getModule() + " module of the " +
                                 j2eeName.getApplication() + " application " +
                                 "cannot be bound to the " + bindingName + " name location. " +
                                 "The " + bdata.ivExplicitInterface + " interface of the " +
                                 "same bean has already been bound to the " + bindingName +
                                 " name location.";
                throw new NameAlreadyBoundException(message);
            } else {
                Tr.error(tc, "NAME_ALREADY_BOUND_FOR_EJB_CNTR0172E",
                         new Object[] { interfaceName,
                                        j2eeName.getComponent(),
                                        j2eeName.getModule(),
                                        j2eeName.getApplication(),
                                        bindingName,
                                        bdata.ivExplicitInterface,
                                        bdata.ivExplicitBean.getComponent(),
                                        bdata.ivExplicitBean.getModule(),
                                        bdata.ivExplicitBean.getApplication() }); // d479669
                String message = "The " + interfaceName + " interface of the " +
                                 j2eeName.getComponent() + " bean in the " +
                                 j2eeName.getModule() + " module of the " +
                                 j2eeName.getApplication() + " application " +
                                 "cannot be bound to the " + bindingName + " name location. " +
                                 "The " + bdata.ivExplicitInterface + " interface of the " +
                                 bdata.ivExplicitBean.getComponent() + " bean in the " +
                                 bdata.ivExplicitBean.getModule() + " module of the " +
                                 bdata.ivExplicitBean.getApplication() + " application " +
                                 "has already been bound to the " + bindingName + " name location.";
                throw new NameAlreadyBoundException(message);
            }
        }
    }

    /**
     * Internal method to add an implicit binding name entry (short-form default)
     * into the server wide map of all binding names for the context associated
     * with this helper (i.e. Local or Remote context).
     *
     * @param interfaceName name of the bean interface, which is also the
     *                          short-form binding name
     **/
    private void addToServerContextBindingMap(String interfaceName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "addToServerContextBindingMap : " + interfaceName +
                         " (implicit)");

        // Re-wrote method to only add short-form default (implicit).    d457053.1

        BindingData bdata = ivServerContextBindingMap.get(interfaceName);

        if (bdata == null) {
            bdata = new BindingData();
            ivServerContextBindingMap.put(interfaceName, bdata);
        }

        ArrayList<J2EEName> beans = bdata.ivImplicitBeans;

        if (beans != null) {
            beans.add(ivHomeRecord.j2eeName);
        } else {
            beans = new ArrayList<J2EEName>(1);
            beans.add(ivHomeRecord.j2eeName);
            bdata.ivImplicitBeans = beans;
        }
    }

    /**
     * Internal method to add a binding name entry into the server wide map
     * of all binding names for the context associated with this helper
     * (i.e. Local or Remote context).
     **/
    private void removeFromServerContextBindingMap(String bindingName,
                                                   boolean explicit) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "removeFromServerContextBindingMap : " + bindingName +
                         ", explicit : " + explicit);

        // Re-wrote method to account for explict and implicit bindings. d457053.1

        BindingData bdata = ivServerContextBindingMap.get(bindingName);

        if (bdata != null) {
            if (explicit) {
                if (ivHomeRecord.j2eeName.equals(bdata.ivExplicitBean)) {
                    bdata.ivExplicitBean = null;
                    bdata.ivExplicitInterface = null;
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "removeFromServerContextBindingMap : " +
                                     "ERROR : explicit binding for different bean : " +
                                     bdata.ivExplicitBean);
                }
            } else {
                if (bdata.ivImplicitBeans != null) {
                    bdata.ivImplicitBeans.remove(ivHomeRecord.j2eeName);

                    if (bdata.ivImplicitBeans.size() == 0) {
                        bdata.ivImplicitBeans = null;
                    }
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "removeFromServerContextBindingMap : " +
                                     "ERROR : no implicit binding");
                }
            }

            if (bdata.ivExplicitBean == null &&
                bdata.ivImplicitBeans == null) {
                ivServerContextBindingMap.remove(bindingName);
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "removeFromServerContextBindingMap : " +
                             "ERROR : no binding data");
        }
    }

    /**
     * Provides a mechanism for reading the system property for disabling short
     * form default bindings. <p>
     *
     * com.ibm.websphere.ejbcontainer.disableShortDefaultBindings <p>
     *
     * This property can be used to identify applications for which Short form
     * default jndi bindings are to be disabled.
     *
     * @param appName A string representing the applicationName we are to be
     *                    checking to see if ShortDefaultBindings are disabled for.
     * @return true if short default interface bindings are enabled, and false
     *         if the property indicated they were to be disabled for
     *         the specified application name.
     **/
    public static Boolean shortDefaultBindingsEnabled(String appName) {
        if (ContainerProperties.DisableShortDefaultBindings != null) {
            if (ContainerProperties.DisableShortDefaultBindings.size() == 0) {
                // The user specified a "*" which means simple bindings are
                // disabled for all applications.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Short form default binding disabled for application : " + appName);
                return Boolean.FALSE;
            } else {
                for (String disabledApp : ContainerProperties.DisableShortDefaultBindings) {
                    if (appName.equalsIgnoreCase(disabledApp)) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Short form default binding explicity disabled for application : " + appName);
                        return Boolean.FALSE;
                    }
                }
            }
        }

        // Default is Short Form Default Bindings are Enabled
        return Boolean.TRUE;
    }

    /**
     * A method for obtaining a Binding Name Helper for use with the local jndi namespace.
     *
     * @param homeRecord The HomeRecord associated with the bean whose interfaces or home are to
     *                       have jndi binding name(s) constructed.
     * @return An instance of a BindingsHelper for generating jndi names intended to be bound into
     *         the local jndi namespace.
     */
    public static BindingsHelper getLocalHelper(HomeRecord homeRecord) {
        if (homeRecord.ivLocalBindingsHelper == null) {
            homeRecord.ivLocalBindingsHelper = new BindingsHelper(homeRecord, cvAllLocalBindings, null);
        }
        return homeRecord.ivLocalBindingsHelper;
    }

    /**
     * A method for obtaining a Binding Name Helper for use with the remote jndi namespace.
     *
     * @param homeRecord The HomeRecord associated with the bean whose interfaces or home are to
     *                       have jndi binding name(s) constructed.
     * @return an instance of a BindingsHelper for generating jndi names intended to be bound into
     *         the remote jndi namespace.
     **/
    public static BindingsHelper getRemoteHelper(HomeRecord homeRecord) {
        if (homeRecord.ivRemoteBindingsHelper == null) {
            homeRecord.ivRemoteBindingsHelper = new BindingsHelper(homeRecord, cvAllRemoteBindings, "ejb/");
        }
        return homeRecord.ivRemoteBindingsHelper;
    }
}
