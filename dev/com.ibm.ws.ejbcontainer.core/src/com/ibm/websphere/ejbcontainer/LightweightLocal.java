/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ejbcontainer;

/**
 * Marker interface to indicate that an entity bean should run in
 * 'lightweight local' mode. <p>
 * 
 * In lightweight local mode, the container streamlines the processing
 * that it performs before and after every method on the bean's local
 * home interface and local business interface. This streamlining can
 * result in improved performance when EntityBean operations are called
 * locally from within an application. Because some processing is skipped
 * when running in lightweight local mode, it can only be used in certain
 * scenarios. <p>
 * 
 * <b>Note: Enabling this option results in behavior not compliant with the
 * official EJB specification.</b> <p>
 * 
 * Lightweight local mode is patterned somewhat after the 'Plain Old Java Object'
 * (POJO) Entity model introduced in the EJB 3.0 specification. Using
 * lightweight local mode, you can obtain some of the performance advantages
 * of the POJO Entity model without having to convert your existing EJB 2.x
 * application code to the new POJO model. Subject to the conditions below,
 * lightweight local mode may be applied to both container-managed persistence
 * (CMP) and bean-managed persistence (BMP) entity types. <p>
 * 
 * You may only apply lightweight local mode to an EntityBean that meets the
 * following criteria:
 * <ul>
 * <li> The bean implements an EJB local interface
 * <li> No security authorization is defined on the EntityBean local home or
 * local business interface methods
 * <li> No "run-as" security attribute is defined on the local home or local
 * business methods
 * <li> The classes for the Calling bean and called EntityBean were loaded by
 * the same Java classloader
 * <li> The EntityBean methods do not call the WebSphere-specific
 * Internationalization or WorkArea services.
 * </ul> <p>
 * 
 * The first criterion prevents CMP 1.x beans from supporting lightweight
 * local mode, since they cannot have local interfaces. <p>
 * 
 * In addition, lightweight local mode will only provide its fullest
 * performance benefits to EntityBean methods that do not need to start
 * a global transaction. This condition will be true if you ensure that
 * your EntityBean also meets the following criteria:
 * <ul>
 * <li> A global transaction is already in effect when the EntityBean home
 * or business method is called (typically this transaction is started
 * by the calling SessionBean)
 * <li> The EntityBean's local business interface methods and the bean's
 * local home methods use only the following transaction attributes:
 * REQUIRED, SUPPORTS, or MANDATORY.
 * </ul> <p>
 * 
 * If an EntityBean method running in lightweight local mode must start a
 * global transaction, it will still function normally but only a partial
 * performance benefit will be seen. <p>
 * 
 * Implementing this interface on an entity bean causes equivalent behavior
 * as setting the lightweightLocal field for that bean in the IBM deployment
 * descriptor extensions (ibm-ejb-jar-ext.xmi) to "true". For scenarios
 * where many beans extend a single root class, it can be more convenient
 * to have that class implement this marker interface than to individually
 * set the environment variable on each of the beans.
 * Setting either the deployment descriptor extension or the environment
 * variable to a value of "false" has no effect; a "false" value does not
 * override the presence of the marker interface. <p>
 * 
 * An EntityBean that defines a remote interface or TimedObject interface
 * (in addition to the local interface) may still be marked for lightweight
 * local mode, but the performance benefit will only be realized when the
 * bean is called through its local interface. If lightweight local mode is
 * applied to any other bean type, that bean will not be allowed to run in
 * the EJB container. <p>
 * 
 * @since WAS 6.1.0
 * @ibm-api
 **/

public interface LightweightLocal
{
    // Empty interface, used only as a marker.
}
