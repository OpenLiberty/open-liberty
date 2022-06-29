/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.context;

import static org.jboss.weld.context.conversation.ConversationIdGenerator.CONVERSATION_ID_GENERATOR_ATTRIBUTE_NAME;
import static org.jboss.weld.util.reflection.Reflections.cast;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.context.ConversationScoped;

import org.jboss.weld.Container;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.config.ConfigurationKey;
import org.jboss.weld.config.WeldConfiguration;
import org.jboss.weld.context.api.ContextualInstance;
import org.jboss.weld.context.beanstore.BoundBeanStore;
import org.jboss.weld.context.beanstore.ConversationNamingScheme;
import org.jboss.weld.context.beanstore.NamingScheme;
import org.jboss.weld.context.conversation.ConversationIdGenerator;
import org.jboss.weld.context.conversation.ConversationImpl;
import org.jboss.weld.event.FastEvent;
import org.jboss.weld.literal.DestroyedLiteral;
import org.jboss.weld.logging.ConversationLogger;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.serialization.BeanIdentifierIndex;
import org.jboss.weld.util.LazyValueHolder;


/**
 * The base of the conversation context, which can use a variety of storage
 * forms
 *
 * @author Pete Muir
 * @author Jozef Hartinger
 * @author George Sapountzis
 * @author Marko Luksa
 */
public abstract class AbstractConversationContext<R, S> extends AbstractBoundContext<R> implements ConversationContext {

    public static final String CONVERSATIONS_ATTRIBUTE_NAME = ConversationContext.class.getName() + ".conversations";
    public static final String DESTRUCTION_QUEUE_ATTRIBUTE_NAME = ConversationContext.class.getName() + ".destructionQueue";
    private static final String CURRENT_CONVERSATION_ATTRIBUTE_NAME = ConversationContext.class.getName() + ".currentConversation";

    private static final String PARAMETER_NAME = "cid";

    private final AtomicReference<String> parameterName;
    private final AtomicLong defaultTimeout;
    private final AtomicLong concurrentAccessTimeout;

    private final ThreadLocal<R> associated;

    private final BeanManagerImpl manager;

    private final BeanIdentifierIndex beanIdentifierIndex;
    private final LazyValueHolder<FastEvent<String>> conversationDestroyedEvent = new LazyValueHolder<FastEvent<String>>() {
        @Override
        protected FastEvent<String> computeValue() {
            return FastEvent.of(String.class, manager, manager.getGlobalLenientObserverNotifier(), DestroyedLiteral.CONVERSATION);
        }
    };

    public AbstractConversationContext(String contextId, ServiceRegistry services) {
        super(contextId, true);
        this.parameterName = new AtomicReference<String>(PARAMETER_NAME);
        WeldConfiguration configuration = services.get(WeldConfiguration.class);
        this.defaultTimeout = new AtomicLong(configuration.getLongProperty(ConfigurationKey.CONVERSATION_TIMEOUT));
        this.concurrentAccessTimeout = new AtomicLong(configuration.getLongProperty(ConfigurationKey.CONVERSATION_CONCURRENT_ACCESS_TIMEOUT));
        this.associated = new ThreadLocal<R>();
        this.manager = Container.instance(contextId).deploymentManager();
        this.beanIdentifierIndex = services.get(BeanIdentifierIndex.class);
    }

    @Override
    public String getParameterName() {
        return parameterName.get();
    }

    @Override
    public void setParameterName(String cid) {
        this.parameterName.set(cid);
    }

    @Override
    public void setConcurrentAccessTimeout(long timeout) {
        this.concurrentAccessTimeout.set(timeout);
    }

    @Override
    public long getConcurrentAccessTimeout() {
        return concurrentAccessTimeout.get();
    }

    @Override
    public void setDefaultTimeout(long timeout) {
        this.defaultTimeout.set(timeout);
    }

    @Override
    public long getDefaultTimeout() {
        return defaultTimeout.get();
    }

    @Override
    public boolean associate(R request) {
        this.associated.set(request);
        /*
         * We need to delay attaching the bean store until activate() is called so that we can attach the correct conversation id. We may need access to the
         * conversation id generator and conversation map are initialized lazily.
         */
        return true;
    }

