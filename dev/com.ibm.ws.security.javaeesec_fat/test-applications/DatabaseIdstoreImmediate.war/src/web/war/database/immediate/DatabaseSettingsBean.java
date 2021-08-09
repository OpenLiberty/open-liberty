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

package web.war.database.immediate;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.security.enterprise.identitystore.IdentityStore.ValidationType;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.javaeesec.JavaEESecConstants;

/**
 * This bean will read DB identity store configuration settings from a well-known file
 * allowing tests to update the DB identity store dynamically by simply updating the
 * well-known file.
 */
@Named
@ApplicationScoped
public class DatabaseSettingsBean {

    private static final String CLASS_NAME = DatabaseSettingsBean.class.getName();

    private Properties props;

    public DatabaseSettingsBean() {}

    public String getCallerQuery() throws IOException {
        refreshConfiguration();
        String prop = getProperty(JavaEESecConstants.CALLER_QUERY);
        System.out.println(CLASS_NAME + ".getCallerQuery() returns: " + prop);
        return prop;
    }

    public String getDataSourceLookup() throws IOException {
        refreshConfiguration();
        String prop = getProperty(JavaEESecConstants.DS_LOOKUP);
        System.out.println(CLASS_NAME + ".getDataSourceLookup() returns: " + prop);
        return prop;
    }

    public String getGroupsQuery() throws IOException {
        refreshConfiguration();
        String prop = getProperty(JavaEESecConstants.GROUPS_QUERY);
        System.out.println(CLASS_NAME + ".getGroupsQuery() returns: " + prop);
        return prop;
    }

    public Object getHashAlgorithmParameters() throws IOException {
        refreshConfiguration();

        String prop = getProperty(JavaEESecConstants.PWD_HASH_PARAMETERS);
        Object result = null;
        if (prop != null) {

            if (prop.startsWith("[") && prop.endsWith("]")) {
                result = prop.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\s", "").split(",");
            } else if (prop.startsWith("{") && prop.endsWith("}")) {
                result = Stream.of(prop.replaceAll("\\{", "").replaceAll("\\}", "").replaceAll("\\s", "").split(","));
            }
        }

        System.out.println(CLASS_NAME + ".getHashAlgorithmParameters() returns: " + result);
        return result;
    }

    public Integer getPriority() throws IOException {
        refreshConfiguration();

        String prop = getProperty(JavaEESecConstants.PRIORITY);
        Integer result = null;
        if (prop != null) {
            result = Integer.valueOf(prop);
        }

        System.out.println(CLASS_NAME + ".getPriority() returns: " + result);
        return result;
    }

    public ValidationType[] getUseFor() throws IOException {
        refreshConfiguration();

        Set<ValidationType> resultsSet = new HashSet<ValidationType>();

        String prop = getProperty(JavaEESecConstants.USE_FOR);
        if (prop != null) {
            if (prop.contains("VALIDATE")) {
                resultsSet.add(ValidationType.VALIDATE);
            }
            if (prop.contains("PROVIDE_GROUPS")) {
                resultsSet.add(ValidationType.PROVIDE_GROUPS);
            }
        }
        ValidationType[] results = null;
        if (resultsSet.size() > 0) {
            results = resultsSet.toArray(new ValidationType[resultsSet.size()]);
        }
        System.out.println(CLASS_NAME + ".getUseFor() returns: " + results);
        return results;
    }

    private void refreshConfiguration() throws IOException {
        props = new Properties();
        props.load(new FileReader("DatabaseSettingsBean.props"));
    }

    /**
     * Update the DatabaseSettingsBean settings that can be read back by the deferred EL expressions
     * set in the servlet's annotations.
     *
     * @param directory Directory to write the properties file to.
     * @param overrides The properties to override the default value(s) of.
     * @throws IOException If there was an error writing to the backing file.
     */
    public static void updateDatabaseSettingsBean(String directory, Map<String, String> overrides) throws IOException {
        Properties props = new Properties();
        props.put(JavaEESecConstants.CALLER_QUERY, "select password from callers where name = ?");
        props.put(JavaEESecConstants.DS_LOOKUP, "java:comp/DefaultDataSource");
        props.put(JavaEESecConstants.GROUPS_QUERY, "select group_name from caller_groups where caller_name = ?");
        props.put(JavaEESecConstants.PWD_HASH_ALGORITHM, ""); // TODO
        props.put(JavaEESecConstants.PWD_HASH_PARAMETERS, "");
        props.put(JavaEESecConstants.PRIORITY, "0");
        props.put(JavaEESecConstants.USE_FOR, "VALIDATE PROVIDE_GROUPS");

        props.putAll(overrides);

        FileOutputStream fout = new FileOutputStream(directory + "/DatabaseSettingsBean.props");
        props.store(fout, "");
        fout.close();

        if (!overrides.isEmpty()) {
            for (int i = 0; i < 3; i++) { // if the build machines are struggling, we can have timing issues reading in updated values.
                Properties checkProps = new Properties();
                checkProps.load(new FileReader(directory + "/DatabaseSettingsBean.props"));

                boolean allprops = true;
                for (String prop : overrides.keySet()) {
                    String fileProp = (String) checkProps.get(prop);
                    if (fileProp == null) {
                        Log.info(DatabaseSettingsBean.class, "updateDatabaseSettingsBean", "could not find " + prop + " in DatabaseSettingsBean.props");
                        allprops = false;
                        break;
                    } else if (!fileProp.equals(overrides.get(prop))) {
                        Log.info(DatabaseSettingsBean.class, "updateDatabaseSettingsBean", "did not change " + prop + " to " + overrides.get(prop) + " yet.");
                        allprops = false;
                        break;
                    } else {
                        Log.info(DatabaseSettingsBean.class, "updateDatabaseSettingsBean", prop + " set to " + fileProp);
                    }
                }

                if (allprops) {
                    Log.info(DatabaseSettingsBean.class, "updateDatabaseSettingsBean", " DatabaseSettingsBean.props are good.");
                    break;
                }

                if (i == 3) {
                    throw new IllegalStateException("Failed to update DatabaseSettingsBean.props for EL testing");
                }

                Log.info(DatabaseSettingsBean.class, "updateDatabaseSettingsBean", "sleep and check DatabaseSettingsBean.props again.");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                }
            }
        }
    }

    /**
     * Common logic for returning a property. If the property's value is a string null "null",
     * return null. This will allow testing null handling from beans.
     *
     * @param prop
     * @return
     */
    private String getProperty(String prop) {
        String value = props.getProperty(prop);
        return "null".equalsIgnoreCase(value) ? null : value;
    }
}
