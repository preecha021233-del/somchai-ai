cmake_minimum_required(VERSION 3.22.1)
project(somchai_llama LANGUAGES C CXX)

include(FetchContent)

# Pin the native inference engine to an audited upstream commit.
FetchContent_Declare(
    llama_cpp
    GIT_REPOSITORY https://github.com/ggml-org/llama.cpp.git
    GIT_TAG 8d60873dfb8619f2b5ed31adfbab07cccb40db5c
    GIT_SHALLOW FALSE
)

set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN OFF CACHE BOOL "" FORCE)

FetchContent_MakeAvailable(llama_cpp)

add_library(somchai_llama SHARED somchai_llama.cpp)
target_compile_features(somchai_llama PRIVATE cxx_std_17)
target_compile_options(somchai_llama PRIVATE -O3 -fexceptions -frtti)
target_include_directories(somchai_llama PRIVATE ${llama_cpp_SOURCE_DIR}/include)
target_link_libraries(somchai_llama PRIVATE llama android log)
