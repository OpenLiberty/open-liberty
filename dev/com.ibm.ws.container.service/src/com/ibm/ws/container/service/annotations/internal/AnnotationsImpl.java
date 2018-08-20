/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.annotations.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_ClassLoader;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;
import com.ibm.wsspi.anno.classsource.ClassSource_Factory;
import com.ibm.wsspi.anno.classsource.ClassSource_Options;
import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.anno.info.InfoStore;
import com.ibm.wsspi.anno.info.InfoStoreException;
import com.ibm.wsspi.anno.info.InfoStoreFactory;
import com.ibm.wsspi.anno.service.AnnotationService_Service;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Exception;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Factory;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

import com.ibm.ws.container.service.annotations.Annotations;
import com.ibm.ws.container.service.annotations.SpecificAnnotations;

/**
 * Common annotations code.
 */
public abstract class AnnotationsImpl implements Annotations {
    public static final TraceComponent tc = Tr.register(AnnotationsImpl.class);

    @SuppressWarnings("unchecked")
    protected static <T> T cacheGet(
    	OverlayContainer container,
    	String targetPath, Class<T> targetClass) {

        Object retrievedInfo = container.getFromNonPersistentCache(targetPath, targetClass);
        return (T) retrievedInfo;
    }

    protected static <T> void cachePut(
        OverlayContainer container,
        String targetPath, Class<T> targetClass,
        T targetObject) {

    	container.addToNonPersistentCache(targetPath, targetClass, targetObject);
    }

    protected static <T> void cacheRemove(
        OverlayContainer container,
        String targetPath, Class<T> targetClass) {

    	container.removeFromNonPersistentCache(targetPath, targetClass);
    }

    //

    /**
     * Create annotations data for an annotations adapter.
     *
     * The adapter provides access to service entities, most importantly, the
     * annotation factories.
     *
     * @param annotationsAdapter The adapter which is creating the annotations data.
     * @param rootContainer The root adaptable container.  Currently always the same
     *     as 'rootAdaptableContainer'.
     * @param rootOverlayContainer The root overlay container.
     * @param rootDelegateContainer The delegate of the root adaptable container.
     * @param rootAdaptableContainer The root adaptable container.  Currently, always
     *     the same as 'rootContainer'.
     * @param appName The name of the enclosing application.  Null if there is no enclosing
     *     application.
     * @param modName The name of the enclosing module.  Null if there is no enclosing module.
     * @param modCatName A category name for the module.  Used to enable multiple results for
     *     ths same module.
     */
	public AnnotationsImpl(
		AnnotationsAdapterImpl annotationsAdapter,
		Container rootContainer, OverlayContainer rootOverlayContainer,
		ArtifactContainer rootDelegateContainer, Container rootAdaptableContainer,
		String appName, String modName, String modCatName) {

		this.annotationsAdapter = annotationsAdapter;

	    this.rootContainer = rootContainer; // Currently the same as 'rootAdaptableContainer', and unused.

	    this.rootDelegateContainer = rootDelegateContainer; // Delegate underlying the target container.
	    this.rootOverlayContainer = rootOverlayContainer; // Overlay view of the target container.
	    this.rootAdaptableContainer = rootAdaptableContainer; // Adaptable view of the target container.

	    this.appName = appName;
	    this.modName = modName;
	    this.modCatName = modCatName;

	    this.isSetClassSource = false;
	    this.classSource = null;

	    this.isSetTargets = false;
	    this.annotationTargets = null;

	    this.isSetInfoStore = false;
	    this.infoStore = null;
	}

	//

	private final AnnotationsAdapterImpl annotationsAdapter;

	public AnnotationService_Service getAnnotationService() {
		try {
			return annotationsAdapter.getAnnotationService(rootAdaptableContainer);
			// throws UnableToAdaptException
		} catch ( UnableToAdaptException e ) {
			return null; // FFDC
		}
	}

	public ClassSource_Factory getClassSourceFactory() {
		AnnotationService_Service useService = getAnnotationService();
		return ( (useService == null) ? null : useService.getClassSourceFactory() );
	}

