/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jakarta.mail;

import org.osgi.framework.ServiceRegistration;

/**
 * Interface to register an MBean for a JavaMail session
 */
public interface MailSessionRegistrar {

    /**
     * Register an MBean for a mail session
     */
    ServiceRegistration<?> registerJavaMailMBean(String mailSessionID);
}
