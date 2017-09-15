/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.filemonitor;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.wsspi.kernel.filemonitor.FileMonitor;

/**
 *
 */
public class SecurityFileMonitorTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private SecurityFileMonitor securityFileMonitor;
    private final FileBasedActionable fileBasedActionable = mockery.mock(FileBasedActionable.class);
    private final File fileToMonitor = new File("fileToMonitor");
    Collection<String> paths = Arrays.asList("fileToMonitor");
    private final long monitorInterval = 5000L;
    ServiceRegistration<FileMonitor> registration;

    @Before
    public void setUp() {
        securityFileMonitor = new SecurityFileMonitor(fileBasedActionable);
        final BundleContext bundleContext = mockery.mock(BundleContext.class);
        final Hashtable<String, Object> fileMonitorProps = new Hashtable<String, Object>();
        fileMonitorProps.put(FileMonitor.MONITOR_FILES, paths);
        fileMonitorProps.put(FileMonitor.MONITOR_INTERVAL, monitorInterval);

        mockery.checking(new Expectations() {
            {
                one(fileBasedActionable).getBundleContext();
                will(returnValue(bundleContext));
                one(bundleContext).registerService(FileMonitor.class, securityFileMonitor, fileMonitorProps);
            }
        });

        registration = securityFileMonitor.monitorFiles(paths, monitorInterval);
        assertNotNull("There must be a service registration", registration);
    }

    @After
    public void tearDown() {
        mockery.assertIsSatisfied();
    }

    @Test
    public void scanComplete_loadOnModified() {
        Collection<File> createdFiles = Collections.emptySet();
        Collection<File> modifiedFiles = new HashSet<File>();
        modifiedFiles.add(fileToMonitor);
        Collection<File> deletedFiles = Collections.emptySet();
        final Collection<File> files = modifiedFiles;

        mockery.checking(new Expectations() {
            {
                one(fileBasedActionable).performFileBasedAction(files);
            }
        });

        securityFileMonitor.onChange(createdFiles, modifiedFiles, deletedFiles);
    }

    @Test
    public void scanComplete_ignoreOnDelete() {
        Collection<File> createdFiles = Collections.emptySet();
        Collection<File> modifiedFiles = Collections.emptySet();
        Collection<File> deletedFiles = new HashSet<File>();
        deletedFiles.add(fileToMonitor);
        final Collection<File> files = deletedFiles;

        mockery.checking(new Expectations() {
            {
                never(fileBasedActionable).performFileBasedAction(files);
            }
        });

        securityFileMonitor.onChange(createdFiles, modifiedFiles, deletedFiles);
    }

    @Test
    public void scanComplete_ignoreOnFirstCreate() {
        Collection<File> createdFiles = new HashSet<File>();
        Collection<File> modifiedFiles = Collections.emptySet();
        Collection<File> deletedFiles = Collections.emptySet();
        createdFiles.add(fileToMonitor);
        final Collection<File> files = createdFiles;

        mockery.checking(new Expectations() {
            {
                never(fileBasedActionable).performFileBasedAction(files);
            }
        });

        securityFileMonitor.onChange(createdFiles, modifiedFiles, deletedFiles);
    }

    /**
     * Tests that the keys are loaded in the notification of the second create in the sequence of create, delete, create.
     * This is the same as starting the server and there is no initial file so it is created, then the file is deleted,
     * and finally a new file with the same name is copied. There is no loading in the first create since the keys are
     * loaded due to the activation of the config object. The contents of the new file in the second create must be
     * loaded since it could be different than the original contents.
     */
    @Test
    public void scanComplete_loadOnNextCreationAfterDeletingFileBeingMonitored() {
        Collection<File> createdFiles = new HashSet<File>();
        Collection<File> modifiedFiles = Collections.emptySet();
        Collection<File> deletedFiles = new HashSet<File>();
        createdFiles.add(fileToMonitor);
        final Collection<File> files = createdFiles;

        mockery.checking(new Expectations() {
            {
                one(fileBasedActionable).performFileBasedAction(files);
            }
        });

        securityFileMonitor.onChange(createdFiles, modifiedFiles, deletedFiles);
        createdFiles.clear();
        deletedFiles.add(fileToMonitor);
        securityFileMonitor.onChange(createdFiles, modifiedFiles, deletedFiles);
        deletedFiles.clear();
        createdFiles.add(fileToMonitor);
        securityFileMonitor.onChange(createdFiles, modifiedFiles, deletedFiles);
    }

}
