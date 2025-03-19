/*******************************************************************************
 * Copyright (c) 2011, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.config.internal;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.service.util.ServiceRegistrationModifier;
import com.ibm.ws.kernel.service.util.ServiceRegistrationModifier.ServicePropertySupplier;
import com.ibm.wsspi.config.Fileset;
import com.ibm.wsspi.config.FilesetChangeListener;
import com.ibm.wsspi.config.internal.ConfigTypeConstants.FilesetAttribute;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

/* disable-formatter */
@Component(service = {},
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true,
           configurationPid = "com.ibm.ws.kernel.metatype.helper.fileset",
           property = {
                        "service.vendor=IBM",
                        "monitor.filter=files",
                        "monitor.recurse:Boolean=false" })
public class FilesetImpl implements Fileset, FileMonitor, ServicePropertySupplier {
    private static final TraceComponent tc = Tr.register(FilesetImpl.class);
    private static final boolean DEFAULT_CASE_SENSITIVITY = true; // default to true
    private static final String DEFAULT_INCLUDES = "*"; // default to * (i.e. all files in
    // current dir)
    private static final String DEFAULT_EXCLUDES = ""; // default to empty string (i.e. no
    // exclusions)
    static final Long MONITOR_OFF = Long.valueOf(0); //constant for monitor interval of 0
    private static final Long DEFAULT_MONITOR = MONITOR_OFF; //default to DO NOT MONITOR

    enum FilterType {
        INCLUDE, EXCLUDE
    };

    private volatile ComponentContext context;

    private String pid;
    private volatile ServiceRegistration<Fileset> filesetRegistration = null;

    private volatile String resolvedBasePath;

    /** Element attributes follow */
    // Required, the base dir for the fileset
    private volatile String basedir;

    // Optional attributes, init to the default values
    private volatile boolean caseSensitive = DEFAULT_CASE_SENSITIVITY;
    private volatile String includesAttribute = DEFAULT_INCLUDES;
    private volatile String excludesAttribute = DEFAULT_EXCLUDES;

    /**
     * The properties that need to be registered on the FileMonitor
     * MONITOR_FILTER
     * MONITOR_RECURSE
     * MONITOR_DIRECTORIES
     * MONITOR_INTERVAL
     * fileset = the name of the fileset
     */
    private final Map<String, Object> fileMonitorProps = new Hashtable<String, Object>(5);

    /** The set of files matching the filters */
    private Collection<File> fileset = Collections.emptySet();

    /** Flag to indicate whether the filters are current */
    private volatile boolean isCurrent = false;

    /** Flag to indicate if the cached set should be returned */
    private volatile boolean returnCached = false;

    /** The fileset filter */
    private final FilesetNameFilter filter = new FilesetNameFilter();

    private final List<FilesetChangeListener> listeners = new ArrayList<FilesetChangeListener>();

    private boolean listenersNotified;

