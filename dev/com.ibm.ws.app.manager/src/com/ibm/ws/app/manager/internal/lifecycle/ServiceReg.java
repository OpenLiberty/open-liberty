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
package com.ibm.ws.app.manager.internal.lifecycle;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 *
 */
public class ServiceReg<T> {
    private final AtomicReference<ServiceRegistration<T>> _reg = new AtomicReference<ServiceRegistration<T>>();
    private final AtomicReference<Hashtable<String, Object>> _props = new AtomicReference<Hashtable<String, Object>>();
    private final ConcurrentMap<String, Object> _additionalProps = new ConcurrentHashMap<String, Object>();
    private final AtomicBoolean _dirty = new AtomicBoolean();
    private final AtomicBoolean _registering = new AtomicBoolean();

    @SuppressWarnings("unchecked")
    @FFDCIgnore(IllegalStateException.class)
    private void setPropertiesInternal(@SuppressWarnings("rawtypes") Dictionary props) {
        ServiceRegistration<T> reg = _reg.get();
        if (reg == null) {
            _dirty.set(true);
        } else {
            try {
                reg.setProperties(props);
            } catch (IllegalStateException e) {
                // This occurs if the registration is unregistered and since we are trying to unregister
                // I think it is probably safe to ignore this.
                _reg.compareAndSet(reg, null);
            }
        }
    }

    public void setProperties(Map<String, ?> props) {
        Hashtable<String, Object> ps = new Hashtable<String, Object>(props);
        ps.putAll(_additionalProps);
        _props.set(ps);
        setPropertiesInternal(ps);
    }

    public boolean setProperty(String name, Object value) {
        Object original = _additionalProps.put(name, value);
        if (!!!value.equals(original)) {
            Hashtable<String, Object> props = _props.get();
            if (props != null) {
                props.put(name, value);
                setPropertiesInternal(props);
            }
            return true;
        }
        return false;
    }

    @FFDCIgnore(IllegalStateException.class)
    public void register(BundleContext ctx, Class<T> clazz, T service) {
        if (_reg.get() == null) {
            try {
                if (_registering.compareAndSet(false, true)) {
                    ServiceRegistration<T> reg = ctx.registerService(clazz, service, _props.get());
                    if (_registering.compareAndSet(true, false) && _reg.compareAndSet(null, reg)) {
                        // If we registered and are dirty then we need to update the properties
                        if (_dirty.compareAndSet(true, false)) {
                            reg.setProperties(_props.get());
                        }
                    } else {
                        reg.unregister();
                    }
                }
            } catch (IllegalStateException ise) {
                // This occurs if the registration is unregistered and since we are trying to unregister
                // I think it is probably safe to ignore this.
            }
        }
    }

    @FFDCIgnore(IllegalStateException.class)
    public void unregister() {
        ServiceRegistration<T> reg = _reg.getAndSet(null);
        if (reg != null) {
            try {
                reg.unregister();
            } catch (IllegalStateException ise) {
                // This occurs if the registration is unregistered and since we are trying to unregister
                // I think it is probably safe to ignore this.
            }
        }
        _registering.set(false);
    }

    @FFDCIgnore(IllegalStateException.class)
    public boolean isRegistered() {
        ServiceRegistration<T> reg = _reg.get();

        if (reg != null) {
            try {
                reg.getReference();
                return true;
            } catch (IllegalStateException ise) {
                // if we get this it means the registration isn't valid anymore
                _reg.compareAndSet(reg, null);
            }
        }

        return false;
    }
}