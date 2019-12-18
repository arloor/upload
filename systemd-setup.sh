#! /bin/bash

cat > /lib/systemd/system/upload.service <<EOF
[Unit]
Description=上传文件
After=network-online.target
Wants=network-online.target

[Service]
WorkingDirectory=/root
ExecStart=/usr/bin/java -jar /root/upload-1.0.0-SNAPSHOT.jar
LimitNOFILE=100000
Restart=always
RestartSec=2

[Install]
WantedBy=multi-user.target
EOF