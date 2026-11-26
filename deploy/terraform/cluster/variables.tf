variable "cluster_name" {
  description = "kind cluster name. The kube context will be kind-<name>."
  type        = string
  default     = "classboard"
}

variable "host_port" {
  description = "Host port mapped to the Classboard NodePort."
  type        = number
  default     = 30080
}
