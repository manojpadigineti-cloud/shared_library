def call ( Map config ) {
  def appName = config.appName
node ('agent1') {
    stage("build ${appName}") {
      sh '''
      hostname -i
        mvn --version
      '''
    }
}
}