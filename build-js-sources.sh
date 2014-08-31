#!/bin/sh

git submodule update --init

if [ ! -d "resources/node_modules/react" ]; then
    cd resources
    npm install react
    cd ..
fi
