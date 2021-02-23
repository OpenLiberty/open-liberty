/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define({
    EXPLORER : "Explorador",
    EXPLORE : "Explorar",
    DASHBOARD : "Panel de instrumentos",
    DASHBOARD_VIEW_ALL_APPS : "Ver todas las aplicaciones",
    DASHBOARD_VIEW_ALL_SERVERS : "Ver todos los servidores",
    DASHBOARD_VIEW_ALL_CLUSTERS : "Ver todos los clústeres",
    DASHBOARD_VIEW_ALL_HOSTS : "Ver todos los hosts",
    DASHBOARD_VIEW_ALL_RUNTIMES : "Ver todos los tiempos de ejecución",
    SEARCH : "Buscar",
    SEARCH_RECENT : "Búsquedas recientes",
    SEARCH_RESOURCES : "Buscar recursos",
    SEARCH_RESULTS : "Resultados de la búsqueda",
    SEARCH_NO_RESULTS : "Sin resultados",
    SEARCH_NO_MATCHES : "Sin coincidencias",
    SEARCH_TEXT_INVALID : "El texto de búsqueda incluye caracteres no válidos",
    SEARCH_CRITERIA_INVALID : "Los criterios de búsqueda no son válidos.",
    SEARCH_CRITERIA_INVALID_COMBO :"{0} no es válido cuando se especifica con {1}.",
    SEARCH_CRITERIA_INVALID_DUPLICATE : "Especifique {0} una sola vez.",
    SEARCH_TEXT_MISSING : "El texto de búsqueda es obligatorio",
    SEARCH_UNSUPPORT_TYPE_APPONSERVER : "La búsqueda de códigos de aplicación en un servidor no está soportada.",
    SEARCH_UNSUPPORT_TYPE_APPONCLUSTER : "La búsqueda de códigos de aplicación en un clúster no está soportada.",
    SEARCH_UNSUPPORT : "Los criterios de búsqueda no están soportados.",
    SEARCH_SWITCH_VIEW : "Conmutar vista",
    FILTERS : "Filtros",
    DEPLOY_SERVER_PACKAGE : "Desplegar paquete de servidor",
    MEMBER_OF : "Miembro de",
    N_CLUSTERS: "{0} Clústeres...",

    INSTANCE : "Instancia",
    INSTANCES : "Instancias",
    APPLICATION : "Aplicación",
    APPLICATIONS : "Aplicaciones",
    SERVER : "Servidor",
    SERVERS : "Servidores",
    CLUSTER : "Clúster",
    CLUSTERS : "Clústeres",
    CLUSTER_NAME : "Nombre del clúster: ",
    CLUSTER_STATUS : "Estado del clúster: ",
    APPLICATION_NAME : "Nombre de la aplicación: ",
    APPLICATION_STATE : "Estado de la aplicación: ",
    HOST : "Host",
    HOSTS : "Hosts",
    RUNTIME : "Ejecución",
    RUNTIMES : "Tiempos de ejecución",
    PATH : "Vía de acceso",
    CONTROLLER : "Controlador",
    CONTROLLERS : "Controladores",
    OVERVIEW : "Descripción general",
    CONFIGURE : "Configurar",

    SEARCH_RESOURCE_TYPE: "Tipo", // Search by resource types
    SEARCH_RESOURCE_STATE: "Estado", // Search by resource types
    SEARCH_RESOURCE_TYPE_ALL: "Todos", // Search all resource types
    SEARCH_RESOURCE_NAME: "Nombre", // Search by resource name
    SEARCH_RESOURCE_TAG: "Etiqueta", // Search by resource tag
    SEARCH_RESOURCE_CONTAINER: "Contenedor", // Search by container type
    SEARCH_RESOURCE_CONTAINER_DOCKER: "Docker", // Search by container type Docker
    SEARCH_RESOURCE_CONTAINER_NONE: "Ninguna", // Search by container type none
    SEARCH_RESOURCE_RUNTIMETYPE: "Tipo de tiempo de ejecución", // Search by runtime type
    SEARCH_RESOURCE_OWNER: "Propietario", // Search by owner
    SEARCH_RESOURCE_CONTACT: "Contacto", // Search by contact
    SEARCH_RESOURCE_NOTE: "Nota", // Search by note

    GRID_HEADER_USERDIR : "Directorio de usuario",
    GRID_HEADER_NAME : "Nombre",
    GRID_LOCATION_NAME : "Ubicación",
    GRID_ACTIONS : "Acciones de cuadrícula",
    GRID_ACTIONS_LABEL : "{0} acciones de cuadrícula",  // name of the grid
    APPONSERVER_LOCATION_NAME : "{0} en {1} ({2})", // server on host (/path)

    STATS : "Supervisor",
    STATS_ALL : "Todos",
    STATS_VALUE : "Valor: {0}",
    CONNECTION_IN_USE_STATS : "{0} En uso = {1} Gestionadas - {2} Libres",
    CONNECTION_IN_USE_STATS_VALUE : "Valor: {0} En uso = {1} Gestionadas - {2} Libres",
    DATA_SOURCE : "Origen de datos: {0}",
    STATS_DISPLAY_LEGEND : "Mostrar leyenda",
    STATS_HIDE_LEGEND : "Ocultar leyenda",
    STATS_VIEW_DATA : "Ver datos de gráfico",
    STATS_VIEW_DATA_TIMESTAMP : "Indicación de fecha y hora",
    STATS_ACTION_MENU : "Menú de acciones de {0}",
    STATS_SHOW_HIDE : "Añadir métricas de recursos",
    STATS_SHOW_HIDE_SUMMARY : "Añadir métricas para resumen",
    STATS_SHOW_HIDE_TRAFFIC : "Añadir métricas para tráfico",
    STATS_SHOW_HIDE_PERFORMANCE : "Añadir métricas para rendimiento",
    STATS_SHOW_HIDE_AVAILABILITY : "Añadir métricas para disponibilidad",
    STATS_SHOW_HIDE_ALERT : "Añadir métricas para alerta",
    STATS_SHOW_HIDE_LIST_BUTTON : "Mostrar u ocultar la lista de métricas de recursos",
    STATS_SHOW_HIDE_BUTTON_TITLE : "Editar gráficos",
    STATS_SHOW_HIDE_CONFIRM : "Guardar",
    STATS_SHOW_HIDE_CANCEL : "Cancelar",
    STATS_SHOW_HIDE_DONE : "Hecho",
    STATS_DELETE_GRAPH : "Suprimir gráfico",
    STATS_ADD_CHART_LABEL : "Añadir gráfico a la vista",
    STATS_JVM_TITLE : "JVM",
    STATS_JVM_BUTTON_LABEL : "Añadir todos los gráficos JVM a la vista",
    STATS_HEAP_TITLE : "Memoria de almacenamiento dinámico utilizada",
    STATS_HEAP_USED : "Utilizada: {0} MB",
    STATS_HEAP_COMMITTED : "Confirmada: {0} MB",
    STATS_HEAP_MAX : "Máx: {0} MB",
    STATS_HEAP_X_TIME : "Hora",
    STATS_HEAP_Y_MB : "MB utilizados",
    STATS_HEAP_Y_MB_LABEL : "{0} MB",
    STATS_CLASSES_TITLE : "Clases cargadas",
    STATS_CLASSES_LOADED : "Cargadas: {0}",
    STATS_CLASSES_UNLOADED : "Descargadas: {0}",
    STATS_CLASSES_TOTAL : "Total: {0}",
    STATS_CLASSES_Y_TOTAL : "Clases cargadas",
    STATS_PROCESSCPU_TITLE : "Uso de CPU",
    STATS_PROCESSCPU_USAGE : "Uso de CPU: {0}%",
    STATS_PROCESSCPU_Y_PERCENT : "Porcentaje de CPU",
    STATS_PROCESSCPU_Y_PCT_LABEL : "{0}%",
    STATS_THREADS_TITLE : "Hebras de JVM activas",
    STATS_LIVE_MSG_INIT : "Mostrando datos actuales",
    STATS_LIVE_MSG :"Este gráfico no tiene datos históricos. Seguirá mostrando los últimos 10 minutos de datos.",
    STATS_THREADS_ACTIVE : "Actuales: {0}",
    STATS_THREADS_PEAK : "Máximo: {0}",
    STATS_THREADS_TOTAL : "Total: {0}",
    STATS_THREADS_Y_THREADS : "Hebras",
    STATS_TP_POOL_SIZE : "Tamaño de agrupación",
    STATS_JAXWS_TITLE : "Servicios web JAX-WS",
    STATS_JAXWS_BUTTON_LABEL : "Añadir todos los gráficos de Servicios web JAX-WS a la vista",
    STATS_JW_AVG_RESP_TIME : "Tiempo medio de respuesta",
    STATS_JW_AVG_INVCOUNT : "Promedio de invocaciones",
    STATS_JW_TOTAL_FAULTS : "Errores totales de tiempo de ejecución",
    STATS_LA_RESOURCE_CONFIG_LABEL : "Seleccionar recursos...",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM : "{0} recursos",
    STATS_LA_RESOURCE_CONFIG_LABEL_NUM_1 : "1 recurso",
    STATS_LA_RESOURCE_CONFIG_SELECT_ONE : "Debe seleccionar al menos un recurso.",
    STATS_LA_RESOURCE_CONFIG_NO_DATA : "No hay datos disponibles para el rango de horas seleccionado.",
    STATS_ACCESS_LOG_TITLE : "Registro de acceso",
    STATS_ACCESS_LOG_BUTTON_LABEL : "Añadir todos los gráficos de Registro de acceso a la vista",
    STATS_ACCESS_LOG_GRAPH : "Recuento de mensajes de Registro de acceso",
    STATS_ACCESS_LOG_SUMMARY : "Resumen de Registro de acceso",
    STATS_ACCESS_LOG_TABLE : "Lista de mensajes de Registro de acceso",
    STATS_MESSAGES_TITLE : "Mensajes y rastreo",
    STATS_MESSAGES_BUTTON_LABEL : "Añadir todos los gráficos de Mensajes y rastreo a la vista",
    STATS_MESSAGES_GRAPH : "Recuento de mensajes de registro",
    STATS_MESSAGES_TABLE : "Lista de mensajes de registro",
    STATS_FFDC_GRAPH : "Recuento de FFDC",
    STATS_FFDC_TABLE : "Lista de FFDC",
    STATS_TRACE_LOG_GRAPH : "Recuento de mensajes de rastreo",
    STATS_TRACE_LOG_TABLE : "Lista de mensajes de rastreo",
    STATS_THREAD_POOL_TITLE : "Agrupación de hebras",
    STATS_THREAD_POOL_BUTTON_LABEL : "Añadir todos los gráficos de Agrupación de hebras a la vista",
    STATS_THREADPOOL_TITLE : "Hebras de Liberty activas",
    STATS_THREADPOOL_SIZE : "Tamaño de agrupación: {0}",
    STATS_THREADPOOL_ACTIVE : "Activas: {0}",
    STATS_THREADPOOL_TOTAL : "Total: {0}",
    STATS_THREADPOOL_Y_ACTIVE : "Hebras activas",
    STATS_SESSION_MGMT_TITLE : "Sesiones",
    STATS_SESSION_MGMT_BUTTON_LABEL : "Añadir todos los gráficos de Sesiones a la vista",
    STATS_SESSION_CONFIG_LABEL : "Seleccionar sesiones...",
    STATS_SESSION_CONFIG_LABEL_NUM  : "{0} sesiones",
    STATS_SESSION_CONFIG_LABEL_NUM_1  : "1 sesión",
    STATS_SESSION_CONFIG_SELECT_ONE : "Debe seleccionar al menos una sesión.",
    STATS_SESSION_TITLE : "Sesiones activas",
    STATS_SESSION_Y_ACTIVE : "Sesiones activas",
    STATS_SESSION_LIVE_LABEL : "Número actual: {0}",
    STATS_SESSION_CREATE_LABEL : "Número de creadas: {0}",
    STATS_SESSION_INV_LABEL : "Número de invalidadas: {0}",
    STATS_SESSION_INV_TIME_LABEL : "Número de invalidadas por tiempo de espera excedido: {0}",
    STATS_WEBCONTAINER_TITLE : "Aplicaciones Web",
    STATS_WEBCONTAINER_BUTTON_LABEL : "Añadir todos los gráficos de Aplicaciones web a la vista",
    STATS_SERVLET_CONFIG_LABEL : "Seleccionar servlets...",
    STATS_SERVLET_CONFIG_LABEL_NUM : "{0} servlets",
    STATS_SERVLET_CONFIG_LABEL_NUM_1 : "1 servlet",
    STATS_SERVLET_CONFIG_SELECT_ONE : "Debe seleccionar al menos un servlet.",
    STATS_SERVLET_REQUEST_COUNT_TITLE : "Número de solicitudes",
    STATS_SERVLET_REQUEST_COUNT_Y_AXIS : "Número de solicitudes",
    STATS_SERVLET_RESPONSE_COUNT_TITLE : "Número de respuestas",
    STATS_SERVLET_RESPONSE_COUNT_Y_AXIS : "Número de respuestas",
    STATS_SERVLET_RESPONSE_MEAN_TITLE : "Tiempo medio de respuesta (ns)",
    STATS_SERVLET_RESPONSE_MEAN_Y_AXIS : "Tiempo de respuesta (ns)",
    STATS_CONN_POOL_TITLE : "Agrupación de conexiones",
    STATS_CONN_POOL_BUTTON_LABEL : "Añadir todos los gráficos de Agrupación de conexiones a la vista",
    STATS_CONN_POOL_CONFIG_LABEL : "Seleccionar orígenes de datos...",
    STATS_CONN_POOL_CONFIG_LABEL_NUM : "{0} orígenes de datos",
    STATS_CONN_POOL_CONFIG_LABEL_NUM_1 : "1 origen de datos",
    STATS_CONN_POOL_CONFIG_SELECT_ONE : "Debe seleccionar al menos un origen de datos.",
    STATS_CONNECT_IN_USE_TITLE : "Conexiones en uso",
    STATS_CONNECT_USED_COUNT_Y_AXIS : "Conexiones",
    STATS_CONNECT_IN_USE_LABEL : "En uso: {0}",
    STATS_CONNECT_USED_USED_LABEL : "Utilizadas: {0}",
    STATS_CONNECT_USED_FREE_LABEL : "Libres: {0}",
    STATS_CONNECT_USED_CREATE_LABEL : "Creadas: {0}",
    STATS_CONNECT_USED_DESTROY_LABEL : "Destruidas: {0}",
    STATS_CONNECT_WAIT_TIME_TITLE : "Tiempo de espera medio (ms)",
    STATS_CONNECT_WAIT_TIME_Y_AXIS : "Tiempo de espera (ms)",
    STATS_TIME_ALL : "Todos",
    STATS_TIME_1YEAR : "1a",
    STATS_TIME_1MONTH : "1me",
    STATS_TIME_1WEEK : "1s",
    STATS_TIME_1DAY : "1d",
    STATS_TIME_1HOUR : "1h",
    STATS_TIME_10MINUTES : "10m",
    STATS_TIME_5MINUTES : "5m",
    STATS_TIME_1MINUTE : "1m",
    STATS_PERSPECTIVE_SUMMARY : "Resumen",
    STATS_PERSPECTIVE_TRAFFIC : "Tráfico",
    STATS_PERSPECTIVE_TRAFFIC_JVM : "Tráfico de JVM",
    STATS_PERSPECTIVE_TRAFFIC_CONN : "Tráfico de conexión",
    STATS_PERSPECTIVE_TRAFFIC_LAACCESS : "Tráfico de Registro de acceso",
    STATS_PERSPECTIVE_PROBLEM : "Problema",
    STATS_PERSPECTIVE_PERFORMANCE : "Rendimiento",
    STATS_PERSPECTIVE_PERFORMANCE_JVM : "Rendimiento de JVM",
    STATS_PERSPECTIVE_PERFORMANCE_CONN : "Rendimiento de conexión",
    STATS_PERSPECTIVE_ALERT : "Análisis de alerta",
    STATS_PERSPECTIVE_ALERT_LAACCESS : "Alerta de Registro de acceso",
    STATS_PERSPECTIVE_ALERT_LAMSGS : "Alerta de mensaje y registro de rastreo",
    STATS_PERSPECTIVE_AVAILABILITY : "Disponibilidad",

    STATS_DISPLAY_TIME_LAST_MINUTE_LABEL : "Último minuto",
    STATS_DISPLAY_TIME_LAST_5_MINUTES_LABEL : "Últimos 5 minutos",
    STATS_DISPLAY_TIME_LAST_10_MINUTES_LABEL : "Últimos 10 minutos",
    STATS_DISPLAY_TIME_LAST_HOUR_LABEL : "Última hora",
    STATS_DISPLAY_TIME_LAST_DAY_LABEL : "Último día",
    STATS_DISPLAY_TIME_LAST_WEEK_LABEL : "Última semana",
    STATS_DISPLAY_TIME_LAST_MONTH_LABEL : "Último mes",
    STATS_DISPLAY_TIME_LAST_YEAR_LABEL : "Último año",

    STATS_DISPLAY_CUSTOM_TIME_LAST_SECOND_LABEL : "Último {0}s",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_LABEL : "Último {0}m",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MINUTE_AND_SECOND_LABEL : "Último {0}m {1}s",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_LABEL : "Última {0}h",
    STATS_DISPLAY_CUSTOM_TIME_LAST_HOUR_AND_MINUTE_LABEL : "Última {0}h {1}m",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_LABEL : "Último {0}d",
    STATS_DISPLAY_CUSTOM_TIME_LAST_DAY_AND_HOUR_LABEL : "Último {0}d {1}h",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_LABEL : "Última {0}w",
    STATS_DISPLAY_CUSTOM_TIME_LAST_WEEK_AND_DAY_LABEL : "Última {0}s {1}d",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_LABEL : "Último {0}me",
    STATS_DISPLAY_CUSTOM_TIME_LAST_MONTH_AND_DAY_LABEL : "Último {0}me {1}d",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_LABEL : "Último {0}a",
    STATS_DISPLAY_CUSTOM_TIME_LAST_YEAR_AND_MONTH_LABEL : "Último {0}a {1}me",

    STATS_LIVE_UPDATE_LABEL: "Actualización en vivo",
    STATS_TIME_SELECTOR_NOW_LABEL: "Ahora",

    LOG_ANALYTICS_LOG_MESSAGE_TITLE: "Mensajes de registro",

    AUTOSCALED_APPLICATION : "Aplicación con escala automática",
    AUTOSCALED_SERVER : "Servidor con escala automática",
    AUTOSCALED_CLUSTER : "Clúster con escala automática",
    AUTOSCALED_POLICY : "Política de escala automática",
    AUTOSCALED_POLICY_DISABLED : "La política de escala automática está inhabilitada",
    AUTOSCALED_NOACTIONS : "No hay acciones disponibles para recursos con escala automática",

    START : "Iniciar",
    START_CLEAN : "Iniciar --clean",
    STARTING : "Iniciando",
    STARTED : "iniciado",
    RUNNING : "Ejecutándose",
    NUM_RUNNING: "{0} en ejecución",
    PARTIALLY_STARTED : "Iniciado parcialmente",
    PARTIALLY_RUNNING : "Parcialmente en ejecución",
    NOT_STARTED : "No iniciado",
    STOP : "Parar",
    STOPPING : "Deteniéndose",
    STOPPED : "Detenidos",
    NUM_STOPPED : "{0} detenidos",
    NOT_RUNNING : "No está en ejecución",
    RESTART : "Reiniciar",
    RESTARTING : "Reiniciando",
    RESTARTED : "Reiniciado",
    ALERT : "Alerta",
    ALERTS : "Alertas",
    UNKNOWN : "Desconocido",
    NUM_UNKNOWN : "{0} desconocidos",
    SELECT : "Seleccionar",
    SELECTED : "Seleccionado",
    SELECT_ALL : "Seleccionar todo",
    SELECT_NONE : "No seleccionar nada",
    DESELECT: "Deseleccionar",
    DESELECT_ALL : "Deseleccionar todo",
    TOTAL : "Total",
    UTILIZATION : "Superior al {0}% de utilización", // percent

    ELLIPSIS_ARIA: "Expandir para ver más opciones.",
    EXPAND : "Expandir",
    COLLAPSE: "Contraer",

    ALL : "Todos",
    ALL_APPS : "Todas las aplicaciones",
    ALL_SERVERS : "Todos los servidores",
    ALL_CLUSTERS : "Todos los clústeres",
    ALL_HOSTS : "Todos los hosts",
    ALL_APP_INSTANCES : "Todas las instancias de aplicaciones",
    ALL_RUNTIMES : "Todos los tiempos de ejecución",

    ALL_APPS_RUNNING : "Todas las aplicaciones en ejecución",
    ALL_SERVER_RUNNING : "Todos los servidores en ejecución",
    ALL_CLUSTERS_RUNNING : "Todos los clústeres en ejecución",
    ALL_APPS_STOPPED : "Todas las aplicaciones detenidas",
    ALL_SERVER_STOPPED : "Todos los servidores detenidos",
    ALL_CLUSTERS_STOPPED : "Todos los clústeres detenidos",
    ALL_SERVERS_UNKNOWN : "Todos los servidores desconocidos",
    SOME_APPS_RUNNING : "Algunas aplicaciones en ejecución",
    SOME_SERVERS_RUNNING : "Algunos servidores en ejecución",
    SOME_CLUSTERS_RUNNING : "Algunos clústeres en ejecución",
    NO_APPS_RUNNING : "Sin aplicaciones en ejecución",
    NO_SERVERS_RUNNING : "Sin servidores en ejecución",
    NO_CLUSTERS_RUNNING : "Sin clústeres en ejecución",

    HOST_WITH_ALL_SERVERS_RUNNING: "Hosts con todos los servidores en ejecución", // not used anymore since 4Q
    HOST_WITH_SOME_SERVERS_RUNNING: "Hosts con algunos servidores en ejecución",
    HOST_WITH_NO_SERVERS_RUNNING: "Hosts sin servidores en ejecución", // not used anymore since 4Q
    HOST_WITH_ALL_SERVERS_STOPPED: "Hosts con todos los servidores detenidos",
    HOST_WITH_SERVERS_RUNNING: "Hosts con los servidores en ejecución",

    RUNTIME_WITH_SOME_SERVERS_RUNNING: "Tiempos de ejecución con algunos servidores en ejecución",
    RUNTIME_WITH_ALL_SERVERS_STOPPED: "Tiempos de ejecución con todos los servidores detenidos",
    RUNTIME_WITH_SERVERS_RUNNING: "Tiempos de ejecución con los servidores en ejecución",

    START_ALL_APPS : "¿Iniciar todas las aplicaciones?",
    START_ALL_INSTANCES : "¿Iniciar todas las instancias de aplicaciones?",
    START_ALL_SERVERS : "¿Iniciar todos los servidores?",
    START_ALL_CLUSTERS : "¿Iniciar todos los clústeres?",
    STOP_ALL_APPS : "¿Detener todas las aplicaciones?",
    STOPE_ALL_INSTANCES : "¿Detener todas las instancias de aplicaciones?",
    STOP_ALL_SERVERS : "¿Detener todos los servidores?",
    STOP_ALL_CLUSTERS : "¿Detener todos los clústeres?",
    RESTART_ALL_APPS : "¿Reiniciar todas las aplicaciones?",
    RESTART_ALL_INSTANCES : "¿Reiniciar todas las instancias de aplicaciones?",
    RESTART_ALL_SERVERS : "¿Reiniciar todos los servidores?",
    RESTART_ALL_CLUSTERS : "¿Reiniciar todos los clústeres?",

    START_INSTANCE : "¿Iniciar instancia de la aplicación?",
    STOP_INSTANCE : "¿Detener instancia de la aplicación?",
    RESTART_INSTANCE : "¿Reiniciar instancia de la aplicación?",

    START_SERVER : "¿Iniciar servidor {0}?",
    STOP_SERVER : "¿Detener servidor {0}?",
    RESTART_SERVER : "¿Reiniciar servidor {0}?",

    START_ALL_INSTS_OF_APP : "¿Iniciar todas las instancias de {0}?", // application name
    START_APP_ON_SERVER : "¿Iniciar {0} en {1}?", // app name, server name
    START_ALL_APPS_WITHIN : "¿Iniciar todas las aplicaciones en {0}?", // resource
    START_ALL_APP_INSTS_WITHIN : "¿Iniciar todas las instancias de aplicaciones en {0}?", // resource
    START_ALL_SERVERS_WITHIN : "¿Iniciar todos los servidores en {0}?", // resource
    STOP_ALL_INSTS_OF_APP : "¿Detener todas las instancias de {0}?", // application name
    STOP_APP_ON_SERVER : "¿Detener {0} en {1}?", // app name, server name
    STOP_ALL_APPS_WITHIN : "¿Detener todas las aplicaciones en {0}?", // resource
    STOP_ALL_APP_INSTS_WITHIN : "¿Detener todas las instancias de aplicaciones en {0}?", // resource
    STOP_ALL_SERVERS_WITHIN : "¿Detener todos los servidores en {0}?", // resource
    RESTART_ALL_INSTS_OF_APP : "¿Reiniciar todas las instancias de {0}?", // application name
    RESTART_APP_ON_SERVER : "¿Reiniciar {0} en {1}?", // app name, server name
    RESTART_ALL_APPS_WITHIN : "¿Reiniciar todas las aplicaciones en {0}?", // resource
    RESTART_ALL_APP_INSTS_WITHIN : "¿Reiniciar todas las instancias de aplicaciones en {0}?", // resource
    RESTART_ALL_SERVERS_WITHIN : "¿Reiniciar todos los servidores en ejecución en {0}?", // resource

    START_SELECTED_APPS : "¿Iniciar todas las instancias de las aplicaciones seleccionadas?",
    START_SELECTED_INSTANCES : "¿Iniciar las instancias de aplicaciones seleccionadas?",
    START_SELECTED_SERVERS : "¿Iniciar los servidores seleccionados?",
    START_SELECTED_SERVERS_LABEL : "Iniciar los servidores seleccionados",
    START_SELECTED_CLUSTERS : "¿Iniciar los clústeres seleccionados?",
    START_CLEAN_SELECTED_SERVERS : "¿Iniciar -clean para los servidores seleccionados?",
    START_CLEAN_SELECTED_CLUSTERS : "¿Iniciar -clean para los clústeres seleccionados?",
    STOP_SELECTED_APPS : "¿Detener todas las instancias de las aplicaciones seleccionadas?",
    STOP_SELECTED_INSTANCES : "¿Detener las instancias de aplicaciones seleccionadas?",
    STOP_SELECTED_SERVERS : "¿Detener los servidores seleccionados?",
    STOP_SELECTED_CLUSTERS : "¿Detener los clústeres seleccionados?",
    RESTART_SELECTED_APPS : "¿Reiniciar todas las instancias de las aplicaciones seleccionadas?",
    RESTART_SELECTED_INSTANCES : "¿Reiniciar las instancias de aplicaciones seleccionadas?",
    RESTART_SELECTED_SERVERS : "¿Reiniciar los servidores seleccionados?",
    RESTART_SELECTED_CLUSTERS : "¿Reiniciar los clústeres seleccionados?",

    START_SERVERS_ON_HOSTS : "¿Iniciar todos los servidores en los hosts seleccionados?",
    STOP_SERVERS_ON_HOSTS : "¿Detener todos los servidores en los hosts seleccionados?",
    RESTART_SERVERS_ON_HOSTS : "¿Reiniciar todos los servidores en ejecución en los hosts seleccionados?",

    SELECT_APPS_TO_START : "Seleccione las aplicaciones detenidas que iniciar.",
    SELECT_APPS_TO_STOP : "Seleccione las aplicaciones iniciadas que detener.",
    SELECT_APPS_TO_RESTART : "Seleccione las aplicaciones iniciadas que reiniciar.",
    SELECT_INSTANCES_TO_START : "Seleccione las instancias de aplicaciones detenidas que iniciar.",
    SELECT_INSTANCES_TO_STOP : "Seleccione las instancias de aplicaciones iniciadas que detener.",
    SELECT_INSTANCES_TO_RESTART : "Seleccione las instancias de aplicaciones iniciadas que reiniciar.",
    SELECT_SERVERS_TO_START : "Seleccione los servidores detenidos que iniciar.",
    SELECT_SERVERS_TO_STOP : "Seleccione los servidores iniciados que detener.",
    SELECT_SERVERS_TO_RESTART : "Seleccione los servidores iniciados que reiniciar.",
    SELECT_CLUSTERS_TO_START : "Seleccione los clústeres detenidos que iniciar.",
    SELECT_CLUSTERS_TO_STOP : "Seleccione los clústeres iniciados que detener.",
    SELECT_CLUSTERS_TO_RESTART : "Seleccione los clústeres iniciados que reiniciar.",

    STATUS : "Estado",
    STATE : "Estado:",
    NAME : "Nombre:",
    DIRECTORY : "Directorio",
    INFORMATION : "Información",
    DETAILS : "Detalles",
    ACTIONS : "Acciones",
    CLOSE : "Cerrar",
    HIDE : "Ocultar",
    SHOW_ACTIONS : "Mostrar acciones",
    SHOW_SERVER_ACTIONS_LABEL : "Acciones {0} de servidor",
    SHOW_APP_ACTIONS_LABEL : "Acciones {0} de aplicación",
    SHOW_CLUSTER_ACTIONS_LABEL : "Acciones {0} de clúster",
    SHOW_HOST_ACTIONS_LABEL : "Acciones {0} de host",
    SHOW_RUNTIME_ACTIONS_LABEL : "Acciones de {0} de tiempo de ejecución",
    SHOW_SERVER_ACTIONS_MENU_LABEL : "Menú de acciones de {0} de servidor",
    SHOW_APP_ACTIONS_MENU_LABEL : "Menú de acciones de {0} de aplicación",
    SHOW_CLUSTER_ACTIONS_MENU_LABEL : "Menú de acciones de {0} de clúster",
    SHOW_HOST_ACTIONS_MENU_LABEL : "Menú de acciones de {0} de host",
    SHOW_RUNTIME_ACTIONS_MENU_LABEL : "Menú de acciones de {0} de tiempo de ejecución",
    SHOW_RUNTIMEONHOST_ACTIONS_MENU_LABEL : "Menú de acciones de {0} de tiempo de ejecución en host",
    SHOW_COLLECTION_MENU_LABEL : "Menú de acciones de estado de {0} de recopilación",  // menu object id
    SHOW_SEARCH_MENU_LABEL : "Menú de acciones de estado de {0} de búsqueda",  // menu object id


    // A bit odd to have sentence casing without punctuation?
    UNKNOWN_STATE : "{0}: estado desconocido", // resourceName
    UNKNOWN_STATE_APPS : "{0} aplicaciones en estado desconocido", // quantity
    UNKNOWN_STATE_APP_INSTANCES : "{0} instancias de aplicaciones en estado desconocido", // quantity
    UNKNOWN_STATE_SERVERS : "{0} servidores en estado desconocido", // quantity
    UNKNOWN_STATE_CLUSTERS : "{0} clústeres en estado desconocido", // quantity

    INSTANCES_NOT_RUNNING : "{0} instancias de aplicaciones no están en ejecución", // quantity
    APPS_NOT_RUNNING : "{0} aplicaciones no están en ejecución", // quantity
    SERVERS_NOT_RUNNING : "{0} servidores no están en ejecución", // quantity
    CLUSTERS_NOT_RUNNING : "{0} clústeres no están en ejecución", // quantity

    APP_STOPPED_ON_SERVER : "{0} detenidos en servidores en ejecución: {1}", // appName, serverName(s)
    APPS_STOPPED_ON_SERVERS : "{0} aplicaciones detenidas en servidores en ejecución: {1}", // quantity, serverName(s)
    APPS_STOPPED_ON_SERVER : "{0} aplicaciones detenidas en servidores en ejecución.", // quantity
    NUMBER_RESOURCES : "{0} recursos", // quantity
    NUMBER_APPS : "{0} aplicaciones", // quantity
    NUMBER_SERVERS : "{0} servidores", // quantity
    NUMBER_CLUSTERS : "{0} clústeres", // quantity
    NUMBER_HOSTS : "{0} hosts", // quantity
    NUMBER_RUNTIMES : "{0} tiempos de ejecución", // quantity
    SERVERS_INSERT : "servidores",
    INSERT_STOPPED_ON_INSERT : "{0} detenido ejecutando {1}.", // NUMBER_APPS, SERVERS_INSERT

    APPSERVER_STOPPED_ON_SERVER : "{0} detenido al ejecutar servidor {1}", //appName, serverName
    APPCLUSTER_STOPPED_ON_SERVER : "{0} en clúster {1} detenido al ejecutar servidores: {2}",  //appName, clusterName, serverName(s)

    INSTANCES_STOPPED_ON_SERVERS : "{0} instancias de aplicaciones detenidas en servidores en ejecución.", // quantity
    INSTANCE_STOPPED_ON_SERVERS : "{0} instancia de aplicación no está en ejecución", // serverNames

    NOT_ALL_APPS_RUNNING : "{0}: no todas las aplicaciones están en ejecución", // serverName[]
    NO_APPS_RUNNING : "{0}: no hay aplicaciones en ejecución", // serverName[]
    NOT_ALL_APPS_RUNNING_SERVERS : "{0} servidores con no todas las aplicaciones en ejecución", // quantity
    NO_APPS_RUNNING_SERVERS : "{0} servidores sin aplicaciones en ejecución", // quantity

    COUNT_OF_APPS_SELECTED : "{0} aplicaciones seleccionadas",
    RATIO_RUNNING : "{0} en ejecución", // ratio ex. 1/2

    RESOURCES_SELECTED : "{0} seleccionados",

    NO_HOSTS_SELECTED : "No hay hosts seleccionados",
    NO_DEPLOY_RESOURCE : "No hay recursos para desplegar la instalación",
    NO_TOPOLOGY : "No hay ningún {0}.",
    COUNT_OF_APPS_STARTED  : "{0} aplicaciones iniciadas",

    APPS_LIST : "{0} aplicaciones",
    EMPTY_MESSAGE : "{0}",  // used to build a list of comma separated resource names
    APPS_INSTANCES_RUNNING_OF_TOTAL : "{0}/{1} instancias en ejecución",
    HOSTS_SERVERS_RUNNING_OF_TOTAL : "{0}/{1} servidores en ejecución",
    RESOURCE_ON_RESOURCE : "{0} en {1}", // resource name, resource name
    RESOURCE_ON_SERVER_RESOURCE : "{0} en el servidor {1}", // resource name, resource name
    RESOURCE_ON_CLUSTER_RESOURCE : "{0} en el clúster {1}", // resource name, resource name

    RESTART_DISABLED_FOR_ADMIN_CENTER: "Reiniciar está inhabilitado para este servidor porque aloja al Centro de administración",
    ACTION_DISABLED_FOR_USER: "Las acciones están inhabilitadas en este recurso porque el usuario no está autorizado",

    RESTART_AC_TITLE: "No se puede reiniciar el Centro de administración",
    RESTART_AC_DESCRIPTION: "{0} proporciona el Centro de administración y éste no se puede reiniciar a sí mismo.",
    RESTART_AC_MESSAGE: "Se reiniciarán el resto de servidores.",
    RESTART_AC_CLUSTER_MESSAGE: "Se reiniciarán todos los demás clústeres seleccionados.",

    STOP_AC_TITLE: "Detener el Centro de administración",
    STOP_AC_DESCRIPTION: "El servidor {0} es un controlador colectivo que ejecuta el Centro de administración. Si se detiene podría afectar a las operaciones de gestión colectiva de Liberty y hacer que el Centro de administración no estuviese disponible.",
    STOP_AC_MESSAGE: "¿Desea detener este controlador?",
    STOP_STANDALONE_DESCRIPTION: "El servidor {0} ejecuta el Centro de administración. Si se detiene, el Centro de administración dejará de estar disponible.",
    STOP_STANDALONE_MESSAGE: "¿Desea detener este servidor?",

    STOP_CONTROLLER_TITLE: "Detener controlador",
    STOP_CONTROLLER_DESCRIPTION: "El servidor {0} es un controlador colectivo. Si se detiene podría repercutir en las operaciones colectivas de Liberty.",
    STOP_CONTROLLER_MESSAGE: "¿Desea detener este controlador?",

    STOP_AC_CLUSTER_TITLE: "Detener clúster {0}",
    STOP_AC_CLUSTER_DESCRIPTION: "El clúster {0} contiene un controlador colectivo que ejecuta el Centro de administración.  Si se detiene podría repercutir en las operaciones de gestión colectiva y hacer que el Centro de administración no esté disponible.",
    STOP_AC_CLUSTER_MESSAGE: "¿Desea detener este clúster?",

    INVALID_URL: "La página no existe.",
    INVALID_APPLICATION: "La aplicación {0} ya no existe en el colectivo.", // application name
    INVALID_SERVER: "El servidor {0} ya no existe en el colectivo.", // server name
    INVALID_CLUSTER: "El clúster {0} ya no existe en el colectivo.", // cluster name
    INVALID_HOST: "El host {0} ya no existe en el colectivo.", // host name
    INVALID_RUNTIME: "El tiempo de ejecución {0} ya no existe en el colectivo.", // runtime name
    INVALID_INSTANCE: "La instancia de aplicación {0} ya no existe en el colectivo.", // application instance name
    GO_TO_DASHBOARD: "Ir al panel de instrumentos",
    VIEWED_RESOURCE_REMOVED: "¡Vaya! El recurso se ha eliminado o ya no está disponible.",

    OK_DEFAULT_BUTTON: "Aceptar",
    CONNECTION_FAILED_MESSAGE: "Se ha perdido la conexión con el servidor. La página ya no mostrará cambios dinámicos en el entorno. Renueve la página para restaurar la conexión y las actualizaciones dinámicas.",
    ERROR_MESSAGE: "Conexión interrumpida",

    // Used by standalone stop message dialog
    STANDALONE_STOP_TITLE : 'Detener el servidor',

    // Tags
    RELATED_RESOURCES: "Recursos relacionados",
    TAGS : "Etiquetas",
    TAG_BUTTON_LABEL : "Etiqueta {0}",  // tag value
    TAGS_LABEL : "Especifique etiquetas separadas por comas, espacios, intro o tabulación.",
    OWNER : "Propietario",
    OWNER_BUTTON_LABEL : "Propietario {0}",  // owner value
    CONTACTS : "Contactos",
    CONTACT_BUTTON_LABEL : "Contacto {0}",  // contact value
    PORTS : "Puertos",
    CONTEXT_ROOT : "Raíz de contexto",
    HTTP : "HTTP",
    HTTPS : "HTTPS",
    MORE : "Más",  // alt text for the ... button
    MORE_BUTTON_MENU : "Menú Más de {0}", // alt text for the menu
    NOTES: "Notas",
    NOTE_LABEL : "Nota {0}",  // note value
    SET_ATTRIBUTES: "Etiquetas y metadatos",
    SETATTR_RUNTIME_NAME: "{0} en {1}",  // runtime, host
    SAVE: "Guardar",
    TAGINVALIDCHARS: "Los caracteres '/', '<' y '>' no son válidos.",
    ERROR_GET_TAGS_METADATA: "El producto no puede obtener las etiquetas y metadatos actuales para el recurso.",
    ERROR_SET_TAGS_METADATA: "Se ha producido un error que ha impedido que el producto establezca las etiquetas y los metadatos.",
    METADATA_WILLBE_INHERITED: "Los metadatos están establecidos en la aplicación y se comparten en todas las instancias del clúster.",
    ERROR_ALT: "Error",

    // Graph Warning Messages
    GRAPH_SERVER_NOT_STARTED: "Las estadísticas actuales no están disponibles para este servidor porque está detenido. Inicie el servidor para comenzar a supervisarlo.",
    GRAPH_SERVER_HOSTING_APP_NOT_STARTED: "Las estadísticas actuales no están disponibles para esta aplicación porque su servidor asociado está detenido. Inicie el servidor para comenzar a supervisar esta aplicación.",
    GRAPH_FEATURES_NOT_CONFIGURED: "Aún no hay nada aquí. Supervise este recurso seleccionando el icono Editar y añadiendo métricas.",
    NO_GRAPHS_AVAILABLE: "No hay métricas disponibles que añadir. Intente instalar características adicionales de supervisión para que haya más métricas disponibles.",
    NO_APPS_GRAPHS_AVAILABLE: "No hay métricas disponibles que añadir. Intente instalar características adicionales de supervisión para que haya más métricas disponibles. Asimismo, asegúrese de que la aplicación está en uso.",
    GRAPH_CONFIG_NOT_SAVED_TITLE : "Cambios no guardados",
    GRAPH_CONFIG_NOT_SAVED_DESCR : "Tiene cambios no guardados. Si va a otra página, perderá los cambios.",
    GRAPH_CONFIG_NOT_SAVED_MSG : "¿Desea guardar los cambios?",

    NO_CPU_STATS_AVAILABLE : "Las estadísticas de uso de CPU no están disponibles para este servidor.",

    // Server Config
    CONFIG_NOT_AVAILABLE: "Para habilitar esta vista, instale la herramienta de configuración del servidor.",
    SAVE_BEFORE_CLOSING_DIALOG_MESSAGE: "¿Desea guardar los cambios en {0} antes de cerrar?",
    SAVE: "Guardar",
    DONT_SAVE: "No guardar",

    // Maintenance mode
    ENABLE_MAINTENANCE_MODE: "Habilitar modalidad de mantenimiento",
    DISABLE_MAINTENANCE_MODE: "Inhabilitar modalidad de mantenimiento",
    ENABLE_MAINTENANCE_MODE_DIALOG_TITLE: "Habilitar modalidad de mantenimiento",
    DISABLE_MAINTENANCE_MODE_DIALOG_TITLE: "Inhabilitar modalidad de mantenimiento",
    ENABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "Habilitar modalidad de mantenimiento en el host y todos sus servidores ({0} servidores)",
    ENABLE_MAINTENANCE_MODE_HOSTS_DESCRIPTION: "Habilitar modalidad de mantenimiento en los hosts y todos sus servidores ({0} servidores)",
    ENABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "Habilitar modalidad de mantenimiento en el servidor",
    ENABLE_MAINTENANCE_MODE_SERVERS_DESCRIPTION: "Habilitar modalidad de mantenimiento en los servidores",
    DISABLE_MAINTENANCE_MODE_HOST_DESCRIPTION: "Inhabilitar modalidad de mantenimiento en el host y todos sus servidores ({0} servidores)",
    DISABLE_MAINTENANCE_MODE_SERVER_DESCRIPTION: "Inhabilitar modalidad de mantenimiento en el servidor",
    BREAK_AFFINITY_LABEL: "Romper la afinidad con las sesiones activas",
    ENABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "Habilitar",
    DISABLE_MAINTENANCE_MODE_DIALOG_ENABLE_BUTTON_LABEL: "Inhabilitar",
    MAINTENANCE_MODE: "Modalidad de mantenimiento",
    ENABLING_MAINTENANCE_MODE: "Habilitando modalidad de mantenimiento",
    MAINTENANCE_MODE_ENABLED: "Modalidad de mantenimiento habilitada",
    MAINTENANCE_MODE_ALTERNATE_SERVERS_NOT_STARTED: "La modalidad de mantenimiento no se ha habilitado porque no se iniciaron los servidores alternativos.",
    MAINTENANCE_MODE_SELECT_FORCE_MESSAGE: "Seleccione Forzar para habilitar la modalidad de mantenimiento sin iniciar los servidores alternativos. Forzar podría violar las políticas de escala automática.",
    MAINTENANCE_MODE_FAILED: "La modalidad de mantenimiento no puede habilitarse.",
    MAINTENANCE_MODE_FORCE_LABEL: "Forzar",
    MAINTENANCE_MODE_CANCEL_LABEL: "Cancelar",
    MAINTENANCE_MODE_SERVERS_IN_MAINTENANCE_MODE: "En este momento hay {0} servidores en modalidad de mantenimiento.",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_EQUAL_OR_LESS_THEN_10: "Habilitando modalidad de mantenimiento en todos los servidores host.",
    MAINTENANCE_MODE_ENABLING_MAINTENANCE_MODE_ON_ALL_HOST_SERVERS_MORE_THAN_10: "Habilitando modalidad de mantenimiento en todos los servidores host.  Mostrar la vista Servidores para estado.",

    SERVER_API_DOCMENTATION: "Definición de API de servidor de vista",

    // objectView title
    TITLE_FOR_CLUSTER: "Clúster {0}", // cluster name
    TITLE_FOR_HOST: "Host {0}", // host name

    // objectView descriptor
    COLLECTIVE_CONTROLLER_DESCRIPTOR: "Controlador colectivo",
    LIBERTY_SERVER : "servidor de Liberty",
    NODEJS_SERVER : "Servidor Node.js",
    CONTAINER : "Contenedor",
    CONTAINER_LIBERTY : "Liberty",
    CONTAINER_DOCKER : "Docker",
    CONTAINER_NODEJS : "Node.js",
    LIBERTY_IN_DOCKER_DESCRIPTOR : "Servidor Liberty en un contenedor de Docker",
    NODEJS_IN_DOCKER_DESCRIPTOR : "Servidor Node.js en un contenedor de Docker",
    RUNTIME_LIBERTY : "Tiempo de ejecución de Liberty",
    RUNTIME_NODEJS : "Tiempo de ejecución de Node.js",
    RUNTIME_DOCKER : "Tiempo de ejecución en un contenedor de Docker"

});
