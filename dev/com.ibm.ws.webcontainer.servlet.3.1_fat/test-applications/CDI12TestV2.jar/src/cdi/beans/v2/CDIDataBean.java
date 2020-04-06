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
package cdi.beans.v2;

/**
 * CDI Testing: Common data bean type.
 */
public class CDIDataBean {

    // public CDIDataBean() {
    //     (new Throwable("Dummy for [ " + getClass().getName() + ".CDIDataBean() ]")).printStackTrace(System.out);
    // }

    /**
     * Answer the bean name.
     * 
     * This implementation always answers the simple class name of the bean.
     * 
     * @return The bean name.
     */
    public String getBeanName() {
        return getClass().getSimpleName();
    }

    /**
     * Answer the scope of this bean.
     * 
     * This default implementation always answers {@link CDICaseScope#Dependent},
     * which is the default when a scope is not specified.
     * 
     * @return The scope of this bean.
     */
    public CDICaseScope getScope() {
        return CDICaseScope.Dependent;
    }

    /**
     * Guard class for data access.
     * 
     * Application scoped beans may be accessed from multiple threads.
     * 
     * Beans of the upgrade handler may be accessed from multiple threads,
     * since the upgrade handler, read listener, and write listener each
     * have their own thread.
     */
    public class DataGuard {
        // EMPTY
    }

    /** Guard for data access. */
    protected final DataGuard dataGuard = new DataGuard();

    /** The data of this bean. */
    protected String data;

    /**
     * Standard bean setter: Set the data of this bean.
     * 
     * Note that the getter does not answer the set value.
     * The getter prefixes the data with the test subject.
     * See {@link #getData()}.
     * 
     * @param data The data to assign to this bean.
     */
    public void setData(String data) {
        synchronized (dataGuard) {
            this.data = data;
        }
    }

    /**
     * Append to the bean data.
     * 
     * @param appendData The data to append.
     * 
     * @return The bean data before the append.
     */
    public String addData(String appendData) {
        synchronized (dataGuard) {
            String oldData = data;
            if (data == null) {
                data = appendData;
            } else {
                data += ":" + appendData;
            }
            return oldData;
        }
    }

    /** Value to emit for unset data. */
    public static final String NULL_DATA = "Null";

    /**
     * Standard bean getter: Answer the bean data.
     * 
     * @return The bean data.
     */
    public String getData() {
        synchronized (dataGuard) {
            String basicData;
            if (this.data == null) {
                basicData = NULL_DATA;
            } else {
                basicData = this.data;
            }
            return getScope().getTag() + ":" + getBeanName() + ":" + basicData;
        }
    }
}
