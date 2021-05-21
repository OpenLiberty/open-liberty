/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal.commands;

import static componenttest.annotation.SkipIfSysProp.OS_ZOS;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import componenttest.annotation.SkipIfSysProp;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FileUtils;

@RunWith(Parameterized.class)
@SkipIfSysProp(OS_ZOS) // Jar not supported on Z/OS
public class PackageLooseRunnableTest extends AbstractLooseConfigTest {
    private static final String MODULE_NAME = "DefaultArchive.war";
    private static final String MODULE_NAME_LOOSE = "DefaultArchive.war.xml";

    //

    private static final List<Object[]> CONFIGS = new ArrayList<Object[]>(2);
    static {
        CONFIGS.add(new Object[] { APPS_DIR });
        CONFIGS.add(new Object[] { DROPINS_DIR });
    };

    @Parameters
    public static Collection<Object[]> data() {
        return CONFIGS;
    }

    public PackageLooseRunnableTest(String targetDir) {
        this.targetDir = targetDir;
    }

    private final String targetDir;

    @Override
    public String getAppsTargetDir() {
        return targetDir;
    }
    
    //

    @Rule
    public TestName testName = new TestName();

    @Before
    public void before() throws Exception {
        System.out.println(testName.getMethodName());
        System.out.println("Target: " + getAppsTargetDir() );

        setServer( LibertyServerFactory.getLibertyServer(SERVER_NAME) );
        getServer().deleteFileFromLibertyServerRoot(ARCHIVE_NAME_ZIP);
    }

    @After
    public void clean() throws Exception {
        try {
            getServer().deleteFileFromLibertyServerRoot( getAppsTargetDir() + '/' + MODULE_NAME_LOOSE);
        } finally {
            FileUtils.recursiveDelete( new File(BUILD_DIR + '/' + WLP_EXTRACT) );
        }
    }

    //

    @Test
    public void testAll() throws Exception {
        String prepackedModuleName = MODULE_NAME_LOOSE;
        String packedModuleName = MODULE_NAME;
        String archiveName = ARCHIVE_NAME_ZIP;
        String[] packageCmd = {
            "--archive=" + archiveName,
            "--include=all",
            "--server-root=" + SERVER_ROOT                
        };
        String archivePath = packageServer(prepackedModuleName, archiveName, packageCmd);

        verifyContents(archivePath,
            SERVER_ROOT, INCLUDE_USR, SERVER_NAME,
            packedModuleName, VERIFY_APP);
    }

    @Test
    public void testRunnable_DefaultRoot() throws Exception {
        String prepackedModuleName = MODULE_NAME_LOOSE;
        String packedModuleName = MODULE_NAME;
        
        PackageCommandTest.assumeSelfExtractExists( getServer() );

        String archivePath = packageRunnable(prepackedModuleName, ARCHIVE_NAME_1_JAR, SERVER_ROOT_DEFAULT);
        verifyContents(archivePath,
            SERVER_ROOT_DEFAULT, INCLUDE_USR, SERVER_NAME,
            packedModuleName, VERIFY_APP);        
        launchRunnable(archivePath);
    }

    @Test
    public void testRunnable() throws Exception {
        String prepackedModuleName = MODULE_NAME_LOOSE;
        String packedModuleName = MODULE_NAME;

        PackageCommandTest.assumeSelfExtractExists( getServer() );

        String archivePath = packageRunnable(prepackedModuleName, ARCHIVE_NAME_2_JAR, SERVER_ROOT);
        // Note the change from 'SERVER_ROOT' to 'SERVER_ROOT_DEFAULT';
        // The server root parameter is ignored when packaging a runnable server.
        verifyContents(archivePath,
            SERVER_ROOT_DEFAULT, INCLUDE_USR, SERVER_NAME,
            packedModuleName, VERIFY_APP);
        launchRunnable(archivePath);
    }
}