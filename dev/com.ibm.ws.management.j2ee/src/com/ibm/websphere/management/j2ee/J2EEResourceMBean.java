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
 * J2EEResource is the base model for all managed object types that represent J2EE
 * resources. J2EE resources are resources utilized by the J2EE core server to provide
 * the J2EE standard services required by the J2EE platform architecture. For each
 * J2EE standard service that a server provides, there must be one managed object that
 * implements the J2EEResource model of the appropriate type.
 */
public interface J2EEResourceMBean extends J2EEManagedObjectMBean {

}
