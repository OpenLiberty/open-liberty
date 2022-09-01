package io.openliberty.security.oidcclientcore.exceptions;

public class AuthenticationErrorResponseException extends AuthenticationResponseException {

    private static final long serialVersionUID = 1L;

    public AuthenticationErrorResponseException(String clientId, String nlsMessage) {
        super(clientId, nlsMessage); // TODO: Create NLS message.
    }

}
