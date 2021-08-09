/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.metadata;

import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.wsspi.adaptable.module.Container;

public interface MetaDataEvent<M extends MetaData> {
    /**
     * The metadata for the event.
     */
    M getMetaData();

    /**
     * The {@link Container} associated with this metadata event, or null if no
     * Container is associated with this event.
     */
    Container getContainer();
}
