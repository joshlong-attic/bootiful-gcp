export GCP_PROJECT=$PROJECT_ID

#riff function create java upper --git-repo https://github.com/trisberg/java-fun-upper.git  --git-revision jar  --artifact "upper-1.0.0.jar"  --handler "functions.Upper" --image gcr.io/$GCP_PROJECT/java-fun-upper

export SERVICE_HOST=`kubectl get route upper -o jsonpath="{.status.domain}"`

export SERVICE_IP=`kubectl get svc knative-ingressgateway -n istio-system -o jsonpath="{.status.loadBalancer.ingress[*].ip}"`

curl -w '\n' --header "Host:$SERVICE_HOST" --header "Content-Type: text/plain" http://${SERVICE_IP} -d knative
