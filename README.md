
     _____     ___ ___     _ __  
    /\ '__`\ /' __` __`\  /\`'__\
    \ \ \L\ \/\ \/\ \/\ \ \ \ \/ 
     \ \ ,__/\ \_\ \_\ \_\ \ \_\ 
      \ \ \/  \/_/\/_/\/_/  \/_/ 
       \ \_\                     
        \/_/                                  

[![Build Status](https://travis-ci.org/kevinchesser/pmr-rest.svg?branch=master)](https://travis-ci.org/kevinchesser/pmr-rest)

This repo contains the code for all of the interactions with the database for pmr. It is written as a Spring rest controller and connects to a SQLite3 database. It uses Kalium for java bindings to the libsodium crypto library.

[kalium](https://github.com/abstractj/kalium) |
[libsodium](https://github.com/jedisct1/libsodium)

Other PMR Repos:

[pmr-server](https://github.com/jaxmann/pmr-server)

[pmr-web](https://github.com/jaxmann/pmr-web)

#Usage
For this to function properly you need a SQLite3 database with the table definitions defined in the pmr-server repo in the db/ directory. Then you will have to update the connection url within UserController.java.
To run this program just use the command `./gradlew bootRun` This will start the rest controller on localhost:8080 and you will be able to access the endpoints at http://localhost:8080/endpoint
