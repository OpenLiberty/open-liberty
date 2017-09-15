/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.config;

/**
 *
 */
public interface MetaTypeProviderConstants {
    /** MetaTypeProvider Added Event Topic */
    String METATYPE_PROVIDER_ADDED_TOPIC = "com/ibm/ws/config/xml/internal/MetaTypeRegistry/METATYPE_PROVIDER_ADDED";

    /** MetaTypeProvider Removed Event Topic */
    String METATYPE_PROVIDER_REMOVED_TOPIC = "com/ibm/ws/config/xml/internal/MetaTypeRegistry/METATYPE_PROVIDER_REMOVED";

    /** MetaTypeProvider Updated PIDS property key */
    String UPDATED_PID = "mtp_updated_pids";

}
