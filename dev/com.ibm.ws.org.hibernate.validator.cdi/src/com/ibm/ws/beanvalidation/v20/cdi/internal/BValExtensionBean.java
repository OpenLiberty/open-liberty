package com.ibm.ws.beanvalidation.v20.cdi.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.util.AnnotationLiteral;
import javax.validation.ValidationException;

import com.ibm.ejs.util.dopriv.SetContextClassLoaderPrivileged;
import com.ibm.ws.beanvalidation.service.BeanValidationExtensionHelper;
import com.ibm.ws.util.ThreadContextAccessor;

/**
 * This is a Bean class created for org.apache.bval.cdi.BValExtension.
 * org.apache.bval.cdi.BValInterceptor injects BValExtension, and since BValExtension is not
 * registered as an extension service it cannot be injected unless defined as a Bean.
 */
public class BValExtensionBean implements Bean<LibertyHibernateValidatorExtension>, PassivationCapable {
    private final Set<Type> types;
    private final Set<Annotation> qualifiers;

    private static final PrivilegedAction<LibertyHibernateValidatorExtension> getBValExtensionAction = new PrivilegedAction<LibertyHibernateValidatorExtension>() {
        @Override
        public LibertyHibernateValidatorExtension run() {
            return new LibertyHibernateValidatorExtension();
        }
    };

    private static final PrivilegedAction<ThreadContextAccessor> getThreadContextAccessorAction = new PrivilegedAction<ThreadContextAccessor>() {
        @Override
        public ThreadContextAccessor run() {
            return ThreadContextAccessor.getThreadContextAccessor();
        }
    };

    public BValExtensionBean() {
        types = new HashSet<Type>();
        types.add(LibertyHibernateValidatorExtension.class);
        types.add(Object.class);

        qualifiers = new HashSet<Annotation>();
        qualifiers.add(new AnnotationLiteral<Default>() {});
        qualifiers.add(new AnnotationLiteral<Any>() {});
    }

    @Override
    public LibertyHibernateValidatorExtension create(CreationalContext<LibertyHibernateValidatorExtension> creationalContext) {
        SetContextClassLoaderPrivileged setClassLoader = null;
        ClassLoader oldClassLoader = null;
        ClassLoader classLoader = null;
        LibertyHibernateValidatorExtension bValExtension;

        try {
            ThreadContextAccessor tca = System.getSecurityManager() == null ? ThreadContextAccessor.getThreadContextAccessor() : AccessController.doPrivileged(getThreadContextAccessorAction);
            classLoader = tca.getContextClassLoader(Thread.currentThread());

            //Use customer classloader to handle multiple validation.xml being in the same ear.
            classLoader = BeanValidationExtensionHelper.newValidationClassLoader(classLoader);

            // set the thread context class loader to be used, must be reset in finally block
            setClassLoader = new SetContextClassLoaderPrivileged(tca);
            oldClassLoader = setClassLoader.execute(classLoader);

            //create a BValExtension with a ValidationClassLoader set.
            bValExtension = null;
            if (System.getSecurityManager() == null)
                bValExtension = new LibertyHibernateValidatorExtension();
            else
                bValExtension = AccessController.doPrivileged(getBValExtensionAction);
        } catch (ValidationException e) {
            bValExtension = null;
        } finally {
            if (setClassLoader != null) {
                setClassLoader.execute(oldClassLoader);
            }
            if (setClassLoader != null && setClassLoader.wasChanged) {
                // TODO ValidationExtensionService.instance().releaseLoader(classLoader);
            }
        }
        return bValExtension;
    }

    @Override
    public void destroy(LibertyHibernateValidatorExtension instance, CreationalContext<LibertyHibernateValidatorExtension> creationalContext) {

    }

    @Override
    public Set<Type> getTypes() {
        return types;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return ApplicationScoped.class;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public String getId() {
        return "BValExtension: " + hashCode();
    }

    @Override
    public Class<?> getBeanClass() {
        return LibertyHibernateValidatorExtension.class;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public boolean isNullable() {
        return false;
    }

}
