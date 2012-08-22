max_show = 20;
settings = {
	sort_type : "time",
	show_count : max_show,
	show_thumbs : false,
	show_utc : true,
	filter_sol : -1,
	filter_cam : [],
	filter_thumbs : false,
	filter_new : false,
	filter_list : "none",
	current_list : {},
	useWget : true,
	wget_syntax : "wget -i -"
}
conf = {};
if (typeof (localStorage["msl-raws-conf"]) != "undefined"
		&& localStorage["msl-raws-conf"] != "") {
	conf = JSON.parse(localStorage["msl-raws-conf"]);
}
conf = $.extend({}, settings, conf);

data = [];
errorCloseButton = "&nbsp;<a href='#' class='cleanLink' onClick='$(\".error\").html(\"\")'>(x)</a>";

/*
 * Lists management
 */
lists = {};
if (typeof (localStorage["msl-raws-lists"]) != "undefined"
	&& localStorage["msl-raws-lists"] != "") {
	try {
		var tmp_lists = JSON.parse(localStorage["msl-raws-lists"]);
		if (tmp_lists.length>=0){
			lists=tmp_lists;
		}
	} catch (e){}
}

function createList(){
	if ($(".listLabel").val() != ""){
		conf.current_list = {
			uuid: new UUID(),
			name: $(".listLabel").val(),
			description: $(".listDescr").val(),
			url: $(".listURL").val(),
			uuids: selectedImages()
		}
		lists[conf.current_list.uuid]=(conf.current_list);
		localStorage["msl-raws-lists"]=JSON.stringify(lists);
		render();
		$(".listCreate input:not(.close)").val("");
	} else {
		$(".error")
		.html("Sorry, you'll have to provide a list name!"+errorCloseButton);
	}
}
function deleteList(){
	delete lists[conf.current_list.uuid];
	conf.current_list = {};
	localStorage["msl-raws-lists"]=JSON.stringify(lists);
	render();
}

function addToList(){
	$(".error")
	.html("Sorry, I haven't implemented that button yet!"+errorCloseButton);
}
function removeFromList(){
	$(".error")
	.html("Sorry, I haven't implemented that button yet!"+errorCloseButton);	
}
function openLists(){
	$(".error")
	.html("Sorry, I haven't implemented that button yet!"+errorCloseButton);
}
/*
 * Tools
 */
if (!Array.prototype.map) {
	Array.prototype.map = function(fun /* , thisp */) {
		var len = this.length;
		if (typeof fun != "function")
			throw new TypeError();

		var res = new Array(len);
		var thisp = arguments[1];
		for ( var i = 0; i < len; i++) {
			if (i in this)
				res[i] = fun.call(thisp, this[i], i, this);
		}

		return res;
	};
}
function pad(number, length) {
	var str = '' + number;
	while (str.length < length) {
		str = '0' + str;
	}
	return str;
}


/*
 * Image listing
 */
highest_sol = 0;
count = 0;
totalImages = 0;
camList = "";
newLimit = 0;

