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
    ERROR : "Eroare",
    ERROR_STATUS : "{0} Eroare: {1}", //responseStatusCode, response.text
    ERROR_URL_REQUEST_NO_STATUS : "A apărut o eroare la cererea {0}.", // url
    ERROR_URL_REQUEST : "{0} A apărut o eroare la cererea {1}.", //responseStatusCode, url
    ERROR_SERVER_UNAVAILABLE : "Serverul nu a răspuns în timpul alocat.",
    ERROR_APP_NOT_AVAILABLE : "Aplicaţia {0} nu mai este disponibilă pentru serverul {1} pe gazda {2} în directorul {3}.", //appName, serverName, hostName, userdirName
    ERROR_APPLICATION_OPERATION : "A apărut o eroare la încercarea la {0} aplicaţia {1} pe serverul {2} pe gazda {3} în directorul {4}.", //operation, appName, serverName, hostName, userdirName
    CLUSTER_STATUS : "Cluster-ul {0} este {1}.", //clusterName, clusterStatus
    CLUSTER_UNAVAILABLE : "Cluster-ul {0} nu mai este disponibil.", //clusterName
    STOP_FAILED_DURING_RESTART : "Oprirea nu s-a finalizat cu succes la repornire.  Eroarea a fost: {0}", //errMsg
    ERROR_CLUSTER_OPERATION : "A apărut o eroare la încercarea {0} cluster {1}.", //operation, clusterName
    SERVER_NONEXISTANT : "Serverul {0} nu există.", // serverName
    ERROR_SERVER_OPERATION : "A apărut o eroare la încercarea la {0} server {1} pe gazda {2} în directorul {3}.", //operation, serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_SET_MAINTENANCE_MODE : "Modul întreţinere pentru server {0} pe gazda {1} din directorul {2} nu a fost setat din cauza unei erori.", //serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_UNSET_MAINTENANCE_MODE : "Încercarea de a anula setarea modului întreţinere pentru serverul {0} pe gazda {1} din directorul {2} nu s-a finalizat din cauza unei erori.", //serverName, hostName, userdirName
    ERROR_HOST_OPERATION_SET_MAINTENANCE_MODE : "Modul întreţinere pentru gazda {0} nu a fost setat din cauza unei erori.", // hostName
    ERROR_HOST_OPERATION_UNSET_MAINTENANCE_MODE : "Încercarea de a anula setarea modului întreţinere pentru gazda {0} nu s-a finalizat din cauza unei erori.", // hostName
    // Dialog messages on operation errors
    SERVER_START_ERROR: "Eroare la pornirea serverului.",
    SERVER_START_CLEAN_ERROR: "Eroare la pornirea serverului --curăţare.",
    SERVER_STOP_ERROR: "Eroare la oprirea serverului.",
    SERVER_RESTART_ERROR: "Eroare la repornirea serverului.",
    // Used by standalone stop operation
    STANDALONE_STOP_NO_MBEAN : 'Serverul nu s-a oprit. API-ul necesar pentru oprirea serverului nu a fost disponibil.',
    STANDALONE_STOP_CANT_DETERMINE_MBEAN : 'Serverul nu s-a oprit. API-ul necesar pentru oprirea serverului nu a putut fi determinat.',
    STANDALONE_STOP_FAILED : 'Operaţia de oprire a serverului nu s-a finalizat cu succes. Verificaţi istoricele serverului pentru detalii.',
    STANDALONE_STOP_SUCCESS : 'Serverul s-a oprit cu succes.',
});

