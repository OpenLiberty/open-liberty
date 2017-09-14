/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.myfaces.component.validate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;
import javax.faces.validator.BeanValidator;
import static javax.faces.validator.BeanValidator.EMPTY_VALIDATION_GROUPS_PATTERN;
import static javax.faces.validator.BeanValidator.MESSAGE_ID;
import static javax.faces.validator.BeanValidator.VALIDATION_GROUPS_DELIMITER;
import static javax.faces.validator.BeanValidator.VALIDATOR_FACTORY_KEY;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import javax.validation.ConstraintViolation;
import javax.validation.MessageInterpolator;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.validation.groups.Default;
import javax.validation.metadata.BeanDescriptor;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;
import org.apache.myfaces.shared.util.MessageUtils;
import org.apache.myfaces.shared.util.MyFacesObjectInputStream;
import org.apache.myfaces.util.ExternalSpecifications;

/**
 *
 */
public class WholeBeanValidator implements Validator
{
    private static final Logger log = Logger.getLogger(WholeBeanValidator.class.getName());
    
    private static final Class<?>[] DEFAULT_VALIDATION_GROUPS_ARRAY = new Class<?>[] { Default.class };

    private static final String DEFAULT_VALIDATION_GROUP_NAME = "javax.validation.groups.Default";
    
    private static final String CANDIDATE_COMPONENT_VALUES_MAP = "oam.WBV.candidatesMap";
    
    private static final String BEAN_VALIDATION_FAILED = "oam.WBV.validationFailed";