	public AnnotationTargets_Factory getTargetsFactory() {
		AnnotationService_Service useService = getAnnotationService();
		return ( (useService == null) ? null : useService.getAnnotationTargetsFactory() );
	}

    public InfoStoreFactory getInfoStoreFactory() {
		AnnotationService_Service useService = getAnnotationService();
		return ( (useService == null) ? null : useService.getInfoStoreFactory() );
    }

	//

	@SuppressWarnings("unused")
	private final Container rootContainer;

	//

	private final ArtifactContainer rootDelegateContainer;

	public ArtifactContainer getRootDelegateContainer() {
		return rootDelegateContainer;
	}

	//

	private final OverlayContainer rootOverlayContainer;

	public OverlayContainer getRootOverlayContainer() {
		return rootOverlayContainer;
	}

    protected <T> T cacheGet(Class<T> targetClass) {
    	return cacheGet(getRootOverlayContainer(), getContainerPath(), targetClass);
    }

    protected <T> void cachePut(Class<T> targetClass, T targetObject) {
    	cachePut( getRootOverlayContainer(), getContainerPath(), targetClass, targetObject);
    }

    protected <T> void cacheRemove(Class<T> targetClass) {
    	cacheRemove( getRootOverlayContainer(), getContainerPath(), targetClass);
    }

    //

	private final Container rootAdaptableContainer;

	@Override
	public Container getContainer() {
		return rootAdaptableContainer;
	}

	@Override
	public String getContainerName() {
		return getContainer().getName();
	}

	@Override 
	public String getContainerPath() {
		return getContainer().getPath();
	}

	//

	private boolean useJandex;

	@Override
	public boolean getUseJandex() {
		return useJandex;
	}

	@Override
	public void setUseJandex(boolean useJandex) {
		this.useJandex = useJandex;
	}

	protected ClassSource_Options createOptions() {
		ClassSource_Factory classSourceFactory = getClassSourceFactory();
		if ( classSourceFactory == null ) {
			return null;
		}

		ClassSource_Options options = classSourceFactory.createOptions();
		options.setUseJandex( getUseJandex() );
		return options;
	}

	private String appName;

	@Override
	public String getAppName() {
		return appName;
	}

	@Override
	public void setAppName(String appName) {
		this.appName = appName;
	}

	private String modName;

	@Override
	public String getModName() {
		return modName;
	}

	@Override
	public void setModName(String modName) {
		this.modName = modName;
	}

	private String modCatName;
	
	@Override
	public String getModCategoryName() {
		return modCatName;
	}

	@Override
	public void setModCategoryName(String modCatName) {
		this.modCatName = modCatName;
	}

	//

	public class ClassSourceLock {
		// EMPTY
	}
	private final ClassSourceLock classSourceLock = new ClassSourceLock();

	protected ClassLoader classLoader;

	protected boolean isSetClassSource;
	protected ClassSource_Aggregate classSource;

	@Override
	public ClassLoader getClassLoader() {
		synchronized ( classSourceLock ) {
			return classLoader;
		}
	}

	@Override
	public void setClassLoader(ClassLoader classLoader) {
		synchronized ( classSourceLock ) { 
			if ( this.classLoader != null ) {
				if ( this.classLoader != classLoader ) {
					throw new IllegalArgumentException("Duplicate class loader [ " + classLoader + " ]; previous [ " + this.classLoader + " ]");
				} else {
					return; // Nothing to do.
				}
			} else {
				this.classLoader = classLoader;
				addExternalToClassSource();
			}
		}
	}

	//

	@Override
	public ClassSource_Aggregate releaseClassSource() {
		synchronized ( classSourceLock ) { 
			if ( !isSetClassSource ) {
				return null;
			}

			isSetClassSource = false;

			ClassSource_Aggregate oldClassSource = classSource;
			classSource = null;

			cacheRemove(ClassSource_Aggregate.class);

			return oldClassSource;
		}
	}

	@Override
	public ClassSource_Aggregate getClassSource() {
		synchronized( classSourceLock ) {
			if ( classSource == null ) {
				classSource = createClassSource();
				addInternalToClassSource();
				addExternalToClassSource();
			}
			return classSource;
		}
	}

