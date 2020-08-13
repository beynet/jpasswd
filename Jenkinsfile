pipeline {
    agent any
    tools {
            maven 'default'
            jdk 'openjdk14'
    }
    parameters {
            string(name: 'release', description: 'Release number')
    }
    stages {
        stage('Change version') {
            echo 'Release ${params.release}'
             sh 'mvn versions:set -DnewVersion=${params.release} -DgenerateBackupPoms=false'
        }
        stage('Build') {
            steps {
                echo 'Building..'
                sh 'mvn clean package'
                archiveArtifacts artifacts: 'target/*jar', fingerprint: true
            }
        }
        stage('Tag and push') {
            echo 'git tag and push'
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

}
