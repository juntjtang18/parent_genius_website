#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
# This prevents unintended side effects if a command fails.
set -e

# --- Configuration ---
# Centralize your configuration here for easy updates.
PROJECT_ID="lucid-arch-451211-b0"
SERVICE_NAME="my-spring-app"
REGION="us-west1"
VERSION_FILE="version_number.txt"

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
  # Read the current version string.
  CURRENT_VERSION=$(cat "$VERSION_FILE")
  
  # Parse the MAJOR and MINOR parts of the version.
  MAJOR_VERSION=$(echo "$CURRENT_VERSION" | cut -d'.' -f1)
  MINOR_VERSION=$(echo "$CURRENT_VERSION" | cut -d'.' -f2)
  
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

# --- Deployment Steps ---

echo "--- Deploying Spring Boot app version: ${VERSION} ---"

# 1. Build and tag the Docker image with the version
echo "Building Docker image: ${IMAGE_NAME}"
docker build -t "${IMAGE_NAME}" .

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
  --revision-suffix "v${VERSION//./-}" # MODIFIED: Replaced '.' with '-' for a valid revision name

echo "--- Deployment of ${SERVICE_NAME} version ${VERSION} complete! ---"