cmake_minimum_required(VERSION 3.23)

project(jdup NONE)

find_package(Java 18 REQUIRED)
include(UseJava)

add_jar(
	"${PROJECT_NAME}"
	SOURCES
	src/jdup.java
	MANIFEST
	src/manifest.mf
	)

set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)
