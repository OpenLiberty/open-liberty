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
package org.apache.myfaces.cdi.util;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>This class contains utility methods to resolve contextual references
 * in situations where no injection is available because the
 * current class is not managed by the CDI Container. This can happen
 * in e.g. a JPA-2.0 EntityListener, a ServletFilter, a Spring managed
 * Bean, etc.</p>
 *
 * <p><b>Attention:</b> This method is intended for being used in user code at runtime.
 * If this method gets used during Container boot (in an Extension), non-portable
 * behaviour results. The CDI specification only allows injection of the
 * BeanManager during CDI-Container boot time.</p>
 *
 * @see BeanManagerProvider
 */
@Typed()
public final class BeanProvider
{
    //private static final Logger LOG = Logger.getLogger(BeanProvider.class.getName());

    /*
    private static final boolean LOG_DEPENDENT_WARNINGS;
    static {
        ProjectStage ps = ProjectStageProducer.getInstance().getProjectStage();
        LOG_DEPENDENT_WARNINGS = ps.equals(ProjectStage.Development) || ps.equals(ProjectStage.UnitTest);
    }*/

    private BeanProvider()
    {
        // this is a utility class which doesn't get instantiated.
    }

    /**
     * <p>Get a Contextual Reference by its type and qualifiers.
     * You can use this method to get contextual references of a given type.
     * A 'Contextual Reference' is a proxy which will automatically resolve
     * the correct contextual instance when you access any method.</p>
     *
     * <p><b>Attention:</b> You shall not use this method to manually resolve a
     * &#064;Dependent bean! The reason is that this contextual instances do usually
     * live in the well-defined lifecycle of their injection point (the bean they got
     * injected into). But if we manually resolve a &#064;Dependent bean, then it does <b>not</b>
     * belong to such a well defined lifecycle (because &#064;Dependent it is not
     * &#064;NormalScoped) and thus will not automatically be
     * destroyed at the end of the lifecycle. You need to manually destroy this contextual instance via
     * {@link javax.enterprise.context.spi.Contextual#destroy(Object, javax.enterprise.context.spi.CreationalContext)}.
     * Thus you also need to manually store the CreationalContext and the Bean you
     * used to create the contextual instance which this method will not provide.</p>
     *
     * @param type the type of the bean in question
     * @param qualifiers additional qualifiers which further distinct the resolved bean
     * @param <T> target type
     * @return the resolved Contextual Reference
     * @throws IllegalStateException if the bean could not be found.
     * @see #getContextualReference(Class, boolean, Annotation...)
     */
    /*
    public static <T> T getContextualReference(Class<T> type, Annotation... qualifiers)
    {
        return getContextualReference(type, false, qualifiers);
    }*/

    /**
     * {@link #getContextualReference(Class, Annotation...)} which returns <code>null</code> if the
     * 'optional' parameter is set to <code>true</code>.
     *
     * @param type the type of the bean in question
     * @param optional if <code>true</code> it will return <code>null</code> if no bean could be found or created.
     *                 Otherwise it will throw an {@code IllegalStateException}
     * @param qualifiers additional qualifiers which further distinct the resolved bean
     * @param <T> target type
     * @return the resolved Contextual Reference
     * @see #getContextualReference(Class, Annotation...)
     */
    /*
    public static <T> T getContextualReference(Class<T> type, boolean optional, Annotation... qualifiers)
    {
        BeanManager beanManager = getBeanManager();

        return getContextualReference(beanManager, type, optional, qualifiers);
    }*/

    /**
     * {@link #getContextualReference(Class, Annotation...)} which returns <code>null</code> if the
     * 'optional' parameter is set to <code>true</code>.
     * This method is intended for usage where the BeanManger is known, e.g. in Extensions.
     *
     * @param beanManager the BeanManager to use
     * @param type the type of the bean in question
     * @param optional if <code>true</code> it will return <code>null</code> if no bean could be found or created.
     *                 Otherwise it will throw an {@code IllegalStateException}
     * @param qualifiers additional qualifiers which further distinct the resolved bean
     * @param <T> target type
     * @return the resolved Contextual Reference
     * @see #getContextualReference(Class, Annotation...)
     */
    public static <T> T getContextualReference(BeanManager beanManager,
                                               Class<T> type,
                                               boolean optional,
                                               Annotation... qualifiers)
    {
        Set<Bean<?>> beans = beanManager.getBeans(type, qualifiers);

        if (beans == null || beans.isEmpty())
        {
            if (optional)
            {
                return null;
            }

            throw new IllegalStateException("Could not find beans for Type=" + type
                    + " and qualifiers:" + Arrays.toString(qualifiers));
        }

        return getContextualReference(type, beanManager, beans);
    }

    /**
     * <p>Get a Contextual Reference by its EL Name.
     * This only works for beans with the &#064;Named annotation.</p>
     *
     * <p><b>Attention:</b> please see the notes on manually resolving &#064;Dependent bean
     * in {@link #getContextualReference(Class, boolean, java.lang.annotation.Annotation...)}!</p>
     *
     * @param name     the EL name of the bean
     * @return the resolved Contextual Reference
     * @throws IllegalStateException if the bean could not be found.
     * @see #getContextualReference(String, boolean)
     */
    /*
    public static Object getContextualReference(String name)
    {
        return getContextualReference(name, false);
    }*/

