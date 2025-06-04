def call ( Map config ) {
  properties[(
    parameters[(
      { choice(name: 'Deploy to', choices: ['Dev', 'QA', 'Stage', 'Prod'], description: 'Where to Deploy') }
    )]
  )]
  def appName = config.appName
  def gitrepo = "https://github.com/manojpadigineti-cloud/i27-eureka.git"
  pipeline {
node ('agent1') {
    stage("checkout SCM") {
    withCredentials([gitUsernamePassword(credentialsId: 'Github_Token_New', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
       checkout scm
     }
    }
    stage("build ${appName}") {
      sh """
        mvn clean package
      """
    }
}
}
}