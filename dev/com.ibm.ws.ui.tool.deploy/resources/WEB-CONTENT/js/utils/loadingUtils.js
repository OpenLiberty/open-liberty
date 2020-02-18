var loadingUtils = (function() {
    "use strict";
    
    var __addLoader = function(rootListId) {
        $('#' + rootListId + "_listViewError").hide();
        $('#' + rootListId + "_listViewList").show();       
        $('#' + rootListId + "_listViewList").empty().addClass('loader');
    };
    
    var __removeLoader = function(rootListId) {
        if ($('#' + rootListId + "_listViewList").hasClass('loader')) {
            $('#' + rootListId + "_listViewList").removeClass('loader');
        }
    };
    
    var __displayFirstInputForRightHandPanel = function(rootListId) {
        // figure out the first list input field on the card and render its list
        var groupId = "default";
        if (rootListId.indexOf("_") > 0) {
            groupId = rootListId.substring(0, rootListId.indexOf("_"));
        }
        var inputs = $('#' + groupId + "_inputVariablesContainer input");
        var i, input, typeObject;
        for(i = 0; i < inputs.length; i++) {
            input = inputs.get(i);
            typeObject = string.retrieveObjectForType(input); 

            if (typeObject.inputVariable.type === "dockerImage") {
                // if the docker repository configuration is not provided, then local image is used and no image list
                // is provided. 
                if (typeObject.isLoadingDone() && !dockerUtils.isLocal(rootListId)) {
                    typeObject.renderDockerImages();
                    break;
                }
            } else if (typeObject.inputVariable.type === "cluster") {
                if (typeObject.isLoadingDone()) {
                    typeObject.renderClusters();
                    break;
                }
            } else if (typeObject.inputVariable.type === "file") {
                typeObject.renderFileUpload();
                break;
            } 
        }
    };
    
    return {
        displayLoader: __addLoader,
        removeLoader: __removeLoader,
        displayFirstInputForRightHandPanel: __displayFirstInputForRightHandPanel
    };
})();
