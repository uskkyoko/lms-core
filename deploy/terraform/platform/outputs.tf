output "namespace" {
  value = "classboard"
}

output "app_url" {
  description = "NodePort published by the kind extraPortMapping."
  value       = "http://127.0.0.1:30080"
}

output "app_image" {
  value = var.app_image
}
