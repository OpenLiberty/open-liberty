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
package org.apache.felix.scr.ext.annotation;

//This will be contributed to apache shortly.
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import aQute.bnd.annotation.xml.XMLAttribute;


public interface DSExt {
	
	@XMLAttribute(namespace = "http://felix.apache.org/xmlns/scr/extensions/v1.0.0", prefix = "felix", mapping="value=configurableServiceProperties")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	@interface ConfigurableServiceProperties {
		boolean value() default true;
	}
	
	@XMLAttribute(namespace = "http://felix.apache.org/xmlns/scr/extensions/v1.0.0", prefix = "felix", mapping="value=persistentFactoryComponent")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	@interface PersistentFactoryComponent {
		boolean value() default true;
	}
	
	@XMLAttribute(namespace = "http://felix.apache.org/xmlns/scr/extensions/v1.0.0", prefix = "felix", mapping="value=deleteCallsModify")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	@interface DeleteCallsModify {
		boolean value() default true;
	}
	
	@XMLAttribute(namespace = "http://felix.apache.org/xmlns/scr/extensions/v1.0.0", prefix = "felix", mapping="value=obsoleteFactoryComponentFactory")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	@interface ObsoleteFactoryComponentFactory {
		boolean value() default true;
	}
	
	@XMLAttribute(namespace = "http://felix.apache.org/xmlns/scr/extensions/v1.0.0", prefix = "felix", mapping="value=configureWithInterfaces")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	@interface ConfigureWithInterfaces {
		boolean value() default true;
	}
	
	@XMLAttribute(namespace = "http://felix.apache.org/xmlns/scr/extensions/v1.0.0", prefix = "felix", mapping="value=delayedKeepInstances")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	@interface DelayedKeepInstances {
		boolean value() default true;
	}

}
