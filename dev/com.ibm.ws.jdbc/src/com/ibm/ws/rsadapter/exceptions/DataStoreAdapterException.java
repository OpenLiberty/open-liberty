/*******************************************************************************
 * Copyright (c) 2001, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter.exceptions;

import java.security.AccessController;
import java.sql.*; 
import java.util.Map; 

import javax.resource.ResourceException;

import org.osgi.framework.FrameworkUtil;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jdbc.osgi.JDBCRuntimeVersion;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.ws.rsadapter.AdapterUtil;

public class DataStoreAdapterException extends ResourceException {

    private static final long serialVersionUID = -1282552127378991160L; 

    private static final TraceComponent tc = Tr.register(DataStoreAdapterException.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);
    final static SecureAction priv = AccessController.doPrivileged(SecureAction.get());

    // whether the exception has already been mapped
    private boolean beenMapped = false;

    /**
     * Constructor with localization message information. The resource Key is assumed to not
     * have any positional parameters other than a location to print the exception.
     * 
     * @param resourceKey java.lang.String The key in the resource bundle that
     *            will be used to select the specific message that is retrieved for
     *            getMessage().
     * @param exception java.lang.Throwable The exception that is to be chained.
     * @param logClass Class of originator of the exception. Typically use <b>this.getClass()</b>
     *            to provide this parameter.
     * @param formatArguments
     *            java.lang.Object[] The arguments to be passed to
     *            the MessageFormat class to act as replacement variables in the message
     *            that is retrieved from the resource bundle. Valid types are those supported
     *            by MessageFormat.
     */
    public DataStoreAdapterException(String resourceKey, Throwable exception, Class<?> logClass, Object... formatArguments) {
        super(formatMessage(resourceKey, exception, formatArguments));

        if (exception == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "DSA_ERROR");
        } else {
            if (exception instanceof SQLException)
            {
                // Since we discovered this is a SQLEXception, Let the user know the error codes in the log
                // I think Instanceof is a beter fit here because most of the exceptions will not be SQLExceptions
                // so there is no need to try a casting right away.
                SQLException sqlex = (SQLException) exception;
                SQLException sqlexChained = sqlex.getNextException(); 
                Throwable cause = sqlex.getCause(); 
                // We do not need to descend the chain because Tr will do it for us.
                // So just send in the actual exception.

                String _sqlState = sqlex.getSQLState();
                int _errorCode = sqlex.getErrorCode();

                // since the WAS RAS doesn't print the sqlstate and errocode in the sqlException, i will make sure
                // that the sqlexception message contains both state and code.
                String _message = AdapterUtil.getNLSMessage("SQL_STATE_ERROR_CODE", new Object[] { _sqlState, _errorCode });

                // The ordering of exceptions is important. More generic exception classes
                // should be listed last.

                if (exception instanceof BatchUpdateException)
                {
                    // JDBCRuntimeVersion injected by declarative services is not accessible from here.
                    // This is an exception path, and should not be performance critical, so just grab it from the service registry again.
                    JDBCRuntimeVersion jdbcRuntime = priv.getService(priv.getBundleContext(FrameworkUtil.getBundle(getClass())), JDBCRuntimeVersion.class);
                    exception = jdbcRuntime.newBatchUpdateException((BatchUpdateException) exception, sqlex.getMessage() + " " + _message);
                }
                else if (exception instanceof DataTruncation)
                {
                    DataTruncation dataX = (DataTruncation) exception;

                    // DataTruncation constructors do not allow the message to be supplied,
                    // so it is necessary to override the getMessage method.

                    exception = new WSDataTruncation(
                                                     dataX.getMessage() + " " + _message,
                                                     dataX.getIndex(),
                                                     dataX.getParameter(),
                                                     dataX.getRead(),
                                                     dataX.getDataSize(),
                                                     dataX.getTransferSize());

                    ((DataTruncation) exception).setNextWarning(dataX.getNextWarning());
                }
                else if (exception instanceof SQLClientInfoException)
                {
                    Map<String, ClientInfoStatus> failedProps = ((SQLClientInfoException) exception).getFailedProperties();
                    exception = new SQLClientInfoException(sqlex.getMessage() + " " + _message, _sqlState, _errorCode, failedProps); 
                }
                else if (exception instanceof SQLDataException)
                    exception = new SQLDataException(sqlex.getMessage() + " " + _message, _sqlState, _errorCode); 
                else if (exception instanceof SQLFeatureNotSupportedException)
                    exception = new SQLFeatureNotSupportedException(sqlex.getMessage() + " " + _message, _sqlState, _errorCode); 
                else if (exception instanceof SQLIntegrityConstraintViolationException)
                    exception = new SQLIntegrityConstraintViolationException(sqlex.getMessage() + " " + _message, _sqlState, _errorCode); 
                else if (exception instanceof SQLInvalidAuthorizationSpecException)
                    exception = new SQLInvalidAuthorizationSpecException(sqlex.getMessage() + " " + _message, _sqlState, _errorCode); 
                else if (exception instanceof SQLNonTransientException)
                    exception = new SQLNonTransientException(sqlex.getMessage() + " " + _message, _sqlState, _errorCode); 
                else if (exception instanceof SQLRecoverableException)
                    exception = new SQLRecoverableException(sqlex.getMessage() + " " + _message, _sqlState, _errorCode); 
                else if (exception instanceof SQLSyntaxErrorException)
                    exception = new SQLSyntaxErrorException(sqlex.getMessage() + " " + _message, _sqlState, _errorCode); 
                else if (exception instanceof SQLTimeoutException)
                    exception = new SQLTimeoutException(sqlex.getMessage() + " " + _message, _sqlState, _errorCode); 
                else if (exception instanceof SQLTransactionRollbackException)
                    exception = new SQLTransactionRollbackException(sqlex.getMessage() + " " + _message, _sqlState, _errorCode); 
                else if (exception instanceof SQLTransientConnectionException)
                    exception = new SQLTransientConnectionException(sqlex.getMessage() + " " + _message, _sqlState, _errorCode); 
                else if (exception instanceof SQLNonTransientConnectionException)
                    exception = new SQLNonTransientConnectionException(sqlex.getMessage() + " " + _message, _sqlState, _errorCode); 
                else if (exception instanceof SQLTransientException)
                    exception = new SQLTransientException(sqlex.getMessage() + " " + _message, _sqlState, _errorCode); 
                else
                    exception = new SQLException(sqlex.getMessage() + " " + _message, _sqlState, _errorCode); 
                //put the stack trace in the new exception

                exception.setStackTrace(sqlex.getStackTrace()); 
                if (cause != null) 
                    exception.initCause(cause); 

                if (sqlexChained != null) 
                    ((SQLException) exception).setNextException(sqlexChained); 
            }

            initCause(exception);

            // Do not log this exception. caller is responsible for logging it.
            // change error message to exit message.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { 
                Tr.debug(tc, "Exception", exception);
            }
        }
    }

    /**
     * barghout
     * Whether the exception has been translated. Exception translation is done to send back
     * a uniform set of exception's even though a very diverse set is reported by the different
     * databases
     * 
     * @return boolean = <b>true</b> if the contained exception has already been mapped.
     */
    public boolean beenMapped() {
        return beenMapped;
    }

    /**
     * Utility method that preserves existing behavior of appending SQL State/error code to a translated message.
     */
    private static final String formatMessage(String resourceKey, Throwable exception, Object... formatArguments) {
        String message = Tr.formatMessage(tc, resourceKey, formatArguments);
        if (exception instanceof SQLException) {
            SQLException sqlX = (SQLException) exception;
            StringBuilder st = new StringBuilder(message.length() + 40);
            st.append(message).append(" with SQL State : ").append(sqlX.getSQLState()).append(" SQL Code : ").append(sqlX.getErrorCode());
            message = st.toString();   
        }
        return message;
    }

    /**
     * barghout
     * sets the flag that indicates if the exception has been mapped
     * 
     */
    public void setBeenMapped() {
        beenMapped = true; 
    }

    /**
     * Retrieve the exception that was saved away
     * 
     * @return Exception
     */
    @Override
    public Exception getLinkedException() {
        //return (Exception) getCause();
        Throwable t = getCause();
        while (t != null) {
            if (t instanceof Exception) {
                return (Exception) t;
            } else {
                t = t.getCause();
            }
        }
        return null;
    }
}
