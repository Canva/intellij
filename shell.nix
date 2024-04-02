# Keep this version aligned across the repos of our team,
# so we don't have to download different tarballs for each of our repos locally and on CI.
let
  tarball = fetchTarball {
    url = "https://github.com/NixOS/nixpkgs/archive/057f9aecfb71c4437d2b27d3323df7f93c010b7e.tar.gz";
    sha256 = "1ndiv385w1qyb3b18vw13991fzb9wg4cl21wglk89grsfsnra41k";
  };
  nixpkgs = import tarball {};
in
with nixpkgs;
clangStdenv.mkDerivation {
  name = "canva-bazelbuild-intellij";
  buildInputs = [
    bazelisk
    # jdk17
    # gcc
    glib
    libcxx
    # llvm
    libclang
    # libgcc
    # llvmPackages_13
    # clang
    # libstdcxx5
    # llvmPackages_14
    # lib
    # glibc
  ];
}
