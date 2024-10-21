/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.internal.local;

/**
 * This interface describes the constants used for configuring a Local Channel.
 */
public interface LocalConfigConstants {

    //TODO: what are the correct message ids?
    //TODO GDH - are these too specific to be in OL?
    String LOCAL_CHANNEL_STARTED = 		"CWUDP0001I";
    String LOCAL_CHANNEL_STOPPED = 		"CWUDP0002I";
    String LOCAL_INCORRECT_PROPERTY = 	"CWUDP0003W";
    String LOCAL_BIND_FAILURE = 		"CWUDP0005E";
    String LOCAL_LOOKUP_FAILURE = 		"CWUDP0006I";

    Integer LOCAL_MAX_RCV_BUFFSIZE = Integer.MAX_VALUE;
    Integer LOCAL_MIN_RCV_BUFFSIZE = 12;
    
}