    @Override
    public boolean dissociate(R request) {
        if (isAssociated()) {
            try {
                copyConversationIdGeneratorAndConversationsToSession();
                return true;
            } finally {
                this.associated.set(null);
                cleanup();
            }
        } else {
            return false;
        }
    }

    protected void copyConversationIdGeneratorAndConversationsToSession() {
        final R request = getRequest();
        if(request == null) {
            return;
        }
        // If necessary, store the conversation id generator and conversation map in the session
        Object conversationIdGenerator = getRequestAttribute(request, CONVERSATION_ID_GENERATOR_ATTRIBUTE_NAME);
        if(conversationIdGenerator != null && getSessionAttribute(request, CONVERSATION_ID_GENERATOR_ATTRIBUTE_NAME, false) == null) {
            setSessionAttribute(request, CONVERSATION_ID_GENERATOR_ATTRIBUTE_NAME, conversationIdGenerator, false);
        }
        Object conversationMap = getRequestAttribute(request, CONVERSATIONS_ATTRIBUTE_NAME);
        if(conversationMap != null && getSessionAttribute(request, CONVERSATIONS_ATTRIBUTE_NAME, false) == null) {
            setSessionAttribute(request, CONVERSATIONS_ATTRIBUTE_NAME, conversationMap, false);
        }
    }

    public void sessionCreated() {
        copyConversationIdGeneratorAndConversationsToSession();
    }


    protected void associateRequestWithNewConversation() {
        ManagedConversation conversation = new ConversationImpl(manager);
        lock(conversation);
        setRequestAttribute(getRequest(), CURRENT_CONVERSATION_ATTRIBUTE_NAME, conversation);

        // Set a temporary bean store, this will be attached at the end of the request if needed
        NamingScheme namingScheme = new ConversationNamingScheme(getNamingSchemePrefix(), "transient", beanIdentifierIndex);
        setBeanStore(createRequestBeanStore(namingScheme, getRequest()));
        setRequestAttribute(getRequest(), ConversationNamingScheme.PARAMETER_NAME, namingScheme);
    }

    protected void associateRequest(ManagedConversation conversation) {
        setRequestAttribute(getRequest(), CURRENT_CONVERSATION_ATTRIBUTE_NAME, conversation);

        NamingScheme namingScheme = new ConversationNamingScheme(getNamingSchemePrefix(), conversation.getId(), beanIdentifierIndex);
        setBeanStore(createRequestBeanStore(namingScheme, getRequest()));
        getBeanStore().attach();
    }

    @Override
    public void activate() {
        this.activate(null);
    }

    @Override
    public void activate(String cid) {
        if (!isAssociated()) {
            throw ConversationLogger.LOG.mustCallAssociateBeforeActivate();
        }
        if (!isActive()) {
            super.setActive(true);
        } else {
            ConversationLogger.LOG.contextAlreadyActive(getRequest());
        }
        initialize(cid);
    }

    protected void initialize(String cid) {
        // Attach the conversation
        // WELD-1315 Don't try to restore the long-running conversation if cid param is empty
        if (cid != null && !cid.isEmpty()) {
            ManagedConversation conversation = getConversation(cid);
            if (conversation != null && !isExpired(conversation)) {
                boolean lock = lock(conversation);
                if (lock) {
                    // WELD-1690 Don't associate a conversation which was ended (race condition)
                    if(conversation.isTransient()) {
                        associateRequestWithNewConversation();
                        throw ConversationLogger.LOG.noConversationFoundToRestore(cid);
                    }
                    associateRequest(conversation);
                } else {
                    // CDI 6.7.4 we must activate a new transient conversation before we throw the exception
                    associateRequestWithNewConversation();
                    throw ConversationLogger.LOG.conversationLockTimedout(cid);
                }
            } else {
                // CDI 6.7.4 we must activate a new transient conversation before we throw the exception
                associateRequestWithNewConversation();
                throw ConversationLogger.LOG.noConversationFoundToRestore(cid);
            }
        } else {
            associateRequestWithNewConversation();
        }
    }

    private boolean lock(ManagedConversation conversation) {
        return conversation.lock(getConcurrentAccessTimeout());
    }

