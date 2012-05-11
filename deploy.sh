#!/bin/bash

SERVERNAME=localhost

ant
scp quickgroups.war ${SERVERNAME}:/var/lib/tomcat7/webapps
