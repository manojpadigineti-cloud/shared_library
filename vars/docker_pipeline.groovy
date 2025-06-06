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
     APPLICATION_PORT = config.port
     def IPADDRESS = 10.2.0.2

       stage ("checkout SCM") {
        withCredentials([gitUsernamePassword(credentialsId: GITCREDS, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
           checkout scm
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
        stage ("Copy Artifact of ${env.appName}") {
          script {
            if (params.Docker_Build_PUSH == 'YES') {
               Artifact_copy()
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

 def Artifact_copy (){
   def POM = readMavenPom file: 'pom.xml'
   def ARTIFACT_FILE = "$WORKSPACE/target/${POM.name}-${POM.version}.${POM.packaging}"
   def JAR_SOURCE = "${POM.name}-${POM.version}.${POM.packaging}"
    sh """
     scp -o StrictHostKeyChecking=no -r ${ARTIFACT_FILE} devops@${IPADDRESS}:/home/devops/
     docker build --build-arg JAR_SOURCE=${JAR_SOURCE} --build-arg PORT=$APPLICATION_PORT -t ${env.appName}-$GIT_COMMIT  .
    """
 }