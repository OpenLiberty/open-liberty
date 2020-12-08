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
    ERROR : "Error",
    ERROR_STATUS : "{0} Error: {1}", //responseStatusCode, response.text
    ERROR_URL_REQUEST_NO_STATUS : "Se ha producido un error al solicitar {0}.", // url
    ERROR_URL_REQUEST : "{0} Se ha producido un error al solicitar {1}.", //responseStatusCode, url
    ERROR_SERVER_UNAVAILABLE : "El servidor no ha respondido en el tiempo asignado.",
    ERROR_APP_NOT_AVAILABLE : "La aplicación {0} ya no está disponible para el servidor {1} en el host {2} del directorio {3}.", //appName, serverName, hostName, userdirName
    ERROR_APPLICATION_OPERATION : "Se ha producido un error al intentar {0} la aplicación {1} en el servidor {2} del host {3} en el directorio {4}.", //operation, appName, serverName, hostName, userdirName
    CLUSTER_STATUS : "El clúster {0} está {1}.", //clusterName, clusterStatus
    CLUSTER_UNAVAILABLE : "El clúster {0} ya no está disponible.", //clusterName
    STOP_FAILED_DURING_RESTART : "La detención no se completó satisfactoriamente durante el reinicio.  El error fue: {0}", //errMsg
    ERROR_CLUSTER_OPERATION : "Se ha producido un error al intentar {0} el clúster {1}.", //operation, clusterName
    SERVER_NONEXISTANT : "El servidor {0} no existe.", // serverName
    ERROR_SERVER_OPERATION : "Se ha producido un error al intentar {0} el servidor {1} en el host {2} en el directorio {3}.", //operation, serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_SET_MAINTENANCE_MODE : "La modalidad de mantenimiento para el servidor {0} del host {1} en el directorio {2} no se ha establecido debido a un error.", //serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_UNSET_MAINTENANCE_MODE : "El intento de anular el establecimiento de la modalidad de mantenimiento para el servidor {0} del host {1} en el directorio {2} no se ha completado debido a un error.", //serverName, hostName, userdirName
    ERROR_HOST_OPERATION_SET_MAINTENANCE_MODE : "La modalidad de mantenimiento para el host {0} no se ha establecido debido a un error.", // hostName
    ERROR_HOST_OPERATION_UNSET_MAINTENANCE_MODE : "El intento de anular el establecimiento de la modalidad de mantenimiento para host {0} no se ha completado debido a un error.", // hostName
    // Dialog messages on operation errors
    SERVER_START_ERROR: "Error durante el inicio del servidor.",
    SERVER_START_CLEAN_ERROR: "Error durante el inicio del servidor --clean.",
    SERVER_STOP_ERROR: "Error durante la detención del servidor.",
    SERVER_RESTART_ERROR: "Error durante el reinicio del servidor.",
    // Used by standalone stop operation
    STANDALONE_STOP_NO_MBEAN : 'El servidor no se ha detenido. La API necesaria para detener el servidor no estaba disponible.',
    STANDALONE_STOP_CANT_DETERMINE_MBEAN : 'El servidor no se ha detenido. La API necesaria para detener el servidor no ha podido determinarse.',
    STANDALONE_STOP_FAILED : 'La operación de detención del servidor no se ha completado satisfactoriamente. Compruebe los registros del servidor para conocer los detalles.',
    STANDALONE_STOP_SUCCESS : 'El servidor se ha detenido satisfactoriamente.',
});

