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
package javax.faces.validator;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.FunctionMapper;
import javax.el.ValueExpression;
import javax.el.ValueReference;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.application.FacesMessage;
import javax.faces.component.PartialStateHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.el.CompositeComponentExpressionHolder;
import javax.validation.ConstraintViolation;
import javax.validation.MessageInterpolator;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.validation.groups.Default;
import javax.validation.metadata.BeanDescriptor;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspProperty;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFValidator;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;

/**
 * <p>
 * <strong>BeanValidator</strong> is a {@link javax.faces.validator.Validator}
 * that doesn't do any validation itself, but delegates validation logic to
 * Bean Validation.
 * </p>
 *
 * @since 2.0
 */
@JSFValidator(
        name="f:validateBean",
        bodyContent="empty")
@JSFJspProperty(
        name = "binding",
        returnType = "javax.faces.validator.BeanValidator",
        longDesc = "A ValueExpression that evaluates to a BeanValidator.")
public class BeanValidator implements Validator, PartialStateHolder
{

    private static final Logger log = Logger.getLogger(BeanValidator.class.getName());

    /**
     * Converter ID, as defined by the JSF 2.0 specification.
     */
    public static final String VALIDATOR_ID = "javax.faces.Bean";

    /**
     * The message ID for this Validator in the message bundles.
     */
    public static final String MESSAGE_ID = "javax.faces.validator.BeanValidator.MESSAGE";

    /**
     * If this init parameter is present, no Bean Validators should be added to an UIInput by default.
     * Explicitly adding a BeanValidator to an UIInput is possible though.
     */
    @JSFWebConfigParam(defaultValue="true", expectedValues="true, false", since="2.0", group="validation")
    public static final String DISABLE_DEFAULT_BEAN_VALIDATOR_PARAM_NAME
            = "javax.faces.validator.DISABLE_DEFAULT_BEAN_VALIDATOR";

    /**
     * The key in the ServletContext where the Bean Validation Factory can be found.
     * In a managed Java EE 6 environment, the container initializes the ValidatorFactory
     * and stores it in the ServletContext under this key.
     * If not present, the manually instantiated ValidatorFactory is stored in the ServletContext
     * under this key for caching purposes.
     */
    public static final String VALIDATOR_FACTORY_KEY = "javax.faces.validator.beanValidator.ValidatorFactory";

    /**
     * This is used as a separator so multiple validation groups can be specified in one String.
     */
    public static final String VALIDATION_GROUPS_DELIMITER = ",";

    /**
     * This regular expression is used to match for empty validation groups.
     * Currently, a string containing only whitespace is classified as empty.
     */
    public static final String EMPTY_VALIDATION_GROUPS_PATTERN = "^[\\W,]*$";
    
    /**
     * Enable f:validateWholeBean use.
     */
    @JSFWebConfigParam(since="2.3", defaultValue = "false", expectedValues = "true, false", group="validation")
    public static final String ENABLE_VALIDATE_WHOLE_BEAN_PARAM_NAME = 
            "javax.faces.validator.ENABLE_VALIDATE_WHOLE_BEAN";
    
    private static final Class<?>[] DEFAULT_VALIDATION_GROUPS_ARRAY = new Class<?>[] { Default.class };

    private static final String DEFAULT_VALIDATION_GROUP_NAME = "javax.validation.groups.Default";
    
    private static final String CANDIDATE_COMPONENT_VALUES_MAP = "oam.WBV.candidatesMap";
    
    private static final String BEAN_VALIDATION_FAILED = "oam.WBV.validationFailed";

    private String validationGroups;

    private Class<?>[] validationGroupsArray;

    private boolean isTransient = false;

    private boolean _initialStateMarked = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(final FacesContext context, final UIComponent component, final Object value)
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
        ValueReference reference = getValueReference(valueExpression, context);
        if (reference == null)
        {
            return;
        }
        Object base = reference.getBase();
        if (base == null)
        {
            return;
        }
        
        Class<?> valueBaseClass = base.getClass();
        if (valueBaseClass == null)
        {
            return;
        }
        
        Object referenceProperty = reference.getProperty();
        if (!(referenceProperty instanceof String))
        {
            // if the property is not a String, the ValueReference does not
            // point to a bean method, but e.g. to a value in a Map, thus we 
            // can exit bean validation here
            return;
        }
        String valueProperty = (String) referenceProperty;

        // Initialize Bean Validation.
        ValidatorFactory validatorFactory = createValidatorFactory(context);
        javax.validation.Validator validator = createValidator(validatorFactory, context);
        BeanDescriptor beanDescriptor = validator.getConstraintsForClass(valueBaseClass);
        if (!beanDescriptor.isBeanConstrained())
        {
            return;
        }
        
        // Note that validationGroupsArray was initialized when createValidator was called
        Class[] validationGroupsArray = this.validationGroupsArray;

