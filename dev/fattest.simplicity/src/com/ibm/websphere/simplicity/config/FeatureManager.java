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
package com.ibm.websphere.simplicity.config;

import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlElement;

/**
 * Represents the list of features enabled in a server configuration
 *
 * @author Tim Burns
 *
 */
public class FeatureManager extends ConfigElement {

    /**
     * Kills the server after 20 minutes in case of emergency;
     * change timeout with System property (ms): "com.ibm.ws.timedexit.timetolive"<br>
     * (See com.ibm.ws.timedexit/src/com/ibm/ws/timedexit/internal/TimedExitComponent.java)
     */
    public static final String FEATURE_TIMED_EXIT_1_0 = "timedexit-1.0";
    public static final String FEATURE_SERVLET_3_0 = "servlet-3.0";
    public static final String FEATURE_JSP_2_2 = "jsp-2.2";
    public static final String FEATURE_SESSION_DATABASE_1_0 = "sessionDatabase-1.0";
    public static final String FEATURE_SSL_1_0 = "ssl-1.0";
    public static final String FEATURE_APP_SECURITY_1_0 = "appSecurity-1.0";

    @XmlElement(name = "feature")
    private Set<String> features;

    /**
     * Retrieves the list of features in this configuration. Uses Strings to
     * define feature names to provide flexability for unknown/future features.
     *
     * @return the list of features in this configuration
     */
    public Set<String> getFeatures() {
        if (this.features == null) {
            this.features = new TreeSet<String>();
        }
        return this.features;
    }

    @Override
    public FeatureManager clone() throws CloneNotSupportedException {
        FeatureManager clone = (FeatureManager) super.clone();

        if (this.features != null) {
            clone.features = new TreeSet<String>();
            for (String feature : this.features)
                clone.features.add(feature);
        }

        return clone;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("FeatureManager{");
        if (this.features != null)
            for (String feature : features)
                buf.append("feature=\"" + feature + "\" ");
        buf.append("}");

        return buf.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof FeatureManager))
            return false;
        return getFeatures().equals(((FeatureManager) o).getFeatures());
    }
}
