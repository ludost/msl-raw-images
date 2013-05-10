// campoint
//
// get camera pointing info for an MSL camera 
//
// usage: campoint <et> <camera>
//     where et = ephemeris time in seconds
//           camera = <MR|ML|NLA|NRA|NLB|NRB|MH|MD|FRA|FRB|FLA|FLB|RRA|RRB|RLA|RLB|CR0>
// e.g., campoint 407975013 NLA
//
// Joe Knapp   jmknapp@gmail.com  12/5/2012

// SPICE furnsh file
#define KFILE   "msl_pointing.dat"
//Arbitrary rotation for display purposes:
#define ZROT 0.0 

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <math.h>
#include <unistd.h>
#include "SpiceUsr.h"

char *compass() ;

void getCamFrame(char* cam, char* cameraFrame){
	// camera is second argument
	if (strcmp(cam,"MR") == 0)
                sprintf(cameraFrame,"MSL_MASTCAM_RIGHT") ;
	else if (strcmp(cam,"ML") == 0)
                sprintf(cameraFrame,"MSL_MASTCAM_LEFT") ;
	else if (strcmp(cam,"NLA") == 0)
                sprintf(cameraFrame,"MSL_NAVCAM_LEFT_A") ;
	else if (strcmp(cam,"NLB") == 0)
                sprintf(cameraFrame,"MSL_NAVCAM_LEFT_B") ;
	else if (strcmp(cam,"NRA") == 0)
                sprintf(cameraFrame,"MSL_NAVCAM_RIGHT_A") ;
	else if (strcmp(cam,"NRB") == 0)
                sprintf(cameraFrame,"MSL_NAVCAM_RIGHT_b") ;
	else if (strcmp(cam,"MH") == 0)
                sprintf(cameraFrame,"MSL_MAHLI") ;
	else if (strcmp(cam,"MD") == 0)
		sprintf(cameraFrame,"MSL_MARDI") ;
	else if (strcmp(cam,"CR0") == 0)
                sprintf(cameraFrame,"MSL_CHEMCAM_RMI") ;
	else if (strcmp(cam,"FRA") == 0)
                sprintf(cameraFrame,"MSL_HAZCAM_FRONT_RIGHT_A") ;
	else if (strcmp(cam,"FRB") == 0)
                sprintf(cameraFrame,"MSL_HAZCAM_FRONT_RIGHT_B") ;
	else if (strcmp(cam,"FLA") == 0)
                sprintf(cameraFrame,"MSL_HAZCAM_FRONT_LEFT_A") ;
	else if (strcmp(cam,"FLB") == 0)
                sprintf(cameraFrame,"MSL_HAZCAM_FRONT_LEFT_B") ;
	else if (strcmp(cam,"RRA") == 0)
                sprintf(cameraFrame,"MSL_HAZCAM_BACK_RIGHT_A") ;
	else if (strcmp(cam,"RRB") == 0)
                sprintf(cameraFrame,"MSL_HAZCAM_BACK_RIGHT_B") ;
	else if (strcmp(cam,"RLA") == 0)
                sprintf(cameraFrame,"MSL_HAZCAM_BACK_LEFT_A") ;
	else if (strcmp(cam,"RLB") == 0)
                sprintf(cameraFrame,"MSL_HAZCAM_BACK_LEFT_B") ;
	else {
		printf("UNKNOWN Camera: %s\n",cam) ;
		exit(1) ;
	}
}

void forTime(int count, SpiceDouble et,char* cameraFrame){
	SpiceDouble cmat[3][3] ;  // 3x3 vector rotation matrix
	SpiceDouble camOpticalAxis[3] ;  // camera optical axis vector in camera frame
	SpiceDouble camtopovec[3] ;  // camera optical axis in topo frame
	SpiceDouble camradius, camaz, camel ; // camera topo vector in latitudinal coordinates

	// get camera->topo rotation matrix cmat
	pxform_c(cameraFrame,"MSL_TOPO",et,cmat) ;

	// make a long vector (optical axis) from the center of the frame
	camOpticalAxis[0] = 0 ;  // x
	camOpticalAxis[1] = 0 ;  // y
	camOpticalAxis[2] = 100000 ;  // z

	// rotate the optical axis to the topo frame using cmat

	// camtopovec will be in rectilinear xyz coordinates 
	mxv_c(cmat,camOpticalAxis,camtopovec) ;

	// convert camtopovec to latitudinal (az/el) coordinates
	reclat_c(camtopovec,&camradius,&camaz,&camel) ;

	// convert to degrees
	camaz *= -dpr_c() ;  // want E to be +90, W -90
	camel *= dpr_c() ;

	// print the bearing string based on this az & el
	printf("%d:%f:%s\n",count,et, compass(camaz+ZROT,camel)) ;
}

