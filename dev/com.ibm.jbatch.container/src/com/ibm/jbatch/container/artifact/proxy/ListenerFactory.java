/*
 * Copyright 2012 International Business Machines Corp.
 * 
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.jbatch.container.artifact.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.batch.api.chunk.listener.ChunkListener;
import javax.batch.api.chunk.listener.ItemProcessListener;
import javax.batch.api.chunk.listener.ItemReadListener;
import javax.batch.api.chunk.listener.ItemWriteListener;
import javax.batch.api.chunk.listener.RetryProcessListener;
import javax.batch.api.chunk.listener.RetryReadListener;
import javax.batch.api.chunk.listener.RetryWriteListener;
import javax.batch.api.chunk.listener.SkipProcessListener;
import javax.batch.api.chunk.listener.SkipReadListener;
import javax.batch.api.chunk.listener.SkipWriteListener;
import javax.batch.api.listener.JobListener;
import javax.batch.api.listener.StepListener;

import com.ibm.jbatch.container.execution.impl.RuntimeStepExecution;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.jsl.model.Listener;
import com.ibm.jbatch.jsl.model.Listeners;
import com.ibm.jbatch.jsl.model.Property;
import com.ibm.jbatch.jsl.model.Step;


public class ListenerFactory {

    private List<ListenerInfo> jobLevelListenerInfo = null;

    private Map<String, List<ListenerInfo>> stepLevelListenerInfo = new HashMap<String, List<ListenerInfo>>();

    /*
     * Build job-level ListenerInfo(s) up-front, but build step-level ones
     * lazily.
     */
    public ListenerFactory(JSLJob jobModel, InjectionReferences injectionRefs) {
        initJobLevelListeners(jobModel, injectionRefs);
    }

    private void initJobLevelListeners(JSLJob jobModel, InjectionReferences injectionRefs) {
        jobLevelListenerInfo = new ArrayList<ListenerInfo>();

        Listeners jobLevelListeners = jobModel.getListeners();

        if (jobLevelListeners != null) {
            for (Listener listener : jobLevelListeners.getListenerList()) {
                ListenerInfo info = buildListenerInfo(listener, injectionRefs);
                jobLevelListenerInfo.add(info);
            }
        }
    }

    /*
     * Does NOT throw an exception if a step-level listener is annotated with
     * 
     * @JobListener, even if that is the only type of listener annotation found.
     */
    private synchronized List<ListenerInfo> getStepListenerInfo(Step step, InjectionReferences injectionRefs) {
        if (!stepLevelListenerInfo.containsKey(step.getId())) {
            List<ListenerInfo> stepListenerInfoList = new ArrayList<ListenerInfo>();
            stepLevelListenerInfo.put(step.getId(), stepListenerInfoList);

            Listeners stepLevelListeners = step.getListeners();
            if (stepLevelListeners != null) {
                for (Listener listener : stepLevelListeners.getListenerList()) {
                    ListenerInfo info = buildListenerInfo(listener, injectionRefs);
                    stepListenerInfoList.add(info);
                }
            }

            return stepListenerInfoList;
        } else {
            return stepLevelListenerInfo.get(step.getId());
        }
    }

    private ListenerInfo buildListenerInfo(Listener listener, InjectionReferences injectionRefs) {

        String id = listener.getRef();

        List<Property> propList = (listener.getProperties() == null) ? null : (List<Property>) listener.getProperties().getPropertyList();

        injectionRefs.setProps(propList);
        Object listenerArtifact = ProxyFactory.loadArtifact(id, injectionRefs);
        if (listenerArtifact == null) {
            throw new IllegalArgumentException("Load of artifact id: " + id + " returned <null>.");
        }
        ListenerInfo info = new ListenerInfo(listenerArtifact, propList);
        return info;

    }

    public List<JobListenerProxy> getJobListeners() {
        List<JobListenerProxy> retVal = new ArrayList<JobListenerProxy>();
        for (ListenerInfo li : jobLevelListenerInfo) {
            if (li.isJobListener()) {
                JobListenerProxy proxy = new JobListenerProxy((JobListener) li.getArtifact());
                retVal.add(proxy);
            }
        }
        return retVal;
    }

    public List<ChunkListenerProxy> getChunkListeners(Step step, InjectionReferences injectionRefs, RuntimeStepExecution stepContext) {

        List<ListenerInfo> stepListenerInfo = getStepListenerInfo(step, injectionRefs);

        List<ChunkListenerProxy> retVal = new ArrayList<ChunkListenerProxy>();

        for (ListenerInfo li : stepListenerInfo) {
            if (li.isChunkListener()) {
                ChunkListenerProxy proxy = new ChunkListenerProxy((ChunkListener) li.getArtifact());
                proxy.setStepContext(stepContext);
                retVal.add(proxy);
            }
        }

        return retVal;
    }

    public List<ItemProcessListenerProxy> getItemProcessListeners(Step step, InjectionReferences injectionRefs, RuntimeStepExecution stepContext) {

        List<ListenerInfo> stepListenerInfo = getStepListenerInfo(step, injectionRefs);

        List<ItemProcessListenerProxy> retVal = new ArrayList<ItemProcessListenerProxy>();

        for (ListenerInfo li : stepListenerInfo) {
            if (li.isItemProcessListener()) {
                ItemProcessListenerProxy proxy = new ItemProcessListenerProxy((ItemProcessListener) li.getArtifact());
                proxy.setStepContext(stepContext);
                retVal.add(proxy);
            }
        }

        return retVal;
    }

    public List<ItemReadListenerProxy> getItemReadListeners(Step step, InjectionReferences injectionRefs, RuntimeStepExecution stepContext) {

        List<ListenerInfo> stepListenerInfo = getStepListenerInfo(step, injectionRefs);

        List<ItemReadListenerProxy> retVal = new ArrayList<ItemReadListenerProxy>();

        for (ListenerInfo li : stepListenerInfo) {
            if (li.isItemReadListener()) {
                ItemReadListenerProxy proxy = new ItemReadListenerProxy((ItemReadListener) li.getArtifact());
                proxy.setStepContext(stepContext);
                retVal.add(proxy);
            }
        }

        return retVal;
    }

    public List<ItemWriteListenerProxy> getItemWriteListeners(Step step, InjectionReferences injectionRefs, RuntimeStepExecution stepContext) {
        List<ListenerInfo> stepListenerInfo = getStepListenerInfo(step, injectionRefs);

        List<ItemWriteListenerProxy> retVal = new ArrayList<ItemWriteListenerProxy>();

        for (ListenerInfo li : stepListenerInfo) {
            if (li.isItemWriteListener()) {
                ItemWriteListenerProxy proxy = new ItemWriteListenerProxy((ItemWriteListener) li.getArtifact());
                proxy.setStepContext(stepContext);
                retVal.add(proxy);
            }
        }

        return retVal;
    }

    public List<RetryProcessListenerProxy> getRetryProcessListeners(Step step, InjectionReferences injectionRefs, RuntimeStepExecution stepContext) {
        List<ListenerInfo> stepListenerInfo = getStepListenerInfo(step, injectionRefs);

        List<RetryProcessListenerProxy> retVal = new ArrayList<RetryProcessListenerProxy>();

        for (ListenerInfo li : stepListenerInfo) {
            if (li.isRetryProcessListener()) {
                RetryProcessListenerProxy proxy = new RetryProcessListenerProxy((RetryProcessListener) li.getArtifact());
                proxy.setStepContext(stepContext);
                retVal.add(proxy);
            }
        }

        return retVal;
    }
    
    public List<RetryReadListenerProxy> getRetryReadListeners(Step step, InjectionReferences injectionRefs, RuntimeStepExecution stepContext) {
        List<ListenerInfo> stepListenerInfo = getStepListenerInfo(step, injectionRefs);

        List<RetryReadListenerProxy> retVal = new ArrayList<RetryReadListenerProxy>();

        for (ListenerInfo li : stepListenerInfo) {
            if (li.isRetryReadListener()) {
                RetryReadListenerProxy proxy = new RetryReadListenerProxy((RetryReadListener) li.getArtifact());
                proxy.setStepContext(stepContext);
                retVal.add(proxy);
            }
        }

        return retVal;
    }
    
    public List<RetryWriteListenerProxy> getRetryWriteListeners(Step step, InjectionReferences injectionRefs, RuntimeStepExecution stepContext) {
        List<ListenerInfo> stepListenerInfo = getStepListenerInfo(step, injectionRefs);

        List<RetryWriteListenerProxy> retVal = new ArrayList<RetryWriteListenerProxy>();

        for (ListenerInfo li : stepListenerInfo) {
            if (li.isRetryWriteListener()) {
                RetryWriteListenerProxy proxy = new RetryWriteListenerProxy((RetryWriteListener) li.getArtifact());
                proxy.setStepContext(stepContext);
                retVal.add(proxy);
            }
        }

        return retVal;
    }
    
    public List<SkipProcessListenerProxy> getSkipProcessListeners(Step step, InjectionReferences injectionRefs, RuntimeStepExecution stepContext) {
        List<ListenerInfo> stepListenerInfo = getStepListenerInfo(step, injectionRefs);

        List<SkipProcessListenerProxy> retVal = new ArrayList<SkipProcessListenerProxy>();

        for (ListenerInfo li : stepListenerInfo) {
            if (li.isSkipProcessListener()) {
                SkipProcessListenerProxy proxy = new SkipProcessListenerProxy((SkipProcessListener) li.getArtifact());
                proxy.setStepContext(stepContext);
                retVal.add(proxy);
            }
        }

        return retVal;
    }

    public List<SkipReadListenerProxy> getSkipReadListeners(Step step, InjectionReferences injectionRefs, RuntimeStepExecution stepContext) {
        List<ListenerInfo> stepListenerInfo = getStepListenerInfo(step, injectionRefs);

        List<SkipReadListenerProxy> retVal = new ArrayList<SkipReadListenerProxy>();

        for (ListenerInfo li : stepListenerInfo) {
            if (li.isSkipReadListener()) {
                SkipReadListenerProxy proxy = new SkipReadListenerProxy((SkipReadListener) li.getArtifact());
                proxy.setStepContext(stepContext);
                retVal.add(proxy);
            }
        }

        return retVal;
    }
    
    public List<SkipWriteListenerProxy> getSkipWriteListeners(Step step, InjectionReferences injectionRefs, RuntimeStepExecution stepContext) {
        List<ListenerInfo> stepListenerInfo = getStepListenerInfo(step, injectionRefs);

        List<SkipWriteListenerProxy> retVal = new ArrayList<SkipWriteListenerProxy>();

        for (ListenerInfo li : stepListenerInfo) {
            if (li.isSkipWriteListener()) {
                SkipWriteListenerProxy proxy = new SkipWriteListenerProxy((SkipWriteListener) li.getArtifact());
                proxy.setStepContext(stepContext);
                retVal.add(proxy);
            }
        }

        return retVal;
    }

    public List<StepListenerProxy> getStepListeners(Step step, InjectionReferences injectionRefs, RuntimeStepExecution stepContext) {
        List<ListenerInfo> stepListenerInfo = getStepListenerInfo(step, injectionRefs);

        List<StepListenerProxy> retVal = new ArrayList<StepListenerProxy>();

        for (ListenerInfo li : stepListenerInfo) {
            if (li.isStepListener()) {
                StepListenerProxy proxy = new StepListenerProxy((StepListener) li.getArtifact());
                proxy.setStepContext(stepContext);
                retVal.add(proxy);
            }
        }

        return retVal;
    }

    private class ListenerInfo {
        Object listenerArtifact = null;
        Class listenerArtifactClass = null;
        List<Property> propList = null;

        Object getArtifact() {
            return listenerArtifact;
        }

        private ListenerInfo(Object listenerArtifact, List<Property> propList) {
            this.listenerArtifact = listenerArtifact;
            this.listenerArtifactClass = listenerArtifact.getClass();
            this.propList = propList;
        }

        boolean isJobListener() {

            return JobListener.class.isAssignableFrom(listenerArtifactClass);
        }

        boolean isStepListener() {
            return StepListener.class.isAssignableFrom(listenerArtifactClass);
        }

        boolean isChunkListener() {
            return ChunkListener.class.isAssignableFrom(listenerArtifactClass);
        }

        boolean isItemProcessListener() {
            return ItemProcessListener.class.isAssignableFrom(listenerArtifactClass);
        }

        boolean isItemReadListener() {
            return ItemReadListener.class.isAssignableFrom(listenerArtifactClass);
        }

        boolean isItemWriteListener() {
            return ItemWriteListener.class.isAssignableFrom(listenerArtifactClass);
        }

        boolean isRetryReadListener() {
            return RetryReadListener.class.isAssignableFrom(listenerArtifactClass);
        }
        
        boolean isRetryWriteListener() {
            return RetryWriteListener.class.isAssignableFrom(listenerArtifactClass);
        }
        
        boolean isRetryProcessListener() {
            return RetryProcessListener.class.isAssignableFrom(listenerArtifactClass);
        }

        boolean isSkipProcessListener() {
            return SkipProcessListener.class.isAssignableFrom(listenerArtifactClass);
        }
        
        boolean isSkipReadListener() {
            return SkipReadListener.class.isAssignableFrom(listenerArtifactClass);
        }
        
        boolean isSkipWriteListener() {
            return SkipWriteListener.class.isAssignableFrom(listenerArtifactClass);
        }

        List<Property> getPropList() {
            return propList;
        }

        void setPropList(List<Property> propList) {
            this.propList = propList;
        }
    }

}
