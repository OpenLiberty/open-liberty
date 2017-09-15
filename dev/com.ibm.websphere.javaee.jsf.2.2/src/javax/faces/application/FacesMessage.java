/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package javax.faces.application;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *see Javadoc of <a href="http://java.sun.com/javaee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 *<p>
 * <code>FacesMessage</code> represents a single validation (or other) message, which is typically associated with a
 * particular component in the view. A {@link FacesMessage} instance may be created based on a specific messageId. The
 * specification defines the set of messageIds for which there must be {@link FacesMessage} instances.
 * </p>
 * 
 *<ui>The implementation must take the following steps when creating FacesMessage instances given a messageId: <li>Call
 * {@link Application#getMessageBundle()}. If <code>non-null</code>, locate the named <code>ResourceBundle</code>, using
 * the <code>Locale</code> from the current {@linkUIViewRoot} and see if it has a value for the argument
 * <code>messageId</code>. If it does, treat the value as the <code>summary</code> of the {@link FacesMessage}. If it
 * does not, or if {@link Application#getMessageBundle()} returned null, look in the ResourceBundle named by the value
 * of the constant {@link #FACES_MESSAGES} and see if it has a value for the argument messageId. If it does, treat the
 * value as the summary of the <code>FacesMessage</code>. If it does not, there is no initialization information for the
 * <code>FacesMessage</code> instance.</li> <li>In all cases, if a <code>ResourceBundle</code> hit is found for the
 * <code>{messageId}</code>, look for further hits under the key <code>{messageId}_detail</code>. Use this value, if
 * present, as the <code>detail</code> for the returned <code>FacesMessage</code>.</li> <li>Make sure to perform any
 * parameter substitution required for the <code>summary</code> and <code>detail</code> of the <code>FacesMessage</code>
 * .</li> </ui>
 * 
 */
public class FacesMessage implements Serializable
{
    private static final long serialVersionUID = 4851488727794169661L;

    /**
     * <code>ResourceBundle</code> identifier for messages whose message identifiers are defined in the JavaServer Faces
     * specification.
     */
    public static final String FACES_MESSAGES = "javax.faces.Messages";

    /**
     * Message severity level indicating an informational message rather than an error.
     */
    public static final FacesMessage.Severity SEVERITY_INFO = new Severity("INFO", 0);

    /**
     * Message severity level indicating that an error might have occurred.
     */
    public static final FacesMessage.Severity SEVERITY_WARN = new Severity("WARN", 1);

    /**
     * Message severity level indicating that an error has occurred.
     */
    public static final FacesMessage.Severity SEVERITY_ERROR = new Severity("ERROR", 2);

    /**
     * Message severity level indicating that a serious error has occurred.
     */
    public static final FacesMessage.Severity SEVERITY_FATAL = new Severity("FATAL", 3);

    /**
     * Immutable <code>Lis</code> of valid {@link FacesMessage.Severity}instances, in ascending order of their ordinal
     * value.
     */
    public static final List VALUES;

    /**
     * Immutable <code>Map</code> of valid {@link FacesMessage.Severity}instances, keyed by name.
     */
    public static final Map VALUES_MAP;

    static
    {
        Map<String, FacesMessage.Severity> map = new HashMap<String, Severity>(7);
        map.put(SEVERITY_INFO.toString(), SEVERITY_INFO);
        map.put(SEVERITY_WARN.toString(), SEVERITY_WARN);
        map.put(SEVERITY_ERROR.toString(), SEVERITY_ERROR);
        map.put(SEVERITY_FATAL.toString(), SEVERITY_FATAL);
        VALUES_MAP = Collections.unmodifiableMap(map);

        List<FacesMessage.Severity> severityList = new ArrayList<Severity>(map.values());
        Collections.sort(severityList); // the JSF spec requires it to be sorted
        VALUES = Collections.unmodifiableList(severityList);
    }

    private transient FacesMessage.Severity _severity;  // transient, b/c FacesMessage.Severity is not Serializable
    private String _summary;
    private String _detail;
    private boolean _rendered;

    /**
     *Construct a new {@link FacesMessage} with no initial values. The severity is set to Severity.INFO.
     */
    public FacesMessage()
    {
        _severity = SEVERITY_INFO;
        _rendered = false;
    }

    /**
     * Construct a new {@link FacesMessage} with just a summary. The detail is null, the severity is set to
     * <code>Severity.INFO</code>.
     */
    public FacesMessage(String summary)
    {
        _summary = summary;
        _severity = SEVERITY_INFO;
        _rendered = false;
    }

    /**
     * Construct a new {@link FacesMessage} with the specified initial values. The severity is set to Severity.INFO.
     * 
     * @param summary
     *            - Localized summary message text
     * @param detail
     *            - Localized detail message text
     */
    public FacesMessage(String summary, String detail)
    {
        _summary = summary;
        _detail = detail;
        _severity = SEVERITY_INFO;
        _rendered = false;
    }

    /**
     * Construct a new {@link FacesMessage}with the specified initial values.
     * 
     * @param severity
     *            - the severity
     * @param summary
     *            - Localized summary message text
     * @param detail
     *            - Localized detail message text
     */
    public FacesMessage(FacesMessage.Severity severity, String summary, String detail)
    {
        if (severity == null)
        {
            throw new NullPointerException("severity");
        }
        _severity = severity;
        _summary = summary;
        _detail = detail;
        _rendered = false;
    }

    /**
     * 
     * @return
     */
    public FacesMessage.Severity getSeverity()
    {
        return _severity;
    }

    /**
     * Return the severity level.
     */
    public void setSeverity(FacesMessage.Severity severity)
    {
        if (severity == null)
        {
            throw new NullPointerException("severity");
        }
        _severity = severity;
    }

    /**
     * Return the localized summary text.
     */
    public String getSummary()
    {
        return _summary;
    }

    /**
     * Set the localized summary text.
     * 
     * @param summary
     *            - The new localized summary text
     */
    public void setSummary(String summary)
    {
        _summary = summary;
    }

    /**
     * 
     * @return
     */
    public String getDetail()
    {
        if (_detail == null)
        {
            // Javadoc:
            // If no localized detail text has been defined for this message, return the localized summary text instead
            return _summary;
        }
        return _detail;
    }

    /**
     * Set the localized detail text.
     * 
     * @param detail
     *            - The new localized detail text
     */
    public void setDetail(String detail)
    {
        _detail = detail;
    }

    public boolean isRendered()
    {
        return _rendered;
    }

    public void rendered()
    {
        this._rendered = true;
    }

    private void writeObject(ObjectOutputStream out) throws IOException
    {
        out.defaultWriteObject();  // write summary, detail, rendered
        out.writeInt(_severity._ordinal);  // FacesMessage.Severity is not Serializable, write ordinal only
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();  // read summary, detail, rendered

        // FacesMessage.Severity is not Serializable, read ordinal and get related FacesMessage.Severity
        int severityOrdinal = in.readInt();
        _severity = (Severity) VALUES.get(severityOrdinal);
    }

    public static class Severity implements Comparable
    {
        private String _name;
        private int _ordinal;

        private Severity(String name, int ordinal)
        {
            _name = name;
            _ordinal = ordinal;
        }

        public int getOrdinal()
        {
            return _ordinal;
        }

        @Override
        public String toString()
        {
            return _name;
        }

        public int compareTo(Object o)
        {
            return getOrdinal() - ((Severity)o).getOrdinal();
        }
    }

}
