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

package web.war.database.deferred;

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
        String prop = getProperty("callerQuery");
        System.out.println(CLASS_NAME + ".getCallerQuery() returns: " + prop);
        return prop;
    }

    public String getDataSourceLookup() throws IOException {
        refreshConfiguration();
        String prop = getProperty("dataSourceLookup");
        System.out.println(CLASS_NAME + ".getDataSourceLookup() returns: " + prop);
        return prop;
    }

    public String getGroupsQuery() throws IOException {
        refreshConfiguration();
        String prop = getProperty("groupsQuery");
        System.out.println(CLASS_NAME + ".getGroupsQuery() returns: " + prop);
        return prop;
    }

    public Object getHashAlgorithmParameters() throws IOException {
        refreshConfiguration();

        String prop = getProperty("hashAlgorithmParameters");
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

        String prop = getProperty("priority");
        Integer result = null;
        if (prop != null) {
            result = Integer.valueOf(prop);
        }

        System.out.println(CLASS_NAME + ".getPriority() returns: " + result);
        return result;
    }

    public Set<ValidationType> getUseFor() throws IOException {
        refreshConfiguration();

        Set<ValidationType> results = null;

        String prop = getProperty("useFor");
        if (prop != null) {
            results = new HashSet<ValidationType>();

            if (prop.contains("VALIDATE")) {
                results.add(ValidationType.VALIDATE);
            }
            if (prop.contains("PROVIDE_GROUPS")) {
                results.add(ValidationType.PROVIDE_GROUPS);
            }
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
        props.put("callerQuery", "select password from callers where name = ?");
        props.put("dataSourceLookup", "java:comp/DefaultDataSource");
        props.put("groupsQuery", "select group_name from caller_groups where caller_name = ?");
        props.put("hashAlgorithm", ""); // TODO
        props.put("hashAlgorithmParameters", "");
        props.put("priority", "0");
        props.put("useFor", "VALIDATE PROVIDE_GROUPS");

        props.putAll(overrides);

        FileOutputStream fout = new FileOutputStream(directory + "/DatabaseSettingsBean.props");
        props.store(fout, "");
        fout.close();
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