        // JSF 2.3: If the ENABLE_VALIDATE_WHOLE_BEAN_PARAM_NAME application parameter is enabled and this Validator 
        // instance has validation groups other than or in addition to the Default group
        boolean containsOtherValidationGroup = false;
        if (validationGroupsArray != null && validationGroupsArray.length > 0)
        {
            for (Class<?> clazz : validationGroupsArray)
            {
                if (!Default.class.equals(clazz))
                {
                    containsOtherValidationGroup = true;
                    break;
                }
            }
        }
        
        // Delegate to Bean Validation.
        Set constraintViolations = validator.validateValue(valueBaseClass, valueProperty, value, validationGroupsArray);
        if (!constraintViolations.isEmpty())
        {
            Set<FacesMessage> messages = new LinkedHashSet<FacesMessage>(constraintViolations.size());
            for (Object violation: constraintViolations)
            {
                ConstraintViolation constraintViolation = (ConstraintViolation) violation;
                String message = constraintViolation.getMessage();
                Object[] args = new Object[]{ message, _MessageUtils.getLabel(context, component) };
                FacesMessage msg = _MessageUtils.getErrorMessage(context, MESSAGE_ID, args);
                messages.add(msg);
            }
            
            if (isValidateWholeBeanEnabled(context) && containsOtherValidationGroup)
            {
                // JSF 2.3: record the fact that this field failed validation so that any <f:validateWholeBean /> 
                // component later in the tree is able to skip class-level validation for the bean for which this 
                // particular field is a property. Regardless of whether or not 
                // ENABLE_VALIDATE_WHOLE_BEAN_PARAM_NAME is set, throw the new exception.            
                context.getViewRoot().getTransientStateHelper().putTransient(BEAN_VALIDATION_FAILED, Boolean.TRUE);
            }
            
            throw new ValidatorException(messages);
        }
        else
        {
            
            // JSF 2.3: If the returned Set is empty, the ENABLE_VALIDATE_WHOLE_BEAN_PARAM_NAME application parameter
            // is enabled and this Validator instance has validation groups other than or in addition to the 
            // Default group
            if (isValidateWholeBeanEnabled(context) && containsOtherValidationGroup)
            {
                // record the fact that this field passed validation so that any <f:validateWholeBean /> component 
                // later in the tree is able to allow class-level validation for the bean for which this particular 
                // field is a property.
                
                Map<String, Object> candidatesMap = (Map<String, Object>) context.getViewRoot()
                        .getTransientStateHelper().getTransient(CANDIDATE_COMPONENT_VALUES_MAP);
                if (candidatesMap == null)
                {
                    candidatesMap = new LinkedHashMap<String, Object>();
                    context.getViewRoot().getTransientStateHelper().putTransient(
                            CANDIDATE_COMPONENT_VALUES_MAP, candidatesMap);
                }
                candidatesMap.put(component.getClientId(context), value);
            }
        }
    }
    
    private boolean isValidateWholeBeanEnabled(FacesContext facesContext)
    {
        Boolean value = (Boolean) facesContext.getAttributes().get(ENABLE_VALIDATE_WHOLE_BEAN_PARAM_NAME);
        if (value == null)
        {
            String enabled = facesContext.getExternalContext().getInitParameter(ENABLE_VALIDATE_WHOLE_BEAN_PARAM_NAME);
            if (enabled == null)
            {
                value = Boolean.FALSE;
            }
            else
            {
                value = Boolean.valueOf(enabled);
            }
        }
        return Boolean.TRUE.equals(value);
    }

    private javax.validation.Validator createValidator(final ValidatorFactory validatorFactory, FacesContext context)
    {
        // Set default validation group when setValidationGroups has not been called.
        // The null check is there to prevent it from happening twice.
        if (validationGroupsArray == null)
        {
            postSetValidationGroups();
        }

        return validatorFactory //
                .usingContext() //
                .messageInterpolator(new FacesMessageInterpolator(
                        validatorFactory.getMessageInterpolator(), context)) //
                .getValidator();

    }

    /**
     * Get the ValueReference from the ValueExpression.
     *
     * @param valueExpression The ValueExpression for value.
     * @param context The FacesContext.
     * @return A ValueReferenceWrapper with the necessary information about the ValueReference.
     */
    private ValueReference getValueReference(
            final ValueExpression valueExpression, final FacesContext context)
    {
        ELContext elCtx = context.getELContext();
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
                if (_ExternalSpecifications.isBeanValidationAvailable())
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
    private void postSetValidationGroups()
    {
        if (this.validationGroups == null || this.validationGroups.matches(EMPTY_VALIDATION_GROUPS_PATTERN))
        {
            this.validationGroupsArray = DEFAULT_VALIDATION_GROUPS_ARRAY;
        }
        else
        {
            String[] classes = this.validationGroups.split(VALIDATION_GROUPS_DELIMITER);
            List<Class<?>> validationGroupsList = new ArrayList<Class<?>>(classes.length);

            for (String clazz : classes)
            {
                clazz = clazz.trim();
                if (!clazz.isEmpty())
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

    /** {@inheritDoc} */
    @Override
    public Object saveState(final FacesContext context)
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }

        if (!initialStateMarked())
        {
           //Full state saving.
           return this.validationGroups;
        }
        else if (DEFAULT_VALIDATION_GROUP_NAME.equals(this.validationGroups))
        {
            // default validation groups can be saved as null.
            return null;
        }
        else
        {
            // Save it fully. Remember that by MYFACES-2528
            // validationGroups needs to be stored into the state
            // because this value is often susceptible to use in "combo"
            return this.validationGroups;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void restoreState(final FacesContext context, final Object state)
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }

        if (state != null)
        {
            this.validationGroups = (String) state;
        }
        else
        {
            // When the value is being validated, postSetValidationGroups() sets
            // validationGroups to javax.validation.groups.Default. 
            this.validationGroups = null;
        }
        // Only the String is saved, recalculate the Class[] on state restoration.
        //postSetValidationGroups();
    }

    /**
     * Get the Bean Validation validation groups.
     * @return The validation groups String.
     */
    @JSFProperty
    public String getValidationGroups()
    {
        return validationGroups;
    }

    /**
     * Set the Bean Validation validation groups.
     * @param validationGroups The validation groups String, separated by
     *                         {@link BeanValidator#VALIDATION_GROUPS_DELIMITER}.
     */
    public void setValidationGroups(final String validationGroups)
    {
        this.validationGroups = validationGroups;
        this.clearInitialState();
        //postSetValidationGroups();
    }

    @JSFProperty
    @SuppressWarnings("unused")
    private Boolean isDisabled()
    {
        return null;
    }
    
    @JSFProperty
    @SuppressWarnings("unused")
    private String getFor()
    {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isTransient()
    {
        return isTransient;
    }

    /** {@inheritDoc} */
    @Override
    public void setTransient(final boolean isTransient)
    {
        this.isTransient = isTransient;
    }

    /** {@inheritDoc} */
    @Override
    public void clearInitialState()
    {
        _initialStateMarked = false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean initialStateMarked()
    {
        return _initialStateMarked;
    }

    /** {@inheritDoc} */
    @Override
    public void markInitialState()
    {
        _initialStateMarked = true;
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

        @Override
        public String interpolate(final String s, final Context context)
        {
            Locale locale = facesContext.getViewRoot().getLocale();
            return interpolator.interpolate(s, context, locale);
        }

        @Override
        public String interpolate(final String s, final Context context, final Locale locale)
        {
            return interpolator.interpolate(s, context, locale);
        }
    }
}

final class _ValueReferenceResolver
{
    /**
     * This method can be used to extract the ValueReference from the given ValueExpression.
     *
     * @param valueExpression The ValueExpression to resolve.
     * @param elCtx The ELContext, needed to parse and execute the expression.
     * @return The ValueReferenceWrapper.
     */
    public static ValueReference resolve(ValueExpression valueExpression, final ELContext elCtx)
    {
        ValueReference valueReference = valueExpression.getValueReference(elCtx);
        
        while (valueReference != null && valueReference.getBase() instanceof CompositeComponentExpressionHolder)
        {
            valueExpression = ((CompositeComponentExpressionHolder) valueReference.getBase())
                                  .getExpression((String) valueReference.getProperty());
            if(valueExpression == null)
            {
                break;
            }
            valueReference = valueExpression.getValueReference(elCtx);
        }
        
        return valueReference;
    }
}

/**
 * This ELContext is used to hook into the EL handling, by decorating the
 * ELResolver chain with a custom ELResolver.
 */
final class _ELContextDecorator extends ELContext
{
    private final ELContext ctx;
    private final ELResolver interceptingResolver;

    /**
     * Only used by ValueExpressionResolver.
     *
     * @param elContext The standard ELContext. All method calls, except getELResolver, are delegated to it.
     * @param interceptingResolver The ELResolver to be returned by getELResolver.
     */
    _ELContextDecorator(final ELContext elContext, final ELResolver interceptingResolver)
    {
        this.ctx = elContext;
        this.interceptingResolver = interceptingResolver;
    }

    /**
     * This is the important one, it returns the passed ELResolver.
     * @return The ELResolver passed into the constructor.
     */
    @Override
    public ELResolver getELResolver()
    {
        return interceptingResolver;
    }

    // ############################ Standard delegating implementations ############################
    @Override
    public FunctionMapper getFunctionMapper()
    {
        return ctx.getFunctionMapper();
    }

    @Override
    public VariableMapper getVariableMapper()
    {
        return ctx.getVariableMapper();
    }

    @Override
    public void setPropertyResolved(final boolean resolved)
    {
        ctx.setPropertyResolved(resolved);
    }

    @Override
    public boolean isPropertyResolved()
    {
        return ctx.isPropertyResolved();
    }

    @Override
    public void putContext(final Class key, Object contextObject)
    {
        ctx.putContext(key, contextObject);
    }

    @Override
    public Object getContext(final Class key)
    {
        return ctx.getContext(key);
    }

    @Override
    public Locale getLocale()
    {
        return ctx.getLocale();
    }

    @Override
    public void setLocale(final Locale locale)
    {
        ctx.setLocale(locale);
    }
}
