pipeline {
  agent any

  options {
    timestamps()
  }

  environment {
    COMPOSE_FILE = "docker-compose.yml"

    BACKEND_URL  = "http://localhost:6969"
    FRONTEND_URL = "http://localhost:5173"

    // Selenium test property'leri (senin BaseUiE2ETest okuyor)
    UI_BASE_URL  = "http://localhost:5173"
    UI_HEADLESS  = "true"
  }

  triggers {
    // Dakikada bir kontrol (polling)
    // Not: GitHub push trigger zaten ayrıca çalıştırır (Job ayarından da açabilirsin)
    pollSCM('* * * * *')
  }

  stages {

    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Detect Docker Compose') {
      steps {
        sh '''
          set -eux
          if docker compose version >/dev/null 2>&1; then
            echo "DC=docker compose" > dc.env
          elif command -v docker-compose >/dev/null 2>&1; then
            echo "DC=docker-compose" > dc.env
          else
            echo "ERROR: Neither 'docker compose' nor 'docker-compose' found."
            docker version || true
            exit 1
          fi
          cat dc.env
        '''
        script {
          def dc = sh(script: "cat dc.env | cut -d= -f2-", returnStdout: true).trim()
          env.DC = dc
          echo "✅ Using compose command: ${env.DC}"
        }
      }
    }

    stage('Docker Compose Up') {
      steps {
        sh '''
          set -eux
          docker version
          $DC version
          $DC -f "$COMPOSE_FILE" up -d --build
          $DC -f "$COMPOSE_FILE" ps
        '''
      }
    }

    stage('Wait: DB Healthy') {
      steps {
        sh '''
          set -eux
          name="municipality_db"
          echo "Waiting DB health: $name"

          ok="false"
          for i in $(seq 1 60); do
            status="$(docker inspect -f '{{.State.Health.Status}}' "$name" 2>/dev/null || true)"
            if [ "$status" = "healthy" ]; then
              ok="true"
              break
            fi
            sleep 2
          done

          if [ "$ok" != "true" ]; then
            echo "DB did not become healthy."
            docker logs "$name" || true
            exit 1
          fi

          echo "✅ DB is healthy."
        '''
      }
    }

    stage('Wait: Backend Ready') {
      steps {
        sh '''
          set -eux
          url="$BACKEND_URL/actuator/health"
          echo "Waiting Backend: $url"

          ok="false"
          for i in $(seq 1 60); do
            if curl -fsS --max-time 2 "$url" | grep -q '"status":"UP"'; then
              ok="true"
              break
            fi
            sleep 2
          done

          if [ "$ok" != "true" ]; then
            echo "Backend did not become ready."
            docker logs municipality_backend || true
            exit 1
          fi

          echo "✅ Backend is ready."
        '''
      }
    }

    stage('Wait: Frontend Ready') {
      steps {
        sh '''
          set -eux
          url="$FRONTEND_URL"
          echo "Waiting Frontend: $url"

          ok="false"
          for i in $(seq 1 60); do
            code="$(curl -s -o /dev/null -w "%{http_code}" --max-time 2 "$url" || true)"
            if [ "$code" != "" ] && [ "$code" -ge 200 ] && [ "$code" -lt 500 ]; then
              ok="true"
              break
            fi
            sleep 2
          done

          if [ "$ok" != "true" ]; then
            echo "Frontend did not become ready."
            docker logs municipality_frontend || true
            exit 1
          fi

          echo "✅ Frontend is ready."
        '''
      }
    }

    stage('Build & UI E2E Tests (mvn verify)') {
      steps {
        dir('municipality-service-backend') {
          sh '''
            set -eux
            if [ -f "./mvnw" ]; then
              chmod +x ./mvnw
              ./mvnw \
                -Dui.baseUrl="$UI_BASE_URL" \
                -Dui.headless="$UI_HEADLESS" \
                clean verify
            else
              mvn \
                -Dui.baseUrl="$UI_BASE_URL" \
                -Dui.headless="$UI_HEADLESS" \
                clean verify
            fi
          '''
        }
      }
    }

    stage('Fake Deploy') {
      when { expression { currentBuild.currentResult == 'SUCCESS' } }
      steps {
        echo "✅ Testler geçti. Fake deploy yapılıyor..."
        echo "🚀 Deploy tamamlandı (fake)."
      }
    }
  }

  post {
    always {
      junit allowEmptyResults: true, testResults: 'municipality-service-backend/**/target/surefire-reports/*.xml'
      junit allowEmptyResults: true, testResults: 'municipality-service-backend/**/target/failsafe-reports/*.xml'

      sh '''
        set +e
        echo "---- compose ps ----"
        $DC -f "$COMPOSE_FILE" ps || true

        echo "---- compose logs ----"
        $DC -f "$COMPOSE_FILE" logs --no-color || true

        echo "---- compose down ----"
        $DC -f "$COMPOSE_FILE" down -v || true
        true
      '''
    }
  }
}
