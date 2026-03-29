# Zoho Domain TXT Records (Verification + SPF)
resource "aws_route53_record" "zoho_txt" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = var.domain_name
  type    = "TXT"
  ttl     = 3600
  records = [
    "zoho-verification=zb46990130.zmverify.zoho.com",
    "v=spf1 include:zohomail.com ~all"
  ]
}

# Zoho MX Records (Receiving Email)
resource "aws_route53_record" "zoho_mx" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = var.domain_name
  type    = "MX"
  ttl     = 3600
  records = [
    "10 mx.zoho.com",
    "20 mx2.zoho.com",
    "50 mx3.zoho.com"
  ]
}

# Zoho DKIM Record (Identity Verification)
resource "aws_route53_record" "zoho_dkim" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = "zmail._domainkey.${var.domain_name}"
  type    = "TXT"
  ttl     = 3600
  records = ["v=DKIM1; k=rsa; p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCSzrIQvagwjf/aVBx6ne/quMzYtiF6/0Q21NSOSIdB4a0iNg3BaeMIo9UFzKj1A5k1/jb6TUuZPRhIFCD0y/wSwnhW5sNk3BdIB5R/q5L9DFO4cbU+2WHNjoiM5/+SBpVBYeTQ3HO0oFKouDGwPM0X79r5CjEy3MxY8uRTTVuhcQIDAQAB"]
}
