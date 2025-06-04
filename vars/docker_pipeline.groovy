def call ( Map config ) {
    properties([
        parameters([
            choice(name: 'Code_Build', choices: ['NO', 'YES'], description: 'Building your code required'),
            choice(name: 'Code_Scan', choices: ['NO', 'YES'], description: 'Code Scan required'),
            choice(name: 'DEPLOY_ENV', choices: ['N/A','Dev', 'QA', 'Stage', 'Prod'], description: 'Where to deploy the application')
        ])
    ])

  // Methods Declaration
    def Build () {
    return {
     sh 'mvn clean package -DskipTests'
    }
   }


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
          if (params.Code_Build == 'YES') {
                  Build().call()
            }
          else {
            echo "Skipping the build as ${params.Code_Build}"
            }
           }
         }
       stage ("Code Scan for ${env.appName}") {
            sh """
            echo "Sonar Scan Stage"
            """
          }
        }
     }