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
 * Identifies a managed URL resource. For each managed URL resource provided by a
 * server there should be one managed object that implements the URLResource
 * model. It is specific to a server implementation which URL resources are exposed as
 * manageable and there are no specific requirements as to which URL resources
 * provided by a server are exposed as managed objects.
 */
public interface URLResourceMBean extends J2EEResourceMBean {

}
