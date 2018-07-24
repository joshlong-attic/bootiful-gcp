source ~/josh-env-ubuntu/bin/gcp/gke.sh

export GCP_PROJECT=$PROJECT_ID
echo "GCP_PROJECT: $GCP_PROJECT"

riff function create java upper --git-repo https://github.com/trisberg/java-fun-upper.git  --git-revision jar  --artifact "upper-1.0.0.jar"  --handler "functions.Upper" --image gcr.io/$GCP_PROJECT/java-fun-upper
