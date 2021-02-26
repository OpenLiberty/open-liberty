/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.common;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * The SharedLocationManager primes the configured locations used by
 * the WsLocationAdmin service and the WsLocationAdminManager. When used in
 * unit test, it allows your test code to drive methods that rely on
 * a configured WsLocationAdmin service or the static WsLocationAdminManager.
 *
 * <h2>Unit Testing</h2>
 * <p>
 * Our unit test cases need to check for the presence of files, etc. in known
 * locations. There is one set of methods providing an abbreviated tree, and
 * another that replicates the default server tree structure, both starting at
 * specified root directoy
 *
 * <h2>Component Testing</h2>
 * <p>
 * The JUnit drivers for our component tests need to check for the presence of
 * files, etc. in the relevant server directory in the target image.
 *
 */
public class SharedLocationManager {
    /**
     * Used in test: construct and replace the singleton instance of the
     * WsLocationAdmin service/manager based on the provided root directory. This
     * will create a full replica of the default server install directory,
     * underneath the provided root directory (instead of wlp/).
     * <p>
     * Directories will not be accessed/created until requested.
     *
     * @param rootDirStr
     *                       Root directory for structure shown above
     * @param serverName
     *                       Name of server
     * @param map
     *                       String-to-Object map containing additional properties that should
     *                       be passed to the location service. The following properties are
     *                       set by this method (and will be overwritten): wlp.install.dir,
     *                       wlp.user.dir, wlp.lib.dir, wlp.server.name
     *
     * @return the configured WsLocationAdmin instance.
     */
    public static Object createDefaultLocations(String rootDirStr, String serverName, Map<String, Object> map) {
        return createDefaultLocations(rootDirStr, serverName, map, false);
    }

    /**
     * Used in test: construct and replace the singleton instance of the
     * WsLocationAdmin service/manager based on the provided root directory. This
     * will create a full replica of the default server install directory,
     * underneath the provided root directory (instead of wlp/).
     * <p>
     * Directories will not be accessed/created until requested.
     *
     * @param rootDirStr
     *                       Root directory for structure shown above
     * @param serverName
     *                       Name of server
     * @param map
     *                       String-to-Object map containing additional properties that should
     *                       be passed to the location service. The following properties are
     *                       set by this method (and will be overwritten): wlp.install.dir,
     *                       wlp.user.dir, wlp.lib.dir, wlp.server.name
     * @param isClient
     *                       boolean value to determine whether this call is used for client or server.
     *
     * @return the configured WsLocationAdmin instance.
     */
    public static Object createDefaultLocations(String rootDirStr, String serverName, Map<String, Object> map, boolean isClient) {
        if (rootDirStr == null || rootDirStr.length() <= 0)
            throw new IllegalArgumentException("createDefaultLocations must be called with a non-null/non-empty string specifying the root directory");

        File root = new File(rootDirStr);
        if (root == null || !root.exists())
            throw new IllegalArgumentException("createDefaultLocations must be called with an existing root directory");

        File lib = new File(root, "lib");
        File usr = new File(root, "usr");

        lib.mkdir();
        usr.mkdir();

        if (map == null)
            map = new HashMap<String, Object>();
        if (serverName == null)
            serverName = "defaultServer";

        map.put("wlp.user.dir", usr.getAbsolutePath() + '/');
        map.put("wlp.lib.dir", lib.getAbsolutePath() + '/');
        map.put("wlp.server.name", serverName);

        // Defined in BootstrapConstants.PROCESS_TYPE_CLIENT
        // Defined in BootstrapConstants.PROCESS_TYPE_SERVER
        map.put("wlp.process.type", isClient ? "client" : "server");

        map.put("wlp.svc.binding.root", usr.getAbsolutePath() + File.separator + "bindings");
        Class<?> impl = getLocServiceImpl();
        resetWsLocationAdmin();

        Method m;
        try {
            m = impl.getDeclaredMethod("createLocations", Map.class);
            m.setAccessible(true);
            m.invoke(null, map);
        } catch (InvocationTargetException ite) {
            System.err.println("Woops! Something is amiss-- could not configure test locations");
            Throwable cause = ite.getCause();
            cause.printStackTrace();
            throw new java.lang.InstantiationError("Unable to configure test locations");
        } catch (Exception e) {
            System.err.println("Woops! Something is amiss-- could not configure test locations");
            e.printStackTrace();
            throw new java.lang.InstantiationError("Unable to configure test locations");
        }

        return getLocationInstance();
    }

    /**
     * Used in test: construct and replace the singleton instance of the
     * WsLocationAdmin service/manager based on the provided root directory. The
     * server directory name will be the provided server name. The config root
     * document
     * will be the default "rootDir/usr/servers/serverName/server.xml" (does not
     * need to exist).
     *
     * @param rootDirStr
     *                       Root directory for structure shown above
     * @param serverName
     *                       Name of server
     *
     * @return the configured WsLocationAdmin instance.
     * @see #createDefaultLocations(String, String, Map)
     */
    public static Object createDefaultLocations(String rootDirStr, String serverName) {
        return createDefaultLocations(rootDirStr, serverName, null);
    }

