/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

define({
      ACCOUNTING_STRING : "Elszámolási karakterlánc",
      SEARCH_RESOURCE_TYPE_ALL : "Minden",
      SEARCH : "Keresés",
      JAVA_BATCH_SEARCH_BOX_LABEL : "A keresési feltételeket a Keresési feltétel hozzáadása gomb kiválasztásával majd érték meghatározásával adhatja meg",
      SUBMITTED : "Elküldve",
      JMS_QUEUED : "JMS sorba állítva",
      JMS_CONSUMED : "JMS feldolgozta",
      JOB_PARAMETER : "Feladat paraméter",
      DISPATCHED : "Intézkedés történt",
      FAILED : "Sikertelen",
      STOPPED : "Leállítva",
      COMPLETED : "Befejeződött",
      ABANDONED : "Elhagyva",
      STARTED : "Elindítva",
      STARTING : "Indul",
      STOPPING : "Leáll",
      REFRESH : "Frissítés",
      INSTANCE_STATE : "Példány állapota",
      APPLICATION_NAME : "Alkalmazás neve",
      APPLICATION: "Alkalmazás",
      INSTANCE_ID : "Példányazonosító",
      LAST_UPDATE : "Legutóbbi frissítés",
      LAST_UPDATE_RANGE : "Utolsó frissítés tartománya",
      LAST_UPDATED_TIME : "Utolsó frissítés dátuma",
      DASHBOARD_VIEW : "Irányítópult nézet",
      HOMEPAGE : "Honlap",
      JOBLOGS : "Feladatnaplók",
      QUEUED : "Sorba állítva",
      ENDED : "Vége",
      ERROR : "Hiba",
      CLOSE : "Bezárás",
      WARNING : "Figyelmeztetés",
      GO_TO_DASHBOARD: "Ugrás az irányítópulthoz",
      DASHBOARD : "Irányítópult",
      BATCH_JOB_NAME: "Kötegelt feladat neve",
      SUBMITTER: "Beküldő",
      BATCH_STATUS: "Köteg állapota",
      EXECUTION_ID: "Feladatvégrehajtás azonosítója",
      EXIT_STATUS: "Kilépési állapot",
      CREATE_TIME: "Létrehozási idő",
      START_TIME: "Kezdési idő",
      END_TIME: "Befejezési idő",
      SERVER: "Kiszolgáló",
      SERVER_NAME: "Kiszolgáló neve",
      SERVER_USER_DIRECTORY: "Felhasználói könyvtár",
      SERVERS_USER_DIRECTORY: "A kiszolgáló felhasználói könyvtára",
      HOST: "Hoszt",
      NAME: "Név",
      JOB_PARAMETERS: "Feladat paraméterei",
      JES_JOB_NAME: "JES feladat neve",
      JES_JOB_ID: "JES feladat azonosítója",
      ACTIONS: "Műveletek",
      VIEW_LOG_FILE: "Naplófájl megtekintése",
      STEP_NAME: "Lépés neve",
      ID: "Azonosító",
      PARTITION_ID: "Partíció {0}",                               // Partition ID number
      VIEW_EXECUTION_DETAILS: "{0} feladatvégrehajtás részleteinek megtekintése",    // Job Execution ID number
      PARENT_DETAILS: "Szülőinformációk részletei",
      TIMES: "Időbélyegek",      // Heading on section referencing create, start, and end timestamps
      STATUS: "Állapot",
      SEARCH_ON: "Szűrő kiválasztása a következőhöz: {1} {0}",                    // {0} Value, {1} Column name
      SEARCH_PILL_INPUT : "Adja meg a keresési feltételeket.",
      BREADCRUMB_JOB_INSTANCE : "Feladatpéldány: {0}",                // Job Instance ID
      BREADCRUMB_JOB_EXECUTION : "Feladatvégrehajtás: {0}",              // Job Execution ID
      BREADCRUMB_JOB_LOG : "Feladatnapló: {0}",
      BATCH_SEARCH_CRITERIA_INVALID : "A keresési feltételek nem érvényesek.",
      ERROR_CANNOT_HAVE_MULTIPLE_SEARCHBY : "A keresési feltételekben csak egy {0} paraméter szerinti szűrés lehet.", // {0} will be another translated message key like SUBMITTER and BATCH_JOB_NAME

      INSTANCES_TABLE_IDENTIFIER: "Feladatpéldányok táblázata",
      EXECUTIONS_TABLE_IDENTIFIER: "Feladatvégrehajtások táblázata",
      STEPS_DETAILS_TABLE_IDENTIFIER: "Lépések részleteinek táblázata",
      LOADING_VIEW : "Az oldal jelenleg betölti az információkat",
      LOADING_VIEW_TITLE : "Nézet betöltése",
      LOADING_GRID : "Várakozás a kiszolgáló által visszaadott keresési találatokra. ",
      PAGENUMBER : "Oldalszám",
      SELECT_QUERY_SIZE: "Lekérdezési méret kiválasztása",
      LINK_EXPLORE_HOST: "Válassza ki a részletek megjelenítését a(z) {0} hoszton a böngészőeszközben. ",      // Host name
      LINK_EXPLORE_SERVER: "Válassza ki a részletek megjelenítését a(z) {0} kiszolgálón a böngészőeszközben. ",  // Server name

      //ACTIONS
      RESTART: "Újraindítás",
      STOP: "Leállítás",
      PURGE: "Kiürítés",
      OK_BUTTON_LABEL: "OK",
      INSTANCE_ACTIONS_BUTTON_LABEL: "A(z) {0} feladatpéldány műveletei",     // Job Instance ID number
      INSTANCE_ACTIONS_MENU_LABEL:  "Feladatpéldány műveletei menü",

      RESTART_INSTANCE_MESSAGE: "Kívánja újraindítani a(z) {0} feladatpéldányhoz tartozó legutóbbi feladat-végrehajtást?",     // Job Instance ID number
      STOP_INSTANCE_MESSAGE: "Kívánja leállítani a(z) {0} feladatpéldányhoz tartozó legutóbbi feladat-végrehajtást?",           // Job Instance ID number
      PURGE_INSTANCE_MESSAGE: "Kívánja kiüríteni a(z) {0} feladatpéldányhoz tartozó összes adatbázis-bejegyzést és feladatnaplót?",     // Job Instance ID number
      PURGE_JOBSTORE_ONLY: "Csak a feladattároló kiürítése",

      RESTART_INST_ERROR_MESSAGE: "Az újraindítási kérés meghiúsult.",
      STOP_INST_ERROR_MESSAGE: "A leállítási kérés meghiúsult.",
      PURGE_INST_ERROR_MESSAGE: "A kiürítési kérés meghiúsult.",
      ACTION_REQUEST_ERROR_MESSAGE: "A műveletkérés a következő állapotkóddal meghiúsult: {0}.  URL: {1}",  // Status Code number, URL string

      // RESTART JOB WITH PARAMETERS DIALOG
      REUSE_PARMS_TOGGLE_LABEL: "Paraméterek újrafelhasználása korábbi végrehajtásból",
      JOB_PARAMETERS_EMPTY: "Ha a '{0}' nincs bejelölve, akkor itt adja meg a feladat paramétereit.",   // 0 - Checkbox label - REUSE_PARMS_TOGGLE_LABEL
      JOB_PARAMETER_NAME: "Paraméter neve",
      JOB_PARAMETER_VALUE: "Paraméter értéke",
      PARM_NAME_COLUMN_HEADER: "Paraméter",
      PARM_VALUE_COLUMN_HEADER: "Érték",
      PARM_ADD_ICON_TITLE: "Paraméter hozzáadása",
      PARM_REMOVE_ICON_TITLE: "Paraméter eltávolítása",
      PARMS_ENTRY_ERROR: "A paraméternév megadása kötelező.",
      JOB_PARAMETER_CREATE: "Válassza a {0} lehetőséget paraméterek hozzáadásához a feladatpéldány következő végrehajtásához.",  // 0 - Button label
      JOB_PARAMETER_CREATE_BUTTON: "Paraméter hozzáadása gomb a táblázat fejlécében.",

      //JOB LOGS PAGE MESSAGES
      JOBLOGS_LOG_CONTENT : "Munkanapló tartalma",
      FILE_DOWNLOAD : "Fájl letöltése",
      DOWNLOAD_DIALOG_DESCRIPTION : "Kívánja letölteni a naplófájlt?",
      INCLUDE_ALL_LOGS : "A feladatvégrehajtás összes naplófájljának szerepeltetése",
      LOGS_NAVIGATION_BAR : "Feladatnaplók navigációs sávja",
      DOWNLOAD : "Letöltés",
      LOG_TOP : "Naplók teteje",
      LOG_END : "Naplók vége",
      PREVIOUS_PAGE : "Előző oldal",
      NEXT_PAGE : "Következő oldal",
      DOWNLOAD_ARIA : "Fájl letöltése",

      //Error messages for popups
      REST_CALL_FAILED : "Az adatlehívás meghiúsult.",
      NO_JOB_EXECUTION_URL : "Nincs megadva feladatvégrehajtási szám az URL címben vagy a példányhoz nem tartoznak megjeleníthető feladatvégrehajtási naplók",
      NO_VIEW : "URL hiba: Nem létezik ilyen nézet.",
      WRONG_TOOL_ID : "Az URL lekérdezési karaktersorozat nem a(z) {0} eszközazonosítóval kezdődött, hanem a következővel: {1}.",   // {0} and {1} are both Strings
      URL_NO_LOGS : "URL hiba: Nincsenek naplók.",
      NOT_A_NUMBER : "URL hiba: A(z) {0} csak szám lehet.",                                                // {0} is a field name
      PARAMETER_REPETITION : "URL hiba: A(z) {0} csak egyszer szerepelhet a paraméterek között.",                   // {0} is a field name
      URL_PAGE_PARAMS_ERROR : "URL hiba: az oldal paraméter kívül esik a tartományon.",
      INVALID_PARAMETER : "URL hiba: {0} nem egy érvényes paraméter.",                                       // {0} is a String
      URL_MULTIPLE_ATTRIBUTES : "URL hiba: Az URL vagy egy feladatvégrehajtást vagy egy feladatpéldányt tartalmazhat, a kettőt egyszerre nem.",
      MISSING_EXECUTION_ID_PARAM : "Hiányzik a kötelező végrehajtási azonosító paraméter.",
      PERSISTENCE_CONFIGURATION_REQUIRED : "A Java Batch eszköz használatához Java köteg állandó adatbázis konfiguráció szükséges. ",
      IGNORED_SEARCH_CRITERIA : "A következő szűrőfeltételek figyelmen kívül maradtak a találatokban: {0}",

      GRIDX_SUMMARY_TEXT : "Legutóbbi ${0} feladatpéldány megjelenítve"

});

