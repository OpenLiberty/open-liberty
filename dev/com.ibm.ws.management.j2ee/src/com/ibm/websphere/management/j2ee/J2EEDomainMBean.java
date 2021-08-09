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
 * The J2EEDomain managed object type represents a management domain. There
 * must be one managed object that implements the J2EEDomain model per
 * management domain. All servers and applications associated with the same domain
 * must be accessible from the J2EEDomain managed object.
 */
public interface J2EEDomainMBean extends J2EEManagedObjectMBean {

    /**
     * A list of all J2EE Servers in this domain. For each J2EE Server running in the
     * domain, there must be one J2EEServer OBJECT_NAME in the servers list that
     * identifies it.
     */
    String[] getservers();

}
