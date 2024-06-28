package com.ibm.ws.cdi.impl.test;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.Extension;

import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.impl.weld.AbstractWebSphereCDIDeployment;
import com.ibm.ws.cdi.internal.interfaces.WebSphereBeanDeploymentArchive;
import com.ibm.ws.cdi.internal.interfaces.WebSphereInjectionServices;
import com.ibm.wsspi.injectionengine.ReferenceContext;

import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.Metadata;

public class MockWebSphereCDIDeployment extends AbstractWebSphereCDIDeployment {

    List<WebSphereBeanDeploymentArchive> allBDAs = new ArrayList<WebSphereBeanDeploymentArchive>();

    public MockWebSphereCDIDeployment(List<WebSphereBeanDeploymentArchive> allBDAs) {
        this.allBDAs = allBDAs;
        Collections.shuffle(allBDAs);
    }

    @Override
    protected Collection<WebSphereBeanDeploymentArchive> getAllBDAs() {
        return allBDAs;
    }

    /// Below this line is just to keep the compiler happy.

    @Override
    public WebSphereInjectionServices getInjectionServices() {
        // not needed for unit tests
        return null;
    }

    @Override
    public void setClassLoader(ClassLoader classloader) {
        // not needed for unit tests

    }

    @Override
    public ClassLoader getClassLoader() {
        // not needed for unit tests
        return null;
    }

    @Override
    public WeldBootstrap getBootstrap() {
        // not needed for unit tests
        return null;
    }

    @Override
    public String getDeploymentID() {
        // not needed for unit tests
        return null;
    }

    @Override
    public WebSphereBeanDeploymentArchive getBeanDeploymentArchive(String archiveID) {
        // not needed for unit tests
        return null;
    }

    @Override
    public WebSphereBeanDeploymentArchive getBeanDeploymentArchive(Class<?> beanClass) {
        // not needed for unit tests
        return null;
    }

    @Override
    public WebSphereBeanDeploymentArchive getBeanDeploymentArchiveFromClass(Class<?> clazz) {
        // not needed for unit tests
        return null;
    }

    @Override
    public Collection<WebSphereBeanDeploymentArchive> getApplicationBDAs() {
        // not needed for unit tests
        return null;
    }

    @Override
    public boolean isCDIEnabled() {
        // not needed for unit tests
        return false;
    }

    @Override
    public boolean isCDIEnabled(String bdaId) {
        // not needed for unit tests
        return false;
    }

    @Override
    public void addBeanDeploymentArchive(WebSphereBeanDeploymentArchive bda) throws CDIException {
        // not needed for unit tests

    }

    @Override
    public void addBeanDeploymentArchives(Set<WebSphereBeanDeploymentArchive> bdas) throws CDIException {
        // not needed for unit tests

    }

    @Override
    public void initializeInjectionServices() throws CDIException {
        // not needed for unit tests

    }

    @Override
    public void shutdown() {
        // not needed for unit tests

    }

    @Override
    public Collection<WebSphereBeanDeploymentArchive> getWebSphereBeanDeploymentArchives() {
        // not needed for unit tests
        return null;
    }

    @Override
    public void validateJEEComponentClasses() throws CDIException {
        // not needed for unit tests

    }

    @Override
    public void addReferenceContext(ReferenceContext referenceContext) {
        // not needed for unit tests

    }

    @Override
    public CDI<Object> getCDI() {
        // not needed for unit tests
        return null;
    }

    @Override
    public Collection<URL> getUnversionedBeansXmlURLs() {
        // not needed for unit tests
        return null;
    }

    @Override
    public Collection<BeanDeploymentArchive> getBeanDeploymentArchives() {
        // not needed for unit tests
        return null;
    }

    @Override
    public Iterable<Metadata<Extension>> getExtensions() {
        // not needed for unit tests
        return null;
    }

    @Override
    public ServiceRegistry getServices() {
        // not needed for unit tests
        return null;
    }

    @Override
    public BeanDeploymentArchive loadBeanDeploymentArchive(Class<?> arg0) {
        // not needed for unit tests
        return null;
    }
}
