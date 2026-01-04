pipeline {
  agent any

  options {
    timestamps()
  }

  environment {
    COMPOSE_FILE = "docker-compose.yml"

    // compose portların
    BACKEND_URL  = "http://localhost:6969"
    FRONTEND_URL = "http://localhost:5173"

    // Selenium test property'leri
    UI_BASE_URL  = "http://localhost:5173"
    UI_HEADLESS  = "true"
    // UI'nizde localStorage'dan okunuyorsa bunu da kullanırız (aşağıda not var)
    E2E_API_BASE = "http://localhost:6969"
  }

  stages {

    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Docker Compose Up') {
      steps {
        bat """
          docker version
          docker compose version
          docker compose -f %COMPOSE_FILE% up -d --build
          docker compose -f %COMPOSE_FILE% ps
        """
      }
    }

    stage('Wait: DB Healthy') {
      steps {
        // db container ismi compose'da municipality_db
        bat """
          powershell -NoProfile -ExecutionPolicy Bypass -Command ^
            "$ErrorActionPreference='Stop';" ^
            "$name='municipality_db';" ^
            "Write-Host ('Waiting DB health: ' + $name);" ^
            "$ok=$false;" ^
            "for($i=1; $i -le 60; $i++){" ^
            "  try {" ^
            "    $status = (docker inspect -f '{{.State.Health.Status}}' $name) 2>$null;" ^
            "    if($status -match 'healthy'){ $ok=$true; break }" ^
            "  } catch {}" ^
            "  Start-Sleep -Seconds 2;" ^
            "}" ^
            "if(-not $ok){ docker logs $name; throw 'DB did not become healthy in time.' }" ^
            "Write-Host 'DB is healthy.'"
        """
      }
    }

    stage('Wait: Backend Ready') {
      steps {
        bat """
          powershell -NoProfile -ExecutionPolicy Bypass -Command ^
            "$ErrorActionPreference='Stop';" ^
            "$url='%BACKEND_URL%/actuator/health';" ^
            "Write-Host ('Waiting Backend: ' + $url);" ^
            "$ok=$false;" ^
            "for($i=1; $i -le 60; $i++){" ^
            "  try {" ^
            "    $r = Invoke-WebRequest -UseBasicParsing -TimeoutSec 2 -Uri $url;" ^
            "    if($r.StatusCode -eq 200 -and $r.Content -match 'UP'){ $ok=$true; break }" ^
            "  } catch {}" ^
            "  Start-Sleep -Seconds 2;" ^
            "}" ^
            "if(-not $ok){ docker logs municipality_backend; throw 'Backend did not become ready in time.' }" ^
            "Write-Host 'Backend is ready.'"
        """
      }
    }

    stage('Wait: Frontend Ready') {
      steps {
        bat """
          powershell -NoProfile -ExecutionPolicy Bypass -Command ^
            "$ErrorActionPreference='Stop';" ^
            "$url='%FRONTEND_URL%';" ^
            "Write-Host ('Waiting Frontend: ' + $url);" ^
            "$ok=$false;" ^
            "for($i=1; $i -le 60; $i++){" ^
            "  try {" ^
            "    $r = Invoke-WebRequest -UseBasicParsing -TimeoutSec 2 -Uri $url;" ^
            "    if($r.StatusCode -ge 200 -and $r.StatusCode -lt 500){ $ok=$true; break }" ^
            "  } catch {}" ^
            "  Start-Sleep -Seconds 2;" ^
            "}" ^
            "if(-not $ok){ docker logs municipality_frontend; throw 'Frontend did not become ready in time.' }" ^
            "Write-Host 'Frontend is ready.'"
        """
      }
    }

    stage('Build & UI E2E Tests (mvn verify)') {
      steps {
        dir('municipality-service-backend') {
          bat """
            if exist mvnw.cmd (
              mvnw.cmd ^
                -Dui.baseUrl=%UI_BASE_URL% ^
                -Dui.headless=%UI_HEADLESS% ^
                -De2e.apiBase=%E2E_API_BASE% ^
                clean verify
            ) else (
              mvn ^
                -Dui.baseUrl=%UI_BASE_URL% ^
                -Dui.headless=%UI_HEADLESS% ^
                -De2e.apiBase=%E2E_API_BASE% ^
                clean verify
            )
          """
        }
      }
    }

    stage('Fake Deploy') {
      steps {
        echo "✅ Testler geçti. Fake deploy yapılıyor..."
        echo "Deploy tamamlandı (fake)."
      }
    }
  }

  post {
    always {
      // junit raporları
      junit allowEmptyResults: true, testResults: 'municipality-service-backend/**/target/surefire-reports/*.xml'
      junit allowEmptyResults: true, testResults: 'municipality-service-backend/**/target/failsafe-reports/*.xml'

      // docker logları (debug için çok iyi)
      bat """
        docker compose -f %COMPOSE_FILE% ps
        docker compose -f %COMPOSE_FILE% logs --no-color
      """

      // her zaman kapat
      bat """
        docker compose -f %COMPOSE_FILE% down -v
      """
    }
  }
}
