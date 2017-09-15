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
package javax.management.j2ee;

import java.io.Serializable;
import java.rmi.RemoteException;

import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

/**
 * Provides the methods to add and remove event listeners
 */
public interface ListenerRegistration extends Serializable {

    /*
     * Add a listener to a registered managed object.
     * Throws:
     * javax.management.InstanceNotFoundException
     * java.rmi.RemoteException
     * Parameters:
     * name - The name of the managed object on which the listener should be
     * added.
     * listener - The listener object which will handle the events emitted
     * by the registered managed object.
     * filter - The filter object. If filter is null, no filtering will be
     * performed before handling events.
     * handback - An opaque object to be sent back to the listener when a
     * notification is emitted which helps the listener to associate information
     * regarding the MBean emitter. This object cannot be used by the
     * Notification broadcaster object. It should be resent unchanged with the
     * notification to the listener.
     */
    public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws RemoteException, InstanceNotFoundException;

    /*
     * Enables to remove a listener from a registered managed object.
     * Throws:
     * javax.management.InstanceNotFoundException
     * javax.management.ListenerNotFoundException
     * java.rmi.RemoteException
     * Parameters:
     * name - The name of the managed object on which the listener should be
     * removed.
     * listener - The listener object which will handle the events emitted
     * by the registered managed object. This method will remove all the
     * information related to this listener.
     */
    public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException, ListenerNotFoundException, RemoteException;

}
