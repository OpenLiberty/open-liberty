/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.anno.info;

/**
 * <p>Info object type representing a java field.</p>
 * 
 * <p>The name and qualified name of a method info object are different: The
 * qualified name of a field includes the name of the class declaring the field.</p>
 */
public interface FieldInfo extends Info {
    /**
     * <p>Answer the info object of the class which declared this field.</p>
     * 
     * @return The info object of the class which declares this field.
     */
    public ClassInfo getDeclaringClass();

    /**
     * <p>Answer the name of the type of this field.</p>
     * 
     * @return The name of the type of this field.
     */
    public String getTypeName();

    /**
     * <p>Answer the type of this field (as a class info object).</p>
     * 
     * @return The type of this field as a class info object.
     */
    public ClassInfo getType();

    //

    /** <p>Answer the default value of this field. */
    public Object getDefaultValue();

}
