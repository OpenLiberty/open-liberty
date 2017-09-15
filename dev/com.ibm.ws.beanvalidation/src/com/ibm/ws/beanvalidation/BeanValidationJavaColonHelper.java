/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.naming.InvalidNameException;
import javax.naming.NameClassPair;
import javax.naming.NamingException;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.osgi.service.component.annotations.Component;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.container.service.naming.JavaColonNamingHelper;
import com.ibm.ws.container.service.naming.NamingConstants.JavaColonNamespace;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 * This {@link JavaColonNamingHelper} implementation provides support for
 * JavaBean Validation in the standard Java EE component naming context
 * (java:comp/env). <p>
 * 
 * It is registered on the JNDI NamingHelper whiteboard and will be
 * consulted during object lookup in the appropriate namespace. <p>
 */
@Component(service = JavaColonNamingHelper.class)
@Trivial
public class BeanValidationJavaColonHelper implements JavaColonNamingHelper {
    private static final TraceComponent tc = Tr.register(BeanValidationJavaColonHelper.class);

    private static final String VALIDATOR_FACTORY = "ValidatorFactory";
    private static final String VALIDATOR = "Validator";

    /** {@inheritDoc} */
    @Override
    public Object getObjectInstance(JavaColonNamespace namespace, String name) throws NamingException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getObjectInstance (" + namespace + ", " + name + ")");

        // This helper only provides support for java:comp/env
        if (namespace != JavaColonNamespace.COMP) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getObjectInstance : null (not COMP)");
            return null;
        }

        boolean isFactory = false;
        ValidatorFactory vfactory = null;

        if (VALIDATOR_FACTORY.equals(name)) {
            isFactory = true;
        } else if (!VALIDATOR.equals(name)) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getObjectInstance : null (not ValidatorFactory or Validator)");
            return null;
        }

        // Retrieve the ValidatorFactory for the currently active module.
        // If there is no metadata on the thread, then a nice exception
        // will be thrown, which should be wrapped in a NamingException.
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        try {
            vfactory = AbstractBeanValidation.instance().getValidatorFactory(cmd);
        } catch (ValidationException vex) {
            NamingException nex = new NamingException("Failed to obtain object of type " + name);
            nex.initCause(vex);
            throw nex;
        }

        Object instance = isFactory ? vfactory : vfactory.getValidator();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getObjectInstance : " + Util.identity(instance));

        return instance;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws NameException
     */
    @Override
    public boolean hasObjectWithPrefix(JavaColonNamespace namespace, String name) throws NamingException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "hasObjectWithPrefix (" + namespace + ", " + name + ")");

        if (name == null) {
            throw new InvalidNameException();
        }

        boolean result = false;
        // This helper only provides support for java:comp/env
        if (namespace == JavaColonNamespace.COMP && name.isEmpty()) {
            result = true;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "hasObjectWithPrefix", result);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<? extends NameClassPair> listInstances(JavaColonNamespace namespace, String nameInContext) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "listInstances (" + namespace + ", " + nameInContext + ")");

        // This helper only provides support for java:comp/env
        if ((namespace == JavaColonNamespace.COMP) && ("".equals(nameInContext))) {
            ArrayList<NameClassPair> retVal = new ArrayList<NameClassPair>();
            retVal.add(new NameClassPair(VALIDATOR_FACTORY, ValidatorFactory.class.getName()));
            retVal.add(new NameClassPair(VALIDATOR, Validator.class.getName()));
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "listInstances", retVal);
            return retVal;
        } else {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "listInstances", "empty (not COMP)");
            return Collections.emptyList();
        }

    }
}
