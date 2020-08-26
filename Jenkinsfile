pipeline {
    agent any
    tools {
            maven 'default'
            jdk 'openjdk14'
    }
    parameters {
            choice(
                        choices: ['non','oui'],
                        description: '',
                        name: 'isRelease')
            string(name: 'release', description: 'Release number')
            string(defaultValue: 'master',name: 'BRANCHE', description: 'git branch name')
    }
    stages {
        stage('Build') {
            steps {
                echo 'Building..'
                sh 'mvn clean package'
                archiveArtifacts artifacts: 'target/*jar', fingerprint: true
            }
        }
        stage('Release') {
            when {
                // Only say hello if a "greeting" is requested
                expression { params.isRelease == 'oui' }
            }

            steps {
                echo "Release ${params.release}"
                sh "git branch --set-upstream-to=origin/${params.BRANCHE} ${params.BRANCHE}"
                sh "mvn versions:set -DnewVersion=${params.release} -DgenerateBackupPoms=false"
                sh 'mvn package -Dmaven.test.skip=true'
                sh "git add pom.xml && git commit -m'version ${params.release}'"
                sh "git tag jpasswd-${params.release}"
                sshagent (credentials: ['GITHUB']) {
                    sh "git push --tags"
                    archiveArtifacts artifacts: 'target/*jar', fingerprint: true
                }
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

}