    @Override
    public void deactivate() {
        // Disassociate from the current conversation
        if (isActive()) {
            if (!isAssociated()) {
                throw ConversationLogger.LOG.mustCallAssociateBeforeDeactivate();
            }

            try {
                if (getCurrentConversation().isTransient() && getRequestAttribute(getRequest(), ConversationNamingScheme.PARAMETER_NAME) != null) {
                    // WELD-1746 Don't destroy ended conversations - these must be destroyed in a synchronized block - see also cleanUpConversationMap()
                    destroy();
                } else {
                    // Update the conversation timestamp
                    getCurrentConversation().touch();
                    if (!getBeanStore().isAttached()) {
                        /*
                         * This was a transient conversation at the beginning of the request, so we need to update the CID it uses, and attach it. We also add
                         * it to the conversations the session knows about.
                         */
                        if (!(getRequestAttribute(getRequest(), ConversationNamingScheme.PARAMETER_NAME) instanceof ConversationNamingScheme)) {
                            throw ConversationLogger.LOG.conversationNamingSchemeNotFound();
                        }
                        ((ConversationNamingScheme) getRequestAttribute(getRequest(), ConversationNamingScheme.PARAMETER_NAME)).setCid(getCurrentConversation()
                                .getId());

                        getBeanStore().attach();
                        getConversationMap().put(getCurrentConversation().getId(), getCurrentConversation());
                    }
                }
            } finally {
                // WELD-1690 always try to unlock the current conversation
                getCurrentConversation().unlock();
                // WELD-1802
                setBeanStore(null);
                // Clean up any expired/ended conversations
                cleanUpConversationMap();
                // Deactivate the context, i.e. remove state threadlocal
                removeState();
            }
        } else {
            throw ConversationLogger.LOG.contextNotActive();
        }
    }

    private void cleanUpConversationMap() {
        Map<String, ManagedConversation> conversations = getConversationMap();
        synchronized (conversations) {
            Iterator<Entry<String, ManagedConversation>> entryIterator = conversations.entrySet().iterator();
            S session = getSessionFromRequest(getRequest(), false);
            while (entryIterator.hasNext()) {
                Entry<String, ManagedConversation> entry = entryIterator.next();
                if (entry.getValue().isTransient()) {
                    destroyConversation(session, entry.getKey());
                    entryIterator.remove();
                }
            }
        }
    }

    public void conversationPromotedToLongRunning(ConversationImpl conversation) {
        getConversationMap().put(conversation.getId(), conversation);
    }

    @Override
    public void invalidate() {
        ManagedConversation currentConversation = getCurrentConversation();
        Map<String, ManagedConversation> conversations = getConversationMap();
        synchronized (conversations) {
            for (Entry<String, ManagedConversation> stringManagedConversationEntry : conversations.entrySet()) {
                ManagedConversation conversation = stringManagedConversationEntry.getValue();
                if (!currentConversation.equals(conversation) && !conversation.isTransient() && isExpired(conversation)) {
                    // Try to lock the conversation and log warning if not successful - unlocking should not be necessary
                    if (!conversation.lock(0)) {
                        ConversationLogger.LOG.endLockedConversation(conversation.getId());
                    }
                    conversation.end();
                }
            }
        }
    }

    public boolean destroy(S session) {
        // the context may be active
        // if it is, we need to re-attach the bean store once the other conversations are destroyed
        final BoundBeanStore beanStore = getBeanStore();
        final boolean active = isActive();
        if (beanStore != null) {
            beanStore.detach();
        }
        try {
            Object conversationMap = getSessionAttributeFromSession(session, CONVERSATIONS_ATTRIBUTE_NAME);
            if (conversationMap instanceof Map) {
                Map<String, ManagedConversation> conversations = cast(conversationMap);
                synchronized (conversations) {
                    if (!conversations.isEmpty()) {
                        // There are some conversations to destroy
                        setActive(true);
                        if (beanStore == null) {
                            // There is no request associated - destroy conversation contexts immediately
                            for (Entry<String, ManagedConversation> entry : conversations.entrySet()) {
                                destroyConversation(session, entry.getKey());
                            }
                        } else {
                            // All conversation contexts created during the current session should be destroyed after the servlet service() completes
                            // However, at that time the session will not be available - store all remaining contextual instances in the request
                            setDestructionQueue(conversations, session);
                        }
                    }
                }
            }
            return true;
        } finally {
            setBeanStore(beanStore);
            setActive(active);
            if (beanStore != null) {
                beanStore.attach();
            } else if (!active) {
                removeState();
                cleanup();
            }
        }
    }

