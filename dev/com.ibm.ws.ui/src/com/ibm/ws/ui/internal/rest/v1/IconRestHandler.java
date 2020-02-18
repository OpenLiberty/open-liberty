/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.rest.v1;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler;
import com.ibm.ws.ui.internal.rest.exceptions.MethodNotSupportedException;
import com.ibm.ws.ui.internal.rest.exceptions.RESTException;
import com.ibm.ws.ui.internal.rest.exceptions.UserNotAuthorizedException;
import com.ibm.ws.ui.internal.v1.IFeatureToolService;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 * <p>This class is designed to hold icon serving methods that different parts of
 * the UI will need to use.</p>
 *
 * <p>Maps to host:port/ibm/api/adminCenter/v1/icons</p>
 *
 * /ibm/api/adminCenter/v1/&lt;featureName&gt; - If the header contains an icon with no size, that will be returned. If all icons do have sizes, the
 * icon that matches the default size will be returned. If this doesn't exist, you will be given the default icon at the default size.
 * /ibm/api/adminCenter/v1/&lt;featureName&gt;?size=nnn - The supplied icon at the requested size will be returned. If this doesn't exist, you get the
 * default icon at that size, and finally if that doesn't exist, you get the default icon at the default size.
 * /ibm/api/adminCenter/v1/&lt;featureName&gt;?sizes - returns a JSON Object listing the sizes of the available icons. Any icons that don't have a
 * size directive, will be returned with an "unsized value".
 *
 * All the above calls, can also have the featureName set to be "default", and you get icons/information about the default icons in the system
 */
public class IconRestHandler extends CommonJSONRESTHandler implements V1Constants {

    private static final TraceComponent tc = Tr.register(IconRestHandler.class);
    private static final String defaultIconPath = "/images/tools/";
    private static final String defaultIconPrefix = "defaultTool_";
    private static final String defaultIconSuffix = "png";
    private static final int defaultIconSize = 142;
    private static final String defaultFeatureRestURI = "default";
    private static final String sizesRestURI = "sizes";
    // Set the default Table/Desktop icon sizes
    private static final Set<Integer> defaultIconSizes = new HashSet<Integer>(Arrays.asList(142, 78, 52, 28));
    private transient IFeatureToolService featureToolService = null;
    // List of valid Icon file types.
    private static final Set<String> validIconTypes = new HashSet<String>(Arrays.asList("png", "jpg", "jpeg", "gif"));

    public IconRestHandler(IFeatureToolService featureToolService) {
        super(ICON_PATH, true, true);
        this.featureToolService = featureToolService;
    }

    /** {@inheritDoc} */
    @Override
    public Object getBase(RESTRequest request, RESTResponse response) throws RESTException {
        throw new MethodNotSupportedException();
    }

    /** {@inheritDoc} */
    @Override
    public Object getChild(RESTRequest request, RESTResponse response, String childResource) throws RESTException {
        if (!isAuthorizedDefault(request, response)) {
            throw new UserNotAuthorizedException();
        }
        return processChild(request, false);
    }

    @Override
    public Object getGrandchild(final RESTRequest request, final RESTResponse response, final String child, final String grandchild) throws RESTException {
        if (!isAuthorizedDefault(request, response)) {
            throw new UserNotAuthorizedException();
        }
        return processChild(request, true);
    }

