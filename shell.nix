# Keep this version aligned across the repos of our team,
# so we don't have to download different tarballs for each of our repos locally and on CI.
let
  tarball = fetchTarball {
    url = "https://github.com/NixOS/nixpkgs/archive/057f9aecfb71c4437d2b27d3323df7f93c010b7e.tar.gz";
    sha256 = "1ndiv385w1qyb3b18vw13991fzb9wg4cl21wglk89grsfsnra41k";
    # url = "https://github.com/NixOS/nixpkgs/archive/c8bc2f2c0d4abafe71132097976380e59d14c7c8.tar.gz";
    # sha256 = "1yyl34qh6356rk2cyqprl0zgy5gw5v2vzmwkc9jsp825fvb34bbz";
  };
  nixpkgs = import tarball {};
in
with nixpkgs;
mkShell {
  name = "canva-bazelbuild-intellij";
  buildInputs = [
    bazelisk
    bazel-buildtools
    bazel
    jdk11

    gcc
    glib
    libcxx
    libstdcxx5
    libgcc
    glibc

    llvm
    # libclang
    # glibc
    # stdenv.cc.cc.lib
    # llvmPackages_13
    # clang
    # libstdcxx5
    # llvmPackages_14
    # lib
  ];
  # LD_LIBRARY_PATH = lib.makeLibraryPath [ stdenv.cc.cc ];
}
