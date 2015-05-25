#!/bin/sh

# The HEAP_SIZE variable defines the Java heap size in MB.
# That is the total amount of memory available to MZmine 2.
# By default we set this to half of the total memory or 
# 2048 MB less than the total memory.
# Feel free to adjust the HEAP_SIZE according to your needs.

echo "Checking physical memory size..."
TOTAL_MEMORY=`free -b | awk '/Mem:/ { print int($2 / 1024^2) }'`
echo "Found $TOTAL_MEMORY MB memory"

if [ "$TOTAL_MEMORY" -gt 4096 ]; then
	HEAP_SIZE=`expr $TOTAL_MEMORY - 2048`
else
	HEAP_SIZE=`expr $TOTAL_MEMORY / 2`
fi
echo Java heap size set to $HEAP_SIZE MB

# Do not modify:
# Store this MZmine instance, a UNID.
export MZMINE_UNID="MZMINE"$$

# The TMP_FILE_DIRECTORY parameter defines the location where temporary 
# files (parsed raw data) will be placed. Default is /tmp.
TMP_FILE_DIRECTORY=/tmp
# Do not modify:
# Make the working temp dir unique per MZmine instance.
export TMP_FILE_DIRECTORY=$TMP_FILE_DIRECTORY/$MZMINE_UNID
mkdir $TMP_FILE_DIRECTORY

# Set R environment variables.
#**#export R_HOME=/usr/lib64/R
export R_HOME=/usr/lib/R

export R_SHARE_DIR=/usr/share/R/share 
export R_INCLUDE_DIR=/usr/share/R/include
export R_DOC_DIR=/usr/share/R/doc
#**#export R_LIBS_USER=${HOME}/R/x86_64-pc-linux-gnu-library/2.10
export R_LIBS_USER=${R_HOME}/site-library

# Include R shared libraries in LD_LIBRARY_PATH.
export LD_LIBRARY_PATH=${R_HOME}/lib:${R_HOME}/bin

# The directory holding the JRI shared library (libjri.so).
JRI_LIB_PATH=${R_LIBS_USER}/rJava/jri

# It is usually not necessary to modify the JAVA_COMMAND parameter, but 
# if you like to run a specific Java Virtual Machine, you may set the 
# path to the java command of that JVM.
JAVA_COMMAND=java

# It is not necessary to modify the following section
JAVA_PARAMETERS="-showversion -classpath lib/\* -Djava.ext.dirs= -XX:+UseParallelGC -Djava.io.tmpdir=$TMP_FILE_DIRECTORY -Xms${HEAP_SIZE}m -Xmx${HEAP_SIZE}m -Djava.library.path=${JRI_LIB_PATH}"
MAIN_CLASS=net.sf.mzmine.main.MZmineCore

# Make sure we are in the correct directory
SCRIPTDIR=`dirname "$0"`
cd "$SCRIPTDIR"

# This command starts the Java Virtual Machine
echo "$JAVA_PARAMETERS" $MAIN_CLASS "$@" | xargs $JAVA_COMMAND


# Do not modify:
# Cleaning Rserve instance if MZmine was killed ungracefully (ex. kill -9 ...)
#pidfile="$TMP_FILE_DIRECTORY/rs_pid.pid"
pidfile=`ls -tr $TMP_FILE_DIRECTORY/rs_pid*.pid 2> /dev/null | tail -1 2> /dev/null`
# File exists (Rserve was used during MZmine session)
if [ -e "$pidfile" ]
then
	echo "Found pidfile: $pidfile"
	value=`cat "$pidfile"`
	#echo "Main Rserve instance pid was '$value'..."
	##kill -9 $value		# Kills only the main instance (not the children)
	# Kill the whole remaining tree from main instance
	#kill -9 -$value		# Kills the whole tree
	if [ -z "$value" ]
	then
		echo "No remaining instances of Rserve."
	else
		echo "Killing Rserve tree from main instance / pid: '$value'."
		kill -9 -$value
		rm "$pidfile"
	fi
fi
