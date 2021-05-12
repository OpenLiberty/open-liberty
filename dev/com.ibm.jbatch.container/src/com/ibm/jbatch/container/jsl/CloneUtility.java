/**
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
package com.ibm.jbatch.container.jsl;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.ibm.jbatch.jsl.model.Batchlet;
import com.ibm.jbatch.jsl.model.CheckpointAlgorithm;
import com.ibm.jbatch.jsl.model.Chunk;
import com.ibm.jbatch.jsl.model.Collector;
import com.ibm.jbatch.jsl.model.End;
import com.ibm.jbatch.jsl.model.ExceptionClassFilter;
import com.ibm.jbatch.jsl.model.Fail;
import com.ibm.jbatch.jsl.model.ItemProcessor;
import com.ibm.jbatch.jsl.model.ItemReader;
import com.ibm.jbatch.jsl.model.ItemWriter;
import com.ibm.jbatch.jsl.model.JSLProperties;
import com.ibm.jbatch.jsl.model.Listener;
import com.ibm.jbatch.jsl.model.Listeners;
import com.ibm.jbatch.jsl.model.Next;
import com.ibm.jbatch.jsl.model.ObjectFactory;
import com.ibm.jbatch.jsl.model.Partition;
import com.ibm.jbatch.jsl.model.Property;
import com.ibm.jbatch.jsl.model.Stop;
import com.ibm.jbatch.jsl.model.helper.TransitionElement;

public class CloneUtility {

    private static final Map<String, ObjectFactory> objectFactoryMap;

    static {
        HashMap<String, ObjectFactory> map = new HashMap<>();
        map.put("com.ibm.jbatch.jsl.model.v1", new com.ibm.jbatch.jsl.model.v1.ObjectFactory());
        map.put("com.ibm.jbatch.jsl.model.v2", new com.ibm.jbatch.jsl.model.v2.ObjectFactory());
        objectFactoryMap = Collections.unmodifiableMap(map);
    }

    public static Batchlet cloneBatchlet(Batchlet batchlet) {
        ObjectFactory jslFactory = objectFactoryMap.get(batchlet.getClass().getPackage().getName());
        Batchlet newBatchlet = jslFactory.createBatchlet();

        newBatchlet.setRef(batchlet.getRef());
        newBatchlet.setProperties(cloneJSLProperties(batchlet.getProperties()));

        return newBatchlet;
    }

    public static JSLProperties cloneJSLProperties(JSLProperties jslProps) {
        if (jslProps == null) {
            return null;
        }

        ObjectFactory jslFactory = objectFactoryMap.get(jslProps.getClass().getPackage().getName());
        JSLProperties newJSLProps = jslFactory.createJSLProperties();

        newJSLProps.setPartition(jslProps.getPartition());;

        List<Property> newPropertyList = (List<Property>) newJSLProps.getPropertyList();
        for (Property jslProp : jslProps.getPropertyList()) {
            Property newProperty = jslFactory.createProperty();

            newProperty.setName(jslProp.getName());
            newProperty.setValue(jslProp.getValue());

            newPropertyList.add(newProperty);
        }

        return newJSLProps;
    }

    public static void cloneControlElements(List<TransitionElement> controlElements, List<TransitionElement> newControlElements) {

        newControlElements.clear();

        for (TransitionElement controlElement : controlElements) {
            ObjectFactory jslFactory = objectFactoryMap.get(controlElement.getClass().getPackage().getName());
            if (controlElement instanceof End) {
                End endElement = (End) controlElement;
                End newEnd = jslFactory.createEnd();
                newEnd.setExitStatus(endElement.getExitStatus());
                newEnd.setOn(endElement.getOn());

                newControlElements.add(newEnd);
            } else if (controlElement instanceof Fail) {
                Fail failElement = (Fail) controlElement;
                Fail newFail = jslFactory.createFail();
                newFail.setExitStatus(failElement.getExitStatus());
                newFail.setOn(failElement.getOn());

                newControlElements.add(newFail);
            } else if (controlElement instanceof Next) {
                Next nextElement = (Next) controlElement;
                Next newNext = jslFactory.createNext();
                newNext.setOn(nextElement.getOn());
                newNext.setTo(nextElement.getTo());

                newControlElements.add(newNext);
            }

            else if (controlElement instanceof Stop) {
                Stop stopElement = (Stop) controlElement;
                Stop newStop = jslFactory.createStop();
                newStop.setExitStatus(stopElement.getExitStatus());
                newStop.setOn(stopElement.getOn());
                newStop.setRestart(stopElement.getRestart());

                newControlElements.add(newStop);
            }
        }

    }

    public static Listeners cloneListeners(Listeners listeners) {
        if (listeners == null) {
            return null;
        }

        ObjectFactory jslFactory = objectFactoryMap.get(listeners.getClass().getPackage().getName());
        Listeners newListeners = jslFactory.createListeners();

        List<Listener> newListenerList = (List<Listener>) newListeners.getListenerList();

        for (Listener listener : listeners.getListenerList()) {
            Listener newListener = jslFactory.createListener();
            newListenerList.add(newListener);
            newListener.setRef(listener.getRef());
            newListener.setProperties(cloneJSLProperties(listener.getProperties()));
        }

        return newListeners;
    }

    public static Chunk cloneChunk(Chunk chunk) {
        ObjectFactory jslFactory = objectFactoryMap.get(chunk.getClass().getPackage().getName());
        Chunk newChunk = jslFactory.createChunk();

        newChunk.setItemCount(chunk.getItemCount());
        newChunk.setRetryLimit(chunk.getRetryLimit());
        newChunk.setSkipLimit(chunk.getSkipLimit());
        newChunk.setTimeLimit(chunk.getTimeLimit());
        newChunk.setCheckpointPolicy(chunk.getCheckpointPolicy());

        newChunk.setCheckpointAlgorithm(cloneCheckpointAlorithm(chunk.getCheckpointAlgorithm()));
        newChunk.setProcessor(cloneItemProcessor(chunk.getProcessor()));
        newChunk.setReader(cloneItemReader(chunk.getReader()));
        newChunk.setWriter(cloneItemWriter(chunk.getWriter()));
        newChunk.setNoRollbackExceptionClasses(cloneExceptionClassFilter(chunk.getNoRollbackExceptionClasses()));
        newChunk.setRetryableExceptionClasses(cloneExceptionClassFilter(chunk.getRetryableExceptionClasses()));
        newChunk.setSkippableExceptionClasses(cloneExceptionClassFilter(chunk.getSkippableExceptionClasses()));

        return newChunk;
    }

    private static CheckpointAlgorithm cloneCheckpointAlorithm(CheckpointAlgorithm checkpointAlgorithm) {
        if (checkpointAlgorithm == null) {
            return null;
        }

        ObjectFactory jslFactory = objectFactoryMap.get(checkpointAlgorithm.getClass().getPackage().getName());
        CheckpointAlgorithm newCheckpointAlgorithm = jslFactory.createCheckpointAlgorithm();
        newCheckpointAlgorithm.setRef(checkpointAlgorithm.getRef());
        newCheckpointAlgorithm.setProperties(cloneJSLProperties(checkpointAlgorithm.getProperties()));

        return newCheckpointAlgorithm;

    }

    private static ItemProcessor cloneItemProcessor(ItemProcessor itemProcessor) {
        if (itemProcessor == null) {
            return null;
        }

        ObjectFactory jslFactory = objectFactoryMap.get(itemProcessor.getClass().getPackage().getName());
        ItemProcessor newItemProcessor = jslFactory.createItemProcessor();
        newItemProcessor.setRef(itemProcessor.getRef());
        newItemProcessor.setProperties(cloneJSLProperties(itemProcessor.getProperties()));

        return newItemProcessor;
    }

    private static ItemReader cloneItemReader(ItemReader itemReader) {
        if (itemReader == null) {
            return null;
        }

        ObjectFactory jslFactory = objectFactoryMap.get(itemReader.getClass().getPackage().getName());
        ItemReader newItemReader = jslFactory.createItemReader();
        newItemReader.setRef(itemReader.getRef());
        newItemReader.setProperties(cloneJSLProperties(itemReader.getProperties()));

        return newItemReader;
    }

    private static ItemWriter cloneItemWriter(ItemWriter itemWriter) {
        ObjectFactory jslFactory = objectFactoryMap.get(itemWriter.getClass().getPackage().getName());
        ItemWriter newItemWriter = jslFactory.createItemWriter();
        newItemWriter.setRef(itemWriter.getRef());
        newItemWriter.setProperties(cloneJSLProperties(itemWriter.getProperties()));

        return newItemWriter;
    }

    private static ExceptionClassFilter cloneExceptionClassFilter(ExceptionClassFilter exceptionClassFilter) {

        if (exceptionClassFilter == null) {
            return null;
        }

        ObjectFactory jslFactory = objectFactoryMap.get(exceptionClassFilter.getClass().getPackage().getName());
        ExceptionClassFilter newExceptionClassFilter = jslFactory.createExceptionClassFilter();
        List<ExceptionClassFilter.Include> newIncludeList = (List<ExceptionClassFilter.Include>) newExceptionClassFilter.getIncludeList();
        List<ExceptionClassFilter.Exclude> newExcludeList = (List<ExceptionClassFilter.Exclude>) newExceptionClassFilter.getExcludeList();

        for (ExceptionClassFilter.Include oldInclude : exceptionClassFilter.getIncludeList()) {
            newIncludeList.add(cloneExceptionClassFilterInclude(oldInclude));
        }

        for (ExceptionClassFilter.Exclude oldExclude : exceptionClassFilter.getExcludeList()) {
            newExcludeList.add(cloneExceptionClassFilterExclude(oldExclude));
        }

        return newExceptionClassFilter;

    }

    private static ExceptionClassFilter.Include cloneExceptionClassFilterInclude(ExceptionClassFilter.Include include) {
        if (include == null) {
            return null;
        }

        ObjectFactory jslFactory = objectFactoryMap.get(include.getClass().getPackage().getName());
        ExceptionClassFilter.Include newInclude = jslFactory.createExceptionClassFilterInclude();

        newInclude.setClazz(include.getClazz());

        return newInclude;

    }

    private static ExceptionClassFilter.Exclude cloneExceptionClassFilterExclude(ExceptionClassFilter.Exclude exclude) {

        if (exclude == null) {
            return null;
        }

        ObjectFactory jslFactory = objectFactoryMap.get(exclude.getClass().getPackage().getName());
        ExceptionClassFilter.Exclude newExclude = jslFactory.createExceptionClassFilterExclude();

        newExclude.setClazz(exclude.getClazz());

        return newExclude;

    }

    /**
     * Creates a java.util.Properties map from a com.ibm.jbatch.jsl.model.Properties
     * object.
     *
     * @param xmlProperties
     * @return
     */
    public static Properties jslPropertiesToJavaProperties(
                                                           final JSLProperties xmlProperties) {

        final Properties props = new Properties();

        for (final Property prop : xmlProperties.getPropertyList()) {
            props.setProperty(prop.getName(), prop.getValue());
        }

        return props;

    }

    /**
     * Creates a new JSLProperties list from a java.util.Properties
     * object.
     *
     * @param xmlProperties
     * @return
     */
    public static JSLProperties javaPropsTojslProperties(final ObjectFactory jslFactory, final Properties javaProps) {

        JSLProperties newJSLProps = jslFactory.createJSLProperties();

        Enumeration<?> keySet = javaProps.propertyNames();

        List<Property> newPropertyList = (List<Property>) newJSLProps.getPropertyList();
        while (keySet.hasMoreElements()) {
            String key = (String) keySet.nextElement();
            String value = javaProps.getProperty(key);

            Property newProperty = jslFactory.createProperty();
            newProperty.setName(key);
            newProperty.setValue(value);
            newPropertyList.add(newProperty);
        }

        return newJSLProps;

    }

    /**
     * @param partition
     * @return
     */
    public static Partition cloneRelevantPartitionModel(Partition partition) {
        ObjectFactory jslFactory = objectFactoryMap.get(partition.getClass().getPackage().getName());
        Partition newPartition = jslFactory.createPartition();
        if (partition.getCollector() != null) {
            // PartitionPlan partitionPlan = jslFactory.createPartitionPlan();
            //partitionPlan.setPartitions(null);
            //newPartition.setPlan(partitionPlan);

            newPartition.setCollector(cloneCollector(partition.getCollector()));
        }
        return newPartition;
    }

    /**
     * @param collector
     * @return
     */
    private static Collector cloneCollector(Collector collector) {

        if (collector == null) {
            return null;
        }

        ObjectFactory jslFactory = objectFactoryMap.get(collector.getClass().getPackage().getName());
        Collector newCollector = jslFactory.createCollector();
        newCollector.setRef(collector.getRef());
        newCollector.setProperties(cloneJSLProperties(collector.getProperties()));

        return newCollector;
    }

}