	protected ClassSource_Aggregate createClassSource() {
		ClassSource_Factory classSourceFactory = getClassSourceFactory();
		if ( classSourceFactory == null ) {
			return null;
		}

		String useAppName = getAppName();
		String useModName = getModName();
		String useModCatName = getModCategoryName();

		ClassSource_Options options = createOptions();

		try {
			return classSourceFactory.createAggregateClassSource(useAppName, useModName, useModCatName, options);
			// throws ClassSource_Exception

		} catch ( ClassSource_Exception e ) {
			return null; // FFDC
		}
	}

	protected abstract void addInternalToClassSource();

	protected void addExternalToClassSource() {
		if ( (classSource == null) || (classLoader == null) ) {
			return; // Nothing yet to do.
		}

		ClassSource_Factory classSourceFactory = getClassSourceFactory();
		if ( classSourceFactory == null ) {
			return;
		}

		ClassSource_ClassLoader classLoaderClassSource;
		try {
			classLoaderClassSource = classSourceFactory.createClassLoaderClassSource(
				classSource, "classloader", classLoader); // throws ClassSource_Exception
		} catch ( ClassSource_Exception e ) {
			return; // FFDC
		}
		classSource.addClassLoaderClassSource(classLoaderClassSource);
	}

	//

	public class TargetsLock {
		// EMPTY
	}
	private final TargetsLock targetsLock = new TargetsLock();

	private boolean isSetTargets;
	private AnnotationTargets_Targets annotationTargets;

	@Override
	public AnnotationTargets_Targets getTargets() {
		synchronized( targetsLock ) {
			if ( !isSetTargets ) {
				isSetTargets = true;
				annotationTargets = createTargets();
			}
			return annotationTargets;
		}
	}

	@Override
	public AnnotationTargets_Targets releaseTargets() {
		synchronized ( targetsLock ) {
			if ( !isSetTargets ) {
				return null;
			}

			isSetTargets = false;

			AnnotationTargets_Targets oldTargets = annotationTargets;
			annotationTargets = null;

			cacheRemove(AnnotationTargets_Targets.class);

			return oldTargets;
		}
	}

	protected AnnotationTargets_Targets createTargets() {
		ClassSource_Aggregate useClassSource = getClassSource();
		if ( useClassSource == null ) {
			return null;
		}

		AnnotationTargets_Factory targetsFactory = getTargetsFactory();
		if ( targetsFactory == null ) {
			return null;
		}

		AnnotationTargets_Targets useTargets;
		try {
			useTargets = targetsFactory.createTargets(); // throws AnnotationTargets_Exception
		} catch ( AnnotationTargets_Exception e ) {
			return null; // FFDC
		}

		useTargets.scan(useClassSource);

		return useTargets;
	}

	//

	public class InfoStoreLock {
		// EMPTY
	}
	private final InfoStoreLock infoStoreLock = new InfoStoreLock();

	private boolean isSetInfoStore;
	private InfoStore infoStore;

	@Override
	public InfoStore releaseInfoStore() {
		synchronized ( infoStoreLock ) {
			if ( !isSetInfoStore ) {
				return null;
			}

			isSetInfoStore = false;

			InfoStore oldStore = infoStore;
			infoStore = null;

			cacheRemove(InfoStore.class);

			return oldStore;
		}
	}

	public InfoStore getInfoStore() {
		synchronized( infoStoreLock ) {
			if ( !isSetInfoStore ) {
				isSetInfoStore = true;
				infoStore = createInfoStore();
			}

			return infoStore;
		}
	}

	protected InfoStore createInfoStore() {
		ClassSource_Aggregate useClassSource = getClassSource();
		if ( useClassSource == null ) {
			return null;
		}

		InfoStoreFactory infoStoreFactory = getInfoStoreFactory();
		if ( infoStoreFactory == null ) {
			return null;
		}

		try {
			return infoStoreFactory.createInfoStore(useClassSource);
		} catch ( InfoStoreException e ) {
			return null; // FFDC
		}
    }

	//

