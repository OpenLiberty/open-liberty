/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.properties;

/*
 * A special class to hold the value of the property: SUPPORT_SAR_TO_WAR
 * is needed as "SarToWarWrapperProvider.java" cannot access the regular "PropertyStorage" class
 * as it contains an internal sip logger (defect #722864)
 */
public class SarToWarProperties {
	
	// isLoaded - true if setSupportSarToWar was already called and SUPPORT_SAR_TO_WAR was initialized
	private static boolean isLoaded = false;
	// SUPPORT_SAR_TO_WAR - holds the value of the property SUPPORT_SAR_TO_WAR in PropertyStore
	private static boolean SUPPORT_SAR_TO_WAR = true;
	// SUPPORT_AMM_ANNOTATION_READING - holds the value of the property SUPPORT_AMM_ANNOTATION_READING in PropertyStore
	private static boolean SUPPORT_AMM_ANNOTATION_READING = true;
	
	// being called after the property map is initialized 
	public static void setSupportSarToWar(boolean supportSarToWar) {
		SUPPORT_SAR_TO_WAR = supportSarToWar;
		isLoaded = true;
	}
	
	public static boolean getSupportSarToWar() {
		return SUPPORT_SAR_TO_WAR;
	}
	
	// being called after the property map is initialized
	public static void setSupportAmmAnnotationReading(boolean supportAmmAnnotationReading) {
		SUPPORT_AMM_ANNOTATION_READING = supportAmmAnnotationReading;
		isLoaded = true;
	}
	
	public static boolean getSupportAmmAnnotationReading() {
		return SUPPORT_AMM_ANNOTATION_READING;
	}
	
	public static boolean isLoaded() {
		return isLoaded;
	}

}