function filter(a) {
	if (parseInt(a.item.sol) > highest_sol)
		highest_sol = parseInt(a.item.sol);
	if (conf.filter_list == "include"
			&& $.inArray(a.item.uuid,conf.current_list.uuids) < 0) return false;
	if (conf.filter_list == "exclude"
			&& $.inArray(a.item.uuid,conf.current_list.uuids) >= 0) return false;
	if (conf.filter_list == "complete"){
		if ($.inArray(a.item.uuid,conf.current_list.uuids) < 0){
			return false;
		}
	} else {
		if (conf.filter_new
				&& parseFloat(a.item.lastModified == "" ? a.item.unixTimeStamp
						: a.item.lastModified) < newLimit)
			return false;
		if (conf.filter_sol >= 0 && a.item.sol != conf.filter_sol)
			return false;
		if (conf.filter_thumbs
				&& (a.item.type == "thumbnail" || a.item.type == "downscaled"))
			return false;
		if (conf.filter_cam.length > 0
				&& $.inArray(a.item.camera, conf.filter_cam) == -1)
			return false;		
	}
	totalImages++;
	if (count++ > conf.show_count)
		return false;
	return true;
}
function sort(a, b) {
	if (conf.sort_type == "time") {
		return parseFloat(a.unixTimeStamp) < parseFloat(b.unixTimeStamp) ? 1
				: -1;
	} else {
		if (a.lastModified == "")
			a.lastModified = a.unixTimeStamp;
		if (b.lastModified == "")
			b.lastModified = b.unixTimeStamp;
		return parseInt(a.lastModified) < parseInt(b.lastModified) ? 1 : -1;
	}
	return 1;
}
function saveConf() {
	localStorage["msl-raws-conf"] = JSON.stringify(conf);
}
function render() {
	newLimit = new Date().getTime() - (24 * 3600 * 1000);
	localStorage["msl-raws-conf"] = JSON.stringify(conf);

	$(".less").toggle(conf.show_count > max_show);
	$('.tab-target th.name').html("Name (loading....)");
	count = 0;
	totalImages = 0;
	$('.tab-target')
			.replaceWith(
					$('.tab-template')
							.clone()
							.directives(
									{
										'.thumbBox@checked' : function(a) {
											return conf.show_thumbs ? "checked" : ""
										},
										'.solBox@checked' : function(a) {
											return conf.filter_sol >= 0 ? "" : "checked"
										},
										'.typeBox@checked' : function(a) {
											return conf.filter_thumbs ? "checked"
													: ""
										},
										'.sortBox@checked' : function(a) {
											return conf.sort_type == "modified" ? "checked"	: ""
										},
										'.newBox@checked' : function(a) {
											return conf.filter_new ? "checked" : ""
										},
										'.utcBox@checked' : function(a) {
											return conf.show_utc ? "checked" : ""
										},
										'.line-template' : {
											'image<-context' : {
												'.name' : 'image.name',
												'.name@href' : 'image.url',
												'input.selector@value' : 'image.uuid',
												'.sol' : 'image.sol',
												'.type' : 'image.type',
												'.cam' : 'image.camera',
												'.newImage@class+' : function(a) {
													if (parseFloat(a.item.lastModified == "" ? a.item.unixTimeStamp	: a.item.lastModified) >= newLimit) {
														return " show";
													}
													return "";
												},
												'.thumbnail' : function(a) {
													if (conf.show_thumbs) {
														return '<img src="'
																+ a.item.thumbnailUrl
																+ '">';
													}
													return "";// TODO: link to
																// load single
																// image
												},
												'.date' : function(a) {
													var millis = 0;
													var date = new Date(millis);
													if (conf.sort_type == "modified") {
														if (a.item.lastModified != "") {
															millis = parseFloat(a.item.lastModified);
															date = new Date(millis);
														}
													} else {
														millis = parseFloat(a.item.unixTimeStamp);
														date = new Date(millis);
													}
													if (conf.show_utc) {
														return date.getUTCFullYear()
																+ "-" + pad(date.getUTCMonth() + 1,2)
																+ "-" + pad(date.getUTCDate(),2)
																+ " " + pad(date.getUTCHours(),2)
																+ ":" + pad(date.getUTCMinutes(),2)
																+ ":" + pad(date.getUTCSeconds(),2)
																+ " UTC";
													} else {
														var offset = -date.getTimezoneOffset() / 60;
														return date.getFullYear()
														+ "-" + pad(date.getMonth() + 1,2)
														+ "-" + pad(date.getDate(),2)
														+ " " + pad(date.getHours(),2)
														+ ":" + pad(date.getMinutes(),2)
														+ ":" + pad(date.getSeconds(),2)
														+ " UTC" + (offset >= 0 ? "+":"-") + offset;
													}
												}
											},
											sort : function(a, b) {
												return sort(a, b);
											},
											filter : function(a) {
												return filter(a);
											}
										}
									}).render(data).removeClass('tab-template')
							.addClass('tab-target'));

	// Revert input fields to conf values:
	$(".imageCount").html(
			totalImages + " of " + data.length + " images selected");
	if (conf.filter_sol < 0) {
		$(".solInput").val(highest_sol);
	} else {
		$(".solInput").val(conf.filter_sol);
	}
	$('.sol .newest').html(" ->" + highest_sol);
	$(".more,.all").toggle(count >= conf.show_count);
	$(".tab-target .camSelectMulti").val(conf.filter_cam);
	$(".tab-target .camSelectMulti").chosen();
	$(".tab-target input.selector").enableCheckboxRangeSelection();
	$(".tab-target input.selector").on('click',toggleSelector);
	$(".tab-target .listSelect").val(conf.filter_list);
	$(".currentList").html(typeof conf.current_list.name != "undefined"?conf.current_list.name:"none");
	toggleSelector();
	
	// ALthough not necessary on all redraws, no problem:
	$(".prefixText").val(conf.wget_syntax);
	$(".prefixBox").attr('checked', conf.useWget);
	// Done
}
function selectList(){
	$(".tab-target input.selector").each(function(){
		var $this = $(this);
		$.inArray($this.attr('value'),conf.current_list.uuids)>=0?$this.attr("checked","checked"):$this.removeAttr("checked");
	});
	toggleSelector();
}
function toggleSelector(){
	selectedImages().length>0?$(".toggleSelector").attr("checked","checked"):$(".toggleSelector").removeAttr("checked");
}
function selectedImages() {
	var selected = new Array();
	$('.tab-target input.selector:checked').each(function() {
		selected.push($(this).attr('value'));
	});
	return selected;
}
function selectedImagesList() {
	var selected = selectedImages();
	if (selected.length == 0)
		return {};
	var result = {};
	data.map(function(image) {
		if ($.inArray(image.uuid, selected) >= 0) {
			result[image.uuid] = image;
		}
	});
	return result;
}
function outputList() {
	var newPage = "";
	var html = $.inArray(BrowserDetect.browser, [ "Safari", "Chrome" ]) >= 0;
	if (conf.useWget) {
		newPage += conf.wget_syntax + ' << ENDOFURLS' + (html ? "<br>" : "\n");
	}
	var list = selectedImagesList();
	selectedImages().map(function(imageId) {
		newPage += list[imageId].url + (html ? "<br>" : "\n");
	});
	if (conf.useWget)
		newPage += "ENDOFURLS" + (html ? "<br>" : "\n");
	var p = window.open('');
	p.document.open("text/plain");
	p.document.write(newPage);
	p.document.close();
}
function reload() {
	if (typeof (localStorage["msl-raws-data"]) != "undefined"
			&& localStorage["msl-raws-data"] != "") {
		try {
			var storedData = JSON.parse(localStorage["msl-raws-data"]);
			if (storedData.length > 0) {
				data = storedData;
				render();
			}
		} catch (e) {
		}
		;
	}
	$.ajax({
				url : "/landing?flat",
				statusCode : {
					200 : function(json) {
						data = json;
						localStorage["msl-raws-data"] = JSON.stringify(data);
						render();
					}
				},
				error : function() {
					$(".error")
							.html(
									"I'm sorry, but something went wrong while trying to reach the server, please try again.<a href='#' onClick='$(\".error\").html(\"\")'>(x)</a>");
				}
			});
}
function startSharrre() {
	$("#sharrre")
			.sharrre(
					{
						share : {
							googlePlus : true,
							facebook : true,
							twitter : true
						},
						template : '<div class="box"><div class="left">Share</div><div class="middle"><a href="#" class="facebook">f</a><a href="#" class="twitter">t</a><a href="#" class="googleplus">+1</a></div><div class="right">{total}</div></div>',
						enableHover : false,
						enableTracking : true,
						enableCounter : true,
						buttons : {
							twitter : {
								via : 'Stellingwerff'
							}
						},
						render : function(api, options) {
							$(api.element).on('click', '.twitter', function() {
								api.openPopup('twitter');
							});
							$(api.element).on('click', '.facebook', function() {
								api.openPopup('facebook');
							});
							$(api.element).on('click', '.googleplus',
									function() {
										api.openPopup('googlePlus');
									});
						},
						urlCurl : "",
						title : "Follow the raw images Curiosity sends back to Earth each sol. This site is focused on providing easy access for downloading, listing and organizing the raw images."

					});
}

