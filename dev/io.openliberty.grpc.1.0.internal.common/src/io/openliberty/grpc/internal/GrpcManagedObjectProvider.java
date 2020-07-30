package io.openliberty.grpc.internal;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.state.ModuleStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectException;
import com.ibm.ws.managedobject.ManagedObjectFactory;
import com.ibm.ws.managedobject.ManagedObjectService;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(name = "io.openliberty.grpc.internal.GrpcManagedObjectProvider", immediate = true, property = { "service.vendor=IBM" })
public class GrpcManagedObjectProvider implements ModuleStateListener {
	
    private static final TraceComponent tc = Tr.register(GrpcManagedObjectProvider.class);
	
    private final static AtomicServiceReference<ManagedObjectService> managedObjectServiceSRRef = new AtomicServiceReference<ManagedObjectService>("managedObjectService");
    
    private static final Map<String, Map<Class<?>, ManagedObjectFactory<?>>> managedObjectFactoryCache = new ConcurrentHashMap<>();

    @Reference(name = "managedObjectService",
            service = ManagedObjectService.class,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY)
	protected void setManagedObjectService(ServiceReference<ManagedObjectService> ref) {
    	managedObjectServiceSRRef.setReference(ref);
	}
	
	protected void unsetManagedObjectService(ServiceReference<ManagedObjectService> ref) {
		managedObjectServiceSRRef.unsetReference(ref);
	}	

   public void activate(ComponentContext context, Map<String, Object> properties) {
       managedObjectServiceSRRef.activate(context);

    }
   
   public void deactivate(ComponentContext context) {
       managedObjectServiceSRRef.deactivate(context);
   }
	
   /**
    * Create a ManagedObject for the given Class
    * 
    * @param clazz
    * @return ManagedObject
    */
	public static ManagedObject<?> createManagedObject(Class<?> clazz) throws ManagedObjectException {
	    ManagedObject<?> mo = null;
	    ManagedObjectFactory<?> mof = getManagedObjectFactory(clazz);    
	    if (mof != null) {
	        mo = mof.createManagedObject();
	    }
		return mo;
	}
	
	/**
	 * Given a fully qualified class name:
	 * 1. create a Class from the class name using the TCCL 
	 * 2. create a ManagedObject from that class
	 * 3. return ManagedObject.getObject() 
	 * 
	 * @param className
	 * @return Object
	 */
	public static Object createObjectFromClassName(String className) throws InstantiationException, IllegalAccessException, 
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ManagedObjectException, 
			ClassNotFoundException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Class<?> clazz = Class.forName(className, true, cl);
		return createObjectFromClass(clazz);
	}
	
	/**
	 * Given a Class, create a ManagedObject and return ManagedObject.getObject().
	 * If that resulting object is null, attempt to return a newInstance()
	 * 
	 * @param Class
	 * @return Object
	 */
	public static Object createObjectFromClass(Class<?> clazz) throws InstantiationException, IllegalAccessException, 
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ManagedObjectException {
		ManagedObject<?> mo = createManagedObject(clazz);
		Object returnObj = null;
		if (mo != null) {
			returnObj = mo.getObject();
		} 
		if (returnObj == null) {
			// something went wrong with the managed object creation; return a basic newInstance()
			returnObj = clazz.getDeclaredConstructor().newInstance();
		}
		return returnObj;
	}
	
    private static ManagedObjectFactory<?> getManagedObjectFactory(Class<?> clazz) {
        ManagedObjectFactory<?> mof = null;
        try {
            ModuleMetaData mmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData().getModuleMetaData();
            Map<Class<?>, ManagedObjectFactory<?>> cache = managedObjectFactoryCache.get(mmd.getName());
            if (cache != null) {
                mof = cache.get(clazz);
            } else {
                managedObjectFactoryCache.putIfAbsent(mmd.getName(), new ConcurrentHashMap<Class<?>, ManagedObjectFactory<?>>());
                cache = managedObjectFactoryCache.get(mmd.getName());
            }
            if (mof != null) {
                return mof;
            }

            ManagedObjectService mos = managedObjectServiceSRRef.getServiceWithException();
            if (mos == null) {
                return null;
            }

            mof = mos.createManagedObjectFactory(mmd, clazz, true);
            cache.put(clazz, mof);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Successfully created ManagedObjectFactory for class: " + clazz.getName());
            }
        } catch (ManagedObjectException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to create ManagedObjectFactory for class: " + clazz.getName() + " Exception is: " + e.toString());
            }
        }
        return mof;
    }

	@Override
	public void moduleStarting(ModuleInfo moduleInfo) throws StateChangeException {	}

	@Override
	public void moduleStarted(ModuleInfo moduleInfo) throws StateChangeException { }

	@Override
	public void moduleStopping(ModuleInfo moduleInfo) {
        managedObjectFactoryCache.remove(moduleInfo.getApplicationInfo().getName());
	}

	@Override
	public void moduleStopped(ModuleInfo moduleInfo) { }
}
