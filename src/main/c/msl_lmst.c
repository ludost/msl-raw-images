#include <stdio.h>
#include <string.h>
#include "SpiceUsr.h"

int main( int argc, char * argv[] )
{
   SpiceInt    scid;
   SpiceDouble et;
   SpiceChar   utc [32];
   SpiceChar   lmst[32];

   if ( argc < 2 )
   {
      printf ( "Please provide a valid UTC time reference in ISO format\n" );
      return(1);
   }

   furnsh_c( "msl.dat" );
   scid = -76900;

   int i=1;
   for (i; i<argc; i++){
   	str2et_c( ( ConstSpiceChar * ) argv[i], &et );
   	sce2s_c ( scid, et, 32, lmst );
	printf  ( "%i:%s\n", i-1, lmst );   
   }
   
   return(0);

}
