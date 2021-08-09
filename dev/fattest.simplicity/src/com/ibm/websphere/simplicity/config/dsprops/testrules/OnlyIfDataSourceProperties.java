/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config.dsprops.testrules;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to indicate that a test should only be executed
 * if one of the <code>DataSourceProperties</code> in the array
 * was a nested properties for the <code>DataSource</code> that was set on
 * the <code>DataSourcePropertiesOnlyRule</code>.
 * 
 * @param value one or classes of type <code>DataSourceProperties</code>.
 * 
 * @see {@link DataSourcePropertiesOnlyRule} for an example of how to use this annotation.
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OnlyIfDataSourceProperties {
    String[] value();
}