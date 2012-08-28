
var jpl = "http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol";
var msss = "http://mars.jpl.nasa.gov/msl-raw-images/msss";
var errorCloseButton = "&nbsp;<a href='#' class='cleanLink' onClick='$(\".error\").html(\"\")'><span class='ui-icon ui-icon-circle-close inline'></span></a>";
dyn = {
	data:[],
	highest_sol:0,
	pagecount:0,
	totalImages:0,
	newLimit:0,
	fullList:[],
	temp_render_max:false
}

var settings = {
	sort_type : "time",
	show_thumbs : false,
	show_utc : true,
	filter_sol : -1,
	filter_cam : [],
	filter_type : ["F","S","D"],
	filter_new : false,
	filter_list : "none",
	current_list : {},
	useWget : true,
	wget_syntax : "wget -i -",
	max_show:20,
	show_count : 20,
	render_max:500
}

conf = {};
if (typeof (localStorage["msl-raws-conf"]) != "undefined"
		&& localStorage["msl-raws-conf"] != "") {
	conf = JSON.parse(localStorage["msl-raws-conf"]);
}
conf = $.extend({}, settings, conf);

local_lists = {};
if (typeof localStorage["msl-raws-lists"] != "undefined"
	&& localStorage["msl-raws-lists"] != "") {
	try {
		var tmp_lists = JSON.parse(localStorage["msl-raws-lists"]);
		local_lists=tmp_lists;
	} catch (e){ console.log("couldn't read lists:",e)}
}
subscribed_lists = {};
if (typeof localStorage["msl-raws-subscriptions"] != "undefined"
	&& localStorage["msl-raws-subscriptions"] != "") {
	try {
		var tmp_lists = JSON.parse(localStorage["msl-raws-subscriptions"]);
		subscribed_lists=tmp_lists;
	} catch (e){ console.log("couldn't read lists:",e)}
}

/*
 * Tools
 */
$.extend({
    getUrlVars : function() {
            var vars = [], hash;
            var loc = window.location.href;
            if (loc.indexOf('#') >=0){
            	loc = loc.slice(0,loc.indexOf('#'));
            }
            var hashes = loc.slice(loc.indexOf('?') + 1).split('&');
            for(var i = 0; i < hashes.length; i++) {
                    hash = hashes[i].split('=');
                    vars.push(hash[0]);
                    vars[hash[0]] = hash[1];
            }
            return vars;
    },
    getUrlVar : function(name) {
            return $.getUrlVars()[name];
    }
});
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

/**
 * List management
 */
function createList(){
	if ($(".listCreate .listLabel").val() != ""){
		conf.current_list = {
			uuid: new UUID(),
			name: $(".listCreate .listLabel").val(),
			description: $(".listCreate .listDescr").val(),
			url: $(".listCreate .listURL").val(),
			uuids: selectedImages()
		}
		local_lists[conf.current_list.uuid.id]=(conf.current_list);
		localStorage["msl-raws-lists"]=JSON.stringify(local_lists);
		render();
		$(".listCreate input:not(.close),.listCreate textarea").val("");
		selectList();
	} else {
		$(".error")
		.html("Sorry, you'll have to provide a list name!"+errorCloseButton);
	}
}
function deleteList(){
	delete local_lists[conf.current_list.uuid.id];
	conf.current_list = {};
	localStorage["msl-raws-lists"]=JSON.stringify(local_lists);
	render();
}
function publishList(listId){
	var list = JSON.stringify(local_lists[listId]);
	console.log("sending:"+list);
	$.ajax({
		url:"/lists/id/"+listId,
		type:"PUT",
		processdata:false,
		data:list
	});
}
function subscribeToList(listId){
	$(".error")
	.html("Sorry, that button hasn't been implemented yet!"+errorCloseButton);	
}
function addToList(){
	var images = selectedImages();
	var list = conf.current_list.uuids;
	images.map(function (uuid){
		if ($.inArray(uuid,list)<0){
			list.push(uuid);
		}
	});
	localStorage["msl-raws-lists"]=JSON.stringify(local_lists);
	render();
	selectList();
}
function removeFromList(){
	var images = selectedImages();
	var list = conf.current_list.uuids;
	images.map(function (uuid){
		var pos = $.inArray(uuid,list); 
		if (pos>=0){
			list.splice(pos,1);
		}
	});
	localStorage["msl-raws-lists"]=JSON.stringify(local_lists);
	render();
	selectList();
}

function openList(listId){
	conf.current_list=local_lists[listId];
	conf.filter_list="complete";
	$tabs.tabs('select',0);
}

