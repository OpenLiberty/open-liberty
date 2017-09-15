/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config.context;

import javax.xml.bind.annotation.XmlAttribute;

import com.ibm.websphere.simplicity.config.ConfigElement;

/**
 * Represents the <securityContext> element which can be nested under <contextService>
 */
public class SecurityContext extends ConfigElement {
    private String callerSubject;
    private String invocationSubject;

    public String getCallerSubject() {
        return callerSubject;
    }

    public String getInvocationSubject() {
        return invocationSubject;
    }

    @XmlAttribute(name = "callerSubject")
    public void setCallerSubject(String subject) {
        callerSubject = subject;
    }

    @XmlAttribute(name = "invocationSubject")
    public void setInvocationSubject(String subject) {
        invocationSubject = subject;
    }

    /**
     * Returns a string containing a list of the properties and their values.
     * 
     * @return String representing the data
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("securityContext{");
        if (getId() != null)
            buf.append("id=").append(getId()).append(' ');
        if (callerSubject != null)
            buf.append("callerSubject=").append(callerSubject).append(' ');
        if (invocationSubject != null)
            buf.append("invocationSubject=").append(invocationSubject).append(' ');
        buf.append("}");
        return buf.toString();
    }
}