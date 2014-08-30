#!/bin/bash

mvn -DaltDeploymentRepository=github-repo-releases::default::file:./build clean deploy
