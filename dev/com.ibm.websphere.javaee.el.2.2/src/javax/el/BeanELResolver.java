/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.el;

//PM21638      10/04/10    pmdinh      Mem Leak when start and stop application repeatedly
//PM43978      12/22/11    pnicoluc    WHEN USING JSF 2.0 CUSTOM COMPONENT DATE VALUES ARE NOT CONVERTED CORRECTLY
//PM55338      03/19/2012  sartoris    Read only property for custom tag is throwing a PropertyNotFoundException instead of PropertyNotWriteableException
//PI69870      09/28/16    hmpadill    Calling a method in an interface when the implementing class is not public results in IllegalAccessException

import java.beans.BeanInfo;
import java.beans.FeatureDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class BeanELResolver extends ELResolver {

    private static final int CACHE_SIZE;
    private static final String CACHE_SIZE_PROP =
                    "org.apache.el.BeanELResolver.CACHE_SIZE";

    static {
        if (System.getSecurityManager() == null) {
            CACHE_SIZE = Integer.parseInt(
                            System.getProperty(CACHE_SIZE_PROP, "1000"));
        } else {
            CACHE_SIZE = AccessController.doPrivileged(
                                                       new PrivilegedAction<Integer>() {

                                                           @Override
                                                           public Integer run() {
                                                               return Integer.valueOf(
                                                                               System.getProperty(CACHE_SIZE_PROP, "1000"));
                                                           }
                                                       }).intValue();
        }
    }

    private final boolean readOnly;

    private final ConcurrentCache<String, BeanProperties> cache =
                    new ConcurrentCache<String, BeanProperties>(CACHE_SIZE);

    public BeanELResolver() {
        this.readOnly = false;
    }

    public BeanELResolver(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    public Object getValue(ELContext context, Object base, Object property)
                    throws NullPointerException, PropertyNotFoundException, ELException {
        if (context == null) {
            throw new NullPointerException();
        }
        if (base == null || property == null) {
            return null;
        }

        context.setPropertyResolved(true);
        Method m = this.property(context, base, property).read(context);
        try {
            return m.invoke(base, (Object[]) null);
        } catch (IllegalAccessException e) {
            throw new ELException(e);
        } catch (InvocationTargetException e) {
            throw new ELException(message(context, "propertyReadError",
                                          new Object[] { base.getClass().getName(),
                                                        property.toString() }), e.getCause());
        } catch (Exception e) {
            throw new ELException(e);
        }
    }

    @Override
    public Class<?> getType(ELContext context, Object base, Object property)
                    throws NullPointerException, PropertyNotFoundException, ELException {
        if (context == null) {
            throw new NullPointerException();
        }
        if (base == null || property == null) {
            return null;
        }

        context.setPropertyResolved(true);
        return this.property(context, base, property).getPropertyType();
    }

    @Override
    public void setValue(ELContext context, Object base, Object property,
                         Object value) throws NullPointerException,
                    PropertyNotFoundException, PropertyNotWritableException,
                    ELException {
        if (context == null) {
            throw new NullPointerException();
        }
        if (base == null || property == null) {
            return;
        }

        context.setPropertyResolved(true);

        if (this.readOnly) {
            throw new PropertyNotWritableException(message(context,
                                                           "resolverNotWriteable", new Object[] { base.getClass()
                                                                           .getName() }));
            //PM55338 start
        } else if (this.property(context, base, property).isReadOnly()) {
            throw new PropertyNotWritableException(message(context,
                                                           "propertyNotWritable", new Object[] { base.getClass().getName(), property }));
        }
        //PM55338 end

        Method m = this.property(context, base, property).write(context);
        try {
            m.invoke(base, value);
        } catch (IllegalAccessException e) {
            throw new ELException(e);
        } catch (InvocationTargetException e) {
            throw new ELException(message(context, "propertyWriteError",
                                          new Object[] { base.getClass().getName(),
                                                        property.toString() }), e.getCause());
        } catch (Exception e) {
            throw new ELException(e);
        }
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property)
                    throws NullPointerException, PropertyNotFoundException, ELException {
        if (context == null) {
            throw new NullPointerException();
        }
        if (base == null || property == null) {
            return false;
        }

        context.setPropertyResolved(true);
        return this.readOnly
                || this.property(context, base, property).isReadOnly();
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
        if (context == null) {
            throw new NullPointerException();
        }

        if (base == null) {
            return null;
        }

        try {
            BeanInfo info = Introspector.getBeanInfo(base.getClass());
            PropertyDescriptor[] pds = info.getPropertyDescriptors();
            for (int i = 0; i < pds.length; i++) {
                pds[i].setValue(RESOLVABLE_AT_DESIGN_TIME, Boolean.TRUE);
                pds[i].setValue(TYPE, pds[i].getPropertyType());
            }
            return Arrays.asList((FeatureDescriptor[]) pds).iterator();
        } catch (IntrospectionException e) {
            //
        }

        return null;
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        if (context == null) {
            throw new NullPointerException();
        }

        if (base != null) {
            return Object.class;
        }

        return null;
    }

    protected final static class BeanProperties {
        private final Map<String, BeanProperty> properties;

        private final Class<?> type;

        public BeanProperties(Class<?> type) throws ELException {
            this.type = type;
            this.properties = new HashMap<String, BeanProperty>();
            try {
                BeanInfo info = Introspector.getBeanInfo(this.type);
                PropertyDescriptor[] pds = info.getPropertyDescriptors();
                for (PropertyDescriptor pd: pds) {
                    this.properties.put(pd.getName(), new BeanProperty(type, pd));
                }
                if (System.getSecurityManager() != null) {
                    // When running with SecurityManager, some classes may be
                    // not accessible, but have accessible interfaces.
                    populateFromInterfaces(type);
                }
            } catch (IntrospectionException ie) {
                throw new ELException(ie);
            }
        }


        private void populateFromInterfaces(Class<?> aClass) throws IntrospectionException {
            Class<?> interfaces[] = aClass.getInterfaces();
            if (interfaces.length > 0) {
                for (Class<?> ifs : interfaces) {
                    BeanInfo info = Introspector.getBeanInfo(ifs);
                    PropertyDescriptor[] pds = info.getPropertyDescriptors();
                    for (PropertyDescriptor pd : pds) {
                        if (!this.properties.containsKey(pd.getName())) {
                            this.properties.put(pd.getName(), new BeanProperty(
                                    this.type, pd));
                        }
                    }
                }
            }
            Class<?> superclass = aClass.getSuperclass();
            if (superclass != null) {
                populateFromInterfaces(superclass);
            }
        }


        private BeanProperty get(ELContext ctx, String name) {
            BeanProperty property = this.properties.get(name);
            if (property == null) {
                throw new PropertyNotFoundException(message(ctx,
                                                            "propertyNotFound",
                                                            new Object[] { type.getName(), name }));
            }
            return property;
        }

        public BeanProperty getBeanProperty(String name) {
            return get(null, name);
        }

        private Class<?> getType() {
            return type;
        }
    }

    protected final static class BeanProperty {
        private final Class<?> type;

        private final Class<?> owner;

        private final PropertyDescriptor descriptor;

        private Method read;

        private Method write;

        public BeanProperty(Class<?> owner, PropertyDescriptor descriptor) {
            this.owner = owner;
            this.descriptor = descriptor;
            this.type = descriptor.getPropertyType();
        }

        // Can't use Class<?> because API needs to match specification
        public @SuppressWarnings("rawtypes")
        Class getPropertyType() {
            return this.type;
        }

        public boolean isReadOnly() {
            return this.write == null
                   && (null == (this.write = getMethod(this.owner, descriptor.getWriteMethod())));
        }

        public Method getWriteMethod() {
            return write(null);
        }

        public Method getReadMethod() {
            return this.read(null);
        }

        private Method write(ELContext ctx) {
            if (this.write == null) {
                this.write = getMethod(this.owner, descriptor.getWriteMethod());
                if (this.write == null) {
                    throw new PropertyNotFoundException(message(ctx,
                                                                "propertyNotWritable", new Object[] {
                                                                                                     type.getName(), descriptor.getName() }));
                }
            }
            return this.write;
        }

        private Method read(ELContext ctx) {
            if (this.read == null) {
                this.read = getMethod(this.owner, descriptor.getReadMethod());
                if (this.read == null) {
                    throw new PropertyNotFoundException(message(ctx,
                                                                "propertyNotReadable", new Object[] {
                                                                                                     type.getName(), descriptor.getName() }));
                }
            }
            return this.read;
        }
    }

    private final BeanProperty property(ELContext ctx, Object base,
                                        Object property) {
        Class<?> type = base.getClass();
        String prop = property.toString();

        BeanProperties props = this.cache.get(type.getName());
        if (props == null || type != props.getType()) {
            props = new BeanProperties(type);
            this.cache.put(new String(type.getName()), props);//PM21638
        }

        return props.get(ctx, prop);
    }

    private final static Method getMethod(Class<?> type, Method m) {
        if (m == null || Modifier.isPublic(type.getModifiers())) {
            return m;
        }
        Class<?>[] inf = type.getInterfaces();
        Method mp = null;
        for (int i = 0; i < inf.length; i++) {
            try {
                mp = inf[i].getMethod(m.getName(), m.getParameterTypes());
                mp = getMethod(mp.getDeclaringClass(), mp);
                if (mp != null) {
                    return mp;
                }
            } catch (NoSuchMethodException e) {
                // Ignore
            }
        }
        Class<?> sup = type.getSuperclass();
        if (sup != null) {
            try {
                mp = sup.getMethod(m.getName(), m.getParameterTypes());
                mp = getMethod(mp.getDeclaringClass(), mp);
                if (mp != null) {
                    return mp;
                }
            } catch (NoSuchMethodException e) {
                // Ignore
            }
        }
        return null;
    }

    private final static class ConcurrentCache<K, V> {

        private final int size;
        private final Map<K, V> eden;
        private final Map<K, V> longterm;

        public ConcurrentCache(int size) {
            this.size = size;
            this.eden = new ConcurrentHashMap<K, V>(size);
            this.longterm = new WeakHashMap<K, V>(size);
        }

        public V get(K key) {
            V value = this.eden.get(key);
            if (value == null) {
                synchronized (longterm) {
                    value = this.longterm.get(key);
                }
                if (value != null) {
                    this.eden.put(key, value);
                }
            }
            return value;
        }

        public void put(K key, V value) {
            if (this.eden.size() >= this.size) {
                synchronized (longterm) {
                    this.longterm.putAll(this.eden);
                }
                this.eden.clear();
            }
            this.eden.put(key, value);
        }

    }

    /**
     * @since EL 2.2
     */
    @Override
    public Object invoke(ELContext context, Object base, Object method,
                         Class<?>[] paramTypes, Object[] params) {
        if (context == null) {
            throw new NullPointerException();
        }
        if (base == null || method == null) {
            return null;
        }

        ExpressionFactory factory = ExpressionFactory.newInstance();

        String methodName = (String) factory.coerceToType(method, String.class);

        // Find the matching method
        Method matchingMethod = null;
        Class<?> clazz = base.getClass();
        if (paramTypes != null) {
            try {
                matchingMethod = clazz.getMethod(methodName, paramTypes);
            } catch (NoSuchMethodException e) {
                throw new MethodNotFoundException(e);
            }
        } else {
            int paramCount = 0;
            if (params != null) {
                paramCount = params.length;
            }
            Method[] methods = clazz.getMethods();
            for (Method m : methods) {
                //PM43978 - start
                if (methodName.equals(m.getName()) &&
                        m.getParameterTypes().length == paramCount) { //PM43978 - end
                    // Same number of parameters - use the first match
                    matchingMethod = m;
                    break;
                }
                if (m.isVarArgs()) {
                    matchingMethod = m;
                }
            }
            if (matchingMethod == null) {
                throw new MethodNotFoundException(
                                "Unable to find method [" + methodName + "] with ["
                                                + paramCount + "] parameters");
            }
        }

        Class<?>[] parameterTypes = matchingMethod.getParameterTypes();
        Object[] parameters = null;
        if (parameterTypes.length > 0) {
            parameters = new Object[parameterTypes.length];
            @SuppressWarnings("null")
            // params.length >= parameterTypes.length
            int paramCount = params.length;
            if (matchingMethod.isVarArgs()) {
                int varArgIndex = parameterTypes.length - 1;
                // First argCount-1 parameters are standard
                for (int i = 0; (i < varArgIndex - 1); i++) {
                    parameters[i] = factory.coerceToType(params[i],
                                                         parameterTypes[i]);
                }
                // Last parameter is the varags
                Class<?> varArgClass =
                                parameterTypes[varArgIndex].getComponentType();
                for (int i = (varArgIndex); i < paramCount; i++) {
                    Object varargs = Array.newInstance(
                                                       parameterTypes[paramCount],
                                                       (paramCount - varArgIndex));
                    Array.set(varargs, i,
                              factory.coerceToType(params[i], varArgClass));
                    parameters[varArgIndex] = varargs;
                }
            } else {
                parameters = new Object[parameterTypes.length];
                for (int i = 0; i < parameterTypes.length; i++) {
                    parameters[i] = factory.coerceToType(params[i],
                                                         parameterTypes[i]);
                }
            }
        }
        matchingMethod = getMethod(clazz, matchingMethod); //PI69870
        Object result = null;
        try {
            result = matchingMethod.invoke(base, parameters);
        } catch (IllegalArgumentException e) {
            throw new ELException(e);
        } catch (IllegalAccessException e) {
            throw new ELException(e);
        } catch (InvocationTargetException e) {
            throw new ELException(e.getCause());
        }

        context.setPropertyResolved(true);
        return result;
    }

}
