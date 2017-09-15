/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.cloudfoundry.util;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class CloudProperties {

    private ResourceBundle properties;

    /*
     * cf.buildpack https://github.com/cloudfoundry/ibm-websphere-liberty-buildpack poss add .git on the end
     * cf.command /usr/bin/cf /usr/bin/X11/cf
     * cf.host stage1.ng.bluemix.net
     * cf.password password here
     * cf.username charcoch@uk.ibm.com
     */

    public CloudProperties() {
        try {
            properties = ResourceBundle.getBundle("com.ibm.cf.generated.cloud");
        } catch (MissingResourceException e) {
            System.err.println("Could not find the resource bundle. This is probably because the test-util project hasn't been built with ant yet. Either build test-util with ant, or set environment variables with the required properties.");

            // Make an empty resource bundle so we don't NPE trying to read
            // stuff out of it
            properties = new ResourceBundle() {
                private final Enumeration<String> keys = new Hashtable<String, String>()
                                .elements();

                @Override
                public Enumeration<String> getKeys() {
                    return keys;
                }

                @Override
                protected Object handleGetObject(String key) {
                    return null;
                }
            };
        }
    }

    /**
     * Returns a string which represents the fully qualified path to a cf
     * executable.
     */
    public String getCfCommand() {
        String cmd = properties.getString("cf.command");

        if (cmd == null) {
            String rubyLoc = getEnvironmentVariable("RUBY_HOME");
            rubyLoc = rubyLoc.replace("C:", "");

            final String cf;
            if (System.getProperty("os.name").contains("Win")) {
                cf = "cf.exe";
            } else {
                cf = "cf";
            }
            cmd = rubyLoc + System.getProperty("file.separator") + cf;
        }

        if (cmd.contains(" ")) {
            cmd = "\"" + cmd + "\"";
        }

        return cmd;

    }

    /**
     * Will always return non-null; if the environment variable is not set, will
     * return the name instead.
     */
    private String getEnvironmentVariable(String name) {
        String env = System.getenv(name);
        if (env == null) {
            // Set a value for the null variable that indicates what we were
            // looking for
            env = name;
        }
        return env;
    }

//    public File getLocalWLPHome() {
//        String home = properties.getString("liberty.dir");
//        if (home == null) {
//            home = getEnvironmentVariable("LIBERTY_HOME");
//        }
//        return new File(home);
//    }

    /**
     * @return
     */
    public String getHost() {
        String host = properties.getString("cf.host");
        if (host == null) {
            host = getEnvironmentVariable("CF_HOST");
        }
        return host;
    }

    public String getUserName() {
        String user = properties.getString("cf.username");
        if (user == null) {
            user = getEnvironmentVariable("CF_USERNAME");
        }
        return user;
    }

    public String getPassword() {
        String password = properties.getString("cf.password");
        if (password == null) {
            password = getEnvironmentVariable("CF_PASSWORD");
        }
        return password;
    }

    public String getBuildpack() {
        String buildpack = properties.getString("cf.buildpack");
        if (buildpack == null) {
            buildpack = getEnvironmentVariable("CF_BUILDPACK");
        }
        return buildpack;
    }

    /**
     * @return
     */
    public int getPort() {
        // TODO Return a sensible value, once we figure out where to get it from
        return 8080;
    }

}
