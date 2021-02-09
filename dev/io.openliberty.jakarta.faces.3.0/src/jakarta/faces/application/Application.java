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
package jakarta.faces.application;

import jakarta.faces.component.behavior.Behavior;
import jakarta.faces.component.search.SearchExpressionHandler;
import jakarta.faces.component.search.SearchKeywordResolver;
import jakarta.faces.event.ActionListener;
import jakarta.faces.event.SystemEvent;
import jakarta.faces.event.SystemEventListener;
import jakarta.faces.validator.Validator;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import jakarta.el.ELContextListener;
import jakarta.el.ELException;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.el.ValueExpression;
import jakarta.faces.FacesException;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.el.MethodBinding;
import jakarta.faces.el.PropertyResolver;
import jakarta.faces.el.ReferenceSyntaxException;
import jakarta.faces.el.ValueBinding;
import jakarta.faces.el.VariableResolver;
import jakarta.faces.flow.FlowHandler;

/**
 * <p>
 * Application represents a per-web-application singleton object where applications based on JavaServer Faces (or
 * implementations wishing to provide extended functionality) can register application-wide singletons that provide
 * functionality required by JavaServer Faces. Default implementations of each object are provided for cases where the
 * application does not choose to customize the behavior.
 * </p>
 * 
 * <p>
 * The instance of {@link Application} is created by calling the <code>getApplication()</code> method of
 * {@link ApplicationFactory}. Because this instance is shared, it must be implemented in a thread-safe manner.
 * </p>
 * 
 * Holds webapp-wide resources for a JSF application. There is a single one of these for a web application, accessable
 * via
 * 
 * <pre>
 * FacesContext.getCurrentInstance().getApplication()
 * </pre>
 * 
 * In particular, this provides a factory for UIComponent objects. It also provides convenience methods for creating
 * ValueBinding objects.
 * 
 * See Javadoc of <a href="http://java.sun.com/javaee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 */
@SuppressWarnings("deprecation")
public abstract class Application
{