    private void setDestructionQueue(Map<String, ManagedConversation> conversations, S session) {
        Map<String, List<ContextualInstance<?>>> contexts = new HashMap<>();
        for (Entry<String, ManagedConversation> entry : conversations.entrySet()) {
            ManagedConversation conversation = entry.getValue();
            // First make all conversations transient
            if (!conversation.isTransient()) {
                conversation.end();
            }
            // Extract contextual instances
            List<ContextualInstance<?>> contextualInstances = new ArrayList<>();
            for (String id : new ConversationNamingScheme(getNamingSchemePrefix(), entry.getKey(), beanIdentifierIndex)
                    .filterIds(getSessionAttributeNames(session))) {
                contextualInstances.add((ContextualInstance<?>) getSessionAttributeFromSession(session, id));
            }
            contexts.put(entry.getKey(), contextualInstances);
        }
        // Store remaining conversation contexts for later destruction
        setRequestAttribute(getRequest(), DESTRUCTION_QUEUE_ATTRIBUTE_NAME, Collections.synchronizedMap(contexts));
    }

    protected void destroyConversation(S session, String id) {
        if (session != null) {
            setBeanStore(createSessionBeanStore(new ConversationNamingScheme(getNamingSchemePrefix(), id, beanIdentifierIndex), session));
            getBeanStore().attach();
            destroy();
            getBeanStore().detach();
            setBeanStore(null);
            conversationDestroyedEvent.get().fire(id);
        }
    }

    @Override
    public String generateConversationId() {
        ConversationIdGenerator generator = getConversationIdGenerator();
        checkContextInitialized();
        return generator.call();
    }

    protected ConversationIdGenerator getConversationIdGenerator() {
        final R request = associated.get();
        if (request == null) {
            throw ConversationLogger.LOG.mustCallAssociateBeforeGeneratingId();
        }
        Object conversationIdGenerator = getRequestAttribute(request, CONVERSATION_ID_GENERATOR_ATTRIBUTE_NAME);
        if (conversationIdGenerator == null) {
            conversationIdGenerator = getSessionAttribute(request, CONVERSATION_ID_GENERATOR_ATTRIBUTE_NAME, false);
            if(conversationIdGenerator == null) {
                conversationIdGenerator = new ConversationIdGenerator();
                setRequestAttribute(request, CONVERSATION_ID_GENERATOR_ATTRIBUTE_NAME, conversationIdGenerator);
                setSessionAttribute(request, CONVERSATION_ID_GENERATOR_ATTRIBUTE_NAME, conversationIdGenerator, false);
            } else {
                setRequestAttribute(request, CONVERSATION_ID_GENERATOR_ATTRIBUTE_NAME, conversationIdGenerator);
            }
        }
        if (!(conversationIdGenerator instanceof ConversationIdGenerator)) {
            throw ConversationLogger.LOG.conversationIdGeneratorNotFound();
        }
        return (ConversationIdGenerator) conversationIdGenerator;
    }

    private static boolean isExpired(ManagedConversation conversation) {
        return System.currentTimeMillis() > (conversation.getLastUsed() + conversation.getTimeout());
    }

    @Override
    public ManagedConversation getConversation(String id) {
        return getConversationMap().get(id);
    }

    @Override
    public Collection<ManagedConversation> getConversations() {
        // Don't return the map view to avoid concurrency issues
        Map<String, ManagedConversation> conversations = getConversationMap();
        synchronized (conversations) {
            return new HashSet<ManagedConversation>(conversations.values());
        }
    }

    private void checkIsAssociated() {
        if (!isAssociated()) {
            throw ConversationLogger.LOG.mustCallAssociateBeforeLoadingKnownConversations();
        }
    }

