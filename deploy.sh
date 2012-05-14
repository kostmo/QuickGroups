#!/bin/bash

#Take this variable from the user's envirnment
#QUICKGROUPS_SERVERNAME=localhost

ant
scp quickgroups.war ${QUICKGROUPS_SERVERNAME}:/var/lib/tomcat7/webapps
