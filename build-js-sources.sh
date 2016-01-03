#!/bin/sh

git submodule update --init

if [ ! -d "resources/node_modules/react" ]; then
    cd resources
    npm install react@0.11.1
    cd ..
fi
