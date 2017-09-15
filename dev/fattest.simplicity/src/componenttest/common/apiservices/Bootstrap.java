/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.common.apiservices;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import com.ibm.websphere.simplicity.ConnectionInfo;
import com.ibm.websphere.simplicity.configuration.PropertiesConfigurationProvider;
import com.ibm.websphere.simplicity.log.Log;

public class Bootstrap {

    private static Class<?> c = Bootstrap.class;
    private static Bootstrap instance;

    private PropertiesConfigurationProvider config;

    /**
     * Get the default Bootstrap instance, or the cached one if obtained previously.
     *
     * @return
     * @throws Exception
     */
    public static Bootstrap getInstance() throws Exception {
        if (instance == null)
            instance = new Bootstrap();
        return instance;
    }

    /**
     * Get the Bootstrap instance for the file, or the cached one if obtained previously. <br>
     * <b><em>Note:</em></b> if bootstrap is already cached, file argument is ignored.
     *
     * @return
     * @throws Exception
     */
    public static Bootstrap getInstance(File file) throws Exception {
        if (instance == null)
            instance = new Bootstrap(file);
        return instance;
    }

    /**
     * Get a Bootstrap instance that corresponds to the file passed, do not cache the response.
     *
     * @param file properties for the bootstrap
     * @return BootStrap instance to correspond to the input file.
     * @throws Exception
     */
    public static Bootstrap getSpecialInstance(File file) throws Exception {
        return new Bootstrap(file);
    }

    File file;

    private Bootstrap() throws Exception {
        this(null);
    }

    private Bootstrap(File file) throws Exception {
        Log.entering(c, "<ctor>", file);
        this.file = file; // first, try the input File
        if (this.file == null || !this.file.exists()) {
            // if the input file is no good, try the system property
            String path = System.getProperty("bootstrapping.properties");
            if (path != null) {
                this.file = new File(path);
                System.out.println("Trying system property: " + this.file.getAbsolutePath());
            }
            if (this.file == null || !this.file.exists()) {
                // if the system property's file is no good, try the user
                // directory
                this.file = new File(System.getProperty("user.dir")
                                     + "/bootstrapping.properties");
                System.out.println("Trying user.dir: " + this.file.getAbsolutePath());
                if (this.file == null || !this.file.exists()) {
                    // if the user.dir's file is no good, try the current
                    // working directory
                    this.file = new File("bootstrapping.properties");
                    System.out.println("Trying current working dir: " + this.file.getAbsolutePath());
                }
                if (this.file == null || !this.file.exists()) {
                    //This could be a tWAS FAT suite so finally check in the local.properties
                    Properties localProps = new Properties();
                    String props = System.getProperty("local.properties");
                    if (props != null) {
                        FileInputStream in = new FileInputStream(props);
                        localProps.load(in);
                        in.close();
                        this.file = new File(localProps.getProperty("bootstrapping.properties"));
                    }
                }
                if (this.file == null || !this.file.exists())
                    generateDefaultBootstrapFile();
            }
        }
        init();
        Log.exiting(c, "<ctor>");
    }

    /**
     * Only works for Liberty installations.
     *
     * @return Path to the server.
     */
    public List<String> getServerKeys() {
        Log.entering(c, "getServerKeys");
        List<String> ret = findKeys("liberty.");
        Log.exiting(c, "getServerKeys", ret);
        return ret;
    }

    public File getFile() {
        return this.file;
    }

    public String getValue(String key) {
        Log.entering(c, "getValue", key);
        String ret = config.getProperty(key);
        Log.exiting(c, "getValue", ret);
        return ret;
    }

    public void setValue(String key, String value) throws Exception {
        config.setProperty(key, value);
        config.writeProperties();
    }

    public ConnectionInfo getMachineConnectionData(String hostname) throws Exception {
        String method = "getMachineConnectionData";
        Log.entering(c, method, hostname);
        String user = null;
        String password = null;

        boolean exact = config.hasProperty(hostname + ".user");
        Log.finer(c, method, "Found exact match: " + exact);
        user = config.getProperty(hostname + ".user");
        password = config.getProperty(hostname + ".password");

        Log.finer(c, method, "Values", new Object[] { "User: " + user,
                                                      "Password: " + password });

        ConnectionInfo ret = null;
        if (user == null) {
            //Assume a username is not required and just default it.
            //This is required for tWAS FAT suites as the user and pword aren't
            //set in the bootstrapping.properties when security is off
            //and the tWAS FAT suites require them
            ret = new ConnectionInfo(hostname, "USERNAME", "PASSWORD");
        } else {
            if (password != null)
                ret = new ConnectionInfo(hostname, user, password);
            else {
                String keystorePath = config.getProperty("keystore");
                Log.finer(c, method, "Keystore: " + keystorePath);
                File f = new File(keystorePath);
                ret = new ConnectionInfo(hostname, f, user, password);
            }
        }

        Log.exiting(c, method, ret);
        return ret;
    }

    private void generateDefaultBootstrapFile() throws Exception {
        final String method = "generateDefaultBootstrapFile";

        Log.finer(c, method, "Creating auto-generated bootstrapping properties.");

        Properties props = new Properties();
        props.setProperty("hostName", "localhost");
        props.setProperty("HOSTNAME.user", "USER");
        props.setProperty("HOSTNAME.password", "PASSWORD");
        props.setProperty("local.sharedLib", "../../test_prereqs/moonstone/sharedlibraries");
        props.setProperty("junit_jar", "../../ant_build/lib");
        props.setProperty("local.java", "../../fattest.simplicity");
        props.setProperty("shared.resources", "../../build.sharedResources/lib");
        props.setProperty("localhost.JavaHome", System.getProperty("java.home"));

        // Try a couple options in order to generate a libertyInstallPath
        String wlpInstallDir = null;
        if (new File("../build.image/wlp").exists())
            wlpInstallDir = "../build.image/wlp";
        if (wlpInstallDir == null)
            wlpInstallDir = System.getProperty("wlp.install.dir");
        if (wlpInstallDir == null)
            wlpInstallDir = "<path to wlp install dir>"; // this will fail later, but the error will be clear
        props.setProperty("libertyInstallPath", wlpInstallDir);
        Log.finer(c, method, "Using libertyInstallPath of: " + wlpInstallDir);

        OutputStream output = new FileOutputStream("bootstrapping.properties");
        try {
            props.store(output, "Auto-generated properties by " + c.getCanonicalName());
        } finally {
            output.close();
        }
    }

    private void init() throws Exception {
        Log.entering(c, "init");
        config = new PropertiesConfigurationProvider(file);
        Log.exiting(c, "init", config);
    }

    /**
     * Get a List of unique keys that begin with keyStart
     *
     * @param keyStart
     *            The prefix of the key to find
     * @return A List of unique keys that begin with keyStart
     */
    private List<String> findKeys(String keyStart) {
        List<String> keyList = new ArrayList<String>();
        Enumeration<?> propNames = config.getPropertyNames();
        while (propNames.hasMoreElements()) {
            String next = (String) propNames.nextElement();
            if (next.startsWith(keyStart)) {
                String suffix = next.substring(keyStart.length());
                String key = keyStart
                             + suffix.substring(0, suffix.indexOf(BootstrapProperty.PROPERTY_SEP.toString()));
                if (!keyList.contains(key)) {
                    keyList.add(key);
                }
            }
        }
        return keyList;
    }

    @Override
    public String toString() {
        return "Bootstrap properties: " + getFile();
    }

}
