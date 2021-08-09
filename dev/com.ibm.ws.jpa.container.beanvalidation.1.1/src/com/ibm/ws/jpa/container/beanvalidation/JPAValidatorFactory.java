/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.container.beanvalidation;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.TraversableResolver;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;

/*
 * This class is used by the plugged in 3rd party JPA providers (such as OpenJPA) to
 * perform bean validation.
 *
 * The JPA support in WAS hands the plugged in JPA provider a Map that contains
 * various environmental artifacts that the JPA provider might need...and one of
 * those artifacts is the javax.validation.ValidatorFactory instance....or at least
 * what the JPA provider thinks is the javax.validation.ValidatorFactory instance.
 *
 * In reality, JPA shoves this JPAValidatorFactory into the Map, and so this is the
 * actual object that the plugged in JPA providers are interacting with.  This is
 * legal because the JPAValidatorFactory implements the javax.validation.ValidatorFactory
 * interface.
 *
 * This JPAValidatorFactory is a wrapper/proxy that attempts to put off
 * obtaining the real javax.valdiation.ValidatorFactory instance for as long as it
 * possibly can.
 *
 * This extreme stalling is needed due to the classloading requirements of JPA.  The
 * plugged in JPA provider is allowed to hook into the application startup process
 * (in response to the 'module starting' event JPA receives on account of it being
 * a DeployedObjectListener) and perform work prior to the modules being started.
 * Among other things, the JPA provider may need to 'transform' the module classes
 * before they are loaded by a classloader instance.
 *
 * Once control is turned over to the JPA provider to do things, the provider can do
 * whatever it wants...including trying to perform bean validation.  Unfortunately, in order
 * to do real bean validation work (which requires that we get the real ValidatorFactory instance),
 * we need to load some classes, and we'll likely be loading classes that JPA needs to 'transform'
 * (because those are likely the classes that we're trying to validate), and that breaks JPAs
 * "must transforms the classes before they are loaded" requirement.
 *
 * In other words, given the current classloading environment in WAS, and given JPAs requirement
 * that the classes be "transformed first/loaded second", its simply not possible to do real
 * bean validation before a certain point in time.
 *
 * The idea behind this JPAValidatorFactory wrapper is to avoid trying to do the real bean validation
 * for as long as possible, in the hopes that when it does finally happen, it'll be after the classes
 * have been 'transformed', and after we've already obtained the real ValidatorFactory instance and
 * shoved it onto the MMD, and after the MMD is already on the thread....in other words, we're hoping
 * to delay bean validation until its safe to actually do it.
 *
 * The JPAValidatorFactory wrapper tries to accomplish this by handling back a
 * "child wrapper" for each type of object its methods return (for example, handing
 * back a 'JPAConstraintValidatorFactory', etc).  In turn, each of these child
 * wrappers implements on the methods on its respective interface (ie, the
 * JPAConstraintValidatorFactory implements all the methods on the
 * java.validation.ConstraintValidatorFactory interface).  Each of these methods
 * is implemented to ask the actual factory (ValidatorFactoryObjectFactory) for the
 * real javax.validation.ValidatorFactory instance, and then it gets the needed
 * object from the ValidatorFactory instance (such as the ConstraintValidatorFactory),
 * invokes the requested method on that object, and returns the result to the user.
 *
 * In regards to the various wrappers and the underlying 'real' objects they
 * contain, the following caching strategy is used:
 *      - If the javax.validation.ValidatorFactory instance returns the same
 *        object instance each time, then we maintain that behavior by having
 *        this JPAValidatorFactory re-use the same wrapper instance each time.
 *
 *        This is the case for the MessageInterpolator, ConstraintValidatorFactory,
 *        and the TraversableResolver.
 *
 *      - If the javax.validation.ValidatorFactory instance returns a new
 *        object instance each time, then we maintain that behavior by having
 *        this JPAValidatorFactory create a new instance of the wrapper each time.
 *
 *        This is the case for the Validator, and the ValidatorContext.
 *
 *      - All the wrappers re-use their underlying 'real' object instance...ie,
 *        there is a one-to-one relationship between a wrapper instance and its
 *        underlying object instance (regardless of whether the wrapper itself
 *        is cached).
 *
 * This wrapper design is dependent upon the plugged in JPA provider
 * maintaining a one-to-one relationship between this JPAValidatorFactory instance
 * and a module.  The design is broken if the JPA provider uses a single JPAValidatorFactory
 * instance to validate multiple modules.  Since the JPA provider thinks the
 * JPAValidatorFactory is actually the javax.validation.ValidatorFactory instance, and
 * since the javax.validation.ValidatorFactory instance is suppose to have a one-to-one
 * relationship with a module, we should be safe in assuming the JPA provider will honor
 * this contract.
 *
 * So, we've got the following scenarios:
 * 1) JPA provider does not attempt to use the ValidatorFactory...which means
 *    this wrapper never gets touched, and so there is no problem.
 *
 * 2) JPA provider asks for the ValidatorFactory...and they are given this
 *    wrapper instance...but they never invoke any methods on it, and so there
 *    is no problem.
 *
 * 3) JPA provider asks for the ValidatorFactory...and they are given this
 *    wrapper instance...and they invoke a method on it...and they get back
 *    a child wrapper...but they never invoke a method on the child wrapper,
 *    and so there is no problem.
 *
 * 4) Same as #3...except this time the JPA provider does invoke a method on the
 *    child wrapper...but they waited a while to do this, and by now its "safe"
 *    to do bean validation (meaning, the classes have been 'transformed', and the
 *    MMD is on the thread and has the real ValidatorFactory cached in its slot),
 *    and so we go off and do real bean validation, and there is no problem.
 *
 * 5) Same as #4....except the JPA provider invokes the method on the child wrapper
 *    before its "safe" to do bean validation (meaning, the classes are not 'transformed'
 *    yet, or the MMD is not on the thread)...and in this case, we will fail, and there
 *    is nothing we can do about it.  This is simply a limitation of the server.  The
 *    hope is that most JPA providers will not encounter this scenario.
 *
 * Note: This "defer as long as possible using wrappers" solution is only used for
 *       the special case JPA flow that uncovered this problem.  Its obviously worse
 *       for performance to add these extra layers of wrappers, and in the typical
 *       use cases (ie, an injection/lookup of the ValidatorFactory inside an ejb or
 *       web component) there is no need to defer anything (because injection and
 *       naming lookups always occur after its "safe" to obtain the real ValidatorFactory).
 */

