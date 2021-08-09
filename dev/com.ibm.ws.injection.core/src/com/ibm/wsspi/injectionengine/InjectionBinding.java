/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.injectionengine;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.IncidentStream;
import com.ibm.ws.injectionengine.InternalInjectionEngine;
import com.ibm.ws.injectionengine.ffdc.Formattable;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.wsspi.injectionengine.factory.InjectionObjectFactory;

/**
 * Represents Reference 'Binding' information, controlling what is to be bound
 * into the java:comp/env name space context. <p>
 */
public abstract class InjectionBinding<A extends Annotation> // d367834.11
implements Formattable
{
    private static final String CLASS_NAME = InjectionBinding.class.getName();
    private static final TraceComponent tc = Tr.register(InjectionBinding.class,
                                                         InjectionConfigConstants.traceString,
                                                         InjectionConfigConstants.messageFile);

    private InjectionProcessorContext ivContext; // F743-33811.1

    /**
     * The processor class associated with this binding. This field will only
     * be set for named injection bindings.
     */
    // d730349
    Class<?> ivProcessorClass;

    /**
     * JNDI name used to bind in the java:comp/env name space.
     */
    private String ivJndiName;

    /**
     * The java namespace scope if ivJndiName has a "java:" prefix.
     */
    private InjectionScope ivInjectionScope; // d660700 F46994

    /**
     * The suffix of ivJndiName after "java:scope/", or ivJndiName if ivJndiName
     * does not have a "java:" prefix.
     */
    private String ivJavaNameSpaceName; // d660700

    /**
     * Processed annotation for this injection binding.
     */
    private A ivAnnotation; // d367834.11

    /**
     * This is the <injection-target-class>. It needs to be set to contain the
     * most fine grained Class type. See the setter method for more information.
     */
    private Class<?> ivInjectionClassType;

    /**
     * This is the name of <injection-target-class>. This field is only used if
     * ivInjectionClassType is <tt>null</tt>, which can happen when the
     * configuration contains a null class loader.
     */
    private String ivInjectionClassTypeName; // F743-32443

    /**
     * ivInjectedObject is an optional field. It is used as a place holder for performance if the
     * injection object is known at preparation time and does not require to be computed by the
     * resolver. E.g. <env-entry>
     */
    private Object ivInjectedObject;

    /**
     * The object to bind into naming.
     */
    private Object ivBindingObject;

    /**
     * If {@link #setObjects(Object, Reference)} was called, this is {@link Reference#getFactoryClassName}.
     */
    private String ivObjectFactoryClassName; // F48603.4

    /**
     * If {@link #setObjects(Object, Reference)} or {@link #setReferenceObject} was called, this is the class corresponding to {@link Reference#getFactoryClassName}.
     */
    private Class<? extends ObjectFactory> ivObjectFactoryClass; // F48603.4

    /**
     * If {@link #ivBindingObject} is a Reference, this field is lazily
     * initialized from {@link #ivObjectFactoryClass}.
     */
    protected volatile ObjectFactory ivObjectFactory;

    /**
     * If {@link #ivObjectFactory} implements the injection extension interface,
     * then this field will be set as well as ivObjectFactory.
     *
     * <p>Access to this variable is synchronized using volatile-read/write of {@link #ivObjectFactory}. A write to this variable must be followed by
     * a write to ivObjectFactory, and a read from this variable must be
     * preceded by a read from ivObjectFactory.
     */
    // F49213.1
    private InjectionObjectFactory ivInjectionObjectFactory;

    /**
     * This list of injection bindings that have been added while processing this
     * binding. This list is lazily initialized when the first injection target
     * is added, and it is cleared again when metadata processing is complete.
     */
    List<InjectionTarget> ivInjectionTargets;

    protected J2EEName ivJ2eeName;

    /**
     * Available only until metedata processing is complete.
     */
    protected ComponentNameSpaceConfiguration ivNameSpaceConfig;

    /**
     * True if {@link InjectionProcessor#resolve} has been attempted for this
     * injection binding. Some injection bindings may attempt resolution but
     * not be {@link #isResolved} (notably, env-entry without a value).
     */
    // F87539
    boolean ivResolveAttempted;

    protected Map<String, InjectionBinding<?>> ivJavaColonCompEnvMap;

    /**
     * True if application has been configured for extra configuration checking.
     */
    // d745970
    protected boolean ivCheckAppConfig;

    /**
     * Internal method to allow caching of the declared methods that start with
     * 'set', for the classes being injected into from XML. <p>
     *
     * Includes static and non-static methods, since client container will need
     * static, and server will need non-static. <p>
     *
     * The number of parameters is not checked, since performance should be
     * better if the caller does that only if the method name matches. <p>
     *
     * @param clazz the Class for which to obtain the declared set methods.
     * @param isClient <tt>true</tt> if static methods should be selected.
     * @return the result of Clazz.getDeclaredMethods; returned from a cache
     *         if available.
     * @throws SecurityException if member access is not allowed.
     */
    // d648283
    private List<Method> getDeclaredSetMethods(Class<?> clazz, boolean isClient)
    {
        Map<Class<?>, List<Method>> localCache = ivContext.getDeclaredSetMethodCache(); // F743-33811.1
        List<Method> methods = localCache.get(clazz);
        if (methods == null)
        {
            methods = new ArrayList<Method>();
            Method[] declaredMethods = clazz.getDeclaredMethods();
            for (Method declared : declaredMethods)
            {
                int modifier = declared.getModifiers();
                if (Modifier.isStatic(modifier) == isClient && // F743-32443
                    declared.getName().startsWith("set"))
                {
                    methods.add(declared);
                }
            }
            localCache.put(clazz, methods);
        }
        return methods;
    }

    /**
     * Called when all metadata processing has completed, to allow InjectionBinding
     * instances to release any resources used during metadata processing that are
     * no longer needed once complete. From this point forward, the only methods
     * called will be to obtain the object to inject or other state data.
     *
     * If overridden, the override method should make a parent call, to insure
     * this abstract base class has the opportunity to release resources.
     */
    // d648283
    public void metadataProcessingComplete()
    {
        ivInjectionTargets = null; // F87539
        ivContext = null; // F743-33811.1
        ivNameSpaceConfig = null;
    }

    /**
     * Initializes all state cleared by {@link #metadataProcessingComplete}.
     */
    // d730349.1
    void metadataProcessingInitialize(ComponentNameSpaceConfiguration nameSpaceConfig)
    {
        ivContext = (InjectionProcessorContext) nameSpaceConfig.getInjectionProcessorContext();
        ivNameSpaceConfig = nameSpaceConfig;

        // Following must be available after ivContext and ivNameSpaceConfig are cleared
        ivCheckAppConfig = nameSpaceConfig.isCheckApplicationConfiguration();
    }

    /**
     * @return true if this binding is complete and should not be modified
     */
    // d730349
    public boolean isComplete()
    {
        return ivNameSpaceConfig == null;
    }

    /**
     *
     */
    public InjectionBinding(A annotation, ComponentNameSpaceConfiguration nameSpaceConfig)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "<init>: " + Util.identity(this));

        metadataProcessingInitialize(nameSpaceConfig); // d730349.1
        ivAnnotation = annotation;
        ivJ2eeName = nameSpaceConfig.getJ2EEName();
        ivJavaColonCompEnvMap = nameSpaceConfig.getJavaColonCompEnvMap();
        ivJndiName = null;
        ivInjectionClassType = null;
        ivInjectedObject = null;
        ivBindingObject = null;
    }

    private void addInjectionTarget(InjectionTarget target)
    {
        if (ivInjectionTargets == null)
        {
            ivInjectionTargets = new ArrayList<InjectionTarget>();
        }
        ivInjectionTargets.add(target);
    }

    /**
     * Adds an InjectionTarget for the specified xml target (field or method). <p>
     *
     * Duplicates will NOT be added to the list. Duplicates will be ignored.
     * This may occur when an injection is defined in both XML and
     * annotations, or annotations are defined in a common base class. <p>
     *
     * This method has no effect if the ClassLoader is null. <p>
     *
     * @param injectionType type of object to be injected
     * @param targetName injection java property name
     * @param targetClassName class that is the target of the injection
     * @throws InjectionException if a problem occurs while creating
     *             the instance to be injected
     */
    public void addInjectionTarget(Class<?> injectionType,
                                   String targetName,
                                   String targetClassName)
                    throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "addInjectionTarget : " + injectionType +
                         ", " + targetName + ", " + targetClassName);

        // F743-32443 - For federated client modules, we don't have a classloader,
        // so we need to defer target resolution until the client process is
        // running.  Save away the target names until they can be processed by
        // InjectionEngineImpl.processClientInjections.
        if (ivNameSpaceConfig.getOwningFlow() == ComponentNameSpaceConfiguration.ReferenceFlowKind.CLIENT &&
            ivNameSpaceConfig.getClassLoader() == null) // F743-32443
        {
            addInjectionTarget(new ClientInjectionTarget(targetClassName, targetName, this));

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "addInjectionTarget: added client injection target");
            return;
        }

        Field targetField = null;
        Method targetMethod = null;
        Class<?> injectionClass = loadClass(targetClassName); // d446474
        InjectionTarget injectionTarget = null;

        if (targetName != null &&
            targetClassName != null &&
            injectionClass != null)
        {
            // --------------------------------------------------------------------
            // First, try to locate the JavaBean property set method for the
            // specified target name. Per the EJB spec, the target name is the
            // JavaBean property name, and injection must be performed into the
            // corresponding set method if it exists.
            //
            // The following loop has been re-written to include looking for
            // type compatible set methods and to improve performance.      d648283
            // ---------------------------------------------------------------------

            Method primitiveMethod = null;
            List<Method> compatibleMethods = null;
            boolean isClient = ivNameSpaceConfig.isClientMain(injectionClass);
            String setMethodName = getMethodFromProperty(targetName);
            Class<?> primitiveClass = getPrimitiveClass(injectionType);
            List<Method> declaredMethods;

            try
            {
                declaredMethods = getDeclaredSetMethods(injectionClass, isClient); // F743-32443
            } catch (LinkageError err) // RTC116577
            {
                String message = Tr.formatMessage(tc,
                                                  "DECLARED_MEMBER_LINKAGE_ERROR_CWNEN0075E",
                                                  targetClassName,
                                                  setMethodName,
                                                  getJndiName(),
                                                  ivNameSpaceConfig.getDisplayName(),
                                                  ivNameSpaceConfig.getModuleName(),
                                                  ivNameSpaceConfig.getApplicationName(),
                                                  err.toString());
                InjectionException ex = new InjectionException(message, err);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "addInjectionTarget", ex);
                throw ex;
            }

            for (Method method : declaredMethods)
            {
                if (method.getName().equals(setMethodName))
                {
                    Class<?>[] params = method.getParameterTypes();
                    if (params.length == 1)
                    {
                        // If the reference type was not defined in XML, then all
                        // methods with this name and 1 parameter are compatible. d667739.1
                        if (injectionType == null)
                        {
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "target method found (compatible) : " + method);
                            if (compatibleMethods == null) {
                                compatibleMethods = new ArrayList<Method>();
                            }
                            compatibleMethods.add(method);
                            continue;
                        }

                        Class<?> param = params[0];

                        // First, look for the set method with specific type parameter;
                        // if found, then this is the one to use, done looking
                        if (param == injectionType)
                        {
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "target method found (specific) : " + method);
                            targetMethod = method;
                            break;
                        }

                        // Next, look for the set method with primitive version of type;
                        // if found, keep looking for one with specific match (above).
                        if (param == primitiveClass)
                        {
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "target method found (primitive) : " + method);
                            primitiveMethod = method;
                        }

                        // Finally, look for a type compatible match;          d648283
                        // if found, keep looking for more specific match (above).
                        else if (param.isAssignableFrom(injectionType))
                        {
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "target method found (compatible) : " + method);
                            if (compatibleMethods == null) {
                                compatibleMethods = new ArrayList<Method>();
                            }
                            compatibleMethods.add(method);
                        }
                    }
                }
            }

            // Now that the methods have been searched, use the one with the
            // most specific parameter type match.
            if (targetMethod == null)
            {
                if (primitiveMethod != null)
                {
                    targetMethod = primitiveMethod;
                }

                // Use a type compatible method if there is only one;
                // otherwise, it is ambiguous.                               d648283
                else if (compatibleMethods != null)
                {
                    if (compatibleMethods.size() == 1)
                    {
                        targetMethod = compatibleMethods.get(0);
                    }
                    else
                    {
                        // More than one set method was found that is compatible
                        // with the injection type - unable to determine which
                        // one to use.                                         d648283
                        InjectionException iex = new InjectionException
                                        ("The " + targetName + " injection target property name for the " +
                                         getJndiName() + " reference is ambiguous in the " + targetClassName +
                                         " class.  The " + compatibleMethods.get(0) + " method and the " +
                                         compatibleMethods.get(1) + " method are both type compatible with the " +
                                         injectionType + " type.");
                        Tr.error(tc, "AMBIGUOUS_INJECTION_METHODS_CWNEN0061E",
                                 targetName, getJndiName(), targetClassName,
                                 compatibleMethods.get(0), compatibleMethods.get(1),
                                 injectionType);
                        throw iex;
                    }
                }
            }

            if (targetMethod != null)
            {
                // The method has been found, so looking for the field will be
                // skipped (below)... however, don't bother creating the target
                // if one already exists for this method.                    d648283
                if (!containsTarget(targetMethod)) {
                    injectionTarget = createInjectionTarget(targetMethod,
                                                            this);
                }
            }

            // --------------------------------------------------------------------
            // Second, if a set method could not be found, try to locate a field
            // with the JavaBean property name (i.e the specified target name).
            // ---------------------------------------------------------------------

            else
            {
                // try to locate the target as field.
                try
                {
                    targetField = injectionClass.getDeclaredField(targetName);
                    Class<?> fieldClass = targetField.getType();

                    // <injection-target-class> and Field type consistency check
                    if (injectionType == null || // not specified          d667739.1
                        isClassesCompatible(fieldClass, injectionType))
                    {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "target field found (compatible) : " + targetField);

                        // The field has been found... however, don't bother creating
                        // the target if a one already exists for this field.  d648283
                        if (!containsTarget(targetField)) {
                            injectionTarget = new InjectionTargetField(targetField,
                                            this);
                        }
                    }
                    else
                    {
                        // This really should be an error, however this is the way
                        // the original code shipped, so changing this to an error
                        // could result in customer regressions.
                        String qualifiedTargetName = injectionClass.getName() + "." + targetName; // d668039
                        Tr.warning(tc, "FIELD_IS_DECLARED_DIFFERENT_THAN_THE_INECTION_TYPE_CWNEN0021W",
                                   qualifiedTargetName, fieldClass.getName(), injectionType.getName());

                        if (isValidationFailable()) // fail if enabled    F743-14449
                        {
                            throw new InjectionConfigurationException("The " + qualifiedTargetName +
                                                                      " field is declared as " + fieldClass.getName() +
                                                                      " but the requested injection type for the field is " +
                                                                      injectionType.getName() + ".");
                        }
                    }
                } catch (NoSuchFieldException e)
                {
                    FFDCFilter.processException(e, CLASS_NAME + ".addInjectionTarget",
                                                "254", this, new Object[] { targetClassName, targetName });

                    // Unable to find either a set method or field, so log an error and
                    // fail indicating that neither could be found.
                    InjectionException iex = new InjectionException
                                    ("An injection target for the " + getJndiName() +
                                     " reference cannot be processed because neither the " +
                                     setMethodName + " method nor the " + targetName +
                                     " field exist on the " + targetClassName + " class.");
                    Tr.error(tc, "UNABLE_TO_FIND_THE_MEMBER_SPECIFIED_CWNEN0022E",
                             getJndiName(), setMethodName, targetName, targetClassName);
                    throw iex;
                } catch (LinkageError err) // RTC116577
                {
                    String message = Tr.formatMessage(tc,
                                                      "DECLARED_MEMBER_LINKAGE_ERROR_CWNEN0075E",
                                                      targetClassName,
                                                      targetName,
                                                      getJndiName(),
                                                      ivNameSpaceConfig.getDisplayName(),
                                                      ivNameSpaceConfig.getModuleName(),
                                                      ivNameSpaceConfig.getApplicationName(),
                                                      err.toString());
                    InjectionException ex = new InjectionException(message, err);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "addInjectionTarget", ex);
                    throw ex;
                }
            }
        }
        else if (targetName != null ||
                 injectionType != null ||
                 injectionClass != null)
        {
            InjectionException iex;
            iex = new InjectionException("One or more of these argument(s) is null.  " + targetName + " targetName, " + injectionType + " injectionType, " + injectionClass
                                         + " injectionClass");
            Tr.error(tc, "INCORRECT_OR_NULL_INJECTION_TARGETS_SPECIFIED_CWNEN0023E", targetName, injectionType, injectionClass);
            throw iex;
        }

        // It may be normal at this point to not have an injection target. This
        // will occur if a target already exists for the field or method, or
        // the type of the field was not compatible with the injection type
        // (which should be an error, but historically has just been a warning.
        // If a target does exist, then finish setting it up and add it to the
        // list of targets associated with this binding.
        if (injectionTarget != null)
        {
            injectionTarget.setInjectionBinding(this);
            injectionTarget.ivFromXML = true; // d510950

            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "adding injection target : " + injectionTarget);

            addInjectionTarget(injectionTarget);
        }
        else
        {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "no injection target to add : duplicate or not compatible");
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "addInjectionTarget : " + ivInjectionTargets);
    }

    /**
     * Notification that a class-level annotation was found for this binding on
     * the specified class
     *
     * @param klass the class containing the annotation
     */
    // F743-30682
    public void addInjectionClass(Class<?> klass)
    {
        // Default requires nothing done
    }

    /**
     * Adds an InjectionTarget for the specified member (field or method). <p>
     *
     * Duplicates will NOT be added to the list. Duplicates will be ignored.
     * This may occur when an injection is defined in both XML and
     * annotations, or annotations are defined in a common base class. <p>
     *
     * @param member field or method reflection object.
     *
     * @throws InjectionException if a configuration problem is detected
     *             relating the member to the binding.
     */
    public void addInjectionTarget(Member member)
                    throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "addInjectionTarget: " + member);

        // -----------------------------------------------------------------------
        // First, determine if the target being added is already in the list.
        // This may occur if the target is specified in both XML and annotations
        // (an override) or if multiple interceptors inherit from a base class
        // where the base class contains an injection annotation.          d457733
        //
        // Next, determine if the customer has incorrectly attempted to inject
        // into both the field and corresponding java beans property method.
        // This is not allowed per the JavaEE, EJB 3.0 Specifications.   d447011.2
        //
        // Finally, note that if the XML target resulted in the method, but the
        // annotation is on the field, then the XML should really be considered
        // to be overriding the field injection, so don't throw an exception
        // and instead just remove the previous target from XML.           d510950
        // -----------------------------------------------------------------------

        boolean containsTarget = false;
        String existingMemberName = null;
        String toAddMemberName = null;
        InjectionTarget injectionTarget = null;

        if (ivInjectionTargets != null)
        {
            for (InjectionTarget target : ivInjectionTargets)
            {
                Member targetMember = target.getMember();

                if (targetMember.equals(member))
                {
                    // Reset 'fromXML' since it also matches an annotation. Reseting
                    // this allows us to detect when they have annotations on both
                    // the field and set method.                                 PK92087
                    target.ivFromXML = false;

                    // Already in list, break out and check for error below, or ignore
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "found: " + target);
                    injectionTarget = target; // save for trace    d643444
                    containsTarget = true;
                    break;
                }

                // If both are from the same class, then check for 'double' injection
                // into both the field and method for a 'property.              d510950
                if (targetMember.getDeclaringClass() == member.getDeclaringClass())
                {
                    // Obtain the 'property' method name from the existing target
                    if (targetMember instanceof Method) {
                        existingMemberName = targetMember.getName();
                    } else {
                        existingMemberName = getMethodFromProperty(targetMember.getName());
                    }

                    // Obtain the 'property' method name from the target being added
                    if (member instanceof Method) {
                        toAddMemberName = member.getName();
                    } else {
                        toAddMemberName = getMethodFromProperty(member.getName());
                    }

                    // When equal, injection has been specified for both field an method.
                    if (existingMemberName.equals(toAddMemberName))
                    {
                        if (target.ivFromXML)
                        {
                            // If the existing one came from XML, then it must be the
                            // method, but is really intended to be an override of the
                            // field annotation... so just remove the method target
                            // and let the field target be added below.            d510950
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "removing: " + targetMember.getName());
                            ivInjectionTargets.remove(target);
                            break;
                        }

                        // Annotation present on both the field and method... error.
                        Tr.error(tc, "INJECTION_DECLARED_IN_BOTH_THE_FIELD_AND_METHOD_OF_A_BEAN_CWNEN0056E",
                                 ivJndiName,
                                 member.getDeclaringClass().getName(),
                                 ivNameSpaceConfig.getModuleName(),
                                 ivNameSpaceConfig.getApplicationName());
                        throw new InjectionConfigurationException("Injection of the " + ivJndiName +
                                                                  " resource was specified for both a property instance" +
                                                                  " variable and its corresponding set method on the " +
                                                                  member.getDeclaringClass().getName() + " class in the " +
                                                                  ivNameSpaceConfig.getModuleName() + " module of the " +
                                                                  ivNameSpaceConfig.getApplicationName() + " application.");
                    }
                }
            }
        }

        // -----------------------------------------------------------------------
        // The following is stated in the EJB Specification overriding rules:
        //
        //    The injection target, if specified, must name exactly the annotated
        //    field or property method.
        //
        // Previously (EJB 3.0 Feature Pack and WAS 7.0) this was interpreted to
        // mean that if any targets were specified in XML, then the XML must
        // contain targets for every annotation.  So, if the target being added
        // due to an annotation is NOT present in the list, then it was not
        // specified in XML, which would have been an error.  WAS did support
        // adding additional targets from XML, but there had to also be a target
        // for every annotation.
        //
        // Since that time, it has been realized that this can be quite annoying
        // to customers, as they must duplicate annotation information just to
        // specify an extra injection target.  Also, this could be quite difficult
        // to enforce for EJBs in WAR modules, where targets and EJB references
        // can now be defined in many different locations.
        //
        // Beginning in WAS 8.0, the above rule is now being interpreted to mean
        // only that if a target in XML matches an annotation, then that target
        // is considered an override of that annotation and has really very
        // little effect.  Additional targets in XML are not considered overrides
        // of any annotations, and are just additional targets.  Thus, targets
        // from XML do not globally replace all targets from annotations, but
        // just add to them.                                               d643444
        //
        // Also note that the rule indicates it is an override when they are an
        // exact match, so if the target was in XML, but was for a set method,
        // and the annotation was for the field... then it is assumed the XML
        // target was really the field (there is no way to indicate field in XML),
        // so the above code would have removed the method target, and will
        // expect the code below to add the field target; this is not an error.
        // To not assume these are an exact match would otherwise always result
        // in an error, since injection may not occur into both the field and
        // corresponding set method.
        //
        // Otherwise, if the XML specified no targets, then this is just an add
        // due to an annotation.  As long as the target is not already in the
        // list, then just add it (i.e. no duplicates).
        //
        // In this scenario, the target may already be in the list if multiple
        // classes being injected both inherit from the same base class. The
        // target will only be added once, and later will be 'collected' into
        // the InjectionTarget arrays for both subclasses.                 d457733
        // -----------------------------------------------------------------------
        if (!containsTarget)
        {
            if (member instanceof Field)
            {
                injectionTarget = new InjectionTargetField((Field) member, this);
            }
            else
            {
                injectionTarget = createInjectionTarget((Method) member, this);
            }

            injectionTarget.setInjectionBinding(this);
            addInjectionTarget(injectionTarget);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "addInjectionTarget : " +
                        ((containsTarget) ? "(duplicate) " : "") + injectionTarget);
    }

    /**
     * Merges the configuration information of an annotation with a binding
     * object created from previously processed XML or annotations. <p>
     *
     * The may occur when there is an XML override of an annotation, or
     * there are multiple annotations defined with the same name (i.e.
     * a multiple target injection scenario).
     *
     * @param annotation the annotation to be merged
     * @param instanceClass the class containing the annotation
     * @param member the Field or Method associated with the annotation;
     *            null if a class level annotation.
     * @throws InjectionException if a problem occurs while creating
     *             the instance to be injected
     **/
    public abstract void merge(A annotation, Class<?> instanceClass, Member member)
                    throws InjectionException;

    /**
     * Returns the ref type associated with this InjectionBinding. This method
     * is used by other utility methods in this class, which cannot be used if
     * it is not implemented.
     */
    protected JNDIEnvironmentRefType getJNDIEnvironmentRefType() {
        throw new UnsupportedOperationException();
    }

    /**
     * Indication that an error has occurred while merging an attribute value.
     *
     * <p>This method requires {@link #getJNDIEnvironmentRefType} to be defined.
     *
     * @param oldValue the old value
     * @param newValue the new value
     * @param xml true if the error occurred for an XML element
     * @param elementName the XML element name if xml is true, or the annotation
     *            element name
     * @param property true if the error occurred for a property
     * @param key the optional property name if property is true, or the
     *            annotation element name
     */
    protected void mergeError(Object oldValue,
                              Object newValue,
                              boolean xml,
                              String elementName,
                              boolean property,
                              String key) throws InjectionConfigurationException {
        JNDIEnvironmentRefType refType = getJNDIEnvironmentRefType();
        String component = ivNameSpaceConfig.getDisplayName();
        String module = ivNameSpaceConfig.getModuleName();
        String application = ivNameSpaceConfig.getApplicationName();
        String jndiName = getJndiName();

        if (xml) {
            Tr.error(tc, "CONFLICTING_XML_VALUES_CWNEN0052E",
                     component,
                     module,
                     application,
                     elementName,
                     refType.getXMLElementName(),
                     refType.getNameXMLElementName(),
                     jndiName,
                     oldValue,
                     newValue);
        } else {
            Tr.error(tc, "CONFLICTING_ANNOTATION_VALUES_CWNEN0054E",
                     component,
                     module,
                     application,
                     elementName,
                     '@' + refType.getAnnotationShortName(),
                     refType.getNameAnnotationElementName(),
                     jndiName,
                     oldValue,
                     newValue);
        }

        String exMsg;
        if (xml) {
            exMsg = "The " + component +
                    " component in the " + module +
                    " module of the " + application +
                    " application has conflicting configuration data in the XML" +
                    " deployment descriptor. Conflicting " + elementName +
                    " element values exist for multiple " + refType.getXMLElementName() +
                    " elements with the same " + refType.getNameXMLElementName() +
                    " element value : " + jndiName +
                    ". The conflicting " + elementName +
                    " element values are " + oldValue +
                    " and " + newValue + ".";
        } else {
            exMsg = "The " + component +
                    " component in the " + module +
                    " module of the " + application +
                    " application has conflicting configuration data" +
                    " in source code annotations. Conflicting " + elementName +
                    " attribute values exist for multiple @" + refType.getAnnotationShortName() +
                    " annotations with the same " + refType.getNameAnnotationElementName() +
                    " attribute value : " + jndiName +
                    ". The conflicting " + elementName +
                    " attribute values are " + oldValue +
                    " and " + newValue + ".";
        }

        throw new InjectionConfigurationException(exMsg);
    }

    /**
     * Merges the value of a boolean annotation value.
     *
     * <p>If an error occurs, {@link #mergeError} will be called, which
     * requires {@link #getJNDIEnvironmentRefType} to be defined.
     *
     * @param oldValue the old value
     * @param oldValueXML true if the old value was set by XML
     * @param newValue the new value
     * @param elementName the annotation element name
     * @param defaultValue the default value as specified in the annotation
     * @return the merged value
     * @throws InjectionConfigurationException if an error occurs
     */
    protected Boolean mergeAnnotationBoolean(Boolean oldValue,
                                             boolean oldValueXML,
                                             boolean newValue,
                                             String elementName,
                                             boolean defaultValue) throws InjectionConfigurationException {
        if (newValue == defaultValue || oldValueXML) {
            return oldValue;
        }

        if (isComplete()) {
            mergeError(oldValue, newValue, false, elementName, false, elementName);
            return oldValue;
        }

        // Merge errors cannot occur for boolean attributes because there are
        // only two possible values: the default value, for which there is no way
        // to detect if it was explicitly specified, and the other value.  So, if
        // any of the annotations specify the non-default value, we use it.
        return newValue;
    }

    /**
     * Merges the value of an integer annotation value.
     *
     * <p>If an error occurs, {@link #mergeError} will be called, which
     * requires {@link #getJNDIEnvironmentRefType} to be defined.
     *
     * @param oldValue the old value
     * @param oldValueXML true if the old value was set by XML
     * @param newValue the new value
     * @param elementName the annotation element name
     * @param defaultValue the default value as specified in the annotation
     * @return the merged value
     * @throws InjectionConfigurationException if an error occurs
     */
    protected Integer mergeAnnotationInteger(Integer oldValue,
                                             boolean oldValueXML,
                                             int newValue,
                                             String elementName,
                                             int defaultValue,
                                             Map<Integer, String> valueNames) throws InjectionConfigurationException {
        if (newValue == defaultValue) {
            return oldValue;
        }

        if (oldValueXML) {
            return oldValue;
        }

        if (oldValue == null ? isComplete() : !oldValue.equals(newValue)) {
            Object oldValueName = valueNames == null ? oldValue : valueNames.get(oldValue);
            Object newValueName = valueNames == null ? newValue : valueNames.get(newValue);
            mergeError(oldValueName, newValueName, false, elementName, false, elementName);
            return oldValue;
        }

        return newValue;
    }

    /**
     * Merges the value of a String or Enum annotation value.
     *
     * <p>If an error occurs, {@link #mergeError} will be called, which
     * requires {@link #getJNDIEnvironmentRefType} to be defined.
     *
     * @param oldValue the old value
     * @param oldValueXML true if the old value was set by XML
     * @param newValue the new value
     * @param elementName the annotation element name
     * @param defaultValue the default value as specified in the annotation
     * @return the merged value
     * @throws InjectionConfigurationException if an error occurs
     */
    protected <T> T mergeAnnotationValue(T oldValue,
                                         boolean oldValueXML,
                                         T newValue,
                                         String elementName,
                                         T defaultValue) throws InjectionConfigurationException {
        if (newValue.equals(defaultValue) || oldValueXML) { // d663356
            return oldValue;
        }

        if (oldValue == null ? isComplete() : !newValue.equals(oldValue)) {
            mergeError(oldValue, newValue, false, elementName, false, elementName);
            return oldValue;
        }

        return newValue;
    }

    /**
     * Indication that an error has occurred while merging an annotation
     * property that is malformed (for example, missing "=").
     *
     * <p>This method requires {@link #getJNDIEnvironmentRefType} to be defined.
     *
     * @param property the property string
     */
    protected void mergeAnnotationPropertyError(String property) throws InjectionConfigurationException {
        JNDIEnvironmentRefType refType = getJNDIEnvironmentRefType();
        String component = ivNameSpaceConfig.getDisplayName();
        String module = ivNameSpaceConfig.getModuleName();
        String application = ivNameSpaceConfig.getApplicationName();
        String jndiName = getJndiName();

        Tr.error(tc, "INVALID_ANNOTATION_PROPERTY_CWNEN0066E",
                 '@' + refType.getAnnotationShortName(),
                 jndiName,
                 refType.getNameAnnotationElementName(),
                 component,
                 module,
                 application,
                 property);

        String exMsg = "The @" + refType.getAnnotationShortName() +
                       " source code annotation with the " + jndiName +
                       " " + refType.getNameAnnotationElementName() +
                       " attribute for the " + component +
                       " component in the " + module +
                       " module in the " + application +
                       " application has configuration data for the properties attribute that is not valid: " + property;
        throw new InjectionConfigurationException(exMsg);
    }

    /**
     * Merges the properties specified in name=value format by an annotation.
     * If an error occurs, {@link #mergeAnnotationPropertyError} or {@link #mergeError} will
     * be called.
     *
     * <p>If an error occurs, {@link #mergeAnnotationPropertyError} or {@link #mergeError} will
     * be called, which require {@link #getJNDIEnvironmentRefType} to be defined.
     *
     * @param oldProperties the old properties, or null if uninitialized
     * @param oldXMLPropertyNames the old property names set by XML
     * @param newProperties the new properties in name=value format
     * @return the old properties updated as necessary
     * @throws InjectionConfigurationException if an error occurs
     */
    protected Map<String, String> mergeAnnotationProperties(Map<String, String> oldProperties,
                                                            Set<String> oldXMLPropertyNames,
                                                            String[] newProperties) throws InjectionConfigurationException {
        if (newProperties.length != 0) {
            if (oldProperties == null) {
                oldProperties = new HashMap<String, String>();
            }

            for (String property : newProperties) {
                int index = property.indexOf('=');
                if (index == -1) {
                    mergeAnnotationPropertyError(property);
                    continue;
                }

                String name = property.substring(0, index);

                if (oldXMLPropertyNames == null || !oldXMLPropertyNames.contains(name)) {
                    String newValue = property.substring(index + 1);
                    Object oldValue = oldProperties.get(name);

                    if (oldValue == null ? isComplete() : !newValue.equals(oldValue)) {
                        mergeError(oldValue, newValue, true, name + " property", true, name);
                        continue;
                    }

                    oldProperties.put(name, newValue);
                }
            }
        }

        return oldProperties;
    }

    /**
     * Merges a value specified in XML.
     *
     * <p>If an error occurs, {@link #mergeError} will be called, which
     * requires {@link #getJNDIEnvironmentRefType} to be defined.
     *
     * @param oldValue the old value
     * @param newValue the new value
     * @param elementName the name of the XML element containing the value
     * @param key the optional key to be passed to {@link #mergeError}
     * @param valueNames the names of possible old and new values to be used
     *            for error messages, or null if the values themselves should
     *            be used when reporting errors
     * @return the merged value
     * @throws InjectionConfigurationException if an error occurs
     */
    protected <T> T mergeXMLValue(T oldValue,
                                  T newValue,
                                  String elementName,
                                  String key,
                                  Map<T, String> valueNames) throws InjectionConfigurationException {
        if (newValue == null) {
            return oldValue;
        }

        if (oldValue != null && !newValue.equals(oldValue)) {
            Object oldValueName = valueNames == null ? oldValue : valueNames.get(oldValue);
            Object newValueName = valueNames == null ? newValue : valueNames.get(newValue);
            mergeError(oldValueName, newValueName, true, elementName, false, key);
            return oldValue;
        }

        return newValue;
    }

    /**
     * Merges the properties specified in XML.
     *
     * <p>If an error occurs, {@link #mergeError} will be called, which
     * requires {@link #getJNDIEnvironmentRefType} to be defined.
     *
     * @param oldProperties the old properties, or null if uninitialized
     * @param oldXMLPropertyNames the old property names set by XML to be updated
     * @param newProperties the new properties
     * @return the old properties updated as necessary
     * @throws InjectionConfigurationException if an error occurs
     */
    protected Map<String, String> mergeXMLProperties(Map<String, String> oldProperties,
                                                     Set<String> oldXMLProperties,
                                                     List<Property> properties) throws InjectionConfigurationException {
        if (!properties.isEmpty()) {
            if (oldProperties == null) {
                oldProperties = new HashMap<String, String>();
            }

            for (Property property : properties) {
                String name = property.getName();
                String newValue = property.getValue();
                Object oldValue = oldProperties.put(name, newValue);

                if (oldValue != null && !newValue.equals(oldValue)) {
                    mergeError(oldValue, newValue, true, name + " property", true, name);
                    continue;
                }

                oldXMLProperties.add(name);
            }
        }

        return oldProperties;
    }

    /**
     * Compare the metadata from a saved injection binding with this injection
     * binding. This operation is only necessary for java:global, java:app, or
     * java:module processing in runtime environments that do not support
     * reference merging during application install. This method is expected to
     * ensure that the injection bindings have "identical" metadata per the
     * Java EE 6 specification by using {@link #mergeSavedValue}. This merge
     * should require identical WAS-specific metadata (such as bindings) but
     * should not require identical XML-specific metadata (such as whether an
     * ejb-ref is Session or Entity).
     *
     * <p>This method will be called after {@link InjectionProcessor#resolve}.
     * Injection processors and bindings must ensure that the resolve method
     * finalizes the state of this object and that this method does not modify
     * any state.
     *
     * <p>By default, this method throws UnsupportedOperationException. It only
     * needs to be overridden by non-simple injection bindings that operate in
     * runtime environments that do not support reference merging during
     * application install.
     *
     * @param injectionBinding the injection binding to merge from
     * @throws InjectionException if the metadata in the bindings is not
     *             identical
     */
    public void mergeSaved(InjectionBinding<A> injectionBinding)
                    throws InjectionException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Validates that two values are identical. This helper method is intended
     * to be used by {@link #mergeSaved}.
     *
     * @param oldValue the value in this binding
     * @param newValue the value from a saved binding
     * @param name the name for the metadata attribute; by convention, this
     *            should be the XML element name from the deployment descriptor, bindings
     *            file, or extension file.
     * @throws InjectionConfigurationException if oldValue and newValue are not
     *             identical (both null or equal).
     */
    protected final <T> void mergeSavedValue(T oldValue, T newValue, String name) // d681743
    throws InjectionConfigurationException
    {
        if (oldValue == null ? newValue != null : !oldValue.equals(newValue))
        {
            Tr.error(tc, "INCOMPATIBLE_MERGE_ATTRIBUTES_CWNEN0072E",
                     getJndiName(), name, oldValue, newValue);
            throw new InjectionConfigurationException("The " + getJndiName() +
                                                      " reference has conflicting values for the " + name +
                                                      " attribute: " + oldValue + " and " + newValue);
        }
    }

    /**
     * Returns the annotation type associated with this InjectionBinding
     */
    public Class<?> getAnnotationType()
    {
        return ivAnnotation.annotationType();
    }

    /**
     * Add the specified property if the value is non-null, or remove it from
     * the map if it is null.
     *
     * @param props the properties to update
     * @param key the key
     * @param value the value
     */
    protected static <K, V> void addOrRemoveProperty(Map<K, V> props, K key, V value)
    {
        if (value == null) {
            // Generic properties have already been added to the map.  Remove them
            // so they aren't confused with the builtin properties.
            props.remove(key);
        } else {
            props.put(key, value);
        }
    }

    /**
     * Create a Reference to a resource definition.
     *
     * @param bindingName the binding name, or null if none
     * @param type the resource type
     * @param properties the resource-specific properties
     * @return the resource definition Reference
     */
    public Reference createDefinitionReference(String bindingName, String type, Map<String, Object> properties) throws InjectionException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Map<String, Object> traceProps = properties;
            if (traceProps.containsKey("password")) {
                traceProps = new HashMap<String, Object>(properties);
                traceProps.put("password", "********");
            }

            Tr.entry(tc, "createDefinitionReference: bindingName=" + bindingName + ", type=" + type, traceProps);
        }

        Reference ref;
        try {
            InternalInjectionEngine injectionEngine = (InternalInjectionEngine) InjectionEngineAccessor.getInstance();
            ref = injectionEngine.createDefinitionReference(ivNameSpaceConfig, ivInjectionScope, getJndiName(), bindingName, type, properties);
        } catch (Exception ex) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "createDefinitionReference", ex);
            throw new InjectionException(ex);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createDefinitionReference", ref);
        return ref;
    }

    /**
     * Sets the objects to use for injection and binding. Usually, these
     * objects are the same; otherwise, {@link #setObjects(Object, Reference)} should be used instead.
     *
     * @param injectionObject the object to inject
     * @param bindingObject the object to bind, or null if injectionObject
     *            should be bound directly
     */
    public final void setObjects(Object injectionObject, Object bindingObject)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "setObjects", injectionObject, bindingObject);

        this.ivInjectedObject = injectionObject; // d392996.3
        this.ivBindingObject = (bindingObject != null) ? bindingObject : injectionObject;

        if (ivBindingObject == null) // F54050
        {
            throw new IllegalArgumentException("expected non-null argument");
        }
    }

    /**
     * Sets the object to use for injection and the Reference to use for
     * binding. Usually, the injection object is null.
     *
     * @param injectionObject the object to inject, or null if the object should
     *            be obtained from the binding object instead
     * @param bindingObject the object to bind, or null if injectionObject
     *            should be bound directly if is non-null
     * @throws InjectionException if a problem occurs while creating
     *             the instance to be injected
     */
    public void setObjects(Object injectionObject, Reference bindingObject)
                    throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "setObjects", injectionObject, bindingObjectToString(bindingObject));

        ivInjectedObject = injectionObject; // d392996.3

        if (bindingObject != null)
        {
            ivBindingObject = bindingObject;
            ivObjectFactoryClassName = bindingObject.getFactoryClassName(); // F48603.4
            if (ivObjectFactoryClassName == null) // F54050
            {
                throw new IllegalArgumentException("expected non-null getFactoryClassName");
            }
        }
        else
        {
            ivBindingObject = injectionObject;
        }
    }

    /**
     * Sets the Reference object to be bound into Naming (java:comp/env) for
     * this binding, and also provides the corresponding ObjectFactory class
     * object, so that it doesn't need to be obtained from a classloader. <p>
     *
     * Similar to setObjects, but assumes the 'injectionObject' is null and
     * takes advantage of the scenario where the ObjectFactory class is
     * available. <p>
     *
     * This method should be used instead of 'setObjects' when the ObjectFactory
     * class is available, as it avoids an attempt to load the class a second
     * time. <p>
     *
     * @param bindingObject the Reference object to be bound into the component
     *            name space (java:comp/env).
     * @param objectFactory the corresponding ObjectFactory.
     *
     * @throws InjectionException if a problem occurs while creating
     *             the instance to be injected
     **/
    // F623-841.1
    public void setReferenceObject(Reference bindingObject,
                                   Class<? extends ObjectFactory> objectFactory)
                    throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "setReferenceObject", bindingObjectToString(bindingObject), objectFactory);

        ivInjectedObject = null;
        ivBindingObject = bindingObject;
        ivObjectFactoryClass = objectFactory; // F48603.4
        ivObjectFactoryClassName = objectFactory.getName(); // F54050
    }

    /**
     * Returns an object to be injected for this injection binding. This method
     * must be used instead of {@link #getInjectionObject} for externally merged
     * java:global/:app/:module bindings.
     *
     * @param targetObject the object being injected into
     * @param targetContext provides access to context data associated with
     *            the target of the injection (e.g. EJBContext). May be null
     *            if not provided by the container, and will be null for a
     *            naming lookup.
     * @return the value to inject
     */
    Object getInjectableObject(Object targetObject, // F743-33811.2
                               InjectionTargetContext targetContext) // F49213.1
    throws InjectionException
    {
        if (getInjectionScope() != InjectionScope.COMP)
        {
            // In some environments, non-comp bindings might not be fully merged,
            // which means injection must do a full lookup.  Allow the runtime
            // environment to decide.
            return InjectionEngineAccessor.getInternalInstance().getInjectableObject(this, targetObject, targetContext);
        }

        // If the injected object will be contextually related to the target,
        // then use the signature that passes the target.                d664351.1
        return getInjectionObject(targetObject, targetContext);
    }

    /**
     * Returns the object to be injected. <p>
     *
     * This may be a cached / constant value, or a different (new)
     * instance every time the method is called. <p>
     *
     * This method must not be called for non-java:comp bindings. <p>
     *
     * @throws InjectionException if a problem occurs while creating
     *             the instance to be injected.
     */
    public final Object getInjectionObject() throws InjectionException
    {
        return getInjectionObject(null, null); // F48603.4
    }

    /**
     * Returns the object to be injected. <p>
     *
     * This may be a cached / constant value, or a different (new)
     * instance every time the method is called. <p>
     *
     * This method must not be called for non-java:comp bindings. <p>
     *
     * Subclasses should generally override {@link #getInjectionObjectInstance} rather than overriding this method because this method provides error
     * handling around the call to that method.
     *
     * @param targetObject the object containing the field or method that will
     *            be injected with the result, or null if the binding
     *            value is being obtained via lookup.
     * @param targetContext provides access to context data associated with the
     *            target of the injection (e.g. EJBContext). Will be
     *            null if the value is being obtained via lookup.
     * @throws InjectionException if a problem occurs while creating
     *             the instance to be injected.
     */
    public Object getInjectionObject(Object targetObject,
                                     InjectionTargetContext targetContext)
                    throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getInjectionObject: " + toSimpleString());

        Object retObj = ivInjectedObject;

        // If the object to be injected has not been cached, then it could
        // be obtained by performing a naming lookup, which would invoke the
        // Reference ObjectFactory with the binding info (RefAddr) object
        // that was bound into naming during populateJavaNameSpace.  However,
        // to improve performance, that same binding info has been saved in
        // this binding so that the factory  may be invoked directly.

        if (retObj == null)
        {
            try
            {
                retObj = getInjectionObjectInstance(targetObject, targetContext); // F48603.4
            } catch (RecursiveInjectionException ex)
            {
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "getInjectionObject: " + ex);
                throw ex;
            } catch (Throwable ex)
            {
                // Only log FFDC if this is a failed injection; failed naming lookups may
                // be normal and the caller should decide if FFDC is needed.
                if (targetObject != null) {
                    FFDCFilter.processException(ex, CLASS_NAME + ".getInjectionObject",
                                                "408", this, (Object[]) null);
                }

                String displayName = getDisplayName();
                Object exMessage = ex.getLocalizedMessage();
                if (exMessage == null)
                {
                    exMessage = ex.toString();
                }

                String message = Tr.formatMessage(tc,
                                                  "FAILED_TO_CREATE_OBJECT_INSTANCE_CWNEN0030E",
                                                  displayName,
                                                  exMessage);
                InjectionException ex2 = new InjectionException(message, ex);

                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "getInjectionObject", ex2);
                throw ex2;
            }

            // getInjectionObjectInstance should never return null.  No-param
            // InjectionTargetMultiParamMethod should return an empty array.
            if (retObj == null) // F87539
            {
                String classTypeName, component, module, app;
                if (ivJ2eeName != null)
                {
                    component = ivJ2eeName.getComponent();
                    module = ivJ2eeName.getModule();
                    app = ivJ2eeName.getApplication();
                }
                else
                {
                    component = module = app = "UNKNOWN";
                }

                classTypeName = ivInjectionClassType == null ? "UNKNOWN" : ivInjectionClassType.getName();

                Tr.error(tc, "UNABLE_TO_RESOLVE_INJECTION_OBJECT_CWNEN0035E",
                         getDisplayName(),
                         classTypeName,
                         component,
                         module,
                         app); // d468667 d502635.1

                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "getInjectionObject : failed");

                throw new InjectionException("The " + getDisplayName() + " reference of type " +
                                             classTypeName + " for the " +
                                             component + " component in the " +
                                             module + " module of the " +
                                             app + " application cannot be resolved."); // d502635.1
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getInjectionObject : " + Util.identity(retObj));

        return retObj;
    }

    /**
     * Obtains an object instance for this binding. By default, this method
     * invokes {@link ObjectFactory#getObjectInstance} on the Reference passed
     * to {@link #setObjects(Object, Reference)} or {@link #setReferenceObject}.
     * Subclasses that return false from {@link #isResolved} should override
     * this method.
     *
     * @param targetObject the object containing the field or method that will
     *            be injected with the result, or null if the binding
     *            value is being obtained via lookup.
     * @param targetContext provides access to context data associated with the
     *            target of the injection (e.g. EJBContext). May be
     *            null if not provided by the container, and will be
     *            null for a naming lookup.
     */
    // F48603.4
    protected Object getInjectionObjectInstance(Object targetObject,
                                                InjectionTargetContext targetContext)
                    throws Exception
    {
        ObjectFactory objectFactory = ivObjectFactory; // volatile-read
        InjectionObjectFactory injObjFactory = ivInjectionObjectFactory;
        if (objectFactory == null)
        {
            try
            {
                InternalInjectionEngine ie = InjectionEngineAccessor.getInternalInstance();
                objectFactory = ie.getObjectFactory(ivObjectFactoryClassName, ivObjectFactoryClass); // F54050
                if (objectFactory instanceof InjectionObjectFactory) // F49213.1
                {
                    injObjFactory = (InjectionObjectFactory) objectFactory;
                    ivInjectionObjectFactory = injObjFactory;
                }
                ivObjectFactory = objectFactory; // volatile-write
            } catch (Throwable ex)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "getInjectionObjectInstance", ex);
                Tr.error(tc, "OBJECT_FACTORY_CLASS_FAILED_TO_LOAD_CWNEN0024E", ivObjectFactoryClassName);
                throw new InjectionException(ex.toString(), ex);
            }
        }

        if (injObjFactory != null) // F49213.1
        {
            return injObjFactory.getInjectionObjectInstance((Reference) getBindingObject(),
                                                            targetObject, targetContext);
        }

        return objectFactory.getObjectInstance(getBindingObject(), null, null, null);
    }

    /**
     * Returns the binding object. This should always be non-null after {@link InjectionProcessor#resolve} unless this binding and its targets
     * should be ignored (for example, env-entry without a value).
     */
    public final Object getBindingObject()
    {
        return ivBindingObject;
    }

    /**
     * Returns the object to be returned to a remote client. This should always
     * be non-null after {@link InjectionProcessor#resolve} unless this binding
     * and its targets should be ignored (for example, env-entry without a value).
     *
     * Default behavior is equivalent to {@link #getBindingObject}.
     *
     * @throws NamingException if a remote object cannot be returned for this binding.
     */
    public Object getRemoteObject() throws NamingException
    {
        return ivBindingObject;
    }

    /**
     * Determines if the binding has been resolved. Only bindings that are
     * resolved are eligible for injection or binding into the namespace. The
     * default implementation of this method returns true only if {@link #getBindingObject} returns non-null.
     *
     * @return true if the binding is resolved
     */
    public boolean isResolved() // F48603.4
    {
        // This should only return null for env-entry without a value.  All other
        // cases should result in an exception from InjectionProcessor.resolve().
        return getBindingObject() != null;
    }

    // d367834.11 Begins
    /**
    *
    */
    public final A getAnnotation()
    {
        return ivAnnotation;
    }

    // d367834.11 Ends

    // d367834.14 Begins
    /**
    *
    */
    public final void setAnnotation(A newAnnotation)
    {
        ivAnnotation = newAnnotation;
    }

    // d367834.14 Ends

    public final void setJndiName(String jndiName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && jndiName.length() != 0)
            Tr.debug(tc, "setJndiName: " + jndiName);

        // Starting with Java EE 6, reference names may now start with java: and
        // then global, app, module, or comp. 'env' is optional, but recommended.
        // The set jndiName will always be the 'short' name, to make it consistent
        // to prior releases. "java:comp/" without the optional 'env' subcontext
        // will be treated specially and will be the full name.          d662985.2
        ivJndiName = InjectionScope.normalize(jndiName); // d726563
    }

    public final String getJndiName() {
        return ivJndiName;
    }

    /**
     * Gets a name for this binding that is suitable for error messages. This
     * method is only intended to be called after metadata processing is
     * completed.
     */
    // F50309.5
    public String getDisplayName()
    {
        return InjectionScope.denormalize(ivJndiName); // RTC105173
    }

    public InjectionScope getInjectionScope() // d660700
    {
        return ivInjectionScope == null ? InjectionScope.COMP : ivInjectionScope;
    }

    /**
     * Set the InjectionClassType to be most fine grained injection target Class type.
     *
     * For instance, if there are three injection classes
     * named Object,Cart,myCart where each extends the prior, the setInjectionClassType
     * will set the class to be myCart.
     *
     * @param injectionClassType type of the object association with reference
     * @throws InjectionException if a problem occurs while creating
     *             the instance to be injected
     */
    public void setInjectionClassType(Class<?> injectionClassType) throws InjectionException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setInjectionClassType: " + injectionClassType);

        // if the injection class type hasn't been set yet ( null from XML or
        // Object from annotations, the default) then set it.
        if (ivInjectionClassType == null ||
            ivInjectionClassType == Object.class)
        {
            ivInjectionClassType = injectionClassType;
        }

        // If the specified class is a sublcass of the current setting, then
        // replace it, otherwise insure it is compatible
        else
        {
            if (ivInjectionClassType.isAssignableFrom(injectionClassType))
            {
                ivInjectionClassType = injectionClassType;
            }
            else
            {
                if (!injectionClassType.isAssignableFrom(ivInjectionClassType))
                {
                    // TODO : Currently this warning will be present for most
                    //        primitives... need to improve this method to
                    //        properly handle primitives, and throw an exception here!
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "WARNING: Class " + injectionClassType +
                                     " is not assignable from " + ivInjectionClassType);
                }
            }
        }
    }

    /**
     * Sets the injection class type name. When class names are specified in
     * XML rather than annotation, sub-classes must call this method or override
     * getInjectionClassTypeName in order to support callers of the injection
     * engine that do not have a class loader.
     *
     * @param name the type name of the referenced object
     */
    public void setInjectionClassTypeName(String name) // F743-32443
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setInjectionClassType: " + name);

        if (ivInjectionClassTypeName != null)
        {
            throw new IllegalStateException("duplicate reference data for " + getJndiName());
        }

        ivInjectionClassTypeName = name;
    }

    /**
     * Set the injection class type(s) of the binding based on the parameter(s)
     * of the specified injection target method. <p>
     *
     * Normally, exactly one parameter is allowed, but if the binding supports
     * initializer methods then it should override this method and evaluate the
     * parameters for annotations as well as type.
     *
     * @param method an injection target for this binding
     */
    // F743-32637
    public void setInjectionClassType(Method method) throws InjectionException
    {
        Class<?>[] parameterTypes = method.getParameterTypes();

        // -----------------------------------------------------------------------
        // Per Specification - an injection method must have exactly 1 parameter
        // -----------------------------------------------------------------------
        if (parameterTypes.length != 1) // d660818.1
        {
            Tr.error(tc, "INJECTION_METHOD_MUST_HAVE_ONE_PARAM_CWNEN0069E",
                     method.getDeclaringClass().getName(),
                     method.getName(),
                     method.getParameterTypes().length);
            InjectionConfigurationException icex = new InjectionConfigurationException
                            ("The injection method " + method.getDeclaringClass().getName() +
                             "." + method.getName() + " must have exactly one parameter" +
                             ", not " + method.getParameterTypes().length + " parameters.");
            throw icex;
        }

        setInjectionClassType(parameterTypes[0]);
    }

    /**
     * Gets the injection class type name either directly or from the injection
     * class type.
     */
    public String getInjectionClassTypeName() // F743-32443
    {
        return ivInjectionClassType != null && ivInjectionClassType != Object.class ?
                        ivInjectionClassType.getName() :
                        ivInjectionClassTypeName;
    }

    /**
    *
    */
    public Class<?> getInjectionClassType()
    {
        return ivInjectionClassType;
    }

    /**
     * Resolves the JNDI that will be used to bind into the component or
     * java:global namespace.
     *
     * @throws InjectionConfigurationException if the JNDI name is invalid
     */
    final void resolveJndiName() // F743-29417
    throws InjectionConfigurationException
    {
        if (ivJndiName.startsWith("java:"))
        {
            // d660700 - Ensure the name starts with a proper java: scope.  Note
            // that setJndiName has already removed the "java:comp/env/" prefix if
            // present.  The logic in this method is separate from setJndiName
            // because that method can't throw InjectionConfigurationException.
            ivInjectionScope = InjectionScope.match(ivJndiName);
            if (ivInjectionScope == null)
            {
                String component = ivNameSpaceConfig.getDisplayName();
                String module = ivNameSpaceConfig.getModuleName();
                String application = ivNameSpaceConfig.getApplicationName();

                Tr.error(tc, "INVALID_REFERENCE_NAME_CWNEN0065E",
                         ivJndiName,
                         component,
                         module,
                         application);

                throw new InjectionConfigurationException("The " + ivJndiName +
                                                          " reference for the " + component +
                                                          " component in the " + module +
                                                          " module in the " + application +
                                                          " application has a name that is not valid.");
            }

            ivJavaNameSpaceName = ivJndiName.substring(ivInjectionScope.prefix().length());
        }
        else
        {
            ivJavaNameSpaceName = ivJndiName;
        }
    }

    private String bindingObjectToString(Object object) // dd675172
    {
        if (object instanceof Reference)
        {
            Reference ref = (Reference) object;

            StringBuilder sb = new StringBuilder(Util.identity(ref));
            sb.append("[").append(ref.getFactoryClassName()).append(", [");

            for (int i = 0; i < ref.size(); i++)
            {
                RefAddr refAddr = ref.get(i);
                sb.append(refAddr.getType()).append('[').append(refAddr.getContent()).append("], ");
            }

            // Remove trailing comma.
            sb.setLength(sb.length() - 2);

            return sb.append("]]").toString();
        }

        return String.valueOf(object);
    }

    /**
     * Binds the binding object to the required namespace and inserts the binding
     * object into {@link #ivJavaColonCompEnvMap} as necessary.
     */
    void bindInjectedObject() throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "bindInjectedObject: " + toSimpleString(), bindingObjectToString(ivBindingObject));

        // d660700 - resolveJndiName will set a scope if the JNDI name starts
        // with "java:", including "java:comp/".  However, JNDI names starting
        // with "java:comp/env/" will not have a scope because that prefix is
        // removed by setJndiName.
        InternalInjectionEngine ie = InjectionEngineAccessor.getInternalInstance();
        ie.bindJavaNameSpaceObject(ivNameSpaceConfig, ivInjectionScope, ivJavaNameSpaceName, this, ivBindingObject);

        // Store this binding object in the caller's map, if provided.  d473811
        if (ivJavaColonCompEnvMap != null &&
            ivInjectionScope == null)
        {
            ivJavaColonCompEnvMap.put(ivJndiName, this);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "bindInjectedObject");
    }

    /**
     * Test if the input two classes are auto-boxing compatible.
     *
     * @param memberClass Class type of the method or field
     * @param injectClass Class type of object being injected
     */
    public static boolean isClassesCompatible(Class<?> memberClass,
                                              Class<?> injectClass)
    {
        // If the class of the field or method is the injection
        // type or a parent of it, then the injection may occur.           d433391
        if (memberClass.isAssignableFrom(injectClass))
            return true;

        // TODO : Remove this hack when EJB injection is working properly
        // Currently for EJB injection from XML, the 'member' class is
        // is always set to 'Object'.                                      d433391
        if (injectClass.isAssignableFrom(memberClass))
            return true;

        // Otherwise, check to see if either is a primitive, and if so,
        // verify that the types are compatible.
        // TODO : this needs to handle where the primitive types are different
        //        but may still be compatible... or one is an Object that
        //        may be autoboxed to a primitive ..like Integer -> short
        return getPrimitiveClass(memberClass) == getPrimitiveClass(injectClass);
    }

    /**
     * Returns the most most specific class to the caller. If the two
     * classes are not compatible a null is returned.
     *
     * @param classOne Class type of the method or field
     * @param classTwo Class type of object being injected
     */
    public static Class<?> mostSpecificClass(Class<?> classOne,
                                             Class<?> classTwo)
    {
        if (classOne.isAssignableFrom(classTwo)) {
            return classTwo; // d479669
        }
        else if (classTwo.isAssignableFrom(classOne)) {
            return classOne; // d479669
        }

        return null;
    }

    /*
     * Attempt to retrieve the associated primitive class of the input object clazz if possible,
     * otherwise return the input class.
     */
    public static final Class<?> getPrimitiveClass(Class<?> clazz)
    {
        Class<?> retValue = clazz;
        if (clazz == java.lang.Long.class)
        {
            retValue = Long.TYPE;
        } else if (clazz == java.lang.Integer.class)
        {
            retValue = Integer.TYPE;
        } else if (clazz == java.lang.Boolean.class)
        {
            retValue = Boolean.TYPE;
        } else if (clazz == java.lang.Short.class)
        {
            retValue = Short.TYPE;
        } else if (clazz == java.lang.Byte.class)
        {
            retValue = Byte.TYPE;
        } else if (clazz == java.lang.Character.class)
        {
            retValue = Character.TYPE;
        } else if (clazz == java.lang.Double.class)
        {
            retValue = Double.TYPE;
        } else if (clazz == java.lang.Float.class)
        {
            retValue = Float.TYPE;
        }
        return retValue;
    }

    public static String getMethodFromProperty(String propertyName)
    {
        String methodName = propertyName;
        StringBuilder methodBuilder = new StringBuilder("set");
        methodBuilder.append(propertyName.substring(0, 1).toUpperCase());
        methodBuilder.append(propertyName.substring(1));
        methodName = methodBuilder.toString();

        return methodName;
    }

    /**
     * Returns true if a target for the specified member has already
     * been added. <p>
     *
     * @param member method or field that is an injection target
     */
    // d648283
    private boolean containsTarget(Member member)
    {
        if (ivInjectionTargets != null)
        {
            for (InjectionTarget target : ivInjectionTargets)
            {
                Member targetMember = target.getMember();

                if (targetMember.equals(member))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Internal method to isolate the class loading logic. <p>
     *
     * Returns the loaded class using the component specific ClassLoader,
     * or null if the specified class name was null or the empty string. <p>
     *
     * This method always returns null if the ClassLoader is null. <p>
     *
     * @param className name of the class to load
     **/
    // d446474
    protected final Class<?> loadClass(String className)
                    throws InjectionConfigurationException
    {
        ClassLoader classLoader = ivNameSpaceConfig.getClassLoader();
        if (className == null || className.equals("") || classLoader == null) // F743-32443
        {
            return null;
        }

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "loadClass : " + className);

        Class<?> loadedClass = null;

        try
        {
            loadedClass = classLoader.loadClass(className);
        } catch (ClassNotFoundException ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".loadClass",
                                        "675", this, new Object[] { className });
            InjectionConfigurationException icex = new InjectionConfigurationException
                            ("Referenced class could not be loaded : " + className, ex);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "loadClass : " + icex);
            throw icex;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "loadClass : " + loadedClass);

        return loadedClass;
    }

    /**
     * Determines if a binding has any injection targets. This method is only
     * intended to be called during {@link InjectionProcessor#resolve}.
     *
     * @return true if the binding has any injection targets
     */
    // d730349
    public boolean hasAnyInjectionTargets()
    {
        return ivInjectionTargets != null; // F87539
    }

    /**
     * Checks whether validation messages should be logged or not. <p>
     */
    // F50309.6
    protected final boolean isValidationLoggable()
    {
        return InjectionEngineAccessor.getInternalInstance().isValidationLoggable(ivCheckAppConfig);
    }

    /**
     * Checks whether more significant validation messages should
     * result in a failure or not. <p>
     */
    // F743-33178
    protected final boolean isValidationFailable()
    {
        return InjectionEngineAccessor.getInternalInstance().isValidationFailable(ivCheckAppConfig); // F50309.6
    }

    /**
     * Appropriately creates an InjectionTarget for a method, such that it
     * will handle either a single or multiple parameters. <p>
     *
     * @param method - injection target method (initializer).
     * @param binding - the binding this target will be associated with.
     */
    // F743-32637
    private InjectionTargetMethod createInjectionTarget(Method method,
                                                        InjectionBinding<?> binding)
                    throws InjectionException
    {
        if (method.getParameterTypes().length != 1) // d706744
        {
            return new InjectionTargetMultiParamMethod(method, binding);
        }
        return new InjectionTargetMethod(method, binding);
    }

    String toSimpleString() // d675172
    {
        return super.toString() + "[name=" + ivJndiName + ']';
    }

    @Override
    public String toString()
    {
        return super.toString() + "[name=" + ivJndiName + ", " + ivAnnotation + ']';
    }

    /**
     * Emit the customized human readable text to represent this object
     * in an FFDC incident report.
     *
     * @param is the incident stream, the data will be written here
     */
    // F49213
    @Override
    public void formatTo(IncidentStream is)
    {
        // -----------------------------------------------------------------------
        // Indicate the start of the dump, and include the identity
        // of InjectionBinding, so this can easily be matched to a trace.
        // -----------------------------------------------------------------------
        is.writeLine("", ">--- Start InjectionBinding Dump ---> " + Util.identity(this));
        is.writeLine("", "JndiName     = " + ivJndiName);
        is.writeLine("", "Annotation   = " + ivAnnotation);
        is.writeLine("", "Scope        = " + ivInjectionScope);
        is.writeLine("", "NameSpace    = " + ivJavaNameSpaceName);
        is.writeLine("", "Type         = " + ((ivInjectionClassType != null) ? ivInjectionClassType.getName()
                        : ivInjectionClassTypeName));
        is.writeLine("", "Resolved     = " + (ivNameSpaceConfig == null));

        is.writeLine("", "");
        is.writeLine("", "InjectedObject = " + Util.identity(ivInjectedObject));
        is.writeLine("", "BindingObject  = " + Util.identity(ivBindingObject));
        is.writeLine("", "ObjectFactory  = " + Util.identity(ivObjectFactory) + ", " +
                         ivObjectFactoryClass + ", " + ivObjectFactoryClassName);

        if (ivInjectionTargets != null)
        {
            is.writeLine("", "");
            is.writeLine("", "Injection Targets : " + ivInjectionTargets.size());
            for (InjectionTarget target : ivInjectionTargets)
            {
                is.writeLine("", "   " + target);
            }
        }

        is.writeLine("", "");
        is.writeLine("", ivNameSpaceConfig != null ? ivNameSpaceConfig.toString() : "ivNameSpaceConfig = null");

        is.writeLine("", "<--- InjectionBinding Dump Complete---< ");
    }

    /**
     * Convert an annotation to a string, but mask members named password.
     *
     * @param ann the annotation
     * @return the string representation of the annotation
     */
    public static String toStringSecure(Annotation ann) {
        Class<?> annType = ann.annotationType();

        StringBuilder sb = new StringBuilder();
        sb.append('@').append(annType.getName()).append('(');

        boolean any = false;
        for (Method m : annType.getMethods()) {
            Object defaultValue = m.getDefaultValue();
            if (defaultValue != null) {
                String name = m.getName();

                Object value;
                try {
                    value = m.invoke(ann);
                    if (name.equals("password") && !defaultValue.equals(value)) {
                        value = "********";
                    } else if (value instanceof Object[]) {
                        value = Arrays.toString((Object[]) value);
                    } else {
                        value = String.valueOf(value);
                    }
                } catch (Throwable t) {
                    value = "<" + t + ">";
                }

                if (any) {
                    sb.append(", ");
                } else {
                    any = true;
                }

                sb.append(name).append('=').append(value);
            }
        }

        return sb.append(')').toString();
    }

    /**
     * Method for injection cleanup such as some objects to be destroyed after the method invocation
     */
    public void cleanAfterMethodInvocation() {};

}
