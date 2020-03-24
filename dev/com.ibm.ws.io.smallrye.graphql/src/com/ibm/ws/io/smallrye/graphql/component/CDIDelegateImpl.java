package com.ibm.ws.io.smallrye.graphql.component;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.context.spi.CreationalContext;

import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.smallrye.graphql.cdi.CDIDelegate;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

//@Component(name = "com.ibm.ws.io.smallrye.graphql.component.CDIDelegateImpl", immediate = true, property = { "service.vendor=IBM" })
public class CDIDelegateImpl implements CDIDelegate {
    private static Logger LOG = Logger.getLogger(CDIDelegateImpl.class.getName());

    @Override
    public Class<?> getClassFromCDI(Class<?> declaringClass) {
        return getInstanceFromCDI(declaringClass).getClass();
    }

    @Override
    public Object getInstanceFromCDI(Class<?> declaringClass) {
        BeanManager manager = GraphQLExtension.getBeanManager();
        Bean bean = getBeanFromCDI(declaringClass);
        CreationalContext creationalContext = manager.createCreationalContext(bean);
        Object obj = null;
        if (bean != null && manager != null) {
            obj = manager.getReference(bean, declaringClass, creationalContext);
        }
        
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("instance, " + obj + " returned from class, " + declaringClass);
        }
        return obj;
    }

    private Bean<?> getBeanFromCDI(Class<?> clazz) {

        Set<Bean<?>> beans = GraphQLExtension.graphQLApiBeans.get(GraphQLExtension.getContextClassLoader());
        for (Bean<?> bean : beans) {
            if (clazz.equals(bean.getBeanClass())) {
                return bean;
            }
        }
        return null;
    }
}
