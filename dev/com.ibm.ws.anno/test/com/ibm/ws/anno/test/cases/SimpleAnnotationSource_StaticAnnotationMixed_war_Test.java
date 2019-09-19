/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.anno.test.cases;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Direct_WAR;
import com.ibm.ws.anno.test.data.Common_Data;
import com.ibm.ws.anno.test.data.LoginMethod_ear_Data;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;
import com.ibm.wsspi.anno.classsource.ClassSource_MappedSimple;

public class SimpleAnnotationSource_StaticAnnotationMixed_war_Test extends AnnotationTest_BaseClass {
    public static final String EAR_NAME = LoginMethod_ear_Data.EAR_NAME;
    public static final String WAR_NAME = LoginMethod_ear_Data.WAR_NAME_STATIC_ANNOTATION_MIXED;

    @Override
    public ClassSource_Specification_Direct_WAR createClassSourceSpecification() {
        return null; // Not in use
    }

    public ClassSource_Aggregate createAggregateClassSource(String name) throws ClassSource_Exception {
        return getClassSourceFactory().createAggregateClassSource(name);
    }

    private List<String> collectFileNames(File dir) throws ClassSource_Exception {
        try {
            return collectFileNames(dir, dir);
        } catch (IOException io) {
            throw new ClassSource_Exception("", io);
        }
    }

    private List<String> collectFileNames(File baseDir, File dir) throws IOException {
        ArrayList<String> names = new ArrayList<String>();
        File[] files = dir.listFiles();
        int chopLength = baseDir.getCanonicalPath().length();
        for (File f : files) {
            if (f.isDirectory()) {
                names.addAll(collectFileNames(baseDir, f));
            } else {
                String relpath = f.getCanonicalPath().substring(chopLength);
                if ("\\".equals(File.separator)) {
                    relpath = relpath.replace("\\", "/");
                }
                if (relpath.startsWith("/")) {
                    relpath = relpath.substring(1);
                }
                names.add(relpath);
            }
        }
        return names;
    }

    @Override
    public ClassSource_Aggregate createClassSource() throws ClassSource_Exception {
        String fullDataPath = Common_Data.putIntoPath(projectPath, dataPath);
        String fullWARPath = Common_Data.putIntoPath(fullDataPath, WAR_NAME);

        ClassSource_Aggregate classSource = createAggregateClassSource("simple" + WAR_NAME);

        final String useWARClassesPath = fullWARPath + "/" + "WEB-INF/classes";

        final File classesDir = new File(useWARClassesPath);
        final List<String> names = collectFileNames(classesDir);

        //System.out.println("NAMES : " + names);

        ClassSource_MappedSimple.SimpleClassProvider provider = new ClassSource_MappedSimple.SimpleClassProvider() {
            @Override
            public String getName() {
                return "SIMPLESOURCETEST";
            }

            @Override
            public Collection<String> getResourceNames() {
                return names;
            }

            @Override
            public InputStream openResource(String resourceName) throws IOException {
                File f = new File(classesDir, resourceName);
                //System.out.println("R: " + resourceName);
                //System.out.println("F: " + f.getAbsolutePath());
                return new FileInputStream(f);
            }

        };

        getClassSourceFactory().addSimpleClassSource(classSource, "simple" + WAR_NAME + " classes", provider, ScanPolicy.SEED);

        getClassSourceFactory().addClassLoaderClassSource(classSource, "simple" + WAR_NAME + " classloader", getClass().getClassLoader());

        return classSource;
    }

    //

    public static final String LOG_NAME = EAR_NAME + " _simple" + WAR_NAME + ".log";

    //

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp(); // throws Exception

        setDataPath(EAR_NAME);
    }

    //

    @Override
    public String getTargetName() {
        return "simple" + WAR_NAME;
    }

    @Override
    public int getIterations() {
        return 5;
    }

    //

    /*
     * @Override
     * public boolean getSeedStorage() {
     * return true;
     * }
     */

    //

    @Test
    public void testSimpleAnnotationSource_StaticAnnotationMixed_war() throws Exception {
        runScanTest(DETAIL_IS_ENABLED,
                    getStoragePath(COMMON_TEMP_STORAGE_PATH), STORAGE_NAME_DETAIL,
                    getSeedStorage(), getStoragePath(COMMON_STORAGE_PATH), STORAGE_NAME_DETAIL,
                    new PrintWriter(System.out, true));
    }
}
