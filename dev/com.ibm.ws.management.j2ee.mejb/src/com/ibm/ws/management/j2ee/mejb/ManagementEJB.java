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
package com.ibm.ws.management.j2ee.mejb;

import java.lang.management.ManagementFactory;
import java.rmi.RemoteException;
import java.util.Set;

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.j2ee.ListenerRegistration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * The implementation for the MEJB. This object exists on the liberty server.
 * 
 */
public class ManagementEJB implements SessionBean, ListenerRegistration {

    private static final long serialVersionUID = 6606640063058812575L;

    private static final TraceComponent tc = Tr.register(ManagementEJB.class);

    private SessionContext ctx;
    private transient MBeanServer mbeanServer;

    /**
     * Constructor for ManagementEJB.
     */
    public ManagementEJB() {}

    public void ejbCreate() {}

    @Override
    public void setSessionContext(SessionContext ctx) throws EJBException, RemoteException {
        this.ctx = ctx;
    }

    @Override
    public void ejbRemove() throws EJBException, RemoteException {
        mbeanServer = null;
    }

    @Override
    public void ejbActivate() throws EJBException, RemoteException {}

    @Override
    public void ejbPassivate() throws EJBException, RemoteException {
        mbeanServer = null;
    }

    /*
     * Gets the names of managed objects controlled by the MEJB. This
     * method enables any of the following to be obtained: The names of all
     * managed objects, the names of a set of managed objects specified by
     * pattern matching on the ObjectName and/or a query expression, a
     * specific managed object name (equivalent to testing whether a managed
     * object is registered). When the object name is null or no domain and key
     * properties are specified, all objects are selected. It returns the set of
     * ObjectNames for the managed objects selected.
     * Parameters:
     * name - The object name pattern identifying the managed objects to be
     * retrieved. If null or no domain and key properties are specified, all the
     * managed objects registered will be retrieved.
     * query - The query expression to be applied for selecting managed
     * objects. If null no query expression will be applied for selecting
     * managed objects.
     * Returns:
     * A set containing the ObjectNames for the managed objects selected. If
     * no managed object satisfies the query, an empty set is returned.
     */
    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) throws RemoteException {

        Set<ObjectName> s = getMBeanServer().queryNames(name, query);

        return s;
    }

    /*
     * Checks whether a managed object, identified by its object name, is
     * already registered with the MEJB.
     * name - The object name of the managed object to be checked.
     * Returns:
     * True if the managed object is already registered in the MEJB, false
     * otherwise.
     */
    public boolean isRegistered(ObjectName name) throws RemoteException {

        boolean b = getMBeanServer().isRegistered(name);

        return b;

    }

    /*
     * Returns the number of managed objects registered in the MEJB.
     */
    public Integer getMBeanCount() throws RemoteException {

        Integer i = getMBeanServer().getMBeanCount();

        return i;
    }

    /*
     * This method discovers the attributes and operations that a managed
     * object exposes for management.
     * Throws:
     * javax.management.IntrospectionException
     * javax.management.InstanceNotFoundException
     * javax.management.ReflectionException
     * java.rmi.RemoteException
     * Parameters:
     * name - The object name of the managed object to analyze.
     * Returns:
     * An instance of javax.management.MBeanInfo allowing the retrieval
     * of all attributes and operations of this managed object.
     */
    public MBeanInfo getMBeanInfo(ObjectName name)
                    throws IntrospectionException, InstanceNotFoundException, ReflectionException {

        MBeanInfo mInfo = getMBeanServer().getMBeanInfo(name);

        return mInfo;

    }

    /*
     * Gets the value of a specific attribute of a named managed object. The
     * managed object is identified by its object name.
     * Throws:
     * javax.management.MBeanException
     * javax.management.AttributeNotFoundException
     * javax.management.InstanceNotFoundException
     * javax.management.ReflectionException
     * java.rmi.RemoteException
     * Parameters:
     * name - The object name of the managed object from which the attribute
     * is to be retrieved.
     * attribute - A String specifying the name of the attribute to be
     * retrieved.
     * Returns:
     * The value of the retrieved attribute.
     */
    public Object getAttribute(ObjectName name, String attribute)
                    throws MBeanException, AttributeNotFoundException, InstanceNotFoundException,
                    ReflectionException {

        Object o = getMBeanServer().getAttribute(name, attribute);

        return o;
    }

    /*
     * Gets the values of several attributes of a named managed object. The
     * managed object is identified by its object name.
     * Throws:
     * javax.management.InstanceNotFoundException
     * javax.management.ReflectionException
     * java.rmi.RemoteException
     * Parameters:
     * name - The object name of the managed object from which the
     * attributes are retrieved.
     * attributes - A list of the attributes to be retrieved.
     * Returns:
     * An instance of javax.management.AttributeList which contains a list of
     * the retrieved attributes as javax.management.Attribute instances.
     */
    public AttributeList getAttributes(ObjectName name, String[] attributes)
                    throws InstanceNotFoundException, ReflectionException {

        AttributeList a = getMBeanServer().getAttributes(name, attributes);

        return a;
    }

    /*
     * Sets the value of a specific attribute of a named managed object. The
     * managed object is identified by its object name.
     * Throws:
     * javax.management.InstanceNotFoundException
     * javax.management.AttributeNotFoundException
     * javax.management.InvalidAttributeValueException
     * javax.management.MBeanException
     * javax.management.ReflectionException
     * java.rmi.RemoteException
     * Parameters:
     * name - The name of the managed object within which the attribute is to
     * be set.
     * attribute - The identification of the attribute to be set and the value
     * it is to be set to.
     * Returns:
     * The value of the attribute that has been set.
     */
    public void setAttribute(ObjectName name, Attribute attribute)
                    throws InstanceNotFoundException, AttributeNotFoundException,
                    InvalidAttributeValueException, MBeanException, ReflectionException {

        getMBeanServer().setAttribute(name, attribute);

    }

    /*
     * Sets the values of several attributes of a named managed object. The
     * managed object is identified by its object name.
     * Throws:
     * javax.management.InstanceNotFoundException
     * javax.management.ReflectionException
     * java.rmi.RemoteException
     * Parameters:
     * name - The object name of the managed object within which the
     * attributes are to be set.
     * attributes - A list of attributes: The identification of the attributes
     * to be set and the values they are to be set to.
     * Returns:
     * The list of attributes that were set, with their new values.
     */
    public AttributeList setAttributes(ObjectName name, AttributeList attributes)
                    throws InstanceNotFoundException, ReflectionException {

        AttributeList a = getMBeanServer().setAttributes(name, attributes);

        return a;
    }

    /*
     * Invokes an operation on a managed object.
     * Throws:
     * javax.management.InstanceNotFoundException
     * javax.management.MBeanException
     * javax.management.ReflectionException
     * java.rmi.RemoteException
     * Parameters:
     * name - The object name of the managed object on which the method is
     * to be invoked.
     * operationName - The name of the operation to be invoked.
     * params - An array containing the parameters to be set when the
     * operation is invoked
     * signature - An array containing the signature of the operation. Each
     * element of the array contains a fully-qualified name of the entity (class,
     * interface, array class, primitive type) that corresponds with a parameter
     * type in the method’s signature. The format of the strings must be as
     * specified by java.lang.Class.getName(). The class objects will be loaded
     * using the same class loader as the one used for loading the managed
     * object on which the operation was invoked.
     * Returns:
     * The object returned by the operation, which represents the result of
     * invoking the operation on the managed object specified.
     */
    public Object invoke(ObjectName name, String operationName, Object[] params,
                         String[] signature)
                    throws InstanceNotFoundException, MBeanException, ReflectionException {

        Object o = getMBeanServer().invoke(name, operationName, params, signature);

        return o;
    }

    /*
     * Returns the default domain name of this MEJB.
     */
    public String getDefaultDomain() throws RemoteException {

        String s = getMBeanServer().getDefaultDomain();

        return s;
    }

    // the method is optional and we do not support it in liberty
    /*
     * Returns the ListenerRegistration implementation for the MEJB
     * component implementation which allows the client to register a event
     * notification listener.
     * Returns:
     * An implementation of ListenerRegistration.
     */
    public ListenerRegistration getListenerRegistry() {

        return this;
    }

    @Override
    public void addNotificationListener(ObjectName name,
                                        NotificationListener listener,
                                        NotificationFilter filter,
                                        Object handback)
                    throws InstanceNotFoundException {

        getMBeanServer().addNotificationListener(name, listener, filter, handback);

    }

    @Override
    public void removeNotificationListener(ObjectName name,
                                           NotificationListener listener)
                    throws InstanceNotFoundException, ListenerNotFoundException {

        getMBeanServer().removeNotificationListener(name, listener);

    }

    // get liberty MBean server  
    private MBeanServer getMBeanServer() {
        if (mbeanServer == null) {
            mbeanServer = ManagementFactory.getPlatformMBeanServer();
        }
        return mbeanServer;
    }

}
