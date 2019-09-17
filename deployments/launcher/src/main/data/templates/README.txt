====
    Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
====

#-------------------------------------------------------------------------------
# Copyright (c) 2014 Red Hat, Inc..
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the GNU Public License v3.0
# which accompanies this distribution, and is available at
# http://www.gnu.org/licenses/gpl.html
# 
# Contributors:
#     Red Hat, Inc. - initial API and implementation
#-------------------------------------------------------------------------------
This is where groovy templates are stored for generating dynamic html for different parts of the browsable system. The default template is actually loaded from the classpath (embedded in one of the jars), but you can override any template here.

Currently, the templates available for override are:

- directory-listing.groovy

    This is the listing for a content directory in a repository, group, or deploy-point. It's meant to generate an index.html file containing links to all files in the directory.
