/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.container.beanvalidation;

import static com.ibm.ws.jpa.management.JPAConstants.JPA_RESOURCE_BUNDLE_NAME;
import static com.ibm.ws.jpa.management.JPAConstants.JPA_TRACE_GROUP;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.validation.ClockProvider;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.ConstraintViolation;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.TraversableResolver;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;
import javax.validation.executable.ExecutableValidator;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.valueextraction.ValueExtractor;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * The purpose of this class is to delay obtaining obtaining the
 * javax.validation.ValidatorFactory instance for as long as possible.
 * 
 * This is only used for JPA providers that are doing bean validation,
 * and the point is to delay doing real bean validation work until after
 * the classes have been 'transformed' and the MMD has been placed on the thread.
 * 
 * Instances of this wrapper are NOT cached in the JPAValidatorFactory wrapper.
 * Rather, each time JPAValidatorFactory.getValidator() is invoked, a new instance
 * of this wrapper is created and returned. This matches the behavior of
 * javax.validation.ValidatorFactory, which also creates a new instance
 * of the Validator each time the .getValidator() method is invoked.
 */
public class JPAValidator implements Validator
{
    private static final TraceComponent tc = Tr.register(JPAValidator.class,
                                                         JPA_TRACE_GROUP,
                                                         JPA_RESOURCE_BUNDLE_NAME);

    private Validator ivValidator = null;

    /**
     * Indicates if this JPAValidatorWrapper was created by a JPAValidatorFactory,
     * or a JPAValidatorContextWrapper.
     * 
     * This matters because the two flows differ in the way they obtain the
     * underlying javax.validation.ValidatorFactory instance.
     */
    private boolean fromValidatorFactory = true;

    // The user can ask the ValidatorContext to create the Validator instance
    // using non-default versions of the TraversableResolver, MessageInterpolator,
    // and ConstraintValidatorFactory.
    //
    // In this case, the specified instances of these objects are cached in the
    // ValidatorContext wrapper until the wrapper is ask to create the Validator
    // instance.
    //
    // Of course, we still don't actually create the Validator instance at that
    // time...rather, we just give back this ValidatorWrapper...which means that
    // this ValidatorWrapper needs to be able to create the underlying
    // javax.validation.Validator instance at some point in the future if needed.
    //
    // To make this possible, these objects are passed into this ValidatorWrapper
    // by the JPAValidatorContextWrapper, and they are cached here until if/when
    // we need them to actually create the javax.validation.Validator instance.
    private TraversableResolver ivSpecifiedTraversableResolver;
    private MessageInterpolator ivSpecifiedMessageInterpolator;
    private ConstraintValidatorFactory ivSpecifiedConstraintValidatorFactory;
    private ParameterNameProvider ivSpecifiedParameterNameProvider;
    private ClockProvider ivClockProvider;
    private List<ValueExtractor<?>> ivValueExtractorList;
    private final ValidatorFactoryLocator ivValidatorFactoryLocator;

    /**
     * This constructor should be called from the JPAValidatorFactory,
     * to mimic the flow where javax.validation.ValidationFactory.getValidator()
     * is called.
     */
    public JPAValidator(ValidatorFactoryLocator locator)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
        {
            Tr.debug(tc, "Creating JPAValidatorWrapper using default constructor.");
        }