    private final ServiceRegistrationModifier<FileMonitor> fileMonitorRegistration = new ServiceRegistrationModifier<>(FileMonitor.class, this, this);

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> props) {
        this.context = context;

        //set the Fileset id
        this.pid = (String) props.get(Constants.SERVICE_PID);

        //set the configured properties as required
        modified(props);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) throws IOException {
        if (filesetRegistration != null) {
            filesetRegistration.unregister();
            filesetRegistration = null;
        }
        fileMonitorRegistration.unregister();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Hashtable<String, Object> getServiceProperties() {
        return (Hashtable<String, Object>) fileMonitorProps;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void setListener(FilesetChangeListener listener) {
        boolean listenersNotified;
        synchronized (listeners) {
            listeners.add(listener);
            listenersNotified = this.listenersNotified;
        }
        if (listenersNotified) {
            listener.filesetNotification(pid, this);
        }
    }

    protected void unsetListener(FilesetChangeListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    @Reference
    protected void setLocationAdmin(WsLocationAdmin locationAdmin) {
        this.resolvedBasePath = locationAdmin.resolveString(WsLocationConstants.SYMBOL_SERVER_CONFIG_DIR);
    }

    /**
     * Returns the directory for the fileset.
     *
     * @return the directory for the fileset.
     */
    @Override
    @Trivial
    public String getDir() {
        return basedir;
    }

    /**
     * @param dir
     *                the base dir to set
     */
    private void setDir(String basedir) {
        // clean up the basedir to use only / for ease of regex
        // Also covert path to be absolute
        this.basedir = PathUtils.slashify(basedir);
        if (!PathUtils.pathIsAbsolute(this.basedir))
            this.basedir = resolvedBasePath + this.basedir;
        this.basedir = PathUtils.normalize(this.basedir);

        // update the file monitor with the new basedir
        this.fileMonitorProps.put(FileMonitor.MONITOR_DIRECTORIES, Arrays.asList(new String[] { this.basedir }));
    }

    /**
     * @param caseSensitive
     *                          boolean to set whether filters are case sensitive
     */
    private void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    /**
     * @param includesAttribute
     *                              the includesAttribute to set
     */
    private void setIncludesAttribute(String includesAttribute) {
        this.includesAttribute = includesAttribute;
    }

    /**
     * @param excludesAttribute
     *                              the excludesAttribute to set
     */
    private void setExcludesAttribute(String excludesAttribute) {
        this.excludesAttribute = excludesAttribute;
    }

    /**
     * Set the monitor interval attribute and return a boolean indicating
     * if the {@link FileMonitor} should be re-registered.
     *
     * @param monitorAttributeObject the monitor interval attribute to set.
     */
    private void setMonitorAttribute(Long monitorInterval) {
        if (MONITOR_OFF.equals(monitorInterval)) {
            this.fileMonitorProps.remove(FileMonitor.MONITOR_INTERVAL);
        } else {
            this.fileMonitorProps.put(FileMonitor.MONITOR_INTERVAL, monitorInterval);
        }
    }

    @Modified
    protected synchronized void modified(Map<String, Object> props) {
        // if the attributes are being set or reset then the cached files are
        // no longer current
        fileMonitorProps.putAll(props);
        this.isCurrent = false;

        for (FilesetAttribute attr : FilesetAttribute.values()) {
            Object o = props.get(attr.toString());
            switch (attr) {
                case dir:
                    setDir((String) o);
                    break;
                case caseSensitive:
                    Boolean b = (o != null) ? (Boolean) o : DEFAULT_CASE_SENSITIVITY;
                    setCaseSensitive(b);
                    break;
                case includes: {
                    String s = (o != null) ? (String) o : DEFAULT_INCLUDES;
                    setIncludesAttribute(s);
                }
                    break;
                case excludes: {
                    String s = (o != null) ? (String) o : DEFAULT_EXCLUDES;
                    setExcludesAttribute(s);
                }
                    break;
                case scanInterval:
                    Long scan = (o != null) ? (Long) o : DEFAULT_MONITOR;
                    setMonitorAttribute(scan);
                    break;
            }
        }
        fileMonitorRegistration.registerOrUpdate(context.getBundleContext());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Trivial
    public synchronized Collection<File> getFileset() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Retrieving fileSet", pid);

        // check if the filters are current
        // and re-evaluate if necessary
        if (!!!this.isCurrent) {
            evaluateFilters();
        }

        // check if the cached set can be returned
        // and if not apply the filters to get a new set
        if (!!!this.returnCached) {
            applyFilters();
        }

        //return a copied Collection of the fileset so that the caller can modify without
        //destroying the real fileset
        Collection<File> filesetToReturn = new ArrayList<File>(this.fileset.size());
        filesetToReturn.addAll(this.fileset);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "returning fileset: " + filesetToReturn, pid);
        return filesetToReturn;
    }

    private void evaluateFilters() {
        // clear the filters before evaluating them again
        filter.clearFilters();

        // if the attributes were set then add them to the filters to evaluate
        if (includesAttribute != null && !!!(includesAttribute.length() == 0))
            filter.addFilter(FilterType.INCLUDE, includesAttribute);
        if (excludesAttribute != null && !!!(excludesAttribute.length() == 0))
            filter.addFilter(FilterType.EXCLUDE, excludesAttribute);

        // mark the filters as current
        this.isCurrent = true;

        // if the filters have been re-evaluated we must not return the cached
        // set
        this.returnCached = false;
    }

    private void applyFilters() {
        Collection<File> matchingFiles = new ArrayList<File>(filesToCheckForMatches.size());

        for (File f : filesToCheckForMatches) {
            if (filter.accept(f))
                matchingFiles.add(f);
        }

        // set the fileset
        this.fileset = matchingFiles;

        // we can now return the cached set until something changes
        this.returnCached = true;
    }

    private final class FilesetNameFilter implements FilenameFilter, FileFilter {

        private final Collection<Pattern> includes = new ArrayList<Pattern>();
        private final Collection<Pattern> excludes = new ArrayList<Pattern>();

        /** {@inheritDoc} */
        @Override
        public boolean accept(File pathname) {
            return accept(pathname.getPath());
        }

        @Override
        public boolean accept(File dir, String name) {
            // get the full file name
            String fullname = dir.getPath() + "/" + name;
            return accept(fullname);
        }

        @Trivial
        private void getHumanReadableSummary(StringBuilder sb) {

            if (!includes.isEmpty()) {
                sb.append("With include filters: ");
                includes.forEach(p -> sb.append(System.lineSeparator()).append("\t").append(p));
                sb.append(System.lineSeparator());
            }

            if (!excludes.isEmpty()) {
                sb.append("With exclude filters: ");
                excludes.forEach(p -> sb.append(System.lineSeparator()).append("\t").append(p));
                sb.append(System.lineSeparator());
            }
        }

        private boolean accept(String fullname) {
            boolean accept = false;

            // get the name using only "/" to make regex easier later
            fullname = fullname.replace('\\', '/');

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "Filtering file name", fullname);

            // a default include filter of * should be included by the metatype

            // apply the includes to the list
            for (Pattern filter : includes) {
                Matcher m = filter.matcher(fullname);
                if (m.matches()) {
                    // if we matched any include filter we don't
                    // need to test the rest of the filters
                    accept = true;
                    break;
                }
            }

            // then the excludes
            for (Pattern filter : excludes) {
                Matcher m = filter.matcher(fullname);
                if (m.matches()) {
                    // if we matched any exclude filter we don't
                    // need to test the rest of the filters
                    accept = false;
                    break;
                }
            }

            return accept;
        }

        private Collection<Pattern> getNamePatternFromFilter(String inputFilter) {

            // clean up the input filter to only use "/"
            inputFilter = inputFilter.replace('\\', '/');

            // filter strings can be "," or " " separated
            // split the string on 0 or more whitespace followed by a single ","
            // and then 0 or more white space characters
            // OR
            // one or more whitespace characters
            String[] filters = inputFilter.split("\\s*,\\s*|\\s+");

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "Filter strings", Arrays.toString(filters));

            // create a Set for the regex patterns based on the supplied filters
            Set<Pattern> patterns = new HashSet<Pattern>(filters.length);

            for (String filter : filters) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "Parsing filter string into pattern", filter);
                if (filter.length() == 0) {
                    // there was nothing in the filter
                    // continue the loop without incrementing
                    // as we don't need a Pattern for this
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "Empty filter string encountered, will be ignored");
                    continue;
                } else {
                    // the filter should always apply from the
                    // specified basedir so we should prepend it
                    // removing any ./ if necessary
                    if (filter.startsWith("./")) {
                        filter = filter.substring(2);
                    }
                    filter = basedir + (basedir.endsWith("/") ? "" : "/") + filter;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "Prepended filter with basedir resulting in filter string", filter);

                    // now quote the filter to escape any regex chars
                    filter = Pattern.quote(filter);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "Filter string following regex quotation", filter);
                }

                // if there are any "*" wildcards
                // then unquote from the regex and replace
                // with [^/]+ so we don't cross path boundaries
                // and match at least 1 character
                filter = filter.replaceAll("\\*", "\\\\E[^/]+\\\\Q");
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "Pattern string to compile after replacing * wildcards is", filter);

                // if there are any ** wildcards then they have been replaced by
                // the regex sequence \E[^/]+\Q (twice - one for each *)
                // so here we search for \E[^/]+\Q\E[^/]+\Q and replace it with
                // a single unquoted instance of \E.*\Q (i.e. match zero or more
                // of
                // any character) so that ** can cross path boundaries
                filter = filter.replaceAll("\\\\E\\[\\^/\\]\\+\\\\Q\\\\E\\[\\^/\\]\\+\\\\Q", "\\\\E.*\\\\Q");
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "Pattern string to compile after replacing ** wildcards is", filter);

                // default to no flags
                int patternFlags = 0;
                if (!!!caseSensitive) {
                    // if the match should not be case sensitive add those flags
                    patternFlags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "Case insensitive matching was specified, applying pattern flags",
                                 patternFlags);
                }
                patterns.add(Pattern.compile(filter, patternFlags));
            }
            return patterns;
        }

        public void addFilter(FilterType f, String inputFilter) {
            Collection<Pattern> patterns = getNamePatternFromFilter(inputFilter);
            switch (f) {
                case INCLUDE:
                    includes.addAll(patterns);
                    break;
                case EXCLUDE:
                    excludes.addAll(patterns);
                    break;
            }
        }

        public void clearFilters() {
            this.includes.clear();
            this.excludes.clear();
        }
    }

    // init to an empty set to check
    private final Collection<File> filesToCheckForMatches = new ArrayList<File>();

    @Override
    public void onBaseline(Collection<File> baseline) {
        // after the first scan completes set the baseline (clearing first in case init is called again which might happen if we change the dir)
        filesToCheckForMatches.clear();
        filesToCheckForMatches.addAll(baseline);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "FileMonitor init completed for fileset: baseline " + baseline, pid);
        returnCached = false;

        filesetRegistration = context.getBundleContext().registerService(Fileset.class, this, getServiceProperties());

        notifyListeners();
    }

    @Override
    public void onChange(Collection<File> createdFiles, Collection<File> modifiedFiles,
                         Collection<File> deletedFiles) {

        //if there were any changes then we set returnCached to false
        //since we need to re-evaluate the fileset the next time it is called for

        // adjust the Collection we are checking based on any changes detected
        // during the scan
        if (!!!deletedFiles.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "FileMonitor scan detected files to remove from fileset", pid, deletedFiles);
            filesToCheckForMatches.removeAll(deletedFiles);
            this.returnCached = false;
        }
        if (!!!createdFiles.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "FileMonitor scan detected files to add to fileset", pid, createdFiles);
            filesToCheckForMatches.addAll(createdFiles);
            this.returnCached = false;
        }

        // if changes were made to the monitored files then notify the listeners
        if (!!!(deletedFiles.isEmpty() && createdFiles.isEmpty() && modifiedFiles.isEmpty()))
            notifyListeners();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "FileMonitor scan completed for fileset", pid);
    }

    private void notifyListeners() {
        List<FilesetChangeListener> copy;
        synchronized (listeners) {
            copy = new ArrayList<FilesetChangeListener>(listeners);
            this.listenersNotified = true;
        }
        for (FilesetChangeListener listener : copy) {
            if (listener != null)
                listener.filesetNotification(pid, this);
        }
    }

    @Override
    @FFDCIgnore(Exception.class)
    @Trivial
    public String toString() {
        StringBuilder sb = new StringBuilder();

        try {
            sb.append("FileSet for directory: " + basedir);
            sb.append(System.lineSeparator());

            filter.getHumanReadableSummary(sb); //provides its own line break

            // append first element
            fileset.stream().limit(1).map(File::getPath).forEach(sb::append);
            // append subsequent elements with delimiters
            fileset.stream().skip(1).map(File::getPath).forEach(p -> sb.append(", ").append(p));

        } catch (Exception e) {
            String originalToString = super.toString();
            sb.append(System.lineSeparator());
            sb.append("Caught an exception calling to String on " + originalToString + " exception was: " + e);
        }

        return sb.toString();
    }
}