    /**
     * Used in test: construct and replace the singleton instance of the
     * WsLocationAdmin service/manager based on the provided root directory. The
     * server directory name will be "defaultServer". The config root document
     * will be the default "rootDir/usr/servers/defaultServer/server.xml" (does
     * not need to exist).
     *
     * @param rootDirStr
     *                       Root directory for structure shown above
     *
     * @return the configured WsLocationAdmin instance.
     * @see #createDefaultLocations(String, String, Map)
     */
    public static Object createDefaultLocations(String rootDirStr) {
        return createDefaultLocations(rootDirStr, null, null);
    }

    /**
     * Used in test: construct and replace the singleton instance of the
     * WsLocationAdmin service/manager based on the provided root directory. This
     * will create/look for the following directory structure:
     *
     * <pre>
     * rootDir/
     * applications/ (shared.app.dir)
     * bundle/
     * getBundleFile()
     * configuration/ (does not match location in server)
     * getResource(&quot;config|feature&quot;, ...)
     * lib/ (does not match location in server)
     * getResource(&quot;bundle|platform|feature&quot;, ...)
     * reposiory/ (shared.repo.dir)
     * resources/ (shared.resource.dir)
     * &gt;serverName&lt;/
     * getServerFile()
     * workarea/
     * getServerWorkareaFile()
     * </pre>
     *
     * Directories will not be accessed/created until requested.
     *
     * @param rootDirStr
     *                       Root directory for structure shown above
     * @param serverName
     *                       Name of server
     * @param map
     *                       String-to-Object map containing additional properties that should
     *                       be passed to the location service.
     * @return the configured WsLocationAdmin instance.
     */
    public static Object createLocations(String rootDirStr, String serverName, Map<String, Object> map) {
        if (rootDirStr == null || rootDirStr.length() <= 0)
            throw new IllegalArgumentException("createLocations must be called with a non-null/non-empty string specifying the root directory");

        File root = new File(rootDirStr);
        if (root == null || !root.exists())
            throw new IllegalArgumentException("createLocations must be called with an existing root directory");

        File lib = new File(root, "lib");
        lib.mkdir();

        if (map == null)
            map = new HashMap<String, Object>();
        if (serverName == null)
            serverName = "defaultServer";

        map.put("wlp.user.dir", rootDirStr + '/');
        map.put("wlp.lib.dir", lib.getAbsolutePath() + '/');
        map.put("wlp.server.name", serverName);

        Class<?> impl = getLocServiceImpl();
        resetWsLocationAdmin();

        Method m;
        try {
            m = impl.getDeclaredMethod("createLocations", Map.class);
            m.setAccessible(true);
            m.invoke(null, map);
        } catch (InvocationTargetException ite) {
            System.err.println("Woops! Something is amiss-- could not configure test locations");
            Throwable cause = ite.getCause();
            cause.printStackTrace();
            throw new java.lang.InstantiationError("Unable to configure test locations");
        } catch (Exception e) {
            System.err.println("Woops! Something is amiss-- could not configure test locations");
            e.printStackTrace();
            throw new java.lang.InstantiationError("Unable to configure test locations");
        }

        return getLocationInstance();
    }

    /**
     * Used in test: construct and replace the singleton instance of the
     * WsLocationAdmin service/manager based on the provided root directory. The
     * config root document will be the default "rootDir/serverName/server.xml"
     * (does not need to exist).
     *
     * @param rootDirStr
     *                       Root directory for structure shown above
     * @param serverName
     *                       Name of server
     * @return the configured WsLocationAdmin instance.
     * @see #createLocations(String, String, Map)
     */
    public static Object createLocations(String rootDirStr, String serverName) {
        return createLocations(rootDirStr, serverName, null);
    }

    /**
     * Used in test: construct and replace the singleton instance of
     * the WsLocationAdmin service/manager based on the provided root
     * directory. The server directory name will be "defaultServer".
     * The config root document will be the default
     * "rootDir/defaultServer/server.xml" (does not need to exist).
     *
     * @param rootDirStr
     *                       Root directory for structure shown above
     * @see #createLocations(String, String, Map)
     */
    public static Object createLocations(String rootDirStr) {
        return createLocations(rootDirStr, null, null);
    }