function renderLists(){
	
	$('.listBrowser-target')
	.replaceWith(
			$('.listBrowser-template')
					.clone()
					.directives({
						'.line-template' : {
							'listItem<-context' : {
								".listPublish@onClick":function(a){
									return "publishList('"+a.item.uuid.id+"')";
								},
								".listLabel@onClick":function(a){
									return "openList('"+a.item.uuid.id+"')";
								},
								".listLabel":"listItem.name",
								".listCount":"listItem.uuids.length",
								".listDescr":"listItem.description",
								".listUrl@href":"listItem.url",
								".listUrl":function(a){ return a.item.url==""?"":"link"}
							}
						}
					}).render(local_lists).removeClass('listBrowser-template')
					.addClass('listBrowser-target'));
	$(".listBrowser-target .line-filler").toggle($.isEmptyObject(local_lists));
}
function renderSubscriptions(){	
	$('.subscriptionBrowser-target')
	.replaceWith(
			$('.subscriptionBrowser-template')
					.clone()
					.directives({
						'.line-template' : {
							'listItem<-context' : {
								".listLabel@onClick":function(a){
									return "openList('"+a.item.uuid.id+"')";
								},
								".listLabel":"listItem.name",
								".listCount":"listItem.uuids.length",
								".listDescr":"listItem.description",
								".listUrl@href":"listItem.url",
								".listUrl":function(a){ return a.item.url==""?"":"link"}
							}
						}
					}).render(subscribed_lists).removeClass('subscriptionBrowser-template')
					.addClass('subscriptionBrowser-target'));
	$(".subscriptionBrowser-target .line-filler").toggle($.isEmptyObject(subscribed_lists));
}
/*
 * Image listing
 */
function filter(a) {
	if (parseInt(a.item.sol) > dyn.highest_sol)
		dyn.highest_sol = parseInt(a.item.sol);
	var skip = typeof conf.current_list == "undefined" || typeof conf.current_list.name == "undefined";
	if (!skip && conf.filter_list == "include"
			&& $.inArray(a.item.uuid,conf.current_list.uuids) < 0) return false;
	if (!skip && conf.filter_list == "exclude"
			&& $.inArray(a.item.uuid,conf.current_list.uuids) >= 0) return false;
	if (!skip && conf.filter_list == "complete"){
		if ($.inArray(a.item.uuid,conf.current_list.uuids) < 0){
			return false;
		}
	} else {
		if (conf.filter_new
				&& parseFloat(a.item.lastModified == "" ? a.item.unixTimeStamp
						: a.item.lastModified) < dyn.newLimit)
			return false;
		if (conf.filter_sol >= 0 && a.item.sol != conf.filter_sol)
			return false;
		if (conf.filter_type.length > 0
				&& $.inArray(a.item.type.substring(0,1).toUpperCase(), conf.filter_type) == -1)
			return false;		
		if (conf.filter_cam.length > 0
				&& $.inArray(a.item.camera, conf.filter_cam) == -1)
			return false;		
	}
	dyn.totalImages++;
	dyn.fullList.push(a.item.uuid);
	if (!dyn.temp_render_max && dyn.pagecount > conf.render_max)
		return false;
	if (dyn.pagecount > conf.show_count)
		return false;
	dyn.pagecount++;
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
	dyn.newLimit = new Date().getTime() - (24 * 3600 * 1000);
	localStorage["msl-raws-conf"] = JSON.stringify(conf);

	$('.tab-target th.name').html("Name (loading....)");
	$('.error').html("");
	dyn.pagecount = 0;
	dyn.totalImages = 0;
	dyn.fullList=[];
	
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
												'.name@href' : function(a){
													return a.item.url[1]=="$"?(a.item.url[0]=="J"?jpl:msss)+a.item.url.substring(2):a.item.url;
												},
												'input.selector@value' : 'image.uuid',
												'.sol' : 'image.sol',
												'.type' : 'image.type',
												'.cam' : 'image.camera',
												'.newImage@class+' : function(a) {
													if (parseFloat(a.item.lastModified == "" ? a.item.unixTimeStamp	: a.item.lastModified) >= dyn.newLimit) {
														return " show";
													}
													return "";
												},
												'.thumbnail' : function(a) {
													if (conf.show_thumbs) {
														return '<img src="'
																+ (a.item.thumbnailUrl[1]=="$"?(a.item.url[0]=="J"?jpl:msss)+a.item.thumbnailUrl.substring(2):a.item.thumbnailUrl)
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
									}).render(dyn.data).removeClass('tab-template')
							.addClass('tab-target'));

	// Revert input fields to conf values:
	if (conf.filter_sol < 0) {
		$(".solInput").val(dyn.highest_sol);
	} else {
		$(".solInput").val(conf.filter_sol);
	}
	$(".tab-target .typeSelectMulti").val(conf.filter_type);
	$(".tab-target .typeSelectMulti").chosen();
	
	$(".tab-target .camSelectMulti").val(conf.filter_cam);
	$(".tab-target .camSelectMulti").chosen();
	$(".tab-target input.selector").enableCheckboxRangeSelection();
	$(".tab-target input.selector").on('click',toggleSelector);
	$(".tab-target .listSelect").val(conf.filter_list);
	toggleSelector();

	//Navigation stuff
	$(".line-infolist").toggle(conf.filter_list=="complete" && typeof conf.current_list.name != "undefined");
	$(".tab-target .line-filler").toggle(dyn.totalImages==0); 
	$(".tab-target .less,.base").toggle(conf.show_count > conf.max_show);
	if (conf.show_count == 2*conf.max_show) $(".tab-target .less").hide();
	$(".tab-target .more,.all").toggle(dyn.pagecount >= conf.show_count);
	$(".tab-target .now").html(conf.show_count==999999?"all":conf.show_count);
	$(".tab-target .more").val("+"+conf.max_show);
	$(".tab-target .less").val("-"+conf.max_show);
	$(".tab-target .base").val("only "+conf.max_show);
	$(".tab-target .all").val("all "+dyn.totalImages);

	//Set some global info fields:
	$(".tab-target .imageCount").html(
			dyn.totalImages + " of " + dyn.data.length + " images selected");
	$(".tab-target .currentList,.listDelete .currentList").html(typeof conf.current_list.name != "undefined"?conf.current_list.name:"####");
	$(".tab-target .listCount").html(typeof conf.current_list.name != "undefined"?conf.current_list.uuids.length+" images":"");
	$('.tab-target .sol .newest').html(" ->" + dyn.highest_sol);
	
	$(".listInfo").html(($(".listInfo").clone().render(conf.current_list,{
		".listLabel" : "name",
		".listCount" : function(a){ return typeof conf.current_list.name != "undefined"?conf.current_list.uuids.length+" images":"" },
		".listDescr" : "description",
		".listURL" : function(a){ return conf.current_list.url==""?"":"<a href='"+conf.current_list.url+"' target='_blank'>"+conf.current_list.url+"</a>"}
	})).html());
	
	$('input.render_max').val(conf.render_max);
	$('input.max_show').val(conf.max_show);

	if (dyn.pagecount >= conf.render_max) {
		$(".error")
		.html("Sorry, cowardly refusing to render more than "+conf.render_max+" images in one page!"+errorCloseButton+"<br><a href='#' onClick='dyn.temp_render_max=true;render();'>Temporary lift limit, really sure?</a>");
	}
	$(".tab-target input:button").button('refresh');

	// ALthough not necessary on all redraws, no problem:
	$(".prefixText").val(conf.wget_syntax);
	$(".prefixBox").attr('checked', conf.useWget);
	// Done
}

