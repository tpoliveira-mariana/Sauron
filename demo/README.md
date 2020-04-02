### Sauron Usage Guide

#### Launch ServerApp
```
$ cd silo-server
$ mvn compile exec:java -Dexec.args="[port]"
```
To launch server on port `[port]`.
If you choose to not include `-Dexec.args="[port]"`, the server will run on port 8080 by default.

With the server running, any of the following clients can be concurrently launched in a separate terminal,
 for example by pressing `ctrl-shift-t`, and proceed as follows:

---

#### Launch EyeApp
```
$ cd ../eye
$ mvn compile exec:java -Dexec.args="[host] [port] [camera name] [latitude] [longitude]"
```
For example, to launch the EyeApp and connect it to the server on localhost:8080 using a camera named Tagus, 
located at (38.737613 -9.403164) use:

```
$ cd ../eye
$ mvn compile exec:java -Dexec.args="localhost 8080 Tagus 38.737613 -9.403164"
```
After launching EyeApp you can submit observations as follows: 
You can add an observation as follows:
```
[type],[ID]
```
##### **Type and ID rules:**    
    
The type must be either `car` or `person`. 
- Car

    1. ID is its license plate number which must have 6 characters
    2. The ID must have 3 subgroups of 2 characters each
    3. Each subgroup can only be of capital letters or numbers
    4. There cannot be 3 subgroups of letters or numbers simultaneously
    
- Person
    1. ID must be a positive integer
    
Each observation, as well as each new command of the following, must be in a new line. 

##### **Comments:** 
Comment lines, which will be ignored, start with `#`.

##### **Sleep:** 

You can also make the Eye sleep for a certain amount of time by typing `zzz,[milliseconds]` between observations.

##### **Submission:** 
When a blank line is detected, the observations above it will be submitted. 
Another way of submitting them is by closing the standard input.

##### **Input redirection:** 
If you want, you may redirect the input from a file of your choice:

```
$ cd ../eye
$ mvn compile exec:java -Dexec.args="localhost 8080 Tagus 38.737613 -9.403164" < ../demo/testEye_1.txt
```
For testing purposes, you can run multiple scenarios using the different `testEye_X.txt` files.

---

#### Launch SpotterApp

```
$ cd ../spotter
$ mvn compile exec:java -Dexec.args="[host] [port]"
```

For example, to launch the SpotterApp and connect it to the server on localhost:8080

```
$ cd ../spotter
$ mvn compile exec:java -Dexec.args="localhost 8080"
```

After launching the spotter app the following commands are available to you:

- #####Init Command

    ```
    init [file]
    ```

    The init command is a control command that receives a file containing cameras and observations to load the server with.
    
    For example, to use the file spotterInit.txt write,
    
    ```
    init spotterInit.txt
    ```
  
    Note: To create your own server init file read the sub-section below `Creating your own init file`.
- #####Clear Command
    ```
    clear
    ```

    The clear command is a control command that clears all the server data. This command takes no arguments.

- #####Ping Command

    ```
    ping
    ```
    
    The ping command is a control command that pings the server. 
    Its only usage is for you to check if the spotter is correctly connected to the server.
    
    The app sends `spotter` as message, to which the server should reply with `Hello spotter!`, meaning everything is working fine.

- #####Spot Command

    ```
    spot [type] [ID]
    ```
    
    The spot command is a command that allows you to search for the last observation registered on the object of the type and ID given.
    
    There are some rules for the type and ID (see the previous subsection `Type and ID rules` in the `Launch EyeApp` section).
        
    For example, to search for the car with ID `AABB33` use:
    
    ```
    spot car AABB33
    ```
  
    The special character `* ` can be specified in the ID, in which case it will return the last observation recorded on every object of the type given which ID matches the partial ID given.
    
    For example to search for every person whose id starts with `1` and ends with `1` use:
    ```
    spot person 1*1
    ```
    
    The output of this command comes in the form of:
    
    ```
    [type],[ID],[observation date],[camera name],[camera latitude],[camera longitude]
    ```
  
- #####Trail Command

    ```
    trail [type] [ID]
    ```
  
    The trail command is a command that allows you to search for every observation registered on the object of the type and ID given.
    
    The output comes in the same format as the command before.
    
    The same rules to ID as before apply to this command (see the previous subsection `Type and ID rules` in the `Launch EyeApp` section).
    
    Note: This command needs the exact ID, therefore the ID cannot contain the `*` character. 
    
    
##### **Creating your own server init file:**

To create your own init file:
- To create a camera write
    ```
    cam,[camera name],[camera latitude],[camera longitude]
    ```
- To create an observation write
    ```
    [type],[ID]
    ```
- Each observation must have a camera declared in the lines before it
- Each observation will be linked to the last created camera on the lines prior
- Each one of the former commands must be written in a new line
- When you are done type `done` in a new line

---

#### Launch ClientApp
```
$ cd ../silo-client
$ mvn compile exec:java -Dexec.args="[host] [port]"
```
The SiloClientApp's single function is to ping the server.
The app sends `SiloClientApp` as message, to which the server should reply with `Hello SiloClientApp!`.

For example, to launch the SiloClientApp on localhost:8080 use:

```
$ cd ../silo-client
$ mvn compile exec:java -Dexec.args="localhost 8080"
```

---

#### Run Automatic Tests 

In order for you to run the automatic tests you must:
1. Launch ServerApp in one terminal
2. Open a different terminal tab by pressing `ctrl-shift-t`
3. Go to project's root directory: `$ cd ../`
4. Install the project without skipping tests: `$ mvn install`

