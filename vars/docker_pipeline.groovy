def call ( map config ) {
  def appName = config.appName
node ('agent1') {
    stage('build ${appName}') {
      sh """"
        mvn --version
      """"
    }
}
}