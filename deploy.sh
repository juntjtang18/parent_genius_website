docker build -t gcr.io/lucid-arch-451211-b0/my-spring-app .
docker push gcr.io/lucid-arch-451211-b0/my-spring-app
gcloud run deploy my-spring-app \
  --image gcr.io/lucid-arch-451211-b0/my-spring-app \
  --platform managed \
  --region us-west1 \
  --allow-unauthenticated

