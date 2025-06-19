#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
# This prevents unintended side effects if a command fails.
set -e

# --- Configuration ---
PROJECT_ID="lucid-arch-451211-b0"
SERVICE_NAME="my-spring-app"
REGION="us-west1"

# --- Versioning ---
# Use the short Git commit hash as the version tag.
# This provides excellent traceability between your deployed code and your git history.
# Before running, make sure you have committed your changes to Git.
VERSION=$(git rev-parse --short HEAD)
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
  --revision-suffix "${VERSION}" # This adds the git hash to the revision name in the GCP console for clarity

echo "--- Deployment of ${SERVICE_NAME} version ${VERSION} complete! ---"
