#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
# This prevents unintended side effects if a command fails.
set -e

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# --- Configuration ---
# Centralize your configuration here for easy updates.
PROJECT_ID="lucid-arch-451211-b0"
SERVICE_NAME="my-spring-app"
REGION="us-west1"
VERSION_FILE="version_number.txt"

# --- Application Build ---
# Create a fresh, tested JAR before changing the version or deploying anything.
echo "--- Building latest Spring Boot application ---"
mvn clean package

# --- Versioning (MAJOR.MINOR) ---
# This script uses a single file 'version_number.txt' to manage the version.
# - It reads the version (e.g., "1.5").
# - It increments the MINOR number (e.g., to "1.6").
# - It saves the new version back to the file.
# To change the MAJOR version, manually edit the file (e.g., change "1.6" to "2.0").

# Initialize or read the version number.
if [ ! -f "$VERSION_FILE" ]; then
  echo "Info: '$VERSION_FILE' not found. Creating it with initial version 1.0."
  VERSION="1.0"
else
  # Read and normalize current version (trim spaces/newlines/CR).
  CURRENT_VERSION=$(tr -d '\r' < "$VERSION_FILE" | xargs)

  # Accept only MAJOR.MINOR numeric format. Recover safely if malformed.
  if [[ ! "$CURRENT_VERSION" =~ ^[0-9]+\.[0-9]+$ ]]; then
    echo "Warning: invalid version '$CURRENT_VERSION' in '$VERSION_FILE'. Resetting to 1.0."
    CURRENT_VERSION="1.0"
  fi

  # Parse MAJOR and MINOR parts.
  MAJOR_VERSION=${CURRENT_VERSION%%.*}
  MINOR_VERSION=${CURRENT_VERSION##*.}

  # Increment the MINOR version.
  NEW_MINOR_VERSION=$((MINOR_VERSION + 1))

  # Combine to create the new version string.
  VERSION="${MAJOR_VERSION}.${NEW_MINOR_VERSION}"
fi

# Save the new version number back to the file for the next run.
echo "$VERSION" > "$VERSION_FILE"


# --- Image Name ---
# Construct the full image name with the dynamic version tag.
IMAGE_NAME="gcr.io/${PROJECT_ID}/${SERVICE_NAME}:${VERSION}"
REVISION_SUFFIX="v${VERSION//./-}-$(date +%m%d%H%M%S)"

# --- Deployment Steps ---

echo "--- Deploying Spring Boot app version: ${VERSION} ---"

# 1. Build and tag the Docker image with the freshly built JAR
echo "Building Docker image: ${IMAGE_NAME}"
HOST_ARCH="$(uname -m)"
DOCKER_BUILD_ARGS=(--provenance=false)
if [[ "$HOST_ARCH" == "arm64" || "$HOST_ARCH" == "aarch64" ]]; then
  echo "Host is ${HOST_ARCH}; cross-building linux/amd64 for Cloud Run"
  DOCKER_BUILD_ARGS+=(--platform linux/amd64)
else
  echo "Host is ${HOST_ARCH}; building native linux/amd64 for Cloud Run"
fi
docker build "${DOCKER_BUILD_ARGS[@]}" -t "${IMAGE_NAME}" .

# 2. Push the versioned Docker image to Google Container Registry
echo "Pushing Docker image..."
docker push "${IMAGE_NAME}"

# 3. Deploy the new version to Cloud Run
echo "Deploying to Cloud Run service: ${SERVICE_NAME}"
gcloud run deploy "${SERVICE_NAME}" \
  --image "${IMAGE_NAME}" \
  --platform "managed" \
  --region "${REGION}" \
  --allow-unauthenticated \
  --revision-suffix "${REVISION_SUFFIX}" \
  --set-env-vars SPRING_PROFILES_ACTIVE=run


echo "--- Deployment of ${SERVICE_NAME} version ${VERSION} complete! ---"