    private Class<?>[] validationGroupsArray;

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }
        if (component == null)
        {
            throw new NullPointerException("component");
        }

        ValueExpression valueExpression = component.getValueExpression("value");
        if (valueExpression == null)
        {
            log.warning("cannot validate component with empty value: " 
                    + component.getClientId(context));
            return;
        }

        // Obtain a reference to the to-be-validated object and the property name.
        /*
        _ValueReferenceWrapper reference = getValueReference(valueExpression, context);
        if (reference == null)
        {
            return;
        }
        Object base = reference.getBase();
        if (base == null)
        {
            return;
        }*/
        Object base = valueExpression.getValue(context.getELContext());
                
        Class<?> valueBaseClass = base.getClass();
        if (valueBaseClass == null)
        {
            return;
        }
        /*
        Object referenceProperty = reference.getProperty();
        if (!(referenceProperty instanceof String))
        {
            // if the property is not a String, the ValueReference does not
            // point to a bean method, but e.g. to a value in a Map, thus we 
            // can exit bean validation here
            return;
        }
        String valueProperty = (String) referenceProperty;
        */

        // Initialize Bean Validation.
        ValidatorFactory validatorFactory = createValidatorFactory(context);
        javax.validation.Validator validator = createValidator(validatorFactory, context, 
                (ValidateWholeBeanComponent)component);
        BeanDescriptor beanDescriptor = validator.getConstraintsForClass(valueBaseClass);
        if (!beanDescriptor.isBeanConstrained())
        {
            return;
        }
        
        // Note that validationGroupsArray was initialized when createValidator was called
        Class[] validationGroupsArray = this.validationGroupsArray;

        // Delegate to Bean Validation.
        
        // TODO: Use validator.validate(...) over the copy instance.
        
        Boolean beanValidationFailed = (Boolean) context.getViewRoot().getTransientStateHelper()
                .getTransient(BEAN_VALIDATION_FAILED);
        
        if (Boolean.TRUE.equals(beanValidationFailed))
        {
            // JSF 2.3 Skip class level validation
            return;
        }
        
        Map<String, Object> candidatesMap = (Map<String, Object>) context.getViewRoot()
                .getTransientStateHelper().getTransient(CANDIDATE_COMPONENT_VALUES_MAP);
        if (candidatesMap != null)
        {
            Object copy = createBeanCopy(base);
            
            UpdateBeanCopyCallback callback = new UpdateBeanCopyCallback(this, base, copy, candidatesMap);
            context.getViewRoot().visitTree(
                    VisitContext.createVisitContext(context, candidatesMap.keySet(), null), 
                    callback);
            
            Set constraintViolations = validator.validate(copy, validationGroupsArray);
            if (!constraintViolations.isEmpty())
            {
                Set<FacesMessage> messages = new LinkedHashSet<FacesMessage>(constraintViolations.size());
                for (Object violation: constraintViolations)
                {
                    ConstraintViolation constraintViolation = (ConstraintViolation) violation;
                    String message = constraintViolation.getMessage();
                    Object[] args = new Object[]{ message, MessageUtils.getLabel(context, component) };
                    FacesMessage msg = MessageUtils.getMessage(FacesMessage.SEVERITY_ERROR, MESSAGE_ID, args, context);
                    messages.add(msg);
                }
                throw new ValidatorException(messages);
            }
        }
    }
    
    private Object createBeanCopy(Object base)
    {
        Object copy = null;
        try
        {
            copy = base.getClass().newInstance();
        }
        catch (InstantiationException ex)
        {
            Logger.getLogger(WholeBeanValidator.class.getName()).log(Level.FINEST, null, ex);
        }
        catch (IllegalAccessException ex)
        {
            Logger.getLogger(WholeBeanValidator.class.getName()).log(Level.FINEST, null, ex);
        }
        if (base instanceof Serializable)
        {
            copy = copySerializableObject(base);
        }
        else if(base instanceof Cloneable)
        { 
            Method cloneMethod;
            try
            {
                cloneMethod = base.getClass().getMethod("clone");
                copy = cloneMethod.invoke(base);
            }
            catch (NoSuchMethodException ex) 
            {
                Logger.getLogger(WholeBeanValidator.class.getName()).log(Level.FINEST, null, ex);
            }
            catch (SecurityException ex) 
            {
                Logger.getLogger(WholeBeanValidator.class.getName()).log(Level.FINEST, null, ex);
            }
            catch (IllegalAccessException ex)
            {
                Logger.getLogger(WholeBeanValidator.class.getName()).log(Level.FINEST, null, ex);
            }
            catch (IllegalArgumentException ex)
            {
                Logger.getLogger(WholeBeanValidator.class.getName()).log(Level.FINEST, null, ex);
            }
            catch (InvocationTargetException ex)
            {
                Logger.getLogger(WholeBeanValidator.class.getName()).log(Level.FINEST, null, ex);
            }
        }
        else
        {
            Class<?> clazz = base.getClass();
            try
            {
                Constructor<?> copyConstructor = clazz.getConstructor(clazz);
                if (copyConstructor != null)
                {
                    copy = copyConstructor.newInstance(base);
                }
            }
            catch (NoSuchMethodException ex)
            {
                Logger.getLogger(WholeBeanValidator.class.getName()).log(Level.FINEST, null, ex);
            }
            catch (SecurityException ex)
            {
                Logger.getLogger(WholeBeanValidator.class.getName()).log(Level.FINEST, null, ex);
            }
            catch (IllegalAccessException ex)
            {
                Logger.getLogger(WholeBeanValidator.class.getName()).log(Level.FINEST, null, ex);
            }
            catch (IllegalArgumentException ex)
            {
                Logger.getLogger(WholeBeanValidator.class.getName()).log(Level.FINEST, null, ex);
            }
            catch (InvocationTargetException ex)
            {
                Logger.getLogger(WholeBeanValidator.class.getName()).log(Level.FINEST, null, ex);
            }
            catch (InstantiationException ex)
            {
                Logger.getLogger(WholeBeanValidator.class.getName()).log(Level.FINEST, null, ex);
            }
        }
        if (copy == null)
        {
            throw new FacesException("Cannot create copy for wholeBeanValidator: "+base.getClass().getName());
        }
        return copy;
    }
    
    private Object copySerializableObject(Object base)
    {
        Object copy = null;
        try 
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(base);
            oos.flush();
            oos.close();
            baos.close();
            byte[] byteData = baos.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(byteData);
            try 
            {
                copy = new MyFacesObjectInputStream(bais).readObject();
            }
            catch (ClassNotFoundException e)
            {
                //e.printStackTrace();
            }
        }
        catch (IOException e) 
        {
            //e.printStackTrace();
        }
        return copy;
    }    
    
    private javax.validation.Validator createValidator(final ValidatorFactory validatorFactory, 
            FacesContext context, ValidateWholeBeanComponent component)
    {
        // Set default validation group when setValidationGroups has not been called.
        // The null check is there to prevent it from happening twice.
        if (validationGroupsArray == null)
        {
            postSetValidationGroups(component);
        }

        return validatorFactory //
                .usingContext() //
                .messageInterpolator(new FacesMessageInterpolator(
                        validatorFactory.getMessageInterpolator(), context)) //
                .getValidator();

    }

    // This boolean is used to make sure that the log isn't trashed with warnings.
    private static volatile boolean firstValueReferenceWarning = true;

    /**
     * Get the ValueReference from the ValueExpression.
     *
     * @param valueExpression The ValueExpression for value.
     * @param context The FacesContext.
     * @return A ValueReferenceWrapper with the necessary information about the ValueReference.
     */
    private _ValueReferenceWrapper getValueReference(
            final ValueExpression valueExpression, final FacesContext context)
    {
        ELContext elCtx = context.getELContext();
        if (ExternalSpecifications.isUnifiedELAvailable())
        {
            // unified el 2.2 is available --> we can use ValueExpression.getValueReference()
            // we can't access ValueExpression.getValueReference() directly here, because
            // Class loading would fail in applications with el-api versions prior to 2.2
            _ValueReferenceWrapper wrapper = _BeanValidatorUELUtils.getUELValueReferenceWrapper(valueExpression, elCtx);
            if (wrapper != null)
            {
                if (wrapper.getProperty() == null)
                {
                    // Fix for issue in Glassfish EL-impl-2.2.3
                    if (firstValueReferenceWarning && log.isLoggable(Level.WARNING))
                    {
                        firstValueReferenceWarning = false;
                        log.warning("ValueReference.getProperty() is null. " +
                                    "Falling back to classic ValueReference resolving. " +
                                    "This fallback may hurt performance. " +
                                    "This may be caused by a bug your EL implementation. " +
                                    "Glassfish EL-impl-2.2.3 is known for this issue. " +
                                    "Try switching to a different EL implementation.");
                    }
                }
                else
                {
                    return wrapper;
                }
            }
        }

        // get base object and property name the "old-fashioned" way
        return _ValueReferenceResolver.resolve(valueExpression, elCtx);
    }

    /**
     * This method creates ValidatorFactory instances or retrieves them from the container.
     *
     * Once created, ValidatorFactory instances are stored in the container under the key
     * VALIDATOR_FACTORY_KEY for performance.
     *
     * @param context The FacesContext.
     * @return The ValidatorFactory instance.
     * @throws FacesException if no ValidatorFactory can be obtained because: a) the
     * container is not a Servlet container or b) because Bean Validation is not available.
     */
    private ValidatorFactory createValidatorFactory(FacesContext context)
    {
        Map<String, Object> applicationMap = context.getExternalContext().getApplicationMap();
        Object attr = applicationMap.get(VALIDATOR_FACTORY_KEY);
        if (attr instanceof ValidatorFactory)
        {
            return (ValidatorFactory) attr;
        }
        else
        {
            synchronized (this)
            {
                if (ExternalSpecifications.isBeanValidationAvailable())
                {
                    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
                    applicationMap.put(VALIDATOR_FACTORY_KEY, factory);
                    return factory;
                }
                else
                {
                    throw new FacesException("Bean Validation is not present");
                }
            }
        }
    }

    /**
     * Fully initialize the validation groups if needed.
     * If no validation groups are specified, the Default validation group is used.
     */
    private void postSetValidationGroups(ValidateWholeBeanComponent component)
    {
        String validationGroups = getValidationGroups(component);
        if (validationGroups == null || validationGroups.matches(EMPTY_VALIDATION_GROUPS_PATTERN))
        {
            this.validationGroupsArray = DEFAULT_VALIDATION_GROUPS_ARRAY;
        }
        else
        {
            String[] classes = validationGroups.split(VALIDATION_GROUPS_DELIMITER);
            List<Class<?>> validationGroupsList = new ArrayList<Class<?>>(classes.length);

            for (String clazz : classes)
            {
                clazz = clazz.trim();
                if (!clazz.equals(""))
                {
                    Class<?> theClass = null;
                    ClassLoader cl = null;
                    if (System.getSecurityManager() != null) 
                    {
                        try 
                        {
                            cl = AccessController.doPrivileged(new PrivilegedExceptionAction<ClassLoader>()
                                    {
                                        public ClassLoader run() throws PrivilegedActionException
                                        {
                                            return Thread.currentThread().getContextClassLoader();
                                        }
                                    });
                        }
                        catch (PrivilegedActionException pae)
                        {
                            throw new FacesException(pae);
                        }
                    }
                    else
                    {
                        cl = Thread.currentThread().getContextClassLoader();
                    }
                    
                    try
                    {                        
                        // Try WebApp ClassLoader first
                        theClass = Class.forName(clazz,false,cl);
                    }
                    catch (ClassNotFoundException ignore)
                    {
                        try
                        {
                            // fallback: Try ClassLoader for BeanValidator (i.e. the myfaces.jar lib)
                            theClass = Class.forName(clazz,false, BeanValidator.class.getClassLoader());
                        }
                        catch (ClassNotFoundException e)
                        {
                            throw new RuntimeException("Could not load validation group", e);
                        }                        
                    }
                    // the class was found
                    validationGroupsList.add(theClass);
                }
            }
                    
            this.validationGroupsArray = validationGroupsList.toArray(new Class[validationGroupsList.size()]);
        }
    }

    /**
     * Get the Bean Validation validation groups.
     * @return The validation groups String.
     */
    @JSFProperty
    public String getValidationGroups(ValidateWholeBeanComponent component)
    {
        return component.getValidationGroups();
    }

    /**
     * Set the Bean Validation validation groups.
     * @param validationGroups The validation groups String, separated by
     *                         {@link BeanValidator#VALIDATION_GROUPS_DELIMITER}.
     */
    public void setValidationGroups(ValidateWholeBeanComponent component, final String validationGroups)
    {
        component.setValidationGroups(validationGroups);
    }

    /**
     * Note: Before 2.1.5/2.0.11 there was another strategy for this point to minimize
     * the instances used, but after checking this with a profiler, it is more expensive to
     * call FacesContext.getCurrentInstance() than create this object for bean validation.
     * 
     * Standard MessageInterpolator, as described in the JSR-314 spec.
     */
    private static class FacesMessageInterpolator implements MessageInterpolator
    {
        private final FacesContext facesContext;
        private final MessageInterpolator interpolator;

        public FacesMessageInterpolator(final MessageInterpolator interpolator, final FacesContext facesContext)
        {
            this.interpolator = interpolator;
            this.facesContext = facesContext;
        }

        public String interpolate(final String s, final MessageInterpolator.Context context)
        {
            Locale locale = facesContext.getViewRoot().getLocale();
            return interpolator.interpolate(s, context, locale);
        }

        public String interpolate(final String s, final MessageInterpolator.Context context, final Locale locale)
        {
            return interpolator.interpolate(s, context, locale);
        }
    }

    private static class UpdateBeanCopyCallback implements VisitCallback
    {
        private WholeBeanValidator validator;
        private Object wholeBeanBase;
        private Object wholeBeanBaseCopy;
        private Map<String, Object> candidateValuesMap;

        public UpdateBeanCopyCallback(WholeBeanValidator validator, Object wholeBeanBase, Object wholeBeanBaseCopy,
                Map<String, Object> candidateValuesMap)
        {
            this.validator = validator;
            this.wholeBeanBase = wholeBeanBase;
            this.wholeBeanBaseCopy = wholeBeanBaseCopy;
            this.candidateValuesMap = candidateValuesMap;
        }

        @Override
        public VisitResult visit(VisitContext context, UIComponent target)
        {
            // The idea is follow almost the same algorithm used by Bean Validation. This 
            // algorithm calculates the base of the ValueExpression used by the component.
            // Then a simple equals() check will do the trick to decide when to call
            // setValue and affect the model. If the base is the same than the value returned by
            // f:validateWholeBean, you are affecting to same instance.
            // 
            
            ValueExpression valueExpression = target.getValueExpression("value");
            if (valueExpression == null)
            {
                log.warning("cannot validate component with empty value: " 
                        + target.getClientId(context.getFacesContext()));
                return VisitResult.ACCEPT;
            }

            // Obtain a reference to the to-be-validated object and the property name.
            _ValueReferenceWrapper reference = validator.getValueReference(
                    valueExpression, context.getFacesContext());
            if (reference == null)
            {
                return VisitResult.ACCEPT;
            }
            Object base = reference.getBase();
            if (base == null)
            {
                return VisitResult.ACCEPT;
            }

            Object referenceProperty = reference.getProperty();
            if (!(referenceProperty instanceof String))
            {
                // if the property is not a String, the ValueReference does not
                // point to a bean method, but e.g. to a value in a Map, thus we 
                // can exit bean validation here
                return VisitResult.ACCEPT;
            }
            String valueProperty = (String) referenceProperty;
            
            // If the base of the EL expression is the same to the base of the one in f:validateWholeBean
            if (base == this.wholeBeanBase || base.equals(this.wholeBeanBase))
            {
                // Do the trick over ELResolver and apply it to the copy.
                
                ELContext elCtxDecorator = new _ELContextDecorator(context.getFacesContext().getELContext(),
                        new CopyBeanInterceptorELResolver(context.getFacesContext().getApplication().getELResolver(),
                            this.wholeBeanBase, this.wholeBeanBaseCopy));
                
                valueExpression.setValue(elCtxDecorator, candidateValuesMap.get(
                        target.getClientId(context.getFacesContext())));
            }
            return VisitResult.ACCEPT;
        }
    }
}