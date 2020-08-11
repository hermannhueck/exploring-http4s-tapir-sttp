import sbt._

object ScalacOptions {

  lazy val defaultScalacOptions = Seq(
    "-encoding",
    "UTF-8",                                     // source files are in UTF-8
    "-deprecation",                              // warn about use of deprecated APIs
    "-unchecked",                                // warn about unchecked type parameters
    "-feature",                                  // warn about misused language features
    "-explaintypes",                             // explain type errors in more detail
    "-opt-warnings",                             // enable optimizer warnings
    "-opt:l:inline",                             // enable inline optimizations ...
    "-opt-inline-from:<source>",                 // ... from source files
    "-Xsource:2.13",                             // Treat compiler input as Scala source for scala-2.13
    "-Xcheckinit",                               // wrap field accessors to throw an exception on uninitialized access
    "-Xasync:true",                              // Enable the async phase for scala.async.Async.{async,await}. [false]
    "-Xlint:adapted-args",                       // An argument list was modified to match the receiver.
    // "-Xlint:byname-implicit",                    // Block adapted by implicit with by-name parameter.
    "-Xlint:constant",                           // Evaluation of a constant arithmetic expression resulted in an error.
    "-Xlint:delayedinit-select",                 // Selecting member of DelayedInit.
    "-Xlint:deprecation",                        // Enable -deprecation and also check @deprecated annotations.
    "-Xlint:doc-detached",                       // When running scaladoc, warn if a doc comment is discarded.
    "-Xlint:eta-sam",                            // The Java-defined target interface for eta-expansion was not annotated @FunctionalInterface.
    "-Xlint:eta-zero",                           // Usage `f` of parameterless `def f()` resulted in eta-expansion, not empty application `f()`.
    "-Xlint:implicit-not-found",                 // Check @implicitNotFound and @implicitAmbiguous messages.
    "-Xlint:inaccessible",                       // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any",                          // A type argument was inferred as Any.
    "-Xlint:missing-interpolator",               // A string literal appears to be missing an interpolator id.
    "-Xlint:nonlocal-return",                    // A return statement used an exception for flow control.
    "-Xlint:nullary-unit",                       // `def f: Unit` looks like an accessor; add parens to look side-effecting.
    "-Xlint:option-implicit",                    // Option.apply used an implicit view.
    "-Xlint:package-object-classes",             // Class or object defined in package object.
    "-Xlint:poly-implicit-overload",             // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow",                     // A private field (or class parameter) shadows a superclass field.
    "-Xlint:recurse-with-default",               // Recursive call used default argument.
    "-Xlint:serial",                             // @SerialVersionUID on traits and non-serializable classes.
    "-Xlint:stars-align",                        // In a pattern, a sequence wildcard `_*` should match all of a repeated parameter.
    "-Xlint:type-parameter-shadow",              // A local type parameter shadows a type already in scope.
    "-Xlint:unit-special",                       // Warn for specialization of Unit in parameter position.
    "-Xlint:valpattern",                         // Enable pattern checks in val definitions.
    "-Xlint:implicit-recursion",                 // Warn when an implicit resolves to an enclosing definition.
    // "-Werror",                                   // Fail the compilation if there are any warnings. // previously: -Xfatal-warnings
    "-Wdead-code",                               // Warn when dead code is identified.
    "-Wextra-implicit",                          // Warn when more than one implicit parameter section is defined.
    "-Wnumeric-widen",                           // Warn when numerics are widened.
    "-Wunused:imports",                          // Warn if an import selector is not referenced.
    "-Wunused:patvars",                          // Warn if a variable bound in a pattern is unused.
    "-Wunused:privates",                         // Warn if a private member is unused.
    "-Wunused:locals",                           // Warn if a local definition is unused.
    "-Wunused:explicits",                        // Warn if an explicit parameter is unused.
    "-Wunused:implicits",                        // Warn if an implicit parameter is unused.
    "-Wunused:params",                           // Enable -Wunused:explicits,implicits.
    "-Wunused:linted",                           // -Xlint:unused.
    "-Wunused:nowarn",                           // Warn if a @nowarn annotation does not suppress any warnings.
    "-Wvalue-discard",                           // Warn when non-Unit expression results are unused.
    "-Ybackend-parallelism",
    "4",                                         // Enable paralellisation — change to desired number!
    "-Ycache-plugin-class-loader:last-modified", // Enables caching of classloaders for compiler plugins
    "-Ycache-macro-class-loader:last-modified",  // and macro definitions. This can lead to performance improvements.
    "-Ymacro-annotations",                       // Enable support for macro annotations, formerly in macro paradise.
    "-Wconf:any:warning-verbose"
    // "-Wconf:cat=deprecation:ws,cat=feature:ws,cat=optimizer:ws,any:warning-verbose"
    // "-Xlint:unused",                          // Enable -Wunused:imports,privates,locals,implicits,nowarn.
    // "-Woctal-literal",                        // Warn on obsolete octal syntax.
    // "-Wmacros:<mode>",                        // Enable lint warnings on macro expansions. Default: `before`, `help` to list choices.
  )

  lazy val filterConsoleScalacOptions = Seq(
    "-Werror",
    "-Wdead-code",
    "-Wunused:imports",
    "-Ywarn-unused:imports",
    "-Ywarn-unused-import",
    "-Ywarn-dead-code",
    "-Xfatal-warnings"
  )

  lazy val consoleScalacOptions = defaultScalacOptions.diff(filterConsoleScalacOptions)
}