    /**
     * Retrieve the current Myfaces Application Instance, lookup
     * on the application map. All methods introduced on jsf 1.2
     * for Application interface should thrown by default
     * UnsupportedOperationException, but the ri scan and find the
     * original Application impl, and redirect the call to that
     * method instead throwing it, allowing application implementations
     * created before jsf 1.2 continue working.   
     * 
     * Note: every method, which uses getMyfacesApplicationInstance() to
     *       delegate itself to the current ApplicationImpl MUST be
     *       overriden by the current ApplicationImpl to prevent infinite loops. 
     */
    private Application getMyfacesApplicationInstance()
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null)
        {
            ExternalContext externalContext = facesContext.getExternalContext();
            if (externalContext != null)
            {
                return (Application) externalContext.getApplicationMap().get(
                                "org.apache.myfaces.application.ApplicationImpl");
            }
        }
        return null;
    }

    private Application getMyfacesApplicationInstance(FacesContext facesContext)
    {
        if (facesContext != null)
        {
            ExternalContext externalContext = facesContext.getExternalContext();
            if (externalContext != null)
            {
                return (Application) externalContext.getApplicationMap().get(
                                "org.apache.myfaces.application.ApplicationImpl");
            }
        }
        return null;
    }

    // The concrete methods throwing UnsupportedOperationExceptiom were added for JSF 1.2.
    // They supply default to allows old Application implementations to still work.

    /**
     * @since 2.0
     * 
     * FIXME: Notify EG, this should not be abstract and throw UnsupportedOperationException
     * 
     * @param behaviorId
     * @param behaviorClass 
     */
    public void addBehavior(String behaviorId, String behaviorClass)
    {
        Application application = getMyfacesApplicationInstance();
        if (application != null)
        {
            application.addBehavior(behaviorId, behaviorClass);
            return;
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Define a new mapping from a logical "component type" to an actual java class name. This controls what type is
     * created when method createComponent of this class is called.
     * <p>
     * Param componentClass must be the fully-qualified class name of some class extending the UIComponent class. The
     * class must have a default constructor, as instances of it will be created using Class.newInstance.
     * <p>
     * It is permitted to override a previously defined mapping, ie to call this method multiple times with the same
     * componentType string. The createComponent method will simply use the last defined mapping.
     */
    /**
     * Register a new mapping of component type to the name of the corresponding {@link UIComponent} class. This allows
     * subsequent calls to <code>createComponent()</code> to serve as a factory for {@link UIComponent} instances.
     * 
     * @param componentType
     *            - The component type to be registered
     * @param componentClass
     *            - The fully qualified class name of the corresponding {@link UIComponent} implementation
     * 
     * @throws NullPointerException
     *             if <code>componentType</code> or <code>componentClass</code> is <code>null</code>
     */
    public abstract void addComponent(String componentType, String componentClass);

    /**
     * Register a new converter class that is capable of performing conversions for the specified target class.
     * 
     * @param targetClass
     *            - The class for which this converter is registered
     * @param converterClass
     *            - The fully qualified class name of the corresponding {@link Converter} implementation
     * 
     * @throws NullPointerException
     *             if <code>targetClass</code> or <code>converterClass</code> is <code>null</code>
     */
    public abstract void addConverter(Class<?> targetClass, String converterClass);

    /**
     * Register a new mapping of converter id to the name of the corresponding {@link Converter} class. This allows
     * subsequent calls to createConverter() to serve as a factory for {@link Converter} instances.
     * 
     * @param converterId
     *            - The converterId to be registered
     * @param converterClass
     *            - The fully qualified class name of the corresponding {@link Converter} implementation
     * 
     * @throws NullPointerException
     *             if <code>componentType</code> or <code>componentClass</code> is <code>null</code>
     */
    public abstract void addConverter(String converterId, String converterClass);

    /**
     * 
     * @since 2.0
     * @param validatorId
     */
    public void addDefaultValidatorId(String validatorId)
    {
    }

    /**
     * <p>
     * Provide a way for Faces applications to register an <code>ELContextListener</code> that will be notified on
     * creation of <code>ELContext</code> instances.
     * <p>
     * 
     * <p>
     * An implementation is provided that throws <code>UnsupportedOperationException</code> so that users that decorate
     * the <code>Application</code> continue to work.
     * </p>
     * 
     * @since 1.2
     */
    public void addELContextListener(ELContextListener listener)
    {
        Application application = getMyfacesApplicationInstance();
        if (application != null)
        {
            application.addELContextListener(listener);
            return;
        }
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Cause an the argument <code>resolver</code> to be added to the resolver chain as specified in section 5.5.1 of
     * the JavaServer Faces Specification.
     * </p>
     * 
     * <p>
     * It is not possible to remove an <code>ELResolver</code> registered with this method, once it has been registered.
     * </p>
     * 
     * <p>
     * It is illegal to register an ELResolver after the application has received any requests from the client. If an
     * attempt is made to register a listener after that time, an IllegalStateException must be thrown. This restriction
     * is in place to allow the JSP container to optimize for the common case where no additional
     * <code>ELResolvers</code> are in the chain, aside from the standard ones. It is permissible to add
     * <code>ELResolvers</code> before or after initialization to a CompositeELResolver that is already in the chain.
     * <p>
     * 
     * <p>
     * The default implementation throws <code>UnsupportedOperationException</code> and is provided for the sole purpose
     * of not breaking existing applications that extend {@link Application}.
     * </p>
     * 
     * @since 1.2
     */
    public void addELResolver(ELResolver resolver)
    {
        // The following concrete methods were added for JSF 1.2.  They supply default 
        // implementations that throw UnsupportedOperationException.  
        // This allows old Application implementations to still work.
        Application application = getMyfacesApplicationInstance();
        if (application != null)
        {
            application.addELResolver(resolver);
            return;
        }
        throw new UnsupportedOperationException();
    }

    /**
     *Register a new mapping of validator id to the name of the corresponding <code>Validator</code> class. This allows
     * subsequent calls to <code>createValidator()</code> to serve as a factory for <code>Validator</code> instances.
     * 
     *@param validatorId  The validator id to be registered
     *@param validatorClass The fully qualified class name of the corresponding Validator implementation
     * 
     *@throws NullPointerException
     *             if <code>validatorId</code> or <code>validatorClass</code> is <code>null</code>
     */
    public abstract void addValidator(String validatorId, String validatorClass);

    /**
     * 
     * @param behaviorId
     * @return
     * @throws FacesException
     * @since 2.0
     * 
     * FIXME: Notify EG, this should not be abstract and throw UnsupportedOperationException
     */
    public Behavior createBehavior(String behaviorId) throws FacesException
    {
        Application application = getMyfacesApplicationInstance();
        if (application != null)
        {
            return application.createBehavior(behaviorId);
        }
        throw new UnsupportedOperationException();
    }

    /**
     * ???
     * 
     * @param context
     * @param componentResource
     * @return
     * 
     * @since 2.0
     */
    public UIComponent createComponent(FacesContext context, Resource componentResource)
    {
        Application application = getMyfacesApplicationInstance(context);
        if (application != null)
        {
            return application.createComponent(context, componentResource);
        }
        throw new UnsupportedOperationException();
    }

    /**
     * 
     * @param context
     * @param componentType
     * @param rendererType
     * @return
     * 
     * @since 2.0
     */
    public UIComponent createComponent(FacesContext context, String componentType, String rendererType)
    {
        Application application = getMyfacesApplicationInstance(context);
        if (application != null)
        {
            return application.createComponent(context, componentType, rendererType);
        }
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Create a new UIComponent subclass, using the mappings defined by previous calls to the addComponent method of
     * this class.
     * </p>
     * 
     * @throws FacesException
     *             if there is no mapping defined for the specified componentType, or if an instance of the specified
     *             type could not be created for any reason.
     */
    public abstract UIComponent createComponent(String componentType) throws FacesException;

    /**
     * <p>
     * Create an object which has an associating "binding" expression tying the component to a user property.
     * </p>
     * 
     * <p>
     * First the specified value-binding is evaluated; if it returns a non-null value then the component
     * "already exists" and so the resulting value is simply returned.
     * </p>
     * 
     * <p>
     * Otherwise a new UIComponent instance is created using the specified componentType, and the new object stored via
     * the provided value-binding before being returned.
     * </p>
     * 
     * @deprecated
     */
    @Deprecated
    public abstract UIComponent createComponent(ValueBinding componentBinding, FacesContext context,
                    String componentType) throws FacesException;

    /**
     * <p>
     * Call the <code>getValue()</code> method on the specified <code>ValueExpression</code>. If it returns a
     * <code>{@link UIComponent}</code> instance, return it as the value of this method. If it does not, instantiate a
     * new <code>{@link UIComponent}</code> instance of the specified component type, pass the new component to the
     * <code>setValue()</code> method of the specified <code>ValueExpression</code>, and return it.
     * </p>
     * 
     * @param componentExpression
     *            - <code>ValueExpression</code> representing a component value expression (typically specified by the
     *            <code>component</code> attribute of a custom tag)
     * @param context
     *            - {@link FacesContext} for the current request
     * @param componentType
     *            - Component type to create if the ValueExpression does not return a component instance
     * 
     * @throws FacesException
     *             if a <code>{@link UIComponent}</code> cannot be created
     * @throws NullPointerException
     *             if any parameter is null
     *             <p>
     *             A default implementation is provided that throws <code>UnsupportedOperationException</code> so that
     *             users that decorate <code>Application</code> can continue to function
     *             </p>
     * 
     * @since 1.2
     */
    public UIComponent createComponent(ValueExpression componentExpression, FacesContext context, String componentType)
                    throws FacesException
    {
        Application application = getMyfacesApplicationInstance(context);
        if (application != null)
        {
            return application.createComponent(componentExpression, context, componentType);
        }
        throw new UnsupportedOperationException();
    }

    /**
     * 
     * @param componentExpression
     * @param context
     * @param componentType
     * @param rendererType
     * @return
     * 
     * @since 2.0
     */
    public UIComponent createComponent(ValueExpression componentExpression, FacesContext context, String componentType,
                    String rendererType)
    {
        Application application = getMyfacesApplicationInstance(context);
        if (application != null)
        {
            return application.createComponent(componentExpression, context, componentType, rendererType);
        }
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Instantiate and return a new <code>{@link Converter}</code> instance of the class that has registered itself as
     * capable of performing conversions for objects of the specified type. If no such <code>{@link Converter}</code>
     * class can be identified, return null.
     * </p>
     * 
     * <p>
     * To locate an appropriate <code>{@link Converter}</code> class, the following algorithm is performed, stopping as
     * soon as an appropriate <code>{@link Converter}</code> class is found: Locate a <code>{@link Converter}</code>
     * registered for the target class itself. Locate a <code>{@link Converter}</code> registered for interfaces that
     * are implemented by the target class (directly or indirectly). Locate a <code>{@link Converter}</code> registered
     * for the superclass (if any) of the target class, recursively working up the inheritance hierarchy.
     * </p>
     * 
     * <p>
     * If the <code>{@link Converter}</code> has a single argument constructor that accepts a Class, instantiate the
     * <code>{@link Converter}</code> using that constructor, passing the argument <code>targetClass</code> as
     * the sole argument. Otherwise, simply use the zero-argument constructor.
     * 
     * @param targetClass
     *            - Target class for which to return a <code>{@link Converter}</code>
     * 
     * @throws FacesException
     *             if the <code>{@link Converter}</code> cannot be created
     * @throws NullPointerException
     *             if <code>targetClass</code> is <code>null</code>
     * 
     */
    public abstract Converter createConverter(Class<?> targetClass);

    /**
     * Instantiate and return a new <code>{@link Converter}</code> instance of the class specified by a previous call to
     * <code>addConverter()</code> for the specified converter id. If there is no such registration for this converter
     * id, return <code>null</code>.
     * 
     * @param converterId
     *            - The converter id for which to create and return a new <code>{@link Converter}</code> instance
     * 
     * @throws FacesException
     *             if the <code>{@link Converter}</code> cannot be created
     * @throws NullPointerException
     *             if converterId is <code>null</code>
     */
    public abstract Converter createConverter(String converterId);

    /**
     * Create an object which can be used to invoke an arbitrary method via an EL expression at a later time. This is
     * similar to createValueBinding except that it can invoke an arbitrary method (with parameters) rather than just
     * get/set a javabean property.
     * <p>
     * This is used to invoke ActionListener method, and ValueChangeListener methods.
     * 
     * @deprecated
     */
    @Deprecated
    public abstract MethodBinding createMethodBinding(String ref, Class<?>[] params) throws ReferenceSyntaxException;

    /**
     * Instantiate and return a new <code>{@link Validator}</code> instance of the class specified by a previous call to
     * <code>addValidator()</code> for the specified validator id.
     * 
     * @param validatorId The <code>{@link Validator}</code> id for which to create and return a new
     *        Validator instance
     * 
     * @throws FacesException
     *             if a <code>{@link Validator}</code> of the specified id cannot be created
     * @throws NullPointerException
     *             if validatorId is <code>null</code>
     */
    public abstract Validator createValidator(String validatorId) throws FacesException;

    /**
     * <p>
     * Create an object which can be used to invoke an arbitrary method via an EL expression at a later time. This is
     * similar to createValueBinding except that it can invoke an arbitrary method (with parameters) rather than just
     * get/set a javabean property.
     * </p>
     * This is used to invoke ActionListener method, and ValueChangeListener methods.
     * 
     * @deprecated
     */
    @Deprecated
    public abstract ValueBinding createValueBinding(String ref) throws ReferenceSyntaxException;

    /**
     * <p>
     * Get a value by evaluating an expression.
     * </p>
     * 
     * <p>
     * Call <code>{@link #getExpressionFactory()}</code> then call
     * <code>ExpressionFactory.createValueExpression(jakarta.el.ELContext, java.lang.String, java.lang.Class)</code>
     * passing the argument <code>expression</code> and <code>expectedType</code>. Call
     * <code>{@link FacesContext#getELContext()}</code> and pass it to
     * <code>ValueExpression.getValue(jakarta.el.ELContext)</code>, returning the result.
     * </p>
     * 
     * <p>
     * An implementation is provided that throws <code>UnsupportedOperationException</code> so that users that decorate
     * the <code>Application</code> continue to work.
     * <p>
     * 
     * @throws jakarta.el.ELException
     */
    public <T> T evaluateExpressionGet(FacesContext context, String expression, Class<? extends T> expectedType)
                    throws ELException
    {
        Application application = getMyfacesApplicationInstance(context);
        if (application != null)
        {
            return application.evaluateExpressionGet(context, expression, expectedType);
        }
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Return the default <code>ActionListener</code> to be registered for all <code>ActionSource</code> components 
     * in this appication. If not explicitly set, a default implementation must be provided that performs the 
     * following functions:
     * </p>
     * <ul>
     * <li>The <code>processAction()</code> method must first call <code>FacesContext.renderResponse()</code>in order to
     * bypass any intervening lifecycle phases, once the method returns.</li>
     * 
     * <li>The <code>processAction()</code> method must next determine the logical 
     * outcome of this event, as follows:</li>
     * 
     * <li>If the originating component has a non-<code>null action</code> property, retrieve the <code>
     *             MethodBinding</code> from the property, and call <code>invoke()</code>
     * on it. Convert the returned value (if any) to a String, and use it as the logical outcome.</li>
     * <li>Otherwise, the logical outcome is null.</li>
     * <li>The <code>processAction()</code> method must finally retrieve the <code>NavigationHandler</code> instance 
     *         for this application and call
     *         code>NavigationHandler.handleNavigation(jakarta.faces.context.FacesContext,
     *                                     java.lang.String, java.lang.String)</code> passing:</li>
     * <li>the {@link FacesContext} for the current request</li>
     * <li>If there is a <code>MethodBinding</code> instance for the <code>action</code> property of this component, the
     * result of calling {@link MethodBinding#getExpressionString()} on it, null otherwise</li>
     * <li>the logical outcome as determined above</li>
     * </ul>
     * <p>
     * Note that the specification for the default <code>ActionListener</code> contiues to call for the use of a
     * deprecated property (<code>action</code>) and class (<code>MethodBinding</code>). Unfortunately, this is
     * necessary because the default ActionListener must continue to work with components that do not implement
     * {@link jakarta.faces.component.ActionSource2}, and only implement {@link jakarta.faces.component.ActionSource}.
     */
    public abstract ActionListener getActionListener();

    /**
     * 
     * @return
     * 
     * @since 2.0
     * 
     * FIXME: Notify EG, this should not be abstract and throw UnsupportedOperationException
     */
    public Iterator<String> getBehaviorIds()
    {
        Application application = getMyfacesApplicationInstance();
        if (application != null)
        {
            return application.getBehaviorIds();
        }
        // It is better to return an empty iterator,
        // to keep compatiblity with previous jsf 2.0 Application
        // instances
        return Collections.EMPTY_LIST.iterator();
    }

    /**
     * Return an <code>Iterator</code> over the set of currently defined component types for this
     * <code>Application</code>.
     */
    public abstract Iterator<String> getComponentTypes();

    /**
     * Return an <code>Iterator</code> over the set of currently registered converter ids for this
     * <code>Application</code>
     * 
     * @return
     */
    public abstract Iterator<String> getConverterIds();

    /**
     *Return an <code>Iterator</code> over the set of <code>Class</code> instances for which <code>{@link Converter}
     * </code> <code>classes</code>have been explicitly registered.
     * 
     * @return
     */
    public abstract Iterator<Class<?>> getConverterTypes();

    /**
     *Return the default <code>Locale</code> for this application. If not explicitly set, <code>null</code> is
     * returned.
     * 
     * @return
     */
    public abstract Locale getDefaultLocale();

    /**
     * Return the <code>renderKitId</code> to be used for rendering this application. If not explicitly set,
     * <code>null</code> is returned.
     * 
     * @return
     */
    public abstract String getDefaultRenderKitId();

    /**
     * 
     * @return
     * 
     * @since 2.0
     */
    public Map<String, String> getDefaultValidatorInfo()
    {
        Application application = getMyfacesApplicationInstance();
        if (application != null)
        {
            return application.getDefaultValidatorInfo();
        }
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * If no calls have been made to <code>addELContextListener(jakarta.el.ELContextListener)</code>, this method must
     * return an empty array
     * <p>
     * .
     * 
     * <p>
     * Otherwise, return an array representing the list of listeners added by calls to
     * <code>addELContextListener(jakarta.el.ELContextListener)</code>.
     * <p>
     * 
     * <p>
     * An <code>implementation</code> is provided that throws UnsupportedOperationException so that users that decorate
     * the <code>Application</code> continue to work.
     * </p>
     * 
     * @since 1.2
     */
    public ELContextListener[] getELContextListeners()
    {
        Application application = getMyfacesApplicationInstance();
        if (application != null)
        {
            return application.getELContextListeners();
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Return the singleton <code>ELResolver</code> instance to be used for all EL resolution. This is actually an
     * instance of <code>CompositeELResolver</code> that must contain the following ELResolver instances in the
     * following order:
     * <ul>
     * <li><code>ELResolver</code> instances declared using the &lt;el-resolver&gt; element in the application 
     * configuration resources.</li>
     * 
     * <li>An <code> implementation</code> that wraps the head of the legacy VariableResolver chain, as per section
     * <code> VariableResolver ChainWrapper</code> in Chapter 5 in the spec document.</li>
     * 
     * <li>An <code>implementation</code> that wraps the head of the legacy PropertyResolver chain, as per section
     * <code>PropertyResolver ChainWrapper</code> in Chapter 5 in the spec document.</li>
     * 
     * <li>Any <code>ELResolver</code> instances added by calls to
     * <code>{@link #addELResolver(jakarta.el.ELResolver)}</code>.</li>
     * 
     * <li>The default implementation throws <code>UnsupportedOperationException</code> and is provided for the sole
     * purpose of not breaking existing applications that extend <code>{@link Application}</code>.</li>
     * </ul>
     * 
     * @since 1.2
     */
    public ELResolver getELResolver()
    {
        Application application = getMyfacesApplicationInstance();
        if (application != null)
        {
            return application.getELResolver();
        }
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Return the <code>ExpressionFactory</code> instance for this application. This instance is used by the convenience
     * method <code>{@link #evaluateExpressionGet(FacesContext, java.lang.String, java.lang.Class)}.
     * </code>
     * </p>
     * 
     * <p>
     * The implementation must return the <code>ExpressionFactory</code> from the JSP container by calling <code>
     * JspFactory.getDefaultFactory().getJspApplicationContext(servletContext).getExpressionFactory()</code>.
     * </p>
     * 
     * <p>
     * An implementation is provided that throws <code>UnsupportedOperationException</code> so that users that decorate
     * the <code>Application</code> continue to work.
     * </p>
     * 
     * @since 1.2
     * @return 
     */
    public ExpressionFactory getExpressionFactory()
    {
        Application application = getMyfacesApplicationInstance();
        if (application != null)
        {
            return application.getExpressionFactory();
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Return the fully qualified class name of the <code>ResourceBundle</code> to be used for JavaServer Faces messages
     * for this application. If not explicitly set, <code>null</code> is returned.
     */
    public abstract String getMessageBundle();

    /**
     *Return the <code>{@link NavigationHandler}</code> instance that will be passed the outcome returned by any
     * invoked application action for this web application. If not explicitly set, a default implementation must be
     * provided that performs the functions described in the <code>{@link NavigationHandler}</code> class description.
     */
    public abstract NavigationHandler getNavigationHandler();

    /**
     * <p>
     * Return the project stage for the currently running application instance. The default value is <code>
     * {@link ProjectStage#Production}</code>
     * </p>
     * 
     * <p>
     * The implementation of this method must perform the following algorithm or an equivalent with the same end result
     * to determine the value to return.
     * </p>
     * 
     * <ul>
     * <li>If the value has already been determined by a previous call to this method, simply return that value.</li>
     * <li>Look for a <code>JNDI</code> environment entry under the key given by the value of
     * <code>{@link ProjectStage#PROJECT_STAGE_JNDI_NAME}</code> (return type of java.lang.String). If found, continue
     * with the algorithm below, otherwise, look for an entry in the <code>initParamMap</code> of the
     * <code>ExternalContext</code> from the current <code>FacesContext</code> with the key
     * <code>{@link ProjectStage#PROJECT_STAGE_PARAM_NAME}</code></li>
     * <li>If a value is found found, see if an enum constant can be obtained by calling
     * <code>ProjectStage.valueOf()</code>, passing the value from the <code>initParamMap</code>. If this succeeds
     * without exception, save the value and return it.</li>
     * <li>If not found, or any of the previous attempts to discover the enum constant value have failed, log a
     * descriptive error message, assign the value as <code>ProjectStage.Production</code> and return it.</li>
     * </ul>
     * 
     * @since 2.0
     */
    public ProjectStage getProjectStage()
    {
        Application application = getMyfacesApplicationInstance();
        if (application != null)
        {
            return application.getProjectStage();
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Get the object used by the VariableResolver to read and write named properties on java beans, Arrays, Lists and
     * Maps. This object is used by the ValueBinding implementation, and during the process of configuring
     * "managed bean" properties.
     * 
     * @deprecated
     */
    @Deprecated
    public abstract PropertyResolver getPropertyResolver();

    /**
     * <p>
     * Find a <code>ResourceBundle</code> as defined in the application configuration resources under the specified
     * name. If a <code>ResourceBundle</code> was defined for the name, return an instance that uses the locale of the
     * current <code>{@link jakarta.faces.component.UIViewRoot}</code>.
     * </p>
     * 
     * <p>
     * The default implementation throws <code>UnsupportedOperationException</code> and is provided for the sole purpose
     * of not breaking existing applications that extend this class.
     * </p>
     * 
     * @return <code>ResourceBundle</code> for the current UIViewRoot, otherwise null
     * 
     * @throws FacesException
     *             if a bundle was defined, but not resolvable
     * @throws NullPointerException
     *             if ctx == null || name == null
     */
    public ResourceBundle getResourceBundle(FacesContext ctx, String name)
    {
        Application application = getMyfacesApplicationInstance(ctx);
        if (application != null)
        {
            return application.getResourceBundle(ctx, name);
        }
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Return the singleton, stateless, thread-safe <code>{@link ResourceHandler}</code> for this application. The JSF
     * implementation must support the following techniques for declaring an alternate implementation of <code>
     * ResourceHandler</code>.
     * </p>
     * 
     * <ul>
     * <li>The <code>ResourceHandler</code> implementation is declared in the application configuration resources by
     * giving the fully qualified class name as the value of the <code>&lt;resource-handler&gt;</code> element 
     * within the
     * <code>application</code> element.</li>
     * <li>RELEASE_PENDING(edburns) It can also be declared via an annotation as 
     * specified in [287-ConfigAnnotations].</li>
     * </ul>
     * 
     * <p>
     * In all of the above cases, the runtime must employ the decorator pattern as for every other pluggable artifact in
     * JSF.
     * </p>
     * 
     * @since 2.0
     */
    public ResourceHandler getResourceHandler()
    {
        Application application = getMyfacesApplicationInstance();
        if (application != null)
        {
            return application.getResourceHandler();
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Return the <code>StateManager</code> instance that will be utilized during the Restore View and Render Response
     * phases of the request processing lifecycle. If not explicitly set, a default implementation must be provided that
     * performs the functions described in the <code>StateManager</code> description in the JavaServer Faces
     * Specification.
     */
    public abstract StateManager getStateManager();

    /**
     * Return an <code>Iterator</code> over the supported <code>Locales</code> for this appication.
     */
    public abstract Iterator<Locale> getSupportedLocales();

    /**
     *Return an <code>Iterator</code> over the set of currently registered validator ids for this
     * <code>Application</code>.
     */
    public abstract Iterator<String> getValidatorIds();

    /**
     * Get the object used to resolve expressions of form "#{...}".
     * 
     * @deprecated
     */
    @Deprecated
    public abstract VariableResolver getVariableResolver();

    /**
     * Set the <code>{@link ViewHandler}</code> instance that will be utilized during the
     * <code> Restore View and Render Response</code> phases of the request processing lifecycle.
     * 
     * @return
     */
    public abstract ViewHandler getViewHandler();

    /**
     * 
     * @param facesContext
     * @param systemEventClass
     * @param sourceBaseType
     * @param source
     * 
     * @since 2.0
     */
    public void publishEvent(FacesContext facesContext, Class<? extends SystemEvent> systemEventClass,
                    Class<?> sourceBaseType, Object source)
    {
        Application application = getMyfacesApplicationInstance(facesContext);
        if (application != null)
        {
            application.publishEvent(facesContext, systemEventClass, sourceBaseType, source);
            return;
        }
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * If there are one or more listeners for events of the type represented by <code>systemEventClass</code>, call
     * those listeners,passing source as the <code>source</code> of the event. The implementation should be as fast as
     * possible in determining whether or not a listener for the given <code>systemEventClass</code> and
     * <code>source</code> has been installed, and should return immediately once such a determination has been made.
     * The implementation of <code>publishEvent</code> must honor the requirements stated in
     * <code>{@link #subscribeToEvent(java.lang.Class, java.lang.Class,
     *                                               SystemEventListener)}</code>
     * <p>
     * <p>
     * The default implementation must implement an algorithm semantically equivalent to the following to locate
     * listener instances and to invoke them.
     * <p>
     * <ul>
     * <li>If the <code>source</code> argument implements
     * <code>{@link jakarta.faces.event.SystemEventListenerHolder}</code>, call
     * <code>{@linkjakarta.faces.event.SystemEventListenerHolder#getListenersForEventClass(java.lang.Class)}</code>
     * on it, passing the
     * <code>systemEventClass</code> argument. If the list is not empty, perform algorithm
     * <code>traverseListenerList</code> on the list.</li>
     * 
     * <li>If any <code>Application</code> level listeners have been installed by previous calls to <code>{@link
     * #subscribeToEvent(java.lang.Class, java.lang.Class, SystemEventListener)}</code>, perform algorithm
     * <code>traverseListenerList</code> on the list.</li>
     * 
     * <li>If any <code>Application</code> level listeners have been installed by previous calls to
     * <code>{@link #subscribeToEvent(java.lang.Class, SystemEventListener)}</code>, perform algorithm
     * <code>traverseListenerList</code> on the list.</li>
     * </ul>
     * 
     * <p>
     * If the act of invoking the <code>processListener</code> method causes an
     * <code>{@link jakarta.faces.event.AbortProcessingException}</code> to be thrown,
     * processing of the listeners must be aborted.
     * </p>
     * 
     * <p>
     * Algorithm <code>traverseListenerList</code>: For each listener in the list,
     * </p>
     * 
     * <ul>
     * <li>Call
     * <code>{@link SystemEventListener#isListenerForSource(java.lang.Object)}</code>, passing the <code>source</code>
     * argument. If this returns <code>false</code>, take no action on the listener.</li>
     * 
     * <li>Otherwise, if the event to be passed to the listener instances has not yet been constructed, construct the
     * event, passing <code>source</code> as the argument to the one-argument constructor that takes an
     * <code>Object</code>. This same event instance must be passed to all listener instances.</li>
     * 
     * <li>Call
     * <code>{@link SystemEvent#isAppropriateListener(jakarta.faces.event.FacesListener)}</code>, passing the listener
     *         instance as the argument. If this returns <code>false</code>, take no action on the listener.</li>
     * 
     * <li>Call <code>{@link SystemEvent#processListener(jakarta.faces.event.FacesListener)}</code>,
     * passing the listener instance.</li>
     * </ul>
     * 
     * @param systemEventClass
     *            - The Class of event that is being published. Must be non-null.
     * 
     * @param source
     *            - The <code>source</code> for the event of type systemEventClass. Must be non- <code>null</code>, and
     *            must implement <code>{@link jakarta.faces.event.SystemEventListenerHolder}</code>.
     * 
     * @since 2.0
     */
    public void publishEvent(FacesContext facesContext, Class<? extends SystemEvent> systemEventClass, Object source)
    {
        Application application = getMyfacesApplicationInstance(facesContext);
        if (application != null)
        {
            application.publishEvent(facesContext, systemEventClass, source);
            return;
        }
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Remove the argument <code>listener</code> from the list of <code>ELContextListeners</code>. If <code>listener
     * </code> is null, no exception is thrown and no action is performed. If <code>listener</code> is not in the list,
     * no exception is thrown and no action is performed.
     * <p>
     * 
     * <p>
     * An implementation is provided that throws <code>UnsupportedOperationException</code> so that users that decorate
     * the <code>Application</code> continue to work.
     * 
     * @param listener
     */
    public void removeELContextListener(ELContextListener listener)
    {
        Application application = getMyfacesApplicationInstance();
        if (application != null)
        {
            application.removeELContextListener(listener);
            return;
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Set the default <code>{@link ActionListener}</code> to be registered for all
     * <code>{@link jakarta.faces.component.ActionSource}</code>
     * components.
     * 
     * @param listener
     *            - The new default <code>{@link ActionListener}</code>
     * 
     * @throws NullPointerException
     *             if listener is null
     */
    public abstract void setActionListener(ActionListener listener);

    /**
     * Set the default <code>Locale</code> for this application.
     * 
     * @param locale
     *            - The new default <code>Locale</code>
     * 
     * @throws NullPointerException
     *             if listener is null
     */
    public abstract void setDefaultLocale(Locale locale);

    /**
     * Return the <code>renderKitId</code> to be used for rendering this application. If not explicitly set, <code>null
     * </code> is returned.
     * 
     * @param renderKitId
     */
    public abstract void setDefaultRenderKitId(String renderKitId);

    /**
     * Set the fully qualified class name of the <code>ResourceBundle </code> to be used for JavaServer Faces messages
     * for this application. See the JavaDocs for the <code>java.util.ResourceBundle </code> class for more information
     * about the syntax for resource bundle names.
     * 
     * @param bundle
     *            - Base name of the resource bundle to be used
     * 
     * @throws NullPointerException
     *             if bundle is null
     */
    public abstract void setMessageBundle(String bundle);

    /**
     * Set the {@link NavigationHandler} instance that will be passed the outcome returned by any invoked application
     * action for this web application.
     * 
     * @param handler
     *            - The new NavigationHandler instance
     */
    public abstract void setNavigationHandler(NavigationHandler handler);

    /**
     * The recommended way to affect the execution of the EL is to provide an &lt;el-resolver&gt; element at the right 
     * place in the application configuration resources which will be considered in the normal course of expression
     * evaluation. This method now will cause the argument resolver to be wrapped inside an implementation of ELResolver
     * and exposed to the EL resolution system as if the user had called addELResolver(jakarta.el.ELResolver).
     * 
     * @deprecated
     */
    @Deprecated
    public abstract void setPropertyResolver(PropertyResolver resolver);

    /**
     * 
     * @param resourceHandler
     * 
     * @since 2.0
     */
    public void setResourceHandler(ResourceHandler resourceHandler)
    {
        Application application = getMyfacesApplicationInstance();
        if (application != null)
        {
            application.setResourceHandler(resourceHandler);
            return;
        }
        throw new UnsupportedOperationException();
    }

    /**
     *Set the {@link StateManager} instance that will be utilized during the <code>Restore View and Render Response
     * </code> phases of the request processing lifecycle.
     * 
     * @param manager The new {@link StateManager}instance
     * 
     * @throws IllegalStateException
     *             if this method is called after at least one request has been processed by the <code>Lifecycle</code>
     *             instance for this application.
     * @throws NullPointerException
     *             if manager is <code>null</code>
     */
    public abstract void setStateManager(StateManager manager);

    /**
     * Set the <code>Locale</code> instances representing the supported <code>Locales</code> for this application.
     * 
     * @param locales The set of supported <code>Locales</code> for this application
     * 
     * @throws NullPointerException
     *             if the argument newLocales is <code>null</code>.
     * 
     */
    public abstract void setSupportedLocales(Collection<Locale> locales);

    /**
     * The recommended way to affect the execution of the EL is to provide an &lt;el-resolver&gt; element at the right 
     * place in the application configuration resources which will be considered in the normal course of expression
     * evaluation. This method now will cause the argument resolver to be wrapped inside an implementation of ELResolver
     * and exposed to the EL resolution system as if the user had called addELResolver(jakarta.el.ELResolver).
     * 
     * @deprecated
     */
    @Deprecated
    public abstract void setVariableResolver(VariableResolver resolver);

    /**
     * Set the {@link ViewHandler} instance that will be utilized during the <code>Restore View and Render Response
     * </code> phases of the request processing lifecycle.
     * 
     * @param handler
     *            - The new {@link ViewHandler} instance
     * 
     * @throws IllegalStateException
     *             if this method is called after at least one request has been processed by the <code>Lifecycle</code>
     *             instance for this application.
     * @throws NullPointerException
     *             if <code>handler</code> is <code>null</code>
     */
    public abstract void setViewHandler(ViewHandler handler);

    /**
     * 
     * @param systemEventClass
     * @param sourceClass
     * @param listener
     * 
     * @since 2.0
     */
    public void subscribeToEvent(Class<? extends SystemEvent> systemEventClass, Class<?> sourceClass,
                    SystemEventListener listener)
    {
        Application application = getMyfacesApplicationInstance();
        if (application != null)
        {
            application.subscribeToEvent(systemEventClass, sourceClass, listener);
            return;
        }
        throw new UnsupportedOperationException();
    }

    /**
     * 
     * @param systemEventClass
     * @param listener
     * 
     * @since 2.0
     */
    public void subscribeToEvent(Class<? extends SystemEvent> systemEventClass, SystemEventListener listener)
    {
        Application application = getMyfacesApplicationInstance();
        if (application != null)
        {
            application.subscribeToEvent(systemEventClass, listener);
            return;
        }
        subscribeToEvent(systemEventClass, null, listener);
    }

    /**
     * 
     * @param systemEventClass
     * @param sourceClass
     * @param listener
     * 
     * @since 2.0
     */
    public void unsubscribeFromEvent(Class<? extends SystemEvent> systemEventClass, Class<?> sourceClass,
                    SystemEventListener listener)
    {
        Application application = getMyfacesApplicationInstance();
        if (application != null)
        {
            application.unsubscribeFromEvent(systemEventClass, sourceClass, listener);
            return;
        }
        throw new UnsupportedOperationException();
    }

    /**
     * 
     * @param systemEventClass
     * @param listener
     * 
     * @since 2.0
     */
    public void unsubscribeFromEvent(Class<? extends SystemEvent> systemEventClass, SystemEventListener listener)
    {
        Application application = getMyfacesApplicationInstance();
        if (application != null)
        {
            application.unsubscribeFromEvent(systemEventClass, listener);
            return;
        }
        unsubscribeFromEvent(systemEventClass, null, listener);
    }
    
    /**
     * @since 2.2
     * @return 
     */
    public FlowHandler getFlowHandler()
    {
        Application application = getMyfacesApplicationInstance();
        if (application != null)
        {
            return application.getFlowHandler();
        }
        throw new UnsupportedOperationException();
    }
    
    /**
     * @since 2.2
     * @param flowHandler 
     */
    public void setFlowHandler(FlowHandler flowHandler)
    {
        Application application = getMyfacesApplicationInstance();
        if (application != null)
        {
            application.setFlowHandler(flowHandler);
            return;
        }
        throw new UnsupportedOperationException();

    }
    
    public void addSearchKeywordResolver(SearchKeywordResolver resolver)
    {
        // The following concrete methods were added for JSF 1.2.  They supply default 
        // implementations that throw UnsupportedOperationException.  
        // This allows old Application implementations to still work.
        Application application = getMyfacesApplicationInstance();
        if (application != null)
        {
            application.addSearchKeywordResolver(resolver);
            return;
        }
        throw new UnsupportedOperationException();
    }
    
    public SearchKeywordResolver getSearchKeywordResolver()
    {
        Application application = getMyfacesApplicationInstance();
        if (application != null)
        {
            return application.getSearchKeywordResolver();
        }
        throw new UnsupportedOperationException();
    }
    
    /**
     * @since 2.3
     * @return 
     */
    public SearchExpressionHandler getSearchExpressionHandler()
    {
        Application application = getMyfacesApplicationInstance();
        if (application != null)
        {
            return application.getSearchExpressionHandler();
        }
        throw new UnsupportedOperationException();
    }
    
    /**
     * @since 2.3
     * @param searchExpressionHandler 
     */
    public void setSearchExpressionHandler(SearchExpressionHandler searchExpressionHandler)
    {
        Application application = getMyfacesApplicationInstance();
        if (application != null)
        {
            application.setSearchExpressionHandler(searchExpressionHandler);
            return;
        }
        throw new UnsupportedOperationException();
    }
}
