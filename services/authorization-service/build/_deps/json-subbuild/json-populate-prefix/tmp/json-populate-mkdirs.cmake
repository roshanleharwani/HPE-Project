# Distributed under the OSI-approved BSD 3-Clause License.  See accompanying
# file Copyright.txt or https://cmake.org/licensing for details.

cmake_minimum_required(VERSION 3.5)

file(MAKE_DIRECTORY
  "/home/kumaresh/Desktop/Dev/HPE/HPE-Project/services/authorization-service/build/_deps/json-src"
  "/home/kumaresh/Desktop/Dev/HPE/HPE-Project/services/authorization-service/build/_deps/json-build"
  "/home/kumaresh/Desktop/Dev/HPE/HPE-Project/services/authorization-service/build/_deps/json-subbuild/json-populate-prefix"
  "/home/kumaresh/Desktop/Dev/HPE/HPE-Project/services/authorization-service/build/_deps/json-subbuild/json-populate-prefix/tmp"
  "/home/kumaresh/Desktop/Dev/HPE/HPE-Project/services/authorization-service/build/_deps/json-subbuild/json-populate-prefix/src/json-populate-stamp"
  "/home/kumaresh/Desktop/Dev/HPE/HPE-Project/services/authorization-service/build/_deps/json-subbuild/json-populate-prefix/src"
  "/home/kumaresh/Desktop/Dev/HPE/HPE-Project/services/authorization-service/build/_deps/json-subbuild/json-populate-prefix/src/json-populate-stamp"
)

set(configSubDirs )
foreach(subDir IN LISTS configSubDirs)
    file(MAKE_DIRECTORY "/home/kumaresh/Desktop/Dev/HPE/HPE-Project/services/authorization-service/build/_deps/json-subbuild/json-populate-prefix/src/json-populate-stamp/${subDir}")
endforeach()
if(cfgdir)
  file(MAKE_DIRECTORY "/home/kumaresh/Desktop/Dev/HPE/HPE-Project/services/authorization-service/build/_deps/json-subbuild/json-populate-prefix/src/json-populate-stamp${cfgdir}") # cfgdir has leading slash
endif()
