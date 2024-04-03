
#!/usr/bin/env bash

set -eu -o pipefail

REPO_DIR=$(git rev-parse --show-toplevel)

# shellcheck source=tools/bash/_logging.sh
source "${REPO_DIR}/tools/bash/_logging.sh"
# shellcheck source=tools/observability/bash/_write_local_telemetry.sh
source "${REPO_DIR}/tools/observability/bash/_write_local_telemetry.sh"

main() {
  bazel build //ijwb:ijwb_bazel_zip --define=ij_product=intellij-oss-latest-stable
}

main "$@"
