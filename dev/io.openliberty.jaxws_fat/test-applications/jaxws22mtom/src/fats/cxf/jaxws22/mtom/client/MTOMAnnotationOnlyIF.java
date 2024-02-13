/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
/**
 * The interface used by the dynamic proxy client.
 */
package fats.cxf.jaxws22.mtom.client;

import javax.jws.*;
import javax.xml.ws.soap.MTOM;

@MTOM(enabled = true, threshold = 0)
@WebService(targetNamespace = "http://server.mtom.jaxws22.cxf.fats/", name = "MTOMAnnotationOnly")
public interface MTOMAnnotationOnlyIF {

    public abstract byte[] echobyte(byte[] b);

}