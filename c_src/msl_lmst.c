#include <stdio.h>
#include <string.h>
#include "SpiceUsr.h"

int main( int argc, char * argv[] )
{
   SpiceInt    scid;
   SpiceDouble et;
   SpiceChar   utc [32];
   SpiceChar   lmst[32];

   if ( argc != 2 )
   {
      printf ( "Please provide a valid UTC time reference in ISO format\n" );
      return(1);
   }

   furnsh_c( "msl_lmst_ops120808_v1.tsc" );
   furnsh_c( "msl.tls" );
   scid = -76900;

   str2et_c( ( ConstSpiceChar * ) argv[1], &et );
   sce2s_c ( scid, et, 32, lmst );
      
   printf  ( "%s\n", lmst );

   return(0);

}
