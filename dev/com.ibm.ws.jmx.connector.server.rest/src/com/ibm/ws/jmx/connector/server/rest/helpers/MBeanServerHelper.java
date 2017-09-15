/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.server.rest.helpers;

import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.RuntimeMBeanException;
import javax.management.RuntimeOperationsException;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jmx.connector.converter.JSONConverter;
import com.ibm.ws.jmx.connector.datatypes.CreateMBean;
import com.ibm.ws.jmx.connector.datatypes.ObjectInstanceWrapper;
import com.ibm.ws.jmx.connector.server.rest.APIConstants;
import com.ibm.ws.jmx.connector.server.rest.notification.ClientNotificationListener;
import com.ibm.ws.jmx.request.RequestContext;
import com.ibm.ws.jmx.request.RequestMetadata;

/**
 * This class provides the needed methods to interact with the MBeanServer.
 * 
 */
public class MBeanServerHelper {

    private static final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

    public static ObjectInstanceWrapper[] queryObjectName(final ObjectName objectName, final QueryExp queryExp, final String className, final JSONConverter converter) {
        //Query the set of object instances
        Set<ObjectInstance> instances = mbeanServer.queryMBeans(objectName, queryExp);

        //Filter based on className, if applicable
        if (className != null) {
            //NOTE: I wanted to avoid making a new List by using Iterator.remove, but
            //we aren't guaranteed that the implementation returned from the MBeanServer
            //allows it, so that's why there's another List built if filtering is needed.
            List<ObjectInstance> filtered = null;

            for (ObjectInstance instance : instances) {
                boolean isInstanceOf = false;
                try {
                    isInstanceOf = instanceOf(instance.getObjectName(), className, converter);
                } catch (Exception e) {
                    //Just let the boolean stay at false and get filtered.
                } finally {
                    if (!isInstanceOf) {
                        if (filtered == null) {
                            filtered = new LinkedList<ObjectInstance>();
                        }
                        filtered.add(instance);
                    }
                }
            }

            //Handle filtering (if any)
            if (filtered != null) {
                for (ObjectInstance filter : filtered) {
                    instances.remove(filter);
                }
            }
        }

        final ObjectInstanceWrapper[] returnInstances = new ObjectInstanceWrapper[instances.size()];

        int i = 0;
        for (ObjectInstance instance : instances) {
            ObjectInstanceWrapper arrayItem = new ObjectInstanceWrapper();
            arrayItem.objectInstance = instance;
            try {
                arrayItem.mbeanInfoURL = APIConstants.JMX_CONNECTOR_API_ROOT_PATH + "/mbeans/" + URLEncoder.encode(instance.getObjectName().getCanonicalName(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
            }

            returnInstances[i++] = arrayItem;
        }

        return returnInstances;
    }

    @FFDCIgnore({ InstanceNotFoundException.class })
    public static boolean instanceOf(ObjectName objectName, String className, JSONConverter converter) {
        try {
            return mbeanServer.isInstanceOf(objectName, className);
        } catch (InstanceNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        }
    }

    public static boolean isRegistered(ObjectName objectName) {
        return mbeanServer.isRegistered(objectName);
    }

    public static Integer getMBeanCount() {
        return mbeanServer.getMBeanCount();
    }

    public static String getDefaultDomain() {
        return mbeanServer.getDefaultDomain();
    }

    public static String[] getDomains() {
        return mbeanServer.getDomains();
    }

    @FFDCIgnore({ InstanceNotFoundException.class, IntrospectionException.class, ReflectionException.class })
    public static MBeanInfo getMBeanInfo(ObjectName objectNameObj) {
        try {
            return mbeanServer.getMBeanInfo(objectNameObj);
        } catch (InstanceNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);
        } catch (IntrospectionException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } catch (ReflectionException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }
    }

    @FFDCIgnore({ InstanceNotFoundException.class, ReflectionException.class, MBeanException.class, RuntimeMBeanException.class })
    public static Object invoke(ObjectName objectName,
                                String operation,
                                Object[] params,
                                String[] signature,
                                JSONConverter converter) {
        try {
            RequestMetadata metadata = new RequestMetadata();
            RequestContext.setRequestMetadata(metadata);
            return mbeanServer.invoke(objectName, operation, params, signature);
        } catch (InstanceNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (MBeanException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } catch (ReflectionException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (RuntimeMBeanException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } finally {
            RequestContext.removeRequestMetadata();
        }
    }

    @FFDCIgnore({ ReflectionException.class, InstanceAlreadyExistsException.class, NotCompliantMBeanException.class,
                 InstanceNotFoundException.class, MBeanException.class, RuntimeMBeanException.class })
    public static ObjectInstance createMBean(CreateMBean createMBean,
                                             JSONConverter converter) {
        try {
            if (createMBean.useLoader && createMBean.useSignature) {
                //Both are enabled, so use 5-param method
                return mbeanServer.createMBean(createMBean.className,
                                               createMBean.objectName,
                                               createMBean.loaderName,
                                               createMBean.params,
                                               createMBean.signature);
            } else if (createMBean.useSignature) {
                //only useSignature is enabled, so use 4-param method
                return mbeanServer.createMBean(createMBean.className,
                                               createMBean.objectName,
                                               createMBean.params,
                                               createMBean.signature);
            } else if (createMBean.useLoader) {
                //only useLoader is enabled, so use 3-param method
                return mbeanServer.createMBean(createMBean.className,
                                               createMBean.objectName,
                                               createMBean.loaderName);
            } else {
                //both are disabled, so use 2-param method
                return mbeanServer.createMBean(createMBean.className,
                                               createMBean.objectName);
            }
        } catch (ReflectionException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (InstanceAlreadyExistsException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (MBeanException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } catch (NotCompliantMBeanException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (InstanceNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (RuntimeMBeanException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }

    }

    @FFDCIgnore({ InstanceNotFoundException.class, MBeanRegistrationException.class })
    public static void unregisterMBean(ObjectName objectNameObj) {
        try {
            mbeanServer.unregisterMBean(objectNameObj);
        } catch (InstanceNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);
        } catch (MBeanRegistrationException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }
    }

    @FFDCIgnore({ InstanceNotFoundException.class, ReflectionException.class })
    public static AttributeList getAttributes(ObjectName objectNameObj, String[] attributes) {
        try {
            return mbeanServer.getAttributes(objectNameObj, attributes);
        } catch (InstanceNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);
        } catch (ReflectionException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }
    }

    @FFDCIgnore({ InstanceNotFoundException.class, ReflectionException.class })
    public static AttributeList setAttributes(ObjectName objectNameObj,
                                              AttributeList attributeList,
                                              JSONConverter converter) {
        try {
            return mbeanServer.setAttributes(objectNameObj, attributeList);
        } catch (InstanceNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (ReflectionException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }
    }

    @FFDCIgnore({ InstanceNotFoundException.class, AttributeNotFoundException.class, ReflectionException.class, MBeanException.class, RuntimeMBeanException.class })
    public static Object getAttribute(ObjectName objectNameObj, String attributeName) {
        try {
            return mbeanServer.getAttribute(objectNameObj, attributeName);
        } catch (InstanceNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);
        } catch (ReflectionException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } catch (AttributeNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);
        } catch (MBeanException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } catch (RuntimeMBeanException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }
    }

    @FFDCIgnore({ InvalidAttributeValueException.class, InstanceNotFoundException.class, AttributeNotFoundException.class, ReflectionException.class, MBeanException.class,
                 RuntimeMBeanException.class })
    public static void setAttribute(ObjectName objectNameObj, Attribute attribute, JSONConverter converter) {
        try {
            mbeanServer.setAttribute(objectNameObj, attribute);
        } catch (InvalidAttributeValueException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (InstanceNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (ReflectionException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } catch (AttributeNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (MBeanException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } catch (RuntimeMBeanException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }
    }

    @FFDCIgnore({ InstanceNotFoundException.class, RuntimeOperationsException.class })
    public static void addClientNotification(ObjectName objectName,
                                             ClientNotificationListener listener,
                                             NotificationFilter filter,
                                             Object handback,
                                             JSONConverter converter) {
        try {
            mbeanServer.addNotificationListener(objectName, listener, filter, handback);
        } catch (InstanceNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (RuntimeOperationsException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }
    }

    @FFDCIgnore({ InstanceNotFoundException.class, RuntimeOperationsException.class })
    public static void addServerNotification(ObjectName objectName,
                                             ObjectName listener,
                                             NotificationFilter filter,
                                             Object handback,
                                             JSONConverter converter) {
        try {
            mbeanServer.addNotificationListener(objectName, listener, filter, handback);
        } catch (InstanceNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (RuntimeOperationsException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }
    }

    @FFDCIgnore({ InstanceNotFoundException.class, ListenerNotFoundException.class })
    public static void removeClientNotification(ObjectName objectName,
                                                NotificationListener listener) {

        try {
            mbeanServer.removeNotificationListener(objectName, listener);
        } catch (InstanceNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);
        } catch (ListenerNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }
    }

    @FFDCIgnore({ InstanceNotFoundException.class, ListenerNotFoundException.class })
    public static void removeServerNotification(ObjectName objectName,
                                                ObjectName listener,
                                                NotificationFilter filter,
                                                Object handback,
                                                JSONConverter converter) {

        try {
            mbeanServer.removeNotificationListener(objectName, listener, filter, handback);
        } catch (InstanceNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (ListenerNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }
    }

    @FFDCIgnore({ InstanceNotFoundException.class, ListenerNotFoundException.class })
    public static void removeServerNotification(ObjectName objectName,
                                                ObjectName listener,
                                                JSONConverter converter) {

        try {
            mbeanServer.removeNotificationListener(objectName, listener);
        } catch (InstanceNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (ListenerNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }
    }

    //For debug only
    public static String getMBeanServerName() {
        return mbeanServer.getClass().getName();
    }
}
