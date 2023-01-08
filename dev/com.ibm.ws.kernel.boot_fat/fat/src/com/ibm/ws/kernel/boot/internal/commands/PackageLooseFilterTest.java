/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test loose archive filtering.
 *
 * This is per issue #15724: "Loose application in runtime is different from server package".
 *
 * The problem is how the filtering used by packaging handles slashes.
 *
 * Previously, the filtering did not normalize loose entry paths to use consistent slashes.  That causes problems when packaging on windows: Filtering used forward slashes, but loose configuration provided paths with forward or reverse slashes depending on the platform.
 *
 * This test verifies that exclude and include processing is performed correctly
 * during packaging.  The test uses a loose configuration with:
 *
 * dir sourceOnDisk="${server.config.dir}\looseConfig\filterContent"
 *     targetInArchive="filterTxt"
 *     excludes="(**\/*.xml)"
 * 
 * dir sourceOnDisk="${server.config.dir}\looseConfig\filterContent"
 *     targetInArchive="filterXml"
 *     excludes="(**\/*.txt)"
 * 
 * dir sourceOnDisk="${server.config.dir}\looseConfig\filterContent"
 *     targetInArchive="filterSample1Txt"
 *     exclude="(**\/*.xml)|(**\/sample0.*)"
 *
 * With the "filterContent" containing:
 *   sample0.txt
 *   sample0.xml
 *   sample1.txt
 *   sample1.xml
 */
@RunWith(FATRunner.class)
public class PackageLooseFilterTest extends AbstractLooseConfigTest {

    private static final String MODULE_NAME_LOOSE = "Filter.war.xml";
    private static final String MODULE_NAME = "Filter.war";

    private static final List<String> FILTER_REQUIRED_ENTRIES;
    private static final List<String> FILTER_FORBIDDEN_ENTRIES;

    static {
        FILTER_REQUIRED_ENTRIES = new ArrayList<String>(2);
        FILTER_FORBIDDEN_ENTRIES = new ArrayList<String>(2);

        FILTER_FORBIDDEN_ENTRIES.add("filterTxt/sample0.xml");
        FILTER_REQUIRED_ENTRIES .add("filterTxt/sample0.txt");
        FILTER_FORBIDDEN_ENTRIES.add("filterTxt/sample1.xml");        
        FILTER_REQUIRED_ENTRIES .add("filterTxt/sample1.txt");
        
        FILTER_REQUIRED_ENTRIES .add("filterXml/sample0.xml");
        FILTER_FORBIDDEN_ENTRIES.add("filterXml/sample0.txt");
        FILTER_REQUIRED_ENTRIES .add("filterXml/sample1.xml");
        FILTER_FORBIDDEN_ENTRIES.add("filterXml/sample1.txt");

        FILTER_FORBIDDEN_ENTRIES.add("filterSample1Txt/sample0.xml");
        FILTER_FORBIDDEN_ENTRIES.add("filterSample1Txt/sample1.xml");
        FILTER_FORBIDDEN_ENTRIES.add("filterSample1Txt/sample0.txt");
        FILTER_REQUIRED_ENTRIES .add("filterSample1Txt/sample1.txt");
    }
    
    private String moduleLoosePath;
    private String moduleExpandedPath;

    //

    @Override
    public String getAppsTargetDir() {
        return APPS_DIR;
    }

    //

    @Rule
    public TestName testName = new TestName();

    @Before
    public void before() throws Exception {
        System.out.println(testName.getMethodName());

        setServer( LibertyServerFactory.getLibertyServer(SERVER_NAME) );

        String appsPath = getServer().getServerRoot() + '/' + getAppsTargetDir() + '/'; 
        moduleLoosePath = appsPath + MODULE_NAME_LOOSE;
        moduleExpandedPath = appsPath + "expanded/" + MODULE_NAME;

        System.out.println("  Module loose config: " + MODULE_NAME_LOOSE);
        System.out.println("  Module loose config path: " + moduleLoosePath);
        System.out.println("  Module archive: " + MODULE_NAME);
        System.out.println("  Module expanded path: " + moduleExpandedPath);

        getServer().deleteFileFromLibertyServerRoot(MODULE_NAME);
    }

    @After
    public void clean() throws Exception {
        new File(moduleLoosePath).delete();
    }

    //

    @Test
    public void testUsr_Filter() throws Exception {
        String[] packageCmd = new String[] {
            "--archive=" + SERVER_NAME,
            "--include=usr",
            "--server-root=" + SERVER_ROOT };
        String archivePath = packageServer(MODULE_NAME_LOOSE, SERVER_NAME_ZIP, packageCmd);
        // Because server-root and include=usr are specified,
        // packaging shifts the server folder up one directory.  The
        // 'usr' directory is excised from the path.
        verifyContents(archivePath,
            SERVER_ROOT, !INCLUDE_USR, SERVER_NAME,
            MODULE_NAME);
    }

    @Override
    protected void verifyContents(
        String archivePath,
        String serverRoot, boolean includeUsr, String serverName,
        String moduleName) throws IOException {

        super.verifyContents(archivePath,
            serverRoot, includeUsr, serverName,
            moduleName, VERIFY_APP);

        verifyExpandedContents(archivePath,
            serverRoot, includeUsr, serverName,
            moduleName);
        
        verifyFilteredContents(archivePath,
            serverRoot, includeUsr, serverName,
            moduleName,
            FILTER_REQUIRED_ENTRIES, FILTER_FORBIDDEN_ENTRIES);
    }
}