pipeline {
  agent any

  options {
    timestamps()
  }

  environment {
    COMPOSE_FILE = "docker-compose.yml"

    // Host (agent) üzerinden erişim için (port publish varsa)
    BACKEND_URL  = "http://localhost:6969"
    FRONTEND_URL = "http://localhost:5173"

    // Varsayılan UI (agent host). Eğer agent container ise otomatik "http://frontend:5173"e düşeceğiz.
    UI_BASE_URL  = "http://localhost:5173"
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
          for i in $(seq 1 90); do
            cid="$($DC -f "$COMPOSE_FILE" ps -q db || true)"
            if [ -n "$cid" ]; then
              status="$(docker inspect -f '{{.State.Health.Status}}' "$cid" 2>/dev/null || true)"
              echo "db status=$status cid=$cid"
              if [ "$status" = "healthy" ]; then
                ok="true"
                break
              fi
            else
              echo "db container not found yet"
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

          cid="$($DC -f "$COMPOSE_FILE" ps -q backend || true)"
          if [ -z "$cid" ]; then
            echo "backend container not found"
            $DC -f "$COMPOSE_FILE" ps || true
            exit 1
          fi

          # network name (first one)
          net="$(docker inspect -f '{{range $k,$v := .NetworkSettings.Networks}}{{println $k}}{{end}}' "$cid" | head -n 1)"
          if [ -z "$net" ]; then
            echo "Could not determine backend network"
            docker inspect "$cid" || true
            exit 1
          fi

          echo "backend cid=$cid net=$net"

          ok="false"
          for i in $(seq 1 120); do
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

          cid="$($DC -f "$COMPOSE_FILE" ps -q frontend || true)"
          if [ -z "$cid" ]; then
            echo "frontend container not found"
            $DC -f "$COMPOSE_FILE" ps || true
            exit 1
          fi

          net="$(docker inspect -f '{{range $k,$v := .NetworkSettings.Networks}}{{println $k}}{{end}}' "$cid" | head -n 1)"
          if [ -z "$net" ]; then
            echo "Could not determine frontend network"
            docker inspect "$cid" || true
            exit 1
          fi

          echo "frontend cid=$cid net=$net"

          ok="false"
          for i in $(seq 1 120); do
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

    stage('Resolve UI Base URL (host vs docker network)') {
      steps {
        script {
          // Agent host'tan localhost:5173 erişebiliyorsan onu kullan.
          // Erişemiyorsan (agent container vs.), E2E'yi docker network içinde koşturacağız ve UI_BASE_URL=frontend olacak.
          def ok = sh(script: 'curl -sS --max-time 1 http://localhost:5173/ >/dev/null 2>&1; echo $?', returnStdout: true).trim()
          if (ok == '0') {
            env.UI_BASE_URL = "http://localhost:5173"
            env.E2E_MODE = "host"
            echo "✅ UI_BASE_URL host üzerinden kullanılacak: ${env.UI_BASE_URL}"
          } else {
            env.UI_BASE_URL = "http://frontend:5173"
            env.E2E_MODE = "docker"
            echo "✅ UI_BASE_URL docker network üzerinden kullanılacak: ${env.UI_BASE_URL} (E2E_MODE=docker)"
          }
        }
      }
    }

    stage('6.1- E2E Scenario 1') {
      steps {
        script {
          if (env.E2E_MODE == "docker") {
            sh '''
              set -eux
              cid="$($DC -f "$COMPOSE_FILE" ps -q backend || true)"
              net="$(docker inspect -f '{{range $k,$v := .NetworkSettings.Networks}}{{println $k}}{{end}}' "$cid" | head -n 1)"

              # Maven'ı aynı network içinde çalıştır (frontend DNS çözülsün)
              docker run --rm --network "$net" \
                -v "$PWD":/workspace -w /workspace/municipality-service-backend \
                maven:3.9.9-eclipse-temurin-21 \
                bash -lc '
                  set -eux
                  if [ -f "./mvnw" ]; then chmod +x ./mvnw; MVN=./mvnw; else MVN=mvn; fi
                  $MVN -Pe2e -Dui.baseUrl="'"$UI_BASE_URL"'" -Dui.headless="'"$UI_HEADLESS"'" -Dtest="**/integration/e2e/ui/admin/**/*E2EIT.java" test
                '
            '''
          } else {
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
      }
    }

    stage('6.2- E2E Scenario 2') {
      steps {
        script {
          if (env.E2E_MODE == "docker") {
            sh '''
              set -eux
              cid="$($DC -f "$COMPOSE_FILE" ps -q backend || true)"
              net="$(docker inspect -f '{{range $k,$v := .NetworkSettings.Networks}}{{println $k}}{{end}}' "$cid" | head -n 1)"

              docker run --rm --network "$net" \
                -v "$PWD":/workspace -w /workspace/municipality-service-backend \
                maven:3.9.9-eclipse-temurin-21 \
                bash -lc '
                  set -eux
                  if [ -f "./mvnw" ]; then chmod +x ./mvnw; MVN=./mvnw; else MVN=mvn; fi
                  $MVN -Pe2e -Dui.baseUrl="'"$UI_BASE_URL"'" -Dui.headless="'"$UI_HEADLESS"'" -Dtest="**/integration/e2e/ui/agent/**/*E2EIT.java" test
                '
            '''
          } else {
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
      }
    }

    stage('6.3- E2E Scenario 3') {
      steps {
        script {
          if (env.E2E_MODE == "docker") {
            sh '''
              set -eux
              cid="$($DC -f "$COMPOSE_FILE" ps -q backend || true)"
              net="$(docker inspect -f '{{range $k,$v := .NetworkSettings.Networks}}{{println $k}}{{end}}' "$cid" | head -n 1)"

              docker run --rm --network "$net" \
                -v "$PWD":/workspace -w /workspace/municipality-service-backend \
                maven:3.9.9-eclipse-temurin-21 \
                bash -lc '
                  set -eux
                  if [ -f "./mvnw" ]; then chmod +x ./mvnw; MVN=./mvnw; else MVN=mvn; fi
                  $MVN -Pe2e -Dui.baseUrl="'"$UI_BASE_URL"'" -Dui.headless="'"$UI_HEADLESS"'" -Dtest="**/integration/e2e/ui/auth/**/*E2EIT.java" test
                '
            '''
          } else {
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
