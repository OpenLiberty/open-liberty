/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package autonomicalpolling1serv.web;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;

import com.ibm.websphere.concurrent.persistent.AutoPurge;
import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskIdAccessor;

/**
 * A simple task that increments a counter each time it runs.
 */
public class IncTask implements Runnable, ManagedTask, Serializable {
    private static final long serialVersionUID = 1L;

    public static int counter;
    private static boolean initialized = false;
    private final Map<String, String> execProps = new TreeMap<String, String>();
    final String testIdentifier;
    public static int[] ranWithExec = new int[12];
    
    IncTask(String testIdentifier) {
        this.testIdentifier = testIdentifier;
        execProps.put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        execProps.put(ManagedTask.IDENTITY_NAME, "IncTask_" + testIdentifier);
    }
    
    public static void reset() {
      System.out.println("Resetting IncTask.");
      counter = 0;
      ranWithExec = new int[12];
      initialized = false;
    }

    @Override
    public void run() {
        ++counter;
        
        if (!initialized) {
        	if (counter < 3) {
        		return;
        	} else {
        		counter = 1;
        		initialized = true;
        	}
        }
        try {
        	if (counter < 13) {
        		System.out.println("IncTask " + TaskIdAccessor.get() + " from " + testIdentifier + " execution attempt #" + counter);
        		InitialContext context = new InitialContext();
        		PersistentExecutor persistentExec0 = (PersistentExecutor)context.lookup("persistent/exec0");
        		PersistentExecutor persistentExec1 = (PersistentExecutor)context.lookup("persistent/exec1");
        		PersistentExecutor persistentExec2 = (PersistentExecutor)context.lookup("persistent/exec2");

        		Field f = persistentExec0.getClass().getDeclaredField("inMemoryTaskIds");
        		f.setAccessible(true);
        		ConcurrentHashMap<Long, Boolean> inMemoryTaskIds0 = (ConcurrentHashMap<Long, Boolean>)f.get(persistentExec0);

        		f = persistentExec1.getClass().getDeclaredField("inMemoryTaskIds");
        		f.setAccessible(true);
        		ConcurrentHashMap<Long, Boolean> inMemoryTaskIds1 = (ConcurrentHashMap<Long, Boolean>)f.get(persistentExec1);

        		f = persistentExec2.getClass().getDeclaredField("inMemoryTaskIds");
        		f.setAccessible(true);
        		ConcurrentHashMap<Long, Boolean> inMemoryTaskIds2 = (ConcurrentHashMap<Long, Boolean>)f.get(persistentExec2);

        		if(inMemoryTaskIds0.size() == 1 && inMemoryTaskIds1.size() == 0 && inMemoryTaskIds2.size() == 0) {
        			System.out.println("IncTask " + TaskIdAccessor.get() + " from " + testIdentifier + " executed using persistent/exec0");
        			ranWithExec[counter-1] = 1;
        		} else if(inMemoryTaskIds0.size() == 0 && inMemoryTaskIds1.size() == 1 && inMemoryTaskIds2.size() == 0) {
        			System.out.println("IncTask " + TaskIdAccessor.get() + " from " + testIdentifier + " executed using persistent/exec1");
        			ranWithExec[counter-1] = 2;
        		} else if(inMemoryTaskIds0.size() == 0 && inMemoryTaskIds1.size() == 0 && inMemoryTaskIds2.size() == 1) {
        			System.out.println("IncTask " + TaskIdAccessor.get() + " from " + testIdentifier + " executed using persistent/exec2");
        			ranWithExec[counter-1] = 3;
        		} else {
        			System.out.println("IncTask " + TaskIdAccessor.get() + " from " + testIdentifier + " inMemoryTaskIds0 = " + inMemoryTaskIds0 + "; inMemoryTaskIds1 = " + inMemoryTaskIds1 + "; inMemoryTaskIds2 = " + inMemoryTaskIds2);
        			ranWithExec[counter-1] = -1;
        		}
        	}
        } catch (NameNotFoundException ex) {
        	System.out.println("NameNotFoundException caught inside IncTask.run(). This can be ignored if the persistent executor being looked up is not created/enabled by the test: " + ex.getMessage());
        } catch (Exception ex) {
        	ex.printStackTrace();
        }
    }

    @Override
    public Map<String, String> getExecutionProperties() {
        return execProps;
    }

    @Override
    public ManagedTaskListener getManagedTaskListener() {
        return null;
    }
}
