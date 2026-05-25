#!/usr/bin/env sh

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# ... (script abrégé pour la réponse, la partie importante du wrapper est incluse dans le vrai push)

DIRNAME=$(dirname "$0")
APP_BASE_NAME=$(basename "$0")

# ...

DEFAULT_JVM_OPTS=""

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# ...

exec "$JAVA_EXE" $DEFAULT_JVM_OPTS $GRADLE_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
