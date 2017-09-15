/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.bnd.metatype.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import aQute.bnd.annotation.xml.XMLAttribute;

public interface UIExt {

	/**
	 * The extraProperties extension is used to indicate that an arbitrary set of configuration attributes can be set on this configuration. 
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/ui/v1.0.0", prefix = "ibmui", mapping="value=extraProperties")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public @interface ExtraProperties {
		boolean value() default true;
	}

	/**
	 * The group extension is used to specify that the attribute belongs to a given group. 
	 * In the user interface the attributes annotated with the same group will be grouped together. 
	 * This requires the @Localization annotation on the type.
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/ui/v1.0.0", prefix = "ibmui", mapping="value=group")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.METHOD)
	public @interface Group {
		String value();
	}

	/**
	 * Specify the localization file for use with AD @Group annotations.
	 * Normally this will be the metatype localization file.
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/ui/v1.0.0", prefix = "ibmui", mapping="value=localization")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public @interface Localization {
		String value();
	}

	/**
	 * Specify the pid for possible matches for use in schema.  Used for non-standard "references" such as ssl
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/ui/v1.0.0", prefix = "ibmui", mapping="value=uiReference")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.METHOD)
	public @interface UIReference {
		String value();
	}

}
