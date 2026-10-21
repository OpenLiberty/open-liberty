/*******************************************************************************
 * Copyright (c) 2012, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.converter;

import java.io.ObjectInputStream;

import com.ibm.ws.jmx.connector.datatypes.ConversionException;

public interface SerializationHelper {

    public ObjectInputStream readObject(Object in, int blen, byte[] binary) throws ClassNotFoundException, ConversionException;

}
