cmake_minimum_required(VERSION 3.10.2)
project("comsquare")

set(ROOT_DIR ${CMAKE_CURRENT_SOURCE_DIR})
include_directories("${ROOT_DIR}/sources")

# Configura SDL2
add_subdirectory(SDL2)
include_directories(SDL2/include)

file(GLOB_RECURSE CORE_SOURCES
    "${ROOT_DIR}/sources/*.cpp"
    "${ROOT_DIR}/sources/*.c"
    "${ROOT_DIR}/native-lib.cpp"
)

# Filtra arquivos antigos/incompatíveis
list(FILTER CORE_SOURCES EXCLUDE REGEX ".*Qt.*")
list(FILTER CORE_SOURCES EXCLUDE REGEX ".*SF.*")
list(FILTER CORE_SOURCES EXCLUDE REGEX ".*Debugger.*")
list(FILTER CORE_SOURCES EXCLUDE REGEX ".*tests.*")
list(FILTER CORE_SOURCES EXCLUDE REGEX ".*/main\.cpp$")

add_library(comsquare SHARED ${CORE_SOURCES})

find_library(log-lib log)
find_library(android-lib android)
find_library(gles-lib GLESv2)

# Linka com SDL2 e bibliotecas do sistema
target_link_libraries(comsquare 
    SDL2::SDL2 
    SDL2::SDL2-static 
    ${log-lib} 
    ${android-lib} 
    ${gles-lib}
)
