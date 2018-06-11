# Convert Null to Optional Refactoring

[![Build Status](https://travis-ci.com/ponder-lab/Null-to-Optional-Eclipse-Plugin.svg?token=ysqq4ZuxzD688KNytWSA&branch=master)](https://travis-ci.com/ponder-lab/Null-to-Optional-Eclipse-Plugin) [![Coverage Status](https://coveralls.io/repos/github/ponder-lab/Null-to-Optional-Eclipse-Plugin/badge.svg?t=pnai3D)](https://coveralls.io/github/ponder-lab/Null-to-Optional-Eclipse-Plugin)

## Screenshot

## Introduction

This prototype refactoring plug-in for [Eclipse](http://eclipse.org) represents ongoing work in developing an automated refactoring tool that would assist developers in optimizing their Java 8 code.

## Usage

### Installation for Usage

### Limitations

## Contributing

### Installation for Development

The project includes a maven configuration file using the tycho plug-in, which is part of the [maven eclipse plugin](http://www.eclipse.org/m2e/). Running `mvn install` will install all dependencies. Note that if you are not using maven, this plugin depends on the following projects and plugins being present in the workspace:
- https://github.com/khatchad/edu.cuny.citytech.refactoring.common (which can be imported as a git repository)
- the **Eclipse SDK**, **Eclipse SDK tests**, and the **Eclipse testing framework**. 
  - These can be installed from the "Install New Software..." menu option under "Help" in Eclipse.
  - Use the 'Update Site': [The Eclipse Project Updates](http://download.eclipse.org/eclipse/updates/4.7)

#### Optionally, you may also install:
JDT UI contributes some useful plugins for working with AST's and the Java Model that are not part of the Eclipse SDK but can be downloaded from this update site: http://www.eclipse.org/jdt/ui/update-site.
- org.eclipse.jdt.astview - ASTView
- org.eclipse.jdt.jeview - JavaElement View


### Running the Evaluator

The plug-in edu.cuny.hunter.optionalrefactoring.eval is the evaluation plug-in. Note that it is not included in the standard update site as that it user focused. To run the evaluator, clone the repository and build and run the plug-in from within Eclipse. This will load the plug-in edu.cuny.hunter.optionalrefactoring.eval (verify in "installation details.").

There is no UI menu options for the evaluator, however, there is an Eclipse command, which is available from the quick execution dialog in Eclipse. Please follow these steps:

1. Select a group of projects.
2. Press CMD-3 or CTRL-3 (command dialog).
3. Search for "evaluate." You'll see an option to run the migration evaluator. Choose it.
4. Once the evaluator completes, a set of `.csv` files will appear in the working directory.
