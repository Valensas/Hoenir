# Hoenir

Hoenir is a composition of multiple Kubernetes controllers that makes your life easier.

## Running the project

To run from source, use `./gradlew run`.

A Docker image is also available: `docker run -v ~/.kube/config:/root/.kube/config valensas/hoenir`.

Hoenir uses the same configuration as `kubectl` to connect to your cluster. Therefore, you can set any environment
variables necessary to connect to your cluster such as `KUBECONFIG` or `KUBECONTEXT`. When running within Kubernetes,
it will by default pick up the service account credentials and auto-discover the master endpoint.


## Grafana Dashboard Controller

Downloads Grafana dashboards into ConfigMaps given a dashboard id and an optional revision.
The ConfigMap is then usable as [persistent Grafana dashboard](https://ranchermanager.docs.rancher.com/how-to-guides/advanced-user-guides/monitoring-alerting-guides/create-persistent-grafana-dashboard#docusaurus_skipToContent_fallback).

### Usage

Create a ConfigMap with the proper labels

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: my-dashboard
  labels:
    grafana_dashboard: '1'
    grafana.valensas.com/dashboard: '11074' # The dashboard id from grafana.com
    grafana.valensas.com/revision: '9' # The revision of the dashboard from grafana.com, optional
```

The `data` of the ConfigMap will be automatically populated by the controller.

### Configuration

Use the following environment variable to configure the controller:

`WATCH_NAMESPACE`: The namespace to watch for ConfigMaps. Unset to watch all namespaces. Defaults to all namesapces.

`GRAFANA_DISABLED`: Set to any value to disable the controller.

`GRAFANA_DEFAULT_DATASOURCE_NAME`: The default datasource to use in dashboards. Defaults to "Prometheus".

`GRAFANA_WORKERS`: Number of workers to use for the controller. Defaults to cores / 2.

## Virtual Service Controller

Creates an Ingress to Istio's Ingress Gateway for a VirtualService definition. This setup is mainly useful when
using both an Ingress Controller and Istio but want to have a single point of entry for both.

### Usage

Create a VirtualService like the one below:

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: web
  namespace: test
  labels:
    hello: 'world'
    # Set this label for the controller to pick up the VirtualService
    istio.valensas.com/ingress: 'true'
  annotations:
    # Sets the ingressClass for the ingress, optional
    istio.valensas.com/ingress-class: 'nginx'
    # Use TLS for the Ingress 
    istio.valensas.com/ingress-tls: 'true'
    # Use this secret for TLS. f TLS is enabled without a
    # secret, a unique name is generated.
    istio.valensas.com/tls-secret: 'my-secret'
    # The ingress gateway service to use for the created Ingress
    istio.valensas.com/ingressgateway-service: 'istio-ingressgateway'
    # The ingress gateway port to use for the created Ingress
    istio.valensas.com/ingressgateway-port: '8080'
    
spec:
  hosts:
  - example.com
  http:
  - route:
    - destination:
        host: web
        port:
          number: 80
```

### Configuration

Use the following environment variable to configure the controller:

`WATCH_NAMESPACE`: The namespace to watch for ConfigMaps. Unset to watch all namespaces. Defaults to all namesapces.

`VIRTUALSERVICE_DISABLED`: Set to any value to disable the controller.

`ISTIO_NAMESPACE`: The namespace where Istio is installed. Defaults to "istio-system"

`ISTIO_INGRESSGATEWAY_SERVICE`: Service name of the Istio ingress gateway. Defaults to "istio-ingressgateway".

`ISTIO_INGRESSGATEWAY_PORT`: The port to use for the Istio ingress gateway. Defaults to 8080.

`VIRTUALSERVICE_WORKERS`: Number of workers to use for the controller. Defaults to cores / 2.
