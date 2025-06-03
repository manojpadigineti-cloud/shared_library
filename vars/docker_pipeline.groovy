def call ( Map config ) {
  def appName = config.appName
  def gitrepo = "https://github.com/manojpadigineti-cloud/i27-eureka.git"
node ('agent1') {
    stage("checkout SCM") {
    withCredentials([gitUsernamePassword(credentialsId: 'Github_Token_New', gitToolName: 'git-tool')]) {
      sh '''
         "git clone ${env.gitrepo}"
      '''
     }
    }
    stage("build ${appName}") {
      sh '''
      hostname -i
        mvn --version
      '''
    }
}
}