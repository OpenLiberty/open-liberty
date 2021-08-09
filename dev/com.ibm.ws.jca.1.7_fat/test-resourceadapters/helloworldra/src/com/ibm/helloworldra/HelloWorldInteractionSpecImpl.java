/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.helloworldra;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class HelloWorldInteractionSpecImpl implements HelloWorldInteractionSpec {

    private String functionName;
    protected transient PropertyChangeSupport propertyChange;

    /**
     * Constructor for HelloWorldInteractionSpecImpl
     */
    public HelloWorldInteractionSpecImpl() {

        super();
    }

    /**
     * Gets the functionName
     *
     * @return Returns a String
     * @see HelloWorldInteractionSpec#getFunctionName()
     */
    @Override
    public String getFunctionName() {

        return functionName;
    }

    /**
     * Sets the functionName
     *
     * @param functionName The functionName to set
     * @see HelloWorldInteractionSpec#setFunctionName(String)
     */
    @Override
    public void setFunctionName(String functionName) {

        String oldFunctionName = functionName;
        this.functionName = functionName;
        firePropertyChange("FunctionName", oldFunctionName, functionName);
    }

    /**
     * The addPropertyChangeListener method was generated to support the propertyChange field.
     */
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {

        getPropertyChange().addPropertyChangeListener(listener);
    }

    /**
     * The addPropertyChangeListener method was generated to support the propertyChange field.
     */
    public synchronized void addPropertyChangeListener(
                                                       String propertyName,
                                                       PropertyChangeListener listener) {

        getPropertyChange().addPropertyChangeListener(propertyName, listener);
    }

    /**
     * The firePropertyChange method was generated to support the propertyChange field.
     */
    public void firePropertyChange(PropertyChangeEvent evt) {

        getPropertyChange().firePropertyChange(evt);
    }

    /**
     * The firePropertyChange method was generated to support the propertyChange field.
     */
    public void firePropertyChange(
                                   String propertyName,
                                   int oldValue,
                                   int newValue) {

        getPropertyChange().firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * The firePropertyChange method was generated to support the propertyChange field.
     */
    public void firePropertyChange(
                                   String propertyName,
                                   Object oldValue,
                                   Object newValue) {

        getPropertyChange().firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * The firePropertyChange method was generated to support the propertyChange field.
     */
    public void firePropertyChange(
                                   String propertyName,
                                   boolean oldValue,
                                   boolean newValue) {

        getPropertyChange().firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Accessor for the propertyChange field.
     */
    protected PropertyChangeSupport getPropertyChange() {

        if (propertyChange == null) {
            propertyChange = new PropertyChangeSupport(this);
        }
        return propertyChange;
    }

    /**
     * The hasListeners method was generated to support the propertyChange field.
     */
    public synchronized boolean hasListeners(String propertyName) {

        return getPropertyChange().hasListeners(propertyName);
    }

    /**
     * The removePropertyChangeListener method was generated to support the propertyChange field.
     */
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {

        getPropertyChange().removePropertyChangeListener(listener);
    }

    /**
     * The removePropertyChangeListener method was generated to support the propertyChange field.
     */
    public synchronized void removePropertyChangeListener(
                                                          String propertyName,
                                                          PropertyChangeListener listener) {

        getPropertyChange().removePropertyChangeListener(propertyName, listener);
    }
}