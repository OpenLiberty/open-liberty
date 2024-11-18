/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others.
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

package org.eclipse.osgi.internal.url;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.function.Supplier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * The URLStreamHandlerProxy is a URLStreamHandler that acts as a proxy for
 * registered URLStreamHandlerServices. When a URLStreamHandler is requested
 * from the URLStreamHandlerFactory and it exists in the service registry, a
 * URLStreamHandlerProxy is created which will pass all the requests from the
 * requestor to the real URLStreamHandlerService. We can't return the real
 * URLStreamHandlerService from the URLStreamHandlerFactory because the JVM
 * caches URLStreamHandlers and therefore would not support a dynamic
 * environment of URLStreamHandlerServices being registered and unregistered.
 */

public class URLStreamHandlerProxy extends URLStreamHandler {

	private static final URLStreamHandlerService NO_HANDLER = new NullURLStreamHandlerService();
	// TODO lots of type-based names
	protected URLStreamHandlerService realHandlerService;

	protected final URLStreamHandlerSetter urlSetter;

	protected final ServiceTracker<URLStreamHandlerService, LazyURLStreamHandlerService> urlStreamHandlerServiceTracker;

	public URLStreamHandlerProxy(String protocol, BundleContext context) {

		urlSetter = new URLStreamHandlerSetter(this);

		Filter filter;
		try {
			filter = context.createFilter(String.format("(&(%s=%s)(%s=%s))", Constants.OBJECTCLASS, //$NON-NLS-1$
					URLStreamHandlerFactoryImpl.URLSTREAMHANDLERCLASS, URLConstants.URL_HANDLER_PROTOCOL, protocol));
		} catch (InvalidSyntaxException e) {
			throw new AssertionError("should never happen!", e); //$NON-NLS-1$
		}

		urlStreamHandlerServiceTracker = new ServiceTracker<>(context, filter,
				new ServiceTrackerCustomizer<URLStreamHandlerService, LazyURLStreamHandlerService>() {

					@Override
					public LazyURLStreamHandlerService addingService(
							ServiceReference<URLStreamHandlerService> reference) {
						return new LazyURLStreamHandlerService(context, reference);
					}

					@Override
					public void modifiedService(ServiceReference<URLStreamHandlerService> reference,
							LazyURLStreamHandlerService service) {
						// nothing to do here...
					}

					@Override
					public void removedService(ServiceReference<URLStreamHandlerService> reference,
							LazyURLStreamHandlerService service) {
						service.dispose();
					}
				});
		URLStreamHandlerFactoryImpl.secureAction.open(urlStreamHandlerServiceTracker);
	}

	/**
	 * @see java.net.URLStreamHandler#equals(URL, URL)
	 */
	@Override
	protected boolean equals(URL url1, URL url2) {
		return getRealHandlerService().equals(url1, url2);
	}

	/**
	 * @see java.net.URLStreamHandler#getDefaultPort()
	 */
	@Override
	protected int getDefaultPort() {
		return getRealHandlerService().getDefaultPort();
	}

	/**
	 * @see java.net.URLStreamHandler#getHostAddress(URL)
	 */
	@Override
	protected InetAddress getHostAddress(URL url) {
		return getRealHandlerService().getHostAddress(url);
	}

	/**
	 * @see java.net.URLStreamHandler#hashCode(URL)
	 */
	@Override
	protected int hashCode(URL url) {
		return getRealHandlerService().hashCode(url);
	}

	/**
	 * @see java.net.URLStreamHandler#hostsEqual(URL, URL)
	 */
	@Override
	protected boolean hostsEqual(URL url1, URL url2) {
		return getRealHandlerService().hostsEqual(url1, url2);
	}

	/**
	 * @see java.net.URLStreamHandler#openConnection(URL)
	 */
	@Override
	protected URLConnection openConnection(URL url) throws IOException {
		return getRealHandlerService().openConnection(url);
	}

	/**
	 * @see java.net.URLStreamHandler#parseURL(URL, String, int, int)
	 */
	@Override
	protected void parseURL(URL url, String str, int start, int end) {
		getRealHandlerService().parseURL(urlSetter, url, str, start, end);
	}

	/**
	 * @see java.net.URLStreamHandler#sameFile(URL, URL)
	 */
	@Override
	protected boolean sameFile(URL url1, URL url2) {
		return getRealHandlerService().sameFile(url1, url2);
	}

	/**
	 * @see java.net.URLStreamHandler#toExternalForm(URL)
	 */
	@Override
	protected String toExternalForm(URL url) {
		return getRealHandlerService().toExternalForm(url);
	}

	/**
	 * @see java.net.URLStreamHandler#setURL(URL, String, String, int, String,
	 *      String, String, String, String)
	 */
	@Override
	public void setURL(URL u, String protocol, String host, int port, String authority, String userInfo, String file,
			String query, String ref) {
		super.setURL(u, protocol, host, port, authority, userInfo, file, query, ref);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void setURL(URL url, String protocol, String host, int port, String file, String ref) {

		// using non-deprecated URLStreamHandler.setURL method.
		// setURL(URL u, String protocol, String host, int port, String authority,
		// String userInfo, String file, String query, String ref)
		super.setURL(url, protocol, host, port, null, null, file, null, ref);
	}

	@Override
	protected URLConnection openConnection(URL u, Proxy p) throws IOException {
		try {
			URLStreamHandlerService service = getRealHandlerService();
			Method openConn = service.getClass().getMethod("openConnection", //$NON-NLS-1$
					new Class[] { URL.class, Proxy.class });
			openConn.setAccessible(true);
			return (URLConnection) openConn.invoke(service, new Object[] { u, p });
		} catch (InvocationTargetException e) {
			if (e.getTargetException() instanceof IOException)
				throw (IOException) e.getTargetException();
			throw (RuntimeException) e.getTargetException();
		} catch (Exception e) {
			// expected on JRE < 1.5
			throw new UnsupportedOperationException(e);
		}
	}

	public boolean isActive() {
		return urlStreamHandlerServiceTracker.getService() != null;
	}

	public URLStreamHandlerService getRealHandlerService() {
		LazyURLStreamHandlerService service = urlStreamHandlerServiceTracker.getService();
		if (service != null) {
			return service.get();
		}
		return NO_HANDLER;
	}

	private static final class LazyURLStreamHandlerService implements Supplier<URLStreamHandlerService> {

		private BundleContext bundleContext;
		private ServiceReference<URLStreamHandlerService> reference;
		private URLStreamHandlerService service;
		private boolean disposed;

		LazyURLStreamHandlerService(BundleContext bundleContext, ServiceReference<URLStreamHandlerService> reference) {
			this.bundleContext = bundleContext;
			this.reference = reference;
		}

		synchronized void dispose() {
			disposed = true;
			if (service != null) {
				service = null;
				bundleContext.ungetService(reference);
			}
		}

		@Override
		public synchronized URLStreamHandlerService get() {
			if (service == null && !disposed) {
				service = URLStreamHandlerFactoryImpl.secureAction.getService(reference, bundleContext);
			}
			return service;
		}

	}
}
