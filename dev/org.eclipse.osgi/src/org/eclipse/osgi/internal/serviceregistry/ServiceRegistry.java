/*******************************************************************************
 * Copyright (c) 2004, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.serviceregistry;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.framework.eventmgr.CopyOnWriteIdentityMap;
import org.eclipse.osgi.framework.eventmgr.EventDispatcher;
import org.eclipse.osgi.framework.eventmgr.ListenerQueue;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.framework.BundleContextImpl;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.framework.FilterImpl;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.framework.hooks.service.ListenerHook.ListenerInfo;

/**
 * The Service Registry. This class is the main control point for service
 * layer operations in the framework.
 *
 * @ThreadSafe
 */
public class ServiceRegistry {
	public static final int SERVICEEVENT = 3;

	static final String listenerHookName = ListenerHook.class.getName();


	/** Published services by class name.
	 * The {@literal List<ServiceRegistrationImpl<?>>}s are both sorted
	 * in the natural order of ServiceRegistrationImpl and also are sets in that
	 * there must be no two entries in a List which are equal.
	 */
	/* @GuardedBy("this") */
	private final Map<String, List<ServiceRegistrationImpl<?>>> publishedServicesByClass;

	/** All published services.
	 * The List is both sorted in the natural order of ServiceRegistrationImpl and also is a
	 * set in that there must be no two entries in the List which are equal.
	 */
	/* @GuardedBy("this") */
	private final List<ServiceRegistrationImpl<?>> allPublishedServices;

	/** Published services by BundleContextImpl.
	 * The {@literal List<ServiceRegistrationImpl<?>>}s are NOT sorted
	 * and also are sets in that
	 * there must be no two entries in a List which are equal.
	 */
	/* @GuardedBy("this") */
	private final Map<BundleContextImpl, List<ServiceRegistrationImpl<?>>> publishedServicesByContext;

	/** next free service id. */
	/* @GuardedBy("this") */
	private long serviceid;

	/** Active Service Listeners.
	 * {@literal Map<BundleContextImpl,CopyOnWriteIdentityMap<ServiceListener,FilteredServiceListener>>}.
	 */
	/* @GuardedBy("serviceEventListeners") */
	private final Map<BundleContextImpl, CopyOnWriteIdentityMap<ServiceListener, FilteredServiceListener>> serviceEventListeners;

	/** initial capacity of the main data structure */
	private static final int initialCapacity = 50;
	/** initial capacity of the nested data structure */
	private static final int initialSubCapacity = 10;
	/** container which created this service registry */
	private final EquinoxContainer container;
	private final BundleContextImpl systemBundleContext;
	final Debug debug;

	/**
	 * Initializes the internal data structures of this ServiceRegistry.
	 *
	 */
	public ServiceRegistry(EquinoxContainer container) {
		this.container = container;
		this.debug = container.getConfiguration().getDebug();
		serviceid = 1;
		publishedServicesByClass = new HashMap<>(initialCapacity);
		publishedServicesByContext = new HashMap<>(initialCapacity);
		allPublishedServices = new ArrayList<>(initialCapacity);
		serviceEventListeners = new LinkedHashMap<>(initialCapacity);
		Module systemModule = container.getStorage().getModuleContainer().getModule(0);
		systemBundleContext = (BundleContextImpl) systemModule.getBundle().getBundleContext();
		systemBundleContext.provisionServicesInUseMap();
	}

	/**
	 * Registers the specified service object with the specified properties
	 * under the specified class names into the Framework. A
	 * <code>ServiceRegistrationImpl</code> object is returned. The
	 * <code>ServiceRegistrationImpl</code> object is for the private use of the
	 * bundle registering the service and should not be shared with other
	 * bundles. The registering bundle is defined to be the context bundle.
	 * Other bundles can locate the service by using either the
	 * {@link #getServiceReferences} or {@link #getServiceReference} method.
	 *
	 * <p>
	 * A bundle can register a service object that implements the
	 * {@link ServiceFactory} interface to have more flexibility in providing
	 * service objects to other bundles.
	 *
	 * <p>
	 * The following steps are required to register a service:
	 * <ol>
	 * <li>If <code>service</code> is not a <code>ServiceFactory</code>,
	 * an <code>IllegalArgumentException</code> is thrown if
	 * <code>service</code> is not an <code>instanceof</code> all the
	 * classes named.
	 * <li>The Framework adds these service properties to the specified
	 * <code>Dictionary</code> (which may be <code>null</code>): a property
	 * named {@link Constants#SERVICE_ID} identifying the registration number of
	 * the service and a property named {@link Constants#OBJECTCLASS} containing
	 * all the specified classes. If any of these properties have already been
	 * specified by the registering bundle, their values will be overwritten by
	 * the Framework.
	 * <li>The service is added to the Framework service registry and may now
	 * be used by other bundles.
	 * <li>A service event of type {@link ServiceEvent#REGISTERED} is fired.
	 * <li>A <code>ServiceRegistration</code> object for this registration is
	 * returned.
	 * </ol>
	 *
	 * @param context The BundleContext of the registering bundle.
	 * @param clazzes The class names under which the service can be located.
	 *        The class names in this array will be stored in the service's
	 *        properties under the key {@link Constants#OBJECTCLASS}.
	 * @param service The service object or a <code>ServiceFactory</code>
	 *        object.
	 * @param properties The properties for this service. The keys in the
	 *        properties object must all be <code>String</code> objects. See
	 *        {@link Constants} for a list of standard service property keys.
	 *        Changes should not be made to this object after calling this
	 *        method. To update the service's properties the
	 *        {@link ServiceRegistration#setProperties} method must be called.
	 *        The set of properties may be <code>null</code> if the service
	 *        has no properties.
	 *
	 * @return A <code>ServiceRegistrationImpl</code> object for use by the bundle
	 *         registering the service to update the service's properties or to
	 *         unregister the service.
	 *
	 * @throws java.lang.IllegalArgumentException If one of the following is
	 *         true:
	 *         <ul>
	 *         <li><code>service</code> is <code>null</code>.
	 *         <li><code>service</code> is not a <code>ServiceFactory</code>
	 *         object and is not an instance of all the named classes in
	 *         <code>clazzes</code>.
	 *         <li><code>properties</code> contains case variants of the same
	 *         key name.
	 *         </ul>
	 *
	 * @throws java.lang.SecurityException If the caller does not have the
	 *         <code>ServicePermission</code> to register the service for all
	 *         the named classes and the Java Runtime Environment supports
	 *         permissions.
	 *
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 *
	 * @see ServiceRegistration
	 * @see ServiceFactory
	 */
	public ServiceRegistrationImpl<?> registerService(BundleContextImpl context, String[] clazzes, Object service, Dictionary<String, ?> properties) {
		if (service == null) {
			if (debug.DEBUG_SERVICES) {
				Debug.println("Service object is null"); //$NON-NLS-1$
			}

			throw new IllegalArgumentException(Msg.SERVICE_ARGUMENT_NULL_EXCEPTION);
		}

		int size = clazzes.length;

		if (size == 0) {
			if (debug.DEBUG_SERVICES) {
				Debug.println("Classes array is empty"); //$NON-NLS-1$
			}

			throw new IllegalArgumentException(Msg.SERVICE_EMPTY_CLASS_LIST_EXCEPTION);
		}

		boolean isListenerHook = false;
		/* copy the array so that changes to the original will not affect us. */
		List<String> copy = new ArrayList<>(size);
		List<Class<?>> hookTypes = null;
		// intern the strings and remove duplicates
		for (int i = 0; i < size; i++) {
			String clazz = clazzes[i].intern();
			if (!copy.contains(clazz)) {
				isListenerHook = isListenerHook || listenerHookName.equals(clazz);
				hookTypes = getHookClass(clazz, hookTypes);
				copy.add(clazz);
			}
		}
		size = copy.size();
		clazzes = copy.toArray(new String[size]);

		/* check for ServicePermissions. */
		checkRegisterServicePermission(clazzes);

		if (!(service instanceof ServiceFactory<?>)) {
			String invalidService = checkServiceClass(clazzes, service);
			if (invalidService != null) {
				if (debug.DEBUG_SERVICES) {
					Debug.println("Service object is not an instanceof " + invalidService); //$NON-NLS-1$
				}
				throw new IllegalArgumentException(NLS.bind(Msg.SERVICE_NOT_INSTANCEOF_CLASS_EXCEPTION, invalidService));
			}
		}

		ServiceRegistrationImpl<?> registration = hookTypes != null
				? new ServiceRegistrationImpl.FrameworkHookRegistration<>(this, context, clazzes, service,
						systemBundleContext, hookTypes)
				: new ServiceRegistrationImpl<>(this, context, clazzes, service);
		registration.register(properties);
		registration.initHookInstance();

		if (isListenerHook) {
			notifyNewListenerHook(registration);
		}
		return registration;
	}

