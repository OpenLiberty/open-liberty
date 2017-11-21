/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package beanvalidation.ejbmodule2.ejb;

import javax.ejb.Stateless;

/**
 * Simple EJB as a place holder to ensure that this module starts up.
 */
@Stateless
public class AnotherTestBean {

    public boolean testMethod() {
        return false;
    }

}