    private Map<String, ManagedConversation> getConversationMap() {
        checkIsAssociated();
        checkContextInitialized();
        final R request = getRequest();
        Object conversationMap = getRequestAttribute(request, CONVERSATIONS_ATTRIBUTE_NAME);
        if(conversationMap == null) {
            conversationMap = getSessionAttribute(request, CONVERSATIONS_ATTRIBUTE_NAME, false);
            if (conversationMap == null) {
                conversationMap = Collections.synchronizedMap(new HashMap<String, ManagedConversation>());
                setRequestAttribute(request, CONVERSATIONS_ATTRIBUTE_NAME, conversationMap);
                setSessionAttribute(request, CONVERSATIONS_ATTRIBUTE_NAME, conversationMap, false);
            } else {
                setRequestAttribute(request, CONVERSATIONS_ATTRIBUTE_NAME, conversationMap);
            }
        }
        if (conversationMap == null || !(conversationMap instanceof Map)) {
            throw ConversationLogger.LOG.unableToLoadConversations(CONVERSATIONS_ATTRIBUTE_NAME, conversationMap, request);
        }
        return cast(conversationMap);
    }

    @Override
    public ManagedConversation getCurrentConversation() {
        checkIsAssociated();
        checkContextInitialized();
        R request = getRequest();
        Object attribute = getRequestAttribute(request, CURRENT_CONVERSATION_ATTRIBUTE_NAME);
        if (attribute == null || !(attribute instanceof ManagedConversation)) {
            throw ConversationLogger.LOG.unableToLoadCurrentConversation(CURRENT_CONVERSATION_ATTRIBUTE_NAME, attribute, request);
        }
        return (ManagedConversation) attribute;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return ConversationScoped.class;
    }

    /**
     * Set an attribute in the session.
     *
     * @param request the request to set the session attribute in
     * @param name    the name of the attribute
     * @param value   the value of the attribute
     * @param create  if false, the attribute will only be set if the session
     *                already exists, otherwise it will always be set
     * @throws IllegalStateException if create is true, and the session can't be
     *                               created
     */
    protected abstract void setSessionAttribute(R request, String name, Object value, boolean create);

    /**
     * Get an attribute value from the session.
     *
     * @param request the request to get the session attribute from
     * @param name    the name of the attribute
     * @param create  if false, the attribute will only be retrieved if the
     *                session already exists, other wise it will always be retrieved
     * @return attribute
     * @throws IllegalStateException if create is true, and the session can't be
     *                               created
     */
    protected abstract Object getSessionAttribute(R request, String name, boolean create);

    /**
     * Get an attribute value from the session.
     *
     * @param session the session to get the session attribute from
     * @param name    the name of the attribute
     * @return attribute
     * @throws IllegalStateException if create is true, and the session can't be
     *                               created
     */
    protected abstract Object getSessionAttributeFromSession(S session, String name);

    /**
     * Remove an attribute from the request.
     *
     * @param request the request to remove the attribute from
     * @param name    the name of the attribute
     */
    protected abstract void removeRequestAttribute(R request, String name);

    /**
     * Set an attribute in the request.
     *
     * @param request the request to set the attribute from
     * @param name    the name of the attribute
     * @param value   the value of the attribute
     */
    protected abstract void setRequestAttribute(R request, String name, Object value);

    /**
     * Retrieve an attribute value from the request
     *
     * @param request the request to get the attribute from
     * @param name    the name of the attribute to get
     * @return the value of the attribute
     */
    protected abstract Object getRequestAttribute(R request, String name);

    protected abstract BoundBeanStore createRequestBeanStore(NamingScheme namingScheme, R request);

    protected abstract BoundBeanStore createSessionBeanStore(NamingScheme namingScheme, S session);

    protected abstract S getSessionFromRequest(R request, boolean create);

    protected abstract String getNamingSchemePrefix();

    /**
     * Check if the context is currently associated
     *
     * @return true if the context is associated
     */
    protected boolean isAssociated() {
        return associated.get() != null;
    }

    /**
     * Get the associated store
     *
     * @return the request
     */
    protected R getRequest() {
        return associated.get();
    }

    protected abstract Iterator<String> getSessionAttributeNames(S session);

}
