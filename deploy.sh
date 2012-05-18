#!/bin/bash

ant
scp quickgroups.war ${QUICKGROUPS_SERVERNAME}:/var/lib/tomcat7/webapps/ROOT.war
