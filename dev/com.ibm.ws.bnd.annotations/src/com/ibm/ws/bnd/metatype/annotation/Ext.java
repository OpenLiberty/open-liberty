/*******************************************************************************
 * Copyright (c) 2015-2020 IBM Corporation and others.
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

public interface Ext {
	
	String INTERNAL = "internal";
	String INTERNAL_DESC = "internal use only";
	String LOCALIZATION = "OSGI-INF/l10n/metatype";

	/**
	 * Sets ibm:beta on an OCD or AD
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=beta")
	@Retention(RetentionPolicy.CLASS)
	@Target({ElementType.TYPE, ElementType.METHOD})
	public @interface Beta {
		boolean value() default true;
	}
	/**
	 * Sets the alias for the OCD.
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=alias")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public @interface Alias {
		String value();
	}

	/**
	 * Specifies the OCD as child-first, with required childAlias and parentPid attributes
	 * Don't use child-first
	 */
	@Deprecated
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public @interface ChildFirst {
		String childAlias();
		String parentPid();
	}

	/**
	 * Indicates that an AD is a copy of another AD
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=copyOf")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.METHOD)
	public @interface CopyOf {
		String value();
	}

	/**
	 * Specifies the pid of the OCD this OCD extends, where the extended class uses it's own class name as pid.  This means that this OCD includes all the AD of the extended OCD
	 * and the pid of the resulting configuration is the pid of this OCD.
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=extends")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public @interface Extends {
		Class<?> value();
	}

	/**
	 * Specifies the pid of the OCD this OCD extends.  This means that this OCD includes all the AD of the extended OCD
	 * and the pid of the resulting configuration is the pid of this OCD.
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=extends")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public @interface ExtendsPid {
		String value();
	}
	
	/**
	 * Specifies the suffix to the nested element name. Generally used without alias and for a flat AD reference.
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=extendsAlias")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public @interface ExtendsAlias {
		String value();
	}

	/**
	 * Indicates an AD cannot be changed from its default value
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=final")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.METHOD)
	public @interface Final {
		boolean value() default true;
	}

	/**
	 * Indicates a reference is flat (nested xml turnes into nested config)
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=flat")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.METHOD)
	public @interface Flat {
		boolean value() default true;
	}

	/**
	 * Indicates a reference is flat (nested xml turnes into nested config)
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=reference")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.METHOD)
	public @interface FlatReference {
		/**
		 * reference pid
		 * @return reference pid
		 */
		Class<?> value(); 
		boolean flat() default true;
		String type() default "pid";
	}

	/**
	 * Indicates a reference is flat (nested xml turnes into nested config)
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=reference")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.METHOD)
	public @interface FlatReferencePid {
		/**
		 * reference pid
		 * @return reference pid
		 */
		String value(); 
		boolean flat() default true;
		String type() default "pid";
	}

	/**
	 * Specifies the "service" strings "exposed" by this OCD.
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=objectClass")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public @interface ObjectClass {
		String[] value();
	}

	/**
	 * Specifies the "service" strings "exposed" by this OCD.
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=objectClass")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public @interface ObjectClassClass {
		Class<?>[] value();
	}

	/**
	 * Specify this AD as a reference, where the referenced class uses it's own class name as pid.
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=reference")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.METHOD)
	public @interface Reference {

		Class<?> value();

		String type() default "pid";

	}

	/**
	 * Specify this AD as a reference, supplying the pid explicitly.
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=reference")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.METHOD)
	public @interface ReferencePid {

		String value();

		String type() default "pid";

	}

	/**
	 * Used on an AD in a @Extends OCD to rename an AD from the extended type.
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=rename")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.METHOD)
	public @interface Rename {
		String value();
	}

	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=requiresFalse")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.METHOD)
	public @interface RequiresFalse {
		String value();
	}

	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=requiresTrue")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.METHOD)
	public @interface RequiresTrue {
		String value();
	}

	/**
	 * Specifies a service for object class matching
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=service")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.METHOD)
	public @interface Service {

		String value();

		String type() default "pid";

	}

	/**
	 * Specifies a service for object class matching
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=service")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.METHOD)
	public @interface ServiceClass {

		Class<?> value();

		String type() default "pid";

	}

	/**
	 * Specifies an additional service filter for object class matching using a service.
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=serviceFilter")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.METHOD)
	public @interface ServiceFilter {

		String value();

		String type() default "pid";

	}

	/**
	 * Used for parent OCD of child-first nested elements
	 * Don't use child-first
	 */
	@Deprecated
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=supportExtensions")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public @interface SupportExtensions {
		boolean value() default true;
	}

	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=supportHiddenExtensions")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public @interface SupportHiddenExtensions {
		boolean value() default true;
	}

	/**
	 * IBM extension type. 
	 * 
	 * duration(h)
	 * duration(m)
	 * duration(s)
	 * duration(ms)
	 * duration
	 * password
	 * pid
	 * onError
	 * token 
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=type")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.METHOD)
	public @interface Type {
		String value();
	}

	/**
	 * The value is a set name. The value of the AD must be unique among all AD with this set name.
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=unique")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.METHOD)
	public @interface Unique {
		String value();
	}

	/**
	 * Name of a variable supplying this AD's value.
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=variable")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.METHOD)
	public @interface Variable {
		String value();
	}
	
	/**
	 * Indicates our config processing should not substitute a variable value.
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=substitution")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.METHOD)
	public @interface VariableSubstitutionDeferred {
		String value() default "deferred";
	}
	
	/**
	 * Indicates our config processing should not substitute a variable value.
	 */
	@XMLAttribute(namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0", prefix = "ibm", mapping="value=requireExplicitConfiguration")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	public @interface RequireExplicitConfiguration {
		boolean value() default true;
	}
	
}
