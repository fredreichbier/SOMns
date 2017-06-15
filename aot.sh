#!/bin/sh

GRAALVM_DIR=/Users/smarr/Projects/SOM/graalvm/graalvm-0.24
JVMCI_DIR=${GRAALVM_DIR}/jdk/jre/lib/jvmci
LIB=${GRAALVM_DIR}/lib
GRAAL=${LIB}/graal

SOM_DIR=/Users/smarr/Projects/SOM/SOMns

CLASSPATH=${JVMCI_DIR}/jvmci-api.jar:${JVMCI_DIR}/jvmci-hotspot.jar:${GRAAL}/enterprise-graal.jar:${GRAAL}/graal.jar:${SOM_DIR}/libs/truffle/truffle/mxbuild/dists/truffle-api.jar:${SOM_DIR}/libs/truffle/truffle/mxbuild/dists/truffle-debug.jar:${LIB}/truffle/truffle-om.jar:${LIB}/svm/svm.jar:${LIB}/svm/svm-api.jar:${LIB}/svm/objectfile.jar:${LIB}/svm/lib/gson-2.2.4.jar

# CLASSPATH=${JVMCI_DIR}/jvmci-api.jar:${JVMCI_DIR}/jvmci-hotspot.jar:${GRAAL}/enterprise-graal.jar:${GRAAL}/graal.jar:${SOM_DIR}/libs/truffle/truffle/mxbuild/dists/truffle-api.jar:${SOM_DIR}/libs/truffle/truffle/mxbuild/dists/truffle-debug.jar:${LIB}/truffle/truffle-om.jar:${LIB}/svm/svm.jar:${LIB}/svm/svm-api.jar:${LIB}/svm/objectfile.jar:${LIB}/svm/lib/gson-2.2.4.jar:${GRAALVM_DIR}/language/R/jline.jar


${GRAALVM_DIR}/jdk/bin/java \
  -server -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI \
  -XX:-UseJVMCIClassLoader -d64 -noverify \
  -cp ${CLASSPATH} \
  -Duser.country=US -Duser.language=en -Dgraal.ForceDebugEnable=false \
  -Dgraal.EagerSnippets=true -Dsubstratevm.version=0.24 \
  -Dgraalvm.locatorDisabled=true -Dcom.oracle.truffle.aot=true \
  -Dtruffle.TruffleRuntime=com.oracle.svm.truffle.api.SubstrateTruffleRuntime \
  -Xms1G -Xss10m -Xmx3G \
 com.oracle.svm.hosted.BootImageGeneratorRunner \
 -imagecp ${CLASSPATH}:${LIB}/svm/library-support.jar:${GRAAL}vm/graalvm.jar:${SOM_DIR}/build/classes/:${SOM_DIR}/libs/somns-deps.jar \
 -H:InspectServerContentPath=${LIB}/svm/inspect \
 -H:CLibraryPath=${LIB}/svm/clibraries/darwin-amd64 \
 -H:Path=. -H:+MultiThreaded -R:YoungGenerationSize=1g -R:OldGenerationSize=3g \
 -H:Class=som.VM -H:Name=somns -H:Features=com.oracle.svm.truffle.TruffleFeature

#  -H:VerifyNamingConventions=true \
# -H:+AddAllCharsets -H:Class=org.truffleruby.Main -H:Name=ruby -H:Features=com.oracle.svm.truffle.TruffleFeature,com.oracle.svm.truffle.TruffleFeature,com.oracle.svm.jnr.SubstrateJNRFeature -H:SubstitutionResources=org/truffleruby/aot/substitutions.json
 

 ## JS settings   -Dtruffle.js.SubstrateVM=true -Dtruffle.js.PrepareFirstContext=true -Dtruffle.js.Debug=false
 
 
 
#  -Dsom.traceFile=${SOM_DIR}/traces/trace -DfailOnMissingOptimization=true
#  -esa -ea -Dgraal.TraceTruffleInlining=false -Dgraal.TraceTruffleCompilation=false -Dsom.tools=${SOM_DIR}/tools -Xbootclasspath/a:: som.VM --platform ${SOM_DIR}/core-lib/Platform.som --kernel ${SOM_DIR}/core-lib/Kernel.som core-lib/Hello.som
# Hello World!

