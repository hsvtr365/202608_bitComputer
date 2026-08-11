$ErrorActionPreference = 'Stop'

Write-Host 'OCI PostgreSQL tunnel: 127.0.0.1:15432 -> ubuntu:5432'
Write-Host 'Stop: Ctrl+C'
ssh -o ExitOnForwardFailure=yes -N -L 15432:127.0.0.1:5432 ubuntu
