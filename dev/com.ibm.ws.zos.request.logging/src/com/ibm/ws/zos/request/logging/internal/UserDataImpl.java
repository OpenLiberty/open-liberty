/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.request.logging.internal;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.zos.request.logging.UserData;
import com.ibm.wsspi.resource.ResourceFactory;

/**
 * UserData api implementation to allow callers to log custom data to a SMF record.
 */
@Component(service = { UserData.class }, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class UserDataImpl implements UserData {

    protected static final ThreadLocal<HashMap<Integer, byte[]>> userDataMap = new ThreadLocal<HashMap<Integer, byte[]>>() {
        @Override
        protected HashMap<Integer, byte[]> initialValue() {
            return new HashMap<Integer, byte[]>();
        }
    };

    /** An empty byte array used to pad user data to length */
    private static final byte[] nulls = new byte[UserData.USER_DATA_MAX_SIZE];

    /**
     * Class names that we register under osgi.jndi.service.name in the service registry.
     */
    final static String[] svcRegClassNames = new String[] { UserDataFactory.class.getName(), ResourceFactory.class.getName() };

    ServiceRegistration<?> jndiRegistration;
    UserDataFactory userDataFactory;

    /**
     * DS method to activate this component.
     *
     * @param properties : Map containing service properties
     */
    @Activate
    protected void activate(BundleContext bundleContext, Map<String, Object> configuration) {
        Dictionary<String, Object> map = new Hashtable<String, Object>();
        map.put(ResourceFactory.JNDI_NAME, "com/ibm/websphere/zos/request/logging/UserData");
        map.put(ResourceFactory.CREATES_OBJECT_CLASS, UserData.class.getName());
        userDataFactory = new UserDataFactory(this);
        jndiRegistration = bundleContext.registerService(svcRegClassNames, userDataFactory, map);
    }

    /**
     * DS method to deactivate this component.
     */
    @Deactivate
    protected void deactivate(BundleContext bundleContext) {
        // Unregister the JNI service.
        if (jndiRegistration != null) {
            jndiRegistration.unregister();
            userDataFactory = null;
        }
    }

    /**
     * Little routine to make an int into a byte array
     *
     * @param Int an int
     * @return the int as a byte array
     */
    private static byte[] intToBytes(int Int) {
        return new byte[] { (byte) (Int >> 24), (byte) (Int >> 16), (byte) (Int >> 8), (byte) (Int) };
    }

    /** {@inheritDoc} */
    @Override
    public int add(int identifier, byte[] dataBytes) {

        int rc = ADD_DATA_OK;

        if (dataBytes != null) {
            if (dataBytes.length > USER_DATA_MAX_SIZE) {
                rc = ADD_DATA_FAILED_TOO_BIG;
            } else {
                // Ready...
                byte[] userDataVersion = intToBytes(CURRENT_USER_DATA_BLOCK_VERSION);
                byte[] userDataType = intToBytes(identifier);
                byte[] userDataLength = intToBytes(dataBytes.length);

                // Set...
                ByteArrayOutputStream userData = new ByteArrayOutputStream();

                // Go...
                userData.write(userDataVersion, 0, userDataVersion.length);
                userData.write(userDataType, 0, userDataType.length);
                userData.write(userDataLength, 0, userDataLength.length);
                userData.write(dataBytes, 0, dataBytes.length);

                // Oh yeah...pad user data with nulls
                userData.write(nulls, 0, nulls.length - dataBytes.length);

                // If we've already got this userdata tag in our map, get it
                HashMap<Integer, byte[]> threadUserData = userDataMap.get();
                byte[] existingData = threadUserData.get(Integer.valueOf(identifier));

                // If we found existing data, replace it
                if (existingData != null) {
                    threadUserData.put(Integer.valueOf(identifier), userData.toByteArray());
                    rc = ADD_DATA_REPLACED_DATA;
                }

                // So its new data..do we already have as many as we allow? Reject if so...
                else if (threadUserData.size() == USER_DATA_MAX_COUNT) {
                    // already as much data as we allow
                    rc = ADD_DATA_FAILED_TOO_MANY;
                }

                // Ok to just add new data to the map
                else {
                    threadUserData.put(Integer.valueOf(identifier), userData.toByteArray());
                }
            }
        } else {
            rc = ADD_DATA_FAILED_DATA_NULL;
        }

        return rc;

    }

    /** {@inheritDoc} */
    @Override
    public int add(int identifier, String data) {

        int rc;
        if (data != null) {
            if (data.length() != 0) {
                if (data.length() > USER_DATA_MAX_SIZE) {
                    rc = ADD_DATA_FAILED_TOO_BIG;
                } else {
                    byte[] dataBytes;
                    try {
                        dataBytes = data.getBytes("Cp1047");
                        rc = add(identifier, dataBytes);
                    } catch (UnsupportedEncodingException e) {
                        rc = ADD_DATA_FAILED_CONVERSION_ERROR;
                    }
                }
            } else {
                rc = ADD_DATA_FAILED_DATA_LENGTH_ZERO;
            }
        } else {
            rc = ADD_DATA_FAILED_DATA_NULL;
        }
        return rc;

    }

    protected HashMap<Integer, byte[]> getUserDataBytes() {

        HashMap<Integer, byte[]> threadUserData = userDataMap.get();
        // Clear the thread user data
        userDataMap.remove();
        return threadUserData;
    }

    protected void clearUserData() {
        // Clear the thread user data
        userDataMap.remove();
    }

}
