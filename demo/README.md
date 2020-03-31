### Sauron Usage Guide

#### Launch ServerApp
```
$ cd ../silo-server/
$ mvn compile exec:java
```

Server runs on port 8080 by default.

#### Launch EyeApp
```
$ cd ../eye/
$ ./target/appassembler/bin/eye [host] [port] [camera name] [latitude] [longitude]
```
For example, to launch the EyeApp on localhost:8080 using a camera named Tagus, 
located at (38.737613 -9.403164) use:

```
$ cd ../eye/
$ ./target/appassembler/bin/eye localhost 8080 Tagus 38.737613 -9.403164
```

If you want, you may redirect the input from a file of your choice:

```
$ cd ../eye/
$ ./target/appassembler/bin/eye localhost 8080 Tagus 38.737613 -9.403164 < ../demo/testEye_1.txt
```
For testing purposes, you can run multiple scenarios using the different `testEye_X.txt` files.


#### Launch SpotterApp


#### Launch ClientApp


#### Run Automatic Tests 

In order for you to run the automatic tests you must:
1. Launch ServerApp in one terminal;
2. Launch different terminal pressing `ctrl-shift-t`
3. Go to project's root directory:
`$ cd ../`
4. Install the project without skipping tests: 
`$ mvn install`

