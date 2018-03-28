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
package com.ibm.ws.container.service.app.deploy.extended;

/**
 * The reason for this interface is to avoid a circular dependency. The com.ibm.wsspi.application.handler
 * project has a dependency on the com.ibmws.container.service project. The container service project needs
 * access to an "ApplicationInformation" but we can't add that dependency. So we add this implementation
 * here in the container service project and we make tgke ApplicationInformation class implement this interface.
 * You'll notice that in EJBDeployedAppInfo.createApplicationInfo that applicatonInformation is cast to
 * this interface.
 */
public interface ApplicationInfoForContainer {

    boolean getUseJandex();

}
