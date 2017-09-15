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

import java.beans.FeatureDescriptor;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.FunctionMapper;
import javax.el.ValueExpression;
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
    
    private static final Class<?>[] DEFAULT_VALIDATION_GROUPS_ARRAY = new Class<?>[] { Default.class };

    private static final String DEFAULT_VALIDATION_GROUP_NAME = "javax.validation.groups.Default";

    private String validationGroups;

    private Class<?>[] validationGroupsArray;

    private boolean isTransient = false;

    private boolean _initialStateMarked = false;

    /**
     * {@inheritDoc}
     */
    public void validate(final FacesContext context, final UIComponent component, final Object value)
            throws ValidatorException
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
        _ValueReferenceWrapper reference = getValueReference(valueExpression, context);
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
            throw new ValidatorException(messages);
        }
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
        if (_ExternalSpecifications.isUnifiedELAvailable())
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

    /** {@inheritDoc} */
    public Object saveState(final FacesContext context)
    {
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
    public void restoreState(final FacesContext context, final Object state)
    {
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
    public boolean isTransient()
    {
        return isTransient;
    }

    /** {@inheritDoc} */
    public void setTransient(final boolean isTransient)
    {
        this.isTransient = isTransient;
    }

    /** {@inheritDoc} */
    public void clearInitialState()
    {
        _initialStateMarked = false;
    }

    /** {@inheritDoc} */
    public boolean initialStateMarked()
    {
        return _initialStateMarked;
    }

    /** {@inheritDoc} */
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

        public String interpolate(final String s, final Context context)
        {
            Locale locale = facesContext.getViewRoot().getLocale();
            return interpolator.interpolate(s, context, locale);
        }

        public String interpolate(final String s, final Context context, final Locale locale)
        {
            return interpolator.interpolate(s, context, locale);
        }
    }
}

/**
 * This class provides access to the object pointed to by the EL expression.
 *
 * It makes the BeanValidator work when Unified EL is not available.
 */
final class _ValueReferenceWrapper
{
    private final Object base;
    private final Object property;

    /**
     * Full constructor.
     *
     * @param base The object the reference points to.
     * @param property The property the reference points to.
     */
    public _ValueReferenceWrapper(final Object base, final Object property)
    {
        this.base = base;
        this.property = property;
    }

    /**
     * The object the reference points to.
     * @return base.
     */
    public Object getBase()
    {
        return base;
    }

    /**
     * The property the reference points to.
     * @return property.
     */
    public Object getProperty()
    {
        return property;
    }
}

/**
 * This class inspects the EL expression and returns a ValueReferenceWrapper
 * when Unified EL is not available.
 */
final class _ValueReferenceResolver extends ELResolver
{
    private final ELResolver resolver;

    /**
     * This is a simple solution to keep track of the resolved objects,
     * since ELResolver provides no way to know if the current ELResolver
     * is the last one in the chain. By assigning (and effectively overwriting)
     * this field, we know that the value after invoking the chain is always
     * the last one.
     *
     * This solution also deals with nested objects (like: #{myBean.prop.prop.prop}.
     */
    private _ValueReferenceWrapper lastObject;

    /**
     * Constructor is only used internally.
     * @param elResolver An ELResolver from the current ELContext.
     */
    _ValueReferenceResolver(final ELResolver elResolver)
    {
        this.resolver = elResolver;
    }

    /**
     * This method can be used to extract the ValueReferenceWrapper from the given ValueExpression.
     *
     * @param valueExpression The ValueExpression to resolve.
     * @param elCtx The ELContext, needed to parse and execute the expression.
     * @return The ValueReferenceWrapper.
     */
    public static _ValueReferenceWrapper resolve(ValueExpression valueExpression, final ELContext elCtx)
    {
        _ValueReferenceResolver resolver = new _ValueReferenceResolver(elCtx.getELResolver());
        ELContext elCtxDecorator = new _ELContextDecorator(elCtx, resolver);
        
        valueExpression.getValue(elCtxDecorator);
        
        while (resolver.lastObject.getBase() instanceof CompositeComponentExpressionHolder)
        {
            valueExpression = ((CompositeComponentExpressionHolder) resolver.lastObject.getBase())
                                  .getExpression((String) resolver.lastObject.getProperty());
            valueExpression.getValue(elCtxDecorator);
        }

        return resolver.lastObject;
    }

    /**
     * This method is the only one that matters. It keeps track of the objects in the EL expression.
     *
     * It creates a new ValueReferenceWrapper and assigns it to lastObject.
     *
     * @param context The ELContext.
     * @param base The base object, may be null.
     * @param property The property, may be null.
     * @return The resolved value
     */
    @Override
    public Object getValue(final ELContext context, final Object base, final Object property)
    {
        lastObject = new _ValueReferenceWrapper(base, property);
        return resolver.getValue(context, base, property);
    }

    // ############################ Standard delegating implementations ############################
    public Class<?> getType(final ELContext ctx, final Object base, final Object property)
    {
        return resolver.getType(ctx, base, property);
    }

    public void setValue(final ELContext ctx, final Object base, final Object property, final Object value)
    {
        resolver.setValue(ctx, base, property, value);
    }

    public boolean isReadOnly(final ELContext ctx, final Object base, final Object property)
    {
        return resolver.isReadOnly(ctx, base, property);
    }

    public Iterator<FeatureDescriptor> getFeatureDescriptors(final ELContext ctx, final Object base)
    {
        return resolver.getFeatureDescriptors(ctx, base);
    }

    public Class<?> getCommonPropertyType(final ELContext ctx, final Object base)
    {
        return resolver.getCommonPropertyType(ctx, base);
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
    public FunctionMapper getFunctionMapper()
    {
        return ctx.getFunctionMapper();
    }

    public VariableMapper getVariableMapper()
    {
        return ctx.getVariableMapper();
    }

    public void setPropertyResolved(final boolean resolved)
    {
        ctx.setPropertyResolved(resolved);
    }

    public boolean isPropertyResolved()
    {
        return ctx.isPropertyResolved();
    }

    public void putContext(final Class key, Object contextObject)
    {
        ctx.putContext(key, contextObject);
    }

    public Object getContext(final Class key)
    {
        return ctx.getContext(key);
    }

    public Locale getLocale()
    {
        return ctx.getLocale();
    }

    public void setLocale(final Locale locale)
    {
        ctx.setLocale(locale);
    }
}
