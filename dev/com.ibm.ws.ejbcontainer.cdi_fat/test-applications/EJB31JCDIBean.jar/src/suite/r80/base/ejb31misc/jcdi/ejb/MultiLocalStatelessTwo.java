/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package suite.r80.base.ejb31misc.jcdi.ejb;

/**
 * A simple local interface for testing beans with multiple local interfaces.
 **/
public interface MultiLocalStatelessTwo {
    /**
     * Returns the EJB name.
     **/
    public String getEjbName();
}
