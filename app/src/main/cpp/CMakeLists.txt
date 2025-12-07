cmake_minimum_required(VERSION 3.10.2)
project("comsquare")

set(SOURCE_DIR ${CMAKE_SOURCE_DIR}/sources)
include_directories(${SOURCE_DIR})

file(GLOB_RECURSE CORE_SOURCES
    "${SOURCE_DIR}/APU/*.cpp"
    "${SOURCE_DIR}/Cartridge/*.cpp"
    "${SOURCE_DIR}/CPU/*.cpp"
    "${SOURCE_DIR}/Memory/*.cpp"
    "${SOURCE_DIR}/Models/*.cpp"
    "${SOURCE_DIR}/PPU/*.cpp"
    "${SOURCE_DIR}/Ram/*.cpp"
    "${SOURCE_DIR}/SNES.cpp"
    "${SOURCE_DIR}/Utility/*.cpp"
    "native-lib.cpp"
)
# Excluir explicitamente arquivos antigos que podem ter sobrado
list(FILTER CORE_SOURCES EXCLUDE REGEX ".*Qt.*")
list(FILTER CORE_SOURCES EXCLUDE REGEX ".*SF.*")
list(FILTER CORE_SOURCES EXCLUDE REGEX ".*Debugger.*")
list(FILTER CORE_SOURCES EXCLUDE REGEX ".*main.cpp")

add_library(comsquare SHARED ${CORE_SOURCES})
find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)
target_link_libraries(comsquare ${log-lib} ${jnigraphics-lib})
