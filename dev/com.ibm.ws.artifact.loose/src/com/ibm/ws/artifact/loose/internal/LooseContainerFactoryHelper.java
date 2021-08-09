/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.loose.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.artifact.contributor.ArtifactContainerFactoryHelper;
import com.ibm.ws.artifact.loose.internal.LooseArchive.EntryType;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.kernel.service.util.DesignatedXMLInputFactory;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.FileUtils;

public class LooseContainerFactoryHelper implements ArtifactContainerFactoryHelper {

    private WsLocationAdmin locationService = null;
    private final AtomicServiceReference<ArtifactContainerFactory> containerFactoryReference = new AtomicServiceReference<ArtifactContainerFactory>("containerFactory");
    private BundleContext ctx;

    private static final TraceComponent tc = Tr.register(LooseContainerFactoryHelper.class);

    protected void activate(ComponentContext ctx) {
        containerFactoryReference.activate(ctx);
        this.ctx = ctx.getBundleContext();
    }

    @Override
    public ArtifactContainer createContainer(File cacheDir, Object o) {
        LooseArchive laRoot = null;

        if (o instanceof File) {
            File f = (File) o;
            //make sure the file exists, can be read and is an xml
            if (FileUtils.fileExists(f) && FileUtils.fileIsFile(f) && FileUtils.fileCanRead(f) && f.getName().toLowerCase().endsWith(".xml")) {
                LooseArchive la = null;
                InputStream fileStream = null;

                //if the xml file exists and is found.
                try {
                    la = new LooseArchive(cacheDir, this, f);
                    laRoot = la;

                    Stack<LooseArchive> stack = new Stack<LooseArchive>();
                    //put the new loose archive (top level archive) on top of stack.
                    stack.push(la);

                    fileStream = FileUtils.getInputStream(f);
                    XMLStreamReader reader = DesignatedXMLInputFactory.newInstance().createXMLStreamReader(fileStream);

                    //while there is more content in the xml file, and la isn't null 
                    //(if la is null we have closed all opened elements and should stop)
                    while (reader.hasNext() && la != null) {
                        int result = reader.nextTag();
                        if (result == XMLStreamConstants.START_ELEMENT) {
                            if ("archive".equals(reader.getLocalName())) {
                                String targetLocation = getAttribute(reader, "targetInArchive");
                                if (targetLocation != null) {
                                    //null will either be root archive or an archive which lacks targetInArchive (only root cannot have this)
                                    if (!targetLocation.startsWith("/")) {
                                        targetLocation = "/" + targetLocation;
                                    }
                                    LooseArchive prior = la;
                                    //create new la with this as parent
                                    //remove the absolute "/" from targetLocation
                                    String relativeLocation = targetLocation.substring(1);
                                    //build new file representing cachedir for the newarchive, beneath the current one.
                                    File newCacheDir = new File(cacheDir, relativeLocation);
                                    la = new LooseArchive(newCacheDir, this, prior, targetLocation, f);
                                    //put la onto stack
                                    stack.push(la);
                                    EntryType.ARCHIVE.put(prior, targetLocation, la, this);
                                }
                            } else if ("dir".equals(reader.getLocalName())) {
                                readElement(reader, EntryType.DIR, la);
                            } else if ("file".equals(reader.getLocalName())) {
                                readElement(reader, EntryType.FILE, la);
                            }
                        } else if (result == XMLStreamConstants.END_ELEMENT) {
                            if ("archive".equals(reader.getLocalName())) {
                                //remove and grab most recently added la from stack.
                                //remove current la from stack
                                stack.pop();
                                //make sure stack isn't empty before we try and look at it
                                if (!stack.empty()) {
                                    //set la to be the top of stack
                                    la = stack.peek();
                                } else {
                                    la = null;
                                }
                            }
                        }
                    }
                } catch (FileNotFoundException fnfe) {
                    FFDCFilter.processException(fnfe, getClass().getName(), "looseconfigxmlnotfound");
                    Tr.error(tc, "XML_NOT_FOUND", f.getAbsolutePath());

                    // Return null if we hit an exception as the loose archive will not be well formed
                    return null;
                } catch (XMLStreamException xse) {
                    FFDCFilter.processException(xse, getClass().getName(), "looseconfigxmlstream");
                    Tr.error(tc, "XML_STREAM_ERROR", f.getAbsolutePath());

                    // Return null if we hit an exception as the loose archive will not be well formed
                    return null;
                } catch (FactoryConfigurationError e) {
                    FFDCFilter.processException(e, getClass().getName(), "looseconfigxmlfactoryissue");

                    // Return null if we hit an exception as the loose archive will not be well formed
                    return null;
                } finally {
                    if (fileStream != null) {
                        try {
                            fileStream.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
        }
        return laRoot;
    }

    private String getAttribute(XMLStreamReader reader, String localName) {
        int attributes = reader.getAttributeCount();
        for (int i = 0; i < attributes; i++) {
            String name = reader.getAttributeLocalName(i);
            if (localName.equals(name)) {
                if ("sourceOnDisk".equals(name)) {
                    return resolvePathVariables(reader.getAttributeValue(i));
                } else {
                    return reader.getAttributeValue(i);
                }
            }
        }
        return null;
    }

    private void readElement(XMLStreamReader reader, EntryType et, LooseArchive la) {
        int attributes = reader.getAttributeCount();
        String target = null;
        String source = null;
        String excludes = null;
        for (int i = 0; i < attributes && (target == null || source == null || excludes == null); i++) {
            String name = reader.getAttributeLocalName(i);
            String value = reader.getAttributeValue(i);
            //when we find an attribute with a name we want, grab it and set it to the correct string
            if ("targetInArchive".equals(name)) {
                target = value;
            } else if ("sourceOnDisk".equals(name)) {
                source = resolvePathVariables(value);
            } else if ("excludes".equals(name)) {
                excludes = value;
            }
        }
        if (target != null && source != null) {
            if (excludes != null) {
                et.put(la, target, source, excludes, this);
            } else {
                et.put(la, target, source, this);
            }
        }
    }

    /**
     * take a path and resolve it in case of $(was.server.blah) variables
     * 
     * @param inputPath
     * @return
     */
    private String resolvePathVariables(String inputPath) {
        if (PathUtil.containsSymbol(inputPath)) {
            return locationService.resolveString(inputPath);
        } else {
            return inputPath;
        }
    }

    @Override
    public ArtifactContainer createContainer(File cacheDir, ArtifactContainer parent, ArtifactEntry entry, Object o) {
        // unused but needed as we implement ContainerFactoryHelper
        // this method intended to create containers nested in other containers. 
        // we currently only support loose archives as top level root containers.
        // by returning null here we prevent say loose wars working inside an ear jar.
        return null;
    }

    protected void setLocationService(WsLocationAdmin locationService) {
        this.locationService = locationService;
    }

    protected void unsetLocationService(WsLocationAdmin locationService) {
        // Unbind method required to avoid error message
    }

    protected void setContainerFactory(ServiceReference<ArtifactContainerFactory> cf) {
        containerFactoryReference.setReference(cf);
    }

    protected void unsetContainerFactory(ServiceReference<ArtifactContainerFactory> cf) {
        containerFactoryReference.unsetReference(cf);
    }

    public ArtifactContainerFactory getContainerFactory() {
        return containerFactoryReference.getServiceWithException();
    }

    public BundleContext getBundleContext() {
        return ctx;
    }
}
