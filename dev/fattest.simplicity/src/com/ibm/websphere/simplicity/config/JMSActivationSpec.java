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

import javax.xml.bind.annotation.XmlElement;

public class JMSActivationSpec extends ActivationSpec {

    @XmlElement(name = "properties.wasJms")
    private ConfigElementList<WasJmsProperties> wasJmsProperties;

    /**
     * @return the wasJmsProperties
     */
    public ConfigElementList<WasJmsProperties> getWasJmsProperties() {
        if (wasJmsProperties == null) {
            wasJmsProperties = new ConfigElementList<WasJmsProperties>();
        }
        return wasJmsProperties;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.simplicity.config.ConfigElement#clone()
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        JMSActivationSpec clone = (JMSActivationSpec) super.clone();
        if (wasJmsProperties != null) {
            clone.wasJmsProperties = new ConfigElementList<WasJmsProperties>();
            for (WasJmsProperties props : wasJmsProperties) {
                clone.getWasJmsProperties().add((WasJmsProperties) props.clone());
            }
        }
        return clone;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.simplicity.config.ActivationSpec#toString()
     */
    @Override
    public String toString() {
        String nl = System.getProperty("line.separator");
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        buf.append("id=\"" + (getId() == null ? "" : getId()) + "\" ");
        if (this.getAuthData() != null)
            buf.append("authDataRef=\"" + this.getAuthData() + "\" ");
        if (wasJmsProperties != null) {
            for (WasJmsProperties props : wasJmsProperties) {
                buf.append(props.toString()).append(nl);
            }
        }
        buf.append("}");
        return buf.toString();
    }
}