public final class JPAValidatorFactory implements ValidatorFactory
{
    private final ValidatorFactoryLocator ivValidatorFactoryLocator;
    private JPAMessageInterpolator ivMessageInterpolatorWrapper;
    private JPAConstraintValidatorFactory ivConstraintValidatorFactoryWrapper;
    private JPAParameterNameProvider ivParameterNameProvider;
    private JPATraversableResolver ivTraversableResolverWrapper;

    public JPAValidatorFactory(ValidatorFactoryLocator locator)
    {
        ivValidatorFactoryLocator = locator;
    }

    @Override
    public ConstraintValidatorFactory getConstraintValidatorFactory()
    {
        if (ivConstraintValidatorFactoryWrapper == null)
        {
            ivConstraintValidatorFactoryWrapper = new JPAConstraintValidatorFactory(ivValidatorFactoryLocator);
        }

        return ivConstraintValidatorFactoryWrapper;
    }

    @Override
    public MessageInterpolator getMessageInterpolator()
    {
        if (ivMessageInterpolatorWrapper == null)
        {
            ivMessageInterpolatorWrapper = new JPAMessageInterpolator(ivValidatorFactoryLocator);
        }

        return ivMessageInterpolatorWrapper;
    }

    @Override
    public TraversableResolver getTraversableResolver()
    {
        if (ivTraversableResolverWrapper == null)
        {
            ivTraversableResolverWrapper = new JPATraversableResolver(ivValidatorFactoryLocator);
        }

        return ivTraversableResolverWrapper;
    }

    @Override
    public Validator getValidator()
    {
        return new JPAValidator(ivValidatorFactoryLocator);
    }

    @Override
    public <T> T unwrap(Class<T> type)
    {
        ValidatorFactory validatorFactory = ivValidatorFactoryLocator.getValidatorFactory();
        return validatorFactory.unwrap(type);
    }

    @Override
    public ValidatorContext usingContext()
    {
        return new JPAValidatorContext(ivValidatorFactoryLocator);
    }

    @Override
    public void close() {
        ivValidatorFactoryLocator.getValidatorFactory().close();
    }

    @Override
    public ParameterNameProvider getParameterNameProvider() {
        if (ivParameterNameProvider == null)
            ivParameterNameProvider = new JPAParameterNameProvider(ivValidatorFactoryLocator);

        return ivParameterNameProvider;
    }
}
