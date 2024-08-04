# Introduction to Distributed Systems - Assignment Template

## General information

The following template is structured as a multi-project build using gradle as a build tool.
By the way, gradle is the only tool supporting multi-project builds.
The main project `idistrsys` specifies all included subprojects in the `settings.gradle` file.
The `client` and `server` are dependent on the `shared` project since all the model classes are included in this project.

## Import the project

1. Open an IDE of your choice (IntelliJ recommended)
2. Import the project as a Gradle project (e.g., File->Open->Select `settings.gradle` in the root folder of the project)

## How to get the server and client running?

### How to start the server?

1. Open a terminal in the root directory of this repository, e.g., directly in IntelliJ
2. Then, build the project with `./gradlew :server:build`
3. Start the server with `./gradlew :server:run`, you can also pass arguments with `--args="arg1 arg2"`. For example for the implementation `UdpRemoteAccess`: `./gradlew :server:run --args="localhost 1337"`
4. Alternatively, use the IntelliJ's Gradle integration. Just select the subproject `server` and double-click on `Tasks/application/<task>`

### How to start the client?

1. Open a second terminal, again in the root directory of this repository (e.g., directly in IntelliJ)
2. You might build the project with `./gradlew :client:build`
3. Start the client implementation with `./gradlew :client:run`, you might also add arguments here: `./gradlew :client:run --args="udp localhost 1337"`
4. Alternatively, use IntelliJ's Gradle integration. Double-click on `client/Tasks/application/<task>`

The `shared` project is automatically included in the dependencies of the client and server.

### How to generate classes via protobuf plugin? (only relevant for Assignment 3 (gRPC)!)

1. Define `Services` and `Messages` in `shared/src/main/proto/ticketManagement.proto`
2. Build the project via `./gradlew :shared:clean :shared:build --console 'verbose'`: the verbose option enables printing of all executed tasks
3. Alternatively, use IntelliJ's Gradle integration. Double-click first on `shared/Tasks/build/clean` and then on `shared/Tasks/build/build`
4. Refresh your gradle project within your IDE (e.g. IntelliJ)
5. Implement client and server interfaces
