cmake_minimum_required(VERSION 3.10.2)
project("comsquare")

# Define o diretório raiz como o local deste CMakeLists.txt (app/src/main/cpp)
set(ROOT_DIR ${CMAKE_CURRENT_SOURCE_DIR})

# Inclui os cabeçalhos (headers) da pasta sources
include_directories("${ROOT_DIR}/sources")

# Busca recursiva por todos os arquivos fonte (.cpp e .c)
# Procura tanto em 'sources' quanto o 'native-lib.cpp' na raiz
file(GLOB_RECURSE CORE_SOURCES
    "${ROOT_DIR}/sources/*.cpp"
    "${ROOT_DIR}/sources/*.c"
    "${ROOT_DIR}/native-lib.cpp"
)

# Filtros para remover arquivos que não compilam no Android (Qt, SFML, Debugger)
list(FILTER CORE_SOURCES EXCLUDE REGEX ".*Qt.*")
list(FILTER CORE_SOURCES EXCLUDE REGEX ".*SF.*")
list(FILTER CORE_SOURCES EXCLUDE REGEX ".*Debugger.*")
list(FILTER CORE_SOURCES EXCLUDE REGEX ".*tests.*")
list(FILTER CORE_SOURCES EXCLUDE REGEX ".*/main\\.cpp$")

# Imprime no log para debug (opcional, ajuda a ver o que foi incluído)
message(STATUS "Compiling the following files:")
foreach(source_file ${CORE_SOURCES})
    message(STATUS "${source_file}")
endforeach()

# Cria a biblioteca compartilhada (.so)
add_library(
        comsquare
        SHARED
        ${CORE_SOURCES}
)

# Busca bibliotecas do sistema Android
find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)

# Linka as bibliotecas
target_link_libraries(
        comsquare
        ${log-lib}
        ${jnigraphics-lib}
)