        fromValidatorFactory = true;
        ivValidatorFactoryLocator = locator;
    }

    /**
     * This constructor should be called from the JPAValidatorContext,
     * to mimic the flow where javax.validation.ValidatorContext.getValidator()
     * is called.
     * 
     * The specified parms represent the TraversableResolver, MessageInterpolator,
     * and ConstraintValidatorFactory instances that the user wanted the
     * ValidatorContext to use when it created the Validator instance. So, the
     * wrapper will use these instances in the course of creating the Validator
     * instance, if it turns out that we need to.
     * 
     * Any/all of these objects may be null. In that case, it simply means that
     * the user did not explicitly request a different instance of that object
     * type, and they want the ValidatorContext to use the default instance of
     * that object in the course of creating the Validator.
     * 
     */
    public JPAValidator(ValidatorFactoryLocator locator,
                        TraversableResolver traversableResolver,
                        MessageInterpolator messageInterpolator,
                        ConstraintValidatorFactory constraintValidatorFactory,
                        ParameterNameProvider parameterNameProvider,
                        ClockProvider clockProvider,
                        List<ValueExtractor<?>> valueExtractorList)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
        {
            Tr.debug(tc, "Creating JPAValidatorWrapper using non-default constructor.");
        }

        fromValidatorFactory = false;
        ivValidatorFactoryLocator = locator;
        ivSpecifiedTraversableResolver = traversableResolver;
        ivSpecifiedMessageInterpolator = messageInterpolator;
        ivSpecifiedConstraintValidatorFactory = constraintValidatorFactory;
        ivClockProvider = clockProvider;
        ivValueExtractorList = new ArrayList<ValueExtractor<?>>(valueExtractorList);
        ivSpecifiedParameterNameProvider = parameterNameProvider;
    }

    /**
     * Obtains the underlying javax.validation.Validator instance that is used by
     * this wrapper.
     * 
     * The underlying javax.validation.Validator instance is obtained in one of
     * two ways.
     * 
     * If this JPAValidatorWrapper was obtained from the JPAValidatorFactory,
     * then the Validator instance is obtained from the ValidatorFactory instance.
     * 
     * On the other hand, if this JPAValidatorWrapper was obtained from the
     * JPAValidatorContextWrapper, then the Validator instance is obtained from the
     * ValidatorContext, and the specified TraversableResolver, MessageInterpolator,
     * and ConstraintValidatorFactory instances are used as part of this process.
     */
    private void obtainValidator()
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (fromValidatorFactory)
        {
            if (isTraceOn && tc.isDebugEnabled())
            {
                Tr.debug(tc, "Obtaining Validator instance from ValidatorFactory...");
            }

            ValidatorFactory validatorFactory = ivValidatorFactoryLocator.getValidatorFactory();
            ivValidator = validatorFactory.getValidator();
        }
        else
        {
            if (isTraceOn && tc.isDebugEnabled())
            {
                Tr.debug(tc, "Obtaining Validator instance from ValidatorContext, using TraversableResolver " + ivSpecifiedTraversableResolver +
                             ", message interpolator " + ivSpecifiedMessageInterpolator +
                             ", parameter name provider " + ivSpecifiedParameterNameProvider
                             + ", and constraint validator factory " + ivSpecifiedConstraintValidatorFactory);
            }

            ValidatorFactory validatorFactory = ivValidatorFactoryLocator.getValidatorFactory();
            ValidatorContext validatorContext = validatorFactory.usingContext();

            if (ivSpecifiedTraversableResolver != null)
            {
                validatorContext.traversableResolver(ivSpecifiedTraversableResolver);
            }

            if (ivSpecifiedMessageInterpolator != null)
            {
                validatorContext.messageInterpolator(ivSpecifiedMessageInterpolator);
            }

            if (ivSpecifiedConstraintValidatorFactory != null)
            {
                validatorContext.constraintValidatorFactory(ivSpecifiedConstraintValidatorFactory);
            }

            if (ivSpecifiedParameterNameProvider != null)
                validatorContext.parameterNameProvider(ivSpecifiedParameterNameProvider);
            
            if (ivClockProvider != null) {
            		validatorContext.clockProvider(ivClockProvider);
            }
            
            if (ivValueExtractorList != null && ivValueExtractorList.size() > 0) {
            		for (ValueExtractor<?> ve : ivValueExtractorList) {
            			validatorContext.addValueExtractor(ve);
            		}
            }

            ivValidator = validatorContext.getValidator();
        }

        if (isTraceOn && tc.isDebugEnabled())
        {
            Tr.debug(tc, "Obtained the Validator: " + ivValidator);
        }

    }

    public BeanDescriptor getConstraintsForClass(Class<?> clazz)
    {
        if (ivValidator == null)
        {
            obtainValidator();
        }

        return ivValidator.getConstraintsForClass(clazz);
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups)
    {
        if (ivValidator == null)
        {
            obtainValidator();
        }

        return ivValidator.validate(object, groups);
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateProperty(T object,
                                                            String propertyName,
                                                            Class<?>... groups)
    {
        if (ivValidator == null)
        {
            obtainValidator();
        }

        return ivValidator.validateProperty(object, propertyName, groups);
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType,
                                                         String propertyName,
                                                         Object value,
                                                         Class<?>... groups)
    {
        if (ivValidator == null)
        {
            obtainValidator();
        }

        return ivValidator.validateValue(beanType, propertyName, value, groups);
    }

    @Override
    public <T> T unwrap(Class<T> type)
    {
        if (ivValidator == null)
        {
            obtainValidator();
        }

        return ivValidator.unwrap(type);
    }

    @Override
    public ExecutableValidator forExecutables() {
        if (ivValidator == null)
            obtainValidator();

        return ivValidator.forExecutables();
    }

}
