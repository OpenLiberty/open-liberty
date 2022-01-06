/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.accesslists;

/**
 * Do not change the values of these without coordinating with the
 * channel framework equivalents.
 */
public interface AccessListsConstants {

    /** TCP RAS trace group name */
    String TCP_TRACE_GROUP = "TCPChannel";

    /** RAS trace bundle for TCP NLS */
    String TCP_MESSAGES = "com.ibm.ws.tcpchannel.internal.resources.TCPChannelMessages";

}
