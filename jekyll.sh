#!/usr/bin/bash

if [[ -f ~/bin/jekyll ]]
then
    JEKYLL=~/bin/jekyll
else
    JEKYLL=jekyll
fi

${JEKYLL} build --source docs --destination build/jekyll --watch
