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
import java.util.WeakHashMap;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.anno.targets.cache.TargetCache_ExternalConstants;

public class TargetCacheImpl_DataApp extends TargetCacheImpl_DataBase {

    public static final String CLASS_NAME = TargetCacheImpl_DataApp.class.getSimpleName();

    //

    public TargetCacheImpl_DataApp(
        TargetCacheImpl_Factory factory,
        String appName, String e_appName, File appDir) {

        super(factory,
              appName, e_appName, appDir,
              TargetCache_ExternalConstants.MOD_PREFIX);

        this.activeModsLock = new ActiveModsLock();
        this.activeMods = new WeakHashMap<String, TargetCacheImpl_DataMod>();
    }

    //

    @Trivial
    protected TargetCacheImpl_DataMod createModData(String modName) {
        String e_modName = encode(modName);
        return createModData( modName, e_modName, e_getModDir(e_modName) );
    }

    @Trivial
    public File e_getModDir(String e_modName) {
        return getDataFile( e_addChildPrefix(e_modName) );
    }

    @Trivial
    protected TargetCacheImpl_DataMod createModData(File modDir) {
        String e_modDirName = modDir.getName();
        String e_modName = e_removeChildPrefix(e_modDirName);
        String modName = decode(e_modName);

        return createModData(modName, e_modName, modDir);
    }

    @Trivial
    protected TargetCacheImpl_DataMod createModData(String modName, String e_modName, File modDir) {
        return getFactory().createModData(modName, e_modName, modDir);
    }

    // Not in use: Module data is now loaded on request.

//  public void loadModStore(final Map<String, TargetCacheImpl_DataMod> useModStore) {
//      if ( isDisabled() ) {
//          return;
//      }
//
//      TargetCacheImpl_Utils.PrefixListWidget modListWidget =
//          new TargetCacheImpl_Utils.PrefixListWidget( getChildPrefixWidget() ) {
//              @Override
//              public void storeChild(String modName, String e_modName, File modDir) {
//                  TargetCacheImpl_DataMod modData = createModData(modName, e_modName, modDir);
//                  useModStore.put(modName, modData);
//              }
//      };
//
//      modListWidget.storeParent( getDataDir() );
//  }

    //

    private class ActiveModsLock {
        // EMPTY
    }
    private final ActiveModsLock activeModsLock;
    private final WeakHashMap<String, TargetCacheImpl_DataMod> activeMods;

    @Trivial
    public Map<String, TargetCacheImpl_DataMod> getActiveMods() {
        return activeMods;
    }

    public TargetCacheImpl_DataMod putActiveMod(String modName) {
        TargetCacheImpl_DataMod modData = createModData(modName);

        synchronized( activeModsLock ) {
            getActiveMods().put(modName, modData);
        }
        
        return modData;
    }

    //

    public TargetCacheImpl_DataMod getActiveModForcing(String modName) {
        synchronized( activeModsLock ) {
            Map<String, TargetCacheImpl_DataMod> useActiveMods = getActiveMods();

            TargetCacheImpl_DataMod activeMod = useActiveMods.get(modName);
            if ( activeMod == null ) {
                activeMod = createModData(modName);
                useActiveMods.put(modName, activeMod);
            }

            return activeMod;
        }
    }
}
