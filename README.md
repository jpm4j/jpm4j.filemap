# jpm4j Filemap (bndtools template)

##About
The demo application is a command line application that scans the disk and uses a
web browser to show a treemap like:

![treemap of file system](https://raw.github.com/jpm4j/jpm4j.filemap/master/jpm4j.filemap/img/treemap.png "Treemap of file system")

This project is a intended to be used a tutorial or template for jpm4j applications developed with
[bndtools](http://bndtools.org/). bndtools is an IDE for OSGi bundles based on [bnd](https://github.com/bndtools/bnd), 
a sister project of jpm4j. bndtools is eminently suitable for creating jpm4j commands and libraries. This
tutorial teaches you to build an OSGi command line application. This repository can be copied and modified
to use as a base for your own applications.

## Requirements
This tutorial assumes you have experience with [Java](http://www.java.com/en/), [OSGi](http://www.osgi.org/Main/HomePage), 
and [bndtools](http://bndtools.org/). Follow the links to learn more about these technologies. It is assumed that
you have bndtools running and some experience with creating projects. This tutorial does not try to teach you
bndtools.

The code and setup files are heavily commented to provide enough information.

## Set Up
This repository is a bndtools _workspace_. You can [fork]
(https://github.com/jpm4j/jpm4j.filemap/fork_select) this repository on github
and then checkout it out on your own system. This process is described on the [github help pages]
(https://help.github.com/articles/fork-a-repo).

## Design
Filemap consists of a single Declarative Service component that registers 2 services:

* A `Servlet` service. The `Servlet` service is picked up by the [Apache Felix Http Whiteboard handler]
  (http://felix.apache.org/site/white-board-pattern-handler.html). This handler
  will register the service with the Http Service under the given `alias` .
* A `Runnable` service. The bndtools launcher will wait for the registration of
  such a service and call its `run` method on the main thread (the thread that
  started the launcher).
  
The Filemap component depends on a single service. When the bndtools launcher
starts the application, it will register an Object service with the commandline
arguments as a property after all initialization is done. Since the Filemap
component depends on this service, it will not start before all is initialized.

The Filemap component therefore implements most of its functionality in the `run`
method. This method parses the commandline arguments and scans the file system.

The project uses OSGi to provide the web infrastructure. External requests are
forwarded to the `doGet` method. There the request is dispatched to method that
reads static resources or one that creates the JSON from the scanned
directories.
 
## Making it a JPM Command
The only requirement for a JPM Command is that it actually contains a main
class that is designated from the manifest in the [Main-Class header]
(http://docs.oracle.com/javase/tutorial/deployment/jar/appman.html). This is a
standard Java facility provided by the JAR files. This way, a JAR can be started
from the command line with the `-jar` option.

    $ java -jar filemap.jar
    
Though any JAR with a `Main-Class` header can be installed with JPM (actually,
any JAR with a main class), it is better to give it a short name. This is done
with the `JPM-Command` header. The syntax of this header is as follows:

	JPM-Command: filemap [';jvmargs=' args ]

The optional `jvmargs` parameter defines the options given to the VM. If there are multiple
commands, separate them with a space and put quotes around the whole `args`:

	JPM-Command: filemap;jvmargs='-Xms6291456 -Xmx81920k'
	
There are a number of other headers that can also be set.

    JPM-Classpath		# Defines dependencies
    JPM-Service			# Defines the command as a system service (start/stop)

TODO link to the appropriate site

## Turning it an executable JAR
When developing with bndtools, it is possible to turn the bnd file (or a .bndrun file)
into an executable jar. This is done with the following command:

	$ bnd package -o ../release filemap.bndrun

The `package` command creates a JAR that contains the framework, resources, bundles, etc. and
creates a control file that when the JAR is started, all its bundles are installed from the JAR.
This single JAR is therefore a full OSGi framework, including all its dependencies. The `-o` 
option places the out (`filemap.jar`) in the release directory.

The command can now be executed like:

    $ java -jar ../release/filemap.jar ~

## Testing it with JPM
The next step is to test filemap with jpm. JPM normally downloads the JARs from the JPM
website but it can also install files from a URL or from a file. 

	$ sudo jpm install --force ../release/filemap.jar
 
The `--force` flag is necessary to override the existing command. Since testing usually
requires multiple iterations it is optional the first time but necessary subsequent times.

You now have a `filemap` command under your  fingertips. Just execute it, and watch the
aninmations while it scans your hard disk.

	$ filemap ~

## Versioning and Meta Data
Once you're satisfied with the executable, it should be shared with the world.
This means thinking about versions. jpm4j uses the OSGi meta data for naming and
versioning, why invent the wheel? OSGi headers are well defined and well
documented.

So you need to add at least the following headers:

	Bundle-Version:	1.0.0.RC1
	Bundle-SymbolicName: jpm4j.filemap.run
	
Notice that it is best to keep the Bundle-SymbolicName of the runtime different
from the Bundle-SymbolicName of the bundle in this project ...

A thing to consider is the use of the _qualifier_, `RC1` in this example. jpm4j
in general derives the _stage_ of the revision from this qualifier. jpm4j
recognizes the following stages:

*  `staged` -  The revision is published for co-developers to use but is not
     release yet. 
* `master` - The revision is available for the larger audience.
* `retired` - The revision is superseded by an newer release but remains available for older builds. 
* `expired` - The revision is withdrawn and should not be used. This is used when the
     revision contains a serious flaw like a fatal security bug. This stage is
     used with care since it will break existing builds.

By default, jpm4j only installs `master` revisions although the `--staged/-s`
install option overrides it. It is therefore paramount to set the qualifier
correctly. During testing, use a qualifier like a time stamp, RCn, etc. 

## Other Meta Data
Since the revision will be shared with the world, it makes sense to properly
document the bundle. The following headers are recognized by jpm4j:

	Bundle-Description:
	Bundle-Vendor:
	Bundle-Copyright:
	Bundle-DocURL:
	Bundle-License:
	Bundle-Developer:
	Bundle-Icon:

TODO link to the appropriate site
	
Notice that the applications are easier to find when the meta data is present and
well written. A good description can make a significant difference.

To minimize the work that needs to be done to maintain meta data, the filemap project
includes the bnd.bnd file in the bndrun file. This way the run file inherits all the
meta data from the filemap bundle:

	-include: ~bnd.bnd
	
	-runbundles: org.apache.felix.scr,\
		org.apache.felix.metatype,\
		org.apache.felix.log,\
		org.apache.felix.http.whiteboard;version='[2,3)',\
		org.apache.felix.http.jetty;version=2.2.0,\
		com.springsource.javax.servlet;version=2.5.0,\
		org.apache.felix.configadmin;version=1.6.0,\
		org.apache.felix.webconsole;version='[3,4)', \
		jpm4j.filemap;version=latest
	
	Bundle-SymbolicName: jpm4j.filemap.run
	JPM-Command: filemap
 
An alternative to the OSGi metadata is to use the pom meta data as described later.

## Uploading
jpm4j is an _index_, not a repository in itself. This means that you must provide
a place for the binaries where they are available on the Internet. This can be
as simple as Dropbox, Google drive, or a simple website using ftp. Any JAR can be manually
given to jpm4j by placing the URL in the search bar and pressing the factory button (you
must be logged in). However, this is manual and therefore error prone.

There exists a very convenient solution: github. Although github is a _source_
repository, there is no reason why it could not also host the binaries. jpm4j has a
special facility to work with github.

This `filemap` example has a directory `./release`. This directory contains a bnd
file repository. By making this a file repository, the bndtools release tool
can be used.

TODO release only works in release 2 and that is not generally available

You can also use a local maven repository and maintain it with the maven
`deploy` command. In that case, you can maintain the meta data in the pom. If there is
a pom file in the directory of the JAR (following maven naming rules for repositories)
it will automatically parse this pom.

[Post hooks] (https://help.github.com/articles/post-receive-hooks) are a feature
of github repositories that can be used to notify jpm4j. Whenever a repo is
committed, github posts a URL to jpm4j. jpm4j then queues this URL and will
parse the repository as soon as possible, picking up any JARs. To use this feature,
go to the repo home page, click `settings` (you must be logged in and have
administrative rights) and then select `Service Hooks` (on the left bar). For the
service hooks, select the top entry: `Webhook URLs(0)`. There enter the following:

	http://jpm4j.org/github
	
Make sure to update the settings. You can manually trigger it on the Webhooks page or
you can not commit the github repository.

jpm4j will automatically link any JARs to the email address of the github account
owner.

Your code is now available on jpm4j, you can search for it, edit its program
page. For filemap:

	https://jpm4j.org/#/p/osgi/jpm4j.filemap.run/


## Further
After you've got this example to work locally, clone this repository and 
*first* change the JPM-Command name. The namespace for these commands is quite
narrow.