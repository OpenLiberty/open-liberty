package jakarta.xml.bind;

import jakarta.xml.bind.ServiceLoaderUtil.ExceptionHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

class ServiceLoaderUtil {

	private static final String OSGI_SERVICE_LOADER_CLASS_NAME = "org.glassfish.hk2.osgiresourcelocator.ServiceLoader";
	private static final String OSGI_SERVICE_LOADER_METHOD_NAME = "lookupProviderClasses";
	
	// Liberty Change
	
        static <P, T extends Exception> P firstByServiceLoader(Class<P> spiClass,
                                                               Logger logger,
                                                               ExceptionHandler<T> handler) throws T {
            // service discovery
            try {
                ServiceLoader<P> serviceLoader = ServiceLoader.load(spiClass);

                for (P impl : serviceLoader) {
                    logger.log(Level.FINE, "ServiceProvider loading Facility used; returning object [{0}]",
                               impl.getClass().getName());

                    return impl;
                }
            } catch (Throwable t) {

                //throw handler.createException(t, "Error while searching for service [" + spiClass.getName() + "]");
                ;
            }
            return null;
        }

        // End Liberty Change

	static Object lookupUsingOSGiServiceLoader(String factoryId, Logger logger) {
		try {
			Class serviceClass = Class.forName(factoryId);
			Class target = Class.forName("org.glassfish.hk2.osgiresourcelocator.ServiceLoader");
			Method m = target.getMethod("lookupProviderClasses", Class.class);
			Iterator iter = ((Iterable) m.invoke((Object) null, serviceClass)).iterator();
			if (iter.hasNext()) {
				Object next = iter.next();
				logger.fine("Found implementation using OSGi facility; returning object [" + next.getClass().getName()
						+ "].");
				return next;
			} else {
				return null;
			}
		} catch (InvocationTargetException | ClassNotFoundException | NoSuchMethodException
				| IllegalAccessException var7) {
			logger.log(Level.FINE, "Unable to find from OSGi: [" + factoryId + "]", var7);
			return null;
		}
	}

	static void checkPackageAccess(String className) {
		SecurityManager s = System.getSecurityManager();
		if (s != null) {
			int i = className.lastIndexOf(46);
			if (i != -1) {
				s.checkPackageAccess(className.substring(0, i));
			}
		}

	}

	static Class nullSafeLoadClass(String className, ClassLoader classLoader) throws ClassNotFoundException {
		return classLoader == null ? Class.forName(className) : classLoader.loadClass(className);
	}

	static <T extends Exception> Object newInstance(String className, String defaultImplClassName,
			ExceptionHandler<T> handler) throws T {
		try {
			return safeLoadClass(className, defaultImplClassName, contextClassLoader(handler)).newInstance();
		} catch (ClassNotFoundException var4) {
			throw handler.createException(var4, "Provider " + className + " not found");
		} catch (Exception var5) {
			throw handler.createException(var5, "Provider " + className + " could not be instantiated: " + var5);
		}
	}

	static Class safeLoadClass(String className, String defaultImplClassName, ClassLoader classLoader)
			throws ClassNotFoundException {
		try {
			checkPackageAccess(className);
		} catch (SecurityException var4) {
			if (defaultImplClassName != null && defaultImplClassName.equals(className)) {
				return Class.forName(className);
			}

			throw var4;
		}

		return nullSafeLoadClass(className, classLoader);
	}

	static ClassLoader contextClassLoader(ExceptionHandler exceptionHandler) throws Exception {
		try {
			return Thread.currentThread().getContextClassLoader();
		} catch (Exception var2) {
			throw exceptionHandler.createException(var2, var2.toString());
		}
	}
  static abstract class ExceptionHandler<T extends Exception> {

        public abstract T createException(Throwable throwable, String message);

    }

}
