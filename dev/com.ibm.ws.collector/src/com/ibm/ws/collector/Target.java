/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.collector;

import java.util.List;

public interface Target {

    /*
     * The list passed here should be a list of formatted events.
     */
    public abstract void sendEvents(List<Object> formattedEvents);

    public abstract void close();
}
