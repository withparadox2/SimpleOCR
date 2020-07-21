# SimpleOCR——莫名书摘

## Introduction
What I want to achieve is to save and share excerpt while reading a
book. None of apps shipped in app store can provide a style that fulfill my
expectation, so I decide to make one by self, that's the birth of SimpleOCR.

For now, there are three templates available, "default", "poetry" and "dream",
both keep tiny and concise. The images rendered by these templates are
showed below.

### Template Default
<img src="assets/default_shadow.png" width="550" height="609">

### Template Poetry
<img src="assets/poetry_shadow.png" width="550" height="704">

### Template Dream
<img src="assets/dream_shadow2.png" width="550" height="361">

## Build
Create file `keystore.properties` and fill it up with your configuration:
```
storePassword=
keyPassword=
keyAlias=
storeFile=
```

Execute `gradlew bundleAll` to generate all template bundles, which are located
in `app/src/main/assets/`

In cases where debugging of a single template is needed, one should config
`debug.template` with a specified name of project in `local.properties`
```
debug.template=templateDefault
```

## Licence

```
Copyright 2020 withparadox2

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```