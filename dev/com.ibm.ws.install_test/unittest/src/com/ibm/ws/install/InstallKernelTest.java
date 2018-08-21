/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.install;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.install.InstallConstants.ExistsAction;
import com.ibm.ws.install.internal.InstallKernelImpl;

/**
 *
 */
public class InstallKernelTest {

    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private static File imageDir;
    private static String orginialTmpDir;

    /**
     * Capture stdout/stderr output to the manager.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        imageDir = new File("build/unittest/wlpDirs/developers/wlp").getAbsoluteFile();
        System.out.println("setUpBeforeClass() imageDir set to " + imageDir);
        if (imageDir == null || !imageDir.exists())
            throw new IllegalArgumentException("Test requires an existing root directory, but it could not be found: " + imageDir.getAbsolutePath());

        File libFeatureDir = new File(String.format("%s/lib/features", imageDir.getAbsoluteFile()));
        File libPlatformDir = new File(String.format("%s/lib/platform", imageDir.getAbsoluteFile()));

        FilenameFilter mfFilenameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".mf");
            }
        };
        int numFeatureMF = libFeatureDir.list(mfFilenameFilter).length;
        int numPlatformMF = libPlatformDir.list(mfFilenameFilter).length;
        if (numFeatureMF == 0 || numPlatformMF == 0)
            throw new IllegalArgumentException(String.format("Some features required by this test are missing"));
        orginialTmpDir = System.getProperty("java.io.tmpdir");
        System.out.println("setUpBeforeClass() java.io.tmpdir set to " + System.getProperty("java.io.tmpdir"));

    }

    private InstallEventListener ieListener;
    private final int state[] = new int[8];
    private final int progress[] = new int[8];

    public InstallEventListener createListener() {
        return new InstallEventListener() {
            @Override
            public void handleInstallEvent(InstallProgressEvent event) {
                switch (event.state) {
                    case InstallProgressEvent.BEGIN:
                        state[0]++;
                        progress[0] = event.progress;
                        break;
                    case InstallProgressEvent.CHECK:
                        state[1]++;
                        progress[1] = event.progress;
                        break;
                    case InstallProgressEvent.POST_INSTALL:
                        state[2]++;
                        progress[2] = event.progress;
                        break;
                    case InstallProgressEvent.CLEAN_UP:
                        state[3]++;
                        progress[3] = event.progress;
                        break;
                    case InstallProgressEvent.COMPLETE:
                        state[4]++;
                        progress[4] = event.progress;
                        break;
                    case InstallProgressEvent.DOWNLOAD:
                        state[5]++;
                        progress[5] = event.progress;
                        break;
                    case InstallProgressEvent.INSTALL:
                        state[6]++;
                        progress[6] = event.progress;
                        break;
                    case InstallProgressEvent.UNINSTALL:
                        state[7]++;
                        progress[7] = event.progress;
                        break;
                    default:
                        break;
                }
            }
        };
    }

    public InstallEventListener getListener() {
        if (ieListener == null) {
            ieListener = createListener();
        }
        return ieListener;
    }

    private void resetCounters() {
        for (int i = 0; i < state.length; i++) {
            state[i] = 0;
            progress[i] = -1;
        }
    }

    private void validateCounters(int expectedState[], int expectedProgress[]) {
        assertEquals("BEGIN state", expectedState[0], state[0]);
        assertEquals("CHECK state", expectedState[1], state[1]);
        assertEquals("POST_INSTALL state", expectedState[2], state[2]);
        assertEquals("CLEAN_UP state", expectedState[3], state[3]);
        assertEquals("COMPLETE state", expectedState[4], state[4]);
        assertEquals("DOWNLOAD state", expectedState[5], state[5]);
        assertEquals("INSTALL state", expectedState[6], state[6]);
        assertEquals("UNINSTALL state", expectedState[7], state[7]);
        assertEquals("BEGIN progress", expectedProgress[0], progress[0]);
        assertEquals("CHECK progress", expectedProgress[1], progress[1]);
        assertEquals("POST_INSTALL progress", expectedProgress[2], progress[2]);
        assertEquals("CLEAN_UP progress", expectedProgress[3], progress[3]);
        assertEquals("COMPLETE progress", expectedProgress[4], progress[4]);
        assertEquals("DOWNLOAD progress", expectedProgress[5], progress[5]);
        assertEquals("INSTALL progress", expectedProgress[6], progress[6]);
        assertEquals("UNINSTALL progress", expectedProgress[7], progress[7]);
    }

    @Test
    public void testInstallKernel_installFeatureWithEmpty() {
        InstallKernel installKernel = InstallKernelFactory.getInstance(imageDir);
        installKernel.addListener(getListener(), InstallConstants.EVENT_TYPE_PROGRESS);
        resetCounters();
        try {
            installKernel.installFeature(new ArrayList<String>(0), "", true, ExistsAction.fail, "userid", "password");
        } catch (Exception e) {
            assertEquals("InstallKernel.installFeature()", "CWWKF1200E: The provided features list is null or empty.", e.getMessage());
        } finally {
            installKernel.removeListener(getListener());
        }
        validateCounters(new int[] { 1, 1, 0, 1, 0, 0, 0, 0 }, new int[] { 0, 1, -1, 98, -1, -1, -1, -1 });

        // to test listener is removed
        resetCounters();
        try {
            installKernel.installFeature(new ArrayList<String>(0), "", true, ExistsAction.fail, "userid", "password");
        } catch (Exception e) {
            assertEquals("InstallKernel.installFeature()", "CWWKF1200E: The provided features list is null or empty.", e.getMessage());
        } finally {
            installKernel.removeListener(getListener());
        }
        validateCounters(new int[] { 0, 0, 0, 0, 0, 0, 0, 0 }, new int[] { -1, -1, -1, -1, -1, -1, -1, -1 });
    }

    /**
     * Uninstall an invalid feature
     */
    @Test
    public void testInstallKernel_uninstallInvalidFeature() {
        InstallKernel installKernel = InstallKernelFactory.getInstance(imageDir);
        try {
            installKernel.uninstallFeaturePrereqChecking("xxx", true, false);
            fail("InstallKernel.uninstallFeature() didn't throw exception.");
        } catch (InstallException e) {
            assertEquals("Uninstall an invalid feature.", "CWWKF1207E: The feature xxx is not installed.", e.getMessage());
        }
    }

