package io.openliberty.security.oidcclientcore.exceptions;

public class NoStateValueStoredException extends AuthenticationResponseException {

    private static final long serialVersionUID = 1L;

    public NoStateValueStoredException(String clientId, String nlsMessage) {
        super(clientId, nlsMessage); // TODO: Create NLS message.
    }

}
