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
    ERROR : "Chyba",
    ERROR_STATUS : "Chyba {0}: {1}", //responseStatusCode, response.text
    ERROR_URL_REQUEST_NO_STATUS : "Při vyžádání adresy {0} došlo k chybě.", // url
    ERROR_URL_REQUEST : "Při vyžádání adresy {1} došlo k chybě {0}.", //responseStatusCode, url
    ERROR_SERVER_UNAVAILABLE : "Server neodpověděl v přiděleném čase.",
    ERROR_APP_NOT_AVAILABLE : "Aplikace {0} již není dostupná pro server {1} na hostiteli {2} v adresáři {3}.", //appName, serverName, hostName, userdirName
    ERROR_APPLICATION_OPERATION : "Při pokusu o operaci {0} s aplikací {1} na serveru {2} na hostiteli {3} v adresáři {4} došlo k chybě.", //operation, appName, serverName, hostName, userdirName
    CLUSTER_STATUS : "Klastr {0} je ve stavu {1}.", //clusterName, clusterStatus
    CLUSTER_UNAVAILABLE : "Klastr {0} již není dostupný.", //clusterName
    STOP_FAILED_DURING_RESTART : "Při restartu nedošlo k úspěšnému zastavení. Došlo k této chybě: {0}", //errMsg
    ERROR_CLUSTER_OPERATION : "Při pokusu o operaci {0} s klastrem {1} došlo k chybě.", //operation, clusterName
    SERVER_NONEXISTANT : "Server {0} neexistuje.", // serverName
    ERROR_SERVER_OPERATION : "Při pokusu o operaci {0} se serverem {1} na hostiteli {2} v adresáři {3} došlo k chybě.", //operation, serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_SET_MAINTENANCE_MODE : "Režim údržby pro server {0} na hostiteli {1} v adresáři {2} nebyl nastaven kvůli chybě.", //serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_UNSET_MAINTENANCE_MODE : "Pokus o zrušení nastavení režimu údržby pro server {0} na hostiteli {1} v adresáři {2} se nedokončil kvůli chybě.", //serverName, hostName, userdirName
    ERROR_HOST_OPERATION_SET_MAINTENANCE_MODE : "Režim údržby pro hostitele {0} nebyl nastaven kvůli chybě.", // hostName
    ERROR_HOST_OPERATION_UNSET_MAINTENANCE_MODE : "Pokus o zrušení nastavení režimu údržby pro hostitele {0} se nedokončil kvůli chybě.", // hostName
    // Dialog messages on operation errors
    SERVER_START_ERROR: "Chyba během spuštění serveru.",
    SERVER_START_CLEAN_ERROR: "Chyba během spuštění příkazu --clean serveru.",
    SERVER_STOP_ERROR: "Chyba během zastavení serveru.",
    SERVER_RESTART_ERROR: "Chyba během restartu serveru.",
    // Used by standalone stop operation
    STANDALONE_STOP_NO_MBEAN : 'Server nebyl zastaven. Nebylo dostupné potřebné rozhraní API k zastavení serveru.',
    STANDALONE_STOP_CANT_DETERMINE_MBEAN : 'Server nebyl zastaven. Potřebné rozhraní API k zastavení serveru nelze určit.',
    STANDALONE_STOP_FAILED : 'Operace zastavení serveru nebyla úspěšně dokončena. Podrobné informace naleznete v protokolech serveru.',
    STANDALONE_STOP_SUCCESS : 'Server byl úspěšně zastaven.',
});

