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
package com.ibm.ws.transport.iiop.spi;

/**
 * Like org.omg.PortableServer.AdapterActivatorOperations but supplies our ORBRef as well.
 */
public interface AdapterActivatorOp {

    boolean unknown_adapter(org.omg.PortableServer.POA parent, String name, ORBRef orbRef);

}
