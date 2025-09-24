/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package org.eclipse.equinox.plurl.impl;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.ContentHandler;
import java.net.ContentHandlerFactory;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.eclipse.equinox.plurl.Plurl;
import org.eclipse.equinox.plurl.PlurlFactory;
import org.eclipse.equinox.plurl.PlurlStreamHandler;
import org.eclipse.equinox.plurl.PlurlStreamHandler.PlurlSetter;
import org.eclipse.equinox.plurl.PlurlStreamHandlerBase;

public final class PlurlImpl implements Plurl {

	private static final String PROTOCOL_HANDLER_PKGS = "java.protocol.handler.pkgs"; //$NON-NLS-1$
	private static final String CONTENT_HANDLER_PKGS = "java.content.handler.pkgs"; //$NON-NLS-1$
	private static final String DEFAULT_VM_CONTENT_HANDLERS = "sun.net.www.content"; //$NON-NLS-1$
	volatile Set<String> forbiddenProtocols = new HashSet<>(
			Arrays.asList("jar", "jmod", "file", "jrt")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	private static final String THIS_PACKAGE = PlurlImpl.class.getPackage().getName();
	static final String PLURL_STREAM_HANDLER_CLASS_NAME = PlurlStreamHandler.class.getName();
	static final Field URL_HANDLER_FIELD = findUrlHandlerField();

	private static final Collection<ClassLoader> systemLoaders;
	static {
		Collection<ClassLoader> loaders = new ArrayList<>();
		try {
			ClassLoader cl = ClassLoader.getSystemClassLoader();
			// we allow the system cl, but not its parents
			cl = cl != null ? cl.getParent() : null;
			while (cl != null) {
				loaders.add(cl);
				cl = cl.getParent();
			}
		} catch (Throwable t) {
			// ignore as if no loaders
		}
		systemLoaders = Collections.unmodifiableCollection(loaders);
	}

	private static boolean isSystemClass(String pName, final Class<?> clazz) {
		if (pName != null && pName.startsWith("jdk.")) { //$NON-NLS-1$
			return true;
		}
		// we want to ignore classes from the system
		ClassLoader cl = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
			@Override
			public ClassLoader run() {
				return clazz.getClassLoader();
			}
		});
		return cl == null || systemLoaders.contains(cl);
	}

	private static Field findUrlHandlerField() {
		Field f = null;
		try {
			f = URL.class.getDeclaredField("handler"); //$NON-NLS-1$
		} catch (Exception e) {
			Field[] fields = URL.class.getDeclaredFields();
			for (Field field : fields) {
				boolean isStatic = Modifier.isStatic(field.getModifiers());
				if (!isStatic && field.getType().equals(URLStreamHandler.class)) {
					f = field;
					break;
				}
			}
		}
		if (f == null) {
			// fallback reflection is blocked by module system
			return null;
		}
		try {
			f.setAccessible(true);
			return f;
		} catch (Exception e) {
			// blocked by module system
		}
		return null;
	}

	static boolean setHandler(URL u, Object h) {
		if (URL_HANDLER_FIELD == null || !(h instanceof URLStreamHandler)) {
			return false;
		}
		try {
			URL_HANDLER_FIELD.set(u, h);
		} catch (Exception e) {
			// should not happen
			throw new IllegalStateException(e);
		}
		return true;
	}

	static enum SetFactories {
		notInstalled, // not installed yet
		primordial, // there were no factories set until plurl
		override, // single factories existed before plurl overrode them
		plurlAlreadySet // some other PlurlImpl instance already installed plurl
	}

	SetFactories setFactories = SetFactories.notInstalled;

	URLStreamHandlerFactory parentURLStreamHandlerFactory = null;
	ContentHandlerFactory parentContentHandlerFactory = null;

	List<URLStreamHandlerFactoryHolder> streamHandlerFactories = Collections.emptyList();
	List<ContentHandlerFactoryHolder> contentHandlerFactories = Collections.emptyList();
	List<PlurlImplHolder> plurlImpls = Collections.emptyList();

	final ServiceLoader<URLStreamHandlerFactory> builtinURLStreamHandlerFactoryLoader;
	final ServiceLoader<ContentHandlerFactory> builtinContentHandlerFactoryLoader;
	final CallStack callStack;

	private final ThreadLocal<List<String>> creatingProtocols = new ThreadLocal<>();
	final URLToHandler urlToHandler = new URLToHandler();

	boolean isRecursive(String protocol) {
		List<String> protocols = creatingProtocols.get();
		if (protocols == null) {
			protocols = new ArrayList<>(1);
			creatingProtocols.set(protocols);
		}
		if (protocols.contains(protocol))
			return true;
		protocols.add(protocol);
		return false;
	}

	void releaseRecursive(String protocol) {
		List<String> protocols = creatingProtocols.get();
		protocols.remove(protocol);
	}

	public interface LegacyFactory {
		public void register(Object factory);

		public void unregister(Object factory);

		public boolean isMultiplexing();
	}

	public class PlurlURLStreamHandlerFactory extends URLStreamHandler
			implements URLStreamHandlerFactory, LegacyFactory {
		private final URLStreamHandlerFactory parent;

		public PlurlURLStreamHandlerFactory(URLStreamHandlerFactory parent) {
			this.parent = parent;
		}

		@Override
		public URLStreamHandler createURLStreamHandler(String protocol) {
			if (protocol.equals(PLURL_PROTOCOL)) {
				return this;
			}
			URLStreamHandler handler = createURLStreamHandlerImpl(protocol);
			if (handler == null && parent != null) {
				return parent.createURLStreamHandler(protocol);
			}
			return handler;
		}

		@Override
		protected URLConnection openConnection(URL u) throws IOException {
			return plurlOperation(u);
		}

		@Override
		public void register(Object factory) {
			add((URLStreamHandlerFactory) factory);
		}

		@Override
		public void unregister(Object factory) {
			remove((URLStreamHandlerFactory) factory);
		}

		@Override
		public boolean isMultiplexing() {
			return PlurlImpl.this.isMultiplexing(getURLStreamHandlerFactories());
		}
	}

	public class PlurlContentHandlerFactory implements ContentHandlerFactory, LegacyFactory {
		private final ContentHandlerFactory parent;

		public PlurlContentHandlerFactory(ContentHandlerFactory parent) {
			this.parent = parent;
		}
		@Override
		public ContentHandler createContentHandler(String mimetype) {
			ContentHandler fromParent = parent == null ? null : parent.createContentHandler(mimetype);
			return createContentHandlerImpl(mimetype, fromParent);
		}

		@Override
		public void register(Object factory) {
			add((ContentHandlerFactory) factory);
		}

		@Override
		public void unregister(Object factory) {
			remove((ContentHandlerFactory) factory);
		}

		@Override
		public boolean isMultiplexing() {
			return PlurlImpl.this.isMultiplexing(getContentHandlerFactories());
		}
	}

	private boolean checkPlurlProtocol() {
		try {
			URL plurl = new URL(Plurl.PLURL_PROTOCOL, Plurl.PLURL_OP, PLURL_REGISTER_IMPLEMENTATION);
			// plurl is already available; try registering our impl
			@SuppressWarnings("unchecked")
			Consumer<Object> addImpl = (Consumer<Object>) plurl.openConnection().getContent();
			addImpl.accept(this);
			this.setFactories = SetFactories.plurlAlreadySet;
			return true;
		} catch (MalformedURLException e) {
			// expected if there is no plurl installed yet.
			return false;
		} catch (IOException e) {
			// could not add our implementation; move on
		}
		return true;
	}

	public synchronized void install(String... forbidden) {
		if (setFactories == SetFactories.override || setFactories == SetFactories.primordial) {
			// already installed; no-op
			return;
		}
		if (forbidden != null && forbidden.length > 0) {
			Set<String> forbiddenSet = new HashSet<>(Arrays.asList(forbidden));
			if (forbiddenSet.contains(PLURL_FORBID_NOTHING)) {
				forbiddenProtocols = Collections.emptySet();
			} else {
				forbiddenProtocols = forbiddenSet;
			}
		}
		if (checkPlurlProtocol()) {
			return;
		}

		// sync on URLConnection to prevent more than one thread from setting plurl
		synchronized (URLConnection.class) {
			// try again with lock
			if (checkPlurlProtocol()) {
				return;
			}
			boolean contentHandlerFactorySet = false;
			try {
				URLConnection.setContentHandlerFactory(new PlurlContentHandlerFactory(null));
				contentHandlerFactorySet = true;
				URL.setURLStreamHandlerFactory(new PlurlURLStreamHandlerFactory(null));
				setFactories = SetFactories.primordial;
			} catch (Throwable t) {
				try {
					forceFactories(t, contentHandlerFactorySet);
					setFactories = SetFactories.override;
				} catch (Exception e) {
					String message = "Cannot install the plurl factories. " //$NON-NLS-1$
							+ "The java.base module must be configured to open the java.net package for reflection " //$NON-NLS-1$
							+ "in order to allow plurl to replace the existing factories. " //$NON-NLS-1$
							+ "For example, by using the JVM option: '--add-opens java.base/java.net=ALL-UNNAMED'. "; //$NON-NLS-1$
					throw new IllegalStateException(message, e);
				}
			}
		}
	}

	private boolean unregisterPlurlImpl() {
		try {
			URL plurl = new URL(Plurl.PLURL_PROTOCOL, Plurl.PLURL_OP, PLURL_UNREGISTER_IMPLEMENTATION);
			// plurl is already available; try adding ours impl
			@SuppressWarnings("unchecked")
			Consumer<Object> removeImpl = (Consumer<Object>) plurl.openConnection().getContent();
			removeImpl.accept(this);
			return true;
		} catch (MalformedURLException e) {
			// expected if there is no plurl installed
			return false;
		} catch (IOException e) {
			// could not remove our implementation; move on
		}
		return true;
	}

	@Override
	public synchronized void uninstall() {
		if (setFactories == SetFactories.notInstalled) {
			// not installed; do nothing
			return;
		}
		if (setFactories == SetFactories.primordial) {
			// we don't let primordial Plurls get uninstalled;
			// this is a no-op;
			return;
		}
		if (setFactories == SetFactories.plurlAlreadySet) {
			// some other plurl is set; unregister ours
			unregisterPlurlImpl();
			setFactories = SetFactories.notInstalled;
			return;
		}

		// set this plurl as not installed
		setFactories = SetFactories.notInstalled;

		// find a designate
		Object designate = null;
		PlurlImplHolder nextHolder = null;
		Iterator<PlurlImplHolder> iHolders = plurlImpls.iterator();
		while (iHolders.hasNext()) {
			PlurlImplHolder next = iHolders.next();
			iHolders.remove();
			// pin designate object to avoid GC
			designate = next.getImpl();
			if (designate != null) {
				nextHolder = next;
				break;
			}
		}

		Exception forceError = null;
		try {
			forceURLStreamHandlerFactory(false, parentURLStreamHandlerFactory);
		} catch (Exception e) {
			// this is unexpected since we forced the plurl at install
			forceError = e;
		}
		try {
			forceContentHandlerFactory(false, parentContentHandlerFactory);
		} catch (Exception e) {
			// this is unexpected since we forced the plurl at install
			if (forceError != null) {
				e.addSuppressed(forceError);
			}
			forceError = e;
		}
		if (forceError != null) {
			// again, this would be very unexpected since we forced plurl at install
			throw new RuntimeException(forceError);
		}

		if (nextHolder != null) {
			// found a designate; now force it to be installed
			nextHolder.install();
		}
		// Hack to make sure the designate isn't GC'ed before we call install by using
		// the reference after; otherwise the JVM could determine the variable is out of
		// scope and therefore allow it to be GC'ed
		if (designate != null) {
			designate.hashCode();
		}
	}

	private void forceFactories(Throwable t, boolean contentHandlerFactorySet) throws Exception {
		try {
			if (!contentHandlerFactorySet) {
				forceContentHandlerFactory(true, null);
			}
			forceURLStreamHandlerFactory(true, null);
		} catch (Exception e) {
			e.addSuppressed(t);
			throw e;
		}
	}

	private void forceURLStreamHandlerFactory(boolean installPlurl, URLStreamHandlerFactory forceBack)
			throws Exception {
		Field factoryField = getStaticField(URL.class, URLStreamHandlerFactory.class);
		if (factoryField == null) {
			throw new Exception("Could not find URLStreamHandlerFactory field"); //$NON-NLS-1$
		}
		// look for a lock to synchronize on
		Object lock = getURLStreamHandlerFactoryLock();
		synchronized (lock) {
			URLStreamHandlerFactory toSet = installPlurl ? (URLStreamHandlerFactory) factoryField.get(null) : forceBack;
			if (installPlurl) {
				parentURLStreamHandlerFactory = toSet;
				// current factory does not support plurl, ok we'll wrap it
				toSet = new PlurlURLStreamHandlerFactory(toSet);
			}

			factoryField.set(null, null);
			// always attempt to clear the handlers cache
			// This allows an optimization for the single framework use-case
			resetURLStreamHandlers();
			URL.setURLStreamHandlerFactory(toSet);
		}
	}

	private Object getURLStreamHandlerFactoryLock() throws IllegalAccessException {
		Object lock;
		try {
			Field streamHandlerLockField = URL.class.getDeclaredField("streamHandlerLock"); //$NON-NLS-1$
			streamHandlerLockField.setAccessible(true);
			lock = streamHandlerLockField.get(null);
		} catch (NoSuchFieldException noField) {
			// could not find the lock, lets sync on the class object
			lock = URL.class;
		}
		return lock;
	}

	private void resetURLStreamHandlers() throws IllegalAccessException {
		Field handlersField = getStaticField(URL.class, Hashtable.class);
		if (handlersField != null) {
			@SuppressWarnings("rawtypes")
			Hashtable<?, ?> handlers = (Hashtable) handlersField.get(null);
			if (handlers != null) {
				handlers.clear();
			}
		}
	}

	private void forceContentHandlerFactory(boolean installPlurl, ContentHandlerFactory forceBack) throws Exception {
		Field factoryField = getStaticField(URLConnection.class, java.net.ContentHandlerFactory.class);
		if (factoryField == null) {
			throw new Exception("Could not find ContentHandlerFactory field"); //$NON-NLS-1$
		}

		ContentHandlerFactory toSet = installPlurl ? (ContentHandlerFactory) factoryField.get(null) : forceBack;
		if (installPlurl) {
			parentContentHandlerFactory = toSet;
			// current factory does not support plurl, ok we'll wrap it
			toSet = new PlurlContentHandlerFactory(toSet);
		}
		// null out the field so that we can successfully call setContentHandlerFactory
		factoryField.set(null, null);
		// always attempt to clear the handlers cache
		// This allows an optimization for the single framework use-case
		resetContentHandlers();
		URLConnection.setContentHandlerFactory(toSet);
	}

	private void resetContentHandlers() throws IllegalAccessException {
		Field handlersField = getStaticField(URLConnection.class, Hashtable.class);
		if (handlersField != null) {
			@SuppressWarnings("rawtypes")
			Hashtable<?, ?> handlers = (Hashtable) handlersField.get(null);
			if (handlers != null) {
				handlers.clear();
			}
		}
	}

	public Field getStaticField(Class<?> clazz, Class<?> type) {
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			boolean isStatic = Modifier.isStatic(field.getModifiers());
			if (isStatic && field.getType().equals(type)) {
				field.setAccessible(true);
				return field;
			}
		}
		return null;
	}

	public PlurlImpl() {
		builtinContentHandlerFactoryLoader = ServiceLoader.load(ContentHandlerFactory.class);
		builtinURLStreamHandlerFactoryLoader = ServiceLoader.load(URLStreamHandlerFactory.class);
		callStack = createCallStack();
	}

	private CallStack createCallStack() {
		try {
			Class.forName("java.lang.StackWalker"); //$NON-NLS-1$
			return new StackWalkerCallStack();
		} catch (ClassNotFoundException e) {
			return new SecurityManagerCallStack();
		}
	}

	ContentHandler createContentHandlerImpl(String mimetype, ContentHandler fromParent) {
		ContentHandler builtin = findBuiltInContentHandler(mimetype);
		if (builtin != null) {
			return builtin;
		}
		// Never return null for content handlers because then
		// we will never get called again.
		return new PlurlRootContentHandler(mimetype, fromParent);
	}

	URLStreamHandler createURLStreamHandlerImpl(String protocol) {
		if (forbiddenProtocols.contains(protocol)) {
			// to dangerous for these to be overridden
			return null;
		}
		// Check if we are recursing
		if (isRecursive(protocol)) {
			return null;
		}
		try {
			try {
				URLStreamHandler builtin = findBuiltinURLStreamHandler(protocol);
				if (builtin != null) {
					return builtin;
				}
			} catch (UnsupportedOperationException e) {
				// check if it is a reflective error. If so then we know there is a built-in
				// protocol and we know we can never replace it. Let the JVM handle it.
				if (e.getCause() instanceof ReflectiveOperationException) {
					return null;
				}
			}
			URLStreamHandlerFactoryHolder factoryHolder = findFactory(getURLStreamHandlerFactories());
			if (factoryHolder != null) {
				PlurlStreamHandler shouldHandle = factoryHolder.getHandler(protocol);
				if (shouldHandle != null) {
					return new PlurlRootURLStreamHandler(protocol);
				}
			}
			// Return null if nothing found that should handle the protocol;
			// We will get called again if the protocol is asked for again.
			return null;
		} finally {
			releaseRecursive(protocol);
		}
	}


	private ContentHandler findBuiltInContentHandler(String mimetype) {
		return AccessController.doPrivileged(new PrivilegedAction<ContentHandler>() {
			@Override
			public ContentHandler run() {
				return findBuiltinContentHandlerImpl(mimetype);
			}
		});
	}

	private URLStreamHandler findBuiltinURLStreamHandler(String protocol) {
		return AccessController.doPrivileged(new PrivilegedAction<URLStreamHandler>() {
			@Override
			public URLStreamHandler run() {
				return findBuiltinURLStreamHandlerImpl(protocol);
			}
		});
	}

	ContentHandler findBuiltinContentHandlerImpl(String contentType) {
		// first check service loader
		for (ContentHandlerFactory f : builtinContentHandlerFactoryLoader) {
			ContentHandler h = f.createContentHandler(contentType);
			if (h != null) {
				return h;
			}
		}
		// now check property
		String builtInHandlers = System.getProperty(CONTENT_HANDLER_PKGS);
		builtInHandlers = builtInHandlers == null ? DEFAULT_VM_CONTENT_HANDLERS
				: DEFAULT_VM_CONTENT_HANDLERS + '|' + builtInHandlers;

		// replace '/' with a '.' and all characters not allowed in a java class name
		// with a '_'.
		String convertedContentType = contentType.replace('.', '_');
		convertedContentType = convertedContentType.replace('/', '.');
		convertedContentType = convertedContentType.replace('-', '_');
		StringTokenizer tok = new StringTokenizer(builtInHandlers, "|"); //$NON-NLS-1$
		while (tok.hasMoreElements()) {
			StringBuilder name = new StringBuilder();
			name.append(tok.nextToken());
			name.append("."); //$NON-NLS-1$
			name.append(convertedContentType);
			try {
				Class<?> clazz = null;
				try {
					clazz = Class.forName(name.toString());
				} catch (ClassNotFoundException e) {
					ClassLoader cl = ClassLoader.getSystemClassLoader();
					if (cl != null) {
						clazz = cl.loadClass(name.toString());
					}
				}
				if (clazz != null) {
					return (ContentHandler) clazz.getConstructor().newInstance();
				}
			} catch (Exception ex) {
				// handle all exceptions here and move on
			}
		}
		return null;
	}

	URLStreamHandler findBuiltinURLStreamHandlerImpl(String protocol) {
		// first check service loader
		for (URLStreamHandlerFactory f : builtinURLStreamHandlerFactoryLoader) {
			URLStreamHandler h = f.createURLStreamHandler(protocol);
			if (h != null) {
				return h;
			}
		}
		// now check property
		String builtInHandlers = System.getProperty(PROTOCOL_HANDLER_PKGS);
		if (builtInHandlers == null)
			return null;

		StringTokenizer tok = new StringTokenizer(builtInHandlers, "|"); //$NON-NLS-1$
		while (tok.hasMoreElements()) {
			URLStreamHandler found = findBuildinURLStreamHandlerImpl(protocol, tok.nextToken());
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	URLStreamHandler findBuildinURLStreamHandlerImpl(String protocol, String inPackage) {
		Class<?> clazz = null;
		StringBuilder name = new StringBuilder();
		name.append(inPackage);
		name.append("."); //$NON-NLS-1$
		name.append(protocol);
		name.append(".Handler"); //$NON-NLS-1$
		try {
			try {
				clazz = Class.forName(name.toString());
			} catch (ClassNotFoundException e) {
				ClassLoader cl = ClassLoader.getSystemClassLoader();
				if (cl != null) {
					try {
						clazz = cl.loadClass(name.toString());
					} catch (ClassNotFoundException e2) {
						// ignore
					}
				}
			}
			if (clazz != null) {
				return (URLStreamHandler) clazz.getConstructor().newInstance();
			}
		} catch (ReflectiveOperationException e) {
			// probably because the package isn't open for reflection
			String message = "The module for class '" + clazz.getName() + "' must be configured to open the '" //$NON-NLS-1$ //$NON-NLS-2$
					+ inPackage + '.' + protocol + "' package for reflection to support the handler." //$NON-NLS-1$
					+ " For example, by using the JVM option: '--add-opens java.base/" + inPackage + '.' + protocol //$NON-NLS-1$
					+ "=ALL-UNNAMED'."; //$NON-NLS-1$
			throw new UnsupportedOperationException(message, e);
		} catch (Exception ex) {
			// handle all exceptions here and move on
		}
		return null;
	}

	URLConnection plurlOperation(URL u) {
		final String path = u.getPath();
		return new URLConnection(u) {
			@Override
			public void connect() throws IOException {
				// do nothing
			}

			@Override
			public Consumer<Object> getContent() throws IOException {
				switch (path) {
				case PLURL_ADD_URL_STREAM_HANDLER_FACTORY:
					return (f) -> add((URLStreamHandlerFactory) f);
				case PLURL_REMOVE_URL_STREAM_HANDLER_FACTORY:
					return (f) -> remove((URLStreamHandlerFactory) f);
				case PLURL_ADD_CONTENT_HANDLER_FACTORY:
					return (f) -> add((ContentHandlerFactory) f);
				case PLURL_REMOVE_CONTENT_HANDLER_FACTORY:
					return (f) -> remove((ContentHandlerFactory) f);
				case PLURL_REGISTER_IMPLEMENTATION:
					return (p) -> addImpl(p);
				case PLURL_UNREGISTER_IMPLEMENTATION:
					return (p) -> removeImpl(p);
				default:
					throw new IOException("Unknown plurl operation: " + path); //$NON-NLS-1$
				}
			}
		};
	}

	boolean isMultiplexing(List<?> factories) {
		return factories.size() > 1;
	}

	synchronized void addImpl(Object p) {
		if (setFactories == SetFactories.primordial) {
			// If this plurl is the primordial factory (no singletons set in the JVM)
			// then we don't track other plurl installs because we will never
			// remove this plurl from the JVM singleton
			return;
		}
		if (p == this) {
			// someone called install again on this Plurl; ignore it
			return;
		}
		List<PlurlImplHolder> updated = new ArrayList<>(plurlImpls);
		// remove any GC'ed handlers or the new impl (incase it is being added again)
		updated.removeIf((h) -> h.getImpl() == null || h.getImpl() == p);
		// add new impl
		updated.add(new PlurlImplHolder(p));
		plurlImpls = updated;
	}

	synchronized void removeImpl(Object p) {
		List<PlurlImplHolder> updated = new ArrayList<>(plurlImpls);
		// remove the impl and any that got GC'ed
		updated.removeIf((h) -> h.getImpl() == p || h.getImpl() == null);
		plurlImpls = updated.isEmpty() ? Collections.emptyList() : updated;
	}

	synchronized void add(URLStreamHandlerFactory f) {
		List<URLStreamHandlerFactoryHolder> updated = new ArrayList<>(streamHandlerFactories);
		// remove any GC'ed handlers
		updated.removeIf((h) -> h.getFactory() == null);
		// add new holder
		updated.add(new URLStreamHandlerFactoryHolder(f));
		streamHandlerFactories = updated;
	}

	synchronized void remove(URLStreamHandlerFactory f) {
		List<URLStreamHandlerFactoryHolder> updated = new ArrayList<>(streamHandlerFactories);
		// remove the factory and any that got GC'ed
		updated.removeIf((h) -> {
			if (h.getFactory() == f || h.getFactory() == null) {
				return true;
			}
			return false;
		});
		streamHandlerFactories = updated.isEmpty() ? Collections.emptyList() : updated;
	}

	synchronized List<URLStreamHandlerFactoryHolder> getURLStreamHandlerFactories() {
		return streamHandlerFactories;
	}

	synchronized List<ContentHandlerFactoryHolder> getContentHandlerFactories() {
		return contentHandlerFactories;
	}

	synchronized void add(ContentHandlerFactory f) {
		List<ContentHandlerFactoryHolder> updated = new ArrayList<>(contentHandlerFactories);
		// remove any GC'ed handlers
		updated.removeIf((h) -> h.getFactory() == null);
		// add new holder
		updated.add(new ContentHandlerFactoryHolder(f));
		contentHandlerFactories = updated;
	}

	synchronized void remove(ContentHandlerFactory f) {
		List<ContentHandlerFactoryHolder> updated = new ArrayList<>(contentHandlerFactories);
		// remove the factory and any that got GC'ed
		updated.removeIf((h) -> {
			if (h.getFactory() == f || h.getFactory() == null) {
				return true;
			}
			return false;
		});
		contentHandlerFactories = updated.isEmpty() ? Collections.emptyList() : updated;
	}

	ContentHandler findContentHandler(String contentType) {
		ContentHandlerFactoryHolder f = findFactory(getContentHandlerFactories());
		if (f != null) {
			return f.getHandler(contentType);
		}
		return null;
	}

	PlurlStreamHandler findPlurlStreamHandler(String protocol) {
		URLStreamHandlerFactoryHolder f = findFactory(getURLStreamHandlerFactories());
		if (f != null) {
			return f.getHandler(protocol);
		}
		return null;
	}

	private <F> F findFactory(List<F> factories) {
		int numFactories = factories.size();
		if (numFactories == 1) {
			// Handle common case of only one; just use it
			return factories.get(0);
		}
		Class<?>[] callStackClasses = getCallStack();
		for (Class<?> stack : callStackClasses) {
			String pName = getPackageName(stack);
			if (THIS_PACKAGE.equals(pName) || isSystemClass(pName, stack)) {
				continue;
			}
			for (F f : factories) {
				boolean shouldHandle = false;
				if (f instanceof PlurlFactory) {
					shouldHandle = ((PlurlFactory) f).shouldHandle(stack);
				} else {
					// use reflection in case this Plurl package isn't visible to the factory impl
					try {
						shouldHandle = (boolean) findShouldHandle(f.getClass()).invoke(f, stack);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if (shouldHandle) {
					return f;
				}
			}
		}
		// Instead of returning null here, the "first" factory is returned;
		// This means the root or "first" factory may provide protocol handlers for call stacks
		// that have no classes known to that factory
		return numFactories > 0 ? factories.get(0) : null;
	}

	Method findShouldHandle(Class<?> clazz) throws NoSuchMethodException {
		Method shouldHandle = null;
		try {
			shouldHandle = clazz.getMethod("shouldHandle", Class.class); //$NON-NLS-1$
		} catch (NoSuchMethodException e) {
			// check for legacy hasAuthority method
			try {
				shouldHandle = clazz.getMethod("hasAuthority", Class.class); //$NON-NLS-1$
			} catch (NoSuchMethodException e1) {
				throw e;
			}
		}
		shouldHandle.setAccessible(true);
		return shouldHandle;
	}

	private String getPackageName(Class<?> clazz) {
		String name = clazz.getName();
		int lastDot = name.lastIndexOf('.');
		if (lastDot >= 0) {
			return name.substring(0, lastDot);
		}
		return ""; //$NON-NLS-1$
	}

	private Class<?>[] getCallStack() {
		return callStack.getClassContext();
	}

	class PlurlRootContentHandler extends ContentHandler {
		private final String contentType;
		private final ContentHandler fromParent;

		PlurlRootContentHandler(String contentType, ContentHandler fromParent) {
			this.contentType = contentType;
			this.fromParent = fromParent;
		}

		@Override
		public Object getContent(URLConnection uConn) throws IOException {
			ContentHandler handler = findContentHandler(contentType);
			if (handler != null) {
				return handler.getContent(uConn);
			}
			if (fromParent != null) {
				return fromParent.getContent(uConn);
			}
			return uConn.getInputStream();
		}
	}

	public abstract class PlurlFactoryHolder<F, H> implements PlurlFactory {
		private final WeakReference<F> factory;
		private final Map<String, H> handlers = new ConcurrentHashMap<>();
		private final Method shouldHandleMethod;

		public PlurlFactoryHolder(F factory) {
			this.factory = new WeakReference<>(factory);
			if (factory instanceof PlurlFactory) {
				shouldHandleMethod = null;
			} else {
				try {
					shouldHandleMethod = findShouldHandle(factory.getClass());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}

		F getFactory() {
			return factory.get();
		}

		@Override
		public boolean shouldHandle(Class<?> clazz) {
			F f = factory.get();
			if (f == null) {
				return false;
			}
			if (shouldHandleMethod != null) {
				try {
					return (boolean) shouldHandleMethod.invoke(f, clazz);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			return ((PlurlFactory) f).shouldHandle(clazz);
		}

		H getHandler(String type) {
			final F f = factory.get();
			if (f == null) {
				// clear handlers
				handlers.clear();
				// remove GC'ed holders
				remove(null);
				return null;
			}
			return handlers.computeIfAbsent(type, (t) -> createHandler(t, f));
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + '@' + System.identityHashCode(this) + '[' + factory.get() + ']'
					+ handlers;
		}

		protected abstract H createHandler(String type, F f);

		protected abstract void remove(F f);
	}

	class PlurlImplHolder {
		private final WeakReference<Object> plurlImpl;

		PlurlImplHolder(Object plurlImpl) {
			this.plurlImpl = new WeakReference<>(plurlImpl);
		}

		public void install() {
			// should be able to reflect on the plurl instance because install is public
			Object currentPlurl = plurlImpl.get();
			// should never be null since we pinned the object before calling
			try {
				Method install = currentPlurl.getClass().getMethod("install", String[].class); //$NON-NLS-1$
				install.invoke(currentPlurl, (Object) forbiddenProtocols.toArray(new String[0]));
			} catch (NoSuchMethodException e) {
				// should never happen
				throw new RuntimeException(e);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			synchronized (PlurlImpl.this) {
				try {
					URL registerPlurl = new URL(PLURL_PROTOCOL, PLURL_OP, PLURL_REGISTER_IMPLEMENTATION);
					for (PlurlImplHolder plurlImplHolder : plurlImpls) {
						Object p = plurlImplHolder.getImpl();
						@SuppressWarnings("unchecked")
						Consumer<Object> addImpl = (Consumer<Object>) registerPlurl.openConnection().getContent();
						addImpl.accept(p);
					}
					URL addContentHandlerFactory = new URL(PLURL_PROTOCOL, PLURL_OP, PLURL_ADD_CONTENT_HANDLER_FACTORY);
					for (ContentHandlerFactoryHolder contentHandlerFactoryHolder : contentHandlerFactories) {
						ContentHandlerFactory c = contentHandlerFactoryHolder.getFactory();
						@SuppressWarnings("unchecked")
						Consumer<Object> addImpl = (Consumer<Object>) addContentHandlerFactory.openConnection()
								.getContent();
						addImpl.accept(c);
					}
					URL addURLStreamHandlerFactory = new URL(PLURL_PROTOCOL, PLURL_OP,
							PLURL_ADD_URL_STREAM_HANDLER_FACTORY);
					for (URLStreamHandlerFactoryHolder urlStreamHandlerFactoryHolder : streamHandlerFactories) {
						URLStreamHandlerFactory s = urlStreamHandlerFactoryHolder.getFactory();
						@SuppressWarnings("unchecked")
						Consumer<Object> addImpl = (Consumer<Object>) addURLStreamHandlerFactory.openConnection()
								.getContent();
						addImpl.accept(s);
					}
				} catch (MalformedURLException e) {
					throw new RuntimeException(e);
				} catch (IOException e) {
					throw new RuntimeException(e);
				} finally {
					plurlImpls.clear();
					contentHandlerFactories.clear();
					streamHandlerFactories.clear();
					parentContentHandlerFactory = null;
					parentURLStreamHandlerFactory = null;
				}
			}

		}

		public Object getImpl() {
			return plurlImpl.get();
		}
	}

	public class ContentHandlerFactoryHolder extends PlurlFactoryHolder<ContentHandlerFactory, ContentHandler> {
		public ContentHandlerFactoryHolder(ContentHandlerFactory factory) {
			super(factory);
		}

		@Override
		protected ContentHandler createHandler(String mimetype, ContentHandlerFactory f) {
			return f.createContentHandler(mimetype);
		}

		@Override
		protected void remove(ContentHandlerFactory f) {
			PlurlImpl.this.remove(f);
		}
	}

	public class URLStreamHandlerFactoryHolder extends PlurlFactoryHolder<URLStreamHandlerFactory, PlurlStreamHandler> {
		public URLStreamHandlerFactoryHolder(URLStreamHandlerFactory factory) {
			super(factory);
		}

		@Override
		protected PlurlStreamHandler createHandler(String protocol, URLStreamHandlerFactory f) {
			URLStreamHandler handler = f.createURLStreamHandler(protocol);
			if (handler == null) {
				return null;
			}
			if (handler instanceof PlurlStreamHandler) {
				return (PlurlStreamHandler) handler;
			}
			PlurlStreamHandler proxyPlurlStreamHandler = newProxyPlurlStreamHandler(handler);
			if (proxyPlurlStreamHandler != null) {
				return proxyPlurlStreamHandler;
			}
			return new PlurlStreamHandlerReflective(handler);
		}

		@Override
		protected void remove(URLStreamHandlerFactory f) {
			PlurlImpl.this.remove(f);
		}
	}

	PlurlStreamHandler newProxyPlurlStreamHandler(URLStreamHandler handler) {
		Class<?> checkClass = handler.getClass();
		while (checkClass != null) {
			for (Class<?> i : checkClass.getInterfaces()) {
				if (PLURL_STREAM_HANDLER_CLASS_NAME.equals(i.getName())) {
					for (Method m : i.getMethods()) {
						if (m.getName().equals("parseURL")) { //$NON-NLS-1$
							Class<?> plurlStreamHandlerClass = i;
							Class<?> plurlSetterClass = m.getParameterTypes()[0];
							return new PlurlStreamHandlerProxy(handler, plurlStreamHandlerClass, plurlSetterClass);
						}
					}
				}
			}
			checkClass = checkClass.getSuperclass();
		}
		return null;
	}

	static class PlurlStreamHandlerProxy extends URLStreamHandler implements PlurlStreamHandler {
		private final URLStreamHandler handler;
		private final Class<?> plurlSetterClass;
		private final Method equals;
		private final Method getDefaultPort;
		private final Method getHostAddress;
		private final Method hashCode;
		private final Method hostsEqual;
		private final Method openConnection;
		private final Method openConnectionProxy;
		private final Method parseURL;
		private final Method sameFile;
		private final Method toExternalForm;
		final Method setURL;
		final Method setURLDeprecated;

		public PlurlStreamHandlerProxy(URLStreamHandler handler, Class<?> plurlUrlHandlerClass,
				Class<?> plurlSetterClass) {
			this.handler = handler;
			this.plurlSetterClass = plurlSetterClass;
			openConnection = findMethod(plurlUrlHandlerClass, "openConnection", URL.class); //$NON-NLS-1$
			openConnectionProxy = findMethod(plurlUrlHandlerClass, "openConnection", URL.class, Proxy.class); //$NON-NLS-1$
			parseURL = findMethod(plurlUrlHandlerClass, "parseURL", plurlSetterClass, URL.class, String.class, //$NON-NLS-1$
					Integer.TYPE, Integer.TYPE);
			equals = findMethod(plurlUrlHandlerClass, "equals", URL.class, URL.class); //$NON-NLS-1$
			getDefaultPort = findMethod(plurlUrlHandlerClass, "getDefaultPort"); //$NON-NLS-1$
			getHostAddress = findMethod(plurlUrlHandlerClass, "getHostAddress", URL.class); //$NON-NLS-1$
			hashCode = findMethod(plurlUrlHandlerClass, "hashCode", URL.class); //$NON-NLS-1$
			hostsEqual = findMethod(plurlUrlHandlerClass, "hostsEqual", URL.class, URL.class); //$NON-NLS-1$
			sameFile = findMethod(plurlUrlHandlerClass, "sameFile", URL.class, URL.class); //$NON-NLS-1$
			toExternalForm = findMethod(plurlUrlHandlerClass, "toExternalForm", URL.class); //$NON-NLS-1$
			setURL = findMethod(plurlUrlHandlerClass, "setURL", URL.class, String.class, String.class, int.class, //$NON-NLS-1$
					String.class, String.class, String.class, String.class, String.class);
			setURLDeprecated = findMethod(plurlUrlHandlerClass, "setURL", URL.class, String.class, String.class, //$NON-NLS-1$
					int.class, String.class, String.class);
		}

		private static Method findMethod(Class<?> plurlUrlHandlerClass, String methodName, Class<?>... args) {
			Method result = null;
			try {
				result = plurlUrlHandlerClass.getDeclaredMethod(methodName, args);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return result;
		}

		Object invoke(Method m, Object... args) {
			try {
				return m.invoke(handler, args);
			} catch (InvocationTargetException e) {
				throw (RuntimeException) e.getTargetException();
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}

		@Override
		public boolean equals(URL u1, URL u2) {
			return (boolean) invoke(equals, u1, u2);
		}

		@Override
		public int hashCode(URL u) {
			return (int) invoke(hashCode, u);
		}

		@Override
		public boolean hostsEqual(URL u1, URL u2) {
			return (boolean) invoke(hostsEqual, u1, u2);
		}

		@Override
		public int getDefaultPort() {
			return (int) invoke(getDefaultPort);
		}

		@Override
		public InetAddress getHostAddress(URL u) {
			return (InetAddress) invoke(getHostAddress, u);
		}

		@Override
		public URLConnection openConnection(URL u) throws IOException {
			return (URLConnection) invoke(openConnection, u);
		}

		@Override
		public URLConnection openConnection(URL u, Proxy p) throws IOException {
			return (URLConnection) invoke(openConnectionProxy, u, p);
		}

		@Override
		public void parseURL(PlurlSetter plurlSetter, URL u, String spec, int start, int limit) {
			setHandler(u, handler);
			Object plurlSetterProxy = null;
			if (plurlSetter != null) {
				plurlSetterProxy = java.lang.reflect.Proxy.newProxyInstance(handler.getClass().getClassLoader(),
					new Class[] { plurlSetterClass }, new InvocationHandler() {

						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							if ("setURL".equals(method.getName())) { //$NON-NLS-1$
									if (args.length == 9) {
									plurlSetter.setURL((URL) args[0], (String) args[1], (String) args[2], (int) args[3],
											(String) args[4], (String) args[5], (String) args[6], (String) args[7],
											(String) args[8]);
									} else {
										plurlSetter.setURL((URL) args[0], (String) args[1], (String) args[2],
												(int) args[3], null, null, (String) args[4], null, (String) args[5]);
								}
							}
							return null;
						}
					});
			}
			invoke(parseURL, plurlSetterProxy, u, spec, start, limit);
		}

		@Override
		public boolean sameFile(URL u1, URL u2) {
			return (boolean) invoke(sameFile, u1, u2);
		}

		@Override
		public String toExternalForm(URL u) {
			return (String) invoke(toExternalForm, u);
		}

		@Override
		public void setURL(URL u, String protocol, String host, int port, String authority, String userInfo,
				String path, String query, String ref) {
			invoke(setURL, u, protocol, host, port, authority, userInfo, path, query, ref);
		}

		@SuppressWarnings("deprecation")
		@Override
		public void setURL(URL u, String protocol, String host, int port, String file, String ref) {
			invoke(setURLDeprecated, u, protocol, host, port, file, ref);
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + '@' + System.identityHashCode(this) + '[' + handler + ']';
		}
	}

	static class PlurlStreamHandlerReflective extends URLStreamHandler implements PlurlStreamHandler {
		private final URLStreamHandler handler;
		private final Method openConnectionMethod;
		private final Method openConnectionProxyMethod;
		private final Method parseURLMethod;
		private final Method equalsMethod;
		private final Method getDefaultPortMethod;
		private final Method getHostAddressMethod;
		private final Method hashCodeMethod;
		private final Method hostsEqualMethod;
		private final Method sameFileMethod;
		private final Method toExternalFormMethod;

		public PlurlStreamHandlerReflective(URLStreamHandler handler) {
			this.handler = handler;
			openConnectionMethod = findMethod(handler, "openConnection", URL.class); //$NON-NLS-1$
			openConnectionProxyMethod = findMethod(handler, "openConnection", URL.class, Proxy.class); //$NON-NLS-1$
			parseURLMethod = findMethod(handler, "parseURL", URL.class, String.class, Integer.TYPE, Integer.TYPE); //$NON-NLS-1$
			equalsMethod = findMethod(handler, "equals", URL.class, URL.class); //$NON-NLS-1$
			getDefaultPortMethod = findMethod(handler, "getDefaultPort"); //$NON-NLS-1$
			getHostAddressMethod = findMethod(handler, "getHostAddress", URL.class); //$NON-NLS-1$
			hashCodeMethod = findMethod(handler, "hashCode", URL.class); //$NON-NLS-1$
			hostsEqualMethod = findMethod(handler, "hostsEqual", URL.class, URL.class); //$NON-NLS-1$
			sameFileMethod = findMethod(handler, "sameFile", URL.class, URL.class); //$NON-NLS-1$
			toExternalFormMethod = findMethod(handler, "toExternalForm", URL.class); //$NON-NLS-1$
			if (URL_HANDLER_FIELD == null) {
				throw new RuntimeException(getReflectionErrorMessage(handler.getClass()));
			}
		}

		@SuppressWarnings("unchecked")
		private <T extends Throwable> Object invoke(Method m, Object... args) throws T {
			try {
				return m.invoke(handler, args);
			} catch (InvocationTargetException e) {
				throw (T) e.getTargetException();
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}

		private static String getReflectionErrorMessage(Class<? extends URLStreamHandler> handlerClass) {
			return "The java.base module must be configured to open the java.net package for reflection to support the handler of type " //$NON-NLS-1$
					+ '\'' + handlerClass.getName() + "'." //$NON-NLS-1$
					+ " For example, by using the JVM option: '--add-opens java.base/java.net=ALL-UNNAMED'. " //$NON-NLS-1$
					+ "Another option is to make the class '" + handlerClass.getName() //$NON-NLS-1$
					+ "' implement the org.eclipse.equinox.purl.PlurlStreamHandler interface."; //$NON-NLS-1$
		}

		private static Method findMethod(URLStreamHandler h, String methodName, Class<?>... args) {
			Method result = null;
			Class<? extends URLStreamHandler> handlerClass = h.getClass();
			try {
				result = handlerClass.getDeclaredMethod(methodName, args);
				result.setAccessible(true);
			} catch (Exception e1) {
				try {
					result = URLStreamHandler.class.getDeclaredMethod(methodName, args);
					result.setAccessible(true);
				} catch (Exception e2) {
					// fallback reflection is blocked by Java modules
					String message = getReflectionErrorMessage(handlerClass);
					throw new RuntimeException(message, e2);
				}
			}
			return result;
		}

		@Override
		public boolean equals(URL u1, URL u2) {
			return (Boolean) invoke(equalsMethod, u1, u2);
		}

		@Override
		public int hashCode(URL u) {
			return (Integer) invoke(hashCodeMethod, u);
		}

		@Override
		public boolean hostsEqual(URL u1, URL u2) {
			return (Boolean) invoke(hostsEqualMethod, u1, u2);
		}

		@Override
		public int getDefaultPort() {
			return (Integer) invoke(getDefaultPortMethod);
		}

		@Override
		public InetAddress getHostAddress(URL u) {
			return (InetAddress) invoke(getHostAddressMethod, u);
		}

		@Override
		public URLConnection openConnection(URL u) throws IOException {
			return (URLConnection) invoke(openConnectionMethod, u);
		}

		@Override
		public URLConnection openConnection(URL u, Proxy p) throws IOException {
			return (URLConnection) invoke(openConnectionProxyMethod, u, p);
		}

		@Override
		public boolean sameFile(URL u1, URL u2) {
			return (Boolean) invoke(sameFileMethod, u1, u2);
		}

		@Override
		public String toExternalForm(URL u) {
			return (String) invoke(toExternalFormMethod, u);
		}

		@Override
		public void parseURL(PlurlSetter plurlSetter, URL u, String spec, int start, int limit) {
			try {
				setHandler(u, handler);
				invoke(parseURLMethod, u, spec, start, limit);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@SuppressWarnings("deprecation")
		@Override
		public void setURL(URL u, String protocol, String host, int port, String file, String ref) {
			super.setURL(u, protocol, host, port, file, ref);
		}

		@Override
		public void setURL(URL u, String protocol, String host, int port, String authority, String userInfo,
				String path, String query, String ref) {
			super.setURL(u, protocol, host, port, authority, userInfo, path, query, ref);
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + '@' + System.identityHashCode(this) + '[' + handler + ']';
		}
	}

	static final PlurlStreamHandler NULL_HANDLER = new PlurlStreamHandlerBase() {
		@Override
		public URLConnection openConnection(URL u) throws IOException {
			throw new UnsupportedOperationException();
		}
	};

	class PlurlRootURLStreamHandler extends URLStreamHandler implements PlurlSetter {
		private final String protocol;
		private final AtomicReference<PlurlStreamHandler> builtin = new AtomicReference<>();

		private PlurlStreamHandler lookupPlurlStreamHandler(URL u) {
			if (u != null && isMultiplexing(getURLStreamHandlerFactories())) {
				// Record the handler found for the URL;
				// This allows to consistently use the same handler for the
				// life of the URL object when we are multiplexing.
				return urlToHandler.get(u, this::findPlurlStreamHandlerImpl);
			}
			return findPlurlStreamHandlerImpl();
		}

		private PlurlStreamHandler findPlurlStreamHandlerImpl() {
			PlurlStreamHandler h = findPlurlStreamHandler(protocol);
			if (h == null) {
				h = findBuiltin();
				if (h == null) {
					throw new IllegalStateException("No handler found for protocol: " + protocol); //$NON-NLS-1$
				}
			}
			return h;
		}

		private PlurlStreamHandler findBuiltin() {
			PlurlStreamHandler result = builtin.updateAndGet((h) -> {
				URLStreamHandler found = findBuildinURLStreamHandlerImpl(protocol, "sun.net.www.protocol"); //$NON-NLS-1$
				if (found == null) {
					return NULL_HANDLER;
				}
				// we can only really do this if java.net is open for reflection
				return new PlurlStreamHandlerReflective(found);
			});
			return (result == NULL_HANDLER) ? null : result;
		}

		PlurlRootURLStreamHandler(String protocol) {
			this.protocol = protocol;
		}

		@Override
		protected boolean equals(URL u1, URL u2) {
			return lookupPlurlStreamHandler(u1).equals(u1, u2);
		}

		@Override
		protected int hashCode(URL u) {
			return lookupPlurlStreamHandler(u).hashCode(u);
		}

		@Override
		protected boolean hostsEqual(URL u1, URL u2) {
			return lookupPlurlStreamHandler(u1).hostsEqual(u1, u2);
		}

		@Override
		protected int getDefaultPort() {
			return lookupPlurlStreamHandler(null).getDefaultPort();
		}

		@Override
		protected InetAddress getHostAddress(URL u) {
			return lookupPlurlStreamHandler(u).getHostAddress(u);
		}

		@Override
		protected URLConnection openConnection(URL u) throws IOException {
			return lookupPlurlStreamHandler(u).openConnection(u);
		}

		@Override
		protected URLConnection openConnection(URL u, Proxy p) throws IOException {
			return lookupPlurlStreamHandler(u).openConnection(u, p);
		}

		@Override
		protected void parseURL(URL u, String spec, int start, int limit) {
			PlurlStreamHandler h = lookupPlurlStreamHandler(u);
			if (setHandler(u, h)) {
				h.parseURL(null, u, spec, start, limit);
			} else {
				h.parseURL(this, u, spec, start, limit);
			}
		}

		@Override
		protected boolean sameFile(URL u1, URL u2) {
			return lookupPlurlStreamHandler(u1).sameFile(u1, u2);
		}

		@Override
		protected String toExternalForm(URL u) {
			return lookupPlurlStreamHandler(u).toExternalForm(u);
		}

		@Override
		public void setURL(URL u, String protocol, String host, int port, String authority, String userInfo,
				String path, String query, String ref) {
			super.setURL(u, protocol, host, port, authority, userInfo, path, query, ref);
		}
	}
}
