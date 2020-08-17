/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.ejbcontainer.jakarta.test.tools;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.RemoteException;

import javax.naming.InitialContext;
import javax.rmi.CORBA.Stub;

import jakarta.ejb.EJBHome;
import jakarta.ejb.EJBLocalHome;
import jakarta.ejb.EJBLocalObject;
import jakarta.ejb.EJBMetaData;
import jakarta.ejb.EJBObject;

public class FATEJBHelper {
    private static String toSystemString(Object o) {
        return o.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(o));
    }

    /**
     * Creates an EJBHome proxy given an EJBLocalHome. The returned object
     * will delegate all calls through the specified local home interface class
     * to the specified object via the corresponding method on the object's
     * remote home interface. All methods called on the resulting object must
     * have identical method parameters. Methods declared on the
     * <tt>EJBLocalObject</tt> interface are not reliable.
     *
     * @param remoteHome the enterprise bean home stub
     * @param localObjectClass the local home interface class to proxy
     * @return a home implementation
     */
    public static <R extends EJBHome, L extends EJBLocalHome> L createEJBLocalHomeProxy(final R remoteHome, Class<L> localHomeClass) {
        try {
            @SuppressWarnings("unchecked")
            Class<EJBHome> remoteIntf = remoteHome.getEJBMetaData().getHomeInterfaceClass();
            return createProxy(remoteHome, remoteIntf, localHomeClass, true, null);
        } catch (RemoteException ex) {
            throw new Error(ex);
        }
    }

    /**
     * Creates an EJBObject proxy given an EJBLocalObject. The returned object
     * will delegate all calls through the specified local interface class to
     * the specified object via the corresponding method on the object's remote
     * interface. All methods called on the resulting object must have
     * identical method parameters. Methods declared on the
     * <tt>EJBLocalObject</tt> interface are not reliable.
     *
     * @param remoteObject the enterprise bean stub
     * @param localObjectClass the local interface class to proxy
     * @return a home implementation
     */
    public static <R extends EJBObject, L extends EJBLocalObject> L createEJBLocalObjectProxy(final R remoteObject, Class<L> localObjectClass) {
        try {
            EJBHome home = remoteObject.getEJBHome();
            @SuppressWarnings("unchecked")
            Class<EJBObject> remoteIntf = home.getEJBMetaData().getRemoteInterfaceClass();
            return createProxy(remoteObject, remoteIntf, localObjectClass, false, home);
        } catch (RemoteException ex) {
            throw new Error(ex);
        }
    }

    private static <R, L> L createProxy(final R remoteObject, final Class<R> remoteClass, final Class<L> localClass, final boolean isHome, final EJBHome home) {
        return localClass.cast(Proxy.newProxyInstance(remoteClass.getClassLoader(),
                                                      new Class[] { localClass },
                                                      new EJBLocalProxyInvocationHandler(remoteObject, remoteClass, localClass, isHome, home)));
    }

    private static class EJBLocalProxyInvocationHandler implements InvocationHandler {
        private final Object ivRemoteObject;
        private final Class<?> ivRemoteClass;
        private final Class<?> ivLocalClass;
        private final boolean ivIsHome;
        private final EJBHome ivHome;

        EJBLocalProxyInvocationHandler(Object remoteObject,
                                       Class<?> remoteClass,
                                       Class<?> localClass,
                                       boolean isHome,
                                       EJBHome home) {
            ivRemoteObject = remoteObject;
            ivRemoteClass = remoteClass;
            ivLocalClass = localClass;
            ivIsHome = isHome;
            ivHome = home;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                String methodName = method.getName();

                if (methodName.equals("toString") && args == null) {
                    Object remoteObjectString = ivRemoteObject instanceof Stub ? toSystemString(ivRemoteObject) : ivRemoteObject;
                    return this + "[" + toSystemString(proxy) + ", class=" + ivLocalClass.getName() + ", object=" + remoteObjectString + ']';
                }

                if (ivHome != null && methodName.equals("getEJBHome") && args == null) {
                    return ivHome;
                }

                Object result = ivRemoteClass.getMethod(methodName, method.getParameterTypes()).invoke(ivRemoteObject, args);

                if (ivIsHome && methodName.startsWith("create")) {
                    @SuppressWarnings("unchecked")
                    Class<? extends EJBLocalObject> type = (Class<? extends EJBLocalObject>) method.getReturnType();
                    return createEJBLocalObjectProxy((EJBObject) result, type);
                }

                return result;
            } catch (InvocationTargetException ex) {
                throw ex.getCause();
            }
        }
    }

    /**
     * Creates an EJBLocalHome proxy given a home class and a lookup string for
     * the binding of an Enterprise Bean 3.0 business interface. The returned object
     * implements the create method to lookup the specified string. Methods
     * declared on the EJBLocalObject interface have undefined behavior when
     * called on objects returned from the create method.
     *
     * @param lookup the lookup string
     * @param remoteHomeClass the home interface
     * @return a home implementation
     */
    public static <L extends EJBLocalHome> L createEJBLocalHomeProxy(final String lookup, Class<L> localHomeClass) {
        return localHomeClass.cast(createHomeProxy(localHomeClass, lookup, null));
    }

    /**
     * Creates an EJBHome proxy given a home class and a lookup string for the
     * binding of an Enterprise Bean 3.0 business interface. The returned object implements
     * the create method to lookup the specified string. Methods declared on
     * the EJBObject interface have undefined behavior when called on objects
     * returned from the create method.
     *
     * @param lookup the lookup string
     * @param remoteHomeClass the home interface
     * @return a home implementation
     */
    public static <R extends EJBHome> R createEJBHomeProxy(final String lookup, Class<R> remoteHomeClass, Class<?> remoteClass) {
        return remoteHomeClass.cast(createHomeProxy(remoteHomeClass, lookup, remoteClass));
    }

    private static Object createHomeProxy(final Class<?> homeClass, final String lookup, final Class<?> remoteClass) {
        return Proxy.newProxyInstance(homeClass.getClassLoader(),
                                      new Class[] { homeClass },
                                      new HomeProxyInvocationHandlerImpl(homeClass, lookup, remoteClass));
    }

    private static class HomeProxyInvocationHandlerImpl implements InvocationHandler {
        private final Class<?> ivHomeClass;
        private final String ivLookup;
        private final Class<?> ivRemoteClass;

        HomeProxyInvocationHandlerImpl(Class<?> homeClass, String lookup, Class<?> remoteClass) {
            ivHomeClass = homeClass;
            ivLookup = lookup;
            ivRemoteClass = remoteClass;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            if (methodName.equals("toString") && args == null) {
                return this + "[" + toSystemString(proxy) + ", class=" + ivHomeClass.getName() + ", lookup=" + ivLookup + ']';
            }

            if (ivRemoteClass != null && methodName.equals("getEJBMetaData") && args == null) {
                return new EJBMetaDataImpl();
            }

            // Only look at no-param create methods.  There is no way to
            // pass init parameters to an Enterprise Bean 3.0 style SFSB.
            if (methodName.equals("create") && args == null) {
                // Create an instance of the object via naming.
                Object result = new InitialContext().lookup(ivLookup);
                EJBHome home = ivRemoteClass == null ? null : (EJBHome) proxy;

                // The home class will require an EJBLocal/Object, which
                // the business interface will not implement.
                @SuppressWarnings({ "unchecked", "rawtypes" })
                Object uncheckedResult = createProxy(result, (Class) result.getClass(), method.getReturnType(), false, home);

                return uncheckedResult;
            }

            throw new UnsupportedOperationException(method.toString());
        }

        class EJBMetaDataImpl implements EJBMetaData {
            @Override
            public String toString() {
                return super.toString() + '[' + ivHomeClass.getName() + ']';
            }

            @Override
            public EJBHome getEJBHome() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Class<?> getHomeInterfaceClass() {
                return ivHomeClass;
            }

            @Override
            public Class<?> getRemoteInterfaceClass() {
                for (Method method : ivHomeClass.getMethods()) {
                    if (method.getName().startsWith("create")) {
                        return method.getReturnType();
                    }
                }

                throw new UnsupportedOperationException();
            }

            @Override
            public Class<?> getPrimaryKeyClass() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isSession() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isStatelessSession() {
                throw new UnsupportedOperationException();
            }
        }
    }
}
