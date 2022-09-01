package io.openliberty.security.oidcclientcore.exceptions;

public class RequestStateDoesNotMatchStoredStateException extends AuthenticationResponseException {

    private static final long serialVersionUID = 1L;

    public RequestStateDoesNotMatchStoredStateException(String clientId, String nlsMessage) {
        super(clientId, nlsMessage); // TODO: Create NLS message.
    }

}
