define("doh/_browserRunner", [
	"dojo/dom", "dojo/dom-geometry", "dojo/dom-style",
	"dojo/_base/fx", "dojo/_base/lang", "dojo/query", "dojo/domReady", "dojo/sniff", "dojo/window",
	"doh/runner"
], function(dom, domGeom, domStyle, baseFx, lang, query, domReady, has, win, doh){
	doh.isBrowser= true;
	var topdog;
	try{
		topdog = (window.parent == window) || !Boolean(window.parent.doh);
	}catch(e){
		//can't access window.parent.doh, then consider ourselves as topdog
		topdog=true;
	}
	if(topdog){
		// we're the top-dog window.

		// borrowed from Dojo, etc.
		var byId = function(id){
			return document.getElementById(id);
		};

		var _addOnEvt = function( type,		// string
									refOrName,	// function or string
									scope){		// object, defaults is window

			if(!scope){ scope = window; }

			var funcRef = refOrName;
			if(typeof refOrName == "string"){
				funcRef = scope[refOrName];
			}
			var enclosedFunc = function(){ return funcRef.apply(scope, arguments); };

			if(domReady && type == "load"){
				domReady(enclosedFunc);
			}else{
				if(window["attachEvent"]){
					window.attachEvent("on"+type, enclosedFunc);
				}else if(window["addEventListener"]){
					window.addEventListener(type, enclosedFunc, false);
				}else if(document["addEventListener"]){
					document.addEventListener(type, enclosedFunc, false);
				}
			}
		};

		//
		// Over-ride or implement base runner.js-provided methods
		//
		var escapeXml = function(str){
			// summary:
			//		Adds escape sequences for special characters in XML: &<>"'
			//		Optionally skips escapes for single quotes
			return str.replace(/&/gm, "&amp;").replace(/</gm, "&lt;").replace(/>/gm, "&gt;").replace(/"/gm, "&quot;"); // string
		};

		var formatTime = function(n){
			switch(true){
				case n<1000: //<1s
					return n+"ms";
				case n<60000: //<1m
					return Math.round(n/100)/10+"s";
				case n<3600000: //<1h
					return Math.round(n/6000)/10+"m";
				default: //>1h
					return Math.round(n/360000)/10+"h";
			}
		};

		var _logBacklog = [], _loggedMsgLen = 0;
		var sendToLogPane = function(args, skip){
			var msg = "";
			for(var x = 0; x < args.length; x++){
				msg += " " + args[x];
			}

			msg = escapeXml(msg);

			// workarounds for IE. Wheeee!!!
			msg = msg.replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;")
				.replace(" ", "&nbsp;")
				.replace("\n", "<br>&nbsp;");
			if(!byId("logBody")){
				_logBacklog.push(msg);
				return;
			}else if(_logBacklog.length && !skip){
				var tm;
				while((tm = _logBacklog.shift())){
					sendToLogPane(tm, true);
				}
			}
			var logBody = byId("logBody");
			var tn = document.createElement("div");
			tn.innerHTML = msg;
			//tn.id="logmsg_"+logBody.childNodes.length;
			logBody.appendChild(tn);
			_loggedMsgLen++;
		};

		var findTarget = function(n){
			while(n && !n.getAttribute('_target')){
				n = n.parentNode;
				if(!n.getAttribute){
					n = null;
				}
			}
			return n;
		};

		doh._jumpToLog = function(e){
			//console.log(e);

			var node = findTarget(e?e.target:window.event.srcElement);
			if(!node){
				return;
			}
			var _t = Number(node.getAttribute('_target'));
			var lb = byId("logBody");
			if(_t>=lb.childNodes.length){
				return;
			}
			var t = lb.childNodes[_t];
			t.scrollIntoView();
			if(domStyle && baseFx){
				//t.parentNode.parentNode is <div class="tabBody">, only it has a explicitly set background-color,
				//all children of it are transparent
				var bgColor = domStyle.get(t.parentNode.parentNode,'backgroundColor');
				//node.parentNode is the tr which has background-color set explicitly
				var hicolor = domStyle.get(node.parentNode,'backgroundColor');
				var unhilight = baseFx.animateProperty({
					node: t,
					duration: 500,
					properties:
					{
						backgroundColor: { start: hicolor, end: bgColor }
					},
					onEnd: function(){
						t.style.backgroundColor="";
					}
				});
				var hilight = baseFx.animateProperty({
					node: t,
					duration: 500,
					properties:
					{
						backgroundColor: { start: bgColor, end: hicolor }
					},
					onEnd: function(){
						unhilight.play();
					}
				});
				hilight.play();
			}
		};

		doh._jumpToSuite = function(e){
			var node = findTarget(e ? e.target : window.event.srcElement);
			if(!node){
				return;
			}
			var _g = node.getAttribute('_target');
			var gn = getGroupNode(_g);
			if(!gn){
				return;
			}
			gn.scrollIntoView();
		};

		doh._init = (function(oi){
			return function(){
				var lb = byId("logBody");
				if(lb){
					// clear the console before each run
					while(lb.firstChild){
						lb.removeChild(lb.firstChild);
					}
					_loggedMsgLen = 0;
				}
				this._totalTime = 0;
				this._suiteCount = 0;
				oi.apply(doh, arguments);
			}
		})(doh._init);

		doh._setupGroupForRun = (function(os){
			//overload _setupGroupForRun to record which log line to jump to when a suite is clicked
			return function(groupName){
				var tg = doh._groups[groupName];
				doh._curTestCount = tg.length;
				doh._curGroupCount = 1;
				var gn = getGroupNode(groupName);
				if(gn){
					//two lines will be added, scroll the second line into view
					gn.getElementsByTagName("td")[2].setAttribute('_target',_loggedMsgLen+1);
				}
				os.apply(doh,arguments);
			}
		})(doh._setupGroupForRun);

		var originalDohReport= doh._report;
		doh._report = function(){
			var tb = byId("testList");
			if(tb){
				var tfoots=tb.getElementsByTagName('tfoot');
				if(tfoots.length){
					tb.removeChild(tfoots[0]);
				}
				var foot = tb.createTFoot();
				var row = foot.insertRow(-1);
				row.className = 'inProgress';
				var cell=row.insertCell(-1);
				cell.colSpan=2;
				cell.innerHTML="Result";
				cell = row.insertCell(-1);
				cell.innerHTML=this._testCount+" tests in "+this._groupCount+" groups /<span class='failure'>"+this._errorCount+"</span> errors, <span class='failure'>"+this._failureCount+"</span> failures";
				cell.setAttribute('_target',_loggedMsgLen+1);
				row.insertCell(-1).innerHTML=formatTime(doh._totalTime);
			}

			//This location can do the final performance rendering for the results
			//of any performance tests.

			if(doh.perfTestResults){
				require(["dojox/math/stats", "dojox/charting/DataChart", "dojox/charting/plot2d/Scatter", "dojox/charting/plot2d/Lines", "dojo/data/ItemFileReadStore"],
						function(stats, DataChart, Scatter, Lines, ItemFileReadStore){
					lang.mixin(doh, stats);

					var plotResults = function(div, name, dataArray) {
						// Performance report generating functions!
						var median = doh.median(dataArray);
						var medarray = [];

						var i;
						for(i = 0; i < dataArray.length; i++){
							medarray.push(median);
						}

						var data = {
							label: "name",
							items: [
								{name: name, trials: dataArray},
								{name: "Median", trials: medarray}
							]
						};
						var ifs = new ItemFileReadStore({data: data});

						var min = Math.floor(doh.min(dataArray));
						var max = Math.ceil(doh.max(dataArray));
						var step = (max - min)/10;

						//Lets try to pad out the bottom and top a bit
						//Then recalc the step.
						if(min > 0){
							min = min - step;
							if(min < 0){
								min = 0;
							}
							min = Math.floor(min);
						}
						if(max > 0){
							max = max + step;
							max = Math.ceil(max);
						}
						step = (max - min)/10;

						var chart = new DataChart(div, {
							type: Lines,
							displayRange: dataArray.length,
							xaxis: {min: 1, max: dataArray.length, majorTickStep: Math.ceil((dataArray.length - 1)/10), htmlLabels: false},
							yaxis: {min: min, max: max, majorTickStep: step, vertical: true, htmlLabels: false}
						});
						chart.setStore(ifs, {name:"*"}, "trials");
					};

					try{
						var g;
						var pBody = byId("perfTestsBody");
						var chartsToRender = [];
						// store analytics for reading later
						// keyed on test group name, each value is in turn an object keyed on test name
						doh.perfTestAnalytics={};
						doh.showPerfTestsPage();
						for(g in doh.perfTestResults){
							doh.perfTestAnalytics[g] = {};
							var grp = doh.perfTestResults[g];
							var hdr = document.createElement("h1");
							hdr.appendChild(document.createTextNode("Group: " + g));
							pBody.appendChild(hdr);
							var ind = document.createElement("blockquote");
							pBody.appendChild(ind);
							var f;
							for(f in grp){
								var fResults = grp[f];
								if(!fResults){ continue; }
								var fhdr = document.createElement("h3");
								fhdr.appendChild(document.createTextNode("TEST: " + f));
								fhdr.style.textDecoration = "underline";
								ind.appendChild(fhdr);
								var div = document.createElement("div");
								ind.appendChild(div);

								//Figure out the basic info
								var results = "<b>TRIAL SIZE: </b>"	 + fResults.trials[0].testIterations + " iterations<br>" +
									"<b>NUMBER OF TRIALS: </b>" + fResults.trials.length + "<br>";

								//Figure out the average test pass cost.
								var i;
								var iAvgArray = [];
								var tAvgArray = [];
								for(i = 0; i < fResults.trials.length; i++){
									iAvgArray.push(fResults.trials[i].average);
									tAvgArray.push(fResults.trials[i].executionTime);
								}
								var analytics = doh.perfTestAnalytics[g][f] = {
									averageTrialExecutionTime: doh.mean(tAvgArray),
									maxTestIterationTime: doh.max(iAvgArray),
									minTestIterationTime: doh.min(iAvgArray),
									averageTestIterationTime: doh.mean(iAvgArray),
									medianTestIterationTime: doh.median(iAvgArray),
									varianceTestIterationTime: doh.variance(iAvgArray),
									standardDeviationTestIterationTime: doh.sd(iAvgArray)
								};
								results += "<b>AVERAGE TRIAL EXECUTION TIME: </b>" + analytics.averageTrialExecutionTime.toFixed(10) + "ms.<br>";
								results += "<b>MAXIMUM TEST ITERATION TIME: </b>" + analytics.maxTestIterationTime.toFixed(10) + "ms.<br>";
								results += "<b>MINIMUM TEST ITERATION TIME: </b>" + analytics.minTestIterationTime.toFixed(10) + "ms.<br>";
								results += "<b>AVERAGE TEST ITERATION TIME: </b>" + analytics.averageTestIterationTime.toFixed(10) + "ms.<br>";
								results += "<b>MEDIAN TEST ITERATION TIME: </b>" + analytics.medianTestIterationTime.toFixed(10) + "ms.<br>";
								results += "<b>VARIANCE TEST ITERATION TIME: </b>" + analytics.varianceTestIterationTime.toFixed(10) + "ms.<br>";
								results += "<b>STANDARD DEVIATION ON TEST ITERATION TIME: </b>" +analytics.standardDeviationTestIterationTime.toFixed(10) + "ms.<br>";

								//Okay, attach it all in.
								div.innerHTML = results;

								div = document.createElement("div");
								div.innerHTML = "<h3>Average Test Execution Time (in milliseconds, with median line)</h3>";
								ind.appendChild(div);
								div = document.createElement("div");
								domStyle.set(div, "width", "600px");
								domStyle.set(div, "height", "250px");
								ind.appendChild(div);
								chartsToRender.push({
									div: div,
									title: "Average Test Execution Time",
									data: iAvgArray
								});

								div = document.createElement("div");
								div.innerHTML = "<h3>Average Trial Execution Time (in milliseconds, with median line)</h3>";
								ind.appendChild(div);
								div = document.createElement("div");
								domStyle.set(div, "width", "600px");
								domStyle.set(div, "height", "250px");
								ind.appendChild(div);
								chartsToRender.push({
									div: div,
									title: "Average Trial Execution Time",
									data: tAvgArray
								});
							}
						}

						//Lazy-render these to give the browser time and not appear locked.
						var delayedRenders = function() {
							if(chartsToRender.length){
								var chartData = chartsToRender.shift();
								plotResults(chartData.div, chartData.title, chartData.data);
							}
							setTimeout(delayedRenders, 50);
						};
						setTimeout(delayedRenders, 150);
					}catch(e){
						doh.debug(e);
					}
				});
			}
			originalDohReport.apply(doh,arguments);
		};


		doh.error = undefined;
		if(this["opera"] && opera.postError){
			doh.debug = function(){
				var msg = "";
				for(var x=0; x<arguments.length; x++){
					msg += " "+arguments[x];
				}
				sendToLogPane([msg]);
				opera.postError("DEBUG:"+msg);
			}
		}else if(window["console"]){
			if(console.error){
				doh.error = function(){
					sendToLogPane.call(window, arguments);
					console.error(Array.prototype.join.call(arguments, " "))
				};
			}
			if(console.debug){
				doh.debug = function(){
					sendToLogPane.call(window, arguments);
					console.debug(Array.prototype.join.call(arguments, " "))
				};
			}else if(console.info){
				doh.debug = function(){
					sendToLogPane.call(window, arguments);
					console.info(Array.prototype.join.call(arguments, " "))
				};
			}else{
				doh.debug = function(){
					sendToLogPane.call(window, arguments);
					console.log("DEBUG:"+ Array.prototype.join.call(arguments, " "));
				};
			}
		}else{
			doh.debug = function(){
				sendToLogPane.call(window, arguments);
			}
		}
		doh.error = doh.error || doh.debug;
		var loaded = false;
		var groupTemplate = null;
		var testTemplate = null;

		var groupNodes = {};

		var _groupTogglers = {};

		var _getGroupToggler = function(group, toggle){
			if(_groupTogglers[group]){ return _groupTogglers[group]; }
			var rolledUp = true;
			return (_groupTogglers[group] = function(evt, forceOpen){
				var nodes = groupNodes[group].__items;
				var x;
				if(rolledUp||forceOpen){
					rolledUp = false;
					for(x=0; x<nodes.length; x++){
						nodes[x].style.display = "";
					}
					toggle.innerHTML = "&#9660;";
				}else{
					rolledUp = true;
					for(x=0; x<nodes.length; x++){
						nodes[x].style.display = "none";
					}
					toggle.innerHTML = "&#9658;";
				}
			});
		};

		var addGroupToList = function(group){
			if(!byId("testList")){
				return;
			}
			var tb = byId("testList").tBodies[0];
			var tg = groupTemplate.cloneNode(true);
			var tds = tg.getElementsByTagName("td");
			var toggle = tds[0];
			toggle.onclick = _getGroupToggler(group, toggle);
			var cb = tds[1].getElementsByTagName("input")[0];
			cb.group = group;
			cb.onclick = function(){
				doh._groups[group].skip = (!this.checked);
			};
			tds[2].innerHTML = "<div class='testGroupName'>" + group + "</div><div style='width:0;'>&nbsp;</div>";
			tds[3].innerHTML = "";

			tb.appendChild(tg);
			return tg;
		};

		var addFixtureToList = function(group, fixture){
			if(!testTemplate){
				return;
			}
			var cgn = groupNodes[group];
			if(!cgn["__items"]){
				cgn.__items = [];
			}
			var tn = testTemplate.cloneNode(true);
			var tds = tn.getElementsByTagName("td");

			tds[2].innerHTML = fixture.name;
			tds[3].innerHTML = "";

			var nn = (cgn.__lastFixture || cgn.__groupNode).nextSibling;
			if(nn){
				nn.parentNode.insertBefore(tn, nn);
			}else{
				cgn.__groupNode.parentNode.appendChild(tn);
			}
			// FIXME: need to make group display toggleable!!
			tn.style.display = "none";
			cgn.__items.push(tn);
			return (cgn.__lastFixture = tn);
		};

		var getFixtureNode = function(group, fixture){
			if(groupNodes[group]){
				return groupNodes[group][fixture.name];
			}
			return null;
		};

		var getGroupNode = function(group){
			if(groupNodes[group]){
				return groupNodes[group].__groupNode;
			}
			return null;
		};

		var updateBacklog = [];
		doh._updateTestList = function(group, fixture, unwindingBacklog){
			if(!loaded){
				if(group && fixture){
					updateBacklog.push([group, fixture]);
				}
				return;
			}else if(updateBacklog.length && !unwindingBacklog){
				var tr;
				while((tr = updateBacklog.shift())){
					doh._updateTestList(tr[0], tr[1], true);
				}
			}
			if(group && fixture){
				if(!groupNodes[group]){
					groupNodes[group] = {
						"__groupNode": addGroupToList(group)
					};
				}
				if(!groupNodes[group][fixture.name]){
					groupNodes[group][fixture.name] = addFixtureToList(group, fixture)
				}
			}
		};

		doh._testRegistered = doh._updateTestList;

		doh._groupStarted = function(group){
			if(this._suiteCount == 0){
				this._runedSuite = 0;
				this._currentGlobalProgressBarWidth = 0;
				this._suiteCount = this._testCount;
			}
			// console.debug("_groupStarted", group);
			if(doh._inGroup != group){
				doh._groupTotalTime = 0;
				doh._runed = 0;
				doh._inGroup = group;
				this._runedSuite++;
			}
			var gn = getGroupNode(group);
			if(gn){
				gn.className = "inProgress";
			}
		};

		doh._groupFinished = function(group, success){
			// console.debug("_groupFinished", group);
			var gn = getGroupNode(group);
			if(gn && doh._inGroup == group){
				doh._totalTime += doh._groupTotalTime;
				gn.getElementsByTagName("td")[3].innerHTML = formatTime(doh._groupTotalTime);
				gn.getElementsByTagName("td")[2].lastChild.className = "";
				doh._inGroup = null;
				//doh._runedSuite++;
				var failure = doh._updateGlobalProgressBar(this._runedSuite / this._groupCount, success, group);
				gn.className = failure ? "failure" : "success";
				//doh._runedSuite--;
				doh._currentGlobalProgressBarWidth = parseInt(this._runedSuite / this._groupCount * 10000) / 100;
				//byId("progressOuter").style.width = parseInt(this._runedSuite/this._suiteCount*100)+"%";
			}
			if(doh._inGroup == group){
				this.debug("Total time for GROUP \"", group, "\" is ", formatTime(doh._groupTotalTime));
			}
		};

		doh._testStarted = function(group, fixture){
			// console.debug("_testStarted", group, fixture.name);
			var fn = getFixtureNode(group, fixture);
			if(fn){
				fn.className = "inProgress";
			}
		};

		var _nameTimes = {};
		var _playSound = function(name){
			if(byId("hiddenAudio") && byId("audio") && byId("audio").checked){
				// console.debug("playing:", name);
				var nt = _nameTimes[name];
				// only play sounds once every second or so
				if((!nt) || (((new Date) - nt) > 700)){
					_nameTimes[name] = new Date();
					var tc = document.createElement("span");
					byId("hiddenAudio").appendChild(tc);
					tc.innerHTML = '<embed src="_sounds/' + name + '.wav" autostart="true" loop="false" hidden="true" width="1" height="1"></embed>';
				}
			}
		};

		doh._updateGlobalProgressBar = function(p, success, group){
			var outerContainer = byId("progressOuter");

			var gdiv = outerContainer.childNodes[doh._runedSuite - 1];
			if(!gdiv){
				gdiv = document.createElement('div');
				outerContainer.appendChild(gdiv);
				gdiv.className = 'success';
				gdiv.setAttribute('_target', group);
			}
			if(!success && !gdiv._failure){
				gdiv._failure = true;
				gdiv.className = 'failure';
				if(group){
					gdiv.setAttribute('title', 'failed group ' + group);
				}
			}
			var tp = parseInt(p * 10000) / 100;
			gdiv.style.width = (tp - doh._currentGlobalProgressBarWidth) + "%";
			return gdiv._failure;
		};
		doh._testFinished = function(group, fixture, success){
			var fn = getFixtureNode(group, fixture);
			var elapsed = fixture.endTime-fixture.startTime;
			var gn;
			if(fn){
				fn.getElementsByTagName("td")[3].innerHTML = formatTime(elapsed);
				fn.className = (success) ? "success" : "failure";
				fn.getElementsByTagName("td")[2].setAttribute('_target', _loggedMsgLen);
				if(!success){
					_playSound("doh");
					gn = getGroupNode(group);
					if(gn){
						gn.className = "failure";
						_getGroupToggler(group)(null, true);
					}
				}
			}
			if(doh._inGroup == group){
				gn = getGroupNode(group);
				doh._runed++;
				if(gn && doh._curTestCount){
					var p = doh._runed/doh._curTestCount;
					var groupfail = this._updateGlobalProgressBar((doh._runedSuite+p-1)/doh._groupCount,success,group);

					var pbar = gn.getElementsByTagName("td")[2].lastChild;
					pbar.className = groupfail?"failure":"success";
					pbar.style.width = parseInt(p*100)+"%";
					gn.getElementsByTagName("td")[3].innerHTML = parseInt(p*10000)/100+"%";
				}
			}
			this._groupTotalTime += elapsed;
			this.debug((success ? "PASSED" : "FAILED"), "test:", fixture.name, elapsed, 'ms');
		};

		doh._registerUrl = function(group, url, timeout, type, dohArgs){
			group= group || url;
			this._registerTest(group, {
				name: url,
				setUp: function(){
					doh.currentGroupName = group;
					doh.currentGroup = this;
					doh.currentUrl = url;
					doh.dohArgs = dohArgs;
					this.d = new doh.Deferred();
					doh.currentTestDeferred = this.d;
					doh.showTestPage();
					byId("testBody").src = url;
				},
				timeout: timeout||10000, // 10s
				// timeout: timeout||1000, // 10s
				runTest: function(){
					// FIXME: implement calling into the url's groups here!!
					return this.d;
				},
				tearDown: function(){
					doh.currentGroupName = null;
					doh.currentGroup = null;
					doh.currentTestDeferred = null;
					doh.currentUrl = null;
					// this.d.errback(false);
					// byId("testBody").src = "about:blank";
					doh.showLogPage();
				}
			}, type);
		};

		//
		// Utility code for runner.html
		//
		// var isSafari = navigator.appVersion.indexOf("Safari") >= 0;
		var tabzidx = 1;
		var _showTab = function(toShow, toHide){
			// FIXME: I don't like hiding things this way.
			var i;
			for(i = 0; i < toHide.length; i++){
				var node = byId(toHide[i]);
				if(node){
					node.style.display = "none";
				}
			}
			toShow = byId(toShow);
			if(toShow){
				toShow.style.display = "";
				toShow.style.zIndex = ++tabzidx;
			}
		};

		doh.showTestPage = function(){
			_showTab("testBody", ["logBody", "perfTestsBody"]);
		};

		doh.showLogPage = function(){
			_showTab("logBody", ["testBody", "perfTestsBody"]);
		};

		doh.showPerfTestsPage = function(){
			_showTab("perfTestsBody", ["testBody", "logBody"]);
		};

		var runAll = true;
		doh.toggleRunAll = function(){
			// would be easier w/ query...sigh
			runAll = !runAll;
			if(!byId("testList")){
				return;
			}
			var tb = byId("testList").tBodies[0];
			var inputs = tb.getElementsByTagName("input");
			var x = 0;
			var tn;
			while((tn = inputs[x++])){
				tn.checked = runAll;
				doh._groups[tn.group].skip = (!runAll);
			}
		};

		var listHeightTimer = null;
		var setListHeight = function(){
			if(listHeightTimer){
				clearTimeout(listHeightTimer);
			}
			var tl = byId("testList");
			if(!tl){
				return;
			}
			listHeightTimer = setTimeout(function(){
				tl.style.display = "none";
				tl.style.display = "";

			}, 10);
		};

		_addOnEvt("resize", setListHeight);
		_addOnEvt("load", setListHeight);
		_addOnEvt("load", function(){
			if(loaded){ return; }
			loaded = true;
			groupTemplate = byId("groupTemplate");
			if(!groupTemplate){
				// make sure we've got an amenable DOM structure
				return;
			}
			groupTemplate.parentNode.removeChild(groupTemplate);
			groupTemplate.style.display = "";
			testTemplate = byId("testTemplate");
			testTemplate.parentNode.removeChild(testTemplate);
			testTemplate.style.display = "";
			doh._updateTestList();
		});

		_addOnEvt("load",
			function(){
				// let robot code run if it gets to this first
				var __onEnd = doh._onEnd;
				doh._onEnd = function(){
					__onEnd.apply(doh, arguments);
					if(doh._failureCount == 0){
						doh.debug("WOOHOO!!");
						_playSound("woohoo");
					}else{
						console.debug("doh._failureCount:", doh._failureCount);
					}
					if(byId("play")){
						toggleRunning();
					}
				};
				if(!byId("play")){
					// make sure we've got an amenable DOM structure
					return;
				}
				var isRunning = false;
				var toggleRunning = function(){
					// ugg, this would be so much better w/ dojo.query()
					if(isRunning){
						byId("play").style.display = byId("pausedMsg").style.display = "";
						byId("playingMsg").style.display = byId("pause").style.display = "none";
						isRunning = false;
					}else{
						byId("play").style.display = byId("pausedMsg").style.display = "none";
						byId("playingMsg").style.display = byId("pause").style.display = "";
						isRunning = true;
					}
				};
				doh.run = (function(oldRun){
					return function(){
						if(!doh._currentGroup){
							toggleRunning();
						}
						return oldRun.apply(doh, arguments);
					}
				})(doh.run);
				var btns = byId("toggleButtons").getElementsByTagName("span");
				var node; var idx=0;
				while((node=btns[idx++])){
					node.onclick = toggleRunning;
				}
			}
		);
	}else{
		// we're in an iframe environment. Time to mix it up a bit.

		var _doh = window.parent.doh;
		var _thisGroup = _doh.currentGroupName;
		var _thisUrl = _doh.currentUrl;
		if(_thisGroup){
			doh._onEnd = function(){
				_doh._errorCount += doh._errorCount;
				_doh._failureCount += doh._failureCount;
				_doh._testCount += doh._testCount;
				// should we be really adding raw group counts?
				//_doh._groupCount += doh._groupCount;
				_doh.currentTestDeferred.callback(true);
			};
			doh._testRegistered = function(group, fixture){
				fixture.name = _thisUrl+"::"+arguments[0]+"::"+fixture.name;
				_doh._updateTestList(_thisGroup, fixture);
			};
			doh.debug = lang.hitch(_doh, "debug");
			doh.error = lang.hitch(_doh, "error");
			doh.registerUrl = lang.hitch(_doh, "registerUrl");
			doh._testStarted = function(group, fixture){
				_doh._testStarted(_thisGroup, fixture);
			};
			doh._testFinished = function(g, f, s){
				_doh._testFinished(_thisGroup, f, s);

				//Okay, there may be performance info we need to filter back
				//to the parent, so do that here.
				if(doh.perfTestResults){
					try{
						var gName = g.toString();
						var localFName = f.name;
						while(localFName.indexOf("::") >= 0){
							localFName = localFName.substring(localFName.indexOf("::") + 2, localFName.length);
						}
						if(!_doh.perfTestResults){
							_doh.perfTestResults = {};
						}
						if(!_doh.perfTestResults[gName]){
							_doh.perfTestResults[gName] = {};
						}
						_doh.perfTestResults[gName][f.name] = doh.perfTestResults[gName][localFName];
					}catch (e){
						doh.debug(e);
					}
				}
			};
			doh._groupStarted = function(){
				if(!this._setParent){
					_doh._curTestCount = this._testCount;
					_doh._curGroupCount = this._groupCount;
					this._setParent = true;
				}
			};
			doh._report = function(){
			};
		}
	}

	var fixHeight = doh._fixHeight = function(){
		// IE9 doesn't give test iframe height because no nodes have an explicit pixel height!
		// Give outer table a pixel height.
		if(has("ie")){
			var headerHeight = 0;
			var rows = query('#testLayout > tbody > tr');
			for(var i = 0; i < rows.length-1; i++){
				headerHeight += domGeom.position(rows[i]).h;
			}
			try{
				// we subtract the headerHeight from the window height because the table row containing the tests is height:100% so they will stretch the table to the intended height.
				dom.byId('testLayout').style.height = (win.getBox().h - headerHeight)+"px";
			}catch(e){
				// An obscure race condition when you load the runner in IE from the command line causes the window reported height to be 0.
				// Try to recover after the window finishes rendering.
				setTimeout(function(){ fixHeight(); },0);
			}
		}
	};

	return doh;
});
