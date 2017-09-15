/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.anno.classsource.specification.internal;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_Aggregate;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Container_WAR;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;

public class ClassSourceImpl_Specification_Container_WAR
                extends ClassSourceImpl_Specification_Container
                implements ClassSource_Specification_Container_WAR {

    public ClassSourceImpl_Specification_Container_WAR(ClassSourceImpl_Factory factory) {
        super(factory);
    }

    //

    protected ContainerData warClassesContainerData;

    protected boolean isResolvedWARClassesContainerData;
    protected ContainerData resolvedWARClassesContainerData;

    @Override
    public String getWARClassesContainerName() {
        return ((warClassesContainerData == null) ? null : warClassesContainerData.name);
    }

    @Override
    public Container getWARClassesContainer() {
        return ((warClassesContainerData == null) ? null : warClassesContainerData.container);
    }

    @Override
    public void setWARClassesContainerData(String name, Container warClassesContainer) {
        this.warClassesContainerData = new ContainerData(name, warClassesContainer);
    }

    @Override
    public void setWARClassesContainerData(ContainerData warClassesContainerData) {
        this.warClassesContainerData = warClassesContainerData;
    }

    @Override
    public ContainerData getWARClassesContainerData() {
        return warClassesContainerData;
    }

    @Override
    public synchronized ContainerData getResolvedWARClassesContainerData() throws ClassSource_Exception {
        // TODO: What to do about exceptions after the first request?
        if (!isResolvedWARClassesContainerData) {
            isResolvedWARClassesContainerData = true;
            resolvedWARClassesContainerData = resolveWARClassesContainerData(); // throws ClassSource_Exception
        }
        return resolvedWARClassesContainerData;
    }

    protected ContainerData resolveWARClassesContainerData() throws ClassSource_Exception {
        ContainerData useWARClassesContainerData = getWARClassesContainerData();
        if (useWARClassesContainerData != null) {
            return useWARClassesContainerData;
        }

        Container useImmediateContainer = getImmediateContainer();
        Entry warClassesEntry = useImmediateContainer.getEntry("/" + "WEB-INF/classes");
        if (warClassesEntry == null) {
            return null; // TODO: Should this be an error?
        }

        String warClassesName = useImmediateContainer.getName() + " to " + "/" + "WEB-INF/classes";

        try {
            Container warClassesContainer = warClassesEntry.adapt(Container.class);
            if (warClassesContainer == null) {
                throw new ClassSource_Exception("WAR classes [ " + warClassesName + " ] could not be used as a container");
            }

            return new ContainerData(warClassesName, warClassesContainer);
        } catch (UnableToAdaptException e) {
            throw new ClassSource_Exception("WAR classes [ " + warClassesName + " ] could not be used as a container: " + e);
        }
    }

    //

    protected ContainerData warLibraryContainerData;

    protected boolean isResolvedWARLibraryContainerData;
    protected ContainerData resolvedWARLibraryContainerData;

    @Override
    public Container getWARLibraryContainer() {
        return ((warLibraryContainerData == null) ? null : warLibraryContainerData.container);
    }

    @Override
    public String getWARLibraryContainerName() {
        return ((warLibraryContainerData == null) ? null : warLibraryContainerData.name);
    }

    @Override
    public void setWARLibraryContainerData(String name, Container warLibraryContainer) {
        this.warLibraryContainerData = new ContainerData(name, warLibraryContainer);
    }

    @Override
    public void setWARLibraryContainerData(ContainerData warLibraryContainerData) {
        this.warLibraryContainerData = warLibraryContainerData;
    }

    @Override
    public ContainerData getWARLibraryContainerData() {
        return warLibraryContainerData;
    }

    @Override
    public synchronized ContainerData getResolvedWARLibraryContainerData() throws ClassSource_Exception {
        // TODO: What to do about exceptions after the first request?
        if (!isResolvedWARLibraryContainerData) {
            isResolvedWARLibraryContainerData = true;
            resolvedWARLibraryContainerData = resolveWARLibraryContainerData(); // throws ClassSource_Exception
        }
        return resolvedWARLibraryContainerData;
    }

    protected ContainerData resolveWARLibraryContainerData() throws ClassSource_Exception {
        ContainerData useWARLibContainerData = getWARLibraryContainerData();
        if (useWARLibContainerData != null) {
            return useWARLibContainerData;
        }

        Container useImmediateContainer = getImmediateContainer();
        Entry warLibEntry = useImmediateContainer.getEntry("/" + "WEB-INF/lib");
        if (warLibEntry == null) {
            return null; // TODO: Should this be an error?
        }

        String warLibName = useImmediateContainer.getName() + " to " + "/" + "WEB-INF/lib";

        try {
            Container warLibContainer = warLibEntry.adapt(Container.class);
            if (warLibContainer == null) {
                throw new ClassSource_Exception("WAR library [ " + warLibName + " ] could not be used as a container");
            }

            return new ContainerData(warLibName, warLibContainer);
        } catch (UnableToAdaptException e) {
            throw new ClassSource_Exception("WAR library [ " + warLibName + " ] could not be used as a container: " + e);
        }
    }

    //

    protected boolean useWARLibrariesContainerData;

    @Override
    public boolean getUseWARLibrariesContainerData() {
        return useWARLibrariesContainerData;
    }

    @Override
    public void setUseWARLibrariesContainerData(boolean useWARLibrariesContainerData) {
        this.useWARLibrariesContainerData = useWARLibrariesContainerData;
    }

    protected List<ContainerData> warLibrariesContainerData;

    @Override
    public List<ContainerData> getWARLibrariesContainerData() {
        return warLibrariesContainerData;
    }

    @Override
    public ContainerData addWARLibrariesContainerData(String name, Container warLibrariesContainer) {
        if (this.warLibrariesContainerData == null) {
            this.warLibrariesContainerData = new ArrayList<ContainerData>();
        }

        ContainerData newContainerData = new ContainerData(name, warLibrariesContainer);

        this.warLibrariesContainerData.add(newContainerData);

        return newContainerData;
    }

    @Override
    public void addWARLibrariesContainerData(ContainerData warLibrariesContainerData) {
        if (this.warLibrariesContainerData == null) {
            this.warLibrariesContainerData = new ArrayList<ContainerData>();
        }

        this.warLibrariesContainerData.add(warLibrariesContainerData);
    }

    @Override
    public void addWARLibrariesContainerData(List<ContainerData> warLibrariesContainerData) {
        if (this.warLibrariesContainerData == null) {
            this.warLibrariesContainerData = new ArrayList<ContainerData>();
        }

        this.warLibrariesContainerData.addAll(warLibrariesContainerData);
    }

    protected Set<ContainerData> warIncludedContainerData;

    @Override
    public Set<ContainerData> getWARIncludedContainerData() {
        return warIncludedContainerData;
    }

    @Override
    public void addWARIncludedContainerData(ContainerData warIncludedContainerData) {
        if (this.warIncludedContainerData == null) {
            this.warIncludedContainerData = new HashSet<ContainerData>();
        }

        this.warIncludedContainerData.add(warIncludedContainerData);
    }

    @Override
    public void addWARIncludedContainerData(Set<ContainerData> warIncludedContainerData) {
        if (this.warIncludedContainerData == null) {
            this.warIncludedContainerData = new HashSet<ContainerData>();
        }

        this.warIncludedContainerData.addAll(warIncludedContainerData);
    }

    //

    @Override
    public ClassSource_Aggregate createClassSource(String targetName, ClassLoader rootClassLoader)
                    throws ClassSource_Exception {

        ClassSourceImpl_Aggregate classSource = createAggregateClassSource(targetName);

        ContainerData warClassesContainerData = getResolvedWARClassesContainerData(); // throws ClassSource_Exception
        if (warClassesContainerData != null) {
            getFactory().addContainerClassSource(classSource,
                                                 warClassesContainerData.name,
                                                 warClassesContainerData.container,
                                                 ScanPolicy.SEED); // throws ClassSource_Exception
        }

        List<ContainerData> warLibrariesContainerData;

        if (getUseWARLibrariesContainerData()) {
            warLibrariesContainerData = getWARLibrariesContainerData();

        } else {
            ContainerData warLibContainerData = getResolvedWARLibraryContainerData(); // throws ClassSource_Exception
            if (warLibContainerData != null) {
                warLibrariesContainerData = selectJars(warLibContainerData);
            } else {
                warLibrariesContainerData = null;
            }
        }

        if (warLibrariesContainerData != null) {
            Set<ContainerData> useIncludedContainerData = getWARIncludedContainerData();

            for (ContainerData nextContainerData : warLibrariesContainerData) {
                boolean isSeed = ((useIncludedContainerData == null) || useIncludedContainerData.contains(nextContainerData));
                ScanPolicy scanPolicy = (isSeed ? ScanPolicy.SEED : ScanPolicy.EXTERNAL);

                getFactory().addContainerClassSource(classSource,
                                                     nextContainerData.name,
                                                     nextContainerData.container,
                                                     scanPolicy); // throws ClassSource_Exception
            }
        }

        addStandardClassSources(targetName, rootClassLoader, classSource);

        return classSource;
    }

    //

    @Override
    public void log(TraceComponent logger) {
        Tr.debug(logger, MessageFormat.format("Class source specification [ {0} ]", getHashText()));

        logLocations(logger);
        logCommon(logger);
    }

    protected void logLocations(TraceComponent logger) {
        ContainerData useWARClassesContainerData = getWARClassesContainerData();
        if (useWARClassesContainerData == null) {
            Tr.debug(logger, MessageFormat.format("  WAR classes container [ {0} ] to [ {1} ] (implied)",
                                                  getImmediateContainer(), "/" + "WEB-INF/classes"));

        } else {
            Tr.debug(logger, MessageFormat.format("  WAR classes container [ {0} ] [ {1} ]",
                                                  useWARClassesContainerData.name, useWARClassesContainerData.container));
        }

        List<ContainerData> warLibrariesContainerData;

        if (getUseWARLibrariesContainerData()) {
            warLibrariesContainerData = getWARLibrariesContainerData();

            if (warLibrariesContainerData != null) {
                for (ContainerData nextContainerData : warLibrariesContainerData) {
                    Tr.debug(logger, MessageFormat.format("  WAR library element container [ {0} ] [ {1} ]",
                                                          nextContainerData.name, nextContainerData.container));
                }
            }

        } else {
            ContainerData useWARLibContainerData = getWARLibraryContainerData();

            if (useWARLibContainerData == null) {
                Tr.debug(logger, "  WAR library container [ {0} ] to [ {1} ] (implied)",
                         getImmediateContainer(), "/" + "WEB-INF/lib");

            } else {
                Tr.debug(logger, MessageFormat.format("  WAR library container [ {0} ] [ {1} ]",
                                                      useWARLibContainerData.name, useWARLibContainerData.container));
            }
        }
    }
}
