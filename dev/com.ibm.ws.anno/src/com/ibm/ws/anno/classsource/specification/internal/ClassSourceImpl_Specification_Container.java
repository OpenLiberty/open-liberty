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
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Container;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;

public abstract class ClassSourceImpl_Specification_Container
                extends ClassSourceImpl_Specification
                implements ClassSource_Specification_Container {

    //

    protected ClassSourceImpl_Specification_Container(ClassSourceImpl_Factory factory) {
        super(factory);
    }

    //

    protected ContainerData immediateContainerData;

    @Override
    public String getImmediateContainerName() {
        return immediateContainerData.name;
    }

    @Override
    public Container getImmediateContainer() {
        return immediateContainerData.container;
    }

    @Override
    public void setImmediateContainerData(String name, Container immediateContainer) {
        this.immediateContainerData = new ContainerData(name, immediateContainer);
    }

    @Override
    public void setImmediateContainerData(ContainerData immediateContainerData) {
        this.immediateContainerData = immediateContainerData;
    }

    @Override
    public ContainerData getImmediateContainerData() {
        return immediateContainerData;
    }

    //

    protected ContainerData applicationLibraryContainerData;

    @Override
    public String getApplicationLibraryContainerName() {
        return ((applicationLibraryContainerData == null) ? null : applicationLibraryContainerData.name);
    }

    @Override
    public Container getApplicationLibraryContainer() {
        return ((applicationLibraryContainerData == null) ? null : applicationLibraryContainerData.container);
    }

    @Override
    public void setApplicationLibraryContainerData(String name, Container applicationLibraryContainer) {
        this.applicationLibraryContainerData = new ContainerData(name, applicationLibraryContainer);
    }

    @Override
    public void setApplicationLibraryContainerData(ContainerData applicationLibraryContainerData) {
        this.applicationLibraryContainerData = applicationLibraryContainerData;
    }

    @Override
    public ContainerData getApplicationLibraryContainerData() {
        return applicationLibraryContainerData;
    }

    //

    protected List<ContainerData> applicationLibrariesContainerData;

    @Override
    public List<ContainerData> getApplicationLibrariesContainerData() {
        return applicationLibrariesContainerData;
    }

    @Override
    public void addApplicationLibrariesContainerData(String name, Container applicationLibraryContainer) {
        if (applicationLibrariesContainerData == null) {
            applicationLibrariesContainerData = new ArrayList<ContainerData>();
        }

        applicationLibrariesContainerData.add(new ContainerData(name, applicationLibraryContainer));
    }

    @Override
    public void addApplicationLibrariesContainerData(ContainerData applicationLibraryContainerData) {
        if (applicationLibrariesContainerData == null) {
            applicationLibrariesContainerData = new ArrayList<ContainerData>();
        }

        this.applicationLibrariesContainerData.add(applicationLibraryContainerData);
    }

    @Override
    public void addApplicationLibrariesContainerData(List<ContainerData> applicationLibrariesContainerData) {
        if (applicationLibrariesContainerData == null) {
            applicationLibrariesContainerData = new ArrayList<ContainerData>();
        }

        this.applicationLibrariesContainerData.addAll(applicationLibrariesContainerData);
    }

    //

    protected List<ContainerData> manifestContainerData;

    @Override
    public List<ContainerData> getManifestContainerData() {
        return manifestContainerData;
    }

    @Override
    public void addManifestContainerData(String name, Container manifestContainer) {
        if (manifestContainerData == null) {
            manifestContainerData = new ArrayList<ContainerData>();
        }

        manifestContainerData.add(new ContainerData(name, manifestContainer));

    }

    @Override
    public void addManifestContainerData(List<ContainerData> manifestContainerData) {
        if (this.manifestContainerData == null) {
            this.manifestContainerData = new ArrayList<ContainerData>();
        }

        this.manifestContainerData.addAll(manifestContainerData);
    }

    @Override
    public void addManifestContainerData(ContainerData manifestContainer) {
        if (this.manifestContainerData == null) {
            this.manifestContainerData = new ArrayList<ContainerData>();
        }

        this.manifestContainerData.addAll(manifestContainerData);
    }

    //

    @Override
    public abstract ClassSource_Aggregate createClassSource(String targetName, ClassLoader rootClassLoader)
                    throws ClassSource_Exception;

    protected void addStandardClassSources(String targetName,
                                           ClassLoader rootClassLoader,
                                           ClassSource_Aggregate classSource) throws ClassSource_Exception {

        ContainerData useApplicationLibContainerData = getApplicationLibraryContainerData();

        List<ContainerData> applicationLibrariesContainerData;

        if (useApplicationLibContainerData != null) {
            applicationLibrariesContainerData = selectJars(useApplicationLibContainerData);
        } else {
            applicationLibrariesContainerData = getApplicationLibrariesContainerData();
        }

        if (applicationLibrariesContainerData != null) {
            for (ContainerData nextContainerData : applicationLibrariesContainerData) {
                String nextContainerName = targetName + " application library " + nextContainerData.name;

                Container nextContainer = nextContainerData.container;

                // Missing an explicitly specified application library.
                if (nextContainer == null) {
                    throw new ClassSource_Exception("Container [ " + nextContainerName + " ] could not be resolved");
                }

                getFactory().addContainerClassSource(classSource,
                                                     nextContainerData.name,
                                                     nextContainerData.container,
                                                     ScanPolicy.EXTERNAL); // throws ClassSource_Exception
            }
        }

        List<ContainerData> manifestContainerData = getManifestContainerData();

        if (manifestContainerData != null) {
            for (ContainerData nextManifestContainerData : manifestContainerData) {
                String nextContainerName = targetName + " manifest " + nextManifestContainerData.name;
                Container nextContainer = nextManifestContainerData.container;

                // Missing an explicitly specified manifest class path entry is an error.
                if (nextContainer == null) {
                    throw new ClassSource_Exception("Container [ " + nextContainerName + " ] could not be resolved");
                }

                getFactory().addContainerClassSource(classSource,
                                                     nextContainerName,
                                                     nextContainer,
                                                     ClassSource_Aggregate.ScanPolicy.EXTERNAL); // throws ClassSource_Exception
            }
        }

        if (rootClassLoader != null) {
            getFactory().addClassLoaderClassSource(classSource, "classloader", rootClassLoader);
        }
    }

    // TODO: This must recurse!

    public List<ContainerData> selectJars(ContainerData targetContainerData) throws ClassSource_Exception {
        List<ContainerData> jarContainers = new ArrayList<ContainerData>();

        // A null target container means than an explicitly
        // specified container was not located.  That is an error.

        Container targetContainer = targetContainerData.container;
        if (targetContainer == null) {
            throw new ClassSource_Exception("Container [ " + targetContainerData.name + " ] was not resolved");
        }

        for (Entry nextEntry : targetContainerData.container) {
            String nextEntryName = nextEntry.getName();
            if (!nextEntryName.toUpperCase().endsWith(".JAR")) {
                continue; // The targets MUST have the ".jar" extension.
            }

            try {
                Container nextContainer = nextEntry.adapt(Container.class);
                if (nextContainer == null) {
                    continue; // TODO: This should be an error
                }

                ContainerData nextContainerData =
                                new ContainerData(targetContainerData.name + " to " + nextEntryName, nextContainer);

                jarContainers.add(nextContainerData);
            } catch (UnableToAdaptException e) {
                throw new ClassSource_Exception("JAR select [ " + nextEntryName + " ] could not be used as a container: " + e);
            }
        }

        return jarContainers;
    }

    //

    @Override
    public abstract void log(TraceComponent logger);

    protected void logCommon(TraceComponent logger) {

        ContainerData useImmediateContainerData = getImmediateContainerData();

        Tr.debug(logger, MessageFormat.format("  Immediate container [ {0} ] [ {1} ]",
                                              useImmediateContainerData.name, useImmediateContainerData.container));

        ContainerData useApplicationLibraryContainerData = getApplicationLibraryContainerData();

        if (useApplicationLibraryContainerData != null) {
            Tr.debug(logger, MessageFormat.format("  Application library container [ {0} ] [ {1} ]",
                                                  useApplicationLibraryContainerData.name,
                                                  useApplicationLibraryContainerData.container));
        }

        List<ContainerData> useLibrariesContainerData = getApplicationLibrariesContainerData();

        if (useLibrariesContainerData != null) {
            for (ContainerData nextContainerData : useLibrariesContainerData) {
                Tr.debug(logger, MessageFormat.format("  Application library element container [ {0} ] [ {1} ]",
                                                      nextContainerData.name, nextContainerData.container));
            }
        }

        List<ContainerData> useManifestContainerData = getManifestContainerData();
        if (useManifestContainerData != null) {
            for (ContainerData nextContainerData : useManifestContainerData) {
                Tr.debug(logger, MessageFormat.format("  Manifest element container  [ {0} ] [ {1} ]",
                                                      nextContainerData.name, nextContainerData.container));
            }
        }
    }
}
