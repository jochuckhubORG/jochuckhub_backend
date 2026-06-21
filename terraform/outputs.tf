output "ec2_public_ip" {
  description = "EC2 퍼블릭 IP"
  value       = aws_instance.app.public_ip
}

output "app_url" {
  description = "앱 접속 URL"
  value       = "http://${aws_instance.app.public_ip}:8080"
}

output "swagger_url" {
  description = "Swagger UI URL"
  value       = "http://${aws_instance.app.public_ip}:8080/swagger-ui/index.html"
}

output "ssh_command" {
  description = "SSH 접속 명령어"
  value       = "ssh -i ~/.ssh/${var.key_pair_name}.pem ec2-user@${aws_instance.app.public_ip}"
}
