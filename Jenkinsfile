pipeline {
  agent any

  options {
    timestamps()
  }

  environment {
    COMPOSE_FILE = "docker-compose.yml"
    BACKEND_URL  = "http://localhost:6969"
    FRONTEND_URL = "http://localhost:5173"
   // UI_BASE_URL  = "http://host.docker.internal:5173"
   //UI_HEADLESS  = "true"

    UI_BASE_URL  = "http://frontend:5173"
    UI_HEADLESS  = "true"


  }

  triggers {
    pollSCM('* * * * *')
  }

  stages {

    stage('1- Checkout') {
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

    stage('2- Build (no tests)') {
      steps {
        dir('municipality-service-backend') {
          sh '''
            set -eux
            if [ -f "./mvnw" ]; then
              chmod +x ./mvnw
              ./mvnw -DskipTests clean package
            else
              mvn -DskipTests clean package
            fi
          '''
        }
      }
    }

    stage('3- Unit + Slice Tests (Surefire)') {
      steps {
        dir('municipality-service-backend') {
          sh '''
            set -eux
            if [ -f "./mvnw" ]; then
              chmod +x ./mvnw
              ./mvnw test
            else
              mvn test
            fi
          '''
        }
      }
    }

    stage('4- Integration Tests (Failsafe *IT)') {
      steps {
        dir('municipality-service-backend') {
          sh '''
            set -eux
            if [ -f "./mvnw" ]; then
              chmod +x ./mvnw
              ./mvnw -DskipTests verify
            else
              mvn -DskipTests verify
            fi
          '''
        }
      }
    }

    stage('5- Docker Compose Up (System Running)') {
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
          echo "Waiting DB health (service=db)"

          ok="false"
          for i in $(seq 1 60); do
            cid="$($DC -f "$COMPOSE_FILE" ps -q db || true)"
            if [ -n "$cid" ]; then
              status="$(docker inspect -f '{{.State.Health.Status}}' "$cid" 2>/dev/null || true)"
              echo "db status=$status cid=$cid"
              if [ "$status" = "healthy" ]; then
                ok="true"
                break
              fi
            fi
            sleep 2
          done

          if [ "$ok" != "true" ]; then
            echo "DB did not become healthy."
            $DC -f "$COMPOSE_FILE" ps || true
            $DC -f "$COMPOSE_FILE" logs --no-color db || true
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

          # backend container id
          cid="$($DC -f "$COMPOSE_FILE" ps -q backend)"
          if [ -z "$cid" ]; then
            echo "backend container not found"
            $DC -f "$COMPOSE_FILE" ps || true
            exit 1
          fi

          # compose network name (e.g. adem_default)
          net="$(docker inspect -f '{{range $k,$v := .NetworkSettings.Networks}}{{$k}}{{end}}' "$cid")"
          echo "backend cid=$cid net=$net"

          ok="false"
          for i in $(seq 1 90); do
            # probe from same docker network, using service DNS name: backend
            if docker run --rm --network "$net" curlimages/curl:8.5.0 \
                 -fsS --max-time 2 http://backend:6969/actuator/health \
               | grep -q '"status":"UP"'; then
              ok="true"
              break
            fi
            sleep 2
          done

          if [ "$ok" != "true" ]; then
            echo "Backend did not become ready."
            $DC -f "$COMPOSE_FILE" logs --no-color backend || true
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

          cid="$($DC -f "$COMPOSE_FILE" ps -q frontend)"
          net="$(docker inspect -f '{{range $k,$v := .NetworkSettings.Networks}}{{$k}}{{end}}' "$cid")"

          ok="false"
          for i in $(seq 1 90); do
            code="$(docker run --rm --network "$net" curlimages/curl:8.5.0 \
                    -s -o /dev/null -w "%{http_code}" --max-time 2 http://frontend:5173/ || true)"
            if [ -n "$code" ] && [ "$code" -ge 200 ] && [ "$code" -lt 500 ]; then
              ok="true"
              break
            fi
            sleep 2
          done

          if [ "$ok" != "true" ]; then
            echo "Frontend did not become ready."
            $DC -f "$COMPOSE_FILE" logs --no-color frontend || true
            exit 1
          fi

          echo "✅ Frontend is ready."
        '''
      }
    }
    stage('Install Chromium (for UI E2E)') {
      steps {
        sh '''
          set -eux
          # Jenkins container root değilse bu stage çalışmaz.
          # Çalışıyorsa:
          apt-get update
          apt-get install -y --no-install-recommends \
            chromium \
            libnss3 libglib2.0-0 libgbm1 libasound2 \
            libx11-6 libx11-xcb1 libxcomposite1 libxdamage1 libxext6 \
            libxfixes3 libxrandr2 libxrender1 libxtst6 \
            fonts-liberation ca-certificates

          chromium --version || true
          which chromium || true
        '''
      }
    }
    stage('Verify UI reachable') {
      steps {
        sh '''
          set -eux
          getent hosts host.docker.internal || true
          curl -I --max-time 5 http://host.docker.internal:5173/ | head -n 1
        '''
      }
    }


    // 6. aşama: çalışan sistem üzerinde 3 senaryo
    stage('6.1- E2E Scenario 1') {
      steps {
        dir('municipality-service-backend') {
          sh '''
            set -eux
            if [ -f "./mvnw" ]; then
              chmod +x ./mvnw
              ./mvnw -Pe2e \
                -Dui.baseUrl="$UI_BASE_URL" \
                -Dui.headless="$UI_HEADLESS" \
                -Dtest="**/integration/e2e/ui/admin/**/*E2EIT.java" \
                test
            else
              mvn -Pe2e \
                -Dui.baseUrl="$UI_BASE_URL" \
                -Dui.headless="$UI_HEADLESS" \
                -Dtest="**/integration/e2e/ui/admin/**/*E2EIT.java" \
                test
            fi
          '''
        }
      }
    }

    stage('6.2- E2E Scenario 2') {
      steps {
        dir('municipality-service-backend') {
          sh '''
            set -eux
            if [ -f "./mvnw" ]; then
              chmod +x ./mvnw
              ./mvnw -Pe2e \
                -Dui.baseUrl="$UI_BASE_URL" \
                -Dui.headless="$UI_HEADLESS" \
                -Dtest="**/integration/e2e/ui/agent/**/*E2EIT.java" \
                test
            else
              mvn -Pe2e \
                -Dui.baseUrl="$UI_BASE_URL" \
                -Dui.headless="$UI_HEADLESS" \
                -Dtest="**/integration/e2e/ui/agent/**/*E2EIT.java" \
                test
            fi
          '''
        }
      }
    }

    stage('6.3- E2E Scenario 3') {
      steps {
        dir('municipality-service-backend') {
          sh '''
            set -eux
            if [ -f "./mvnw" ]; then
              chmod +x ./mvnw
              ./mvnw -Pe2e \
                -Dui.baseUrl="$UI_BASE_URL" \
                -Dui.headless="$UI_HEADLESS" \
                -Dtest="**/integration/e2e/ui/auth/**/*E2EIT.java" \
                test
            else
              mvn -Pe2e \
                -Dui.baseUrl="$UI_BASE_URL" \
                -Dui.headless="$UI_HEADLESS" \
                -Dtest="**/integration/e2e/ui/auth/**/*E2EIT.java" \
                test
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
      // Surefire (unit/slice)
      junit allowEmptyResults: true, testResults: 'municipality-service-backend/**/target/surefire-reports/*.xml'
      // Failsafe (integration)
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
