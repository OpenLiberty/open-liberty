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
 * Identifies a deployed singleton session bean. The specification was not
 * updated to model singleton session beans, so this interface and the
 * corresponding SingletonSessionBean j2eeType are extensions.
 */
public interface SingletonSessionBeanMBean extends SessionBeanMBean {

}
