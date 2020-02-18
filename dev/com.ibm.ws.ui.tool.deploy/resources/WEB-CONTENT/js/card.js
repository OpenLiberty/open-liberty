var card = (function() {
    "use strict";

    /*
       generic card card.js

       create card with input inputVariable

       encode group name (is it workable?)
     */

    var __createCard = function(inputVariable) {
        // create new card by cloning existing template

        var id = idUtils.getCardId(inputVariable);
        console.log("__createCard card id ", id);
        var groupId = idUtils.getGroupId(inputVariable.group);
        console.log("__createCard group id ", groupId);

        /* template to be clone
        <section id="parameters" class="card">
                <section id="parametersLeftPane">
                    <header id="parameterBanner" data-externalizedString="DEPLOYMENT_PARAMETERS"></header>
                    <article id="parametersDescription" data-externalizedString="PARAMETERS_DESCRIPTION"></article>
                    <div id="inputVariablesContainer"></div>
                </section>

                <section id="parametersRightPane">
                </section>
        <section>
        */
        // id example: parameters_group1_card
        // query the template and set id
        var newCardElement = $("#parameters").clone().attr("id", id).removeClass("hidden");

        newCardElement.attr("tabindex", "0");
        newCardElement.find(".parametersLeftPane").attr("aria-labelledBy", groupId + "_parameterBanner");
        newCardElement.find(".parametersLeftPane").attr("id", groupId + "_parametersLeftPane");
        newCardElement.find(".parameterBanner").attr("id", groupId + "_parameterBanner");
        newCardElement.find(".parametersDescription").attr("id", groupId + "_parametersDescription");
        newCardElement.find(".parameterToggle").attr("id", groupId + "_parameterToggle");
        newCardElement.find(".parameterToggleController").attr("id", groupId + "_parameterToggleController");
        newCardElement.find(".parameterToggleUpload").attr("id", groupId + "_parameterToggleUpload");
        newCardElement.find(".inputVariablesContainer").attr("id", groupId + "_inputVariablesContainer");
        newCardElement.find(".parametersRightPane").attr("id", groupId + "_parametersRightPane");

        return newCardElement;
    };

    var __getCard = function(inputVariable) {

        var cardId = idUtils.getCardId(inputVariable);
        // "parameters_" + __getGroupId(inputVariable.group) + "_card; etc parameters_group1_card
        console.log("__getCard cardId ", cardId);
        // search for cardId, if already exist return, otherwise create new one
        var theCard = $("#" + cardId);
        console.log("__getCard existingCard ", theCard);
        console.log("__getCard existingCard.length ", theCard.length);

        // create new card
        if (theCard.length === 0) {
            // search for all cards
            var deplParamsCards = $('[id$="_card"]');
            console.log("deplParamsCards ", deplParamsCards);
            // get the last card to insert the new card after
            // otherwise append after the default template
            var lastCard = $("#parameters");
            if (deplParamsCards.length > 0) {
                lastCard = deplParamsCards[deplParamsCards.length - 1];
                console.log("lastCard ", lastCard);
            }
            theCard = __createCard(inputVariable);
            console.log("append card after ", lastCard.id);
            theCard.insertAfter(lastCard);
        }

        // just for testing
        //console.log("testing ", $("#contents"));
        //$("#contents").append(theCard);
        //console.log("testing ", $("#parameters"));
        //$("#parameters").append(theCard);
        //theCard.insertAfter($("#parameters"));
        //var deplParams = $('[id$="_card"]');
        //console.log("deplParams ", deplParams);
        //$("#" + cardId).insertAfter($("#parameters"));
        //$("#" + cardId).show();
        return theCard;
    };
    var addInputToContainer = function(container, inputVariable, persistedValue, confirmContainer, lastDeployLocal){
        var typeObject;
        var appendAtEnd = true;

        if (inputVariable.type === "password") {
            typeObject = password.create(inputVariable);
        } else if (inputVariable.type === "file") {
            typeObject = file.create(inputVariable, lastDeployLocal);
        } else if (inputVariable.type === "dockerImage") {
            typeObject = docker.create(inputVariable);
            appendAtEnd = false;
        } else if (inputVariable.type === "cluster") {
            console.log("calling cluster to create new type");
            typeObject = cluster.create(inputVariable);
            appendAtEnd = false;
        } else {
            typeObject = string.create(inputVariable);
        }

        var input = typeObject.getInput();

        // Wrap the input and clear field button in a div
        var inputDivId = idUtils.getInputDivId(typeObject);
        var div = $("<div class='inputDiv' id='" + inputDivId + "'>");
        div.append(input);

        if(inputVariable.type !== "password"){
          var clearFieldButtonId = idUtils.getClearFieldButtonId(typeObject);
          var clearFieldButton = $("<a class='parameterClearFieldButton' id='" + clearFieldButtonId + "'>");
          clearFieldButton.attr("tabindex" , "0");
          clearFieldButton.attr("aria-label", messages.CLEAR_FIELD_BUTTON_ARIA);
          clearFieldButton.attr("title", messages.CLEAR_FIELD_BUTTON_ARIA);
          var img = imgUtils.getSVGSmallObject('gray-selected');
          img.setAttribute("role", "button");
          img.setAttribute("pointer-events", "none");
          clearFieldButton.append(img);
          div.append(clearFieldButton);
        }

        // Persisted value from the last deployment
        if(persistedValue){
          input.val(persistedValue);
        }
        var description = typeObject.getDescription();
        var label = typeObject.getLabel();
        // Note: it is important that the input is put in the container first before calling
        // setObjectForType
        if(appendAtEnd){
            container.append(label);
            container.append(div);
            container.append(description);
        }
        else{
            container.prepend(description);
            container.prepend(div);
            container.prepend(label);
        }

        string.setObjectForType(input, typeObject);

        if (inputVariable.type === "password") {
            var confirmPasswordInput = typeObject.getConfirmPasswordInput();
            var confirmPasswordInputDivId = confirmPasswordInput.prop('id') + "_inputDiv";
            var confirmInputDiv = $("<div class='inputDiv' id='" + confirmPasswordInputDivId + "'>");
            confirmContainer.append(typeObject.getConfirmPasswordLabel());

            confirmInputDiv.append(confirmPasswordInput);
            confirmContainer.append(confirmInputDiv);
            string.setObjectForType(confirmPasswordInput, typeObject);
        }

        typeObject.addInputListener();
        if(inputVariable.type !== "password"){
          typeObject.addClearButtonListener(); // 'X' button used to clear fields
        }
    };

    var clearAndHideOldCards = function() {
        var parameterCards = $(".parameters");
        parameterCards.css('min-height', '0'); // Reset the min-height for calculating the height of the card for the next rule loaded
        parameterCards.addClass("hidden");
        parameterCards.removeClass("parametersGrayBackground");

        var inputVariablesDeploymentToggle = $(".parameterToggle");
        inputVariablesDeploymentToggle.addClass("hidden");

        var inputVariablesContainer = $(".inputVariablesContainer");
        inputVariablesContainer.empty();

        var parametersRightPane = $(".parametersRightPane");
        parametersRightPane.empty();

        var securityCard = $("#security");
        securityCard.hide();

        var securityInputContainer = $("#securityInputContainer");
        securityInputContainer.empty();

        var securityInputConfirmContainer = $("#securityInputConfirmContainer");
        securityInputConfirmContainer.empty();

        var cards = $(".parameters.card").addClass("hidden");

        dockerUtils.clearDockerImages();
        clusterUtils.clearClusters();
        hostUtils.resetHostList();
    };

    // listener to set title attribute to show the full label when hover over an overflow label
    var __addLabelListener = function() {
        $('.parameters label').on('mouseenter',  function(){
            var $this = $(this);

            if (this.offsetWidth < this.scrollWidth && !$this.attr('title')){
                $this.attr('title', $this.text());
            }
        });

        $('#security label').on('mouseenter', function(){
            var $this = $(this);

            if (this.offsetWidth < this.scrollWidth && !$this.attr('title')){
                $this.attr('title', $this.text());
            }
        });
    };

    return {
        /*
         * code usage example:
         *   var myCard = card.create(inputVariable);

         */
        create: __getCard,
        addLabelListener: __addLabelListener,
        addInputToContainer: addInputToContainer,
        clearAndHideOldCards: clearAndHideOldCards
    };

})();