function selectList(){
	if (typeof conf.current_list.uuids == "undefined") return;
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
	dyn.data.map(function(image) {
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
	$("div.reload").html("<span class='reloading'>Loading...</span>");
	$(".error").html("");
	if (typeof _gaq != "undefined"){
		_gaq.push(['_trackEvent', "imageList", "Reload"]);
	}
	$.ajax({
				url : "/landing?flat",
				statusCode : {
					200 : function(json) {
						dyn.data = json;
						if (typeof dyn.data == "string"){
							dyn.data = JSON.parse(json);
						}
						render();
					}
				},
				error : function() {
					$(".error")
							.html(
									"I'm sorry, but something went wrong while trying to reach the server, please try again."+errorCloseButton);
				}
			});
}


	
$(document).ready(function() {
	var reloadString = "<a href='#' class='reload cleanLink' onClick='reload()'>Reload</a>";
	$("div.reload").html(reloadString);
	$("div.reload").ajaxStart(function() {
		$(this).html("<span class='reloading'>Loading...</span>")})
	.ajaxStop(function() {
		$(this).html(reloadString)});
	$("input:button").button();

	var listId = $.getUrlVar("list");
	if (typeof listId != "undefined" && listId != "" && typeof local_lists[listId] != "undefined"){
		conf.current_list=local_lists[listId];
		conf.filter_list="complete";
	}
	$tabs = $(".tabContainer").tabs({
		selected:0,
		show: function(event, ui) {
			if (ui.panel.id == "Lists") renderLists();
			if (ui.panel.id == "Subscriptions") renderSubscriptions();
			if (ui.panel.id == "Images") render();			
		}
	});
	$(".listCreate").dialog({
		autoOpen : false,
		title : "Create list",
		modal : true,
		width : "50em"		
	});
	$(".listDelete").dialog({
		autoOpen : false,
		title : "Delete list",
		modal : true		
	});
	$(".listInfo").dialog({
		autoOpen : false,
		title : "Meta info",
		modal : true,
		width : "50em"
	});
	$(".listHelp").dialog({
		autoOpen : false,
		title : "Legenda",
		modal : true,
		width : "40em"
	});
	$(".preferences").dialog({
		autoOpen : false,
		title : "Preferences",
		modal : true,
		width : "35em"
	});
	render();
	reload();
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
	!function(d,s,id){
		var js,fjs=d.getElementsByTagName(s)[0];
		if(!d.getElementById(id)){
			js=d.createElement(s);
			js.id=id;
			js.src="//platform.twitter.com/widgets.js";
			fjs.parentNode.insertBefore(js,fjs);
		}}(document,"script","twitter-wjs")
	;
	stLight.options({
			publisher : "d1b23a5c-25a8-40b8-aa83-a7d19b05523d"
	});
});
