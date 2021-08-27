package com.ibm.ws.logging.internal.osgi.stackjoiner;
import java.lang.instrument.Instrumentation;
import org.osgi.framework.BundleContext;

public class ThrowableProxyActivatorStorage{
	private static ThrowableProxyActivatorStorage throwableProxyActivatorStorage = null;
	private Instrumentation inst;
	private BundleContext bundleContext;
	
	
	private ThrowableProxyActivatorStorage(Instrumentation inst, BundleContext bundleContext) {
		this.inst = inst;
		this.bundleContext = bundleContext;
		
	}
	
	/**
	* Create getInstance method which is the only way that WeakSet object can be accessed.
	*/
	public synchronized static ThrowableProxyActivatorStorage getInstance(Instrumentation inst, BundleContext bundleContext) {
		if (throwableProxyActivatorStorage == null) {
			throwableProxyActivatorStorage = new ThrowableProxyActivatorStorage(inst, bundleContext);

		}
	    return throwableProxyActivatorStorage;
	}
	
	/**
	* Create getInstance method which is the only way that WeakSet object can be accessed.
	*/
	public synchronized static ThrowableProxyActivatorStorage getInstance() {
	    return throwableProxyActivatorStorage;
	}
	
	public Instrumentation getInstrumentation() {
		return inst;
	}
	
	public BundleContext getBundleContext() {
		return bundleContext;
	}
	
}