    /**
     * <p>Get a Contextual Reference by its EL Name.
     * This only works for beans with the &#064;Named annotation.</p>
     *
     * <p><b>Attention:</b> please see the notes on manually resolving &#064;Dependent bean
     * in {@link #getContextualReference(Class, boolean, java.lang.annotation.Annotation...)}!</p>
     *
     * @param name     the EL name of the bean
     * @param optional if <code>true</code> it will return <code>null</code> if no bean could be found or created.
     *                 Otherwise it will throw an {@code IllegalStateException}
     * @return the resolved Contextual Reference
     */
    /*
    public static Object getContextualReference(String name, boolean optional)
    {
        return getContextualReference(name, optional, Object.class);
    }*/

    /**
     * <p>Get a Contextual Reference by its EL Name.
     * This only works for beans with the &#064;Named annotation.</p>
     *
     * <p><b>Attention:</b> please see the notes on manually resolving &#064;Dependent bean
     * in {@link #getContextualReference(Class, boolean, java.lang.annotation.Annotation...)}!</p>
     *
     *
     * @param name the EL name of the bean
     * @param optional if <code>true</code> it will return <code>null</code> if no bean could be found or created.
     *                 Otherwise it will throw an {@code IllegalStateException}
     * @param type the type of the bean in question - use {@link #getContextualReference(String, boolean)}
     *             if the type is unknown e.g. in dyn. use-cases
     * @param <T> target type
     * @return the resolved Contextual Reference
     */
    /*
    public static <T> T getContextualReference(String name, boolean optional, Class<T> type)
    {
        BeanManager beanManager = getBeanManager();
        Set<Bean<?>> beans = beanManager.getBeans(name);

        if (beans == null || beans.isEmpty())
        {
            if (optional)
            {
                return null;
            }

            throw new IllegalStateException("Could not find beans for Type=" + type
                    + " and name:" + name);
        }

        return getContextualReference(type, beanManager, beans);
    }*/

    /**
     * Get the Contextual Reference for the given bean.
     *
     * @param type the type of the bean in question
     * @param bean bean-definition for the contextual-reference
     * @param <T> target type
     * @return the resolved Contextual Reference
     */
    /*
    public static <T> T getContextualReference(Class<T> type, Bean<T> bean)
    {
        return getContextualReference(type, getBeanManager(), bean);
    }*/

    private static <T> T getContextualReference(Class<T> type, BeanManager beanManager, Bean<?> bean)
    {
        //noinspection unchecked
        return getContextualReference(type, beanManager, new HashSet<Bean<?>>((Collection) Arrays.asList(bean)));
    }

    /**
     * <p>Get a list of Contextual References by type independent of the qualifier
     * (including dependent scoped beans).
     *
     * You can use this method to get all contextual references of a given type.
     * A 'Contextual Reference' is a proxy which will automatically resolve
     * the correct contextual instance when you access any method.</p>
     *
     * <p><b>Attention:</b> please see the notes on manually resolving &#064;Dependent bean
     * in {@link #getContextualReference(Class, boolean, java.lang.annotation.Annotation...)}!</p>
     * <p><b>Attention:</b> This will also return instances of beans for which an Alternative
     * exists! The &#064;Alternative resolving is only done via {@link BeanManager#resolve(java.util.Set)}
     * which we cannot use in this case!</p>
     *
     * @param type the type of the bean in question
     * @param optional if <code>true</code> it will return an empty list if no bean could be found or created.
     *                 Otherwise it will throw an {@code IllegalStateException}
     * @param <T> target type
     * @return the resolved list of Contextual Reference or an empty-list if optional is true
     */
    /*
    public static <T> List<T> getContextualReferences(Class<T> type, boolean optional)
    {
        return getContextualReferences(type, optional, true);
    }*/

    /**
     * <p>Get a list of Contextual References by type independent of the qualifier.
     *
     * Further details are available at {@link #getContextualReferences(Class, boolean)}
     * <p><b>Attention:</b> please see the notes on manually resolving &#064;Dependent bean
     * in {@link #getContextualReference(Class, boolean, java.lang.annotation.Annotation...)}!</p>
     * <p><b>Attention:</b> This will also return instances of beans for which an Alternative
     * exists! The &#064;Alternative resolving is only done via {@link BeanManager#resolve(java.util.Set)}
     * which we cannot use in this case!</p>
     *
     * @param type the type of the bean in question
     * @param optional if <code>true</code> it will return an empty list if no bean could be found or created.
     *                 Otherwise it will throw an {@code IllegalStateException}
     * @param includeDefaultScopedBeans specifies if dependent scoped beans should be included in the result
     * @param <T> target type
     * @return the resolved list of Contextual Reference or an empty-list if optional is true
     */
    /*
    public static <T> List<T> getContextualReferences(Class<T> type,
                                                      boolean optional,
                                                      boolean includeDefaultScopedBeans)
    {
        BeanManager beanManager = getBeanManager();

        Set<Bean<T>> beans = getBeanDefinitions(type, optional, includeDefaultScopedBeans, beanManager);

        List<T> result = new ArrayList<T>(beans.size());

        for (Bean<?> bean : beans)
        {
            //noinspection unchecked
            result.add(getContextualReference(type, beanManager, bean));
        }
        return result;
    }*/

