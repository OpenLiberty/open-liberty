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
define({
    ERROR : "Hiba",
    ERROR_STATUS : "{0} Hiba: {1}", //responseStatusCode, response.text
    ERROR_URL_REQUEST_NO_STATUS : "Hiba történt a következő kérésekor: {0}.", // url
    ERROR_URL_REQUEST : "{0} Hiba történt a következő kérésekor: {1}.", //responseStatusCode, url
    ERROR_SERVER_UNAVAILABLE : "A kiszolgáló nem válaszolt a rendelkezésre álló időn belül.",
    ERROR_APP_NOT_AVAILABLE : "A(z) {0} alkalmazás már nem érhető el a(z) {1} kiszolgáló számára a(z) {2} hoszt {3} könyvtárában.", //appName, serverName, hostName, userdirName
    ERROR_APPLICATION_OPERATION : "Hiba történt a(z) {0} művelet során a {2} kiszolgáló {3} hosztjának {4} könyvtárában található {1} alkalmazáson.", //operation, appName, serverName, hostName, userdirName
    CLUSTER_STATUS : "A(z) {0} fürt állapota: {1}.", //clusterName, clusterStatus
    CLUSTER_UNAVAILABLE : "A(z) {0} fürt már nem érhető el.", //clusterName
    STOP_FAILED_DURING_RESTART : "A leállítás nem fejeződött be sikeresen az újraindítás során.  A hiba: {0}", //errMsg
    ERROR_CLUSTER_OPERATION : "Hiba történt a(z) {1} fürtön végzett {0} művelet során.", //operation, clusterName
    SERVER_NONEXISTANT : "A(z) {0} kiszolgáló nem létezik.", // serverName
    ERROR_SERVER_OPERATION : "Hiba történt az {1} kiszolgáló {2} hosztjának {3} könyvtárán végzett {0} művelet során.", //operation, serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_SET_MAINTENANCE_MODE : "A(z) {0} kiszolgáló karbantartási módja a(z) {1} hoszton a(z) {2} könyvtárban hiba miatt nincs beállítva.", //serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_UNSET_MAINTENANCE_MODE : "A(z) {0} kiszolgáló karbantartási mód beállításának visszavonására tett kísérlet a(z) {1} hoszton a(z) {2} könyvtárban hiba miatt sikertelen volt.", //serverName, hostName, userdirName
    ERROR_HOST_OPERATION_SET_MAINTENANCE_MODE : "A(z) {0} hoszt karbantartási módja hiba miatt nincs beállítva.", // hostName
    ERROR_HOST_OPERATION_UNSET_MAINTENANCE_MODE : "A(z) {0} hoszt karbantartási mód beállításának visszavonására tett kísérlet hiba miatt sikertelen volt.", // hostName
    // Dialog messages on operation errors
    SERVER_START_ERROR: "Hiba történt a kiszolgáló indítása során.",
    SERVER_START_CLEAN_ERROR: "Hiba történt a kiszolgáló indítása során --clean.",
    SERVER_STOP_ERROR: "Hiba történt a kiszolgáló leállítása során.",
    SERVER_RESTART_ERROR: "Hiba történt a kiszolgáló újraindítása során.",
    // Used by standalone stop operation
    STANDALONE_STOP_NO_MBEAN : 'A kiszolgáló nem állt le. A kiszolgáló leállításához szükséges API nem volt elérhető.',
    STANDALONE_STOP_CANT_DETERMINE_MBEAN : 'A kiszolgáló nem állt le. A kiszolgáló leállításához szükséges API-t nem sikerült meghatározni.',
    STANDALONE_STOP_FAILED : 'A kiszolgáló leállítása nem fejeződött be sikeresen. A részleteket nézze meg a kiszolgáló naplóiban.',
    STANDALONE_STOP_SUCCESS : 'A kiszolgáló leállítása sikerült.',
});

