/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jca.cm.ConnectorService;
import com.ibm.ws.jdbc.DataSourceService;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.wsspi.kernel.service.utils.MetatypeUtils;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

/**
 * A list of vendor properties.
 * For example,
 * <properties databaseName="myDB" serverName="localhost" portNumber="50000"/>
 */
public class PropertyService extends Properties {
    private static final long serialVersionUID = -6017388542378621407L;
    private static final TraceComponent tc = Tr.register(PropertyService.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    /**
     * Vendor properties with type that varies between vendors, that use one of the ibm:type="duration(?)" types
     */
    public static final List<String> DURATION_MIXED_PROPS = Arrays.asList(
                                                                           "lockTimeout" // milliseconds for Microsoft; seconds for DB2 i
                                                                           );

    /**
     * Vendor properties of type long that use the ibm:type="duration(ms)" type.
     */
    private static final List<String> DURATION_MS_LONG_PROPS = Arrays.asList(
                                                                             "ifxCPMServiceInterval"
                                                                             );

    /**
     * Vendor properties of type int that use the ibm:type="duration(ms)" type.
     */
    public static final List<String> DURATION_MS_INT_PROPS = Arrays.asList(
                                                                            "ifxIFX_SOC_TIMEOUT",
                                                                            "soTimeout"
                                                                            );

    /**
     * Vendor properties of type int that use the ibm:type="duration(s)" type.
     * These are supplied to us as Long, but the JDBC driver wants int.
     */
    public static final List<String> DURATION_S_INT_PROPS = Arrays.asList(
                                                                           "abandonedConnectionTimeout",
                                                                           "affinityFailbackInterval",
                                                                           "blockingReadConnectionTimeout",
                                                                           "commandTimeout",
                                                                           "connectionRetryDelay",
                                                                           "connectionTimeout",
                                                                           "connectionWaitTimeout",
                                                                           "currentLockTimeout",
                                                                           "ifxIFX_LOCK_MODE_WAIT",
                                                                           "ifxINFORMIXCONTIME",
                                                                           "inactiveConnectionTimeout",
                                                                           "keepAliveTimeOut",
                                                                           "loginTimeout",
                                                                           "maxConnectionReuseTime",
                                                                           "maxIdleTime",
                                                                           "memberConnectTimeout",
                                                                           "queryTimeout",
                                                                           "retryIntervalForClientReroute",
                                                                           "secondsToTrustIdleConnection",
                                                                           "soLinger",
                                                                           "timeoutCheckInterval",
                                                                           "timeToLiveConnectionTimeout"
                                                                           );

    /**
     * Factory persistent identifier for general data source properties.
     */
    public final static String FACTORY_PID = DataSourceService.FACTORY_PID + ".properties";

    /**
     * Name of unique identifier property.
     */
    static final String ID = "id";

    /**
     * Vendor properties of type String that use the ibm:type="password" type.
     */
    private static final List<String> PASSWORD_PROPS = Arrays.asList(DataSourceDef.password.name(),
                                                                     //DataSourceDefinition requires url to be all lowercase
                                                                     DataSourceDef.url.name(), 
                                                                     "accessToken",
                                                                     "apiKey",
                                                                     "connectionProperties",
                                                                     "connectionFactoryProperties",
                                                                     "keyStoreSecret",
                                                                     "trustStorePassword",
                                                                     "sslTrustStorePassword",
                                                                     "sslKeyStorePassword",
                                                                     //Server config will parse URL to all uppercase
                                                                     "URL" 
                                                                     );

    /**
     * Name of element used for general JDBC driver vendor properties.
     * Also a prefix for vendor specific property lists.
     */
    public static final String PROPERTIES = "properties";

    /**
     * Factory pid for the specific type of vendor properties element.
     */
    private String factoryPID;

    /**
     * Added to make FindBugs happy. {@inheritDoc}
     */
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    /**
     * Returns the factory pid for the specific type of vendor properties.
     * 
     * @return the factory pid for the specific type of vendor properties.
     */
    public String getFactoryPID() {
        return factoryPID;
    }
    
    /**
     * Factory pid for the nested vendor properties elements.
     * 
     * @param factoryPID factory pid for vendor properties element.
     */
    public void setFactoryPID(String factoryPID) {
        this.factoryPID = factoryPID;
    }

    /**
     * Added to make FindBugs happy. {@inheritDoc}
     */
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Utility method to hide passwords from a map structure.
     * Make a copy of the map and replace password values with ******.
     * 
     * @param map collection of name/value pairs. Values might be passwords.
     * @return copy of the map with passwords hidden.
     */
    @SuppressWarnings("unchecked")
    public static final Map<?, ?> hidePasswords(Map<?, ?> map) {
        map = new HashMap<Object, Object>(map);

        for (@SuppressWarnings("rawtypes") Map.Entry entry : map.entrySet()) {
            if (entry.getKey() instanceof String && PropertyService.isPassword((String) entry.getKey()))
                entry.setValue("******");
            else if(entry.getKey() instanceof String && entry.getValue() instanceof String && ((String) entry.getKey()).toLowerCase().contains("url"))
                entry.setValue(filterURL((String) entry.getValue()));
            else if (entry.getKey() instanceof String && entry.getValue() instanceof String && ((String) entry.getKey()).toLowerCase().contains("connectionproperties"))
                entry.setValue(filterConnectionProperties((String) entry.getValue()));
        }
        return map;
    }
    
    public static String filterURL(String url) {
        int first = url.indexOf(":");
        int second = url.indexOf(":", first + 1);
        int third = url.indexOf(":", second + 1);
        StringBuilder sb = new StringBuilder(url.length());
        
        Pattern alphanumericPattern = Pattern.compile("[\\w]*");
        
        if(first < 0 || second < 0) {
            //This is a violation of the JDBC specification, so we shouldn't ever get here
        } else {
            if(third < 0) {
                //JDBC driver does not have more detailed info, just append first and second for now
                sb.append(url.substring(0, second+1));
            } else {
                String subString = url.substring(second+1, third);
                if(alphanumericPattern.matcher(subString).matches()) {
                    //String between second : and third : is all alphanumeric, so append it
                    sb.append(url.substring(0, third+1));
                } else {
                    //String between second : and third : contains non-alphanumeric value(s) so it is not safe to append
                    //append to second : and proceed
                    sb.append(url.substring(0, second+1));
                }
            }
        }
                
        sb.append("****");
        return sb.toString();
    }
    
    public static String filterConnectionProperties(String props) {
        final String regex = "Password\\s*=\\s*(.*?)\\s*(;|$)";
        
        StringBuffer sb = new StringBuffer();
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(props);

        while (matcher.find()) {
            //group(0) = "Password = abcd;" group(1) = "abcd" group(2) = ";"
            //This appends a replacement for group(0), so we want to just replace group(1) with *****
            matcher.appendReplacement(sb, matcher.group(0).replace(matcher.group(1), "*****"));
        }
        
        //Append any trailing characters after matches
        //If there were no matches this will just append props
        matcher.appendTail(sb);
        
        return sb.toString(); 
    }

    /**
     * Determines, based on the name of a property, if we expect the value might contain a password.
     * 
     * @param name property name.
     * @return true if the property value might be expected to contain a password, otherwise false.
     */
    public static final boolean isPassword(String name) {
        return PASSWORD_PROPS.contains(name) || name.toLowerCase().contains(DataSourceDef.password.name());
    }

    /**
     * Parse and convert duration type properties.
     * 
     * @param vendorProps the vendor properties.
     * @param className data source implementation class name.
     * @param connectorSvc connector service.
     * @throws Exception if an error occurs.
     */
    public static void parseDurationProperties(Map<String, Object> vendorProps, String className, ConnectorService connectorSvc) throws Exception {

        // type=long, unit=milliseconds
        for (String propName : PropertyService.DURATION_MS_LONG_PROPS) {
            Object propValue = vendorProps.remove(propName);
            if (propValue != null)
                try {
                    vendorProps.put(propName, MetatypeUtils.evaluateDuration((String) propValue, TimeUnit.MILLISECONDS));
                } catch (Exception x) {
                    x = connectorSvc.ignoreWarnOrFail(tc, x, x.getClass(), "UNSUPPORTED_VALUE_J2CA8011", propValue, propName, className);
                    if (x != null)
                        throw x;
                }
        }

        // type=int, unit=milliseconds
        for (String propName : PropertyService.DURATION_MS_INT_PROPS) {
            Object propValue = vendorProps.remove(propName);
            if (propValue != null)
                try {
                    vendorProps.put(propName, MetatypeUtils.evaluateDuration((String) propValue, TimeUnit.MILLISECONDS).intValue());
                } catch (Exception x) {
                    x = connectorSvc.ignoreWarnOrFail(tc, x, x.getClass(), "UNSUPPORTED_VALUE_J2CA8011", propValue, propName, className);
                    if (x != null)
                        throw x;
                }
        }

        // type=int, unit=seconds
        for (String propName : PropertyService.DURATION_S_INT_PROPS) {
            Object propValue = vendorProps.remove(propName);
            if (propValue != null)
                if (propValue instanceof String)
                    try {
                        vendorProps.put(propName, MetatypeUtils.evaluateDuration((String) propValue, TimeUnit.SECONDS).intValue());
                    } catch (Exception x) {
                        x = connectorSvc.ignoreWarnOrFail(tc, x, x.getClass(), "UNSUPPORTED_VALUE_J2CA8011", propValue, propName, className);
                        if (x != null)
                            throw x;
                    }
                else
                    // loginTimeout is already converted to int
                    vendorProps.put(propName, propValue);
        }

        // lockTimeout has different type/units between Microsoft and DB2 i
        Object lockTimeout = vendorProps.remove("lockTimeout");
        if (lockTimeout != null)
            try {
                if (className.startsWith("com.microsoft"))
                    vendorProps.put("lockTimeout", MetatypeUtils.evaluateDuration((String) lockTimeout, TimeUnit.MILLISECONDS));
                else
                    vendorProps.put("lockTimeout", MetatypeUtils.evaluateDuration((String) lockTimeout, TimeUnit.SECONDS).intValue());
            } catch (Exception x) {
                x = connectorSvc.ignoreWarnOrFail(tc, x, x.getClass(), "UNSUPPORTED_VALUE_J2CA8011", lockTimeout, "lockTimeout", className);
                if (x != null)
                    throw x;
            }
    }

    /**
     * Parse and convert password properties to SerializableProtectedString.
     * 
     * @param vendorProps
     */
    public static final void parsePasswordProperties(Map<String, Object> vendorProps) {

        for (String propName : PASSWORD_PROPS) {
            String propValue = (String) vendorProps.remove(propName);
            if (propValue != null)
                vendorProps.put(propName, new SerializableProtectedString(propValue.toCharArray()));
        }
    }

    /**
     * Returns text representing the properties, with passwords hidden.
     * 
     * @return text representing the properties, with passwords hidden.
     */
    public String toString() {
        Map<?, ?> copyWithPasswordsHidden;
        synchronized (this) {
            copyWithPasswordsHidden = hidePasswords(this);
        }
        return copyWithPasswordsHidden.toString();
    }
}