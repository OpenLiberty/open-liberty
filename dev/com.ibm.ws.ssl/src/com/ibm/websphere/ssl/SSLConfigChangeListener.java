/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ssl;

/**
 * <p>
 * This interface is for components and applications to receive notifications
 * of dynamic changes to the SSL configurations returned by the JSSEHelper API.
 * An SSLConfigChangeEvent will be sent with the previous SSL selection
 * information including the alias and type of selection (direct, dynamic).
 * It's up to the listener implementation to call JSSEHelper API
 * again if it's desired to dynamically refresh the SSL configuration.
 * </p>
 * 
 * @author IBM Corporation
 * @version 1.0
 * @since WAS 6.1
 * @see com.ibm.websphere.ssl.JSSEHelper
 * @ibm-api
 **/

public interface SSLConfigChangeListener {
    void stateChanged(SSLConfigChangeEvent e);
}
