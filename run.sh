#/bin/bash

 clj -T:build build
 clj -X omnom.app.core/-main
