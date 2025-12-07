cmake_minimum_required(VERSION 3.10.2)
project("comsquare")

set(ROOT_DIR ${CMAKE_CURRENT_SOURCE_DIR})
include_directories("${ROOT_DIR}/sources")

file(GLOB_RECURSE CORE_SOURCES
    "${ROOT_DIR}/sources/*.cpp"
    "${ROOT_DIR}/sources/*.c"
    "${ROOT_DIR}/native-lib.cpp"
)

# Filtros
list(FILTER CORE_SOURCES EXCLUDE REGEX ".*Qt.*")
list(FILTER CORE_SOURCES EXCLUDE REGEX ".*SF.*")
list(FILTER CORE_SOURCES EXCLUDE REGEX ".*Debugger.*")
list(FILTER CORE_SOURCES EXCLUDE REGEX ".*tests.*")
list(FILTER CORE_SOURCES EXCLUDE REGEX ".*/main\.cpp$")

add_library(comsquare SHARED ${CORE_SOURCES})

# Adiciona a biblioteca 'android' para manipulação nativa de janelas
find_library(android-lib android)
find_library(log-lib log)

target_link_libraries(comsquare ${log-lib} ${android-lib})
