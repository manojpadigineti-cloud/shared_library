def call ( Map config ) {
    properties([
        parameters([
            choice(name: 'Code_Build', choices: ['NO', 'YES'], description: 'Building your code required'),
            choice(name: 'Code_Scan', choices: ['NO', 'YES'], description: 'Code Scan required'),
            choice(name: 'DEPLOY_ENV', choices: ['N/A','Dev', 'QA', 'Stage', 'Prod'], description: 'Where to deploy the application')
        ])
    ])

    node ('agent1') {
    // Global ENV
     env.appName = config.appName

       stage("checkout SCM") {
        withCredentials([gitUsernamePassword(credentialsId: 'Github_Token_New', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
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
                //  timeout(time: 2, unit: 'MINUTES')
                {
                   waitForQualityGate abortPipeline: true
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