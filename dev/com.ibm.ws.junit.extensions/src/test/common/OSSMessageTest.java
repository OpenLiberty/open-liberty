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
package test.common;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.Test;

/**
 * The purpose of this test is to ensure that we always pick up changes OSS NLS files so we can translate
 * the changes into languages that OpenLiberty ships but the OSS library may not
 * Currently OpenLiberty provides: cs, de, es, fr, hu, it, ja, ko, pl, pt_BR, ro, ru, zh_TW, zh
 */
public class OSSMessageTest {

    private static final String CLASSPATH = System.getProperty("java.class.path").replace("\\", "/");
    private static final String testBuildDir = System.getProperty("test.buildDir").replace("\\", "/");
    private static final String mainClassesDir = System.getProperty("main.classesDir").replace("\\", "/");

    private final String OSS_JAR_NAME;
    private final String WLP_JAR_NAME;
    private final String MSG_FILE_PATH;

    /**
     * @param ossJarBaseName
     *            The basename of the OSS jar name. For example if the OSS jar is yasson-1.0.2.jar, put 'yasson-1'
     * @param wlpJarBaseName
     *            The basename of the repackaged OSS jar in the wlp install. For example if the jar becomes com.ibm.ws.org.eclipse.yasson.1.0.jar, put
     *            'com.ibm.ws.org.eclipse.yasson.1'
     * @param messageFilePath
     *            The path of the NLS message file within the OSS jar to compare checksums against. For example if
     *            there is a messgae file at OSS_JAR!/org/whatever/foo/Messages.properties, put 'org/whatever/foo/Messages.properties'
     */
    public OSSMessageTest(String ossJarBaseName, String wlpJarBaseName, String messageFilePath) {
        OSS_JAR_NAME = ossJarBaseName;
        WLP_JAR_NAME = wlpJarBaseName;
        MSG_FILE_PATH = messageFilePath;
    }

    @Test
    public void testMessagesUnchanged() throws Exception {

        File srcNLSFile = new File(mainClassesDir + "/../../../../resources/" + MSG_FILE_PATH);
        assertTrue("Checked-in NLS file did not exist at: " + srcNLSFile.getAbsolutePath(), srcNLSFile.exists());

        File ossFile = null;
        for (String classpathElement : CLASSPATH.split(System.getProperty("path.separator"))) {
            if (classpathElement.endsWith(".jar") && classpathElement.contains(OSS_JAR_NAME)) {
                ossFile = new File(classpathElement);
                break;
            }
        }
        assertTrue("OSS jar did not exist at: " + ossFile.getAbsolutePath(), ossFile.exists());

        File buildDir = new File(testBuildDir);
        assertTrue("Test buildDir did not exist at: " + buildDir.getAbsolutePath(), buildDir.exists());
        File wlpFile = null;
        for (File f : buildDir.listFiles()) {
            if (f.getName().endsWith(".jar") && f.getName().contains(WLP_JAR_NAME)) {
                wlpFile = f;
                break;
            }
        }
        assertTrue("Liberty bundle did not exist at: " + wlpFile.getAbsolutePath(), wlpFile.exists());

        long ossMessageChecksum = getChecksum(ossFile);
        long wsMessageChecksum = getChecksum(wlpFile);
        assertEquals("The checksum for " + MSG_FILE_PATH + " in the " + wlpFile.getName() +
                     " bundle has changed from " + ossMessageChecksum + " to " + wsMessageChecksum + ". " +
                     "If you are checking in a new version of this OSS library, be sure to copy the message files from the open source jar into the " +
                     "resources/ folder of this project so the translation team can be notified of the changes and provide " +
                     "message updates in languages that Liberty provides but the OSS library does not.",
                     ossMessageChecksum, wsMessageChecksum);
    }

    private long getChecksum(File f) throws IOException {
        try (JarFile jar = new JarFile(f)) {
            JarEntry nlsFile = jar.getJarEntry(MSG_FILE_PATH);
            assertNotNull("Did not find NLS message file path at: " + f.getAbsolutePath() + "!" + MSG_FILE_PATH, nlsFile);
            return nlsFile.getCrc();
        }
    }

}
