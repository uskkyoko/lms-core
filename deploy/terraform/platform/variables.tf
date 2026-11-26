variable "cluster_name" {
  description = "Must match the kind cluster name. Context is kind-<name>."
  type        = string
  default     = "classboard"
}

variable "db_password" {
  description = "Postgres password. Set TF_VAR_db_password. Do not commit it."
  type        = string
  sensitive   = true
}

variable "app_image" {
  description = "Documented image tag. The Deployment manifest pins classboard:local; load that tag into kind before apply."
  type        = string
  default     = "classboard:local"
}
