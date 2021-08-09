/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.jsp.context;

/**
 * The JspClassloaderContext interface allows the control of what ClassLoader is used to load JSP dependent
 * Classes (taglibrary classes etc.)
 */
public interface JspClassloaderContext {
    /**
     * @return ClassLoader The ClassLoader to be used to load JSP dependent classes.
     */
    ClassLoader getClassLoader();
    /**
     * @return String The classpath to be used to compile the generated servlet.
     */
    String getClassPath();
    /**
     * @return String The Optional Optimized classpath to be used to compile the generated servlet.
     */
    String getOptimizedClassPath();
    /**
     * @return boolean Indicates whether this classloader supports predefining classes.
     */
    boolean isPredefineClassEnabled();
    /**
     * @return byte[] Returns the predefined class bytes.
     */
    byte[] predefineClass(String className, byte[] classData);
}