int main(int argc, char **argv)
{
	SpiceDouble et ;	// MSL ephemeris time -- seconds since 1/1/1970
	char cameraFrame[100] ;  // camera frame name string
	SPICEDOUBLE_CELL        ( cover, 200000 );
        SpiceDouble             b;
        SpiceDouble             e;
     	SpiceInt                niv;
            #define  TIMLEN         51
        SPICEINT_CELL           ( ids,   1000 );     
	SpiceChar               timstr  [ TIMLEN ];

	// usage
	if (argc < 3) {
		fprintf(stderr,"usage: %s <MR|ML|NLA|NRA|NLB|NRB|MH|MD|FRA|FRB|FLA|FLB|RRA|RRB|RLA|RLB|CR0> [ISO DateTime]\n",argv[0]) ;
		exit(1) ;
	}

	// load SPICE kernels
	furnsh_c(KFILE) ;
	getCamFrame(argv[1],cameraFrame);

	ckobj_c ( "kernels/msl_surf_rsm_tlmenc.bc", &ids );
	scard_c ( 0,  &cover ); 
	ckcov_c("kernels/msl_surf_rsm_tlmres.bc",SPICE_CELL_ELEM_I( &ids, 0 ),SPICEFALSE,"SEGMENT",10.0,"TDB",&cover);
	niv = wncard_c( &cover );
	wnfetd_c ( &cover, niv-1, &b, &e );
	int i=2;
	for (i; i<argc; i++){
   	    str2et_c( ( ConstSpiceChar * ) argv[i], &et );
	    if (et>e || et < 0){
	    	printf("%d:%f:%s\n",i-2,et,"---");
	    } else {
	    	forTime(i-2, et, cameraFrame);
	    }
   	}
	exit(0) ;
}

// return bearing string given az and el in degrees
// result string uses html &deg; for the degree symbol
char *compass(SpiceDouble az, SpiceDouble el) {
	int cardinaldir ;  // compass cardinal direction, 0=N, 1=NNE, 2=NE, ...
	SpiceDouble az2 ;  // shifted azimuth for cardinal direction calc
	char cpoint[10] ;  // cardinal point string, "N", "NNE", ...
	static char bearing[100] ; // result string

	// get cardinal direction number, 22.5 degrees for each bucket
	az2 = az + 11.25 ; 
	if (az2 < 0)
		az2 += 360 ;
	if (az2 > 360)
		az2 -= 360 ;
	cardinaldir = az2/22.5 ;

	// normalize azimuth to 0-360 if needed
	if (az < 0) 
		az += 360 ;

	// set cardinal direction string
	switch(cardinaldir) {
		case 0:
			strcpy(cpoint,"N") ;
			break;
		case 1:
			strcpy(cpoint,"NNE") ;
			break;
		case 2:
			strcpy(cpoint,"NE") ;
			break;
		case 3:
			strcpy(cpoint,"ENE") ;
			break;
		case 4:
			strcpy(cpoint,"E") ;
			break;
		case 5:
			strcpy(cpoint,"ESE") ;
			break;
		case 6:
			strcpy(cpoint,"SE") ;
			break;
		case 7:
			strcpy(cpoint,"SSE") ;
			break;
		case 8:
			strcpy(cpoint,"S") ;
			break;
		case 9:
			strcpy(cpoint,"SSW") ;
			break;
		case 10:
			strcpy(cpoint,"SW") ;
			break;
		case 11:
			strcpy(cpoint,"WSW") ;
			break;
		case 12:
			strcpy(cpoint,"W") ;
			break;
		case 13:
			strcpy(cpoint,"WNW") ;
			break;
		case 14:
			strcpy(cpoint,"NW") ;
			break;
		case 15:
			strcpy(cpoint,"NNW") ;
			break;
		default:
			strcpy(cpoint,"???") ;
	}
	sprintf(bearing,"%.2f&deg; (%s), elevation %.2f&deg;",az,cpoint,el) ;
	return(bearing) ;
}
