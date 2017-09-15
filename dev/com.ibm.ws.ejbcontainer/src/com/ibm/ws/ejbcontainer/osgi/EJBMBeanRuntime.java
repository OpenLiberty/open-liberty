/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi;

import java.util.List;

import org.osgi.framework.ServiceRegistration;

import com.ibm.ws.ejbcontainer.EJBComponentMetaData;
import com.ibm.ws.ejbcontainer.EJBType;
import com.ibm.wsspi.adaptable.module.Container;

public interface EJBMBeanRuntime {
    ServiceRegistration<?> registerModuleMBean(String appName, String moduleName, Container container, String ddPath, List<EJBComponentMetaData> ejbs);

    ServiceRegistration<?> registerEJBMBean(String appName, String moduleName, String beanName, EJBType type);
}
