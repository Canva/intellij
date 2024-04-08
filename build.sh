
#!/usr/bin/env nix-shell
#! nix-shell --pure
#! nix-shell -i bash
#! nix-shell ./shell.nix

set -eu -o pipefail

main() {
  echo "--- Build Bazel Plugin"
  bazel build //ijwb:ijwb_bazel_zip --define=ij_product=intellij-oss-latest-stable
  
  echo "--- Push to Depot"
  $DEPOT -b -f ./bazel-bin/ijwb/ijwb_bazel.zip -v 2023.3 -s canva -d bazelbuild-intellij-canva-shared-index.zip
}

main "$@"
