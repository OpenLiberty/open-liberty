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

import java.util.Enumeration;
import java.util.List;
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

    private static ObjectFactory jslFactory = new ObjectFactory();

    public static Batchlet cloneBatchlet(Batchlet batchlet) {
        Batchlet newBatchlet = jslFactory.createBatchlet();

        newBatchlet.setRef(batchlet.getRef());
        newBatchlet.setProperties(cloneJSLProperties(batchlet.getProperties()));

        return newBatchlet;
    }

    public static JSLProperties cloneJSLProperties(JSLProperties jslProps) {
        if (jslProps == null) {
            return null;
        }

        JSLProperties newJSLProps = jslFactory.createJSLProperties();

        newJSLProps.setPartition(jslProps.getPartition());;

        for (Property jslProp : jslProps.getPropertyList()) {
            Property newProperty = jslFactory.createProperty();

            newProperty.setName(jslProp.getName());
            newProperty.setValue(jslProp.getValue());

            newJSLProps.getPropertyList().add(newProperty);
        }

        return newJSLProps;
    }

    public static void cloneControlElements(List<TransitionElement> controlElements, List<TransitionElement> newControlElements) {

        newControlElements.clear();

        for (TransitionElement controlElement : controlElements) {
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

        Listeners newListeners = jslFactory.createListeners();

        for (Listener listener : listeners.getListenerList()) {
            Listener newListener = jslFactory.createListener();
            newListeners.getListenerList().add(newListener);
            newListener.setRef(listener.getRef());
            newListener.setProperties(cloneJSLProperties(listener.getProperties()));
        }

        return newListeners;
    }

    public static Chunk cloneChunk(Chunk chunk) {
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

        CheckpointAlgorithm newCheckpointAlgorithm = jslFactory.createCheckpointAlgorithm();
        newCheckpointAlgorithm.setRef(checkpointAlgorithm.getRef());
        newCheckpointAlgorithm.setProperties(cloneJSLProperties(checkpointAlgorithm.getProperties()));

        return newCheckpointAlgorithm;

    }

    private static ItemProcessor cloneItemProcessor(ItemProcessor itemProcessor) {
        if (itemProcessor == null) {
            return null;
        }

        ItemProcessor newItemProcessor = jslFactory.createItemProcessor();
        newItemProcessor.setRef(itemProcessor.getRef());
        newItemProcessor.setProperties(cloneJSLProperties(itemProcessor.getProperties()));

        return newItemProcessor;
    }

    private static ItemReader cloneItemReader(ItemReader itemReader) {
        if (itemReader == null) {
            return null;
        }

        ItemReader newItemReader = jslFactory.createItemReader();
        newItemReader.setRef(itemReader.getRef());
        newItemReader.setProperties(cloneJSLProperties(itemReader.getProperties()));

        return newItemReader;
    }

    private static ItemWriter cloneItemWriter(ItemWriter itemWriter) {
        ItemWriter newItemWriter = jslFactory.createItemWriter();
        newItemWriter.setRef(itemWriter.getRef());
        newItemWriter.setProperties(cloneJSLProperties(itemWriter.getProperties()));

        return newItemWriter;
    }

    private static ExceptionClassFilter cloneExceptionClassFilter(ExceptionClassFilter exceptionClassFilter) {

        if (exceptionClassFilter == null) {
            return null;
        }

        ExceptionClassFilter newExceptionClassFilter = jslFactory.createExceptionClassFilter();

        for (ExceptionClassFilter.Include oldInclude : exceptionClassFilter.getIncludeList()) {
            newExceptionClassFilter.getIncludeList().add(cloneExceptionClassFilterInclude(oldInclude));
        }

        for (ExceptionClassFilter.Exclude oldExclude : exceptionClassFilter.getExcludeList()) {
            newExceptionClassFilter.getExcludeList().add(cloneExceptionClassFilterExclude(oldExclude));
        }

        return newExceptionClassFilter;

    }

    private static ExceptionClassFilter.Include cloneExceptionClassFilterInclude(ExceptionClassFilter.Include include) {
        if (include == null) {
            return null;
        }

        ExceptionClassFilter.Include newInclude = jslFactory.createExceptionClassFilterInclude();

        newInclude.setClazz(include.getClazz());

        return newInclude;

    }

    private static ExceptionClassFilter.Exclude cloneExceptionClassFilterExclude(ExceptionClassFilter.Exclude exclude) {

        if (exclude == null) {
            return null;
        }

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
    public static JSLProperties javaPropsTojslProperties(final Properties javaProps) {

        JSLProperties newJSLProps = jslFactory.createJSLProperties();

        Enumeration<?> keySet = javaProps.propertyNames();

        while (keySet.hasMoreElements()) {
            String key = (String) keySet.nextElement();
            String value = javaProps.getProperty(key);

            Property newProperty = jslFactory.createProperty();
            newProperty.setName(key);
            newProperty.setValue(value);
            newJSLProps.getPropertyList().add(newProperty);
        }

        return newJSLProps;

    }

    /**
     * @param partition
     * @return
     */
    public static Partition cloneRelevantPartitionModel(Partition partition) {
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

        Collector newCollector = jslFactory.createCollector();
        newCollector.setRef(collector.getRef());
        newCollector.setProperties(cloneJSLProperties(collector.getProperties()));

        return newCollector;
    }

}
