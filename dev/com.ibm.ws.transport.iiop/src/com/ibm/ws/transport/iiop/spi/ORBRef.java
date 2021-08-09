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
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.spi;

import java.util.Map;

import org.omg.PortableServer.POA;

/**
 * @version $Revision: 465172 $ $Date: 2006-10-18 01:16:14 -0700 (Wed, 18 Oct 2006) $
 */
public interface ORBRef extends ClientORBRef {

    POA getPOA();

    Map<String, Object> getExtraConfig();

}