    /**
     * Uninstall multiple invalid features
     */
    @Test
    public void testInstallKernel_uninstallInvalidFeatures() {
        InstallKernel installKernel = InstallKernelFactory.getInstance(imageDir);
        ArrayList<String> features = new ArrayList<String>(1);
        features.add("xxx");
        features.add("appSecurity-1.0");
        try {
            installKernel.uninstallFeaturePrereqChecking(features);
            fail("InstallKernel.uninstallFeatures() didn't throw exception.");
        } catch (InstallException e) {
            assertEquals("Uninstall an invalid feature.", "CWWKF1207E: The feature xxx is not installed.", e.getMessage());
        }
    }

    @Test
    public void testInstallKernel_uninstallInvalidFix() throws InstallException {
        InstallKernel installKernel = InstallKernelFactory.getInstance(imageDir);
        installKernel.uninstallFix((String) null);
        try {
            installKernel.uninstallFix("xxx");
        } catch (InstallException e) {
            assertEquals("Uninstall an invalid fix xxx.", "CWWKF1209E: Fix xxx is not installed.", e.getMessage());
        }
    }

    @Test
    public void testInstallKernel_uninstallInvalidFixes() throws InstallException {
        InstallKernel installKernel = InstallKernelFactory.getInstance(imageDir);
        try {
            installKernel.uninstallFix((Collection<String>) null);
            fail("Expected NPE");
        } catch (NullPointerException e) {
        }

        installKernel.addListener(getListener(), InstallConstants.EVENT_TYPE_PROGRESS);
        resetCounters();
        installKernel.uninstallFix(new ArrayList<String>(0));
        installKernel.removeListener(getListener());
        validateCounters(new int[] { 1, 1, 0, 1, 1, 0, 0, 0 }, new int[] { 0, 1, -1, 98, 100, -1, -1, -1 });

        try {
            Collection<String> fixes = new ArrayList<String>(2);
            fixes.add("yyy");
            fixes.add("xxx");
            installKernel.uninstallFix(fixes);
        } catch (InstallException e) {
            assertEquals("Uninstall an invalid fix yyy.", "CWWKF1209E: Fix yyy is not installed.", e.getMessage());
        }
    }

    @Test
    public void testInstallKernel_multipleListeners() {
        InstallKernel installKernel = InstallKernelFactory.getInstance(imageDir);
        installKernel.addListener(getListener(), InstallConstants.EVENT_TYPE_PROGRESS);
        installKernel.addListener(createListener(), InstallConstants.EVENT_TYPE_PROGRESS);
        installKernel.addListener(createListener(), null);
        installKernel.addListener(createListener(), "");
        installKernel.addListener(createListener(), "Unknown");
        installKernel.addListener(null, InstallConstants.EVENT_TYPE_PROGRESS);
        resetCounters();
        try {
            installKernel.installFeature(new ArrayList<String>(0), "", true, ExistsAction.fail, "userid", "password");
        } catch (Exception e) {
            assertEquals("InstallKernel.installFeature()", "CWWKF1200E: The provided features list is null or empty.", e.getMessage());
        } finally {
            installKernel.removeListener(getListener());
        }
        validateCounters(new int[] { 2, 2, 0, 2, 0, 0, 0, 0 }, new int[] { 0, 1, -1, 98, -1, -1, -1, -1 });

        // to test listener is removed
        resetCounters();
        try {
            installKernel.installFeature(new ArrayList<String>(0), "", true, ExistsAction.fail, "userid", "password");
        } catch (Exception e) {
            assertEquals("InstallKernel.installFeature()", "CWWKF1200E: The provided features list is null or empty.", e.getMessage());
        } finally {
            installKernel.removeListener(getListener());
        }
        validateCounters(new int[] { 1, 1, 0, 1, 0, 0, 0, 0 }, new int[] { 0, 1, -1, 98, -1, -1, -1, -1 });
    }

