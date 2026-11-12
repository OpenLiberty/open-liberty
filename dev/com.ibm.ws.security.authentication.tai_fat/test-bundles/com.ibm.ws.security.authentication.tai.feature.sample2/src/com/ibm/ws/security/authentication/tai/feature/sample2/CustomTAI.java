/*******************************************************************************
 * Copyright (c) 2011, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.authentication.tai.feature.sample2;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.security.WebTrustAssociationException;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.security.tai.TAIResult;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;
import com.ibm.wsspi.security.token.AttributeNameConstants;

public class CustomTAI implements TrustAssociationInterceptor {

    private String scenario = null;
    private String username = null;
    private final String nonZeroScenario = "nonZero";
    private final String returnFalseScenario = "false";
    private final String userScenario = "user";
    private final String subjectScenario = "subject";
    private final String hashtableScenario = "hashtable";
    private final String unauthorizedScenario = "403";
    private final String internalServerErrorScenario = "500";
    private final String nullScenario = "null";
    private final String customSubject = "customSubject";

    static final String CFG_KEY_PROPERTIES_PID = "propertiesRef";
    static final String KEY_CONFIGURATION_ADMIN = "configurationAdmin";
    private final AtomicServiceReference<ConfigurationAdmin> configAdminRef = new AtomicServiceReference<ConfigurationAdmin>(KEY_CONFIGURATION_ADMIN);
    private final Properties properties = new Properties();

    /** Default Constructor **/
    public CustomTAI() {}

    protected void activate(ComponentContext componentContext, Map<String, Object> newProperties) {
        configAdminRef.activate(componentContext);
        modified(newProperties);
    }

    protected void modified(Map<String, Object> newProperties) {
        properties.clear();
        scenario = null;
        username = null;
        processProperties(newProperties.get(CFG_KEY_PROPERTIES_PID));
        if (!properties.isEmpty()) {
            try {
                this.initialize(properties);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void deactivate() {
        properties.clear();
        scenario = null;
        username = null;
    }

    protected void setConfigurationAdmin(ServiceReference<ConfigurationAdmin> ref) {
        configAdminRef.setReference(ref);
    }

    protected void unsetConfigurationAdmin(ServiceReference<ConfigurationAdmin> ref) {
        configAdminRef.unsetReference(ref);
    }

    @Override
    public void cleanup() {}

    @Override
    public String getType() {
        return "CustomTAI";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public int initialize(Properties props)
                    throws WebTrustAssociationFailedException {
        if (props != null) {
            scenario = props.getProperty("scenario");
            username = props.getProperty("username");
        }
        if (scenario != null && scenario.contains(nonZeroScenario)) {
            return 1;
        }
        return 0;
    }

    @Override
    public boolean isTargetInterceptor(HttpServletRequest req)
                    throws WebTrustAssociationException {
        if (scenario != null && scenario.contains(returnFalseScenario)) {
            return false;
        }
        return true;
    }

    @Override
    public TAIResult negotiateValidateandEstablishTrust(HttpServletRequest req, HttpServletResponse rsp)
                    throws WebTrustAssociationFailedException {

        try {
            //getWriter() is used here for the Liberty FAT test, but in tWAS this needs to be changed
            // to System.out.println() in order to work in tWAS
            rsp.getWriter().println("TAI scenario: " + scenario);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        Subject subject = new Subject();
        if (scenario != null && scenario.contains(userScenario)) {
            return TAIResult.create(HttpServletResponse.SC_OK, username);
        } else if (scenario != null && scenario.contains(subjectScenario)) {
            subject.getPublicCredentials().add(customSubject);
            return TAIResult.create(HttpServletResponse.SC_OK, username, subject);
        } else if (scenario != null && scenario.contains(hashtableScenario)) {
            Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
            hashtable.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, username);
            hashtable.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, username);
            subject.getPublicCredentials().add(hashtable);
            return TAIResult.create(HttpServletResponse.SC_OK, username, subject);
        } else if (scenario != null && scenario.contains(unauthorizedScenario)) {
            return TAIResult.create(HttpServletResponse.SC_FORBIDDEN);
        } else if (scenario != null && scenario.contains(internalServerErrorScenario)) {
            return TAIResult.create(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } else if (scenario != null && scenario.contains(nullScenario)) {
            return null;
        } else {
            return TAIResult.create(HttpServletResponse.SC_CONTINUE);
        }
    }

    /**
     * @param configAdmin
     * @param obj
     */
    private void processProperties(Object o) {
        properties.clear();
        if (o == null)
            return;

        String pid = (String) o;
        ConfigurationAdmin configAdmin = configAdminRef.getServiceWithException();

        Configuration[] configList = null;
        try {
            // We do not want to create a missing pid, only find one that we were told exists
            configList = configAdmin.listConfigurations(FilterUtils.createPropertyFilter("service.pid", pid));
        } catch (InvalidSyntaxException e) {
        } catch (IOException e) {
        }

        if (configList != null && configList.length > 0) {
            // Just get the first one (there should only ever be one.. )
            Dictionary<String, ?> cProps = configList[0].getProperties();
            Enumeration<String> keys = cProps.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                // Skip certain keys
                if (key.startsWith(".")
                    || key.startsWith("config.")
                    || key.startsWith("service.")
                    || key.equals("id")) {
                    continue;
                }
                properties.put(key, cProps.get(key));
            }
        }
    }
}
