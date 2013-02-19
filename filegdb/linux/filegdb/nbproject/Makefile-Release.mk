#
# Generated Makefile - do not edit!
#
# Edit the Makefile in the project folder instead (../Makefile). Each target
# has a -pre and a -post target defined where you can add customized code.
#
# This makefile implements configuration specific macros and targets.


# Environment
MKDIR=mkdir
CP=cp
GREP=grep
NM=nm
CCADMIN=CCadmin
RANLIB=ranlib
CC=gcc
CCC=g++
CXX=g++
FC=gfortran
AS=as

# Macros
CND_PLATFORM=GNU-Linux-x86
CND_CONF=Release
CND_DISTDIR=dist
CND_BUILDDIR=build

# Include project Makefile
include Makefile

# Object Directory
OBJECTDIR=${CND_BUILDDIR}/${CND_CONF}/${CND_PLATFORM}

# Object Files
OBJECTFILES= \
	${OBJECTDIR}/convstr.o \
	${OBJECTDIR}/_ext/61665655/table.o \
	${OBJECTDIR}/_ext/61665655/menv.o \
	${OBJECTDIR}/_ext/61665655/stdafx.o \
	${OBJECTDIR}/_ext/61665655/enumRows.o \
	${OBJECTDIR}/_ext/61665655/jstring.o \
	${OBJECTDIR}/_ext/61665655/row.o \
	${OBJECTDIR}/_ext/61665655/geodatabase.o


# C Compiler Flags
CFLAGS=

# CC Compiler Flags
CCFLAGS=-std=gnu++0x
CXXFLAGS=-std=gnu++0x

# Fortran Compiler Flags
FFLAGS=

# Assembler Flags
ASFLAGS=

# Link Libraries and Options
LDLIBSOPTIONS=-L../../filegdb/esri_linux64_dependencies -lfgdbunixrtl -lFileGDBAPI

# Build Targets
.build-conf: ${BUILD_SUBPROJECTS}
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libfilegdb.so

${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libfilegdb.so: ${OBJECTFILES}
	${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}
	${LINK.cc} -Wl -export-dynamic -shared -o ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libfilegdb.so -fPIC ${OBJECTFILES} ${LDLIBSOPTIONS} 

${OBJECTDIR}/convstr.o: convstr.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} $@.d
	$(COMPILE.cc) -O2 -I../../filegdb/esri_headers/linux -I../../filegdb/esri_headers -I../../filegdb -I../../../rel/jni -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/convstr.o convstr.cpp

${OBJECTDIR}/_ext/61665655/table.o: ../../filegdb/table.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/61665655
	${RM} $@.d
	$(COMPILE.cc) -O2 -I../../filegdb/esri_headers/linux -I../../filegdb/esri_headers -I../../filegdb -I../../../rel/jni -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/61665655/table.o ../../filegdb/table.cpp

${OBJECTDIR}/_ext/61665655/menv.o: ../../filegdb/menv.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/61665655
	${RM} $@.d
	$(COMPILE.cc) -O2 -I../../filegdb/esri_headers/linux -I../../filegdb/esri_headers -I../../filegdb -I../../../rel/jni -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/61665655/menv.o ../../filegdb/menv.cpp

${OBJECTDIR}/_ext/61665655/stdafx.o: ../../filegdb/stdafx.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/61665655
	${RM} $@.d
	$(COMPILE.cc) -O2 -I../../filegdb/esri_headers/linux -I../../filegdb/esri_headers -I../../filegdb -I../../../rel/jni -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/61665655/stdafx.o ../../filegdb/stdafx.cpp

${OBJECTDIR}/_ext/61665655/enumRows.o: ../../filegdb/enumRows.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/61665655
	${RM} $@.d
	$(COMPILE.cc) -O2 -I../../filegdb/esri_headers/linux -I../../filegdb/esri_headers -I../../filegdb -I../../../rel/jni -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/61665655/enumRows.o ../../filegdb/enumRows.cpp

${OBJECTDIR}/_ext/61665655/jstring.o: ../../filegdb/jstring.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/61665655
	${RM} $@.d
	$(COMPILE.cc) -O2 -I../../filegdb/esri_headers/linux -I../../filegdb/esri_headers -I../../filegdb -I../../../rel/jni -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/61665655/jstring.o ../../filegdb/jstring.cpp

${OBJECTDIR}/_ext/61665655/row.o: ../../filegdb/row.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/61665655
	${RM} $@.d
	$(COMPILE.cc) -O2 -I../../filegdb/esri_headers/linux -I../../filegdb/esri_headers -I../../filegdb -I../../../rel/jni -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/61665655/row.o ../../filegdb/row.cpp

${OBJECTDIR}/_ext/61665655/geodatabase.o: ../../filegdb/geodatabase.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/61665655
	${RM} $@.d
	$(COMPILE.cc) -O2 -I../../filegdb/esri_headers/linux -I../../filegdb/esri_headers -I../../filegdb -I../../../rel/jni -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/61665655/geodatabase.o ../../filegdb/geodatabase.cpp

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r ${CND_BUILDDIR}/${CND_CONF}
	${RM} ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libfilegdb.so

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
