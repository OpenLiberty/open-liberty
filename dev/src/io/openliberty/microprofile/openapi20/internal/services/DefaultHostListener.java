package io.openliberty.microprofile.openapi20.internal.services;

import io.openliberty.microprofile.openapi20.internal.utils.ServerInfo;

public interface DefaultHostListener {

    /**
     * The getDefaultHostServerInfo method returns the ServerInfo object for the default_host virtual host.
     *
     * @return ServerInfo
     * The ServerInfo object for the default_host virtual host.
     */
    ServerInfo getDefaultHostServerInfo();

}