/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.runtime;

import java.rmi.RemoteException;
import java.util.List;

import javax.ejb.CreateException;
import javax.naming.NamingException;

import com.ibm.ejs.container.HomeRecord;
import com.ibm.websphere.csi.HomeWrapperSet;

/**
 * Handles binding and unbinding namespace names for a single module. After
 * creation, either {@link #beginBind} or {@link #beginUnbind} will be called.
 * If the begin method completes successfully, then a series of bind or unbind
 * methods may be called, and then {@link #end} must be called.
 */
public interface NameSpaceBinder<T> {
    /**
     * Prepares the binder for subsequent calls to {@link #bindJavaGlobal}, {@link #bindJavaApp}, {@link #bindJavaModule}, {@link #bindBindings}, and {@link #bindEJBFactory}.
     */
    void beginBind() throws NamingException;

    /**
     * Create the object to bind for a single interface, or returns null if
     * nothing should be bound into the namespace.
     *
     * @param hr the bean home record
     * @param homeSet the remote and local home wrappers, or <tt>null</tt> if
     *            deferred initialization bindings should be used
     * @param interfaceName the interface name to bind
     * @param interfaceIndex the interface index, or -1 if the interface is a
     *            home interface
     * @param local <tt>true</tt> if the interface to bind is a local interface
     * @return the binding object, or null if no binding should occur
     */
    T createBindingObject(HomeRecord hr,
                          HomeWrapperSet homeSet,
                          String interfaceName,
                          int interfaceIndex,
                          boolean local) throws NamingException, RemoteException, CreateException;

    /**
     * Creates an alternate binding object for java: namespaces.
     *
     * @param hr the bean home record
     * @param homeSet the remote and local home wrappers, or <tt>null</tt> if
     *            deferred initialization bindings should be used
     * @param interfaceName the interface name to bind
     * @param interfaceIndex the interface index, or -1 if the interface is a
     *            home interface
     * @param local <tt>true</tt> if the interface to bind is a local interface
     * @param isHome <tt>true</tt> if the interface is a home interface
     * @param bindingObject the result of {@link #createBindingObject}
     * @return the binding object
     */
    T createJavaBindingObject(HomeRecord hr,
                              HomeWrapperSet homeSet,
                              String interfaceName,
                              int interfaceIndex,
                              boolean local,
                              T bindingObject);

    /**
     * Binds an object to the java:global/appname/modname/ context.
     *
     * @param name the binding name without subcontexts
     * @param bindingObject the result of {@link #createJavaBindingObject}
     */
    void bindJavaGlobal(String name, T bindingObject) throws NamingException;

    /**
     * Binds an object to the java:app/modname/ context.
     *
     * @param name the binding name without subcontexts
     * @param bindingObject the result of {@link #createJavaBindingObject}
     */
    void bindJavaApp(String name, T bindingObject) throws NamingException;

    /**
     * Binds an object to the java:module/ context.
     *
     * @param name the binding name without subcontexts
     * @param bindingObject the result of {@link #createJavaBindingObject}
     */
    void bindJavaModule(String name, T bindingObject) throws NamingException;

    /**
     * Binds the specified object into naming as specified by its configured
     * bindings.
     *
     * @param bindingObject the result of {@link #createBindingObject}
     * @param hr the bean home record
     * @param numInterfaces the number of remote or local interfaces
     * @param singleGlobalInterface <tt>true</tt> if this bean has only one
     *            total interface (counting local and remote together)
     * @param interfaceIndex the interface index, or -1 if the interface is a
     *            home interface
     * @param interfaceName the interface name represented by the binding object
     * @param local <tt>true</tt> if local interfaces should be bound, or
     *            <tt>false</tt> if remote interfaces should be bound
     * @param isHome <tt>true</tt> if the interface is a home interface
     * @param deferred <tt>true</tt> if bean initialization is being deferred
     */
    void bindBindings(T bindingObject,
                      HomeRecord hr,
                      int numInterfaces,
                      boolean singleGlobalInterface,
                      int interfaceIndex,
                      String interfaceName,
                      boolean local,
                      boolean deferred) throws NamingException;

    /**
     * Bind an EJBFactory reference into the global namespace for the
     * specified application (if not already bound) and module. <p>
     */
    void bindEJBFactory() throws NamingException;

    /**
     * Prepares the binder for subsequent calls to {@link #unbindJavaGlobal}, {@link #unbindJavaApp}, {@link #unbindJavaModule}, {@link #unbindBindings}, and
     * {@link #unbindEJBFactory}.
     *
     * @param error true if the call is being made because an error occurred
     *            while starting the module
     */
    void beginUnbind(boolean error) throws NamingException;

    /**
     * Removes a set of names from the java:global/appname/modname/ context.
     * As many names should be unbound as possible, even if an error occurs.
     *
     * @param names the names without subcontexts to unbind
     */
    void unbindJavaGlobal(List<String> names) throws NamingException;

    /**
     * Removes a set of names from the java:app/modname/ context. As many names
     * should be unbound as possible, even if an error occurs.
     *
     * @param names the names without subcontexts to unbind
     */
    void unbindJavaApp(List<String> names) throws NamingException;

    /**
     * Undoes the bindings from {@link #bindBindings}.
     *
     * @param hr the bean home record
     */
    void unbindBindings(HomeRecord hr) throws NamingException;

    /**
     * Undoes the bindings from {@link #bindEJBFactory}.
     */
    void unbindEJBFactory() throws NamingException;

    /**
     * Ends the module bind or unbind. No methods may be called on this object
     * after this method has been called.
     */
    void end() throws NamingException;

    /**
     * Binds the default form of an object to the ejblocal naming context
     *
     * @param bindingObject the EJBBinding
     * @param hr the bean home record
     */
    void bindDefaultEJBLocal(T bindingObject, HomeRecord hr) throws NamingException;

    /**
     * Adds the default remote legacy bindings to root
     *
     * @param bindingObject the EJB Binding information
     * @param hr the HomeRecord of the EJB
     */
    void bindDefaultEJBRemote(T bindingObject, HomeRecord hr) throws NamingException;

    /**
     * Undoes the bindings from ejblocal namespace.
     */
    void unbindEJBLocal(List<String> names) throws NamingException;

    /**
     * Binds the simpleBindingName custom binding
     *
     * @param bindingObject - the EJBBinding
     * @param hr - the bean home record
     * @param local - if it is a local bean
     * @param generateDisambiguatedSimpleBindingNames - A boolean, which when true
     *            will cause any generated simple binding names to be
     *            constructed to include "#<interfaceName>" at the end
     *            of the binding name.
     */
    void bindSimpleBindingName(T bindingObject, HomeRecord hr, boolean local, boolean generateDisambiguatedSimpleBindingNames) throws NamingException;

    /**
     * Binds the localHomeBindingName custom binding
     *
     * @param bindingObject - the EJBBinding
     * @param hr - the bean home record
     */
    void bindLocalHomeBindingName(T bindingObject, HomeRecord hr) throws NamingException;

    /**
     * Binds the interface binding-name custom binding for local
     *
     * @param bindingObject - the EJBBinding
     * @param hr - the bean home record
     */
    void bindLocalBusinessInterface(T bindingObject, HomeRecord hr) throws NamingException;

    /**
     * Undoes the bindings from local namespace.
     */
    void unbindLocalColonEJB(List<String> names) throws NamingException;

    /**
     * Undoes the root remote bindings.
     */
    void unbindRemote(List<String> names);

}
