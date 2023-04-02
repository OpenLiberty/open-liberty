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
package com.ibm.ws.sib.api.jmsra.impl;

import com.ibm.ws.sib.ra.inbound.impl.SibRaResourceAdapterImpl;

/**
 * Resource adapter implementation.
 */
public final class JmsJcaResourceAdapterImpl extends SibRaResourceAdapterImpl {

	/*
	 * Admin currently require a separate implementation of this class for the
	 * JMS resource adapter as they use the class name stored in the
	 * resources.xml to find JMS resources
	 */

}
