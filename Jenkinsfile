pipeline {
    agent any
    tools {
            maven 'default'
            jdk 'openjdk14'
    }
    stages {
        stage('Build') {
            steps {
                echo 'Building..'
                sh 'mvn clean package'
            }
        }

        /*stage('Test') {
            steps {
                echo 'Testing..'
            }
        }
        stage('Deploy') {
            steps {
                echo 'Deploying....'
            }
        }*/
    }
    post {
                    always {
                        archiveArtifacts artifacts: 'target/*jar', fingerprint: true
                        junit 'target/surefire-reports/*.xml'
                    }
    }
}
