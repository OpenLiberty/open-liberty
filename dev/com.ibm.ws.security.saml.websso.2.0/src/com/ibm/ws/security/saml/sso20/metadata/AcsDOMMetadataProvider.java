/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.ws.security.saml.sso20.metadata;

import java.io.File;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.metadata.resolver.filter.FilterException;
import org.opensaml.saml.metadata.resolver.impl.DOMMetadataResolver;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.w3c.dom.Element;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.internal.utils.FileInfo;

import net.shibboleth.utilities.java.support.xml.QNameSupport;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;

/**
 * A <code>MetadataProvider</code> implementation that retrieves metadata from a DOM <code>Element</code> as
 * supplied by the user.
 *
 * It is the responsibility of the caller to re-initialize, via {@link #initialize()}, if any properties of this
 * provider are changed.
 */
public class AcsDOMMetadataProvider extends DOMMetadataResolver {
    @SuppressWarnings("unused")
    private static TraceComponent tc = Tr.register(AcsDOMMetadataProvider.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);
    private final FileInfo fileInfo;
    private final Element mdElement;

    /**
     * Constructor.
     *
     * @param mdElement the metadata element
     * @throws SamlException
     */
    public AcsDOMMetadataProvider(Element mdElement, final File file) throws SamlException {
        super(mdElement);
        this.mdElement = mdElement;
        fileInfo = FileInfo.getFileInfo(file);
    }

    public String getMetadataFilename() {
        return fileInfo.getPath();
    }

    public boolean sameIdpFile(File file) throws SamlException {
        FileInfo newFile = FileInfo.getFileInfo(file);
        return this.fileInfo.equals(newFile);
    }

    public String getEntityId() {
        if (this.mdElement != null) {
            return this.mdElement.getAttribute("entityID");
        }
        return null;
    }

//    @Override
//    protected void initMetadataResolver() /* throws ComponentInitializationException */ {
//        try {
//            super.initMetadataResolver();
//        } catch (Exception e1) {
//            // TODO Auto-generated catch block
//            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
//            e1.printStackTrace();
//        }
//        XMLObjectBuilderFactory xmlObjectBuilderFactory;
//        try {
//            Tr.debug(tc, "@AV999, Babuji, in the override , mde", SerializeSupport.nodeToString(this.mdElement));
//            xmlObjectBuilderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
//            XMLObjectBuilder<?> xmlObjectBuilder = xmlObjectBuilderFactory.getBuilder(this.mdElement);
//
//            Tr.debug(tc, "@AV999, Babuji, builder for ", QNameSupport.getNodeQName(this.mdElement));
//            Tr.debug(tc, "@AV999, Babuji, builder is =  ", xmlObjectBuilder.getClass().getName());
//            
////            XMLObject md = xmlObjectBuilder.buildObject(this.mdElement);
////            
////            Tr.debug(tc, "@AV999, Babuji, in the override , xml object ", (EntityDescriptor)md);
////            
//            
//            final Unmarshaller unmarshaller = getUnmarshallerFactory().getUnmarshaller(this.mdElement);
//            final XMLObject metadataTemp = unmarshaller.unmarshall(this.mdElement);
//            Class<?>[] interfaces = metadataTemp.getClass().getInterfaces();
//            for(int i = 0; i < interfaces.length; i++) 
//            { 
//                Tr.debug(tc, "Lalaji ,classloader of the interface from implementation ", interfaces[i].getClassLoader()); 
//                Tr.debug(tc, "Lalaji, classloader of the interface itself ", EntityDescriptor.class.getClassLoader()); 
//            }
//            ;
//            Tr.debug(tc, "@AV999, Babuji, in the override , xml object ", SerializeSupport.nodeToString(metadataTemp.getDOM()));
//            Tr.debug(tc, "@AV999, Babuji, in the override , xml object instance before filter ", (EntityDescriptor) metadataTemp);
//            final XMLObject filteredMetadata = filterMetadata(metadataTemp);
//
//            Tr.debug(tc, "@AV999, Babuji, in the override , xml object after filter ", SerializeSupport.nodeToString(filteredMetadata.getDOM()));
//            Tr.debug(tc, "@AV999, Babuji, in the override , xml object instance after filter ", (EntityDescriptor) filteredMetadata);
//
//            final BatchEntityBackingStore newBackingStore = preProcessNewMetadata(metadataTemp);
//            releaseMetadataDOM(metadataTemp);
//            setBackingStore(newBackingStore);
//        } catch (final UnmarshallingException e) {
//            final String errorMsg = "Unable to unmarshall metadata element";
//            //log.error("{} {}: {}", getLogPrefix(), errorMsg, e.getMessage());
//            //throw new ComponentInitializationException(errorMsg, e);
//        } catch (final FilterException e) {
//            final String errorMsg = "Unable to filter metadata";
//            //log.error("{} {}: {}", getLogPrefix(), errorMsg, e.getMessage());
//            //throw new ComponentInitializationException(errorMsg, e);
//        }
//    }
}