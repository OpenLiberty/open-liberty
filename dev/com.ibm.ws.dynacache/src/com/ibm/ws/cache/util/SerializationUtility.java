/*******************************************************************************
 * Copyright (c) 1997, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.security.AccessController;
import java.util.Collection;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.CacheService;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.serialization.DeserializationObjectInputStream;
import com.ibm.ws.serialization.SerializationService;
import com.ibm.ws.util.ThreadContextAccessor;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.library.Library;

/**
 * This is a helper class that factors out the code to serialize and deserialize an object.
 * It is used by various classes in the dynacache package.
 */
public class SerializationUtility {

	private static TraceComponent tc = Tr.register(SerializationUtility.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
	
	private static final ThreadContextAccessor threadContextAccessor = AccessController.doPrivileged(ThreadContextAccessor.getPrivilegedAction());
	public static ClassLoader getContextClassLoader() { return threadContextAccessor.getContextClassLoaderForUnprivileged(Thread.currentThread());	}
	private final AtomicServiceReference<SerializationService> serializationServiceRef = new AtomicServiceReference<SerializationService>("serializationService");
	
	private static SerializationUtility _instance;
	private static BundleContext _bundleContext;
	
	public void activate(ComponentContext context) {
    	serializationServiceRef.activate(context);
    	_instance = this;
    	_bundleContext = context.getBundleContext();
    }
	
    public void deactivate(ComponentContext context) {
    	serializationServiceRef.deactivate(context);
    	_instance = null;
    	_bundleContext = null;
    }
	
   /**
	* This serializes an object into a byte array.
	*
	* @param serializable The object to be serialized.
	* @return The serialized object.
	*/
	public static final byte[] serialize(Serializable serializable) throws IOException {
	
		if (serializable == null) {
			return null;
		}
		
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = null;
		byte[] result = null;

		if (null != _instance ){
			SerializationService ss = _instance.serializationServiceRef.getService();
			if (null != ss){
				objectOutputStream = ss.createObjectOutputStream(byteArrayOutputStream);
			}
		} 
		
		if (null == objectOutputStream){
			objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
		}
		
		try {
			objectOutputStream.writeObject(serializable);
			result = byteArrayOutputStream.toByteArray();
		} finally {

			if (byteArrayOutputStream != null) {
				byteArrayOutputStream.close();
			}
			if (objectOutputStream != null) {
				objectOutputStream.close();
			}
		}

		return result;
	}
	
	public static Library getSharedLibrary(String cacheName) {
		Library sharedLib = null;
		try {
			Collection<ServiceReference<CacheService>> cacheServiceRefs = 
					_bundleContext.getServiceReferences(CacheService.class, "(id=" + cacheName + ")");
//			if (tc.isDebugEnabled()){
//				Tr.debug(tc, "Retrieved cache service references", cacheServiceRefs);
//			}
			if (null != cacheServiceRefs && !cacheServiceRefs.isEmpty()){
			    ServiceReference<CacheService> csRef = cacheServiceRefs.iterator().next();
				CacheService cs = _bundleContext.getService(csRef);
//				if (tc.isDebugEnabled()){
//					Tr.debug(tc, "Retrieved cache ", cs.getCacheConfig());
//				}
				sharedLib = cs.getSharedLibrary();
			} else {
//				if (tc.isDebugEnabled()){
//					cacheServiceRefs =  _bundleContext.getServiceReferences(CacheService.class, null);
//					for (ServiceReference<CacheService> serviceReference : cacheServiceRefs) {
//						CacheService cs =  _bundleContext.getService(serviceReference);
//						Tr.debug(tc, "Retrieved cache ",cs.getCacheName(), cs.getSharedLibrary(), serviceReference.getPropertyKeys());
//					}
//				}
			}
		} catch (InvalidSyntaxException e) {
		    FFDCFilter.processException(e, SerializationUtility.class.getName(), "getSharedLibrary");
		}
		return sharedLib;
	}

   /**
	* This deserializes a byte array into an object.
	*
	* @param array The object to be deserialized.
	* @return The deserialized object.
	*/
	@FFDCIgnore({ClassNotFoundException.class })
	public static final Serializable deserialize(byte[] array, String cacheName) throws IOException, ClassNotFoundException {

		if (array == null) {
			return null;
		}
		
		Serializable object = null;

		try {
			if (null != _instance) {
				SerializationService ss = _instance.serializationServiceRef.getService();
				if (null != ss) {
					try {
						ClassLoader tccl = getContextClassLoader();
						ObjectInputStream objectInputStream = ss.createObjectInputStream(new ByteArrayInputStream(array), tccl);
						object = (Serializable) objectInputStream.readObject();
					}catch (ClassNotFoundException e){
						if (null != cacheName){
							Library sharedLib = getSharedLibrary(cacheName);
							if (null != sharedLib){
								ObjectInputStream objectInputStream = new DeserializationObjectInputStream(new ByteArrayInputStream(array), sharedLib.getClassLoader());
								object = (Serializable) objectInputStream.readObject();
							}
						}
					}
				}
			}

			if (null == object) {
				object = deserializeUsingTCCL(array, 0, array.length, getContextClassLoader());
			}
		} catch (ClassNotFoundException e) {
			try {
				object = deserialize(array, 0, array.length);
			} catch (ClassNotFoundException e1) {
				throw e1;
			}
		}
		return object;
	}

   /**
    * This deserializes a byte array into an object.
    * @param array The object to be deserialized.
    * @return The deserialized object.
    */
   private static final Serializable deserialize(byte[] array, int offset, int length) throws IOException, ClassNotFoundException {  

	   ByteArrayInputStream byteArrayInputStream = null;
	  ObjectInputStream objectInputStream = null;
	  try {
          byteArrayInputStream = new ByteArrayInputStream(array, offset, length);
		  byteArrayInputStream.mark(array.length);
      	  objectInputStream = new ObjectInputStream(byteArrayInputStream);

		  return (Serializable) objectInputStream.readObject();
	  } catch (ClassNotFoundException cnfe) {
		  ClassLoader cl = getClassLoader();
		  if (cl != null) {
			  byteArrayInputStream.reset();
          	  objectInputStream = new WsObjectInputStream(byteArrayInputStream, cl);
			  return (Serializable) objectInputStream.readObject();
		  } else {
			  throw cnfe;
		  }
	  } catch (NoClassDefFoundError cnfe) {
		  ClassLoader cl = getClassLoader();
		  if (cl != null) {
			  byteArrayInputStream.reset();
          	  objectInputStream = new WsObjectInputStream(byteArrayInputStream, cl);
			  return (Serializable) objectInputStream.readObject();
		  } else {
			  throw cnfe;
		  }	    
	  } catch (StreamCorruptedException cnfe) {
		  ClassLoader cl = getClassLoader();
		  if (cl != null) {
			  byteArrayInputStream.reset();
          	  objectInputStream = new WsObjectInputStream(byteArrayInputStream, cl);
			  return (Serializable) objectInputStream.readObject();
		  } else {
			  throw cnfe;
		  }
	  } finally { 
          if (byteArrayInputStream != null) {
              byteArrayInputStream.close();
          }
          if (objectInputStream != null) {
              objectInputStream.close();
          }
      }
   }
		  
	private static final Serializable deserializeUsingTCCL(byte[] array, int offset, int length, ClassLoader tccl)
		throws IOException, ClassNotFoundException { 

		ByteArrayInputStream byteArrayInputStream = null;
		ObjectInputStream objectInputStream = null;
		try {
			byteArrayInputStream = new ByteArrayInputStream(array, offset, length);
			byteArrayInputStream.mark(array.length);
			byteArrayInputStream.reset();
			objectInputStream = new WsObjectInputStream( byteArrayInputStream, tccl);
			Object obj = objectInputStream.readObject();
			return (Serializable) obj;

		} catch (ClassNotFoundException cnfe) {
            FFDCFilter.processException(cnfe, SerializationUtility.class.getName() + ".deserializeUsingTCCL()", "248");
			throw cnfe;
		} catch (StreamCorruptedException cnfe) {
            FFDCFilter.processException(cnfe, SerializationUtility.class.getName() + ".deserializeUsingTCCL()", "251");
			throw cnfe;
		} finally {
			if (byteArrayInputStream != null) {
				byteArrayInputStream.close();
			}
			if (objectInputStream != null) {
				objectInputStream.close();
			}
		}
	}
	
	private static ClassLoader getClassLoader(){
		
		if (null == _bundleContext){
			return SerializationUtility.class.getClassLoader();
		} else {
			return _bundleContext.getClass().getClassLoader();
		}
	}

	protected void setSerializationService(ServiceReference<SerializationService> ref) {
        serializationServiceRef.setReference(ref);
    }

    protected void unsetSerializationService(ServiceReference<SerializationService> ref) {
        serializationServiceRef.unsetReference(ref);
    }
	
}