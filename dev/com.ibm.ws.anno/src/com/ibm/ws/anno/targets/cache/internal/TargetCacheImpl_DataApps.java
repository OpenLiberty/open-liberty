/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.anno.targets.cache.internal;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.anno.targets.cache.TargetCache_ExternalConstants;

public class TargetCacheImpl_DataApps extends TargetCacheImpl_DataBase {

    public static final String CLASS_NAME = TargetCacheImpl_DataApps.class.getSimpleName();

    //

    public TargetCacheImpl_DataApps(
        TargetCacheImpl_Factory factory,
        String cacheName, String e_cacheName) {

        super( factory,
               cacheName, e_cacheName,
               new File( factory.getCacheOptions().getDir() ),
               TargetCache_ExternalConstants.APP_PREFIX );

        this.activeApps = new WeakHashMap<String, TargetCacheImpl_DataApp>();
        this.appsLock = new AppsLock();
    }

    //

    @Trivial
    protected File e_getAppDir(String e_appName) {
        return getDataFile( e_addChildPrefix(e_appName) );
    }

    @Trivial    
    protected TargetCacheImpl_DataApp createAppData(String appName, String e_appName, File appDir) {
        return getFactory().createAppData(appName, e_appName, appDir);
    }

    @Trivial
    protected TargetCacheImpl_DataApp createAppData(String appName) {
        String e_appName = encode(appName);
        return createAppData( appName, e_appName, e_getAppDir(e_appName) );
    }

    @Trivial
    protected TargetCacheImpl_DataApp createAppData(File appDir) {
        String appDirName = appDir.getName();
        String e_appName = e_removeChildPrefix(appDirName);
        String appName = decode(e_appName);

        return createAppData(appName, e_appName, appDir);
    }

    //

    private class AppsLock {
        // EMPTY
    }
    private final AppsLock appsLock;
    private final WeakHashMap<String, TargetCacheImpl_DataApp> activeApps;

    @Trivial
    protected Map<String, TargetCacheImpl_DataApp> getActiveApps() {
        return activeApps;
    }

    @Trivial
    public Set<String> getActiveAppNames() {
        synchronized( appsLock ) {
            return getActiveApps().keySet();
        }
    }

    public TargetCacheImpl_DataApp getActiveApp(String appName) {
        synchronized( appsLock ) {
            return getActiveApps().get(appName);
        }
    }

    public TargetCacheImpl_DataApp putActiveApp(String appName) {
        synchronized( appsLock ) {
            TargetCacheImpl_DataApp appData = createAppData(appName);
            getActiveApps().put(appName, appData);
            return appData;
        }
    }

    public TargetCacheImpl_DataApp getActiveAppForcing(String appName) {
        synchronized( appsLock ) {
            Map<String, TargetCacheImpl_DataApp> useActiveApps = getActiveApps();

            TargetCacheImpl_DataApp activeApp = useActiveApps.get(appName);
            if ( activeApp == null ) {
                activeApp = createAppData(appName);
                useActiveApps.put(appName, activeApp);
            }

            return activeApp;
        }
    }

    //

//  if ( !loadedAppStore ) {
//      synchronized ( appStoreLock ) {
//          if ( !loadedAppStore ) {
//              loadAppStore(appStore);
//              loadedAppStore = true;
//          }
//      }
//  }

//  protected volatile boolean loadedAppStore;

//    protected void loadAppStore(final Map<String, TargetCacheImpl_DataApp> useAppStore) {
//        TargetCacheImpl_Utils.PrefixListWidget appListWidget =
//            new TargetCacheImpl_Utils.PrefixListWidget( getChildPrefixWidget() ) {
//                @Override
//                public void storeChild(String appName, String e_appName, File appDir) {
//                    TargetCacheImpl_DataApp appData = createAppData(appName, e_appName, appDir);
//                    useAppStore.put(appName, appData);
//                }
//        };
//
//        appListWidget.storeParent( getDataDir() );
//    }
}
