variable "origin_domain_name" {
  description = "The DNS domain name of your custom origin (e.g., EC2 DNS)"
  type        = string
}

variable "origin_id" {
  description = "Unique identifier for the origin"
  type        = string
}

variable "comment" {
  description = "Comment for the CloudFront distribution"
  type        = string
  default     = "Managed by Terraform"
}

variable "aliases" {
  description = "Extra CNAMEs for the distribution"
  type        = list(string)
  default     = []
}

variable "acm_certificate_arn" {
  description = "ACM certificate ARN for HTTPS. If not provided, CloudFront default certificate is used."
  type        = string
  default     = null
}