    /**
     * For use with component tests: Populate an WsLocationAdmin instance with
     * paths pointing to the install image directory using the "image.dir" system
     * property so that the test clients can check for the presence of files, etc.
     *
     * @param serverName
     *                       Name of server
     * @param map
     *                       String-to-Object map containing additional properties that should
     *                       be passed to the location service.
     * @return the configured WsLocationAdmin instance.
     */
    public static Object createImageLocations(String serverName, Map<String, Object> map) {
        if (serverName == null || serverName.length() <= 0)
            throw new IllegalArgumentException("createImageLocations must be called with a non-null/non-empty string specifying the server name");

        String installName = System.getProperty("install.name", "wlp");
        String imageDir = System.getProperty("image.dir");
        if (imageDir == null || imageDir.length() <= 0) {
            imageDir = "../build.image/" + installName;
            System.setProperty("install.dir", imageDir);
            System.out.println("install.dir system property not set: using " + imageDir);
        }

        File root = new File(imageDir);
        if (root == null || !root.exists())
            throw new IllegalArgumentException("createLocations must be called with an existing root directory. Could not find " + root.getAbsolutePath());

        File lib = new File(root, "lib");
        if (lib == null || !lib.exists())
            throw new IllegalArgumentException("createLocations must be called with an existing lib directory. Could not find " + lib.getAbsolutePath());

        if (map == null)
            map = new HashMap<String, Object>();

        map.put("wlp.lib.dir", lib.getAbsolutePath() + '/');
        map.put("wlp.user.dir", root.getAbsolutePath() + "/usr/");

        map.put("wlp.server.name", serverName);

        Class<?> impl = getLocServiceImpl();
        resetWsLocationAdmin();

        Method m;
        try {
            m = impl.getDeclaredMethod("createLocations", Map.class);
            m.setAccessible(true);
            m.invoke(null, map);
        } catch (InvocationTargetException ite) {
            System.err.println("Woops! Something is amiss-- could not configure test locations");
            Throwable cause = ite.getCause();
            cause.printStackTrace();
            throw new java.lang.InstantiationError("Unable to configure test locations");
        } catch (Exception e) {
            System.err.println("Woops! Something is amiss-- could not configure test locations");
            e.printStackTrace();
            throw new java.lang.InstantiationError("Unable to configure test locations");
        }

        return getLocationInstance();
    }

    /**
     * Used in test: construct and replace the singleton instance of the
     * WsLocationAdmin service/manager with the location of the build
     * image and the provided server name (e.g.
     * build.image/wlp/usr/servers/serverName).
     *
     * Will use the default configuration file(s) contained in the server
     * directory (if present, both are optional):
     * <ul>
     * <li>build.image/wlp/usr/servers/serverName/server.xml,</li>
     * <li>build.image/wlp/usr/servers/serverName/bootstrap.properties</li>
     * </ul>
     *
     * @param serverName
     *                       Name of server
     * @return the configured WsLocationAdmin instance.
     * @see #createImageLocations(String, String)
     */
    public static Object createImageLocations(String serverName) {
        return createImageLocations(serverName, null);
    }

    /**
     * Clear/reset locations
     */
    public static void resetWsLocationAdmin() {
        Class<?> impl = getLocServiceImpl();

        Field f;
        try {
            f = impl.getDeclaredField("instance");
            f.setAccessible(true);
            f.set(null, null);
        } catch (Exception e) {
            System.err.println("Woops! Something is amiss; could not reset test locations");
            e.printStackTrace();
            throw new java.lang.InstantiationError("Unable to reset test locations");
        }
    }

    /**
     * Return the instance of the WsLocationAdminManager for use with jmock
     * objects:
     *
     * <pre>
     * final ServiceReference mockServiceRef = context.mock(ServiceReference.class);
     * final BundleContext mockBundleContext = context.mock(BundleContext.class);
     * final WsLocationAdmin location = (WsLocationAdmin) SharedLocationManager.getLocationInstance();
     *
     * context.checking(new Expectations() {
     *     {
     *         one(mockBundleContext).getServiceReference(with(WsLocationAdmin.class.getName()));
     *         will(returnValue(mockServiceRef));
     *         one(mockBundleContext).getService(mockServiceRef);
     *         will(returnValue(location));
     *     }
     * });
     * </pre>
     *
     * @return the configured WsLocationAdmin instance.
     */
    public static Object getLocationInstance() {
        Class<?> impl = getLocServiceImpl();

        Method m;
        try {
            m = impl.getDeclaredMethod("getInstance");
            return m.invoke(null);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof IllegalStateException) {
                return null;
            }
            System.err.println("Woops! Something is amiss; could not get WsLocationAdmin instance");
            e.printStackTrace();
            throw new java.lang.InstantiationError("Unable to get test WsLocationAdmin instance");
        } catch (Exception e) {
            System.err.println("Woops! Something is amiss; could not get WsLocationAdmin instance");
            e.printStackTrace();
            throw new java.lang.InstantiationError("Unable to get test WsLocationAdmin instance");
        }
    }

    /**
     * Print out configured locations
     *
     * @return Formatted string describing configured locations
     */
    public static String debugConfiguredLocations() {
        Class<?> impl = getLocServiceImpl();
        Object instance = getLocationInstance();

        Method m;
        String s;

        try {
            m = impl.getDeclaredMethod("printLocations", boolean.class);
            s = (String) m.invoke(instance, true);
        } catch (Exception e) {
            System.err.println("Woops! Something is amiss; could not configure test locations");
            e.printStackTrace();
            throw new IllegalStateException("Unable to configure test locations");
        }

        return s;
    }

    private static Class<?> locSvcImplClass = null;

    private static Class<?> getLocServiceImpl() {
        if (locSvcImplClass != null)
            return locSvcImplClass;

        try {
            locSvcImplClass = Class.forName("com.ibm.ws.kernel.service.location.internal.WsLocationAdminImpl");
            return locSvcImplClass;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Add the kernel service jar/project to your classpath");
        }
    }

}
