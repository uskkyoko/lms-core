output "cluster_name" {
  description = "kind cluster name."
  value       = kind_cluster.this.name
}

output "kube_context" {
  description = "kubectl context created for this cluster."
  value       = "kind-${kind_cluster.this.name}"
}

output "app_url" {
  description = "Board URL on the host, once the platform stack is applied."
  value       = "http://127.0.0.1:${var.host_port}"
}