    @Override
   public boolean isIncludedClass(String className) {
    	AnnotationTargets_Targets useTargets = getTargets();
    	if ( useTargets == null ) {
    		return false;
    	} else {
    		return useTargets.isSeedClassName(className);
    	}
   }

    @Override
   public boolean isPartialClass(String className) {
    	AnnotationTargets_Targets useTargets = getTargets();
    	if ( useTargets == null ) {
    		return false;
    	} else {
    		return useTargets.isPartialClassName(className);
    	}
   }

    @Override
   public boolean isExcludedClass(String className) {
    	AnnotationTargets_Targets useTargets = getTargets();
    	if ( useTargets == null ) {
    		return false;
    	} else {
    		return useTargets.isExcludedClassName(className);
    	}
   }

    @Override
   public boolean isExternalClass(String className) {
    	AnnotationTargets_Targets useTargets = getTargets();
    	if ( useTargets == null ) {
    		return false;
    	} else {
    		return useTargets.isExternalClassName(className);
    	}
    }

	//

    @Override
    public ClassInfo getClassInfo(String className) {
        return getInfoStore().getDelayableClassInfo(className);
    }

    @Override
    public void openInfoStore() {
        try {
            getInfoStore().open();
        } catch ( InfoStoreException e ) {
        	// FFDC
        }
    }

    @Override
    public void closeInfoStore() {
        try {
            getInfoStore().close();
        } catch ( InfoStoreException e ) {
        	// FFDC
        }
    }

    //

    @Override
    public SpecificAnnotations getSpecificAnnotations(Set<String> specificClassNames) throws UnableToAdaptException {
        ClassSource_Aggregate useClassSource = getClassSource();
        if ( useClassSource == null ) {
        	return null;
        }

        AnnotationTargets_Factory useTargetsFactory = getTargetsFactory();
        if ( useTargetsFactory == null ) {
        	return null;
        }

        AnnotationTargets_Targets specificTargets;
        try {
            specificTargets = useTargetsFactory.createTargets();
        } catch (AnnotationTargets_Exception e) {
            String msg = Tr.formatMessage(tc, "failed.to.obtain.module.annotation.targets.CWWKM0473E",
            	"Failed to obtain annotation targets", e);
            throw new UnableToAdaptException(msg);
        }

        try {
            specificTargets.scan(useClassSource, specificClassNames);
        } catch ( AnnotationTargets_Exception e ) {
            String msg = Tr.formatMessage(tc, "failed.to.obtain.module.annotation.targets.CWWKM0474E",
            	"Failed to obtain annotation targets", specificClassNames, e);
            throw new UnableToAdaptException(msg);
        }

        return new SpecificAnnotationsImpl(specificTargets);
    }

    //

    @Override
    public boolean hasSpecifiedAnnotations(Collection<String> annotationClassNames) {
        AnnotationTargets_Targets useTargets = getTargets();
        if ( useTargets == null ) {
        	return false;
        }

        // d95160: The prior implementation obtained classes from the SEED location.
        //         That implementation is not changed by d95160.

        for ( String annotationClassName : annotationClassNames ) {
            Set<String> annotatedClassNames = useTargets.getAnnotatedClasses(annotationClassName);
            if ( !annotatedClassNames.isEmpty() ) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<String> getClassesWithSpecifiedInheritedAnnotations(Collection<String> annotationClassNames) {
        AnnotationTargets_Targets useTargets = getTargets();
        if ( useTargets == null ) {
        	return Collections.emptySet();
        }

        Set<String> allAnnotatedClassNames = new HashSet<String>();

        for ( String annotationClassName : annotationClassNames ) {
            Set<String> annotatedClassNames =
                useTargets.getAllInheritedAnnotatedClasses(annotationClassName);
            allAnnotatedClassNames.addAll(annotatedClassNames);
        }

        return allAnnotatedClassNames;
    }

    //

    protected long scanStart() {
        long startTime = getTimeInNanoSec();
        return startTime;
    }

    protected long scanEnd(long startTime) {
        long endTime = getTimeInNanoSec();
        long elapsedTime = endTime - startTime;
        return elapsedTime;
    }

    private long getTimeInNanoSec() {
        return System.nanoTime();
    }
}
