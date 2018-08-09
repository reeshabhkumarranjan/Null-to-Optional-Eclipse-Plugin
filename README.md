# Convert Null to Optional Refactoring

[![Build Status](https://travis-ci.com/ponder-lab/Null-to-Optional-Eclipse-Plugin.svg?token=ysqq4ZuxzD688KNytWSA&branch=master)](https://travis-ci.com/ponder-lab/Null-to-Optional-Eclipse-Plugin) [![Coverage Status](https://coveralls.io/repos/github/ponder-lab/Null-to-Optional-Eclipse-Plugin/badge.svg?t=pnai3D)](https://coveralls.io/github/ponder-lab/Null-to-Optional-Eclipse-Plugin)

## Eclipse Application extending the JDT Plug-in API
![N2O ui](https://i.imgur.com/VVFBFIH.png)
## Introduction

This prototype refactoring plug-in for [Eclipse](http://eclipse.org) represents ongoing work in developing an automated refactoring tool that would assist developers in optimizing their Java 8 code. The ultimate goal is to add to the Eclipse JDT a fully automated, semantics-preserving refactoring for legacy Java code that replaces occurrences of null values with instances of an appropriately parameterized optional type, first introduced in Java 8.

## Usage
The user can initiate a refactoring by right-clicking on a project, source folder or .jar, package, compilation unit, type, field or method element inside the Package Explorer view. Mouse over 'Refactor' and then left-click on 'Convert Null To Optional' to run the plugin.

![Using](https://i.imgur.com/j7Tjczz.png)


### Installation for Usage And Development
Currently, the plugin builds and runs in both evaluation and refactoring mode on [Eclipse Oxygen for RCP/RAP](https://www.eclipse.org/downloads/packages/release/oxygen/3a/eclipse-rcp-and-rap-developers).
The project includes a maven configuration file using the tycho plug-in, which is part of the [maven eclipse plugin](http://www.eclipse.org/m2e/). Running `mvn install` will install all dependencies. Note that if you are not using maven, this plugin depends on the following projects and plugins being present in the workspace:
- https://github.com/khatchad/edu.cuny.citytech.refactoring.common (which can be imported as a git repository)
- the **Eclipse SDK**, **Eclipse SDK tests**, and the **Eclipse testing framework**. 
  - These can be installed from the "Install New Software..." menu option under "Help" in Eclipse.
  - Choose the 'update site': [The Eclipse Project Updates](http://download.eclipse.org/eclipse/updates/4.7) in 'Work With' field.
- After installing these plugins, there will be two 'Missing API Baseline' errors visible in the 'Problems' view. Simply quick fix these and reduce the errors to warnings, as it isn't particularly critical.
![QuickFix](https://i.imgur.com/XATKKxA.png) ![changeToWarnings](https://i.imgur.com/SOKJkNr.png)

#### Optionally, you may also install:
Some useful plugins for working with AST's and the Java Model that are not part of the Eclipse SDK but can be downloaded from this update site: http://www.eclipse.org/jdt/ui/update-site.
- org.eclipse.jdt.astview - ASTView
- org.eclipse.jdt.jeview - JavaElement View

### Limitations
This plugin for the Eclipse JDT is still very much a work in progress as it constitutes the application of ongoing research, and as such should not be relied upon.
Some of what's missing:
- Handling method references and streams API programming. Right now, any elements that are type-dependent on a value from a Stream API are simply failed: there will be no transformation suggested.
- Error and UI Wizard messages: These need lots of work, right now it is hard to tell why an element might have failed as the messages are not particularly descriptive to users, and mostly are carried over from the test debugging.

### Running the Evaluator
The plug-in edu.cuny.hunter.optionalrefactoring.eval is the evaluation plug-in. Note that it is not included in the standard update site as that it user focused. To run the evaluator, clone the repository and build and run the plug-in from within Eclipse. This will load the plug-in edu.cuny.hunter.optionalrefactoring.eval (verify in "installation details.").

There is no UI menu options for the evaluator, however, there is an Eclipse command, which is available from the quick execution dialog in Eclipse. Please follow these steps:

1. Select a group of projects.
2. Press CMD-3 or CTRL-3 (command dialog).
3. Search for "evaluate." You'll see an option to run the migration evaluator. Choose it.
4. Once the evaluator completes, a set of `.csv` files will appear in the working directory.

## Contributing
