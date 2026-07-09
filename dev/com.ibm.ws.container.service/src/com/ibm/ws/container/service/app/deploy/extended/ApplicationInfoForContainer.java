/*******************************************************************************
 * Copyright (c) 2018, 2026 IBM Corporation and others.
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
package com.ibm.ws.container.service.app.deploy.extended;

/**
 * The reason for this interface is to avoid a circular dependency. The com.ibm.wsspi.application.handler
 * project has a dependency on the com.ibmws.container.service project. The container service project needs
 * access to an "ApplicationInformation" but we can't add that dependency. So we add this implementation
 * here in the container service project and we make the ApplicationInformation class implement this interface.
 * You'll notice that in EJBDeployedAppInfo.createApplicationInfo that applicatonInformation is cast to
 * this interface.
 */
public interface ApplicationInfoForContainer {
    /**
     * Tell if jandex is enabled. That is usually supplied by a "useJandex" attribute
     * on the application element or the application manager element.
     * 
     * @return True or false telling if jandex use is enabled. The default is false.
     */
    boolean getUseJandex();
    
    /**
     * Tell if jandex index files are to be read from
     * <code>WEB-INF/classes/META-INF/jandex.idx</code> when processing
     * a web module. That is usually supplied by a "enableWebInfJandex"
     * attribute on the application element or the application manager element.
     * 
     * The initial implementation read jandex index files for web modules from
     * <code>META-INF/jandex.idx</code>. However, the industry standard is to
     * read the index files relative to the web module class path.
     * 
     * @return True or false telling if jandex index files are to be read from
     *     the <code>WEB-INF</code> location. The default is false.
     */
    boolean getEnableWebInfJandex();

    String getAnnotationScanLibrary();
}
