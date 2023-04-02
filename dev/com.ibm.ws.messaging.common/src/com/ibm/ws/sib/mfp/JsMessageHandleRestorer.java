/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.sib.mfp;

import com.ibm.wsspi.sib.core.SIMessageHandleRestorer;

/**
 * JsMessageHandleRestorer is the internal interface for restoring any
 * JsMesasgeHandles from their flattened forms.
 * It extends the Core SPI SIMessageHandleRestorer will be used by WPS.
 */
public abstract class JsMessageHandleRestorer extends SIMessageHandleRestorer {

}