	@SuppressWarnings("deprecation")
	private List<Class<?>> getHookClass(String className, List<Class<?>> hookTypes) {
		switch (className) {
		case "org.osgi.framework.hooks.bundle.CollisionHook": //$NON-NLS-1$
			return addHook(org.osgi.framework.hooks.bundle.CollisionHook.class, hookTypes);
		case "org.osgi.framework.hooks.bundle.EventHook": //$NON-NLS-1$
			return addHook(org.osgi.framework.hooks.bundle.EventHook.class, hookTypes);
		case "org.osgi.framework.hooks.bundle.FindHook": //$NON-NLS-1$
			return addHook(org.osgi.framework.hooks.bundle.FindHook.class, hookTypes);
		case "org.osgi.framework.hooks.service.EventHook": //$NON-NLS-1$
			return addHook(org.osgi.framework.hooks.service.EventHook.class, hookTypes);
		case "org.osgi.framework.hooks.service.EventListenerHook": //$NON-NLS-1$
			return addHook(org.osgi.framework.hooks.service.EventListenerHook.class, hookTypes);
		case "org.osgi.framework.hooks.service.FindHook": //$NON-NLS-1$
			return addHook(org.osgi.framework.hooks.service.FindHook.class, hookTypes);
		case "org.osgi.framework.hooks.service.ListenerHook": //$NON-NLS-1$
			return addHook(org.osgi.framework.hooks.service.ListenerHook.class, hookTypes);
		case "org.osgi.framework.hooks.weaving.WeavingHook": //$NON-NLS-1$
			return addHook(org.osgi.framework.hooks.weaving.WeavingHook.class, hookTypes);
		case "org.osgi.framework.hooks.weaving.WovenClassListener": //$NON-NLS-1$
			return addHook(org.osgi.framework.hooks.weaving.WovenClassListener.class, hookTypes);
		default:
			return hookTypes;
		}
	}

	private List<Class<?>> addHook(Class<?> hookType, List<Class<?>> hookTypes) {
		if (hookTypes == null) {
			hookTypes = new ArrayList<>(1);
		}
		hookTypes.add(hookType);
		return hookTypes;
	}

