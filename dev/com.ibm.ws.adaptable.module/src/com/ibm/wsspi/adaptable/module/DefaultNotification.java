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
package com.ibm.wsspi.adaptable.module;

import java.util.Collection;
import java.util.Collections;

import com.ibm.wsspi.adaptable.module.Notifier.Notification;

/**
 * Default implementation of the {@link Notification} interface.
 */
public class DefaultNotification implements Notification {
    private final Container root;
    private final Collection<String> paths;

    /**
     * Constructs a Notification object for a single path (will be converted to a singleton collection)<p>
     * Path must be absolute, and the container passed must be from the notifier the notification is used for.
     * <p>
     * Path may be prefixed with '!' to mean 'non recursive' eg.<br>
     * <ul>
     * <li> /WEB-INF <em>(the /WEB-INF directory, and all files/dirs beneath it recursively.)</em>
     * <li> / <em>(all files/dirs in the entire container)</em>
     * <li> !/META-INF <em>(the /META-INF directory and its immediate children)</em>
     * <li> !/ <em>(the container itself, and entries directly on its root.)</em>
     * </ul>
     * 
     * @param root
     * @param path
     */
    public DefaultNotification(Container root, String path) {
        this(root, Collections.singleton(path));
    }

    /**
     * Constructs a Notification object <p>
     * Paths must be absolute, and the container passed must be from the notifier the notification is used for.
     * <p>
     * Paths may be prefixed with '!' to mean 'non recursive' eg.<br>
     * <ul>
     * <li> /WEB-INF <em>(the /WEB-INF directory, and all files/dirs beneath it recursively.)</em>
     * <li> / <em>(all files/dirs in the entire container)</em>
     * <li> !/META-INF <em>(the /META-INF directory and its immediate children)</em>
     * <li> !/ <em>(the container itself, and entries directly on its root.)</em>
     * </ul>
     * 
     * @param root the container to check the paths against. Must not be null.
     * @param paths the collection of paths to check. Must not be null.
     * @throws IllegalArgumentException if either argument is null.
     */
    public DefaultNotification(Container root, Collection<String> paths) {
        super();
        if (root == null) {
            throw new IllegalArgumentException();
        }
        this.root = root;
        if (paths == null) {
            throw new IllegalArgumentException();
        }
        this.paths = paths;
    }

    /**
     * @return the associated container
     */
    @Override
    public Container getContainer() {
        return root;
    }

    /**
     * @return the paths
     */
    @Override
    public Collection<String> getPaths() {
        return paths;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" + this.getContainer() + "::" + this.getPaths() + ")";
    }

}