    private Object processChild(final RESTRequest request, final boolean isGrandChild) {
        Object responseResult = null;

        // If it is calling from getChild, get the last element in the URI. This should be the featureName.
        // If it is calling from getGrandChild, get the last 2 elements in the URI. This should be the featureName + endpointName.
        String[] urlElements = request.getPath().split("/");
        String featureName;
        String featureOrEndpointName = urlElements[urlElements.length - 1];
        if (isGrandChild) {
            featureName = urlElements[urlElements.length - 2] + "/" + featureOrEndpointName;
        } else {
            featureName = featureOrEndpointName;
        }

        // If the queryString contains sizes, then return the list of icon sizes for the required feature.
        // If it contains size=nnn then find the icon that matches the requested size. Otherwise get the unsized icon.
        String queryString = request.getQueryString();
        if (queryString != null && sizesRestURI.equalsIgnoreCase(queryString)) {
            // If the queryString is sizes then return the list of sizes of icons for the requires feature.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Finding icon sizes for " + featureName);
            responseResult = getSizes(featureName);
        } else {
            // If the queryString contains size=nnn then find the icon that matches the requested size. If no size is listed then
            // get the unsized icon. If there is no icon found we'll return a default equivalent.
            if (queryString != null && queryString.toLowerCase().startsWith("size=")) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Finding icon size " + queryString.substring(5) + " for " + featureName);
                responseResult = getIcon(featureName, queryString.substring(5));
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Finding default icon for " + featureName);
                responseResult = getIcon(featureName, "0");
            }
        }

