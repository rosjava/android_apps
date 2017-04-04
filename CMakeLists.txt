cmake_minimum_required(VERSION 2.8.3)
project(android_apps)

find_package(catkin REQUIRED rosjava_build_tools)

catkin_android_setup(assembleRelease assembleDebug uploadArchives)

catkin_package()

##############################################################################
# Installation
##############################################################################

install(DIRECTORY ${CATKIN_DEVEL_PREFIX}/${CATKIN_GLOBAL_MAVEN_DESTINATION}/com/github/rosjava/android_apps/
        DESTINATION ${CATKIN_GLOBAL_MAVEN_DESTINATION}/com/github/rosjava/android_apps)
