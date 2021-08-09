/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.unittest.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>This annotation is used to specify the permissions associated with a 
 *   method or a class. It is used at runtime and can be applied to classes or
 *   methods.
 * </p>
 *
 * <p>SIB build component: sib.unittest.security</p>
 *
 * @author nottinga
 * @version 1.1
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Permission
{
  /**
   * A String array of permission specifications. Each String takes the format 
   * from the java.policy file which is:
   * 
   *   <Java Permission class name> [<resource>] [<action>];
   * 
   * If resource or action contains spaces then they must be enclosed in ".
   */
  public String[] value();
}