# Heracles
The Heracles framework for developing and evaluating text mining algorithms

Heracles is a Java software framework intended to help develop and evaluate text mining algorithms. It is the software that I use to conduct my experiments and it might be of use to other developers.

## Key features:
 * Layered structure
 * Very generic data model that should fit any text mining task
 * Wrappers for Stanford CoreNLP
 * Link to Weka toolkit for machine learning algorithms
 * Proper evaluation methods, including cross-validation, testing algorithms side-by-side, and t-tests for statistical significance

## Installation
The Heracles framework is developed as an Eclipse Java project, so after cloning, you can just import it as an existing project into your Eclipse workspace. Be aware that this project uses Java 8, so make sure you have that installed and that you have an Eclipse version recent enough to support this.

You can use the Git functionality from within Eclipse if desired, as Eclipse will detect this to be a git-based project. Maven is used to automatically link the project to all the necessary external libraries and Eclipse will start downloading the libraries as soon as you add it to the workspace. This might take some time, depending on your connection.

## Included Algorithms
The framework currently includes the code for my ESWC2018 submission. The data files that these algorithms work with can be obtained from the [SemEval-2015](http://alt.qcri.org/semeval2015/task12/index.php?id=data-and-tools) and [SemEval-2016](http://alt.qcri.org/semeval2016/task5/index.php?id=data-and-tools) sites.
 
---
 
More documentation is coming soon.
