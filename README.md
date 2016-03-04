# Encrypted Zip encoder plugin for Embulk

Encrypted Zip Encoder.

## Overview

* **Plugin type**: encoder

## Configuration

- filename: filename format in the zip archive file (string, default: `result.%1$03d.%1$03d`)
- password: encryption password (string, required)

## Example

```yaml
out:
  encoders:
    - type: encrypted_zip
      filename: 'result.%03d.%03d.csv'
      password: 'mypassword'
  type: file
  path_prefix: ./path/to/output/data
  file_ext: zip
  formatter:
    type: csv
```


## Build

```
$ ./gradlew gem  # -t to watch change of files and rebuild continuously
```
