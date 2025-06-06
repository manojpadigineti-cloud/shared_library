// Pluging require:
//  - Pipeline Utility Steps : To read POM in jenkins
//  - Sonarqube Plugin
//  - Blue Ocean Plugin



def call ( Map config ) {
    properties([
        parameters([
            choice(name: 'Code_Build', choices: ['NO', 'YES'], description: 'Building your code required'),
            choice(name: 'Code_Scan', choices: ['NO', 'YES'], description: 'Code Scan required'),
            choice(name: 'Docker_Build_PUSH', choices: ['NO', 'YES'], description: 'Docker Build and Push to Repository'),
            choice(name: 'DEPLOY_ENV', choices: ['N/A','Dev', 'QA', 'Stage', 'Prod'], description: 'Where to deploy the application')
        ])
    ])


    node ('agent1') {
    // Global ENV
     env.appName = config.appName
     def GITCREDS = 'Github_Token_New'
     def IPADDRESS = '10.2.0.2'
     def DOCKER_CREDS = 'Docker_Server'
     def DOCKER_HUB = 'DOCKER_HUB_CREDS'
     def DOCKER_REPO = 'manojpadigineti'
     env.port = config.port
     def IMAGE_REGISTRY = 'docker.io'


       stage ("checkout SCM") {
//         withCredentials([gitUsernamePassword(credentialsId: GITCREDS, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
//            checkout scm // In this you wont get commit id
//          }
         script {
            withCredentials([gitUsernamePassword(credentialsId: GITCREDS, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
              def FETCH_COMMIT_ID = checkout scm
              env.GIT_COMMIT = FETCH_COMMIT_ID.GIT_COMMIT
              echo "Checked out commit: ${env.GIT_COMMIT}"
            }
          }
        }
       stage ("Build Application ${env.appName}") {
          script {
          if (params.Code_Build == 'YES' || params.Code_Scan == 'YES') {
                  Build().call()
            }
          else {
            echo "Skipping the build as ${params.Code_Build}"
            }
           }
         }
       stage ("Code Scan for ${env.appName}") {
             script {
              if (params.Code_Scan == 'YES') {
              withSonarQubeEnv('Sonar-Server') {
                 Sonar_Scan().call()
                 }
                  timeout(time: 2, unit: 'MINUTES') {
                   waitForQualityGate abortPipeline: true
                }
              }
               else {
                 echo "Skipping the scan for ${params.Code_Scan}"
                }
            }
          }
        stage ("Docker Build_Push of Application ${env.appName}") {
          script {
            if (params.Docker_Build_PUSH == 'YES') {
            withCredentials([usernamePassword(credentialsId: DOCKER_CREDS, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
              withCredentials([usernamePassword(credentialsId: DOCKER_HUB, usernameVariable: 'DOCKER_USR', passwordVariable: 'DOCKER_PSW')]) {
               Docker_Build_Push(IPADDRESS, env.port, PASSWORD, DOCKER_REPO, env.GIT_COMMIT, DOCKER_PSW, DOCKER_USR, IMAGE_REGISTRY)
              }
             }
            }
          }
         }
        }
     }


 // Methods
 def Build () {
   return {
    sh 'mvn clean package -DskipTests'
   }
 }

 def Sonar_Scan() {
   return {
   sh """
     mvn clean verify sonar:sonar \
              -Dsonar.projectKey=i27eureka
    """
   }
 }

 def Docker_Build_Push (IPADDRESS, APPLICATION_PORT, PASSWORD, REPO_NAME, GIT_COMMIT, DOCKER_PASSWORD, DOCKER_USERNAME, IMAGE_REGISTRY){
   def POM = readMavenPom file: 'pom.xml'
   def ARTIFACT_FILE = "$WORKSPACE/target/${POM.name}-${POM.version}.${POM.packaging}"
   def JAR_SOURCE = "${POM.name}-${POM.version}.${POM.packaging}"
   sh """
     sshpass -p '${PASSWORD}' scp -o StrictHostKeyChecking=no -r ${ARTIFACT_FILE} devops@${IPADDRESS}:/home/devops
     sshpass -p '${PASSWORD}' scp -o StrictHostKeyChecking=no -r Dockerfile devops@${IPADDRESS}:/home/devops
     sshpass -p '${PASSWORD}' -v ssh -o StrictHostKeyChecking=no devops@${IPADDRESS} docker build --no-cache --build-arg JAR_SOURCE=${JAR_SOURCE} --build-arg PORT=${APPLICATION_PORT} -t ${IMAGE_REGISTRY}/${REPO_NAME}/${env.appName}:${GIT_COMMIT}  .
     sshpass -p '${PASSWORD}' -v ssh -o StrictHostKeyChecking=no devops@${IPADDRESS} podman login docker.io -u ${DOCKER_USERNAME} -p ${DOCKER_PASSWORD}
     sshpass -p '${PASSWORD}' -v ssh -o StrictHostKeyChecking=no devops@${IPADDRESS} podman push ${IMAGE_REGISTRY}/${REPO_NAME}/${env.appName}:${GIT_COMMIT}
    """
 }