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

import java.rmi.RemoteException;
import java.util.Set;

import javax.ejb.EJBObject;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;

/**
 * The Management interface provides the APIs to navigate and
 * manipulate managed objects. The Management EJB component must
 * implement this interface as its remote interface.
 */
public interface Management extends EJBObject {

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
    public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, RemoteException;

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
    public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException, RemoteException;

    /*
     * Returns the default domain name of this MEJB.
     * Throws:
     * java.rmi.RemoteException
     */
    public String getDefaultDomain() throws RemoteException;

    /*
     * Returns the number of managed objects registered in the MEJB.
     * Throws:
     * java.rmi.RemoteException
     */
    public Integer getMBeanCount() throws RemoteException;

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
    public MBeanInfo getMBeanInfo(ObjectName name) throws IntrospectionException, InstanceNotFoundException, ReflectionException, RemoteException;

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
    public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException, RemoteException;

    /*
     * Checks whether a managed object, identified by its object name, is
     * already registered with the MEJB.
     * Throws:
     * java.rmi.RemoteException
     * Parameters:
     * name - The object name of the managed object to be checked.
     * Returns:
     * True if the managed object is already registered in the MEJB, false
     * otherwise.
     */
    public boolean isRegistered(ObjectName name) throws RemoteException;

    /*
     * Gets the names of managed objects controlled by the MEJB. This
     * method enables any of the following to be obtained: The names of all
     * managed objects, the names of a set of managed objects specified by
     * pattern matching on the ObjectName and/or a query expression, a
     * specific managed object name (equivalent to testing whether a managed
     * object is registered). When the object name is null or no domain and key
     * properties are specified, all objects are selected. It returns the set of
     * ObjectNames for the managed objects selected.
     * Throws:
     * java.rmi.RemoteException
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
    public Set queryNames(ObjectName name, QueryExp query) throws RemoteException;

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
    public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException, RemoteException;

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
    public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException, RemoteException;

    /*
     * Returns the ListenerRegistration implementation for the MEJB
     * component implementation which allows the client to register a event
     * notification listener.
     * Throws:
     * java.rmi.RemoteException
     * Returns:
     * An implementation of ListenerRegistration.
     */
    public ListenerRegistration getListenerRegistry() throws RemoteException;

}
