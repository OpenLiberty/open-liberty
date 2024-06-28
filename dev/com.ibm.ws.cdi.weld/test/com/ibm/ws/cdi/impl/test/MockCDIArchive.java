package com.ibm.ws.cdi.impl.test;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.internal.interfaces.Application;
import com.ibm.ws.cdi.internal.interfaces.ArchiveType;
import com.ibm.ws.cdi.internal.interfaces.CDIArchive;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.Resource;
import com.ibm.ws.cdi.internal.interfaces.ResourceInjectionBag;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.wsspi.injectionengine.ReferenceContext;

public class MockCDIArchive implements CDIArchive {

    ArchiveType type;

    public MockCDIArchive(ArchiveType type) {
        this.type = type;
    }

    @Override
    public ArchiveType getType() {
        return type;
    }

    @Override
    public J2EEName getJ2EEName() throws CDIException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<String> getClassNames() throws CDIException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Resource getResource(String path) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isModule() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Application getApplication() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getClientModuleMainClass() throws CDIException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getInjectionClassList() throws CDIException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MetaData getMetaData() throws CDIException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResourceInjectionBag getAllBindings() throws CDIException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getClientAppCallbackHandlerName() throws CDIException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPath() throws CDIException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<CDIArchive> getModuleLibraryArchives() throws CDIException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<String> getBeanDefiningAnnotations() throws CDIException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<String> getAnnotatedClasses(Set<String> annotations) throws CDIException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Resource getBeansXml() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<String> getExtensionClasses() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<String> getBuildCompatibleExtensionClasses() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ReferenceContext getReferenceContext(Set<Class<?>> injectionClasses) throws CDIException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CDIRuntime getCDIRuntime() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ReferenceContext getReferenceContext() throws CDIException {
        // TODO Auto-generated method stub
        return null;
    }

}
