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

public interface SchemaExt {

	/**
	 * Something to do with schema generation
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=action")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public @interface Action {
		String value();
	}

	/**
	 * Something to do with schema generation
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=any")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public @interface Any {
		int value();
	}

	/**
	 * Comma separated list of child first pids to exclude from schema generation
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=excludeChildren")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public @interface ExcludeChildren {
		String value();
	}

}
