/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.cm;

/**
 * Implemented by JCA managed resources that can be defined by applications.
 * For example, via @DataSourceDefinition, @ConnectionFactoryDefinition and @AdministeredObjectDefinition
 */
public interface AppDefinedResource {
    /**
     * Name of property that identifies the application for application-defined resources.
     */
    static final String APPLICATION = "application";

    /**
     * Name of property that identifies the component for application-defined resources.
     */
    static final String COMPONENT = "component";

    /**
     * Name of property that identifies the module for application-defined resources.
     */
    static final String MODULE = "module";

    /**
     * The common portion of the prefix that we add to unique identifiers for all application-defined resources.
     * Example unique identifiers:
     * application[App1]/dataSource[java:app/env/jdbc/ds1]
     * application[App1]/module[Mod1]/dataSource[java:module/env/jdbc/ds2]
     * application[App1]/module[Mod1]/component[Comp1]/dataSource[java:comp/env/jdbc/ds3]
     */
    static final String PREFIX = APPLICATION + '[';

    /**
     * Returns the name of the application in which the resource is defined.
     * 
     * @return the name of the application in which the resource is defined.
     */
    String getApplication();

    /**
     * Returns the name of the component (if any) in which the resource is defined.
     * 
     * @return the name of the component (if any) in which the resource is defined.
     */
    String getComponent();

    /**
     * Returns the name of the module (if any) in which the resource is defined.
     * 
     * @return the name of the module (if any) in which the resource is defined.
     */
    String getModule();
}
