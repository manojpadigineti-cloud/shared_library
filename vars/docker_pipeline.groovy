def call ( Map config ) {
    properties([
        parameters([
            choice(name: 'Code_Build', choices: ['NO', 'YES'], description: 'Building your code required'),
            choice(name: 'Code_Scan', choices: ['NO', 'YES'], description: 'Code Scan required'),
            choice(name: 'copyartifact', choices: ['NO', 'YES'], description: 'Copy Artifact'),
            choice(name: 'DEPLOY_ENV', choices: ['N/A','Dev', 'QA', 'Stage', 'Prod'], description: 'Where to deploy the application')
        ])
    ])

   // Declare Credential Variables (Since env. is not allowed in Scripted pipeline, we will declare a variable name and use it dynamically in pipeline)
   def GITCREDS = 'Github_Token_New'

    node ('agent1') {
    // Global ENV
     env.appName = config.appName

       stage("checkout SCM") {
        withCredentials([gitUsernamePassword(credentialsId: GITCREDS, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
           checkout scm
         }
        }
       stage("build ${env.appName}") {
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

        stage("Copy Artifact of ${env.appName}") {
          script {
            if (params.copyartifact == 'YES') {
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
   sh 'pwd ; ls -ltr'
   echo POM.name
   echo POM.version
   echo POM.packaging
  sh """
  cp $(pwd)/target/POM.name-POM.version.POM.packaging $(pwd)
  """
 }