/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.csi;

/**
 * The <code>EJBKey</code> interface is a marker interface
 * that objects which uniquely identify EJB instances must
 * implement. <p>
 * 
 * An <code>EJBKey</code> instance must correctly implement the
 * hashCode() and equals() methods. <p>
 */
public interface EJBKey
{
    // Marker interface for abstraction only
}