	/**
	 * Returns an array of <code>ServiceReferenceImpl</code> objects. The returned
	 * array of <code>ServiceReferenceImpl</code> objects contains services that
	 * were registered under the specified class, match the specified filter
	 * criteria, and the packages for the class names under which the services
	 * were registered match the context bundle's packages as defined in
	 * {@link ServiceReference#isAssignableTo(Bundle, String)}.
	 *
	 * <p>
	 * The list is valid at the time of the call to this method, however since
	 * the Framework is a very dynamic environment, services can be modified or
	 * unregistered at anytime.
	 *
	 * <p>
	 * <code>filter</code> is used to select the registered service whose
	 * properties objects contain keys and values which satisfy the filter. See
	 * {@link Filter} for a description of the filter string syntax.
	 *
	 * <p>
	 * If <code>filter</code> is <code>null</code>, all registered services
	 * are considered to match the filter. If <code>filter</code> cannot be
	 * parsed, an {@link InvalidSyntaxException} will be thrown with a human
	 * readable message where the filter became unparsable.
	 *
	 * <p>
	 * The following steps are required to select a set of
	 * <code>ServiceReferenceImpl</code> objects:
	 * <ol>
	 * <li>If the filter string is not <code>null</code>, the filter string
	 * is parsed and the set <code>ServiceReferenceImpl</code> objects of
	 * registered services that satisfy the filter is produced. If the filter
	 * string is <code>null</code>, then all registered services are
	 * considered to satisfy the filter.
	 * <li>If the Java Runtime Environment supports permissions, the set of
	 * <code>ServiceReferenceImpl</code> objects produced by the previous step is
	 * reduced by checking that the caller has the
	 * <code>ServicePermission</code> to get at least one of the class names
	 * under which the service was registered. If the caller does not have the
	 * correct permission for a particular <code>ServiceReferenceImpl</code>
	 * object, then it is removed from the set.
	 * <li>If <code>clazz</code> is not <code>null</code>, the set is
	 * further reduced to those services that are an <code>instanceof</code>
	 * and were registered under the specified class. The complete list of
	 * classes of which a service is an instance and which were specified when
	 * the service was registered is available from the service's
	 * {@link Constants#OBJECTCLASS} property.
	 * <li>The set is reduced one final time by cycling through each
	 * <code>ServiceReference</code> object and calling
	 * {@link ServiceReference#isAssignableTo(Bundle, String)} with the context
	 * bundle and each class name under which the <code>ServiceReference</code>
	 * object was registered. For any given <code>ServiceReferenceImpl</code>
	 * object, if any call to
	 * {@link ServiceReference#isAssignableTo(Bundle, String)} returns
	 * <code>false</code>, then it is removed from the set of
	 * <code>ServiceReferenceImpl</code> objects.
	 * <li>An array of the remaining <code>ServiceReferenceImpl</code> objects is
	 * returned.
	 * </ol>
	 *
	 * @param context The BundleContext of the requesting bundle.
	 * @param clazz The class name with which the service was registered or
	 *        <code>null</code> for all services.
	 * @param filterstring The filter criteria.
	 * @param allservices True if the bundle called getAllServiceReferences.
	 * @return An array of <code>ServiceReferenceImpl</code> objects or
	 *         <code>null</code> if no services are registered which satisfy
	 *         the search.
	 * @throws InvalidSyntaxException If <code>filter</code> contains an
	 *         invalid filter string that cannot be parsed.
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 */
	public ServiceReferenceImpl<?>[] getServiceReferences(final BundleContextImpl context, final String clazz, final String filterstring, final boolean allservices) throws InvalidSyntaxException {
		if (debug.DEBUG_SERVICES) {
			Debug.println((allservices ? "getAllServiceReferences(" : "getServiceReferences(") + clazz + ", \"" + filterstring + "\")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		Filter filter = (filterstring == null) ? null : context.createFilter(filterstring);
		List<ServiceRegistrationImpl<?>> registrations = lookupServiceRegistrations(clazz, filter);
		List<ServiceReferenceImpl<?>> references = new ArrayList<>(registrations.size());
		for (ServiceRegistrationImpl<?> registration : registrations) {
			ServiceReferenceImpl<?> reference;
			try {
				reference = registration.getReferenceImpl();
			} catch (IllegalStateException e) {
				continue; // got unregistered, don't return reference
			}
			if (allservices || isAssignableTo(context, clazz, reference)) {
				try { /* test for permission to get the service */
					checkGetServicePermission(reference);
				} catch (SecurityException se) {
					continue; // don't return reference
				}
			} else {
				continue; // don't return reference
			}
			references.add(reference);
		}

		Collection<ServiceReferenceImpl<?>> copyReferences = references;
		if (context.getBundleImpl().getBundleId() == 0) {
			// Make a copy for the purposes of calling the hooks;
			// The the removals from the hooks are ignored for the system bundle
			copyReferences = new ArrayList<>(references);
		}
		Collection<ServiceReference<?>> shrinkable = new ShrinkableCollection<>(copyReferences);
		notifyFindHooks(context, clazz, filterstring, allservices, shrinkable);

		int size = references.size();
		if (size == 0) {
			return null;
		}
		return references.toArray(new ServiceReferenceImpl[size]);
	}

	/**
	 * Returns a <code>ServiceReference</code> object for a service that
	 * implements and was registered under the specified class.
	 *
	 * <p>
	 * This <code>ServiceReference</code> object is valid at the time of the
	 * call to this method, however as the Framework is a very dynamic
	 * environment, services can be modified or unregistered at anytime.
	 *
	 * <p>
	 * This method is the same as calling
	 * {@link BundleContext#getServiceReferences(String, String)} with a
	 * <code>null</code> filter string. It is provided as a convenience for
	 * when the caller is interested in any service that implements the
	 * specified class.
	 * <p>
	 * If multiple such services exist, the service with the highest ranking (as
	 * specified in its {@link Constants#SERVICE_RANKING} property) is returned.
	 * <p>
	 * If there is a tie in ranking, the service with the lowest service ID (as
	 * specified in its {@link Constants#SERVICE_ID} property); that is, the
	 * service that was registered first is returned.
	 *
	 * @param context The BundleContext of the requesting bundle.
	 * @param clazz The class name with which the service was registered.
	 * @return A <code>ServiceReference</code> object, or <code>null</code>
	 *         if no services are registered which implement the named class.
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 */
	public ServiceReferenceImpl<?> getServiceReference(BundleContextImpl context, String clazz) {
		if (debug.DEBUG_SERVICES) {
			Debug.println("getServiceReference(" + clazz + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		try {
			ServiceReferenceImpl<?>[] references = getServiceReferences(context, clazz, null, false);

			if (references != null) {
				// Since we maintain the registrations in a sorted List, the first element is always the
				// correct one to return.
				return references[0];
			}
		} catch (InvalidSyntaxException e) {
			if (debug.DEBUG_GENERAL) {
				Debug.println("InvalidSyntaxException w/ null filter" + e.getMessage()); //$NON-NLS-1$
				Debug.printStackTrace(e);
			}
		}

		return null;
	}

	/**
	 * Returns the specified service object for a service.
	 * <p>
	 * A bundle's use of a service is tracked by the bundle's use count of that
	 * service. Each time a service's service object is returned by
	 * {@link #getService(BundleContextImpl, ServiceReferenceImpl)} the context bundle's use count for
	 * that service is incremented by one. Each time the service is released by
	 * {@link #ungetService(BundleContextImpl, ServiceReferenceImpl)} the context bundle's use count
	 * for that service is decremented by one.
	 * <p>
	 * When a bundle's use count for a service drops to zero, the bundle should
	 * no longer use that service.
	 *
	 * <p>
	 * This method will always return <code>null</code> when the service
	 * associated with this <code>reference</code> has been unregistered.
	 *
	 * <p>
	 * The following steps are required to get the service object:
	 * <ol>
	 * <li>If the service has been unregistered, <code>null</code> is
	 * returned.
	 * <li>The context bundle's use count for this service is incremented by
	 * one.
	 * <li>If the context bundle's use count for the service is currently one
	 * and the service was registered with an object implementing the
	 * <code>ServiceFactory</code> interface, the
	 * {@link ServiceFactory#getService(Bundle, ServiceRegistration)} method is
	 * called to create a service object for the context bundle. This service
	 * object is cached by the Framework. While the context bundle's use count
	 * for the service is greater than zero, subsequent calls to get the
	 * services's service object for the context bundle will return the cached
	 * service object. <br>
	 * If the service object returned by the <code>ServiceFactory</code>
	 * object is not an <code>instanceof</code> all the classes named when the
	 * service was registered or the <code>ServiceFactory</code> object throws
	 * an exception, <code>null</code> is returned and a Framework event of
	 * type {@link FrameworkEvent#ERROR} containing a {@link ServiceException}
	 * describing the error is fired.
	 * <li>The service object for the service is returned.
	 * </ol>
	 *
	 * @param context The BundleContext of the requesting bundle.
	 * @param reference A reference to the service.
	 * @return A service object for the service associated with
	 *         <code>reference</code> or <code>null</code> if the service is
	 *         not registered, the service object returned by a
	 *         <code>ServiceFactory</code> does not implement the classes
	 *         under which it was registered or the <code>ServiceFactory</code>
	 *         threw an exception.
	 * @throws java.lang.SecurityException If the caller does not have the
	 *         <code>ServicePermission</code> to get the service using at
	 *         least one of the named classes the service was registered under
	 *         and the Java Runtime Environment supports permissions.
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 * @see #ungetService(BundleContextImpl, ServiceReferenceImpl)
	 * @see ServiceFactory
	 */
	public <S> S getService(BundleContextImpl context, ServiceReferenceImpl<S> reference) {
		/* test for permission to get the service */
		checkGetServicePermission(reference);
		return reference.getRegistration().getService(context, ServiceConsumer.singletonConsumer);
	}

	/**
	 * Returns the {@link ServiceObjects} object for the service referenced by
	 * the specified {@code ServiceReference} object.
	 *
	 * <p>
	 * The {@link ServiceObjects} object can be used to obtain multiple
	 * service objects for services with {@link Constants#SCOPE_PROTOTYPE
	 * prototype} scope. For services with {@link Constants#SCOPE_SINGLETON
	 * singleton} or {@link Constants#SCOPE_BUNDLE bundle} scope, the
	 * {@link ServiceObjects#getService()} method behaves the same as the
	 * {@link BundleContext#getService(ServiceReference)} method and the
	 * {@link ServiceObjects#ungetService(Object)} method behaves the same as
	 * the {@link BundleContext#ungetService(ServiceReference)} method. That is, only one,
	 * use-counted service object is available from the {@link ServiceObjects}
	 * object.
	 *
	 * <p>
	 * This method will always return {@code null} when the service associated
	 * with the specified {@code reference} has been unregistered.
	 *
	 * @param <S> Type of Service.
	 * @param context The BundleContext of the requesting bundle.
	 * @param reference A reference to the service.
	 * @return A {@link ServiceObjects} object for the service associated with
	 *         the specified {@code reference} or {@code null} if the service is
	 *         not registered.
	 * @throws SecurityException If the caller does not have the
	 *         {@code ServicePermission} to get the service using at least one
	 *         of the named classes the service was registered under and the
	 *         Java Runtime Environment supports permissions.
	 */
	public <S> ServiceObjectsImpl<S> getServiceObjects(BundleContextImpl context, ServiceReferenceImpl<S> reference) {
		checkGetServicePermission(reference);
		return reference.getRegistration().getServiceObjects(context);
	}

	/**
	 * Releases the service object referenced by the specified
	 * <code>ServiceReference</code> object. If the context bundle's use count
	 * for the service is zero, this method returns <code>false</code>.
	 * Otherwise, the context bundle's use count for the service is decremented
	 * by one.
	 *
	 * <p>
	 * The service's service object should no longer be used and all references
	 * to it should be destroyed when a bundle's use count for the service drops
	 * to zero.
	 *
	 * <p>
	 * The following steps are required to unget the service object:
	 * <ol>
	 * <li>If the context bundle's use count for the service is zero or the
	 * service has been unregistered, <code>false</code> is returned.
	 * <li>The context bundle's use count for this service is decremented by
	 * one.
	 * <li>If the context bundle's use count for the service is currently zero
	 * and the service was registered with a <code>ServiceFactory</code>
	 * object, the
	 * {@link ServiceFactory#ungetService(Bundle, ServiceRegistration, Object)}
	 * method is called to release the service object for the context bundle.
	 * <li><code>true</code> is returned.
	 * </ol>
	 *
	 * @param context The BundleContext of the requesting bundle.
	 * @param reference A reference to the service to be released.
	 * @return <code>false</code> if the context bundle's use count for the
	 *         service is zero or if the service has been unregistered;
	 *         <code>true</code> otherwise.
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 * @see #getService
	 * @see ServiceFactory
	 */
	public boolean ungetService(BundleContextImpl context, ServiceReferenceImpl<?> reference) {
		ServiceRegistrationImpl<?> registration = reference.getRegistration();

		return registration.ungetService(context, ServiceConsumer.singletonConsumer, null);
	}

	/**
	 * Returns this bundle's <code>ServiceReference</code> list for all
	 * services it has registered or <code>null</code> if this bundle has no
	 * registered services.
	 *
	 * <p>
	 * If the Java runtime supports permissions, a <code>ServiceReference</code>
	 * object to a service is included in the returned list only if the caller
	 * has the <code>ServicePermission</code> to get the service using at
	 * least one of the named classes the service was registered under.
	 *
	 * <p>
	 * The list is valid at the time of the call to this method, however, as the
	 * Framework is a very dynamic environment, services can be modified or
	 * unregistered at anytime.
	 *
	 * @param context The BundleContext of the requesting bundle.
	 * @return An array of <code>ServiceReference</code> objects or
	 *         <code>null</code>.
	 * @throws java.lang.IllegalStateException If this bundle has been
	 *         uninstalled.
	 * @see ServiceRegistration
	 * @see ServiceReference
	 * @see ServicePermission
	 */
	public ServiceReferenceImpl<?>[] getRegisteredServices(BundleContextImpl context) {
		List<ServiceRegistrationImpl<?>> registrations = lookupServiceRegistrations(context);
		List<ServiceReferenceImpl<?>> references = new ArrayList<>(registrations.size());
		for (ServiceRegistrationImpl<?> registration : registrations) {
			ServiceReferenceImpl<?> reference;
			try {
				reference = registration.getReferenceImpl();
			} catch (IllegalStateException e) {
				continue; // got unregistered, don't return reference
			}
			try {
				/* test for permission to get the service */
				checkGetServicePermission(reference);
			} catch (SecurityException se) {
				continue; // don't return reference
			}
			references.add(reference);
		}

		int size = references.size();
		if (size == 0) {
			return null;
		}
		return references.toArray(new ServiceReferenceImpl[size]);
	}

	/**
	 * Returns this bundle's <code>ServiceReference</code> list for all
	 * services it is using or returns <code>null</code> if this bundle is not
	 * using any services. A bundle is considered to be using a service if its
	 * use count for that service is greater than zero.
	 *
	 * <p>
	 * If the Java Runtime Environment supports permissions, a
	 * <code>ServiceReference</code> object to a service is included in the
	 * returned list only if the caller has the <code>ServicePermission</code>
	 * to get the service using at least one of the named classes the service
	 * was registered under.
	 * <p>
	 * The list is valid at the time of the call to this method, however, as the
	 * Framework is a very dynamic environment, services can be modified or
	 * unregistered at anytime.
	 *
	 * @param context The BundleContext of the requesting bundle.
	 * @return An array of <code>ServiceReference</code> objects or
	 *         <code>null</code>.
	 * @throws java.lang.IllegalStateException If this bundle has been
	 *         uninstalled.
	 * @see ServiceReference
	 * @see ServicePermission
	 */
	public ServiceReferenceImpl<?>[] getServicesInUse(BundleContextImpl context) {
		Map<ServiceRegistrationImpl<?>, ServiceUse<?>> servicesInUse = context.getServicesInUseMap();
		if (servicesInUse == null) {
			return null;
		}

		List<ServiceRegistrationImpl<?>> registrations;
		synchronized (servicesInUse) {
			if (servicesInUse.isEmpty()) {
				return null;
			}
			registrations = new ArrayList<>(servicesInUse.keySet());
		}
		List<ServiceReferenceImpl<?>> references = new ArrayList<>(registrations.size());
		for (ServiceRegistrationImpl<?> registration : registrations) {
			ServiceReferenceImpl<?> reference;
			try {
				reference = registration.getReferenceImpl();
			} catch (IllegalStateException e) {
				continue; // got unregistered, don't return reference
			}
			try {
				/* test for permission to get the service */
				checkGetServicePermission(reference);
			} catch (SecurityException se) {
				continue; // don't return reference
			}
			references.add(reference);
		}

		int size = references.size();
		if (size == 0) {
			return null;
		}
		return references.toArray(new ServiceReferenceImpl[size]);
	}

	/**
	 * Called when the BundleContext is closing to unregister all services
	 * currently registered by the bundle.
	 *
	 * @param context The BundleContext of the closing bundle.
	 */
	public void unregisterServices(BundleContextImpl context) {
		for (ServiceRegistrationImpl<?> registration : lookupServiceRegistrations(context)) {
			try {
				registration.unregister();
			} catch (IllegalStateException e) {
				/* already unregistered */
			}
		}
		removeServiceRegistrations(context); // remove empty list
	}

	/**
	 * Called when the BundleContext is closing to unget all services
	 * currently used by the bundle.
	 *
	 * @param context The BundleContext of the closing bundle.
	 */
	public void releaseServicesInUse(BundleContextImpl context) {
		Map<ServiceRegistrationImpl<?>, ServiceUse<?>> servicesInUse = context.getServicesInUseMap();
		if (servicesInUse == null) {
			return;
		}
		List<ServiceRegistrationImpl<?>> registrations;
		synchronized (servicesInUse) {
			if (servicesInUse.isEmpty()) {
				return;
			}
			registrations = new ArrayList<>(servicesInUse.keySet());
		}
		if (debug.DEBUG_SERVICES) {
			Debug.println("Releasing services"); //$NON-NLS-1$
		}
		for (ServiceRegistrationImpl<?> registration : registrations) {
			registration.releaseService(context);
		}
	}

	/**
	 * Add a new Service Listener for a bundle.
	 *
	 * @param context Context of bundle adding listener.
	 * @param listener Service Listener to be added.
	 * @param filter Filter string for listener or null.
	 * @throws InvalidSyntaxException If the filter string is invalid.
	 */
	public void addServiceListener(BundleContextImpl context, ServiceListener listener, String filter) throws InvalidSyntaxException {
		if (debug.DEBUG_EVENTS) {
			String listenerName = listener.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(listener)); //$NON-NLS-1$
			Debug.println("addServiceListener[" + context.getBundleImpl() + "](" + listenerName + ", \"" + filter + "\")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}

		FilteredServiceListener filteredListener = new FilteredServiceListener(context, listener, filter);
		FilteredServiceListener oldFilteredListener;
		synchronized (serviceEventListeners) {
			CopyOnWriteIdentityMap<ServiceListener, FilteredServiceListener> listeners = serviceEventListeners.get(context);
			if (listeners == null) {
				listeners = new CopyOnWriteIdentityMap<>();
				serviceEventListeners.put(context, listeners);
			}
			oldFilteredListener = listeners.put(listener, filteredListener);
		}

		if (oldFilteredListener != null) {
			oldFilteredListener.markRemoved();
			Collection<ListenerInfo> removedListeners = Collections.singletonList(oldFilteredListener);
			notifyListenerHooks(removedListeners, false);
		}

		Collection<ListenerInfo> addedListeners = Collections.singletonList(filteredListener);
		notifyListenerHooks(addedListeners, true);
	}

	/**
	 * Remove a Service Listener for a bundle.
	 *
	 * @param context Context of bundle removing listener.
	 * @param listener Service Listener to be removed.
	 */
	public void removeServiceListener(BundleContextImpl context, ServiceListener listener) {
		if (debug.DEBUG_EVENTS) {
			String listenerName = listener.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(listener)); //$NON-NLS-1$
			Debug.println("removeServiceListener[" + context.getBundleImpl() + "](" + listenerName + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		FilteredServiceListener oldFilteredListener;
		synchronized (serviceEventListeners) {
			Map<ServiceListener, FilteredServiceListener> listeners = serviceEventListeners.get(context);
			if (listeners == null) {
				return; // this context has no listeners to begin with
			}
			oldFilteredListener = listeners.remove(listener);
		}

		if (oldFilteredListener == null) {
			return;
		}
		oldFilteredListener.markRemoved();
		Collection<ListenerInfo> removedListeners = Collections.singletonList(oldFilteredListener);
		notifyListenerHooks(removedListeners, false);
	}

	/**
	 * Remove all Service Listener for a bundle.
	 *
	 * @param context Context of bundle removing all listeners.
	 */
	public void removeAllServiceListeners(BundleContextImpl context) {
		Map<ServiceListener, FilteredServiceListener> removedListenersMap;
		synchronized (serviceEventListeners) {
			removedListenersMap = serviceEventListeners.remove(context);
		}
		if ((removedListenersMap == null) || removedListenersMap.isEmpty()) {
			return;
		}
		Collection<FilteredServiceListener> removedListeners = removedListenersMap.values();
		for (FilteredServiceListener oldFilteredListener : removedListeners) {
			oldFilteredListener.markRemoved();
		}
		notifyListenerHooks(asListenerInfos(removedListeners), false);
	}

	/**
	 * Coerce the generic type of a collection from Collection<FilteredServiceListener>
	 * to Collection<ListenerInfo>
	 * @param c Collection to be coerced.
	 * @return c coerced to Collection<ListenerInfo>
	 */
	@SuppressWarnings("unchecked")
	private static Collection<ListenerInfo> asListenerInfos(Collection<? extends ListenerInfo> c) {
		return (Collection<ListenerInfo>) c;
	}

	/**
	 * Deliver a ServiceEvent.
	 *
	 * @param event The ServiceEvent to deliver.
	 */
	public void publishServiceEvent(final ServiceEvent event) {
		if (System.getSecurityManager() == null) {
			publishServiceEventPrivileged(event);
		} else {
			AccessController.doPrivileged(new PrivilegedAction<Void>() {
				@Override
				public Void run() {
					publishServiceEventPrivileged(event);
					return null;
				}
			});
		}
	}

	void publishServiceEventPrivileged(final ServiceEvent event) {
		/* Build the listener snapshot */
		Map<BundleContextImpl, Set<Map.Entry<ServiceListener, FilteredServiceListener>>> listenerSnapshot;
		Set<Map.Entry<ServiceListener, FilteredServiceListener>> systemServiceListenersOrig = null;
		BundleContextImpl systemContext = null;
		synchronized (serviceEventListeners) {
			listenerSnapshot = new LinkedHashMap<>(serviceEventListeners.size());
			for (Map.Entry<BundleContextImpl, CopyOnWriteIdentityMap<ServiceListener, FilteredServiceListener>> entry : serviceEventListeners.entrySet()) {
				Map<ServiceListener, FilteredServiceListener> listeners = entry.getValue();
				if (!listeners.isEmpty()) {
					if (entry.getKey().getBundleImpl().getBundleId() == 0) {
						systemContext = entry.getKey();
						// make a copy that we can use to discard hook removals later
						systemServiceListenersOrig = listeners.entrySet();
					}
					listenerSnapshot.put(entry.getKey(), listeners.entrySet());
				}
			}
		}

		/* shrink the snapshot.
		 * keySet returns a Collection which cannot be added to and
		 * removals from that collection will result in removals of the
		 * entry from the snapshot.
		 */
		Collection<BundleContext> contexts = asBundleContexts(listenerSnapshot.keySet());
		notifyEventHooksPrivileged(event, contexts);
		if (!listenerSnapshot.isEmpty()) {
			Map<BundleContext, Collection<ListenerInfo>> listeners = new ShrinkableValueCollectionMap<>(
					listenerSnapshot);
			notifyEventListenerHooksPrivileged(event, listeners);
		}
		// always add back the system service listeners if they were removed
		if (systemServiceListenersOrig != null) {
			// No contains key check is done because hooks may have removed
			// a single listener from the value instead of the whole context key.
			// It is more simple to just replace with the original snapshot.
			listenerSnapshot.put(systemContext, systemServiceListenersOrig);
		}
		if (listenerSnapshot.isEmpty()) {
			return;
		}

		/* deliver the event to the snapshot */
		ListenerQueue<ServiceListener, FilteredServiceListener, ServiceEvent> queue = container.newListenerQueue();
		for (Map.Entry<BundleContextImpl, Set<Map.Entry<ServiceListener, FilteredServiceListener>>> entry : listenerSnapshot.entrySet()) {
			@SuppressWarnings({"unchecked", "rawtypes"})
			EventDispatcher<ServiceListener, FilteredServiceListener, ServiceEvent> dispatcher = (EventDispatcher) entry.getKey();
			Set<Map.Entry<ServiceListener, FilteredServiceListener>> listenerSet = entry.getValue();
			queue.queueListeners(listenerSet, dispatcher);
		}
		queue.dispatchEventSynchronous(SERVICEEVENT, event);
	}

	/**
	 * Coerce the generic type of a collection from Collection<BundleContextImpl>
	 * to Collection<BundleContext>
	 * @param c Collection to be coerced.
	 * @return c coerced to Collection<BundleContext>
	 */
	@SuppressWarnings("unchecked")
	private static Collection<BundleContext> asBundleContexts(Collection<? extends BundleContext> c) {
		return (Collection<BundleContext>) c;
	}

	/**
	 * Return the next available service id.
	 *
	 * @return next service id.
	 */
	synchronized long getNextServiceId() {
		long id = serviceid;
		serviceid = id + 1;
		return id;
	}

	/**
	 * Add the ServiceRegistrationImpl to the data structure.
	 *
	 * @param context The BundleContext of the bundle registering the service.
	 * @param registration The new ServiceRegistration.
	 */
	/* @GuardedBy("this") */
	void addServiceRegistration(BundleContextImpl context, ServiceRegistrationImpl<?> registration) {
		assert Thread.holdsLock(this);
		// Add the ServiceRegistrationImpl to the list of Services published by BundleContextImpl.
		List<ServiceRegistrationImpl<?>> contextServices = publishedServicesByContext.get(context);
		if (contextServices == null) {
			contextServices = new ArrayList<>(initialSubCapacity);
			publishedServicesByContext.put(context, contextServices);
		}
		// The list is NOT sorted, so we just add
		contextServices.add(registration);

		// Add the ServiceRegistrationImpl to the list of Services published by Class Name.
		int insertIndex;
		for (String clazz : registration.getClasses()) {
			List<ServiceRegistrationImpl<?>> services = publishedServicesByClass.get(clazz);

			if (services == null) {
				services = new ArrayList<>(initialSubCapacity);
				publishedServicesByClass.put(clazz, services);
			}

			// The list is sorted, so we must find the proper location to insert
			insertIndex = -Collections.binarySearch(services, registration) - 1;
			services.add(insertIndex, registration);
		}

		// Add the ServiceRegistrationImpl to the list of all published Services.
		// The list is sorted, so we must find the proper location to insert
		insertIndex = -Collections.binarySearch(allPublishedServices, registration) - 1;
		allPublishedServices.add(insertIndex, registration);
	}

	/**
	 * Modify the ServiceRegistrationImpl in the data structure.
	 *
	 * @param context The BundleContext of the bundle registering the service.
	 * @param registration The modified ServiceRegistration.
	 */
	/* @GuardedBy("this") */
	void modifyServiceRegistration(BundleContextImpl context, ServiceRegistrationImpl<?> registration,
			int previousRanking) {
		assert Thread.holdsLock(this);
		// The list of Services published by BundleContextImpl is not sorted, so
		// we do not need to modify it.

		// If the insert location has changed
		if (registration.compareTo(previousRanking, registration.getId()) != 0) {
			// Remove the ServiceRegistrationImpl from the list of Services published by
			// Class Name
			// and then add at the correct index.
			int insertIndex;
			for (String clazz : registration.getClasses()) {
				List<ServiceRegistrationImpl<?>> services = publishedServicesByClass.get(clazz);
				services.remove(registration);
				// The list is sorted, so we must find the proper location to insert
				insertIndex = -1 - Collections.binarySearch(services, registration);
				services.add(insertIndex, registration);
			}

			// Remove the ServiceRegistrationImpl from the list of all published Services
			// and then add at the correct index.
			allPublishedServices.remove(registration);
			// The list is sorted, so we must find the proper location to insert
			insertIndex = -1 - Collections.binarySearch(allPublishedServices, registration);
			allPublishedServices.add(insertIndex, registration);
		}
	}

	/**
	 * Remove the ServiceRegistrationImpl from the data structure.
	 *
	 * @param context The BundleContext of the bundle registering the service.
	 * @param registration The ServiceRegistration to remove.
	 */
	/* @GuardedBy("this") */
	void removeServiceRegistration(BundleContextImpl context, ServiceRegistrationImpl<?> registration) {
		assert Thread.holdsLock(this);
		// Remove the ServiceRegistrationImpl from the list of Services published by BundleContextImpl.
		List<ServiceRegistrationImpl<?>> contextServices = publishedServicesByContext.get(context);
		if (contextServices != null) {
			contextServices.remove(registration);
		}

		// Remove the ServiceRegistrationImpl from the list of Services published by Class Name.
		for (String clazz : registration.getClasses()) {
			List<ServiceRegistrationImpl<?>> services = publishedServicesByClass.get(clazz);
			services.remove(registration);
			if (services.isEmpty()) { // remove empty list
				publishedServicesByClass.remove(clazz);
			}
		}

		// Remove the ServiceRegistrationImpl from the list of all published Services.
		allPublishedServices.remove(registration);
	}

	/**
	 * Lookup Service Registrations in the data structure by class name and filter.
	 *
	 * @param clazz The class name with which the service was registered or
	 *        <code>null</code> for all services.
	 * @param filter The filter criteria.
	 * @return List<ServiceRegistrationImpl>
	 */
	private List<ServiceRegistrationImpl<?>> lookupServiceRegistrations(String clazz, Filter filter) {
		List<ServiceRegistrationImpl<?>> result;
		synchronized (this) {
			if (clazz == null) {
				if (filter instanceof FilterImpl) {
					// check if we can determine the clazz from the filter
					String filterObjectClazz = ((FilterImpl) filter).getRequiredObjectClass();
					if (filterObjectClazz != null) {
						result = publishedServicesByClass.get(filterObjectClazz);
						if (((FilterImpl) filter).getChildren().isEmpty()) {
							// this is a simple (objectClass=serviceClass) filter;
							// no need to evaluate the filter
							filter = null;
						}
					} else {
					  result = allPublishedServices;
					}
				} else {
					// have to check all services
					result = allPublishedServices;
				}
			} else {
				/* services registered under the class name */
				result = publishedServicesByClass.get(clazz);
			}

			if ((result == null) || result.isEmpty()) {
				return Collections.emptyList();
			}

			result = new LinkedList<>(result); /* make a new list since we don't want to change the real list */
		}

		if (filter == null) {
			return result;
		}

		for (Iterator<ServiceRegistrationImpl<?>> iter = result.iterator(); iter.hasNext();) {
			ServiceRegistrationImpl<?> registration = iter.next();
			ServiceReferenceImpl<?> reference;
			try {
				reference = registration.getReferenceImpl();
			} catch (IllegalStateException e) {
				iter.remove(); /* service was unregistered after we left the synchronized block above */
				continue;
			}
			if (!filter.match(reference)) {
				iter.remove();
			}
		}
		return result;
	}

	/**
	 * Lookup Service Registrations in the data structure by BundleContext.
	 *
	 * @param context The BundleContext for which to return Service Registrations.
	 * @return List<ServiceRegistrationImpl>
	 */
	private synchronized List<ServiceRegistrationImpl<?>> lookupServiceRegistrations(BundleContextImpl context) {
		List<ServiceRegistrationImpl<?>> result = publishedServicesByContext.get(context);

		if ((result == null) || result.isEmpty()) {
			return Collections.emptyList();
		}

		return new ArrayList<>(result); /* make a new list since we don't want to change the real list */
	}

	/**
	 * Remove Service Registrations in the data structure by BundleContext.
	 *
	 * @param context The BundleContext for which to remove Service Registrations.
	 */
	private synchronized void removeServiceRegistrations(BundleContextImpl context) {
		publishedServicesByContext.remove(context);
	}

	/**
	 * Check for permission to register a service.
	 *
	 * The caller must have permission for ALL names.
	 */
	private static void checkRegisterServicePermission(String[] names) {
		SecurityManager sm = System.getSecurityManager();
		if (sm == null) {
			return;
		}
		for (int i = 0, len = names.length; i < len; i++) {
			sm.checkPermission(new ServicePermission(names[i], ServicePermission.REGISTER));
		}
	}

	/**
	 * Check for permission to get a service.
	 */
	private static void checkGetServicePermission(ServiceReference<?> reference) {
		SecurityManager sm = System.getSecurityManager();
		if (sm == null) {
			return;
		}
		sm.checkPermission(new ServicePermission(reference, ServicePermission.GET));
	}

	/**
	 * Check for permission to listen to a service.
	 */
	static boolean hasListenServicePermission(ServiceEvent event, BundleContextImpl context) {
		ModuleRevision revision = context.getBundleImpl().getModule().getCurrentRevision();
		if (revision == null) {
			return false;
		}
		ProtectionDomain domain = ((Generation) revision.getRevisionInfo()).getDomain();
		if (domain == null) {
			return true;
		}

		return domain.implies(new ServicePermission(event.getServiceReference(), ServicePermission.GET));
	}

	/**
	 * Return the name of the class that is not satisfied by the service object.
	 * @param clazzes Array of class names.
	 * @param serviceObject Service object.
	 * @return The name of the class that is not satisfied by the service object.
	 */
	static String checkServiceClass(final String[] clazzes, final Object serviceObject) {
		ClassLoader cl = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
			@Override
			public ClassLoader run() {
				return serviceObject.getClass().getClassLoader();
			}
		});
		for (int i = 0, len = clazzes.length; i < len; i++) {
			try {
				Class<?> serviceClazz = cl == null ? Class.forName(clazzes[i]) : cl.loadClass(clazzes[i]);
				if (!serviceClazz.isInstance(serviceObject))
					return clazzes[i];
			} catch (ClassNotFoundException e) {
				//This check is rarely done
				if (extensiveCheckServiceClass(clazzes[i], serviceObject.getClass()))
					return clazzes[i];
			}
		}
		return null;
	}

	private static boolean extensiveCheckServiceClass(String clazz, Class<?> serviceClazz) {
		if (clazz.equals(serviceClazz.getName()))
			return false;
		Class<?>[] interfaces = serviceClazz.getInterfaces();
		for (int i = 0, len = interfaces.length; i < len; i++)
			if (!extensiveCheckServiceClass(clazz, interfaces[i]))
				return false;
		Class<?> superClazz = serviceClazz.getSuperclass();
		if (superClazz != null)
			if (!extensiveCheckServiceClass(clazz, superClazz))
				return false;
		return true;
	}

	static boolean isAssignableTo(BundleContextImpl context, String clazz, ServiceReferenceImpl<?> reference) {
		Bundle bundle = context.getBundleImpl();
		String[] clazzes = reference.getClasses();
		for (int i = 0, len = clazzes.length; i < len; i++)
			if (!reference.getRegistration().isAssignableTo(bundle, clazzes[i], clazzes[i] == clazz))
				return false;
		return true;
	}

	/**
	 * Call the registered FindHook services to allow them to inspect and possibly shrink the result.
	 * The FindHook must be called in order: descending by service.ranking, then ascending by service.id.
	 * This is the natural order for ServiceReference.
	 *
	 * @param context The context of the bundle getting the service references.
	 * @param clazz The class name used to search for the service references.
	 * @param filterstring The filter used to search for the service references.
	 * @param allservices True if getAllServiceReferences called.
	 * @param result The result to return to the caller which may have been shrunk by the FindHooks.
	 */
	private void notifyFindHooks(final BundleContextImpl context, final String clazz, final String filterstring, final boolean allservices, final Collection<ServiceReference<?>> result) {
		if (System.getSecurityManager() == null) {
			notifyFindHooksPrivileged(context, clazz, filterstring, allservices, result);
		} else {
			AccessController.doPrivileged(new PrivilegedAction<Void>() {
				@Override
				public Void run() {
					notifyFindHooksPrivileged(context, clazz, filterstring, allservices, result);
					return null;
				}
			});
		}
	}

	void notifyFindHooksPrivileged(final BundleContextImpl context, final String clazz, final String filterstring, final boolean allservices, final Collection<ServiceReference<?>> result) {
		if (debug.DEBUG_HOOKS) {
			Debug.println("notifyServiceFindHooks(" + context.getBundleImpl() + "," + clazz + "," + filterstring + "," + allservices + "," + result + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		}
		notifyHooksPrivileged(FindHook.class, "find", //$NON-NLS-1$
				(hook, hookRegistration) -> hook.find(context, clazz, filterstring, allservices, result));
	}

	/**
	 * Call the registered EventHook services to allow them to inspect and possibly shrink the result.
	 * The EventHooks must be called in order: descending by service.ranking, then ascending by service.id.
	 * This is the natural order for ServiceReference.
	 *
	 * @param event The service event to be delivered.
	 * @param result The result to return to the caller which may have been shrunk by the EventHooks.
	 */
	@SuppressWarnings("deprecation")
	private void notifyEventHooksPrivileged(final ServiceEvent event, final Collection<BundleContext> result) {
		if (debug.DEBUG_HOOKS) {
			Debug.println("notifyServiceEventHooks(" + event.getType() + ":" + event.getServiceReference() + "," + result + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		notifyHooksPrivileged(org.osgi.framework.hooks.service.EventHook.class, "event", //$NON-NLS-1$
				(hook, hookRegistration) -> hook.event(event, result));
	}

	/**
	 * Call the registered EventListenerHook services to allow them to inspect and possibly shrink the result.
	 * The EventListenerHooks must be called in order: descending by service.ranking, then ascending by service.id.
	 * This is the natural order for ServiceReference.
	 *
	 * @param event The service event to be delivered.
	 * @param result The result to return to the caller which may have been shrunk by the EventListenerHooks.
	 */
	private void notifyEventListenerHooksPrivileged(final ServiceEvent event, final Map<BundleContext, Collection<ListenerInfo>> result) {
		if (debug.DEBUG_HOOKS) {
			Debug.println("notifyServiceEventListenerHooks(" + event.getType() + ":" + event.getServiceReference() + "," + result + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		notifyHooksPrivileged(EventListenerHook.class, "event", (hook, r) -> hook.event(event, result)); //$NON-NLS-1$
	}

	/**
	 * Calls all hook services of the type specified by the hook context.
	 * 
	 * @param <T>
	 *
	 * @param hookContext Context to use when calling the hook services.
	 */
	@SuppressWarnings("unchecked")
	public <T> void notifyHooksPrivileged(Class<T> hookType, String serviceMethod, HookContext<T> hookContext) {
		List<ServiceRegistrationImpl<?>> hooks = lookupServiceRegistrations(hookType.getName(), null);
		// Since the list is already sorted, we don't need to sort the list to call the hooks
		// in the proper order.

		for (ServiceRegistrationImpl<?> registration : hooks) {
			notifyHookPrivileged(systemBundleContext, (ServiceRegistrationImpl<T>) registration, serviceMethod,
					hookContext);
		}
	}

	/**
	 * Call a hook service via a hook context.
	 * 
	 * @param <T>
	 *
	 * @param context      Context of the bundle to get the hook service.
	 * @param registration Hook service to call.
	 * @param hookContext  Context to use when calling the hook service.
	 */
	private <T> void notifyHookPrivileged(BundleContextImpl context, ServiceRegistrationImpl<T> registration,
			String serviceMethod, HookContext<T> hookContext) {
		if (hookContext.skipRegistration(registration)) {
			return;
		}
		T hook = registration.getHookInstance();
		if (hook == null) {
			// The hook may not be initialized yet
			// We do not call the hook until after it has been registered
			// This means we could miss calls to a hook during the registered event.
			return;
		}
		try {
			hookContext.call(hook, registration);
		} catch (Throwable t) {
			if (debug.DEBUG_HOOKS) {
				Debug.println(hook.getClass().getName() + "." + serviceMethod + "() exception: " + t.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
				Debug.printStackTrace(t);
			}
			// allow the adaptor to handle this unexpected error
			container.handleRuntimeError(t);
			ServiceException se = new ServiceException(
					NLS.bind(Msg.SERVICE_FACTORY_EXCEPTION, hook.getClass().getName(), serviceMethod), t);
			container.getEventPublisher().publishFrameworkEvent(FrameworkEvent.ERROR, registration.getBundle(), se);
		}
	}

	/**
	 * Call a newly registered ListenerHook service to provide the current collection of
	 * service listeners.
	 *
	 * @param registration The newly registered ListenerHook service.
	 */
	private void notifyNewListenerHook(final ServiceRegistrationImpl<?> registration) {
		if (System.getSecurityManager() == null) {
			notifyNewListenerHookPrivileged(registration);
		} else {
			AccessController.doPrivileged(new PrivilegedAction<Void>() {
				@Override
				public Void run() {
					notifyNewListenerHookPrivileged(registration);
					return null;
				}
			});
		}

	}

	void notifyNewListenerHookPrivileged(ServiceRegistrationImpl<?> registration) {
		if (debug.DEBUG_HOOKS) {
			Debug.println("notifyServiceNewListenerHook(" + registration + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		// snapshot the listeners
		Collection<ListenerInfo> addedListeners = new ArrayList<>(initialCapacity);
		synchronized (serviceEventListeners) {
			for (CopyOnWriteIdentityMap<ServiceListener, FilteredServiceListener> listeners : serviceEventListeners.values()) {
				if (!listeners.isEmpty()) {
					addedListeners.addAll(listeners.values());
				}
			}
		}

		final Collection<ListenerInfo> listeners = Collections.unmodifiableCollection(addedListeners);
		notifyHookPrivileged(systemBundleContext, registration, "added", (hook, hookRegistration) -> { //$NON-NLS-1$
			if (hook instanceof ListenerHook) {
				((ListenerHook) hook).added(listeners);
			}
		});
	}

	/**
	 * Call the registered ListenerHook services to notify them of newly added or removed service listeners.
	 * The ListenerHook must be called in order: descending by service.ranking, then ascending by service.id.
	 * This is the natural order for ServiceReference.
	 *
	 * @param listeners A non-empty, unmodifiable collection of ListenerInfo objects.
	 * All elements in the list must be for the same bundle.
	 * @param added <code>true</code> if the specified listeners are being added. <code>false</code>
	 * if they are being removed.
	 */
	private void notifyListenerHooks(final Collection<ListenerInfo> listeners, final boolean added) {
		if (System.getSecurityManager() == null) {
			notifyListenerHooksPrivileged(listeners, added);
		} else {
			AccessController.doPrivileged(new PrivilegedAction<Void>() {
				@Override
				public Void run() {
					notifyListenerHooksPrivileged(listeners, added);
					return null;
				}
			});
		}

	}

	void notifyListenerHooksPrivileged(final Collection<ListenerInfo> listeners, final boolean added) {
		assert !listeners.isEmpty();
		if (debug.DEBUG_HOOKS) {
			Debug.println("notifyServiceListenerHooks(" + listeners + "," + (added ? "added" : "removed") + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}

		notifyHooksPrivileged(ListenerHook.class, added ? "added" : "removed", (hook, hookRegistration) -> { //$NON-NLS-1$ //$NON-NLS-2$
			if (added) {
				hook.added(listeners);
			} else {
				hook.removed(listeners);
			}
		});
	}

	final EquinoxContainer getContainer() {
		return container;
	}
}
