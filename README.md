# Hoenir

Hoenir is a composition of multiple Kubernetes controllers that makes your life easier.

## Grafana Dashboard Controller

Downloads Grafana dashboards into ConfigMaps given a dashboard id and an optional revision.
The ConfigMap is then usable as [persistent Grafana dashboard](https://rancher.com/docs/rancher/v2.6/en/monitoring-alerting/guides/persist-grafana/).

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

Creates an Ingress to Istio's Ingress Gateway for a VirtualService definition.

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
