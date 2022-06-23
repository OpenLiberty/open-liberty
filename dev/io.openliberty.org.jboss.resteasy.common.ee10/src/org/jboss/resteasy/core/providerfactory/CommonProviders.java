package org.jboss.resteasy.core.providerfactory;

import org.jboss.resteasy.core.MediaTypeMap;
import org.jboss.resteasy.core.interception.jaxrs.ReaderInterceptorRegistryImpl;
import org.jboss.resteasy.core.interception.jaxrs.WriterInterceptorRegistryImpl;
import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;
import org.jboss.resteasy.spi.interception.JaxrsInterceptorRegistry;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.WriterInterceptor;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class CommonProviders {
   private static final TraceComponent tc = Tr.register(CommonProviders.class); //Liberty change
   protected ResteasyProviderFactoryImpl rpf;
   protected boolean lockSnapshots;
   protected boolean attachedMessageBodyReaders;
   protected volatile MediaTypeMap<SortedKey<MessageBodyReader>> messageBodyReaders;
   protected boolean attachedMessageBodyWriters;
   protected volatile MediaTypeMap<SortedKey<MessageBodyWriter>> messageBodyWriters;
   protected boolean attachedReaderInterceptors;
   protected volatile JaxrsInterceptorRegistry<ReaderInterceptor> readerInterceptorRegistry;
   protected boolean attachedWriterInterceptors;
   protected volatile JaxrsInterceptorRegistry<WriterInterceptor> writerInterceptorRegistry;
   protected boolean attachedFeatures;
   protected volatile Set<DynamicFeature> dynamicFeatures;

   CommonProviders() {
   }

   public CommonProviders(final ResteasyProviderFactoryImpl rpf)
   {
      this.rpf = rpf;
   }

   /**
    * Shallow Copy of a parent.
    *
    * @param rpf
    * @param parent
    */
   public CommonProviders(final ResteasyProviderFactoryImpl rpf, final CommonProviders parent)
   {
      this(rpf);
      this.lockSnapshots = true;
      if (parent.messageBodyReaders != null) {
         this.messageBodyReaders = parent.messageBodyReaders;
         attachedMessageBodyReaders = true;
      }
      if (parent.messageBodyWriters != null) {
         this.messageBodyWriters = parent.messageBodyWriters;
         attachedMessageBodyWriters = true;
      }
      if (parent.readerInterceptorRegistry != null) {
         this.readerInterceptorRegistry = parent.readerInterceptorRegistry;
         attachedReaderInterceptors = true;
      }
      if (parent.writerInterceptorRegistry != null) {
         this.writerInterceptorRegistry = parent.writerInterceptorRegistry;
         attachedWriterInterceptors = true;
      }
      if (parent.dynamicFeatures != null) {
         this.dynamicFeatures = parent.dynamicFeatures;
         attachedFeatures = true;
      }
   }

   protected void processProviderContracts(Class provider, Integer priorityOverride, boolean isBuiltin,
         Map<Class<?>, Integer> contracts, Map<Class<?>, Integer> newContracts)
   {
      // Liberty change - try/catch
      try {
      if (Utils.isA(provider, MessageBodyReader.class, contracts))
      {
         try
         {
            int priority = Utils.getPriority(priorityOverride, contracts, MessageBodyReader.class, provider);
            addMessageBodyReader(Utils.createProviderInstance(rpf, (Class<? extends MessageBodyReader>) provider), provider,
                  priority, isBuiltin);
            newContracts.put(MessageBodyReader.class, priority);
         }
         catch (Exception e)
         {
            throw new RuntimeException(Messages.MESSAGES.unableToInstantiateMessageBodyReader(), e);
         }
      }
      if (Utils.isA(provider, MessageBodyWriter.class, contracts))
      {
         try
         {
            int priority = Utils.getPriority(priorityOverride, contracts, MessageBodyWriter.class, provider);
            addMessageBodyWriter(Utils.createProviderInstance(rpf, (Class<? extends MessageBodyWriter>) provider), provider,
                  priority, isBuiltin);
            newContracts.put(MessageBodyWriter.class, priority);
         }
         catch (Exception e)
         {
            throw new RuntimeException(Messages.MESSAGES.unableToInstantiateMessageBodyWriter(), e);
         }
      }
      if (Utils.isA(provider, ReaderInterceptor.class, contracts))
      {
         int priority = Utils.getPriority(priorityOverride, contracts, ReaderInterceptor.class, provider);
         addReaderInterceptor(provider, priority);
         newContracts.put(ReaderInterceptor.class, priority);
      }
      if (Utils.isA(provider, WriterInterceptor.class, contracts))
      {
         int priority = Utils.getPriority(priorityOverride, contracts, WriterInterceptor.class, provider);
         addWriterInterceptor(provider, priority);
         newContracts.put(WriterInterceptor.class, priority);
      }
      if (Utils.isA(provider, DynamicFeature.class, contracts))
      {
         int priority = Utils.getPriority(priorityOverride, contracts, DynamicFeature.class, provider);
         addDynamicFeature(provider);
         newContracts.put(DynamicFeature.class, priority);
      }
      } catch (Exception ex) {
         Tr.warning(tc, "INVALID_PROVIDER_CWWKW1305W", provider == null ? "null" : provider.getName());
      }
      // Liberty change end
   }

   protected void processProviderInstanceContracts(Object provider, Map<Class<?>, Integer> contracts,
         Integer priorityOverride, boolean builtIn, Map<Class<?>, Integer> newContracts)
   {
      if (Utils.isA(provider, MessageBodyReader.class, contracts))
      {
         try
         {
            int priority = Utils.getPriority(priorityOverride, contracts, MessageBodyReader.class, provider.getClass());
            addMessageBodyReader((MessageBodyReader) provider, provider.getClass(), priority, builtIn);
            newContracts.put(MessageBodyReader.class, priority);
         }
         catch (Exception e)
         {
            throw new RuntimeException(Messages.MESSAGES.unableToInstantiateMessageBodyReader(), e);
         }
      }
      if (Utils.isA(provider, MessageBodyWriter.class, contracts))
      {
         try
         {
            int priority = Utils.getPriority(priorityOverride, contracts, MessageBodyWriter.class, provider.getClass());
            addMessageBodyWriter((MessageBodyWriter) provider, provider.getClass(), priority, builtIn);
            newContracts.put(MessageBodyWriter.class, priority);
         }
         catch (Exception e)
         {
            throw new RuntimeException(Messages.MESSAGES.unableToInstantiateMessageBodyWriter(), e);
         }
      }
      if (Utils.isA(provider, ReaderInterceptor.class, contracts))
      {
         int priority = Utils.getPriority(priorityOverride, contracts, ReaderInterceptor.class, provider.getClass());
         JaxrsInterceptorRegistry<ReaderInterceptor> registry = getReaderInterceptorRegistryForWrite();
         registry.registerSingleton((ReaderInterceptor) provider, priority);
         attachedReaderInterceptors = false;
         readerInterceptorRegistry = registry;
         newContracts.put(ReaderInterceptor.class, priority);
      }
      if (Utils.isA(provider, WriterInterceptor.class, contracts))
      {
         int priority = Utils.getPriority(priorityOverride, contracts, WriterInterceptor.class, provider.getClass());
         JaxrsInterceptorRegistry<WriterInterceptor> registry = getWriterInterceptorRegistryForWrite();
         registry.registerSingleton((WriterInterceptor) provider, priority);
         attachedWriterInterceptors = false;
         writerInterceptorRegistry = registry;
         newContracts.put(WriterInterceptor.class, priority);
      }
      if (Utils.isA(provider, DynamicFeature.class, contracts))
      {
         int priority = Utils.getPriority(priorityOverride, contracts, DynamicFeature.class, provider.getClass());
         Set<DynamicFeature> registry = getDynamicFeaturesForWrite();
         registry.add((DynamicFeature) provider);
         attachedFeatures = false;
         dynamicFeatures = registry;
         newContracts.put(DynamicFeature.class, priority);
      }
   }

   protected void addMessageBodyReader(MessageBodyReader provider, Class<?> providerClass, int priority,
         boolean isBuiltin)
   {
      SortedKey<MessageBodyReader> key = new SortedKey<MessageBodyReader>(MessageBodyReader.class, provider,
            providerClass, priority, isBuiltin);
      Utils.injectProperties(rpf, providerClass, provider);
      Consumes consumeMime = provider.getClass().getAnnotation(Consumes.class);
      MediaTypeMap<SortedKey<MessageBodyReader>> registry = getMessageBodyReadersForWrite();
      if (consumeMime != null)
      {
         for (String consume : consumeMime.value())
         {
            registry.add(consume, key);
         }
      }
      else
      {
         registry.addWildcard(key);
      }
      attachedMessageBodyReaders = false;
      messageBodyReaders = registry;
   }

   protected void addMessageBodyWriter(MessageBodyWriter provider, Class<?> providerClass, int priority,
         boolean isBuiltin)
   {
      Utils.injectProperties(rpf, providerClass, provider);
      Produces consumeMime = provider.getClass().getAnnotation(Produces.class);
      SortedKey<MessageBodyWriter> key = new SortedKey<MessageBodyWriter>(MessageBodyWriter.class, provider,
            providerClass, priority, isBuiltin);
      MediaTypeMap<SortedKey<MessageBodyWriter>> registry = getMessageBodyWritersForWrite();
      if (consumeMime != null)
      {
         for (String consume : consumeMime.value())
         {
            registry.add(consume, key);
         }
      }
      else
      {
         registry.add(MediaType.WILDCARD, key);
      }
      attachedMessageBodyWriters = false;
      messageBodyWriters = registry;
   }

   protected MediaTypeMap<SortedKey<MessageBodyReader>> getMessageBodyReadersForWrite() {
      if (messageBodyReaders == null) {
         return new MediaTypeMap<>();
      } else if (lockSnapshots || attachedMessageBodyReaders) {
         return new MediaTypeMap<>(messageBodyReaders);
      }
      return messageBodyReaders;
   }

   protected MediaTypeMap<SortedKey<MessageBodyWriter>> getMessageBodyWritersForWrite() {
      if (messageBodyWriters == null) {
         return new MediaTypeMap<>();
      } else if (lockSnapshots || attachedMessageBodyWriters) {
         return new MediaTypeMap<>(messageBodyWriters);
      }
      return messageBodyWriters;
   }

   protected JaxrsInterceptorRegistry<ReaderInterceptor> getReaderInterceptorRegistryForWrite() {
      if (readerInterceptorRegistry == null) {
         return new ReaderInterceptorRegistryImpl(rpf);
      } else if (lockSnapshots || attachedReaderInterceptors) {
         return readerInterceptorRegistry.clone(rpf);
      }
      return readerInterceptorRegistry;
   }

   protected JaxrsInterceptorRegistry<WriterInterceptor> getWriterInterceptorRegistryForWrite() {
      if (writerInterceptorRegistry == null) {
         return new WriterInterceptorRegistryImpl(rpf);
      } else if (lockSnapshots || attachedWriterInterceptors) {
         return writerInterceptorRegistry.clone(rpf);
      }
      return writerInterceptorRegistry;
   }

   protected Set<DynamicFeature> getDynamicFeaturesForWrite() {
      if (dynamicFeatures == null) {
         return new HashSet<>();
      } else if (lockSnapshots || attachedFeatures) {
         return new HashSet<>(dynamicFeatures);
      }
      return dynamicFeatures;
   }

   public MediaTypeMap<SortedKey<MessageBodyReader>> getMessageBodyReaders() {
      return messageBodyReaders;
   }

   public MediaTypeMap<SortedKey<MessageBodyWriter>> getMessageBodyWriters() {
      return messageBodyWriters;
   }

   public JaxrsInterceptorRegistry<ReaderInterceptor> getReaderInterceptorRegistry() {
      return readerInterceptorRegistry;
   }

   public JaxrsInterceptorRegistry<WriterInterceptor> getWriterInterceptorRegistry() {
      return writerInterceptorRegistry;
   }

   public Set<DynamicFeature> getDynamicFeatures() {
      return dynamicFeatures;
   }

   public void lockSnapshots() {
      this.lockSnapshots = true;
      if (messageBodyReaders != null) {
         messageBodyReaders.lockSnapshots();
      }
      if (messageBodyWriters != null) {
         messageBodyWriters.lockSnapshots();
      }
   }

   // for quarkus

   public void addDynamicFeature(Class provider) {
      Set<DynamicFeature> registry = getDynamicFeaturesForWrite();
      registry.add((DynamicFeature) rpf.injectedInstance(provider));
      attachedFeatures = false;
      dynamicFeatures = registry;
   }

   public void addWriterInterceptor(Class provider, int priority) {
      JaxrsInterceptorRegistry<WriterInterceptor> registry = getWriterInterceptorRegistryForWrite();
      registry.registerClass(provider, priority);
      attachedWriterInterceptors = false;
      writerInterceptorRegistry = registry;
   }

   public void addReaderInterceptor(Class provider, int priority) {
      JaxrsInterceptorRegistry<ReaderInterceptor> registry = getReaderInterceptorRegistryForWrite();
      registry.registerClass(provider, priority);
      attachedReaderInterceptors = false;
      readerInterceptorRegistry = registry;
   }

   public void addWildcardMBR(SortedKey<MessageBodyReader> mbr) {
      MediaTypeMap<SortedKey<MessageBodyReader>> registry = getMessageBodyReadersForWrite();
      registry.addWildcard(mbr);
      attachedMessageBodyReaders = false;
      messageBodyReaders = registry;
   }

   public void addSubtypeWildMBR(MediaType mediaType, SortedKey<MessageBodyReader> mbr) {
      MediaTypeMap<SortedKey<MessageBodyReader>> registry = getMessageBodyReadersForWrite();
      registry.addWildSubtype(mediaType, mbr);
      attachedMessageBodyReaders = false;
      messageBodyReaders = registry;
   }

   public void addRegularMBR(MediaType mediaType, SortedKey<MessageBodyReader> mbr) {
      MediaTypeMap<SortedKey<MessageBodyReader>> registry = getMessageBodyReadersForWrite();
      registry.addRegular(mediaType, mbr);
      attachedMessageBodyReaders = false;
      messageBodyReaders = registry;
   }

   public void addCompositeWildcardMBR(MediaType mediaType, SortedKey<MessageBodyReader> mbr, String baseSubtype) {
      MediaTypeMap<SortedKey<MessageBodyReader>> registry = getMessageBodyReadersForWrite();
      registry.addCompositeWild(mediaType, mbr, baseSubtype);
      attachedMessageBodyReaders = false;
      messageBodyReaders = registry;
   }

   public void addWildcardCompositeMBR(MediaType mediaType, SortedKey<MessageBodyReader> mbr, String baseSubtype) {
      MediaTypeMap<SortedKey<MessageBodyReader>> registry = getMessageBodyReadersForWrite();
      registry.addWildComposite(mediaType, mbr, baseSubtype);
      attachedMessageBodyReaders = false;
      messageBodyReaders = registry;
   }

   public void addWildcardMBW(SortedKey<MessageBodyWriter> mbw) {
      MediaTypeMap<SortedKey<MessageBodyWriter>> registry = getMessageBodyWritersForWrite();
      registry.addWildcard(mbw);
      attachedMessageBodyWriters = false;
      messageBodyWriters = registry;
   }

   public void addRegularMBW(MediaType mediaType, SortedKey<MessageBodyWriter> mbw) {
      MediaTypeMap<SortedKey<MessageBodyWriter>> registry = getMessageBodyWritersForWrite();
      registry.addRegular(mediaType, mbw);
      attachedMessageBodyWriters = false;
      messageBodyWriters = registry;
   }
   public void addSubtypeWildMBW(MediaType mediaType, SortedKey<MessageBodyWriter> mbw) {
      MediaTypeMap<SortedKey<MessageBodyWriter>> registry = getMessageBodyWritersForWrite();
      registry.addWildSubtype(mediaType, mbw);
      attachedMessageBodyWriters = false;
      messageBodyWriters = registry;
   }

   public void addCompositeWildcardMBW(MediaType mediaType, SortedKey<MessageBodyWriter> mbw, String baseSubtype) {
      MediaTypeMap<SortedKey<MessageBodyWriter>> registry = getMessageBodyWritersForWrite();
      registry.addCompositeWild(mediaType, mbw, baseSubtype);
      attachedMessageBodyWriters = false;
      messageBodyWriters = registry;
   }

   public void addWildcardCompositeMBW(MediaType mediaType, SortedKey<MessageBodyWriter> mbw, String baseSubtype) {
      MediaTypeMap<SortedKey<MessageBodyWriter>> registry = getMessageBodyWritersForWrite();
      registry.addWildComposite(mediaType, mbw, baseSubtype);
      attachedMessageBodyWriters = false;
      messageBodyWriters = registry;
   }
}
