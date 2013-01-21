
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
};

var settings = {
	sort_column : "unixTimeStamp",
	sort_reverse : false,
	show_thumbs : false,
	show_utc : true,
	show_lag : true,
	show_orientation : true,
	display_mode: "normal",
	filter_sol : -1,
	filter_cam : [],
	filter_type : ["F","S","D"],
	filter_new : false,
	filter_list : "none",
	current_list : {},
	localList: true,
	useWget : true,
	wget_syntax : "wget -i -",
	max_show:20,
	show_count : 20,
	render_max:500,
	font_size:10
};

conf = {};
if (typeof (localStorage["msl-raws-conf"]) != "undefined"
		&& localStorage["msl-raws-conf"] != "") {
	conf = JSON.parse(localStorage["msl-raws-conf"]);
}
conf = $.extend({}, settings, conf);
if (typeof conf.personalId == "undefined"){
	conf.personalId = new UUID();
}

local_lists = {};
if (typeof localStorage["msl-raws-lists"] != "undefined"
	&& localStorage["msl-raws-lists"] != "") {
	try {
		var tmp_lists = JSON.parse(localStorage["msl-raws-lists"]);
		local_lists=tmp_lists;
	} catch (e){ console.log("couldn't read lists:",e);}
}
subscriptions = [];
if (typeof localStorage["msl-raws-subscriptions"] != "undefined"
	&& localStorage["msl-raws-subscriptions"] != "") {
	try {
		var tmp_lists = JSON.parse(localStorage["msl-raws-subscriptions"]);
		subscriptions=tmp_lists;
	} catch (e){ console.log("couldn't read subscriptions:",e);}
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
                    if (typeof hash[1] != "string"){
                    	hash[1]="";
                    }
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

function render_timezone(){
	if (conf.show_utc){
		return "UTC";
	}
	var date = new Date();
	var offset = -date.getTimezoneOffset() / 60;
	return "UTC" + (offset >= 0 ? "+":"-") + offset;
}

function renderDate(date,utc,addUTC){
	if (isNaN(date.getTime())) return "---";
	if (typeof utc != "undefined" && utc){
		return date.getUTCFullYear()
		+ "-" + pad(date.getUTCMonth() + 1,2)
		+ "-" + pad(date.getUTCDate(),2)
		+ " " + pad(date.getUTCHours(),2)
		+ ":" + pad(date.getUTCMinutes(),2)
		+ ":" + pad(date.getUTCSeconds(),2)
		+ (addUTC?" UTC":"");		
	} else {
		var offset = -date.getTimezoneOffset() / 60;
		return date.getFullYear()
		+ "-" + pad(date.getMonth() + 1,2)
		+ "-" + pad(date.getDate(),2)
		+ " " + pad(date.getHours(),2)
		+ ":" + pad(date.getMinutes(),2)
		+ ":" + pad(date.getSeconds(),2)
		+ (addUTC?" UTC" + (offset >= 0 ? "+":"-") + offset:"");
	}
}

/**
 * List management
 */

function list_import(){
	var new_lists = JSON.parse($("#list_import").val());
	if (typeof new_lists != "undefined"){
		$.extend(local_lists,new_lists);
		localStorage["msl-raws-lists"]=JSON.stringify(local_lists);
		renderLists();
		$tabs.tabs('select',1);
	} else {
		alert("Sorry, can't parse lists, try again!");
	}	
}
function createList(){
	if ($(".listCreate .listLabel").val() != ""){
		conf.current_list = {
			uuid: new UUID(),
			creator: conf.personalId,
			created: new Date().getTime(),
			name: $(".listCreate .listLabel").val(),
			description: $(".listCreate .listDescr").val(),
			url: $(".listCreate .listURL").val(),
			uuids: selectedImages()
		};
		conf.localList = true;
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
	var list = local_lists[listId];
	list.shared = new Date().getTime();
	local_lists[listId]=list;
	localStorage["msl-raws-lists"]=JSON.stringify(local_lists);
	renderLists();
	$.ajax({
		url:"/lists/id/"+listId,
		type:"PUT",
		processdata:false,
		data:JSON.stringify(list),
		error : function() {
			$(".error")
				.html("I'm sorry, but something went wrong while trying to reach the server, please try again."+errorCloseButton);
		}
	});
}
function subscribeToList(listId){
	if ($.inArray(listId,subscriptions)< 0) subscriptions.push(listId);
	localStorage["msl-raws-subscriptions"]=JSON.stringify(subscriptions);
	updateSubscriptions();
}
function unsubscribeList(listId){
	subscriptions.splice($.inArray(listId,subscriptions),1);
	localStorage["msl-raws-subscriptions"]=JSON.stringify(subscriptions);
	updateSubscriptions();
}
function updateSubscriptions(){
	$.ajax({
		url:"/lists",
		type:"POST",
		data:{"listIds":subscriptions.join()},
		statusCode:{
			200: function(json){
				subscribed_lists= json;
				renderSubscriptions();
			}
		}
	});
}
function addToList(){
	var images = selectedImages();
	var list = conf.current_list.uuids;
	images.map(function (uuid){
		if ($.inArray(uuid,list)<0){
			list.push(uuid);
		}
	});
	conf.current_list.uuids = list;
	local_lists[conf.current_list.uuid.id]=(conf.current_list);
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
	conf.current_list.uuids = list;
	local_lists[conf.current_list.uuid.id]=(conf.current_list);
	localStorage["msl-raws-lists"]=JSON.stringify(local_lists);
	render();
	selectList();
}

function openList(listId){
	if (typeof local_lists[listId] != "undefined"){
		conf.current_list=local_lists[listId];
		conf.localList = true;
	} else {
		conf.current_list=subscribed_lists[listId];
		conf.localList = false;
	}
	conf.filter_list="complete";
	$tabs.tabs('select',0);
}
function deselectCurrent(){
	conf.current_list={};
	conf.localList= true;
	render();
}

function renderLists(){
	//TODO: sort on last Modified, last Shared, etc.
	$('.error').html("");
	$('.listBrowser-target')
	.replaceWith(
			$('.listBrowser-template')
					.clone()
					.directives({
						'.line-template' : {
							'listItem<-context' : {
								/*".listPublish@onClick":function(a){
									return "publishList('"+a.item.uuid.id+"')";
								},
								".listPublishLink@href":function(a){
									return "http://msl-raw-images.appspot.com/?subscribe="+a.item.uuid.id;
								},
								".listPublishLink@style":function(a){
									if (typeof a.item.shared == "undefined" || a.item.shared == ""){
										return "display:none";
									}
									return "";
								},
								".listLastShare" : function (a){
									millis = parseFloat(a.item.shared);
									date = new Date(millis);
									return renderDate(date,conf.show_utc);
								},*/
								".listCreated" : function (a){
									millis = parseFloat(a.item.created);
									date = new Date(millis);
									return renderDate(date,conf.show_utc);
								},
								".listLabel@onClick":function(a){
									return "openList('"+a.item.uuid.id+"')";
								},
								".listLabel":"listItem.name",
								".listCount":"listItem.uuids.length",
								".listDescr pre":"listItem.description",
								".listUrl@href":"listItem.url",
								".listUrl":function(a){ return a.item.url==""?"":"Weblink"; }
							}
						}
					}).render(local_lists).removeClass('listBrowser-template')
					.addClass('listBrowser-target'));
	$(".listBrowser-target .line-filler").toggle($.isEmptyObject(local_lists));
}
function renderSubscriptions(){	
	//TODO: sort on last Modified, last Shared, etc.
	$('.error').html("");
	$('.subscriptionBrowser-target')
	.replaceWith(
			$('.subscriptionBrowser-template')
					.clone()
					.directives({
						'.line-template' : {
							'listItem<-context' : {
								".unsubscribe@onClick":function(a){
									return "unsubscribeList('"+a.item.uuid.id+"')";
								},
								".listPublishLink@href":function(a){
									return "http://msl-raw-images.appspot.com/?subscribe="+a.item.uuid.id;
								},
								".listPublishLink@style":function(a){
									if (typeof a.item.shared == "undefined" || a.item.shared == ""){
										return "display:none";
									}
									return "";
								},
								".listLastShare" : function (a){
									millis = parseFloat(a.item.shared);
									date = new Date(millis);
									return renderDate(date,conf.show_utc);
								},
								".listLabel@onClick":function(a){
									return "openList('"+a.item.uuid.id+"')";
								},
								".listLabel":"listItem.name",
								".listCount":"listItem.uuids.length",
								".listDescr pre":"listItem.description",
								".listUrl@href":"listItem.url",
								".listUrl":function(a){ return a.item.url==""?"":"link";}
							}
						}
					}).render(subscribed_lists).removeClass('subscriptionBrowser-template')
					.addClass('subscriptionBrowser-target'));
	$(".subscriptionBrowser-target .line-filler").toggle($.isEmptyObject(subscribed_lists));
}
/*
 * Image listing
 * 
 * TODO: non filtering, but modify image to be visible or not.
 */
function filter(a) {
	a.item.visible=true;
	if (parseInt(a.item.sol) > dyn.highest_sol)
		dyn.highest_sol = parseInt(a.item.sol);
	var skip = typeof conf.current_list == "undefined" || typeof conf.current_list.name == "undefined";
	if (!skip && conf.filter_list == "include"
			&& $.inArray(a.item.name,conf.current_list.uuids) < 0) return false;
	if (!skip && conf.filter_list == "exclude"
			&& $.inArray(a.item.name,conf.current_list.uuids) >= 0) return false;
	if (!skip && conf.filter_list == "complete"){
		if ($.inArray(a.item.name,conf.current_list.uuids) < 0){
			return false;
		}
	} else {
		if (conf.filter_new
				&& parseFloat((a.item.lastModified == ""||a.item.lastModified == "---") ? a.item.unixTimeStamp
						: a.item.lastModified) < dyn.newLimit)
			return false;
		if (conf.filter_sol >= 0 && a.item.sol != conf.filter_sol)
			return false;
		//console.log(a.item);
		if (conf.filter_type.length > 0
				&& $.inArray(a.item.type.substring(0,1).toUpperCase(), conf.filter_type) == -1)
			return false;		
		if (conf.filter_cam.length > 0
				&& $.inArray(a.item.camera, conf.filter_cam) == -1)
			return false;		
	}
	dyn.totalImages++;
	dyn.fullList.push(a.item);
	if (!dyn.temp_render_max && dyn.pagecount > conf.render_max)
		return false;
	if (dyn.pagecount > conf.show_count)
		return false;
	dyn.pagecount++;
	return true;
}

//TODO: Don't add to render tree, but add placeholder empty rows for gaps.
function visibilityFilter(a) {
	a.item.visible=true;
	if (parseInt(a.item.sol) > dyn.highest_sol)
		dyn.highest_sol = parseInt(a.item.sol);
	var skip = typeof conf.current_list == "undefined" || typeof conf.current_list.name == "undefined";
	if (!skip && conf.filter_list == "include"
			&& $.inArray(a.item.name,conf.current_list.uuids) < 0) { 
			a.item.visible=false; 
			return false;
		}
	if (!skip && conf.filter_list == "exclude"
			&& $.inArray(a.item.name,conf.current_list.uuids) >= 0){ 
			a.item.visible=false; 
			return false;
		}
	if (!skip && conf.filter_list == "complete"){
		if ($.inArray(a.item.name,conf.current_list.uuids) < 0){ 
			a.item.visible=false;
			return false;
		}
	} else {
		if (conf.filter_new
				&& parseFloat((a.item.lastModified == ""||a.item.lastModified == "---") ? a.item.unixTimeStamp
						: a.item.lastModified) < dyn.newLimit){ a.item.visible=false; return false;}
		if (conf.filter_sol >= 0 && a.item.sol != conf.filter_sol){ a.item.visible=false; return false;}
		//console.log(a.item);
		if (conf.filter_type.length > 0
				&& $.inArray(a.item.type.substring(0,1).toUpperCase(), conf.filter_type) == -1){a.item.visible=false; return false;}
			
		if (conf.filter_cam.length > 0
				&& $.inArray(a.item.camera, conf.filter_cam) == -1){
			a.item.visible=false;
			return false;
		}
	}
	dyn.totalImages++;
	dyn.fullList.push(a.item);
	if (!dyn.temp_render_max && dyn.pagecount > conf.render_max){
		a.item.visible=false; 
		return true;
	}
	if (dyn.pagecount > conf.show_count){
		a.item.visible=false; 
		return true;	
	}
	dyn.pagecount++;
	return true;
}

function sort(a, b) {
	var ca=0;
	var cb=0;
	if (conf.sort_column == "lmst"){
		la = a["lmst"].match(/\d/g);
		lb = b["lmst"].match(/\d/g);
		if (typeof la == "undefined" || la == null || typeof lb == "undefined" || lb == null) {
			ca = parseFloat(a.unixTimeStamp);
			cb = parseFloat(b.unixTimeStamp);
		} else {
			ca= parseFloat(la.join(""));
			cb= parseFloat(lb.join(""));	
		}
		if (ca==cb){
			ca = parseFloat(a.unixTimeStamp);
			cb = parseFloat(b.unixTimeStamp);
		}
	} else if (conf.sort_column != "name"){
		ca= parseFloat(a[conf.sort_column]);
		cb= parseFloat(b[conf.sort_column]);	
		if (ca==cb){
			ca = parseFloat(a.unixTimeStamp);
			cb = parseFloat(b.unixTimeStamp);
		}
	}
	if (ca==cb){
		ca = a.name;
		cb = b.name;
	}
	var result = (ca <= cb ? 1	: -1);
	if (conf.sort_reverse) result*=-1;
	return result;
}
function saveConf() {
	localStorage["msl-raws-conf"] = JSON.stringify(conf);
}

var odd=true;
var normal_directives={
		'.line-template' : {
			'image<-context' : {
				'@class+' : function(a) {
					odd=!odd;
					var result=odd?" odd":" even";
					return result + (!a.item.visible?" hidden":"");
				},
				'.name' : 'image.name',
				'.name@href' : function(a) {
					return a.item.url[1]=="$"?(a.item.url[0]=="J"?jpl:msss)+a.item.url.substring(2):a.item.url;
				},
				'input.selector@value' : 'image.name',
				'input.selector@id' : 'image.name',
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
					return "";
				},
				'.taken' : function(a) {
					var millis = parseFloat(a.item.unixTimeStamp);
					var date = new Date(millis);
					return renderDate(date,conf.show_utc,false);
				},
				'.lmst' : 'image.lmst', 
				'.orientation': function(a){
					return conf.show_orientation?a.item.bearing:"";
				},
				'.released' : function(a){
					var millis = parseFloat(a.item.lastModified);
					var date = new Date(millis);
					var lag = Math.round((parseFloat(a.item.lastModified) - parseFloat(a.item.unixTimeStamp))/3600000);
					return renderDate(date,conf.show_utc,false)+((conf.show_lag && !isNaN(lag))?" (+"+lag+"h)":"");
				}
			},
			sort : function(a, b) {
				return sort(a, b);
			},
			filter : function(a) {
				return filter(a);
			}
		}
	};

var mosaic_directives = {
		".image" : {
			"image<-context" : {
				"@id":"image.name",
				"img@src":function(a){
					return (a.item.thumbnailUrl[1]=="$"?(a.item.thumbnailUrl[0]=="J"?jpl:msss)+a.item.thumbnailUrl.substring(2):a.item.thumbnailUrl);
				},
				".label" : function(a){
					return (a.item.sol + "-" + a.item.lmst + "<br>" + a.item.camera + " ("+a.item.type+")");
				}
			},
			sort : function(a, b) {
				return sort(a, b);
			},
			filter : function(a) {
				return filter(a);
			}
		}
};

var map = {"none":"None","include":"Include","exclude":"Exclude","complete":"Full"};
var normal_rfn = null;
var mosaic_rfn = null;
function render() {
	var selected = selectedImages();
	if (normal_rfn == null){
		normal_rfn = $('.tab-template').compile(normal_directives);
		mosaic_rfn = $('.mosaic-template').compile(mosaic_directives);
	}
	dyn.newLimit = new Date().getTime() - (24 * 3600 * 1000);
	localStorage["msl-raws-conf"] = JSON.stringify(conf);
	$('.error').html("");
	dyn.pagecount = 0;
	dyn.totalImages = 0;
	dyn.fullList=[];
	
	odd=true;
	$('.tab-target tbody')
			.replaceWith(
					$(conf.display_mode=="normal"?'.tab-template':'.mosaic-template')
						.clone()
						.render(dyn.data,conf.display_mode=="normal"?normal_rfn:mosaic_rfn)
						.removeClass("tab-template mosaic-template")
			);
	
	if (conf.display_mode=="mosaic"){
		$('.tab-target .image').unbind("click")
			.on("click",function(){
				$(this).toggleClass("selected");
				toggleSelector();
			});
		$('.tab-target .image_container').css('max-width',($(window).width()-100)+'px');
	}
	$(".labels").toggle(conf.display_mode!="mosaic");
	$(".filter .thumbnail").toggle(conf.display_mode!="mosaic");
	
	// Reselect selected images (if still visible):
	selected.map(function(image_name){
		if (conf.display_mode=="normal"){
			$('#'+image_name).attr("checked","checked");
		}
		if (conf.display_mode=="mosaic"){
			$('#'+image_name).toggleClass("selected",true);
		}
	});
	
	// Revert input fields to conf values:
	if (conf.filter_sol < 0) {
		$(".solInput").val(dyn.highest_sol);
	} else {
		$(".solInput").val(conf.filter_sol);
	}

	if (conf.show_thumbs) $('.thumbBox').attr("checked", "checked");
	if (conf.filter_sol < 0) $('.solBox').attr("checked", "checked");
	if (conf.filter_new) $('.newBox').attr("checked", "checked");
	if (conf.show_lag) $('.lagBox').attr("checked", "checked");
	if (conf.show_orientation) $('.bearingBox').attr("checked", "checked");
	
	$(".tab-target .typeSelectMulti").val(conf.filter_type);
	$(".tab-target .typeSelectMulti").chosen();
	
	$(".tab-target .camSelectMulti").val(conf.filter_cam);
	$(".tab-target .camSelectMulti").chosen();
	$(".tab-target input.selector").enableCheckboxRangeSelection();
	$(".tab-target input.selector").on('click',toggleSelector);
	$(".tab-target #filter_list_"+conf.filter_list).attr("checked","checked");
	$(".tab-target input:radio[name=filter_list]").unbind("click").on("click",function(){
		conf.filter_list=$(this).val();
		$(".listViewer").hide();
		render();
	});
	$(".tab-target .listSelectFeedback").html(map[conf.filter_list]);
	toggleSelector();

	//Sorting stuff
	$(".tab-target .sortable .sortIndicator").remove();
	$(".tab-target .sortable").unbind("click").on("click",function(){
		if (conf.sort_column == this.id){
			conf.sort_reverse = !conf.sort_reverse;
		} else {
			conf.sort_reverse=false;
		}
		conf.sort_column = this.id;
		console.log(conf.sort_column,conf.sort_reverse);
		render();
		return false;
	});
	$(".tab-target #"+conf.sort_column).append("<span class='sortIndicator inline ui-icon "+(conf.sort_reverse?"ui-icon-carat-1-n":" ui-icon-carat-1-s")+"'></span>");
	
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
	$(".tab-target .imageCount").html("showing:"+(Math.min(conf.show_count,dyn.totalImages))+
			" of "+dyn.totalImages+ " filtered images (total:" + dyn.data.length + ")");
	$(".tab-target .currentList,.listDelete .currentList").html(typeof conf.current_list.name != "undefined"?conf.current_list.name:"-none-");
	$(".tab-target .listCount").html(typeof conf.current_list.name != "undefined"?conf.current_list.uuids.length:"");
	$('.tab-target .labels .newest').html(" ->" + dyn.highest_sol);
	
	$(".listInfo").html(($(".listInfo").clone().render(conf.current_list,{
		".listLabel" : "name",
		".listCount" : function(a){ return typeof conf.current_list.name != "undefined"?conf.current_list.uuids.length+" images":""; },
		".listDescr pre" : "description",
		".listURL" : function(a){ return conf.current_list.url==""?"":"<a href='"+conf.current_list.url+"' target='_blank'>"+conf.current_list.url+"</a>";}
	})).html());
	$('th.listFilter').css("visibility",typeof conf.current_list.name != "undefined"?"visible":"hidden");
	$('input.render_max').val(conf.render_max);
	$('input.max_show').val(conf.max_show);
	$('.timezone').html(render_timezone());
	if (dyn.pagecount >= conf.render_max) {
		$(".error")
		.html("Sorry, cowardly refusing to render more than "+conf.render_max+" images in one page!"+errorCloseButton+"<br><a href='#' onClick='dyn.temp_render_max=true;render();'>Temporary lift limit, really sure?</a>");
	}
	$(".tab-target input:button").button('refresh');

	$(".tab-target .ui-icon").removeClass('ui-state-disabled');
	//List management buttons
	if (typeof conf.current_list.name == "undefined"){
		$(".tab-target .msl-deSelectList .ui-icon").addClass('ui-state-disabled');
	}
	if (typeof conf.current_list.name == "undefined" || !conf.localList){
		$(".tab-target .msl-dropList .ui-icon").addClass('ui-state-disabled');
		$(".tab-target .msl-addToList .ui-icon").addClass('ui-state-disabled');
		$(".tab-target .msl-delFromList .ui-icon").addClass('ui-state-disabled');
		$(".tab-target .msl-selectImages .ui-icon").addClass('ui-state-disabled');
		$(".tab-target .msl-deSelectImages .ui-icon").addClass('ui-state-disabled');
		$(".tab-target .msl-metaInfo .ui-icon").addClass('ui-state-disabled');			
	}
	
	// Although not necessary on all redraws, no problem:
	$("style#dynCss").html("* { font-size:"+conf.font_size+"pt }");
	$(".utcBox").attr('checked',conf.show_utc);
	$(".font_size").val(conf.font_size);
	$(".prefixText").val(conf.wget_syntax);
	$(".prefixBox").attr('checked', conf.useWget);
	// Done
}
function disabled(obj){
	return !obj.find('.ui-icon').hasClass('ui-state-disabled');
}
function error(text){
	$(".error").html(text+errorCloseButton);
}

function selectList(){
	if (typeof conf.current_list.uuids == "undefined") return;
	$(".tab-target input.selector").each(function(){
		var $this = $(this);
		$.inArray($this.attr('value'),conf.current_list.uuids)>=0?$this.attr("checked","checked"):$this.removeAttr("checked");
	});
	toggleSelector();
}
function deselectList(){
	if (typeof conf.current_list.uuids == "undefined") return;
	$(".tab-target input.selector").each(function(){
		var $this = $(this);
		$.inArray($this.attr('value'),conf.current_list.uuids)>=0?$this.removeAttr("checked"):$this.attr("checked","checked");
	});
	toggleSelector();
}
function toggleSelector(){
	if (selectedImages().length > 0){
		$(".toggleSelector").attr("checked","checked");
		$(".tab-target .msl-export .ui-icon").removeClass('ui-state-disabled');
		$(".tab-target .msl-createList .ui-icon").removeClass('ui-state-disabled');
		if (typeof conf.current_list.name != "undefined" && conf.localList){
			$(".tab-target .msl-addToList .ui-icon").removeClass('ui-state-disabled');
			$(".tab-target .msl-delFromList .ui-icon").removeClass('ui-state-disabled');
		}
	}
	if (selectedImages().length <= 0){
		$(".toggleSelector").removeAttr("checked");
		$(".tab-target .msl-export .ui-icon").addClass('ui-state-disabled');
		$(".tab-target .msl-createList .ui-icon").addClass('ui-state-disabled');
		$(".tab-target .msl-addToList .ui-icon").addClass('ui-state-disabled');
		$(".tab-target .msl-delFromList .ui-icon").addClass('ui-state-disabled');
	}
	
}
function toggleAll(){
	if ($('.toggleSelector').attr("checked") == "checked"){
		$(".tab-target input.selector").attr({ checked:"checked"});
	} else {
		$(".tab-target input.selector").removeAttr("checked");	
	}
	toggleSelector();
}
function selectedImages() {
	var selected = new Array();
	$('.tab-target input.selector:checked').each(function() {
		selected.push($(this).attr('value'));
	});
	$('.tab-target .selected').each(function(){
		selected.push($(this).attr('id'));
	});
	return selected;
}
function selectedImagesList() {
	var selected = selectedImages();
	if (selected.length == 0)
		return {};
	var result = {};
	dyn.data.map(function(image) {
		if ($.inArray(image.name, selected) >= 0) {
			result[image.name] = image;
		}
	});
	return result;
}
function selectedImagesArray(){
	var selected = selectedImages();
	var result = [];
	if (selected.length == 0) return result;
	dyn.data.map(function(image){
		if ($.inArray(image.name, selected) >= 0) {
			result.push(image);
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
	selectedImagesArray().map(function(image){
		var url = image.url;
		newPage += (url[1]=="$"?(url[0]=="J"?jpl:msss):"")+url.substring(2) + (html ? "<br>" : "\n");
	});
	if (conf.useWget)
		newPage += "ENDOFURLS" + (html ? "<br>" : "\n");
	var p = window.open('');
	p.document.open("text/plain");
	p.document.write(newPage);
	p.document.close();
}

function updateProgress(){
	$("div.reload").progressbar({value: dyn.data.length });
}
function load_sol(sol,ifmodified){
	$.ajax({
		//url : "http://msl-raw-images.storage.googleapis.com/sol_"+sol+".json",
		url : "json?sol="+sol,
		ifModified:ifmodified,
		cache:true,
		dataType:"json",
		success: function (result, textStatus, jqXHR) {
	        // if 304, re-request the data
	        if (typeof result == "undefined" && textStatus == 'notmodified') {
	           load_sol(sol,false);
	        } else {
	        	if (result == null || typeof result == "undefined"){
	        		return;
	        	}
	        	if (typeof result == "string"){
	        		result = JSON.parse(result);
				}
				if (typeof result.responseText == "string"){
					result = JSON.parse(result.responseText);
				}
				dyn.data = dyn.data.concat(result);
				updateProgress();
	        }
		}
	});
}
function reload() {
	$(".error").html("");
	if (typeof _gaq != "undefined"){
		_gaq.push(['_trackEvent', "imageList", "Reload"]);
	}
	$.ajax({
		//url : "http://msl-raw-images.storage.googleapis.com/MaxSol",
		url : "json?sol=-1",
		dataType:"json",
		cache:true,
		ifModified:false,
		statusCode : {
			200 : function(json){
				if (typeof json == "string"){
					json = JSON.parse(json);
				}
				if (typeof json.responseText == "string"){
					json = JSON.parse(json.responseText);
				}
				dyn.data=[];
				if (typeof json.count != "undefined"){
					dyn.totalImages=json.count;
					$("div.reload").progressbar({value:0,max:dyn.totalImages});
				}
				for (var i=0; i<json.sol+1; i++){
					load_sol(i,true);
				}
			}
		},
		complete:function(res){
			if (res.readyState == 0 && res.status==0){
				console.log("CORS error?, falling back to landing page!");
				$.ajax({
					url : "/landing",
					dataType:"json",
					statusCode : {
						200 : function(json) {
							if (typeof json == "string"){
								json = JSON.parse(json);
							}
							if (typeof json.responseText == "string"){
								json = JSON.parse(json.responseText);
							}
							dyn.data = json;
						}
					},
					error : function() {
						$(".error").html(
							"I'm sorry, but something went wrong while trying to reach the server, please try again."+errorCloseButton);
					}
				});
			}
		}
	});
	updateSubscriptions();
}

function openGallery(){
	var images = [];
	var list = selectedImagesArray();
	if (list.length == 0) list = dyn.fullList;
	list.map(function (image){
		var url=image.url;
		url = (url[1]=="$"?(url[0]=="J"?jpl:msss)+url.substring(2):url);
		var text="<a href='"+url+"' target='_blank'>Full Resolution</a><br>Filename: "+image.name
				+"<br>Taken on: "+renderDate(new Date(parseFloat(image.unixTimeStamp)),conf.show_utc)
				+"(LMST:"+image.lmst+")";
		images.push([url,text]);
	});
	if (images.length <= 0)return;
	jQuery.slimbox(images, 0, {
		loop:true, 
		initialWidth: $(window).height()-150, 
		initialHeight:$(window).height()-150,
		captionAnimationDuration:1,
		imageFadeDuration:1
	});
}
function openNormal(){
	conf.display_mode='normal';render();
}
function openGrid(){
	conf.display_mode='mosaic';render();
//	var container = $("<div class='gridContainer'>");
//	dyn.fullList.slice(0,conf.render_max).map(function (image){
//		var url=image.url;
//		var thumb=image.thumbnailUrl;
//		url = (url[1]=="$"?(url[0]=="J"?jpl:msss):"")+url.substring(2);
//		thumb = (thumb[1]=="$"?(thumb[0]=="J"?jpl:msss):"")+thumb.substring(2);
//		container.append("<div><a target='_blank' href="+url+"><img src='"+thumb+"' title='"+image.name+"'/></a></div>");
//	});
//	container.dialog({
//		autoOpen : true,
//		title : "GridView",
//		modal : true,
//		width : "90%"		
//	});
}

	
$(document).ready(function() {
	var reloadString = "<a href='#' class='reload cleanLink' onClick='reload(true)'><span class='ui-icon ui-icon-refresh inline'/>Reload</a>";
	$("div.reload").html(reloadString);
	$("div.reload").ajaxStart(function() {
		$(this).html("<span class='reloading'>Loading...</span>");})
	.ajaxStop(function() {
		$(this).progressbar("destroy");
		$(this).html(reloadString);
		render();
	});
	$("input:button").button();

	if (typeof $.getUrlVar("list_import") == "string"){
		$(".list_import").show();
	} 
	var listId = $.getUrlVar("list");
	if (typeof listId != "undefined" && listId != "" && typeof local_lists[listId] != "undefined"){
		conf.current_list=local_lists[listId];
		conf.localList = true;
		conf.filter_list="complete";
	}
	
	var subscriptionId = $.getUrlVar("subscribe");
	if (typeof subscriptionId != "undefined" && subscriptionId != ""){
		subscribeToList(subscriptionId);
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
	
//	$(".tweetRSS").rssfeed('http://search.twitter.com/search.atom?q=from:MslRawImages&rpp=1',{
//		limit:1,
//		header:false,
//		snippet:false,
//		media:false,
//		content:false
//	});
	render();
	reload();
	updateSubscriptions();
	
	$(window).scroll(function() {
		   if($(window).scrollTop() + $(window).height() > $(document).height() - 100) {
		       console.log("adding more images");
//		       conf.show_count+=conf.max_show;
//		       render();
		   }
	});
	
	
	!function(d,s,id){
		var js,fjs=d.getElementsByTagName(s)[0];
		if(!d.getElementById(id)){
			js=d.createElement(s);
			js.id=id;
			js.src="//platform.twitter.com/widgets.js";
			fjs.parentNode.insertBefore(js,fjs);
		}}(document,"script","twitter-wjs")
	;
	if (typeof stLight != "undefined") stLight.options({
			publisher : "d1b23a5c-25a8-40b8-aa83-a7d19b05523d"
	});
	if (stButtons){stButtons.locateElements();}
});