    @Test
    public void testInstallKernel_getInstalledLicense() {
        InstallKernel installKernel = InstallKernelFactory.getInstance(imageDir);
        assertEquals("InstallKernel.getInstalledLicense()", 1, installKernel.getInstalledLicense().size());
    }

    @Test
    public void testInstallKernel_getInstalledFeatures() {
        InstallKernel installKernel = InstallKernelFactory.getInstance(imageDir);
        Set<String> features = installKernel.getInstalledFeatures(InstallConstants.TO_CORE);
        assertEquals("InstallKernel.getInstalledFeatures(\"core\")", 86, features.size());
        assertTrue("InstallKernel.getInstalledFeatures(\"core\") contains com.ibm.websphere.appserver.logging-1.0", features.contains("com.ibm.websphere.appserver.logging-1.0"));
        features = installKernel.getInstalledFeatures(InstallConstants.TO_USER);
        assertEquals("InstallKernel.getInstalledFeatures(\"usr\")", 0, features.size());
        features = installKernel.getInstalledFeatures("extension");
        assertEquals("InstallKernel.getInstalledFeatures(\"extension\")", 0, features.size());
    }

    @Test
    public void testInstallLocalFeatureESAFileNotExist() {
        InstallKernel installKernel = InstallKernelFactory.getInstance(imageDir);
        try {
            ((InstallKernelImpl) installKernel).installLocalFeature("unknown", "", true, ExistsAction.fail);
            fail("InstallKernel.installLocalFeature() did not throw exception");
        } catch (InstallException e) {
            assertEquals("InstallKernel.installLocalFeature() should throw exception",
                         "CWWKF1009E: The file unknown does not exist.",
                         e.getMessage());
        }
    }

    @Test
    public void testInstallLocalFeatureESAInvalidURL() {
        InstallKernel installKernel = InstallKernelFactory.getInstance(imageDir);
        try {
            ((InstallKernelImpl) installKernel).installLocalFeature("htp://unknown", "", true, ExistsAction.fail);
            fail("InstallKernel.installLocalFeature() did not throw exception");
        } catch (InstallException e) {
            if (!e.getMessage().contains("CWWKF1009E")) {
                outputMgr.failWithThrowable("testInstallLocalFeatureESAInvalidURL", e);
            }
        }
    }

    @Test
    public void testInstallLocalFeatureESAURLNotExist() {
        InstallKernel installKernel = InstallKernelFactory.getInstance(imageDir);
        try {
            ((InstallKernelImpl) installKernel).installLocalFeature("http://www.cik.unknown.com/cik.user.esa", "", true, ExistsAction.fail);
            fail("InstallKernel.installLocalFeature() did not throw exception");
        } catch (InstallException e) {
            // For some platforms which may fail at create temp file before download, check CWWKF1008E too
            if (!e.getMessage().contains("CWWKF1008E") && !e.getMessage().contains("CWWKF1007E")) {
                outputMgr.failWithThrowable("testInstallLocalFeatureESAURLNotExist", e);
            }
        }
    }

    //Disabled for not using java tmp to download
    //@Test
    public void testInstallLocalFeatureFailedCreateTemp() {
        InstallKernel installKernel = InstallKernelFactory.getInstance(imageDir);
        System.out.println("testInstallLocalFeatureFailedCreateTemp() java.io.tmpdir is " + System.getProperty("java.io.tmpdir"));
        System.setProperty("java.io.tmpdir", "./invalidPath");
        System.out.println("testInstallLocalFeatureFailedCreateTemp() java.io.tmpdir set to " + System.getProperty("java.io.tmpdir"));
        try {
            ((InstallKernelImpl) installKernel).installLocalFeature("http://www.cik.unknown.com/cik.user.esa", "", true, ExistsAction.fail);
            fail("InstallKernel.installLocalFeature() did not throw exception");
        } catch (InstallException e) {
            // For some platforms which cannot set the java.io.tmpdir, check CWWKF1007E
            if (!e.getMessage().contains("CWWKF1008E") && !e.getMessage().contains("CWWKF1007E")) {
                outputMgr.failWithThrowable("testInstallLocalFeatureFailedCreateTemp", e);
            }
        } finally {
            System.setProperty("java.io.tmpdir", orginialTmpDir);
            System.out.println("testInstallLocalFeatureFailedCreateTemp() java.io.tmpdir finally set to " + System.getProperty("java.io.tmpdir"));
        }
    }

    private void verifyContains(List<String> strings, String[] expected) {
        for (String s : expected) {
            if (!strings.contains(s))
                fail("\"" + s + "\" is missing in the stdout or stderr.");
        }
    }
}
