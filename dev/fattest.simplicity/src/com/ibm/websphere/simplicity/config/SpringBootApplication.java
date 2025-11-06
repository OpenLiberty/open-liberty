/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.websphere.simplicity.config;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 */
public class SpringBootApplication extends Application {
    @Override
    public WebApplication clone() throws CloneNotSupportedException {
        return (WebApplication) super.clone();
    }

    @XmlElement(name = "applicationArgument")
    private List<String> applicationArguments;

    private Boolean setEEContextOnStartup = true;

    public Boolean getEEContextOnStartup() {
        return setEEContextOnStartup;
    }

    @XmlAttribute(name = "setEEContextOnStartup")
    public void setEEContextOnStartup(Boolean b) {
        this.setEEContextOnStartup = b;
    }

    /**
     * Retrieves the list of application arguments in this configuration.
     *
     * @return the list of application arguments in this configuration
     */
    public List<String> getApplicationArguments() {
        if (this.applicationArguments == null) {
            this.applicationArguments = new ArrayList<>();
        }
        return this.applicationArguments;
    }
}
