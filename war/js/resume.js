var education = [{
		start:"1996-09-01",
		end:"1998-09-01",
		title:"Bachelor of Engineering",
		subject:"Mechanical Engineering",
		company:"Delft University of Technology",
		graded:false,
		description:"Although I failed on the more abstract mathematics, I consider these two years as a solid introduction into the engineering field. I was most effective on the dynamic systems subjects, especially if some form of software was involved.",
		tags:["Technical Drawings","Mechatronics","Pascal","Linear Algebra","Calculus","Systems","Dynamics","Mechanical Production Process"]
	},
	{
		start:"1998-09-01",
		end:"2002-02-01",
		title:"Bachelor of Engineering",
		subject:"Aerospace Technology",
		company:"Hogeschool Haarlem",
		graded:true,
		description:"Through this schooling I've become a systems engineer with a high affinity with software.",
		tags:["Mechanical Engineering","Control Systems","Matlab","CAD (Catia V5)","Assembler","Aerodynamics","Space Systems","Simulation","Mechanics of Materials","Fluid Dynamics","Heat Transfer"]
	},
	{
		start:"2007-09-01",
		end:"2010-02-01",
		title:"Master of Science",
		subject:"Software Engineering",
		company:"University of Liverpool",
		graded:true,
		description:"Distance learning study: http://www.uol.ohecampus.com/home/index.phtml",
		tags:["Object Oriented Analysis and Design","Planning","Software Lifecycle","Java","UML"]
	}
];
var experience = [
	{
		type:"Traineeship",
		title:"Trainee",
		start:"1999-09-01",
		end:"1999-12-01",
		company:"Elektriciteitsbedrijf Zuid-Holland (now part of EON)",
		summary:"Experiencing physical maintenance and working with procedures",
		description:"Trainee at the maintenance department of an electrical power plant. This traineeship contained physical maintenance of gasturbine engines and the creation of a calibration manual for measurement equipment on gasturbines.",
		tags:["Gasturbines","Physical Maintenance","Calibration","Documentation","Procedures"]
	},
	{
		type:"Traineeship",
		title:"Trainee",
		start:"2000-02-01",
		end:"2000-09-01",
		company:"Transavia Airlines",
		summary:"Maintenance program streamlining",
		description:"Trainee at Transavia Airlines Technical Service, engineering department. The trainee assignment consisted of supporting the subgroup, which was responsible for the creating and organization of the maintenance program of the airfleet. Through this work I obtained experience and knowledge about the structure, targets and mission of maintenance. Conducted a survey into possible efficiency enhancements in the maintenance program. "+
				"Besides the regular job I also supported the implementation of an internal ICT project, using ZyLab software, which was aimed at the digitalization of the current paperwork and the scanning, storing and distribution of the workorders. " +
				"During this traineeship Transavia encountered a severe attack of a MSWord macro virus, I created a cleanup script to help remove the virus. " +
				"After the trainee job I have worked at the same department at a short project. One of the aircraft had to be reclaimed by the lease company. To support this return, the current state of maintenance of the airplane had to be checked and documented. My job consisted of the mayor part of this survey work.",
		tags:["Maintenance Program","Optimization","Safety","Documentation","Work Orders","Digitalization","Virus Removal","Visual Basic"]
	},
	{
		type:"Traineeship",
		title:"Trainee",
		start:"2001-08-01",
		end:"2002-02-01",
		company:"Fokker Services (Stork)",
		summary:"Mathematical analysis of the prediction methods based on windtunnel measurements",
		description:"The concluding assignment of my aerospace program consisted of making a comparison between the predicted aerodynamic flight-characteristics of the Fokker 100 with the actual measured certification data. This comparison was supported by a theoretical and mathematical study of the prediction methods used by Fokker.",
		tags:["Prediction Methods","Numerical Approximation","Aerodynamics"]
	},
	{
		type:"Full Time",
		title:"Software Developer",
		start:"2002-04-01",
		end:"2003-02-01",
		company:"Jonkers Automation",
		summary:"All-in-one firewall appliance development",
		description:"Construction and implementation of the Iconnect, a firewall product, complex router and mail-filter. My job consisted mainly of software programming (C and Perl) and system integration. Third line support for Iconnects in the field.",
		tags:["Linux","Remote System Management","Webapplication Development","Perl","HTML","Email","Virus Protaction","Spam Filtering"]
	},
	{
		type:"Owner",
		title:"Founder, CTO",
		start:"2003-02-01",
		end:"2005-07-01",
		company:"V & S",
		summary:"Co-owning a firewall appliance development company",
		description:"Design and implementation of the Protactive firewall, a Linux-based complete firewall solution, which stood out because of the (then-inventive) web-based user interface. Shared copyright holder of the firewall's software, founder and co-owner of this (small) company. Most of the technical aspects of a small scale ICT-company were my responsibility: development, support, pre sales, training, etc. "+ 
				    "Work on this firewall product included working with and understanding of the network subsystem of the Linux kernel.",
		tags:["Business Administration","Software Life Cycle","Customer Support","Kernel Development","Ocaml","HTML","JavaScript","Email","Virus Protaction","Spam Filtering","Design","Development","User Interface"]
	},
	{
		type:"Full Time",
		title:"System Architect",
		start:"2005-07-01",
		end:"2006-03-01",
		company:"Basewall",
		summary:"Failed try on international market",
		description:"Based on our self-developed Protactive firewall, BaseWall offered a large, international commercial network. Unfortunately BaseWall didn't prove a solid enough, visionary company, it's shareholders weren't able to sufficiently protect themselves against a former business partner in the USA. A lost legal battle rendered BaseWall incapable of financially upholding their company.",
		tags:["Merger Negotiations","Contract Negotiations", "Business Administration"]
	},
	{
		type:"Full Time",
		title:"Technical Consultant",
		start:"2006-03-01",
		end:"2007-10-31",
		company:"Kahuna Business Solutions",
		summary:"Large, governmental customers experience",
		description:"Consultancy services for the deployment and maintenance of Kana Interactive Communication Suite. This software suite is built on EJB. The job included programming, setup, support and documentation. The suite is deployed at various large customers, both governmental and commercial. My job provided insight into various aspects of large scale software solutions and organizations, both technological as on a management scale. In-depth experience with Oracle and J2EE. Most noteworthy event during this job was the debugging and repairing a highly visible, political important project, which was broken due to scalability issues on the database design level.",
		tags:["Govermental Customers","Java","JBoss","Oracle","Project Management","Debugging","Formal Documentation","Support","Maintenance"]
	},
	{
		type:"Full Time",
		title:"Senior Software/Product developer",
		start:"2007-12-01",
		end:"2009-02-01",
		company:"ASK Community Systems",
		summary:"Agent based software in intelligent tele-communication systems",
		description:"Through ASK I work on a very innovative communication platform. This platform aims to dynamically, adaptively connect large groups of contacts with each other, primarily through telephone and SMS."+
					"ASK is a Rotterdam-based company working on a spin-off product of the research into 'self-organization' and 'hybrid networks' conducted at Almende. The product is based on a network of software agents, cooperating to provide a scalable, high-performance coordination system for communication transactions. At ASK I'm responsible for keeping the product close to it's architectural design, designing changes to adhere to this design and to work with Almende on the future model.",
		tags:["Artificial Intelligence","Agent-based Software Design","Feedback Systems","C","PHP","MySQL","Subversion"]
	},
	{
		type:"Full Time",
		title:"CTO",
		start:"2009-02-01",
		end:"2012-02-01",
		company:"ASK Community Systems",
		summary:"CTO at ASK",
		description:"Taking responsibility for all technical aspects of the ASK platform, including management of the development team, architecture adherence, quality assurance and maintenance & technical support.",
		tags:["Management","Team Lead","Software Architecture","Sales Support","HTML5","JQuery","JQuery-Mobile","Android","Cordova/PhoneGap","Google App Engine"]
	},
	{
		type:"Full Time",
		title:"Senior Software Engineer",
		start:"2012-02-01",
		end:null,
		company:"Almende",
		summary:"Long-term toolset development, research support",
		description:"Working on parts of CHAP, the common Hybrid Agent Platform. Creating tools for making software agents, using various Artificial Intelligence techniques. Through Almende, I'm working on building a replacement toolset for ASK (and other CHAP based products). Intermitted supporting robotics department at Almende, with part design for 3d printing.",
		tags:["Google App Engine","Java","NoSQL Databases","CAD (OpenSCAD)","3d Printing","Robotics"]
	}
];
var general_info = { 
		"name":"Ludo T. Stellingwerff",
		"gender":"Male",
		"date_of_birth":"16 December 1977",
		"place_of_birth":"Amsterdam, The Netherlands",
		"nationality":"Dutch",
		"marital_status":"Married",
		"p_degree":"Master of Science, Software Engineering",
		"s_degree":"Bachelor of Engineering, Aerospace Technology",
		"address":"Rietkraag 5, 2771KX, Boskoop, The Netherlands",
		"email":"ludo@stwerff.xs4all.nl",
		"mobile":"+31624495602",
		"summary":"Experienced software engineer, translating a systems thinking approach to analysis, design, software lifecycle and debugging. I've worked in many fields: computer networks and security, enterprise level database systems, VoIP communication and artificial intelligence systems. Common denominators between the jobs: web applications, semi-realtime interaction, legacy system integration, innovative tooling and a constant drive to learn and understand.",
		"objective":"Creating meaningfull software for real users, but at the cutting edge of technology. I work best in a high-risk, challenging environment working towards a common ambitious goal. Bringing experience from a wide range of computer fields together, I strive to solve any software problem in any software infrastructure, environment and/or software language.",
};
var complex_items={
		"languages":[{"lang":"Dutch","level":"Native proficiency"},{"lang":"English","level":"Full professional proficiency"},{"lang":"German","level":"Limited working proficiency"},{"lang":"Hungarian","level":"Limited working proficiency"}],
		"interests":["Reading","philosophical debate","Linux","space technology (MSL Curiosity, Mars rovers, Commercial Spaceflight, Cassini/Huygens, ISS, etc.)","the free software debate","general politics"]
}
var pdate = function(dateStr){
	if (typeof dateStr == "undefined" || dateStr == null) return new Date();
	var parts = dateStr.split("-");
	return new Date(parts[0],parts[1]-1,parts[2]);
}
var sortfun = function(a,b){
//	if (typeof a.end == "undefined" || a.end == null) return 1;
//	if (typeof b.end == "undefined" || b.end == null) return -1;
	if (pdate(a.end) == pdate(b.end)) return (pdate(a.start)>pdate(b.start)?1:-1);
	return (pdate(a.end)<=pdate(b.end)?1:-1);
}

$(document).ready(function() {
	var directive = {
			".multiple_entries":{
				"entry<-":{
//					".type":function(a){
//						if (typeof a.entry.item.type != "undefined"){
//							return "("+a.entry.item.type+") ";	
//						}
//						return "";
//					},
					".subject":function(a){
						if (typeof a.entry.item.subject != "undefined"){
							return " - "+ a.entry.item.subject;
						} 
						return "";
					},
					".jobtitle":function(a){
						if (typeof a.entry.item.graded != "undefined"){
							if (!a.entry.item.graded){
								return "(Unfinished) "+a.entry.item.title;
							}
						}
						return a.entry.item.title;
					},
					".company":"entry.company",
					".dates":function(a){
						if (a.entry.item.end == null) return pdate(a.entry.item.start).getFullYear() + " - present";
						return pdate(a.entry.item.start).getFullYear() + " - " + pdate(a.entry.item.end).getFullYear();
					},
					".description":"entry.description",
					".tags":function(a){
						return (a.entry.item.tags+"").split(",").join(", ");
					}
				},
				sort:sortfun
			}
	};
	$("body").autoRender(general_info);
	$(".experience").render($.merge(education,experience),directive);
	//$(".experience").render(experience,directive);
});