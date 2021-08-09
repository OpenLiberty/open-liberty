/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.collector;

public interface Formatter {

    /*
     * Collector will implement this method to return a formatted
     * event. Event should be formatted in such a way that it is fit to be consumed
     * by target.
     */
    public abstract Object formatEvent(String source, String location, Object event, String[] tags, int maxFieldLength);
}
