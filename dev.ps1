$ErrorActionPreference = "Stop"

$repositoryRoot = $PSScriptRoot
$healthUrl = "http://localhost:8000/health"

Push-Location $repositoryRoot
try {
    # Spring Boot runs on this machine, not in Docker, so the AI container reaches it through the host.
    $env:AI_BACKEND_URL = "http://host.docker.internal:8080"
    docker compose up --build --detach sage-math ai-service

    Write-Host "Waiting for SageMath at $healthUrl ..."
    $healthy = $false
    for ($attempt = 1; $attempt -le 60; $attempt++) {
        try {
            $response = Invoke-RestMethod -Uri $healthUrl -TimeoutSec 2
            if ($response.status -eq "ok") {
                $healthy = $true
                break
            }
        }
        catch {
            Start-Sleep -Seconds 2
        }
    }

    if (-not $healthy) {
        throw "SageMath did not become healthy within 120 seconds. Run 'docker compose logs sage-math' for details."
    }

    Write-Host "SageMath is ready. Starting Spring Boot..."
    & .\mvnw.cmd spring-boot:run
    if ($LASTEXITCODE -ne 0) {
        throw "Spring Boot exited with code $LASTEXITCODE."
    }
}
finally {
    Write-Host "Stopping the SageMath and AI services..."
    docker compose stop sage-math ai-service
    Pop-Location
}
