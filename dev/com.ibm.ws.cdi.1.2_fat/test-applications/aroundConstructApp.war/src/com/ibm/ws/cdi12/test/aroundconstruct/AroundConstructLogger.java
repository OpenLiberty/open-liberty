/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.test.aroundconstruct;

import static com.ibm.ws.cdi12.test.aroundconstruct.AroundConstructLogger.ConstructorType.DEFAULT;
import static com.ibm.ws.cdi12.test.utils.Utils.id;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class AroundConstructLogger {

    public static enum ConstructorType {
        INJECTED, DEFAULT
    }

    private final List<String> constructorInterceptors = new ArrayList<String>();
    private final List<String> interceptedBeans = new ArrayList<String>();
    private ConstructorType beanConstructorUsed = DEFAULT;
    private Constructor<?> constructor = null;
    private Object target = null;

    public void addConstructorInterceptor(final Class<?> interceptor) {
        constructorInterceptors.add(id(interceptor));
    }

    public List<String> getConstructorInterceptors() {
        return constructorInterceptors;
    }

    public List<String> getInterceptedBeans() {
        return interceptedBeans;
    }

    public void setConstructorType(final ConstructorType type) {
        beanConstructorUsed = type;
    }

    public ConstructorType getConstructorType() {
        return beanConstructorUsed;
    }

    public void setConstructor(final Constructor<?> constructor) {
        this.constructor = constructor;
    }

    public Constructor<?> getConstructor() {
        return constructor;
    }

    public void setTarget(final Object target) {
        this.target = target;
    }

    public Object getTarget() {
        return target;
    }
}
