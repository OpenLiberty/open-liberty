/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.org.hibernate.validator;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.Test;

/**
 * The purpose of this test is to ensure that we always pick up changes in Hibernate Validator NLS files so we can translate
 * the changes into languages that OpenLiberty ships but Hibernate Validator does not.
 * Currently OpenLiberty provides: cs, de, es, fr, hu, it, ja, ko, pl, pt_BR, ro, ru, zh_TW, zh
 * Currently Hibernate Validator provides: ar, cs, de, en, es, fa, fr, hu, ko, mn_MN, pt_BR, ru, sk, tr, uk, zh_CN
 *
 * This means that OpenLiberty must provide the following languages: it, ja, pl, ro, zh_TW, zh
 */
public class HibernateValidatorMessageTest {

    private static final String CLASSPATH = System.getProperty("java.class.path").replace("\\", "/");
    private static final String testBuildDir = System.getProperty("test.buildDir").replace("\\", "/");

    @Test
    public void testMessagesUnchanged() throws Exception {

        File ossHibernateFile = null;
        for (String classpathElement : CLASSPATH.split(System.getProperty("path.separator"))) {
            if (classpathElement.endsWith(".jar") && classpathElement.contains("hibernate-validator-6")) {
                ossHibernateFile = new File(classpathElement);
                break;
            }
        }
        assertTrue(ossHibernateFile.exists());

        File buildDir = new File(testBuildDir);
        assertTrue((buildDir.exists()));
        File wsHibernateFile = null;
        for (File f : buildDir.listFiles()) {
            if (f.getName().endsWith(".jar") && f.getName().contains("hibernate.validator.6")) {
                wsHibernateFile = f;
                break;
            }
        }
        assertTrue(wsHibernateFile.exists());

        long ossMessageChecksum = getChecksum(ossHibernateFile);
        long wsMessageChecksum = getChecksum(wsHibernateFile);
        assertEquals("The checksum for org/hibernate/validator/ValidationMessages.properties in the " + wsHibernateFile.getName() +
                     " bundle has changed ( " + ossMessageChecksum + " --> " + wsMessageChecksum + " ). " +
                     "If you are checking in a new version of Hibernate Validator, be sure to copy the message files from the open source jar into the " +
                     "resources/org/hibernate/validator/ folder of this project so the translation team can be notified of the changes and provide " +
                     "message updates in languages that Liberty provides but Hibernate Validator does not.",
                     ossMessageChecksum, wsMessageChecksum);
    }

    private long getChecksum(File f) throws IOException {
        try (JarFile jar = new JarFile(f)) {
            JarEntry nlsFile = jar.getJarEntry("org/hibernate/validator/ValidationMessages.properties");
            assertNotNull(nlsFile);
            return nlsFile.getCrc();
        }
    }

}
