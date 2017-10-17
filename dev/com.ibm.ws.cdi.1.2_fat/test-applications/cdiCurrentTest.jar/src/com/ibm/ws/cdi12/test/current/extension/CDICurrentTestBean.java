/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2016
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.test.current.extension;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.PassivationCapable;

public class CDICurrentTestBean implements Bean<CDICurrent>, PassivationCapable {

    private final HashSet<Type> types;
    private final HashSet<Annotation> qualifiers;

    public CDICurrentTestBean() {
        types = new HashSet<Type>();
        types.add(CDICurrent.class);
        types.add(Object.class);

        qualifiers = new HashSet<Annotation>();
        qualifiers.add(DefaultLiteral.INSTANCE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.context.spi.Contextual#create(javax.enterprise.context.spi.CreationalContext)
     */
    @Override
    public CDICurrent create(CreationalContext creationalContext) {
        CDICurrent bean1 = new CDICurrent();
        return bean1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.context.spi.Contextual#destroy(java.lang.Object, javax.enterprise.context.spi.CreationalContext)
     */
    @Override
    public void destroy(CDICurrent instance, CreationalContext creationalContext) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.inject.spi.BeanAttributes#getTypes()
     */
    @Override
    public Set getTypes() {
        // TODO Auto-generated method stub
        return types;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.inject.spi.BeanAttributes#getQualifiers()
     */
    @Override
    public Set getQualifiers() {
        // TODO Auto-generated method stub
        return qualifiers;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.inject.spi.BeanAttributes#getScope()
     */
    @Override
    public Class getScope() {
        return SessionScoped.class;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.inject.spi.BeanAttributes#getName()
     */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.inject.spi.BeanAttributes#getStereotypes()
     */
    @Override
    public Set getStereotypes() {
        // TODO Auto-generated method stub
        return Collections.emptySet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.inject.spi.BeanAttributes#isAlternative()
     */
    @Override
    public boolean isAlternative() {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.inject.spi.Bean#getBeanClass()
     */
    @Override
    public Class getBeanClass() {
        // TODO Auto-generated method stub
        return CDICurrent.class;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.inject.spi.Bean#getInjectionPoints()
     */
    @Override
    public Set getInjectionPoints() {
        // TODO Auto-generated method stub
        return Collections.emptySet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.inject.spi.Bean#isNullable()
     */
    @Override
    public boolean isNullable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getId() {
        return "CDICurrent1: " + hashCode();
    }
}
