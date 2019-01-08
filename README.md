# SimpleOCR

## Introduction
The project name is SimpleOCR, since it has nothing to do with ocr, the
app's name may differ.

What I want to accomplish is to save and share excerpt while reading a
book. None of apps shipped in play store can provide a style I expect, so
I decide to make one by my self, that's the birth of SimpleOCR.

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