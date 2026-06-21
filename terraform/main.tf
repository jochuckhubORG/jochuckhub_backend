terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  required_version = ">= 1.3"
}

provider "aws" {
  region = var.aws_region
}

# -------------------------------------------------------------------
# AMI: Amazon Linux 2023 최신
# -------------------------------------------------------------------
data "aws_ami" "amazon_linux_2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# -------------------------------------------------------------------
# Security Group — SSH(22) + 앱(8080)
# -------------------------------------------------------------------
resource "aws_security_group" "app_sg" {
  name        = "${var.app_name}-sg"
  description = "SSH + Spring Boot"

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Spring Boot"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# -------------------------------------------------------------------
# EC2 — t2.micro, MySQL + Spring Boot 함께 실행
# -------------------------------------------------------------------
resource "aws_instance" "app" {
  ami                    = data.aws_ami.amazon_linux_2023.id
  instance_type          = "t2.micro"
  key_name               = var.key_pair_name
  vpc_security_group_ids = [aws_security_group.app_sg.id]

  user_data = templatefile("${path.module}/user_data.sh", {
    db_password                 = var.db_password
    jwt_secret                  = var.jwt_secret
    kakao_client_id             = var.kakao_client_id
    kakao_client_secret         = var.kakao_client_secret
    kakao_redirect_uri          = var.kakao_redirect_uri
    kakao_frontend_redirect_uri = var.kakao_frontend_redirect_uri
    app_name                    = var.app_name
  })

  tags = {
    Name = "${var.app_name}-server"
  }
}
