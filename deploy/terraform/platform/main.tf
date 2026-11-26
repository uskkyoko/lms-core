terraform {
  required_version = ">= 1.5.0"

  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.36"
    }
    kubectl = {
      source  = "gavinbunney/kubectl"
      version = "~> 1.14"
    }
  }
}

provider "kubernetes" {
  config_path    = pathexpand("~/.kube/config")
  config_context = "kind-${var.cluster_name}"
}

provider "kubectl" {
  config_path    = pathexpand("~/.kube/config")
  config_context = "kind-${var.cluster_name}"
}

resource "kubectl_manifest" "namespace" {
  yaml_body = file("${path.module}/../../k8s/namespace.yaml")
}

resource "kubernetes_secret_v1" "db" {
  depends_on = [kubectl_manifest.namespace]

  metadata {
    name      = "classboard-db"
    namespace = "classboard"
  }

  type = "Opaque"

  data = {
    DB_PASSWORD = var.db_password
  }
}

resource "kubectl_manifest" "config" {
  for_each = toset([
    "configmap.yaml",
    "postgres-pvc.yaml",
    "postgres-service.yaml",
    "app-service.yaml",
  ])

  yaml_body  = file("${path.module}/../../k8s/${each.value}")
  depends_on = [kubectl_manifest.namespace]
}

resource "kubectl_manifest" "workloads" {
  for_each = toset([
    "postgres-deployment.yaml",
    "app-deployment.yaml",
  ])

  yaml_body = file("${path.module}/../../k8s/${each.value}")

  depends_on = [
    kubectl_manifest.config,
    kubernetes_secret_v1.db,
  ]
}

resource "kubectl_manifest" "pdb" {
  yaml_body  = file("${path.module}/../../k8s/pdb.yaml")
  depends_on = [kubectl_manifest.workloads]
}