        return responseResult;
    }

    /**
     * This method returns a Set of Integers that lists the sizes of icons for the particular feature.
     *
     * @param featureName - The feature to look up the icons for.
     * @return - A Set of Integers that list the sizes of available icons.
     */
    private Set<Integer> getSizes(String featureName) {
        Set<Integer> sizes = new HashSet<Integer>();

        // If we are asking about the default icons, return our pre-defined list of sizes.
        if (defaultFeatureRestURI.equals(featureName)) {
            sizes.addAll(defaultIconSizes);
            // othewise go and find the list.
        } else {
            sizes.addAll(getFeatureToolIconInfo(featureName).keySet());
        }

        return sizes;
    }

    /**
     * This method takes an icon header and divides it up into the uri and optionally the size of the icon.
     *
     * @param iconHeader          - The string representing the Subsystem-Icon header.
     * @param featureSymbolicName - The Symbolic name of the feature.
     * @param iconLocation        - The absolute path to the icons dir for the current feature.
     * @return - A Map keyed by size, and values of the relative Icon uri.
     */
    private Map<Integer, String> processIconHeader(String iconHeader, String featureSymbolicName, String iconLocation) {

        Map<Integer, String> iconMapping = new HashMap<Integer, String>();
        if (iconHeader != null) {
            if (iconLocation == null)
                iconLocation = "";

            // If we have a header string, then split on commas.
            String[] icons = iconHeader.split(",");
            for (String iconDirective : icons) {
                // For each icon attempt to get the size directive if there is one.
                String[] iconDirectives = iconDirective.split(";");
                String relativeIconURI = iconDirectives[0].trim();
                // Default the size to 0 so we can identify any unsized icons. It makes no sense to have multiple unsized icons,
                // so if there are multiple specified, we will return just one of them.
                Integer iconSize = 0;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Icon URI " + relativeIconURI);

                if (iconDirectives.length > 1) {
                    String iconSizeString = iconDirectives[1].trim();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Icon Directive " + iconSizeString);
                    if (iconSizeString != null && iconSizeString.startsWith("size="))
                        try {
                            iconSize = new Integer(iconSizeString.substring(5));
                        } catch (NumberFormatException nfe) {
                            // Nothing to do as we will default to an unsized icon.
                        }
                }

                File installPrefix = new File(iconLocation, featureSymbolicName);
                File iconURL = new File(installPrefix, relativeIconURI.trim());

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Icon Absolute URL " + iconURL.getAbsolutePath());
                // Check that the absolute path of the icon is actually still in the
                // icon dir that we expect. This check stops people from having ../../ in their Icon URI's in
                // their Subsystem-Icon headers.
                if (iconURL.getAbsolutePath().startsWith(installPrefix.getAbsolutePath())) {
                    iconMapping.put(iconSize, iconURL.getAbsolutePath());
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Icon URL " + iconURL.getAbsolutePath() + " is not mapping to the Standard Icons Dir. We will not be adding the "
                                     + "url to the map, and so feature + " + featureSymbolicName + " will use the default icons.");
                }
            }
        }
        return iconMapping;
    }

    /**
     * This method returns a Map of the sizes and URIs for the icons for the requested feature. If the feature
     * doesn't exist or doesn't have an icon header, an empty map is returned.
     *
     * @param featureName - A String containing the name of the feature to look up.
     * @return - A map keyed by an Integer for the icon size, and a value of the relative URI to the Icon.
     */
    private synchronized Map<Integer, String> getFeatureToolIconInfo(String featureName) {
        Map<Integer, String> iconInfo = new HashMap<Integer, String>();

        if (featureToolService != null) {
            // Get the IconMap from the featureToolService. This should be completely up-to-date.
            Map<String, String> allIconMap = featureToolService.getIconMap();
            // Attempt to get the icon header for the required feature.
            String iconHeader = allIconMap.get(featureName);
            // If we have a header, then process it, otherwise return the default icon at the required size.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Icon header for " + featureName + ": " + iconHeader);
            if (iconHeader != null) {
                // Attempt to get the installLocation for the Icon.
                String iconInstallLocation = featureToolService.getFeatureIconInstallDir(featureName);
                // Process the header. the returns the sizes and the URI that each size refers to.
                iconInfo = processIconHeader(iconHeader, featureName, iconInstallLocation);
            }
        }
        return iconInfo;
    }

    /**
     * This method returns an IconImage object containing the Icon for the feature at the requested size, or
     * the default equivalent. If there are no default icons at the size requested, then we return the default icon
     * at the default size.
     *
     * @param featureName - the feature name to get the icon for.
     * @param size        - the required size of the icon. If 0 or null is passed in, the unsized icon will be selected.
     * @return - An IconImage object containing the bytes of the image as well as the image type.
     */
    private IconImage getIcon(String featureName, String sizeString) {

        IconImage image = null;

        Integer size = Integer.valueOf(0);
        try {
            size = Integer.valueOf(sizeString);
        } catch (NumberFormatException nfe) {
            // Nothing to do here, because we default to 0.
        }

        // If someone has set a size < 0 then that won't match an icon, so we'll default to the unsized
        // icon.
        if (size < 0)
            size = 0;

        // set a final var so we can use it in the dopriv block.
        final Integer iconSize = size;
        // Process default icon requests. If not default icon, then process the feature info.
        if (defaultFeatureRestURI.equals(featureName)) {
            image = getDefaultIcon(size);
        } else {
            // Get the icon sizes and URI's for the required Feature.
            Map<Integer, String> iconInfo = getFeatureToolIconInfo(featureName);
            final String iconFileName;

            //if we've attempted to get a default sized icon and not found one because all icons
            //in manifest header have associated sizes, then attempt to get icon with default size rather than 0)
            if (iconInfo.get(size) != null) {
                iconFileName = iconInfo.get(size);
            } else {
                iconFileName = iconInfo.get(defaultIconSize);
            }

            // If we find that we have the sized icon in the feature, attempt to read and process that. Otherwise
            // drop back to using the default icons.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Icon FileName: " + iconFileName);

            if (iconFileName != null) {
                image = AccessController.doPrivileged(new PrivilegedAction<IconImage>() {

                    @Override
                    public IconImage run() {
                        IconImage returnImage = null;
                        // Create a file pointing to the on disk location. Check that we're pointing to a file and that it exists and that
                        // the file type is valid. If not drop back to the default Icon.
                        File iconFile = new File(iconFileName);
                        String iconType = iconFile.getName().substring(iconFile.getName().lastIndexOf(".") + 1);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Icon FileName: " + iconFile.getAbsolutePath() + " - Exists?: " + iconFile.exists() +
                                         " - IsFile: " + iconFile.isFile() + " - IconType: " + iconType);

                        if (iconFile.exists() && iconFile.isFile() && validIconTypes.contains(iconType)) {
                            // Attempt to read the bytes. If we can read the file, create the IconImage for the icon.
                            // Otherwise drop back to the default Icon.
                            byte[] iconBytes = readIconFile(iconFile);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(tc, "IconBytes length: " + iconBytes.length);
                            if (iconBytes != null && iconBytes.length > 0) {
                                returnImage = new IconImage(iconType, iconBytes);
                            } else {
                                returnImage = getDefaultIcon(iconSize);
                            }
                        } else {
                            returnImage = getDefaultIcon(iconSize);
                        }
                        return returnImage;
                    }
                });
            } else {
                image = getDefaultIcon(iconSize);
            }
        }

        return image;
    }

    /**
     * This method returns the default icon of the size requested. If the requested size doesn't match the list of sizes for the
     * default icon, we return the default size.
     *
     * @param size - the size of the default icon to return
     * @return An IconImage object containing the bytes of the image as well as the image type.
     */
    private IconImage getDefaultIcon(Integer size) {

        // Default to the default size, and change it to the require size if it exists in the list of sizes.
        Integer sizeToUse = defaultIconSize;
        if (defaultIconSizes.contains(size)) {
            sizeToUse = size;
        }

        // Read the default icon from the adminCenter bundle, via the getResourceAsStream.
        final String iconPath = defaultIconPath + defaultIconPrefix + sizeToUse + "x" + sizeToUse + "." + defaultIconSuffix;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "IconPath: " + iconPath);

        byte[] imageBytes = AccessController.doPrivileged(new PrivilegedAction<byte[]>() {

            @Override
            public byte[] run() {
                byte[] imageBytes = null;
                InputStream iconIS = null;
                try {
                    iconIS = this.getClass().getResourceAsStream(iconPath);
                    imageBytes = readIconFile(iconIS);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "imageBytes length: " + imageBytes.length);
                } finally {
                    if (iconIS != null) {
                        try {
                            iconIS.close();
                        } catch (IOException ioe) {
                            // nothing we can do here except trace failure.
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(tc, "Exception thrown closing inputStream : " + ioe);
                        }
                    }
                }
                return imageBytes;
            }
        });
        return new IconImage(defaultIconSuffix, imageBytes);
    }

    /**
     * This method returns a byte array of the require icon file.
     *
     * @param iconFile - A file pointing to the icon on disk.
     * @return a byte[] containing the contents of the icon file.
     */
    private byte[] readIconFile(final File iconFile) {

        byte[] bytes = AccessController.doPrivileged(new PrivilegedAction<byte[]>() {

            @Override
            public byte[] run() {
                FileInputStream fis = null;
                byte[] bytes = new byte[] {};
                try {
                    fis = new FileInputStream(iconFile);
                    bytes = readIconFile(fis);
                } catch (FileNotFoundException fnfe) {
                    // FFDC the error, and trace out the issue, but we don't need to issue an error here.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Unable to find file " + iconFile.getAbsolutePath() + ": " + fnfe);
                } finally {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException ioe) {
                            // nothing we can do here except trace failure.
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(tc, "Exception thrown closing inputStream : " + ioe);
                        }
                    }
                }
                return bytes;
            }
        });

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "bytes length: " + bytes.length);
        return bytes;
    }

    /**
     * This method returns a byte array of the require icon input stream.
     *
     * @param iconFileStream - An inputstream to the required icon.
     * @return A byte[] containing the contents of the icon file.
     */
    private byte[] readIconFile(InputStream iconFileStream) {
        // Create the outputStream to store the bytes from the input stream.
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        if (iconFileStream != null) {
            try {
                // Read the inputstream into the outputStream.
                byte[] bytes = new byte[4096];
                int bytesRead = 0;
                while ((bytesRead = iconFileStream.read(bytes)) >= 0) {
                    bos.write(bytes, 0, bytesRead);
                }
            } catch (Exception e) {
                // Issue FFDC but we don't need to report here
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Exception thrown reading inputStream : " + e);
            } finally {
                try {
                    bos.flush();
                    bos.close();
                } catch (IOException ioe) {
                    // Issue FFDC but we don't need to report here
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Exception thrown closing outputStream : " + ioe);
                }
            }
        }
        // Return the bytes.

        return bos.toByteArray();
    }

    /**
     * {@inheritDoc} <p>This method defines the default behaviour for all handlers, both
     * those which only handle themselves, and those which handle direct children.
     * <b>This method should not be overriden by extenders.</b></p>
     * <p>This method will invoke delegate methods for GET, POST, PUT and
     * DELETE, and it will also protect against the handler being called for
     * a resource which this is not handling, such as a deeply nested child
     * resource.</p>
     * <p>This method also guards against any RuntimeExceptions that may be
     * encountred while processing. These internal errors must not be propgated
     * back to the caller.</p>
     */
    @Override
    protected final void delegateMethod(final RESTRequest request, final RESTResponse response) throws RESTException {
        if (!isAuthorizedDefault(request, response)) {
            throw new UserNotAuthorizedException();
        }
        final String method = request.getMethod();
        if (HTTP_METHOD_GET.equals(method)) {
            Object responseResult = doGET(request, response);
            if (responseResult instanceof IconImage) {
                setOutputResponse(response, (IconImage) responseResult, HTTP_OK);
            } else {
                setJSONResponse(response, responseResult, HTTP_OK);
            }
        } else {
            throw new MethodNotSupportedException();
        }
    }

    /**
     * <p>Sets the RESTResponse payload with the specific POJO's Byte array of the icon image.
     * Also sets the RESTResponse content type to the required media type and status code of 200.</p>
     * <p>If there is any exception, then return a 500 to indicate something went wrong.</p>
     *
     * @param response The RESTResponse from handleRequest
     * @param pojo     The byte[] to convert write to the response payload
     * @param status   The desired HTTPS status to set
     */
    final void setOutputResponse(final RESTResponse response, final IconImage image, final int status) {

        String mediaType = null;
        // check that the file type is one that we can process. Otherwise return an error.
        if ("png".equals(image.getImageType())) {
            mediaType = MEDIA_TYPE_IMAGE_PNG;
        } else if ("gif".equals(image.getImageType())) {
            mediaType = MEDIA_TYPE_IMAGE_GIF;
        } else if ("jpg".equals(image.getImageType()) || "jpeg".equals(image.getImageType())) {
            mediaType = MEDIA_TYPE_IMAGE_JPG;
        } else {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Unsupported Icon type. PNG, GIF or JPG are supported.");
            }
            response.setStatus(HTTP_INTERNAL_ERROR);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Media Type:" + mediaType);

        if (mediaType != null) {
            response.setResponseHeader(HTTP_HEADER_CONTENT_TYPE, mediaType);

            try {
                response.setStatus(status);
                response.getOutputStream().write(image.getImageBytes());
            } catch (IOException e) {
                if (tc.isEventEnabled()) {
                    Tr.event(tc, "Unexpected IOException while writing out POJO response", e);
                }
                response.setStatus(HTTP_INTERNAL_ERROR);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public POSTResponse postBase(RESTRequest request, RESTResponse response) throws RESTException {
        throw new MethodNotSupportedException();
    }

    /** {@inheritDoc} */
    @Override
    public POSTResponse postChild(RESTRequest request, RESTResponse response, String childResource) throws RESTException {
        throw new MethodNotSupportedException();
    }

    /** {@inheritDoc} */
    @Override
    public Object putBase(RESTRequest request, RESTResponse response) throws RESTException {
        throw new MethodNotSupportedException();
    }

    /** {@inheritDoc} */
    @Override
    public Object putChild(RESTRequest request, RESTResponse response, String childResource) throws RESTException {
        throw new MethodNotSupportedException();
    }

    /** {@inheritDoc} */
    @Override
    public Object deleteBase(RESTRequest request, RESTResponse response) throws RESTException {
        throw new MethodNotSupportedException();
    }

    /** {@inheritDoc} */
    @Override
    public Object deleteChild(RESTRequest request, RESTResponse response, String childResource) throws RESTException {
        throw new MethodNotSupportedException();
    }

    private static class IconImage {

        private byte[] imageBytes = null;
        private String imageType = null;

        public IconImage(String imageType, byte[] imageBytes) {
            this.imageBytes = imageBytes;
            this.imageType = imageType;
        }

        /**
         * @return the imageBytes
         */
        public byte[] getImageBytes() {
            return imageBytes;
        }

        /**
         * @return the imageType
         */
        public String getImageType() {
            return imageType;
        }
    }
}
