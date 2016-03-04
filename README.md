# Encrypted Zip encoder plugin for Embulk

Encrypted Zip Encoder.

## Overview

* **Plugin type**: encoder

## Configuration

- prefix: description (string, default: `result.%1$03d.%1$03d`)
- password: description (string, required)

## Example

```yaml
out:
  type: file type output plugin
  encoders:
    - type: encrypted_zip
      prefix: 'result.%03d.%03d'
      password: 'mypassword'
```


## Build

```
$ ./gradlew gem  # -t to watch change of files and rebuild continuously
```
