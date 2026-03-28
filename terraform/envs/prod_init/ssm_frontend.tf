resource "aws_ssm_parameter" "frontend_url" {
  name        = "/${var.project_name}/${var.environment}/frontend/url"
  description = "Frontend application URL"
  type        = "String"
  value       = "https://${var.domain_name}"

  tags = { Module = "budget" }
}
