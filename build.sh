
#!/usr/bin/env bash

set -eu -o pipefail

REPO_DIR=$(git rev-parse --show-toplevel)

main() {
  bazel build //ijwb:ijwb_bazel_zip --define=ij_product=intellij-oss-latest-stable
}

main "$@"
