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
package com.ibm.websphere.simplicity.config;

import java.util.ArrayList;
import java.util.List;

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
