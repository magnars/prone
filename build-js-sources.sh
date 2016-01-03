#!/bin/sh

git submodule update --init

if [ ! -d "dev-resources/node_modules/react" ]; then
    cd dev-resources
    npm install react@0.11.1
    cd ..
fi

mkdir -p resources

concat() {
    SRC=$1
    DEST=$2

    echo "/* $SRC */" >> $DEST
    cat $SRC >> $DEST
    echo "" >> $DEST
}

rm -f resources/prone.css
concat dev-resources/prone/better-errors.css resources/prone.css
concat dev-resources/prismjs/themes/prism.css resources/prone.css
concat dev-resources/prismjs/plugins/line-highlight/prism-line-highlight.css resources/prone.css
concat dev-resources/prismjs/plugins/line-numbers/prism-line-numbers.css resources/prone.css
concat dev-resources/prone/styles.css resources/prone.css

rm -f resources/prone-lib.js
concat dev-resources/node_modules/react/dist/react.min.js resources/prone-lib.js
concat dev-resources/prismjs/prism.js resources/prone-lib.js
concat dev-resources/prismjs/plugins/line-numbers/prism-line-numbers.min.js resources/prone-lib.js
concat dev-resources/prismjs/plugins/line-highlight/prism-line-highlight.min.js resources/prone-lib.js
concat dev-resources/prone/prism-line-numbers.js resources/prone-lib.js
concat dev-resources/prism-clojure/prism.clojure.js resources/prone-lib.js
