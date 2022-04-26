/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
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

import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.framework.BundleContextImpl;
import org.eclipse.osgi.internal.framework.FilterImpl;
import org.osgi.framework.AllServiceListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.UnfilteredServiceListener;
import org.osgi.framework.hooks.service.ListenerHook;

/**
 * Service Listener delegate.
 */
class FilteredServiceListener implements ServiceListener, ListenerHook.ListenerInfo {
	/** Filter for listener. */
	private final FilterImpl filter;
	/** Real listener. */
	private final ServiceListener listener;
	/** The bundle context */
	private final BundleContextImpl context;
	/** is this an AllServiceListener */
	private final boolean allservices;
	/** is this an UnfilteredServiceListener */
	private final boolean unfiltered;
	/** an objectClass required by the filter */
	private final String objectClass;
	/** indicates whether the listener has been removed */
	private volatile boolean removed;
	private final Debug debug;

	/**
	 * Constructor.
	 *
	 * @param context The bundle context of the bundle which added the specified service listener.
	 * @param filterstring The filter string specified when this service listener was added.
	 * @param listener The service listener object.
	 * @exception InvalidSyntaxException if the filter is invalid.
	 */
	FilteredServiceListener(final BundleContextImpl context, final ServiceListener listener, final String filterstring) throws InvalidSyntaxException {
		this.debug = context.getContainer().getConfiguration().getDebug();
		this.unfiltered = (listener instanceof UnfilteredServiceListener);
		if (filterstring == null) {
			this.filter = null;
			this.objectClass = null;
		} else {
			FilterImpl filterImpl = FilterImpl.newInstance(filterstring, context.getContainer().getConfiguration().getDebug().DEBUG_FILTER);
			String clazz = filterImpl.getRequiredObjectClass();
			if (unfiltered || (clazz == null)) {
				this.objectClass = null;
				this.filter = filterImpl;
			} else {
				this.objectClass = clazz.intern(); /*intern the name for future identity comparison */
				// a filter with no children and non-null requiredObjectClass is a simple
				// filter;
				// e.g. (objectClass=SomeService)
				this.filter = filterImpl.getChildren().isEmpty() ? null : filterImpl;
			}
		}
		this.removed = false;
		this.listener = listener;
		this.context = context;
		this.allservices = (listener instanceof AllServiceListener);
	}

	/**
	 * Receives notification that a service has had a lifecycle change.
	 *
	 * @param event The <code>ServiceEvent</code> object.
	 */
	@Override
	public void serviceChanged(ServiceEvent event) {
		ServiceReferenceImpl<?> reference = (ServiceReferenceImpl<?>) event.getServiceReference();

		// first check if we can short circuit the filter match if the required objectClass does not match the event
		objectClassCheck: if (objectClass != null) {
			String[] classes = reference.getClasses();
			int size = classes.length;
			for (int i = 0; i < size; i++) {
				if (classes[i] == objectClass) // objectClass strings have previously been interned for identity comparison
					break objectClassCheck;
			}
			return; // no class in this event matches a required part of the filter; we do not need to deliver this event
		}
		// TODO could short circuit service.id filters as well since the id is constant for a registration.

		if (!ServiceRegistry.hasListenServicePermission(event, context))
			return;

		if (debug.DEBUG_EVENTS) {
			String listenerName = this.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(this)); //$NON-NLS-1$
			Debug.println("filterServiceEvent(" + listenerName + ", \"" + getFilter() + "\", " + reference.getRegistration().getProperties() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}

		event = filterMatch(event);
		if (event == null) {
			return;
		}
		if (allservices || ServiceRegistry.isAssignableTo(context, objectClass, reference)) {
			if (debug.DEBUG_EVENTS) {
				String listenerName = listener.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(listener)); //$NON-NLS-1$
				Debug.println("dispatchFilteredServiceEvent(" + listenerName + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}

			listener.serviceChanged(event);
		}
	}

	/**
	 * Returns a service event that should be delivered to the listener based on the filter evaluation.
	 * This may result in a service event of type MODIFIED_ENDMATCH.
	 *
	 * @param delivered The service event delivered by the framework.
	 * @return The event to be delivered or null if no event is to be delivered to the listener.
	 */
	private ServiceEvent filterMatch(ServiceEvent delivered) {
		boolean modified = delivered.getType() == ServiceEvent.MODIFIED;
		ServiceEvent event = modified ? ((ModifiedServiceEvent) delivered).getModifiedEvent() : delivered;
		if (unfiltered || (filter == null)) {
			return event;
		}
		ServiceReference<?> reference = event.getServiceReference();
		if (filter.match(reference)) {
			return event;
		}
		if (modified) {
			ModifiedServiceEvent modifiedServiceEvent = (ModifiedServiceEvent) delivered;
			if (modifiedServiceEvent.matchPreviousProperties(filter)) {
				return modifiedServiceEvent.getModifiedEndMatchEvent();
			}
		}
		// does not match and did not match previous properties; do not send event
		return null;
	}

	/**
	 * The string representation of this Filtered listener.
	 *
	 * @return The string representation of this listener.
	 */
	@Override
	public String toString() {
		String filterString = getFilter();
		if (filterString == null) {
			filterString = ""; //$NON-NLS-1$
		}
		return listener.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(listener)) + filterString; //$NON-NLS-1$
	}

	/**
	 * Return the bundle context for the ListenerHook.
	 * @return The context of the bundle which added the service listener.
	 * @see org.osgi.framework.hooks.service.ListenerHook.ListenerInfo#getBundleContext()
	 */
	@Override
	public BundleContext getBundleContext() {
		return context;
	}

	/**
	 * Return the filter string for the ListenerHook.
	 * @return The filter string with which the listener was added. This may
	 * be <code>null</code> if the listener was added without a filter.
	 * @see org.osgi.framework.hooks.service.ListenerHook.ListenerInfo#getFilter()
	 */
	@Override
	public String getFilter() {
		if (filter != null) {
			return filter.toString();
		}
		return getObjectClassFilterString(objectClass);
	}

	/**
	 * Return the state of the listener for this addition and removal life
	 * cycle. Initially this method will return <code>false</code>
	 * indicating the listener has been added but has not been removed.
	 * After the listener has been removed, this method must always return
	 * <code>true</code>.
	 *
	 * @return <code>false</code> if the listener has not been been removed,
	 *         <code>true</code> otherwise.
	 */
	@Override
	public boolean isRemoved() {
		return removed;
	}

	/**
	 * Mark the service listener registration as removed.
	 */
	void markRemoved() {
		removed = true;
	}

	/**
	 * Returns an objectClass filter string for the specified class name.
	 * @return A filter string for the specified class name or <code>null</code> if the
	 * specified class name is <code>null</code>.
	 */
	private static String getObjectClassFilterString(String className) {
		if (className == null) {
			return null;
		}
		return "(" + Constants.OBJECTCLASS + "=" + className + ")"; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}
}
