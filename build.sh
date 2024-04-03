
#!/usr/bin/env nix-shell
#! nix-shell --pure
#! nix-shell -i bash
#! nix-shell ./shell.nix

set -eu -o pipefail

main() {
  bazel build //ijwb:ijwb_bazel_zip --define=ij_product=intellij-oss-latest-stable
}

main "$@"
