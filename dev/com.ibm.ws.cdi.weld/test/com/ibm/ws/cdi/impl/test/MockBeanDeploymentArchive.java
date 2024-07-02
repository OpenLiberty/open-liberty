package com.ibm.ws.cdi.impl.test;

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.impl.weld.AbstractBeanDeploymentArchive;
import com.ibm.ws.cdi.internal.interfaces.ArchiveType;
import com.ibm.ws.cdi.internal.interfaces.CDIArchive;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.ManagedBeanDescriptor;
import com.ibm.ws.cdi.internal.interfaces.WebSphereBeanDeploymentArchive;
import com.ibm.ws.cdi.internal.interfaces.WebSphereCDIDeployment;
import com.ibm.wsspi.injectionengine.ReferenceContext;

import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.ejb.spi.EjbDescriptor;

import junit.framework.Assert;

public class MockBeanDeploymentArchive extends AbstractBeanDeploymentArchive {

    private static AtomicInteger orderScannedCounter = new AtomicInteger(1);
    Set<WebSphereBeanDeploymentArchive> descendetBDAs = new HashSet<WebSphereBeanDeploymentArchive>();
    private int orderScanned = -1;

    String id;
    final CDIArchive archive;
    private final int acceptableFloor;
    private final int acceptableCeiling;

    public MockBeanDeploymentArchive(ArchiveType type, int acceptableFloor, int acceptableCeiling, String id) {
        archive = new MockCDIArchive(type);
        this.acceptableFloor = acceptableFloor;
        this.acceptableCeiling = acceptableCeiling;
        this.id = id + ":" + type.toString();
    }

    public boolean isScanned() {
        return orderScanned != -1;
    }

    public int getOrderScanned() {
        return orderScanned;
    }

    public int getOrderAcceptibleCeiling() {
        return acceptableCeiling;
    }

    public int getOrderAcceptibleFloor() {
        return acceptableFloor;
    }

    public void scannedAfter(MockBeanDeploymentArchive other) {
        Assert.assertTrue("archve: + " + id + " was scanned after " + other.getId(), orderScanned > other.getOrderScanned());
    }

    @Override
    public void scan() throws CDIException {
        orderScanned = orderScannedCounter.getAndIncrement();
    }

    @Override
    public CDIArchive getArchive() {
        return archive;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    protected String getHumanReadableName() {
        return toString(); //not used in unit test, jutst keeps the compiler happy.
    }

    @Override
    protected Set<WebSphereBeanDeploymentArchive> getDescendentBDAs() {
        return descendetBDAs;
    }

    @Override
    public void addDescendantBda(WebSphereBeanDeploymentArchive descendantBda) {
        descendetBDAs.add(descendantBda);

    }

    // Everything below is not needed for the unit test

    @Override
    public void addBeanDeploymentArchive(WebSphereBeanDeploymentArchive accessibleBDA) {

    }

    @Override
    public void addEjbDescriptor(EjbDescriptor<?> ejbDescriptor) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addEjbDescriptors(Collection<EjbDescriptor<?>> ejbDescriptors) {
        // TODO Auto-generated method stub

    }

    @Override
    public WebSphereCDIDeployment getCDIDeployment() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addToBeanClazzes(Class<?> clazz) {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<String> getAllClazzes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BeanManager getBeanManager() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<WebSphereBeanDeploymentArchive> getWebSphereBeanDeploymentArchives() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<WebSphereBeanDeploymentArchive> getDescendantBdas() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CDIRuntime getCDIRuntime() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ArchiveType getType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasBeans() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ReferenceContext initializeInjectionServices() throws CDIException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Class<?>> getInjectionClasses() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<String> scanForBeanDefiningAnnotations(boolean scanChildren) throws CDIException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean extensionCanSeeApplicationBDAs() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void createInjectionTargetsForJEEComponentClasses() throws CDIException {
        // TODO Auto-generated method stub

    }

    @Override
    public void createInjectionTargetsForJEEComponentClass(Class<?> clazz) throws CDIException {
        // TODO Auto-generated method stub

    }

    @Override
    public <T> List<InjectionPoint> getJEEComponentInjectionPoints(Class<T> clazz) throws CDIException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> InjectionTarget<T> getJEEComponentInjectionTarget(Class<T> clazz) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> void addJEEComponentInjectionTarget(Class<T> clazz, InjectionTarget<T> injectionTarget) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean containsBeanClass(Class<?> beanClass) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasBeenScanned() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Set<Class<?>> getJEEComponentClasses() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addManagedBeanDescriptor(ManagedBeanDescriptor<?> managedBeanDescriptor) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addManagedBeanDescriptors(Collection<ManagedBeanDescriptor<?>> managedBeanDescriptors) {
        // TODO Auto-generated method stub

    }

    @Override
    public ClassLoader getClassLoader() throws CDIException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isExtension() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Set<EjbDescriptor<?>> getEjbDescriptor(Class<?> clazz) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getEEModuleDescriptorId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Supplier<Extension>> getSPIExtensionSuppliers() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setSPIExtensionSuppliers(Set<Supplier<Extension>> spiExtensionSuppliers) {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<String> getBuildCompatibleExtensionClassNames() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public URL getBeansXmlResourceURL() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<String> getBeanClasses() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<BeanDeploymentArchive> getBeanDeploymentArchives() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BeansXml getBeansXml() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<EjbDescriptor<?>> getEjbs() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceRegistry getServices() {
        // TODO Auto-generated method stub
        return null;
    }

}
