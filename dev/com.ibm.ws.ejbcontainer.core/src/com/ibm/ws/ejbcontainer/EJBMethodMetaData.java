/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer;

import java.lang.reflect.Method;
import java.util.List;

import com.ibm.ws.runtime.metadata.MethodMetaData;

/**
 * The metadata for a method belonging to a method interface type.
 */
public interface EJBMethodMetaData
                extends MethodMetaData
{
    /**
     * @return the owning EJB component metadata
     * @see MethodMetaData#getComponentMetaData
     */
    EJBComponentMetaData getEJBComponentMetaData();

    /**
     * @return the EJB method interface
     */
    EJBMethodInterface getEJBMethodInterface();

    /**
     * @return the method name
     */
    String getMethodName();

    /**
     * @return a method from one of the interfaces
     */
    Method getMethod();

    /**
     * Get the JVM method descriptor string for the parameter and return types.
     * The format is "(", then each parameter type separated by a string, then
     * ")", then the return type. Primitive types are encoded using a single
     * character ("V", "Z", "C", "B", "S", "I", "F", "J", and "D"), class types
     * are encoded with the class name using "/" as a package delimiter, and
     * array types are prefixed with "[" for each dimension.
     * 
     * @return the JVM method descriptor string
     */
    String getMethodDescriptor();

    /**
     * Get the signature for this method. The format is the method name, then
     * ":", and then optionally the method parameter types separated by ",".
     * Primitive types are spelled as they are in Java (for example, "int"),
     * and array types are suffixed with "[]" for each dimension.
     * 
     * @return the method signature
     */
    String getMethodSignature();

    /**
     * @return the transaction attribute
     */
    EJBTransactionAttribute getEJBTransactionAttribute();

    /**
     * @return true if this method is a stateful remove method
     */
    boolean isStatefulRemove();

    /**
     * Return boolean indicating that no security roles are allowed
     * to execute this method.
     * 
     * @return boolean indicating if all roles are not to be permitted to execute
     *         this method.
     */
    boolean isDenyAll();

    /**
     * Return boolean indicating that all security roles are allowed
     * to execute this method.
     * 
     * @return boolean indicating if all roles are permitted to execute
     *         this method.
     */
    boolean isPermitAll();

    /**
     * Return a list containing all security roles that are
     * allowed to execute this method.
     * 
     * @return List of strings containing all security roles that are
     *         allowed to execute this method.
     */
    List<String> getRolesAllowed();

    /**
     * Return a boolean indicating if the identity for the execution of this
     * method is to come from the caller.
     * 
     * @return boolean indicating if the identity for the execution of this
     *         method is to come from the caller.
     */
    boolean isUseCallerPrincipal();

    /**
     * Return a boolean indicating if the identity for the execution of this
     * method is the system principle.
     * 
     * @return boolean indicating if the identity for the execution of this
     *         method is the system principle.
     */
    boolean isUseSystemPrincipal();

    /**
     * Return a String indicating the run-as identity for the execution of this
     * method.
     * 
     * @return String indicating the run-as identity for the execution of this
     *         method.
     */
    String getRunAs();
}
