import opendataflow.{ OpenDataFlowBuild, Formatting, Dependencies, ScaladocNoVerificationOfDiagrams }

OpenDataFlowBuild.defaultSettings

Formatting.formatSettings

Dependencies.cli

// enablePlugins(ScaladocNoVerificationOfDiagrams)

resolvers +=  Resolver.sonatypeRepo("public")