/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.service.location.internal;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.wsspi.kernel.service.location.MalformedLocationException;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.location.WsResource.Type;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

public final class ResourceUtils {
    public static final List<String> EMPTY_STRING_LIST = Collections.unmodifiableList(new ArrayList<String>());

    private ResourceUtils() {
        throw new AssertionError("This class is not instantiable");
    }

    /**
     * @param parent
     * @param child
     * @return
     */
    static boolean isFileAChild(File parent, File child) {
        String pNormalized = PathUtils.normalize(parent.getAbsolutePath());
        String cNormalized = PathUtils.normalize(child.getAbsolutePath());

        // If the child's normalized path length is <= to the the parent's,
        // it isn't a child
        if (cNormalized.length() <= pNormalized.length())
            return false;

        if (cNormalized.startsWith(pNormalized))
            return true;

        return false;
    }

    /**
     * @param parent
     * @param child
     * @return
     * @throws MalformedLocationException
     *             if path is not relative (is an absolute path, a file URI, or
     *             starts with a symbol)
     */
    static InternalWsResource getChildResource(InternalWsResource parent, String child) {
        // Return null if the name is null
        if (child == null || parent == null)
            return null;

        child = PathUtils.normalizeDescendentPath(child);
        if (child.length() == 0)
            return null;

        // look for slashes in all but last character
        for (int i = 0; i < child.length() - 1; i++) {
            if (child.charAt(i) == File.separatorChar || child.charAt(i) == '/')
                throw new MalformedLocationException("Child name can not contain path separator characters");
        }

        File f = new File(parent.getNormalizedPath(), child);

        if (f.exists()) {
            String repPath = parent.getRawRepositoryPath();
            if (repPath != null) {
                if (repPath.endsWith("/"))
                    repPath += child;
                else
                    repPath += '/' + child;
            }

            return LocalFileResource.newResourceFromResource(parent.getNormalizedPath() + child, repPath, parent);
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    static Iterator<String> getChildren(InternalWsResource parent, File wrappedFile) {
        return new ResourceIterators.ChildIterator(wrappedFile, wrappedFile.list());
    }

    /**
     * {@inheritDoc}
     */
    static Iterator<String> getChildren(InternalWsResource parent, File wrappedFile, final String regex) {
        String[] listFiles = wrappedFile.list(new FilenameFilter()
        {
            Pattern p = Pattern.compile(regex);

            @Override
            public boolean accept(File dir, String name)
        {
            // Add a trailing slash to the child name if it's a directory
            // -- this allows filtering for files/directories in the regex
            // based on the presence of the trailing /
            File f = new File(dir, name);
            if (f.isDirectory())
                name += '/';

            Matcher m = p.matcher(name);
            return m.matches();
        }
        });

        return new ResourceIterators.ChildIterator(wrappedFile, listFiles);
    }

    /**
     * @param child
     *            Local resource to obtain parent of
     * @param wrappedFile
     *            The file wrapped by that resource
     * @return WsResource wrapping the parent of the specified local file
     *         resource,
     *         or null if the parent is outside/above the root
     */
    static InternalWsResource getParentResource(InternalWsResource child, SymbolicRootResource root) {
        String parentPath = new File(child.getNormalizedPath()).getParent();

        if (parentPath == null)
            return null;

        // parent of the child's normalized path will already be normalized
        // on windows, the parent path will be back to backslashes.. *sigh*
        parentPath = PathUtils.slashify(parentPath + '/');

        if (root.getNormalizedPath().equals(parentPath))
            return root;

        // Make sure the file is in our tree
        if (root.contains(parentPath)) {
            // If a child repository path was set, use it to calculate the parent's
            // path: ${A}/child/ or ${A}/child --> ${A}/
            String repositoryPath = child.getRawRepositoryPath();
            if (repositoryPath != null) {
                int last = child.isType(Type.DIRECTORY) ? repositoryPath.length() - 2 : repositoryPath.length() - 1;
                repositoryPath = repositoryPath.substring(0, repositoryPath.lastIndexOf('/', last) + 1);
            }

            return LocalFileResource.newResourceFromResource(parentPath, repositoryPath, child);
        }

        return null;
    }

    /**
     * @param base
     * @param relativePath
     * @param root
     * @throws WsLocationAdminException
     *             if the relativePath attempts to navigate outside/beyond the
     *             resource root.
     * @see URI#URI(String)
     * @see URI#URI(String, String, String, String)
     */
    static WsResource getRelativeResource(InternalWsResource base, String relativePath) {
        if (base == null)
            throw new NullPointerException("Resolving relative resource requires a base");

        if (relativePath == null)
            return null;

        String path = PathUtils.normalize(relativePath);

        // If this is not a relative path, push back through resolve
        if (PathUtils.pathIsAbsolute(path))
            return WsLocationAdminImpl.getInstance().resolveResource(relativePath);

        String newPath = resolveRelativeUsingFile(base.getNormalizedPath(), relativePath);

        SymbolicRootResource root = base.getSymbolicRoot();

        if (root.getNormalizedPath().equals(newPath))
            return root;

        if (root.contains(newPath))
            return LocalFileResource.newResourceFromResource(newPath, null, base);

        // will throw for unreachable location
        SymbolicRootResource newRoot = SymbolRegistry.getRegistry().findRoot(newPath);

        return LocalFileResource.newResource(newPath, null, newRoot);
    }

    /**
     * @param normalizedRoot
     * @param relativePath
     * @return
     */
    static String resolveRelativeUsingFile(String normalizedRoot, String relativePath) {
        if (normalizedRoot == null)
            return null;

        int n_length = normalizedRoot.length();

        if (n_length == 0)
            return relativePath;

        // Resolving relative to a directory: potentially a child
        if (normalizedRoot.charAt(n_length - 1) == '/')
            return PathUtils.normalize(normalizedRoot + relativePath);

        // Otherwise, we're looking for something relative to a file:
        // strip off the file portion (but preserve the slash of the parent)
        int index = normalizedRoot.lastIndexOf('/');
        String root = normalizedRoot.substring(0, index + 1);

        return PathUtils.normalize(root + relativePath);
    }

    /**
     * @param descendant
     * @return
     */
    static String createRepositoryURI(InternalWsResource descendant, SymbolicRootResource root) {
        // LocalFileResource paths are normalized
        String normalizedPath = descendant.getNormalizedPath();
        String normalizedRoot = root.getNormalizedPath();
        String rootSymbolicPath = root.toRepositoryPath();

        if (root.contains(normalizedPath)) {
            String substr = normalizedPath.substring(normalizedRoot.length());

            // Repository ID: ${symbol}/remainder/of/path
            StringBuilder uri = new StringBuilder(rootSymbolicPath.length() + substr.length());
            uri.append(rootSymbolicPath).append(substr);

            return uri.toString();
        }

        return descendant.toExternalURI().toString();
    }
}
