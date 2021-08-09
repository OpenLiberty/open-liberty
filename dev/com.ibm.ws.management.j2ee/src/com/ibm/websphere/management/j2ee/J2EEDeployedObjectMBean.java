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
package com.ibm.websphere.management.j2ee;


/**
 * The base model for J2EEApplication and J2EEModule. All J2EEDeployedObject
 * managed objects contain the original XML deployment descriptor that was created
 * for the application or module during the deployment process.
 */
public interface J2EEDeployedObjectMBean extends J2EEManagedObjectMBean {

    /**
     * The deploymentDescriptor string must contain the original XML
     * deployment descriptor that was created for this module during the deployment
     * process. The deploymentDescriptor attribute must provide a full deployment
     * descriptor based on any partial deployment descriptor plus deployment
     * annotations.
     */
    String getdeploymentDescriptor();

    /**
     * The J2EE server the application or module is deployed on.
     */
    String getserver();

}
