/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.subsystem;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 *
 */
public class MockBundleContext implements BundleContext {

    Set<String> registeredServices = new HashSet<String>();

    @Override
    public String getProperty(String key) {
        return null;
    }

    @Override
    public Bundle getBundle() {
        return null;
    }

    @Override
    public Bundle installBundle(String location, InputStream input) throws BundleException {
        return null;
    }

    @Override
    public Bundle installBundle(String location) throws BundleException {
        return null;
    }

    @Override
    public Bundle getBundle(long id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle[] getBundles() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
        // TODO Auto-generated method stub

    }

    @Override
    public void addServiceListener(ServiceListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeServiceListener(ServiceListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addBundleListener(BundleListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeBundleListener(BundleListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addFrameworkListener(FrameworkListener listener) {}

    @Override
    public void removeFrameworkListener(FrameworkListener listener) {}

    @Override
    public ServiceRegistration<?> registerService(String[] clazzes, Object service, Dictionary<String, ?> properties) {
        this.registeredServices.add((String) properties.get("ibm.featureName"));
        return null;
    }

    @Override
    public ServiceRegistration<?> registerService(String clazz, Object service, Dictionary<String, ?> properties) {
        this.registeredServices.add((String) properties.get("ibm.featureName"));
        return null;
    }

    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ?> properties) {
        this.registeredServices.add((String) properties.get("ibm.featureName"));
        return null;
    }

    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> clazz, ServiceFactory<S> factory, Dictionary<String, ?> properties) {
        this.registeredServices.add((String) properties.get("ibm.featureName"));
        return null;
    }

    // Simple getter for the list of services. Probably should create serviceReferences from the generated ServiceRegistrations but 
    // for the FeatureRepository test, I just need to know which services were registered.
    public Set<String> getRegisteredServices() {
        return this.registeredServices;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleContext#getServiceReferences(java.lang.String, java.lang.String)
     */
    @Override
    public ServiceReference<?>[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleContext#getAllServiceReferences(java.lang.String, java.lang.String)
     */
    @Override
    public ServiceReference<?>[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleContext#getServiceReference(java.lang.String)
     */
    @Override
    public ServiceReference<?> getServiceReference(String clazz) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleContext#getServiceReference(java.lang.Class)
     */
    @Override
    public <S> ServiceReference<S> getServiceReference(Class<S> clazz) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleContext#getServiceReferences(java.lang.Class, java.lang.String)
     */
    @Override
    public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> clazz, String filter) throws InvalidSyntaxException {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleContext#getService(org.osgi.framework.ServiceReference)
     */
    @Override
    public <S> S getService(ServiceReference<S> reference) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleContext#ungetService(org.osgi.framework.ServiceReference)
     */
    @Override
    public boolean ungetService(ServiceReference<?> reference) {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleContext#getDataFile(java.lang.String)
     */
    @Override
    public File getDataFile(String filename) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleContext#createFilter(java.lang.String)
     */
    @Override
    public Filter createFilter(String filter) throws InvalidSyntaxException {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleContext#getBundle(java.lang.String)
     */
    @Override
    public Bundle getBundle(String location) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleContext#getServiceObjects(org.osgi.framework.ServiceReference)
     */
    @Override
    public <S> ServiceObjects<S> getServiceObjects(ServiceReference<S> reference) {
        // TODO Auto-generated method stub
        return null;
    }

}
