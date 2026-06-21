#!/bin/bash
set -e
exec > /var/log/user-data.log 2>&1
echo "=== 시작: $(date) ==="

# ── Java 17 설치 ────────────────────────────────────────────────────
dnf install -y java-17-amazon-corretto-headless

# ── MySQL 8.0 설치 ──────────────────────────────────────────────────
dnf install -y mysql-server
systemctl enable mysqld
systemctl start mysqld

# DB 및 유저 생성
mysql -u root << SQL
CREATE DATABASE IF NOT EXISTS jochuckhub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'jochuck'@'localhost' IDENTIFIED BY '${db_password}';
GRANT ALL PRIVILEGES ON jochuckhub.* TO 'jochuck'@'localhost';
FLUSH PRIVILEGES;
SQL

# ── 앱 디렉토리 준비 ────────────────────────────────────────────────
mkdir -p /opt/jochuckhub
chown ec2-user:ec2-user /opt/jochuckhub

# ── 환경변수 파일 ────────────────────────────────────────────────────
cat > /opt/jochuckhub/app.env << EOF
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/jochuckhub?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=jochuck
SPRING_DATASOURCE_PASSWORD=${db_password}
JWT_SECRET=${jwt_secret}
KAKAO_CLIENT_ID=${kakao_client_id}
KAKAO_CLIENT_SECRET=${kakao_client_secret}
KAKAO_REDIRECT_URI=${kakao_redirect_uri}
KAKAO_FRONTEND_REDIRECT_URI=${kakao_frontend_redirect_uri}
EOF

chmod 600 /opt/jochuckhub/app.env

# ── systemd 서비스 ───────────────────────────────────────────────────
# JAR 파일은 scp로 /opt/jochuckhub/app.jar 에 올린 뒤 서비스를 시작합니다.
cat > /etc/systemd/system/jochuckhub.service << 'SERVICEEOF'
[Unit]
Description=JochuckHub Spring Boot App
After=network.target mysqld.service

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/opt/jochuckhub
EnvironmentFile=/opt/jochuckhub/app.env
ExecStart=/usr/bin/java -Xms256m -Xmx400m \
  -Dspring.datasource.url=$${SPRING_DATASOURCE_URL} \
  -Dspring.datasource.username=$${SPRING_DATASOURCE_USERNAME} \
  -Dspring.datasource.password=$${SPRING_DATASOURCE_PASSWORD} \
  -Djwt.secret=$${JWT_SECRET} \
  -Dkakao.client-id=$${KAKAO_CLIENT_ID} \
  -Dkakao.client-secret=$${KAKAO_CLIENT_SECRET} \
  -Dkakao.redirect-uri=$${KAKAO_REDIRECT_URI} \
  -Dkakao.frontend-redirect-uri=$${KAKAO_FRONTEND_REDIRECT_URI} \
  -Dspring.jpa.hibernate.ddl-auto=update \
  -jar /opt/jochuckhub/app.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
SERVICEEOF

systemctl daemon-reload
# 서비스는 JAR 업로드 후 수동으로 start: sudo systemctl start jochuckhub

echo "=== 완료: $(date) ==="
echo "다음 단계: scp로 JAR 업로드 후 sudo systemctl start jochuckhub"
