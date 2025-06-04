def call ( Map config ) {
    properties([
        parameters([
            choice(name: 'DEPLOY_ENV', choices: ['Dev', 'QA', 'Stage', 'Prod'], description: 'Where to deploy the application')
        ])
    ])
  def appName = config.appName
    node ('agent1') {
        stage("checkout SCM") {
        withCredentials([gitUsernamePassword(credentialsId: 'Github_Token_New', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
           checkout scm
         }
        }
        stage("build ${appName}") {
          sh """
            mvn clean package -DskipTests
          """
        stage ("Code Scan for ${appName}") {
            sh """
            echo "Sonar Scan Stage"
            """
          }
        }
     }
 }