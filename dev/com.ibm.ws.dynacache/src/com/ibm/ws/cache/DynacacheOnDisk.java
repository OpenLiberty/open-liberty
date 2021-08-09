/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

public interface DynacacheOnDisk {
	
   public void clearDiskCache();
   public void delCacheEntry(CacheEntry ce, int cause, int source, boolean fromDepIdTemplateInvalidation);
   public void delCacheEntry(ValueSet removeList, int cause, int source, boolean fromDepIdTemplateInvalidation, boolean fireEvent);
   public void delDependency(Object id); 
   public void delTemplate(String template);
   public void delDependencyEntry(Object id, Object entry); 
   public void delTemplateEntry(String template, Object entry);
   public CacheEntry readCacheEntry(Object id); 
   public CacheEntry readCacheEntry(Object id, boolean calledFromRemove);
   public ValueSet readDependency(Object id, boolean delete);   
   public ValueSet readTemplate(String template, boolean delete);
   public ValueSet readCacheIdsByRange(int index, int length);
   public ValueSet readDependencyByRange(int index, int length);
   public ValueSet readTemplatesByRange(int index, int length);
   public int writeCacheEntry(CacheEntry ce);  
   public int writeDependency(Object id, ValueSet vs); 
   public int writeDependencyEntry(Object id, Object entry);   
   public int writeTemplate(String template, ValueSet vs);
   public int writeTemplateEntry(String template, Object entry);
   public void close(boolean deleteInProgressFile);
   public void stop(boolean completeClear);
   public void stopOnError(Exception exception);
   public int writeAuxiliaryDepTables();
   public int getCacheIdsSize(boolean filter);
   public int getDepIdsSize();
   public int getTemplatesSize();
   public void deleteDiskCacheFiles();
   public boolean isCleanupRunning();
   public boolean containsKey(Object id);
   public void invokeDiskCleanup(boolean scan);
   public long getCacheSizeInBytes();
   public int getPendingRemovalSize();
   public int getDepIdsBufferedSize();
   public int getTemplatesBufferedSize();
   public int getStartState();
   public void clearInvalidationBuffers();
   public boolean isInvalidationBuffersFull();
   public boolean shouldPopulateEvictionTable(); 
   public int getDiskCacheSizeLimit();
   public int getDiskCacheSizeHighLimit();
   public int getDiskCacheSizeInGBLimit();
   public long getDiskCacheSizeInBytesLimit(); 
   public long getDiskCacheSizeInBytesHighLimit();
   public long getDiskCacheEntrySizeInBytesLimit();
   public boolean invokeDiskCacheGarbageCollector(int GCType); 
   public int getEvictionPolicy();    
   public void releaseUnusedPools();
   public Result readHashcodeByRange(int index, int length, boolean debug, boolean useValue);  
   public int updateExpirationTime(Object id, long oldExpirationTime, int size, long newExpirationTime, long newValidatorExpirationTime);
   public Exception getDiskCacheException();
   public void waitForCleanupComplete();
   public boolean isCacheIdInAuxDepIdTable(Object id);
}