    /**
     * Get a set of {@link Bean} definitions by type independent of the qualifier.
     *
     * @param type the type of the bean in question
     * @param optional if <code>true</code> it will return an empty set if no bean could be found.
     *                 Otherwise it will throw an {@code IllegalStateException}
     * @param includeDefaultScopedBeans specifies if dependent scoped beans should be included in the result
     * @param <T> target type
     * @return the resolved set of {@link Bean} definitions or an empty-set if optional is true
     */
    /*
    public static <T> Set<Bean<T>> getBeanDefinitions(Class<T> type,
                                                      boolean optional,
                                                      boolean includeDefaultScopedBeans)
    {
        BeanManager beanManager = getBeanManager();
        
        return getBeanDefinitions(type, optional, includeDefaultScopedBeans, beanManager);
    }*/
    /*
    private static <T> Set<Bean<T>> getBeanDefinitions(Class<T> type,
                                                       boolean optional,
                                                       boolean includeDefaultScopedBeans,
                                                       BeanManager beanManager)
    {
        Set<Bean<?>> beans = beanManager.getBeans(type, new AnyLiteral());

        if (beans == null || beans.isEmpty())
        {
            if (optional)
            {
                return Collections.emptySet();
            }

            throw new IllegalStateException("Could not find beans for Type=" + type);
        }

        if (!includeDefaultScopedBeans)
        {
            beans = filterDefaultScopedBeans(beans);
        }
        
        Set<Bean<T>> result = new HashSet<Bean<T>>();
        
        for (Bean<?> bean : beans)
        {
            //noinspection unchecked
            result.add((Bean<T>) bean);
        }
        
        return result;
    }*/
    
    /**
     * Allows to perform dependency injection for instances which aren't managed by CDI.
     * <p/>
     * Attention:<br/>
     * The resulting instance isn't managed by CDI; only fields annotated with @Inject get initialized.
     *
     * @param instance current instance
     * @param <T> current type
     * @return instance with injected fields (if possible - or null if the given instance is null)
     *//*
    @SuppressWarnings("unchecked")
    public static <T> T injectFields(T instance)
    {
        if (instance == null)
        {
            return null;
        }

        BeanManager beanManager = getBeanManager();

        CreationalContext creationalContext = beanManager.createCreationalContext(null);

        AnnotatedType annotatedType = beanManager.createAnnotatedType(instance.getClass());
        InjectionTarget injectionTarget = beanManager.createInjectionTarget(annotatedType);
        injectionTarget.inject(instance, creationalContext);
        return instance;
    }*/

    /*
    private static Set<Bean<?>> filterDefaultScopedBeans(Set<Bean<?>> beans)
    {
        Set<Bean<?>> result = new HashSet<Bean<?>>(beans.size());

        Iterator<Bean<?>> beanIterator = beans.iterator();

        Bean<?> currentBean;
        while (beanIterator.hasNext())
        {
            currentBean = beanIterator.next();

            if (!Dependent.class.isAssignableFrom(currentBean.getScope()))
            {
                result.add(currentBean);
            }
        }
        return result;
    }*/

    /**
     * Internal helper method to resolve the right bean and resolve the contextual reference.
     *
     * @param type the type of the bean in question
     * @param beanManager current bean-manager
     * @param beans beans in question
     * @param <T> target type
     * @return the contextual reference
     */
    private static <T> T getContextualReference(Class<T> type, BeanManager beanManager, Set<Bean<?>> beans)
    {
        Bean<?> bean = beanManager.resolve(beans);

        //logWarningIfDependent(bean);

        CreationalContext<?> creationalContext = beanManager.createCreationalContext(bean);

        @SuppressWarnings({ "unchecked", "UnnecessaryLocalVariable" })
        T result = (T) beanManager.getReference(bean, type, creationalContext);
        return result;
    }

    /**
     * Log a warning if the produced creational instance is of
     * Scope &#064;Dependent as we cannot properly cleanup
     * the contextual instance afterwards.
     */
    /*
    private static void logWarningIfDependent(Bean<?> bean)
    {
        if (LOG_DEPENDENT_WARNINGS && bean.getScope().equals(Dependent.class))
        {
            LOG.log(Level.WARNING, "BeanProvider shall not be used to create @Dependent scoped beans. "
                    + "Bean: " + bean.toString());
        }
    }*/

    /**
     * Internal method to resolve the BeanManager via the {@link BeanManagerProvider}
     * @return current bean-manager
     */
    /*
    private static BeanManager getBeanManager()
    {
        return BeanManagerProvider.getInstance().getBeanManager();
    }*/
}