var _gaq = _gaq || [];
_gaq.push([ '_setAccount', 'UA-34102591-1' ]);
_gaq.push([ '_trackPageview' ]);

(function() {
	var ga = document.createElement('script');
	ga.type = 'text/javascript';
	ga.async = true;
	ga.src = ('https:' == document.location.protocol ? 'https://ssl'
			: 'http://www')
			+ '.google-analytics.com/ga.js';
	var s = document.getElementsByTagName('script')[0];
	s.parentNode.insertBefore(ga, s);
})();

$(document).ready(function() {
	var reloadString = "<a href='#' class='reload cleanLink' onClick='reload()'>Reload</a>";
	$("div.reload").html(reloadString);
	$("div.reload").ajaxStart(function() {
		$(this).html("<span class='reloading'>Loading...</span>")})
	.ajaxStop(function() {
		$(this).html(reloadString)});
	
	reload();
	startSharrre();
	$("input:button").button();
	$(".prefix").dialog({
		autoOpen : false,
		title : "export prefix",
		modal : true
	});
	$(".listCreate").dialog({
		autoOpen : false,
		title : "create list",
		modal : true		
	});
	$(".listDelete").dialog({
		autoOpen : false,
		title : "delete list",
		modal : true		
	});
	$(".listHelp").dialog({
		autoOpen : false,
		title : "legenda",
		modal : true,
		width : "40em"
	});
});
