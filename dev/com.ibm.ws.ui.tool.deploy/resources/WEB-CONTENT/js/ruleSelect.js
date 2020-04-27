var ruleSelect = (function(){
       "use strict";

    // TODO: Modify Data Constructs -- Will Aid in keeping in sync this.data
    var sliderMinHeight = 380,
        sliderMaxHeight = 625,
        defaultBackgroundColor = '#323232',
        defaultListItemFontColor = '#ffffff',
        svgFills = {
            '#562E71': '#311940',
            '#4B853D': '#345D2A',
            '#EFC100': defaultBackgroundColor
        },
        debounce = function debounce(func, wait, immediate) {
            var timeout;

            return function() {
                var context = this, args = arguments;
                var later = function() {
                    timeout = null;
                    if (!immediate){
                           func.apply(context, args);
                    }
                };
                var callNow = immediate && !timeout;

                clearTimeout(timeout);
                timeout = setTimeout(later, wait);
                if (callNow){
                       func.apply(context, args);
                }
            };
        };

    ////////////////////////
    ////////////////////////
    // Commenting Structure
    //
    // -- Eventing Functions
    // -- Update UI Methods
    // -- Large Animation Functions
    // -- Utility Functions
    // -- Helper Functions
    ////////////////////////
    ////////////////////////

    var App= {
        data: null,
        view: 'server', // server, deploy, preview, final
        init: function(data) {
            if (data){
                this.data = data;
            }

            // Slider Components
            this.sectionSlider = $('section.slider');
            this.slider = $('.slider--list');

            this.editServerButton = $('.server.bg--lighter div.positioning');
            this.editRuleButton = $('.deploy.bg--lighter div.positioning');

            this.serverSelectSection = $('.server--list-types');
            this.serverSelectDefaultList = $('.slider--list-item.server .list-types--default');
            this.serverSelectCustomList = $('.slider--list-item.server .list-types--custom');

            this.ruleSelectSection = $('.deploy--list-types');
            this.ruleSelectDefaultList = $('.slider--list-item.deploy .list-types--default');
            this.ruleSelectCustomList = $('.slider--list-item.deploy .list-types--custom');

            this.serverAndDeployListItems = $('li.list-item--arrow li a');
            this.serverListItems = $('.server--list-types li a');
            this.deployListItems = $('.deploy--list-types li a');
            this.typeListItem = $('.slider--list-item.list-item--arrow');
            this.serverListItem = $('.slider--list-item.bg--lighter');
            this.initialOverlays = $('.initial--overlay');
            this.chooseVerbiage = this.initialOverlays.find('.choosing--headline');
            this.chosenVerbiage = this.initialOverlays.find('.choosing--selected');
            this.editOverlay = $('.choosing--edit');
            this.editButtons = $('.headline-edit');
            this.backButton = $('.button--back');

            // Scroll Components
            this.deployScrollableSection = $('.deploy--list-types');
            this.deployScrollAnchors = $('.js--scroll');

            // Confirmation Components
            this.reviewBar = $('.slider--submission-bar');
            this.reviewButton = $('.slider--submission-bar .button--done');
            this.sliderGradient = $('.submission--gradient');
            this.confirmationOverlays = $('.js--confirmation-overlay');
            this.chosenVerbiageOverlay = this.confirmationOverlays.find('.choosing--selected');
            this.recapNav = $('.js--nav');
            this.editNavAnchor = $('.js--edit-selections');
            this.selectedRuntimeText = $('.server--selected .headline-top--selected');
            this.selectedPackageText = $('.deploy--selected .headline-top--selected');

            // Lock to prevent onHover while the onClick is running
            this.handlingClick = null;
            // Breadcrumb
            this.breadcrumbRuntime = $('.nav--selections .js--nav__server');
            this.breadcrumbRule = $('.nav--selections .js--nav__deploy');

            // Form Overlay
            this.formOverlay = $('.js--form--overlay');
            this.form = $('.form');

            this.handleInitialState();
        },
        handleInitialState: function() {
            var self = this,
                findNodesFromPassedInData = function(idx, data){
                    var node = $.map(self.serverListItems, function(el, idx){
                        var $element = $(el);

                        if ( data.dataTitle === $element.attr('title')) {
                            var obj = self.buildDataObject( $element );

                            self.updateSelectedTextContainer(obj);
                            self.transitionSelectedIn(obj);
                            self.updateBackButtonUI(obj);
                        }
                    });
                },
                findDeployNodesFromPassedInData = function(idx, data){
                    var node = $.map(self.deployListItems, function(el,idx){
                        var $element = $(el);

                        if ( data.dataTitle === $element.attr('title')) {
                            var obj = self.buildDataObject( $element );

                            self.updateSelectedTextContainer(obj);
                            self.transitionSelectedIn(obj);
                            self.updateBackButtonUI(obj);
                        }
                    });
                };

            if (this.data) { //if persistence data found
                ruleSelectUtils.generateContainerList(self.data.server.dataTitle); //TODO: may need to use dataRuntime but it was undefined

                $.each(this.data, findNodesFromPassedInData); //updates this.data to contain more information than the passed in info
                $.each(this.data, findDeployNodesFromPassedInData); //updates this.data to contain more information than the passed in info

                this.bindEvents();
                this.updateBreadCrumb();
                this.initializeBreadCrumb();
                this.accessibilityAccess(true);
                this.updateTypeListItemsUI(true);
                this.setView('final');
                this.accessibilityTabbingOrder();
                this.slider.velocity({
                    marginLeft: this.calculateMarginLeft('deploy')
                });
                this.animateToBreadCrumb();
            } else {
                this.bindEvents();
                this.shouldScrollShowOnDeployLists();
                this.initializeSliderOnPageLoad();
                utils.disableForm('ruleSelect');
                this.setView('server');
                this.accessibilityTabbingOrder();
                this.sectionSlider.attr('aria-label', utils.formatString(messages.RULESELECT_PANEL_ARIA, [messages.RULESELECT_OPEN]));
            }
            this.setAriaLabels();
            this.handleOverflow();
        },
        bindEvents: function() {
            $(window).resize(debounce(this.handleBrowserResize.bind(this), 250));

            this.deployScrollAnchors.on('click', this.handleDeployScroll.bind(this));
            this.backButton.on('click', this.handleBackButton.bind(this));
            this.editOverlay.on('click', this.handleEditOverlay.bind(this));
            this.reviewButton.on('click', this.handleConfirmationofSelectionsMade.bind(this));
            this.editNavAnchor.on('click', this.handleNavEditSelection.bind(this));
            this.recapNav.find('.js--nav__server, .js--nav__deploy').on('click', this.handleNavSelection.bind(this));

            this.initialOverlays.find('.positioning').keypress(this.handleEditOverlayTabbing.bind(this));

        },
        bindServerEvents: function() {
            this.serverListItems = $('.server--list-types li a');
            this.serverListItems.hover(this.handleServerAndDeployListItems.bind(this));
            this.serverListItems.on('click', this.handleClick.bind(this));
            this.serverListItems.focusin(this.handleServerAndDeployListItems.bind(this));
            this.serverListItems.focusout(this.focusOffAnimation.bind(this));
            //TODO: bug where if focused and you perform hoverOff, text turns white instead of staying correct color
        },
        bindDeployEvents: function() {
            this.deployListItems = $('.deploy--list-types li a');
            this.deployListItems.hover(this.handleServerAndDeployListItems.bind(this), this.hoverOffAnimation.bind(this));
            this.deployListItems.on('click', this.handleClick.bind(this));
            this.deployListItems.focusin(this.handleServerAndDeployListItems.bind(this));
            this.deployListItems.focusin(this.focusOnAnimation.bind(this));
            this.deployListItems.focusout(this.focusOffAnimation.bind(this));
        },
        bindBreadcrumbEvents: function() {
            this.breadcrumbRuntime.hover(this.breadcrumbHoverOnAnimation.bind(this), this.breadcrumbHoverOffAnimation.bind(this));
            this.breadcrumbRule.hover(this.breadcrumbHoverOnAnimation.bind(this), this.breadcrumbHoverOffAnimation.bind(this));
        },
        // Handles overflow of initial text
        handleOverflow: function() {
            // Only disable wrapping for Japanese, Russian, and Romanian
            var deployHeadlines = $('.headline-top');
            this.checkElementForOverflow(deployHeadlines, "headline-top--overflow");

            var deployHeadlinesSelected = $('.headline-top--selected');
            this.checkElementForOverflow(deployHeadlinesSelected, "headline-top--selected--overflow");
        },
        // Checks if the text is overflowing each element in the jQueryObj, if so add overflow class to scale the text
        checkElementForOverflow : function(jQueryObj, overflowClass){
            var languageCode = globalization.getLanguageCode();
            if(jQueryObj){
                jQueryObj.each(function(index){
                    var elem = $(this);
                    var val = elem[0].innerHTML;
                    var words = val.split(" ");

                    if(languageCode === 'ja' && index === 1){
                        elem.addClass(overflowClass);
                    }
                    else{
                        var container = elem.parents(".initial--overlay");
                        var elemLeft = elem.offset().left;
                        var containerLeft = container.offset().left;

                        elem.removeClass(overflowClass);

                        var i = 0;
                        while(i < words.length){
                            // Calculate the width of the word's text without the empty space
                            var word = words[i];
                            var span = document.createElement('span');
                            span.style.fontSize = elem.css("fontSize");
                            span.style.fontFamily = elem.css("font-family");
                            span.innerHTML = word;
                            document.body.appendChild(span);
                            var wordWidth = span.offsetWidth;

                            if(elem.innerWidth() < elem[0].scrollWidth || (containerLeft + container.outerWidth()) < (elemLeft + wordWidth)){
                                elem.addClass(overflowClass);
                                document.body.removeChild(span);
                                break;
                            }

                            document.body.removeChild(span);
                            i++;
                        }
                    }
                });
            }
        },


        ////////////////////////
        // Eventing Functions
        // 1. Resize -- Window Resize
        // 2. Hover -- Deploy/Server List Items
        // 3. Click -- Scrolling within Deploy List Items
        // 4. Hover -- Deploy/Server List Items
        // 5. Click -- Deploy/Server List Items
        // 6. Click -- Back Button on Deploy View
        // 7. Click -- Edit Overlays on Preview
        // 8. Click -- Done Button
        // 9. Click -- Bread Crumb
        // 10. Hover On -- Next Button, Invert Colors
        // 11. Hover Off -- Next Button, Invert Colors
        // 12. Keypress -- Enter Key on Edit Overlay
        ////////////////////////

        ////////////////////////
        // 1. Resize -- Window Resize
        ////////////////////////
        /**
        ** @event resize - browser
        ** @desc Adjust 'margin-left' when browser is resized
        **/
        handleBrowserResize: function() {
            var view = this.view,
                marginLeft;

            if (view === 'deploy') {
                marginLeft = this.calculateMarginLeft('server');
            } else if (view === 'preview') {
                marginLeft = this.calculateMarginLeft('preview');
            } else if (view === 'final') {
                marginLeft = this.calculateMarginLeft('preview');
            }

            $('.slider--list').velocity({
                marginLeft: marginLeft
            });

            // restore original text in case window resized larger, and rebind dotdotdot
            this.selectedRuntimeText.trigger("destroy.dot");
            var runtimeContent = this.selectedRuntimeText.triggerHandler("originalContent");
            if (runtimeContent) {
                this.selectedRuntimeText[0].innerHTML = (runtimeContent.text());
                this.selectedRuntimeText.dotdotdot({ watch: "window", height: "173px" });
            }

            this.selectedPackageText.trigger("destroy.dot");
            var packageContent = this.selectedPackageText.triggerHandler("originalContent");
            if (packageContent) {
                this.selectedPackageText[0].innerHTML = (packageContent.text());
                this.selectedPackageText.dotdotdot({ watch: "window", height: "173px" });
            }

            this.handleOverflow();
        },
        ////////////////////////
        // 2. Hover -- Deploy/Server List Items
        ////////////////////////
        /**
        ** @event hover - deploy/server list items
        ** @desc each list item has specific color associated to it on hover
        **/
        hoverOnAnimation: function(e) {
            var $element = this.isElementSpan(e),
                dataLiColor = this.getDataLiColor($element);

            if (!$element.hasClass('js--selected')) {
                $element.stop().velocity({
                    color: dataLiColor,
                    opacity: 0.75
                }, { duration: 'fast' });
            }
        },
        hoverOffAnimation: function(e) {
            var $element = this.isElementSpan(e);

            $element.stop().velocity({
                opacity: 1
            }, {duration: 'fast'});

            if (!$element.hasClass('js--selected')) {
                $element.stop().velocity({
                    color: defaultListItemFontColor
                }, { duration: 'fast' });
            }
        },

        focusOnAnimation: function(e) {
            var $element = this.isElementSpan(e),
                dataLiColor = this.getDataLiColor($element);

            if (!$element.hasClass('js--selected')) {
                $element.stop().velocity({
                    color: dataLiColor,
                    opacity: 0.75
                }, { duration: 'fast' });
            }
        },
        focusOffAnimation: function(e) {
            var $element = this.isElementSpan(e);

            if (!$element.hasClass('js--selected')) {
                $element.stop().velocity({
                    color: defaultListItemFontColor,
                    opacity: 1
                }, { duration: 'fast' });
            }
        },
        ////////////////////////
        // 3. Click -- Scrolling within Deploy List Items
        ////////////////////////
        /**
        ** @event click - deploy list items
        ** @desc scroll within deploy list items
        **/
        handleDeployScroll: function(e){
            e.preventDefault();
            e.stopPropagation();

            var $element = $(e.target),
                isSvg = $element.is('polyline') || $element.is('svg'),
                scrollTop = this.deployScrollableSection.scrollTop(),
                totalHeight = this.getDeployListInnerContentsHeight(),
                direction;

            $element = isSvg ? $element.closest('.js--scroll') : $element;
            direction = $element.hasClass('js--scroll-top') ? 'up' : 'down';

            if (direction === 'up') {
                if (scrollTop === 0) {
                    return;
                }
                this.deployScrollableSection.animate({
                    scrollTop: scrollTop -= 200
                });
            } else {
                if (scrollTop === totalHeight) {
                    return;
                }

                this.deployScrollableSection.animate({
                    scrollTop: scrollTop += 200
                });
            }
        },
        ////////////////////////
        // 4. Hover -- Deploy/Server List Items
        ////////////////////////
        /**
        ** @event hover - deploy/server list items
        ** @desc hovering over list item should change font color and background color
        **/
        handleServerAndDeployListItems: function(e) {
            e.preventDefault();

            // If the user hovers over something of the same type while the code is handling the onClick for a Server/Deploy list item, ignore event
            if(this.handlingClick && this.handlingClick.getAttribute('data-type') === e.target.getAttribute('data-type')){
                return;
            }

            //remove dotdotdot binding to prevent text color reset on new bind
            this.selectedRuntimeText.trigger("destroy.dot");
            this.selectedPackageText.trigger("destroy.dot");

            // Purpose: Currently, we are omitting the selection of 'Python'.
            var $element = this.isElementSpan(e),
                isActiveLink = $element.data('active');

            if (isActiveLink && isActiveLink === 'non-active') {
                return false;
            }

            $element.velocity({
                opacity: 1
            }, { duration: '0'});

            var obj = this.buildDataObject($element);
            this.handleSelectedServerOrDeployListItems(obj);
            this.setEditButtonAria();

            this.handleOverflow();

            //rebind dotdotdot for multi-line ellipsis
            this.selectedRuntimeText.dotdotdot({ watch: "window", height: "173px" });
            this.selectedPackageText.dotdotdot({ watch: "window", height: "173px" });
        },

        ////////////////////////
        // 5. Click -- Deploy/Server List Items
        ////////////////////////
        /**
        ** @desc User clicks 'Next' -- data type determines path
        ** @param obj -- determined by click event
        **/
        handleClick: function(e, isNavBarClick) {
            this.handlingClick = e.target;
            var self = this;
            var $element = $(e.target);
            var isBreadCrumb = $element.hasClass('js--nav__deploy');
            // HACK: Trick 'selectionMade' if Bread Crumbs is for Deploy Selection
            var dataType = isBreadCrumb ? 'server' : this.getDataType( $element );
            var selectionMade = isBreadCrumb ? 'server' : this.verifySelectionHasBeenMade(dataType);
            var marginLeft = this.calculateMarginLeft(dataType);

            // var elementToFocus = this.typeListItem.last().find('.js--selected');

            // check if 'deploy' already has selection made when on server
            if (selectionMade && dataType === 'server') {
                // If you start on Server Selection, moving over to Deploy Selection
                // the button will not be active. So need to activate if selection has been made.

                // this.clearDeploySelection(e);

                //generate packages from selected server
                ruleSelectUtils.generateContainerList(this.data__server.dataRuntime, isNavBarClick);

                var verifyDeploySelection = this.chooseVerbiage.last().css('visibility') === 'visible';
            }

            if (selectionMade) {

                if (dataType === 'deploy') {
                    this.setView('preview');
                    this.accessibilityTabbingOrder();
                    this.moveToPreviewMode();
                } else {
                    this.clearDeploySelection(e);
                    this.setView('deploy');
                    this.shouldScrollShowOnDeployLists();
                    this.accessibilityTabbingOrder();
                }

                this.slider.stop().velocity({
                    marginLeft: marginLeft
                },{
                    complete: function () {
                        if (self.view === 'deploy') {
                            // If the deploy package data has been set then set the background
                            if(self.data.deploy){
                              var obj = self.data.deploy;
                              self.transitionSelectedIn(obj);
                              self.updateSelectedTextContainer(obj);
                            }
                            self.handlingClick = null;
                            // self.setFocusOnSelectedListItem(self.typeListItem.last());
                       }
                       self.handlingClick = null;
                  }
               });
            } else {
                self.handlingClick = null;
                return;
            }
        },
        ////////////////////////
        // 6. Click -- Back Button on Deploy View
        ////////////////////////
        /**
        ** @desc Back Button on Deploy Selection
        ** @param obj -- determined by click event
        **/
        handleBackButton: function(e) {
            e.preventDefault();

            this.setView('server');
            this.accessibilityTabbingOrder();

            this.slider.velocity({
                marginLeft: 0
            });
        },
        ////////////////////////
        // 7. Click -- Edit Overlays on Preview
        ////////////////////////
        /**
        ** @event hover - edit overlay
        ** @desc User clicks 'Edit' on Preview of selected Server/Deploy Type
        ** @param obj -- determined by click event
        **/
        handleEditOverlay: function(e) {
            this.editOverlay.off('click');
            var self = this,
                $element = $(e.target),
                dataType = this.getDataType($element),
                marginLeft = 0;

            // HACK: Logic to determine marginLeft
            if ($element.hasClass('headline-edit')) {
                $element = $element.parent();
                dataType = this.getDataType($element);
            }
            if (!$element.hasClass('choosing--edit')) {
                $element = $element.parent();
            }
            if ($element.hasClass('positioning--wrapper') && !$element.hasClass('choosing--edit')) {
                $element = $element.find('choosing--edit');
            }

            if (dataType === 'deploy') {
                var width = this.slider.find('li').first().width();
                marginLeft = -Math.abs(width * 2);
            }
            // HACK: End -- Logic to determine marginLeft

            this.slider.velocity({
                marginLeft: marginLeft
            }, {
                complete: function() {
                    self.setView(dataType);
                    self.toggleReviewStateClassonBody();

                    // Custom
                    self.accessibilityTabbingOrder();

                    // if (dataType === 'deploy') {
                    //     self.setFocusOnSelectedListItem( self.typeListItem.last() );
                    // }
                }
            });

            this.handleBackButtonDisplay(1);
        },
        ////////////////////////
        // 8. Click -- Done Button
        ////////////////////////
        /**
        ** @event click - User selected 'Done' on 'Preview'
        ** @desc each list item has specific color associated to it on hover
        **/
        handleConfirmationofSelectionsMade: function(e) {
            e.preventDefault();

            var id = ruleSelectUtils.getIdFromIndex(this.data__deploy.dataIndex);
            var persistedParameters = ruleSelectUtils.getPersistedParameters();
            if(persistedParameters){
              persistedParameters = persistedParameters[id];
            }

            ruleSelectUtils.renderRule(this.data__deploy.dataIndex, persistedParameters);

            //Save persistent data
            var persist = { runtime: this.data__server.dataRuntime, id: id};
            console.log('Saving persistent data', persist);
            userConfig.save('lastDeploy', persist);

            this.toggleReviewStateClassonBody();
            this.animateTextWithBounceEffect();
            this.setView('final');
            this.accessibilityTabbingOrder();
            utils.enableForm();
        },
        ////////////////////////
        // 9. Click -- Bread Crumb
        ////////////////////////
        /**
        ** @event click - User has clicked on Bread Crumb
        ** @desc Bread Crumb selection determines where to slide (server|deploy)
        **/
        handleNavSelection: function(e) {
            e.preventDefault();
            this.editOverlay.off('click');

            var self = this,
                $element = $(e.target),
                dataType = $element.data('type');

            // Animation
            this.handleNavEditSelection(e);

            // Set View for Tabbing Order
            this.setView(dataType);
            this.accessibilityTabbingOrder();

            setTimeout(function(){
                if (dataType === 'server') {
                    self.handleBackButton(e);
                }
                else {
                    self.handleClick(e, true);
                }

                if (dataType) {
                    // Remove the 'Done' Bar
                    self.toggleReviewStateClassonBody();

                    // Prep and Display the Back Button for Future use
                    self.handleBackButtonDisplay(1);
                }
            }, 1200);
        },
        // 9.a 9.b Hover Breadcrumb
        breadcrumbHoverOnAnimation: function(e) {
            var $element = $(e.target),
                dataType = $element.data('type');

            var hoverColor;
            if (dataType === 'server') {
                   hoverColor = this.data__server.dataBreadcrumbHoverColor;
            } else if (dataType === 'deploy') {
                hoverColor = this.data__deploy.dataBreadcrumbHoverColor;
            }

            $element.stop().velocity({
                color: hoverColor
            }, { duration : 50});
        },
        breadcrumbHoverOffAnimation: function(e) {
            var $element = $(e.target),
                dataType = $element.data('type');

            var color;
            if (dataType === 'server') {
                color = this.data__server.dataBreadcrumbColor;
            } else if (dataType === 'deploy') {
                color = this.data__deploy.dataBreadcrumbColor;
            }

            $element.stop().velocity({
                color: color
            }, { duration : 100});
        },
        ////////////////////////
        // 10. Hover On -- Next Button, Invert Colors
        ////////////////////////
        /**
        ** @event hover - deploy/server next buttons
        ** @desc invert font color to bg color, btn bg color goes to white
        **/
        handleNextButtonHoverOn: function(e) {
            e.preventDefault();

            var $element = $(e.target),
                isDisabled = $element.hasClass('button--disabled');

            if (!isDisabled) {
                var dataType = $element.data('type'),
                    data = this.data[dataType];

                if (data.dataRule === 'custom') {
                    $element.stop().velocity({
                        color: data.dataTextColor,
                        backgroundColor: data.dataNextColor,
                        borderColor: data.dataNextColor
                    }, { duration: 'fast' });
                } else {
                    $element.stop().velocity({
                        color: data.dataBgColor,
                        backgroundColor: data.dataNextColor,
                        borderColor: data.dataNextColor
                    }, { duration: 'fast' });
                }
            }
        },
        ////////////////////////
        // 11. Hover Off -- Next Button, Invert Colors
        ////////////////////////
        /**
        ** @event hover - deploy/server next buttons
        ** @desc revert font color back to white, btn bg color to selected color
        **/
        handleNextButtonHoverOff: function(e) {
            e.preventDefault();

            var $element = $(e.target),
                isButton = $element.is('button'),
                isDisabled = $element.hasClass('button--disabled');

            if (isButton && !isDisabled) {
                var dataType = $element.data('type'),
                    data = this.data[dataType];

                if (data.dataRule === 'custom') {
                    $element.stop().velocity({
                        color: data.dataTextColor,
                        backgroundColor: data.dataBgColor,
                        borderColor: data.dataTextColor
                    }, { duration: 'fast' });
                } else {
                    $element.stop().velocity({
                        color: data.dataTextColor,
                        backgroundColor: data.dataBgColor,
                        borderColor: data.dataTextColor
                    }, { duration: 'fast' });
                }
            }
        },
        ////////////////////////
        // 12. Keypress -- Enter Key on Edit Overlay
        ////////////////////////
        /**
        ** @event keypress - Edit Overlay on Preview
        ** @desc User hits 'Enter' on keyboard, should mimic click event
        **/
        handleEditOverlayTabbing: function(e) {
            var key = e.which,
                target = $(e.target),
                isNextButton = target.is('button'),
                isEnterKey = key === 13;


            if (!isNextButton && isEnterKey) {
                var isServerSelection = target.closest('.server'),
                    marginLeft = 0,
                    self = this,
                    dataType = 'server';

                if (isServerSelection.length === 0) {
                    var width = this.slider.find('li').first().width();
                    marginLeft = -Math.abs(width * 2);
                    dataType = 'deploy';
                }

                // Copied from 'this.handleEditOverlay()'
                this.slider.velocity({
                    marginLeft: marginLeft
                }, {
                    complete: function() {
                        self.setView(dataType);
                        self.toggleReviewStateClassonBody();

                        // Custom
                        self.accessibilityTabbingOrder();

                        if (dataType === 'deploy') {
                            self.setFocusOnSelectedListItem( self.typeListItem.last() );
                        }
                    }
                });

                this.handleBackButtonDisplay(1);
            }
        },

        /**
        ** @desc handles displaying or hiding back button
        ** @param integer - 1 for visible, 0 for hidden
        **/
        handleBackButtonDisplay: function(visibility) {
            // _deploy.scss has css animation logic
            // resize back button to 0%, overlay to 100%
            var opacity = visibility || 0;
            var display = opacity === 0 ? 'none' : 'block';
            var width = opacity === 0 ? '0%' : '10%';
            var columnWidth = opacity === 0 ? '100%' : '90%';
            var columnLeft = opacity === 0 ? '0%' : '10%';

            // hide/show back button
            this.backButton.velocity({
                opacity: opacity,
                width: width
            }, { display: display });

            $('li.deploy .initial--overlay').velocity({
                left: columnLeft,
                width: columnWidth
            }, { duration: 'fast' });
        },

        ////////////////////////
        // Update UI Methods
        ////////////////////////
        updateTypeListItemsUI: function(toHide) {
            var backgroundColor = '#323232';
            var hide = toHide || false;

            this.sectionSlider.find('.slider--wrapper')
                .velocity({
                    'backgroundColor': backgroundColor
                });
        },
        /**
        ** @desc Update hidden container with selected text
        ** @param object -- defined from 'buildDataObject'
        **/
        updateSelectedTextContainer : function(obj) {
            var headlineTopSelected = obj.chosenVerbiage.find('.headline-top--selected'),
                headlineBottomSelected = obj.chosenVerbiage.find('.headline-bottom--selected');

            headlineTopSelected.text(obj.dataTitle);
            headlineTopSelected.attr("title", obj.dataTitle);

            headlineTopSelected.css('color', obj.dataTextColor);
            headlineBottomSelected.css('color', obj.dataTextColor);
        },
        /**
        ** @desc Update Back Button UI when Selection is made
        ** @param object -- defined from 'handleServerAndDeployListItems'
        **/
        updateBackButtonUI: function(obj) {
            var backgroundColor = obj.dataBgColor,
                svgFill = svgFills[obj.dataBgColor];

            if (obj.dataType === 'server') {
                this.backButton.velocity({
                    backgroundColor: backgroundColor
                });

                this.backButton.find('svg polyline').velocity({
                    stroke: svgFill
                });
            }
        },
        /**
        ** @desc Update Hidden Bread Crumb for Final Stage
        ** @param object -- defined from 'handleServerAndDeployListItems'
        **/
        updateBreadCrumb: function(obj) {
            // TODO: Refactor Bread Crumb Updates
            var self = this,
                serverBreadCrumbNode = this.recapNav.find('.js--nav__server'),
                deployBreadCrumbNode = this.recapNav.find('.js--nav__deploy'),
                textBeforeBreak;

            if (this.data) {
                $.each(this.data, function(key, value) {
                    if (key === 'server') {
                        serverBreadCrumbNode.text(value.dataTitle).attr("title",value.dataTitle).css('color', value.dataBreadcrumbColor);
                    } else {
                        deployBreadCrumbNode.text(value.dataTitle).attr("title",value.dataTitle).css('color', value.dataBreadcrumbColor);
                    }
                });
            } else {
                if (obj.dataRule && obj.dataRule === 'default') {
                    textBeforeBreak = obj.$element.text();
                    deployBreadCrumbNode.text(textBeforeBreak).attr("title",obj.dataTitle).css('color', obj.dataBreadcrumbColor);
                } else if (obj.dataRule && obj.dataRule === 'custom') {
                    textBeforeBreak = obj.$element.find('span').first().text();
                    deployBreadCrumbNode.text(textBeforeBreak).attr("title",obj.dataTitle).css('color', obj.dataBreadcrumbColor);
                } else {
                    serverBreadCrumbNode.text(obj.dataTitle).attr("title",obj.dataTitle).css('color', obj.dataBreadcrumbColor);
                }
            }
        },
        /**
        ** @event animation - CSS
        ** @desc add class to body, handles animations with CSS,
        ** animating height 'Done' section
        **/
        toggleReviewStateClassonBody: function() {
            $('body').toggleClass('js--review-state');
        },
        /**
        ** @event animation: slide up, part 3
        ** @event animation - CSS
        ** @desc add class to body, determines ability to scroll
        **/
        toggleScrollableStateClassonBody: function() {
            $('body').toggleClass('js--scrollable');
        },


        ////////////////////////
        // Large Animation Functions
        ////////////////////////
        /**
        ** @desc Initialize Slider Animation on Page Load
        **/
        initializeSliderOnPageLoad: function() {
            // 1. Bring down slider
            // 2. Bring in overlay
            // 3. Drop down form

            // 1
            this.sectionSlider
                .velocity({
                    top: 0
                }, {
                    easing: 'easeInOutQuad',
                    delay: 500 // 200
                });

            // 2
            this.formOverlay
                .velocity({
                    opacity: 0.9
                }, {
                    easing: 'easeInOutQuad',
                    delay: 600, //300
                    duration: 'fast',
                    complete: function() {
                        $(this).css('z-index', '8');
                    }
                });

            // 3
            this.form
                .velocity({
                    marginTop: 754
                }, {
                    delay: 500 // 200
                });
        },
        /**
        ** @desc Initialize Bread Crumb based on predefined data
        ** Note: Majority of initial CSS values will be invalid.
        **/
        initializeBreadCrumb: function() {
            // 1. Hide Back Button & Next Buttons
            // 2. Hide Choose Server/Deploy Text
            // 3. Animate to Black Overlay
            // 4. Bring Down Slider to 8% Height
            // 5. Show BreadCrumb/Nav

            // 1
            this.handleBackButtonDisplay(0);

            // 2
            this.chooseVerbiage.first()
                .velocity({
                        opacity: 0
                    }, {
                        queue: false,
                        delay: 500 // 300
                    }
                ).css("visibility", "hidden");

            this.chooseVerbiage.last()
                .velocity({
                        opacity: 0
                    }, {
                        queue: false,
                        delay: 500 // 300
                    }
                ).css("visibility", "hidden");

            // 3
            this.serverListItem.first().velocity({
                backgroundColor: '#323232'
            }, { queue: false, duration: 'fast' });

            this.serverListItem.last().velocity({
                backgroundColor: '#323232'
            }, { queue: false, duration: 'fast' });

            // 4
            this.sectionSlider
                .velocity({
                    top: 0,
                    height: '8%',
                    minHeight: 0,
                    maxHeight: 80
                }, {
                    easing: 'easeInOutQuad',
                    delay: 500
                } ); // easeInOutQuad

            // 5
            this.recapNav.find('.nav--selections, .js--edit-selections').css('display', 'block');
            this.recapNav
                .velocity({
                    opacity: 1
                }, {
                    delay: 575,
                    queue: false
                });
        },
        /**
        ** @event animation: slide up, part 1
        ** @desc Reverse background and font colors w/ psuedo bounce
        **/
        animateTextWithBounceEffect: function() {
            var self = this,
                finalBackgroundColor = '#323232';

            // 1. Transition Background Colors on to Font Colors
            //  A. Server Side Text
            //  B. Deploy Side Text
            // 2. Psuedo Bounce Effect

            // 1. A.
            this.data__server.chosenVerbiage.find('.headline-top--selected').velocity({
                color: this.data__server.dataTextColor
            }, { queue: false, duration: 'fast' });

            this.data__server.chosenVerbiage.find('.headline-bottom--selected').velocity({
                color: this.data__server.dataTextColor
            }, { queue: false, duration: 'fast' });

            this.data__server.serverListItem.velocity({
                backgroundColor: finalBackgroundColor
            }, { queue: false, duration: 'fast' });

            // 1. B.
            this.data__deploy.chosenVerbiage.find('.headline-top--selected').velocity({
                color: this.data__deploy.dataTextColor
            }, { queue: false, duration: 'fast' });

            this.data__deploy.chosenVerbiage.find('.headline-bottom--selected').velocity({
                color: this.data__deploy.dataTextColor
            }, { queue: false, duration: 'fast' });

            this.data__deploy.serverListItem.velocity({
                backgroundColor: finalBackgroundColor
            }, { queue: false, duration: 'fast' });

            // 2
            this.slider
                .velocity({ top: 20 }, { delay: 200 }, [500, 20])
                .velocity('reverse', {
                    delay: 200,
                    complete: function() {
                        self.recapNav.velocity({
                            zIndex: 6
                        });
                    }
                });

            this.animateToBreadCrumb();
        },
        /**
        ** @event animation: slide up, part 2
        ** @desc Animate to Bread Crumb, following psuedo bounce
        **/
        animateToBreadCrumb: function() {
            // 1. slider height moves to 8%
            // 2. change opacity on text to 0
            // 3. remove slider overlay
            // 4. adjust form
            // 5. raise height of nav
            // 6. change aria label

            // 1
            this.sectionSlider
                .velocity({
                    height: '8%',
                    minHeight: 0
                }, {
                    easing: 'easeInOutQuad',
                    delay: 500
                } ); // easeInOutQuad
            this.sliderGradient.velocity({
                opacity: 0
            });

            // 2
            this.data__server.chosenVerbiage
                .velocity({
                        opacity: 0
                    }, {
                        queue: false,
                        delay: 500 // 300
                    }
                );

            this.data__deploy.chosenVerbiage
                .velocity({
                        opacity: 0
                    }, {
                        queue: false,
                        delay: 500 // 300
                    }
                );

            // 3
            this.formOverlay
                .velocity({
                    opacity: 0
                }, {
                    delay: 900,
                    duration: 'fast',
                    complete: function() {
                        // this = this.formOverlay
                        $(this).velocity({ height: 0 });
                        $(this).css('z-index', '-1');
                    }
                });

            // 4
            this.form
                .velocity({
                    marginTop: 160
                }, {
                    delay: 500,
                    queue: false
                });

            // 5
            this.recapNav.find('.nav--selections, .js--edit-selections').css('display', 'block');
            this.recapNav
                .velocity({
                    opacity: 1
                }, {
                    delay: 575,
                    queue: false
                });

            // 6
            this.sectionSlider.attr('aria-label', utils.formatString(messages.RULESELECT_PANEL_ARIA, [messages.RULESELECT_CLOSED]));
            this.bindBreadcrumbEvents();

            this.toggleScrollableStateClassonBody();

        },
        /**
        ** @event User clicks 'Edit Selection'
        ** @param obj -- determined by click event
        **/
        handleNavEditSelection: function(e) {
            e.preventDefault();

            // disable the form
            utils.disableForm('ruleSelect');

            var self = this,
                isBreadCrumb = $(e.target).hasClass('js--nav__server') || $(e.target).hasClass('js--nav__deploy');

            // set overlay to 100% so fade in works properly
            self.formOverlay.css( 'height', '100%' );

            // 1. lower height of nav
            // 2. slider height moves to sliderMaxHeight
            // 3. change opacity on text to 1
            // 4. add back slider overlay
            // 5. add in psuedo 'bounce'
            // 5. enable 'next' button
            // 6. add 'done' bar
            // 7. set view
            // 8. slide to approriate place
            // 9. change aria label

            // 1
            this.recapNav.find('.nav--selections, .js--edit-selections').css('display', 'none');
            this.recapNav
                .velocity({
                    opacity: 0
                }, {
                    queue: false
                });

            // 2
            this.sectionSlider
                .velocity({
                    height: '100%',
                    minHeight: sliderMinHeight,
                    maxHeight: sliderMaxHeight
                }, {
                    easing: 'easeInOutQuad',
                    delay: 200,
                    complete: function() {
                        self.recapNav.velocity({
                            zIndex: 6
                        });
                    }
                } ); // easeInOutQuad

            this.sliderGradient.velocity({
                opacity: 0.15
            });

            // 3
            this.data__server.chosenVerbiage
                .velocity({
                        opacity: 1
                    }, {
                        queue: false,
                        delay: 300
                    }
                );

            this.data__deploy.chosenVerbiage
                .velocity({
                        opacity: 1
                    }, {
                        queue: false,
                        delay: 300
                    }
                );

            // 4
            this.formOverlay
                .velocity({
                    opacity: 0.9
                }, {
                    delay: 300,
                    duration: 'fast',
                    complete: function() {
                        $(this).css('z-index', '8');
                    }
                });

            // Server Side - Text
            this.data__server.chosenVerbiage.find('.headline-top--selected').velocity({
                color: this.data__server.dataTextColor
            }, { delay: 600, duration: 'fast' });

            this.data__server.chosenVerbiage.find('.headline-bottom--selected').velocity({
                color: this.data__server.dataTextColor
            }, { delay: 600, duration: 'fast' });

            // Server Side - Background
            this.data__server.serverListItem.velocity({
                backgroundColor: this.data__server.dataBgColor
            }, { delay: 600, duration: 'fast' });

            // Deploy Side - Text
            this.data__deploy.chosenVerbiage.find('.headline-top--selected').velocity({
                color: this.data__deploy.dataTextColor
            }, { delay: 600, duration: 'fast' });

            this.data__deploy.chosenVerbiage.find('.headline-bottom--selected').velocity({
                color: this.data__deploy.dataTextColor
            }, { delay: 600, duration: 'fast' });

            // Deploy Side - Background
            this.data__deploy.serverListItem.velocity({
                backgroundColor: this.data__deploy.dataBgColor
            }, { delay: 600, duration: 'fast' });

            // Add ellipsis to long strings
            this.selectedRuntimeText.dotdotdot({ watch: "window", height: "173px" });
            this.selectedPackageText.dotdotdot({ watch: "window", height: "173px" });

            setTimeout(function(){
                // 6 - add done bar
                self.toggleScrollableStateClassonBody();
                self.toggleReviewStateClassonBody();

                // 7 - set view
                self.setView('preview');
                self.accessibilityAccess(false);
                self.accessibilityTabbingOrder();
            }, 500); // 700

            // 9 - aria label
            this.sectionSlider.attr('aria-label', utils.formatString(messages.RULESELECT_PANEL_ARIA, [messages.RULESELECT_OPEN]));

            this.form
                .velocity({
                    marginTop: 754
                }, {
                    delay: 200
                });
        },

        ////////////////////////
        // Utility Functions
        ////////////////////////
        /**
        ** @desc build object to use in selection animation - 'handleSelectedServerOrDeployListItems'
        ** @param $element
        ** @return object
        **/
        buildDataObject: function($element) {
            var obj = {};

            obj.$element = $element;
            obj.dataBreadcrumbColor = this.getDataBreadcrumbColor($element);
            obj.dataBreadcrumbHoverColor = this.getDataBreadcrumbHoverColor($element);
            obj.dataBgColor = this.getDataBgColor($element);
            obj.dataTextColor = this.getDataTextColor($element);
            obj.dataLiColor = this.getDataLiColor($element);
            obj.dataNextColor = this.getDataNextColor($element);
            obj.dataTitle = this.getDataTitle($element);
            obj.dataType = this.getDataType($element);
            obj.dataRule = this.getDataRule($element);

            if (obj.dataType === 'deploy') {
                obj.chooseVerbiage = this.chooseVerbiage.last();
                obj.chosenVerbiage = this.chosenVerbiage.last();
                obj.serverListItem = this.serverListItem.last();
                obj.confirmationOverlay = this.confirmationOverlays.last();
                obj.dataIndex = this.getDataIndex($element);
                this.data__deploy = obj;
            } else {
                obj.chooseVerbiage = this.chooseVerbiage.first();
                obj.chosenVerbiage = this.chosenVerbiage.first();
                obj.serverListItem = this.serverListItem.first();
                obj.confirmationOverlay = this.confirmationOverlays.first();
                obj.dataRuntime = this.getDataRuntime($element);
                this.data__server = obj;
            }

            this.setDataModel(obj);

            return obj;
        },
        /**
        ** @desc step by step animation for selected server/deploy item
        ** @param obj -- build form 'buildDataObject'
        **/
        handleSelectedServerOrDeployListItems: function(obj, runInitialState) {
            var previousSelected = this.removePreviousSelectedListItem(obj);

            if (previousSelected){
                this.transitionPreviousSelectionOut(previousSelected);
            }
            this.transitionSelectedIn(obj);
            this.fadeOutOriginalTextForSelectedItem(obj);
            this.updateBreadCrumb(obj);
            this.updateBackButtonUI(obj);
        },
        /**
        ** @desc find selected list item, remove active class state
        ** @param object -- defined from 'handleServerAndDeployListItems'
        ** @return jQuery element - element was previously selected
        **/
        removePreviousSelectedListItem: function(obj) {
            var selected = (obj.dataType === 'server') ? this.typeListItem.first() : this.typeListItem.last();
            return selected.find('.js--selected').removeClass('js--selected');
        },
        /**
        ** @desc return list item to default color | previously selected
        ** @param jQuery object -- element
        **/
        transitionPreviousSelectionOut: function(previousSelected) {
            previousSelected
                .velocity({
                    color: defaultListItemFontColor
                }, { duration: '50'});
        },
        /**
        ** @desc newly selected list item needs font color change
        ** @param object -- defined from 'handleServerAndDeployListItems'
        **/
        transitionSelectedIn: function(obj) {
            var color = obj.dataLiColor;

            obj.$element
                .addClass('js--selected')
                .velocity({
                    color: color
                },{ duration: '0' });
        },
        /**
        ** @desc Fade out 'Choose Server/Deploy Type' to Selected List Item Text
        ** @param object -- defined from 'handleServerAndDeployListItems'
        **/
        fadeOutOriginalTextForSelectedItem: function(obj) {
            // Hide 'Choose Server/Deploy Type' Text
            obj.chooseVerbiage.velocity({ opacity: 0 }).css("visibility", "hidden");

            // Before Showing Selected List Item Text, Must Update Text
            this.updateSelectedTextContainer(obj);

            // Now Show Selected List Item Text
            obj.chosenVerbiage.velocity({ opacity: 1 }, {duration: 'fast'}, {queue: 'false'});
            obj.serverListItem.velocity({ backgroundColor: obj.dataBgColor }, {duration: 'fast'}, {queue: 'false'});
        },
        /**
        ** @desc User has selected deploy type, now moving to Preview Mode
        **/
        moveToPreviewMode: function() {
            // hide/remove ::after arrows
            this.typeListItem.removeClass('list-item--arrow');

            this.handleBackButtonDisplay();
            this.editOverlay.off('click'); //ensure we don't double-bind the following
            this.editOverlay.on('click', this.handleEditOverlay.bind(this));

            // css animation handles height and button manipulation
            this.toggleReviewStateClassonBody();
            this.reviewButton.focus();
        },


        ////////////////////////
        // Helper Functions
        // 1. Getter/Setters
        // 2. Click Events
        // 3. Slider Positioning
        // 4. Scrolling
        ////////////////////////

        ////////////////////////
        // Helper Functions -- Getter/Setters
        ////////////////////////
        /**
        ** @desc Set 'this.data' with obj.dataType
        ** @param JS Object -- Built from 'this.buildDataObject'
        **/
        setDataModel: function(obj){
            var isDataEmpty = $.isEmptyObject(this.data);

            if (isDataEmpty){
                   this.data = {};
            }

            this.data[obj.dataType] = obj;
        },
        /**
        ** @desc Sets view
        ** 1. Helps determine margin-left on resize
        ** 2. Help keep context within events
        ** @param String -- Set on 'this.view'
        **/
        setView: function(view) {
            if ($.type(view) === 'string') {
                this.view = view;
            } else {
                console.error("Trying to set 'this.view' and String was not passed in.");
            }
        },
        /**
        ** @desc Gets view
        ** @return String -- Get on 'this.view'
        **/
        getView: function() {
            return this.view;
        },
        /**
         ** @desc grab 'data-bg-color' for use as text color
         ** @param jQuery object - $element
         ** @return string - hex value
         **/
        getDataBgColor: function($element) {
            return '#' + $element.data('bg-color');
        },

        getDataTextColor: function($element) {
            return '#' + $element.data('text-color');
        },

        getDataLiColor: function($element) {
            return '#' + $element.data('li-color');
        },

        getDataNextColor: function($element) {
            return '#' + $element.data('next-color');
        },

        getDataBreadcrumbColor: function($element) {
            return '#' + $element.data('breadcrumb-color');
        },

        getDataBreadcrumbHoverColor: function($element) {
            return '#' + $element.data('breadcrumb-hover-color');
        },
        /**
        ** @desc grab title/text to be displayed
        ** @param jQuery object - $element
        ** @return string - title
        **/
        getDataTitle: function($element) {
            var dataType = $element.data('type'),
                dataRule = $element.data('rule'),
                text;

            if (dataType === 'deploy' && dataRule === 'custom') {
                text = $element.data('title');
            } else {
                text = $element.text();
            }

            return text;
        },
        /**
        ** @desc grab 'data-type' to use for logistics
        ** @param jQuery object - $element
        ** @return string - should either be 'server' or 'deploy'
        **/
        getDataType: function($element) {
            return $element.data('type');
        },
        /**
        ** @desc grab 'data-rule' to use on deploy selection
        ** @param jQuery object - $element
        ** @return string - either 'default' or 'custom'
        **/
        getDataRule: function($element) {
            return $element.data('rule');
        },
        /**
         * @desc grab 'data-runtime'
         * @param jQuery object - $element
         * @return string - the runtimeType
         */
        getDataRuntime: function($element) {
            return $element.data('runtime');
        },
        /**
         * @desc grab 'data-index' - the index in the JSON from rules API
         * @param jQuery object - $element
         * @return int - index of the rule from rules API
         */
        getDataIndex: function($element) {
            return $element.data('index');
        },

        ////////////////////////
        // 2. Helper Functions -- Click Events
        ////////////////////////
        /**
        ** @desc verify user has made a selection
        ** @param string -- data type
        ** @return boolean
        **/
        verifySelectionHasBeenMade: function(dataType) {
            var col;

            if (dataType === 'server') {
                col = this.chosenVerbiage.first();
            } else  {
                col = this.chosenVerbiage.last();
            }

            return col.css('opacity') === '1';
        },

        isElementSpan: function(e) {
            var isSpan = $(e.target).is('span'),
                $element = isSpan ? $(e.target).parent() : $(e.target);

            return $element;
        },

        ////////////////////////
        // 3. Helper Functions -- Slider Positioning
        ////////////////////////
        /**
        ** @desc find width of 'column' in slider, help determine left margin
        ** @param string -- data type
        ** @return integer -- must be negative
        **/
        calculateMarginLeft: function(dataType) {
            var width = this.slider.find('li').first().width();

            if (dataType === 'server') {
                width = -Math.abs(width * 2);
            } else {
                width = -Math.abs(width);
            }

            return width;
        },

        ////////////////////////
        // Helper Functions -- Scrolling
        ////////////////////////
        /**
        ** @desc Determine IF deploy selections exceed parent containers height
        **/
        shouldScrollShowOnDeployLists: function() {
            var deployScrollableSectionHeight = this.deployScrollableSection.height(),
                totalHeight = this.getDeployListInnerContentsHeight();

            if (totalHeight > deployScrollableSectionHeight) {
                this.deployScrollableSection.parent().addClass('overflow--scroll');
            }
        },
        /**
        ** @desc Determines HEIGHT of deploy selections (default|custom)
        ** @return Integer - Total Height
        **/
        getDeployListInnerContentsHeight: function() {
            var listItemsContainer = $('.deploy--list-types'),
                defaultItems = listItemsContainer.find('.list-types--default'),
                customItems = listItemsContainer.find('.list-types--custom'),
                defaultItemsWidth = defaultItems.height(),
                customItemsWidth = customItems.height(),
                totalHeight = defaultItemsWidth + customItemsWidth;

            return totalHeight;
        },

        setAriaLabels: function() {
            this.serverSelectSection.attr('aria-label', messages.RULESELECT_SERVER_TYPE + ', ' + messages.RULESELECT_SELECT_ONE);
            this.serverSelectDefaultList.attr('aria-label', messages.RULESELECT_SERVER_DEFAULT);
            this.serverSelectCustomList.attr('aria-label', messages.RULESELECT_SERVER_CUSTOM_ARIA);

            this.ruleSelectSection.attr('aria-label', messages.RULESELECT_DEPLOY_TYPE + ', ' + messages.RULESELECT_SELECT_ONE);
            this.ruleSelectDefaultList.attr('aria-label', messages.RULESELECT_RULE_DEFAULT);
            this.ruleSelectCustomList.attr('aria-label', messages.RULESELECT_RULE_CUSTOM);

            this.setEditButtonAria(); //initial call for persisted data
        },
        setEditButtonAria: function() {
            this.currentServerSelection = $('.server.bg--lighter div.positioning .headline-top--selected');
            this.currentRuleSelection = $('.deploy.bg--lighter div.positioning .headline-top--selected');
            this.editServerButton.attr('aria-label', utils.formatString(messages.RULESELECT_EDIT_SERVER_ARIA, [this.currentServerSelection.text()]));
            this.editRuleButton.attr('aria-label', utils.formatString(messages.RULESELECT_EDIT_RULE_ARIA, [this.currentRuleSelection.text()]));
        },

        accessibilityAccess: function(hideTabableElements) {
            var hideElements = hideTabableElements || false;
            var opacity = hideElements ? '0' : '1';
            var visibility = hideElements ? 'hidden' : 'visible';

            this.typeListItem.css({
                'visibility': visibility,
                'opacity': opacity
            });
        },
        accessibilityTabbingOrder: function() {
            var sectionToRemoveTabIndex,
                sectionToAddTabIndex,
                buttonToRemoveTabIndex,
                buttonToAddTabIndex,
                sectionAddedIndexList,
                indexToBeAdded,
                indexToBeRemoved,
                arrayToIndex = [],
                arrayToDeIndex = [];

            if (this.view === 'server') {
                // Lose :focus on Edit Overlay
                $(':focus').blur();

                sectionToRemoveTabIndex = this.typeListItem.last();
                sectionToAddTabIndex = this.typeListItem.first();

                // Build Array Index
                sectionToAddTabIndex.find('a').filter('[data-type]').each(function(idx, el) {
                    arrayToIndex.push(el);
                });
                arrayToIndex.push( buttonToAddTabIndex );

                // Build Array De-Index
                this.initialOverlays.find('.positioning').each(function(idx, el){
                    arrayToDeIndex.push(el);
                });
                sectionToRemoveTabIndex.find('a').filter('[data-type]').each(function(idx, el) {
                    arrayToDeIndex.push(el);
                });
                arrayToDeIndex.push(buttonToRemoveTabIndex);
                arrayToDeIndex.push(this.reviewButton);
                arrayToDeIndex.push(this.backButton);
                arrayToDeIndex.push(this.editNavAnchor);

                indexToBeAdded = arrayToIndex;
                indexToBeRemoved = arrayToDeIndex;
            } else if (this.view === 'deploy') {
                // Lose :focus on Edit Overlay
                $(':focus').blur();

                sectionToRemoveTabIndex = this.typeListItem.first();
                sectionToAddTabIndex = this.typeListItem.last();

                // Build Array Index
                $( sectionToAddTabIndex.find('a').filter('[data-type]') ).each(function(idx, el) {
                    arrayToIndex.push(el);
                });
                arrayToIndex.push(buttonToAddTabIndex);
                arrayToIndex.push(this.backButton);

                // Build Array De-Index
                this.initialOverlays.find('.positioning').each(function(idx, el) {
                    arrayToDeIndex.push(el);
                });
                sectionToRemoveTabIndex.find('a').each(function(idx, el) {
                    arrayToDeIndex.push(el);
                });
                arrayToDeIndex.push(buttonToRemoveTabIndex);
                arrayToDeIndex.push(this.editNavAnchor);

                indexToBeAdded = arrayToIndex;
                indexToBeRemoved = arrayToDeIndex;
            } else if (this.view === 'preview') {
                sectionToRemoveTabIndex = this.typeListItem.last();

                this.serverAndDeployListItems.each(function(idx, el) {
                    arrayToDeIndex.push(el);
                });

                // Build Array De-Index
                sectionToRemoveTabIndex.find('a').each(function(idx, el) {
                    arrayToDeIndex.push(el);
                });
                arrayToDeIndex.push(buttonToRemoveTabIndex);
                arrayToDeIndex.push(this.backButton);
                arrayToDeIndex.push(this.editNavAnchor);

                // Build Array Index
                this.initialOverlays.find('.positioning').each(function(idx, el) {
                    arrayToIndex.push(el);
                });
                arrayToIndex.push(this.reviewButton);

                indexToBeRemoved = arrayToDeIndex;
                indexToBeAdded = arrayToIndex;
            } else if (this.view === 'final') {

                // Remove tabindex from Edit buttons
                this.initialOverlays.find('.positioning').each(function(idx, el) {
                    arrayToDeIndex.push(el);
                });

                arrayToIndex.push(this.editNavAnchor);

                indexToBeAdded = arrayToIndex;
                indexToBeRemoved = arrayToDeIndex;
            }

            // Add Tabindex
            this.createTabIndex( indexToBeAdded );
            // Remove Tabindex
            this.removeTabIndex( indexToBeRemoved );
        },

        setFocusOnSelectedListItem: function(sectionToSetFocus) {
            var list = sectionToSetFocus.find('a');
            list.first().focus();
        },

        removeTabIndex: function( elementsToRemoveIndex ) {
            $(elementsToRemoveIndex).each(function( idx, el ) {
                $(el).attr('tabindex', '-1');
            });
        },

        createTabIndex: function( elementsToAddIndex ) {
            $(elementsToAddIndex).each(function(idx, el) {
                $(el).attr('tabindex', idx + 1);
            });
        },

        clearDeploySelection: function(e) {
             e.preventDefault();

             // Verify Selections Have Been made
             this.fadeOutDeploySelectedItemForOriginalText();
         },
         fadeOutDeploySelectedItemForOriginalText: function() {
             if (this.data && this.data.deploy) {
                 var obj = this.data.deploy;

                 // Show 'Choose Server/Deploy Type' Text
                 obj.chooseVerbiage.velocity({ opacity: 1 }).css("visibility", "visible");

                 // Clear Selected Text and Bread Crumb
                 this.clearDeploySelectedTextContainer(obj);
//                 this.clearDeployBreadCrumb();

                 // Hide Chosen Verbiage
                 obj.chosenVerbiage.velocity({ opacity: 0 });

                 obj.serverListItem.removeAttr("style");

                 var previousSelected = this.removePreviousSelectedListItem(obj);
                 if (previousSelected) {
                     this.transitionPreviousSelectionOut(previousSelected);
                 }

             }
         },
         clearDeploySelectedTextContainer: function(obj) {
             var headlineTopSelected = obj.chosenVerbiage.find('.headline-top--selected'),
                 headlineBottomSelected = obj.chosenVerbiage.find('.headline-bottom--selected'),
                 textBeforeBreak;

             if(headlineTopSelected.html() !== ""){
               return;
             }

             headlineTopSelected
                 .text("")
                 .removeAttr("style");

             headlineBottomSelected
                 .text("")
                 .removeAttr("style");

             this.editRuleButton.attr('aria-label', messages.RULESELECT_EDIT_RULE_ARIA);
         },
         clearDeployBreadCrumb: function(obj) {
             var self = this,
                 deployBreadCrumbNode = this.recapNav.find('.js--nav__deploy');

             deployBreadCrumbNode
                 .text("")
                 .removeAttr("style");
          }
    };

    return {
        App: App
    };
})();
