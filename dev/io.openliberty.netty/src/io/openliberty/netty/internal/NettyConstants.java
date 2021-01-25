/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.netty.internal;

/**
 * This purpose of this interface is to consolidate Strings used throughout
 * the Channel Framework to prevent future changes from rippling to all
 * files.
 */
public interface NettyConstants {

    /** Trace group id used for the feature */
    String BASE_TRACE_NAME = "Netty";
    /** Resource bundle used for the feature */
    String BASE_BUNDLE = "io.openliberty.netty.internal.resources.NettyMessages";

}
