/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.util.dopriv;

import java.lang.reflect.AccessibleObject;
import java.security.PrivilegedExceptionAction;

/**
 * This is a PrivilegedAction that is used to call the setAccessible
 * method on a java.lang.reflect.Accessible object that allows accessibility
 * to be changed (e.g. Method or Field). The primary use of this
 * method is to allow WebSphere Application Server code to call private
 * methods or set private fields in a class provided by the application.
 * 
 * The intended use pattern is as follows:
 * <p>
 * <ul>
 * <li> SetAccessiblePrivilegedAction pa = new SetAccessiblePrivilegedAction( method, true );
 * <li> try {
 * <li> AccessController.doPrivileged( pa );
 * <li> }
 * <li> catch ( PrivilegedActionException pae ) {
 * <li> FFDCFilter.processException( pae, CLASS_NAME + ".xxxx", "id" );
 * <li> SecurityException rootex = (SecurityException)pae.getException();
 * <li> throw rootex;
 * <li> }
 * </ul>
 * <p>
 * Alternatively, to reuse the same SetAccessiblePriviledgedAction object for
 * different Method objects, a final static variable can be declared as follows:
 * <pre>
 * private final static SetAccessiblePrivilegedAction cvPA = new SetAccessiblePrivilegedAction();
 * </pre>
 * <p>
 * Then the static variable would be used as follows whenever for each Method object:
 * <ul>
 * <li> cvPA.setParameters( method, true );
 * <li> try {
 * <li> AccessController.doPrivileged( cvPA );
 * <li> }
 * <li> catch ( PrivilegedActionException pae ) {
 * <li> FFDCFilter.processException( pae, CLASS_NAME + ".xxxx", "id" );
 * <li> SecurityException rootex = (SecurityException)pae.getException();
 * <li> throw rootex;
 * <li> }
 * </ul>
 * <p>
 */
public class SetAccessiblePrivilegedAction implements PrivilegedExceptionAction<AccessibleObject> {
    /**
     * The java reflection AccessibleObject that is the target of the setAccessible call.
     */
    private AccessibleObject ivObject;

    /**
     * The boolean value to pass as an argument to setAccessible method.
     */
    private boolean ivAccessible;

    /**
     * Use this CTOR in conjunction with the setAccessible method of this class
     * if you wish to reuse this object for setting accessible in
     * different AccessibleObject objects as described in description of this class.
     */
    public SetAccessiblePrivilegedAction() {
        ivObject = null;
        ivAccessible = true;
    }

    /**
     * Use this CTOR if you only need to use this object once for setting accessible
     * for a specified AccessibleObject object.
     * 
     * @param obj is the AccessibleObject object that is the target of the setAccessible invocation.
     * @param accessible is the boolean argument for the setAccessible invocation.
     */
    public SetAccessiblePrivilegedAction(AccessibleObject obj, boolean accessible) {
        ivObject = obj;
        ivAccessible = accessible;
    }

    /**
     * Set parameters used by this object.
     * 
     * @param obj is the AccessibleObject object that is the target of the setAccessible invocation.
     * @param accessible is the boolean argument for the setAccessible invocation.
     */
    public void setParameters(AccessibleObject obj, boolean accessible) {
        ivObject = obj;
        ivAccessible = accessible;
    }

    /**
     * Performs the setAccessible priviledged action whenever AccessController.doPrivileged( this )
     * is executed. Note, once executed, the setParameters method of this class must be used
     * before AccessController.doPrivileged( this ) is executed again. If setParameters is not called,
     * this method will return without ever calling setAccessible.
     * 
     * @return the AccessibleObject that setAccessible was performed on or null if doPriviledged was
     *         executed without a preceeding setParameters method call.
     * 
     * @throws SecurityException if the AccessibleObject is one where accessibility may not be
     *             changed (for example, if this element object is a Constructor object for the class Class).
     */
    public AccessibleObject run() throws SecurityException {
        AccessibleObject rv = ivObject;
        ivObject = null;
        if (rv != null) {
            rv.setAccessible(ivAccessible);
        }
        return rv;
    }

}
