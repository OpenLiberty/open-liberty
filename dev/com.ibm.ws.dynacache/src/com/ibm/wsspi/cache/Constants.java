/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.cache;

/**
 * This class provides Dynacache constants that are used by other components.
 * @ibm-spi 
 */
public class Constants {
   
    /**
     * When an attribute with this name is set to true by the WebContainer it informs Dynacache
     * that the current request is a Remote Request Dispatcher Request and buffering should be
     * used for the next include.
     */
     public static final String IBM_DYNACACHE_RRD_BUFFERING = "IBM-DYNACACHE-RRD-BUFFERING";
	
    /**
     * The Remote Request Dispatcher Rules are added to a HashMap and then set as a request attribute
     * using this name.  This HashMap is used by the WebContainer.
     */
     public static final String IBM_DYNACACHE_RRD_ESI = "IBM-DYNACACHE-RRD-ESI";
	
    /**
     * Used by the WebContainer to get the locale Remote Request Dispatcher Rule from the HashMap
     */
     public static final String IBM_DYNACACHE_RRD_LOCALE = "locale";
	
    /**
     * Used by the WebContainer to get the requestType Remote Request Dispatcher Rule from the HashMap
     */
     public static final String IBM_DYNACACHE_RRD_REQUEST_TYPE = "requestType";
	
}