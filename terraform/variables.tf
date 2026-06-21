variable "aws_region" {
  description = "AWS 리전"
  type        = string
  default     = "ap-northeast-2"
}

variable "app_name" {
  description = "애플리케이션 이름"
  type        = string
  default     = "jochuckhub"
}

variable "key_pair_name" {
  description = "EC2 SSH 접속용 키페어 이름 (AWS 콘솔에서 미리 생성 필요)"
  type        = string
}

variable "db_password" {
  description = "MySQL root 비밀번호"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "JWT 서명 키 (256비트 이상 Base64)"
  type        = string
  sensitive   = true
}

variable "kakao_client_id" {
  description = "카카오 REST API 키"
  type        = string
  sensitive   = true
}

variable "kakao_client_secret" {
  description = "카카오 Client Secret"
  type        = string
  sensitive   = true
}

variable "kakao_redirect_uri" {
  description = "카카오 OAuth2 Redirect URI"
  type        = string
}

variable "kakao_frontend_redirect_uri" {
  description = "로그인 후 프론트엔드 리다이렉트 URI"
  type        = string
}
