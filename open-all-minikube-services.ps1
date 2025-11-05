# Script para abrir todos los servicios de Minikube en PowerShell
# Guarda este archivo como open-all-minikube-services.ps1 y ejecútalo en tu terminal

$services = kubectl get svc --no-headers | ForEach-Object {
    $_.Split(" ")[0]
}

foreach ($service in $services) {
    Write-Host "Obteniendo URL de servicio: $service"
    $url = minikube service $service --url
    if ($url -match "http") {
        Write-Host "Abriendo en navegador: $url"
        Start-Process $url
    }
}
