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
 * Identifies a deployed EJB JAR module.
 */
public interface EJBModuleMBean extends J2EEModuleMBean {

    /**
     * A list of EJB components contained in the deployed EJB JAR module. For
     * each EJB component contained in the deployed EJB JAR there must be one EJB
     * OBJECT_NAME in the ejbs list that identifies it.
     */
    String[] getejbs();

}
