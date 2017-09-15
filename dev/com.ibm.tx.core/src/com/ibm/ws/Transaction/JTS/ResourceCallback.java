package com.ibm.ws.Transaction.JTS;
/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/** This interface should be implemented by all classes that require to
 *  receive resource callbacks.
 */
public interface ResourceCallback
{
    /** The resource is being destroyed. Any necessary cleanup
     *  should now be performed.
     */
    void destroy();
}
