### Sauron Usage Guide

#### Launch ServerApp
```
$ cd silo-server
$ mvn compile exec:java
```

Server runs on port 8080 by default.

With the server running, any of the following clients can be concurrently launched in a separate terminal,
 for example by pressing `ctrl-shift-t`, and proceed as follows:

#### Launch EyeApp
```
$ cd ../eye
$ ./target/appassembler/bin/eye [host] [port] [camera name] [latitude] [longitude]
```
For example, to launch the EyeApp on localhost:8080 using a camera named Tagus, 
located at (38.737613 -9.403164) use:

```
$ cd ../eye
$ ./target/appassembler/bin/eye localhost 8080 Tagus 38.737613 -9.403164
```

If you want, you may redirect the input from a file of your choice:

```
$ cd ../eye
$ ./target/appassembler/bin/eye localhost 8080 Tagus 38.737613 -9.403164 < ../demo/testEye_1.txt
```
For testing purposes, you can run multiple scenarios using the different `testEye_X.txt` files.


#### Launch SpotterApp


#### Launch ClientApp
```
$ cd ../silo-client
$ ./target/appassembler/bin/silo-client [host] [port]
```
The SiloClientApp's single function is to ping the server.
The app sends `SiloClientApp` as message, to which the server should reply with `Hello SiloClientApp!`.

For example, to launch the SiloClientApp on localhost:8080 use:

```
$ cd ../silo-client
$ ./target/appassembler/bin/silo-client localhost 8080
```


#### Run Automatic Tests 

In order for you to run the automatic tests you must:
1. Launch ServerApp in one terminal
2. Open a different terminal tab by pressing `ctrl-shift-t`
3. Go to project's root directory: `$ cd ../`
4. Install the project without skipping tests: `$ mvn install`

