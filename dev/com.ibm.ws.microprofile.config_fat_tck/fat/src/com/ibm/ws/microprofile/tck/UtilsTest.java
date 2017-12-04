/**
 *
 */
package com.ibm.ws.microprofile.tck;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import org.junit.Test;

/**
 *
 */
public class UtilsTest {

    String wlp = "/libertyGit/open-liberty/dev/build.image/wlp";

    /**
     * Tests resolution of
     * systemPath>${api.stable}com.ibm.websphere.org.eclipse.microprofile.config.${mpconfig.version}_${mpconfig.bundle.version}.${version.qualifier}.jar</systemPath>
     */
    @Test
    public void testFindCnf1() {
        String jarName = "com.ibm.ws.microprofile.config";
        String expectedPath = wlp + "/lib/com.ibm.ws.microprofile.config_1.0.18.jar";
        checkFind(jarName, expectedPath);
    }

    /**
     * Tests resolution of <systemPath>${lib}com.ibm.ws.microprofile.config_${liberty.version}.jar</systemPath>
     */
    @Test
    public void testFindCnf2() {
        String jarName = "com.ibm.websphere.org.eclipse.microprofile.config";
        String expectedPath = wlp + "/dev/api/stable/com.ibm.websphere.org.eclipse.microprofile.config.1.1_1.2.18.jar";
        checkFind(jarName, expectedPath);
    }

    /**
     * Test resolution of <systemPath>${lib}com.ibm.ws.microprofile.config.cdi_${liberty.version}.jar</systemPath>
     */
    @Test
    public void testFindCnfCdi1() {
        String jarName = "com.ibm.ws.microprofile.config.cdi";
        String expectedPath = wlp + "/lib/com.ibm.ws.microprofile.config.cdi_1.0.18.jar";
        checkFind(jarName, expectedPath);
    }

    /**
     * @param jarName
     * @param expectedPath
     */
    private void checkFind(String jarName, String expectedPath) {
        String path = genericResolveJarPath(jarName, wlp);
        assertEquals(expectedPath, path);
    }

    /**
     * @param jarName
     * @param wlpPathName
     * @return
     */
    public static String genericResolveJarPath(String jarName, String wlpPathName) {
        String dev = wlpPathName + "/dev/";
        String api = dev + "api/";
        String spi = dev + "spi/";
        String apiSpec = api + "spec/";
        String apiStable = api + "stable/";
        String apiThirdParty = api + "third-party/";
        String apiIbm = api + "ibm/";
        String spiSpec = spi + "spec/";
        String spiThirdParty = spi + "third-party/";
        String spiIbm = spi + "ibm/";
        String lib = wlpPathName + "/lib/";

        ArrayList<String> places = new ArrayList<String>();
        places.add(apiStable);
        places.add(lib);

        String jarPath = null;
        for (Iterator<String> iterator = places.iterator(); iterator.hasNext();) {
            String dir = iterator.next();
            jarPath = jarPathInDir(jarName, dir);
            if (jarPath != null) {
                break;
            }
        }
        return jarPath;
    }

    /**
     * @param jarPath
     * @param dir
     * @return
     */
    public static String jarPathInDir(String jarPath, String dir) {
        String result = null;
        File d = new File(dir);
        String[] files = d.list();
        System.out.println("GDH P " + jarPath + " " + dir);

        for (int i = 0; i < files.length; i++) {
            System.out.println("GDH" + files[i]);
            if (files[i].contains(jarPath)) {
//              <systemPath>${api.stable}com.ibm.websphere.org.eclipse.microprofile.config.${mpconfig.version}_${mpconfig.bundle.version}.${version.qualifier}.jar</systemPath>
//              <systemPath>${lib}com.ibm.ws.microprofile.config_${liberty.version}.jar</systemPath>
//              <systemPath>${lib}com.ibm.ws.microprofile.config.cdi_${liberty.version}.jar</systemPath>
                result = files[i];
                // We do not want to allow any prefixes or postfixes that contain letters.
                String r = result.replaceAll(".jar", "").replaceAll("[^a-zA-Z]", "");
                System.out.println("GDH r" + r);
                if (r.length() == 0) {
                    return result;
                }
            }
        }
        return result;
    }

}
